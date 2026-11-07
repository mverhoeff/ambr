"""Validate an ambr capture package (folder or .zip) against SPEC.md version 1.

Usage: python validate.py <capture dir or zip>

Prints "OK <n> frames" and exits 0, or prints every failure and exits 1.
"""

import json
import re
import sys
import tempfile
import zipfile
from datetime import datetime
from pathlib import Path

import numpy as np
from PIL import Image

PRESETS = {"object", "room", "outdoor"}
IMAGE_SOURCES = {"arcore_cpu"}
SCAN_ID_RE = re.compile(r"^scan-\d{8}-\d{6}$")
ORTHO_TOL = 1e-3

MANIFEST_CONSTANTS = {
    "format": "ambr-capture",
    "version": 1,
    "coordinate_system": "arcore_world_opengl",
    "units": "meters",
    "depth_units": "millimeters",
}
MANIFEST_STRINGS = ["scan_id", "preset", "created_utc", "device_model", "app_version"]
FRAME_NUMBERS = ["fl_x", "fl_y", "cx", "cy", "sharpness"]
FRAME_INTS = ["w", "h", "depth_w", "depth_h", "timestamp_ns"]
FRAME_STRINGS = ["file_path", "depth_file_path", "confidence_file_path", "image_source"]
PLY_PROPS = [("float", "x"), ("float", "y"), ("float", "z"),
             ("uchar", "red"), ("uchar", "green"), ("uchar", "blue")]


def is_int(v):
    return isinstance(v, int) and not isinstance(v, bool)


def is_number(v):
    return isinstance(v, (int, float)) and not isinstance(v, bool)


def load_json(path, errors):
    if not path.is_file():
        errors.append(f"{path.name}: missing")
        return None
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (ValueError, UnicodeDecodeError) as e:
        errors.append(f"{path.name}: not valid JSON ({e})")
        return None
    if not isinstance(data, dict):
        errors.append(f"{path.name}: top level must be an object")
        return None
    return data


def check_manifest(m, root, errors):
    for key, expected in MANIFEST_CONSTANTS.items():
        if key not in m:
            errors.append(f"manifest.json: missing key '{key}'")
        elif m[key] != expected or type(m[key]) is not type(expected):
            errors.append(f"manifest.json: '{key}' must be {expected!r}, got {m[key]!r}")
    for key in MANIFEST_STRINGS:
        if key not in m:
            errors.append(f"manifest.json: missing key '{key}'")
        elif not isinstance(m[key], str):
            errors.append(f"manifest.json: '{key}' must be a string")
    for key in ["frame_count", "coverage_percent"]:
        if key not in m:
            errors.append(f"manifest.json: missing key '{key}'")
        elif not is_int(m[key]):
            errors.append(f"manifest.json: '{key}' must be an integer")

    scan_id = m.get("scan_id")
    if isinstance(scan_id, str):
        if not SCAN_ID_RE.match(scan_id):
            errors.append(f"manifest.json: scan_id {scan_id!r} must look like scan-YYYYMMDD-HHMMSS")
        elif SCAN_ID_RE.match(root.name) and root.name != scan_id:
            errors.append(f"manifest.json: scan_id {scan_id!r} does not match folder name {root.name!r}")
    if isinstance(m.get("preset"), str) and m["preset"] not in PRESETS:
        errors.append(f"manifest.json: preset must be one of {sorted(PRESETS)}, got {m['preset']!r}")
    created = m.get("created_utc")
    if isinstance(created, str):
        try:
            parsed = datetime.fromisoformat(created.replace("Z", "+00:00"))
            if parsed.tzinfo is None:
                errors.append("manifest.json: created_utc must include a UTC offset (e.g. 'Z')")
        except ValueError:
            errors.append(f"manifest.json: created_utc {created!r} is not ISO-8601")
    cov = m.get("coverage_percent")
    if is_int(cov) and not 0 <= cov <= 100:
        errors.append(f"manifest.json: coverage_percent must be 0-100, got {cov}")


def check_transform(label, mat, errors):
    try:
        a = np.array(mat, dtype=np.float64)
    except (ValueError, TypeError):
        errors.append(f"{label}: transform_matrix is not numeric")
        return
    if a.shape != (4, 4):
        errors.append(f"{label}: transform_matrix must be 4x4, got shape {a.shape}")
        return
    if not np.all(np.isfinite(a)):
        errors.append(f"{label}: transform_matrix contains non-finite values")
        return
    if not np.array_equal(a[3], [0, 0, 0, 1]):
        errors.append(f"{label}: transform_matrix bottom row must be [0, 0, 0, 1], got {a[3].tolist()}")
    r = a[:3, :3]
    dev = np.abs(r.T @ r - np.eye(3)).max()
    if dev >= ORTHO_TOL:
        errors.append(f"{label}: transform_matrix rotation is not orthonormal (max |RᵀR - I| = {dev:.2e})")


def check_npy(root, rel, dtype, shape, errors):
    path = root / rel
    if not path.is_file():
        errors.append(f"{rel}: missing")
        return
    try:
        with open(path, "rb") as f:
            version = np.lib.format.read_magic(f)
            if version == (1, 0):
                arr_shape, fortran, arr_dtype = np.lib.format.read_array_header_1_0(f)
            elif version == (2, 0):
                arr_shape, fortran, arr_dtype = np.lib.format.read_array_header_2_0(f)
            else:
                errors.append(f"{rel}: unsupported .npy version {version}")
                return
            data_start = f.tell()
    except (ValueError, OSError) as e:
        errors.append(f"{rel}: not a valid .npy file ({e})")
        return
    if version != (1, 0):
        errors.append(f"{rel}: .npy format version must be 1.0, got {version[0]}.{version[1]}")
    if fortran:
        errors.append(f"{rel}: must be C order (fortran_order False)")
    if arr_dtype.str != dtype:
        errors.append(f"{rel}: dtype must be {dtype}, got {arr_dtype.str}")
    if tuple(arr_shape) != shape:
        errors.append(f"{rel}: shape must be {shape} (depth_h, depth_w), got {tuple(arr_shape)}")
    expected_bytes = data_start + int(np.prod(arr_shape)) * arr_dtype.itemsize
    if path.stat().st_size != expected_bytes:
        errors.append(f"{rel}: file size {path.stat().st_size} does not match header ({expected_bytes} bytes)")


def check_frame(root, i, fr, errors):
    label = f"transforms.json frames[{i}]"
    if not isinstance(fr, dict):
        errors.append(f"{label}: must be an object")
        return
    ok = True
    for key in FRAME_STRINGS:
        if key not in fr:
            errors.append(f"{label}: missing key '{key}'"); ok = False
        elif not isinstance(fr[key], str):
            errors.append(f"{label}: '{key}' must be a string"); ok = False
    for key in FRAME_INTS:
        if key not in fr:
            errors.append(f"{label}: missing key '{key}'"); ok = False
        elif not is_int(fr[key]):
            errors.append(f"{label}: '{key}' must be an integer"); ok = False
    for key in FRAME_NUMBERS:
        if key not in fr:
            errors.append(f"{label}: missing key '{key}'"); ok = False
        elif not is_number(fr[key]):
            errors.append(f"{label}: '{key}' must be a number"); ok = False
    if "transform_matrix" not in fr:
        errors.append(f"{label}: missing key 'transform_matrix'")
    else:
        check_transform(label, fr["transform_matrix"], errors)
    if not ok:
        return

    if fr["image_source"] not in IMAGE_SOURCES:
        errors.append(f"{label}: image_source must be one of {sorted(IMAGE_SOURCES)}, got {fr['image_source']!r}")
    for key, folder, ext in [("file_path", "images", ".jpg"),
                             ("depth_file_path", "depth", ".npy"),
                             ("confidence_file_path", "confidence", ".npy")]:
        if not re.fullmatch(rf"{folder}/\d{{6}}\{ext}", fr[key]):
            errors.append(f"{label}: {key} must look like {folder}/NNNNNN{ext}, got {fr[key]!r}")

    img_rel = fr["file_path"]
    img_path = root / img_rel
    if not img_path.is_file():
        errors.append(f"{img_rel}: missing")
    else:
        try:
            with Image.open(img_path) as im:
                if im.format != "JPEG":
                    errors.append(f"{img_rel}: must be a JPEG, got {im.format}")
                if im.size != (fr["w"], fr["h"]):
                    errors.append(f"{img_rel}: size {im.size[0]}x{im.size[1]} does not match "
                                  f"frame w x h {fr['w']}x{fr['h']}")
        except OSError as e:
            errors.append(f"{img_rel}: cannot be read as an image ({e})")

    shape = (fr["depth_h"], fr["depth_w"])
    check_npy(root, fr["depth_file_path"], "<u2", shape, errors)
    check_npy(root, fr["confidence_file_path"], "|u1", shape, errors)


def check_transforms(t, root, errors):
    if t.get("camera_model") != "OPENCV":
        errors.append(f"transforms.json: camera_model must be 'OPENCV', got {t.get('camera_model')!r}")
    if t.get("ply_file_path") != "points3d.ply":
        errors.append(f"transforms.json: ply_file_path must be 'points3d.ply', got {t.get('ply_file_path')!r}")
    for key in ["k1", "k2", "p1", "p2"]:
        if key not in t:
            errors.append(f"transforms.json: missing key '{key}'")
        elif not is_number(t[key]) or t[key] != 0:
            errors.append(f"transforms.json: '{key}' must be 0, got {t[key]!r}")
    for key in ["fl_x", "fl_y", "cx", "cy", "w", "h"]:
        if key not in t:
            errors.append(f"transforms.json: missing key '{key}'")
        elif not is_number(t[key]) or (key in ("w", "h") and not is_int(t[key])):
            errors.append(f"transforms.json: '{key}' has the wrong type")

    frames = t.get("frames")
    if not isinstance(frames, list):
        errors.append("transforms.json: 'frames' must be a list")
        return None
    if not frames:
        errors.append("transforms.json: 'frames' is empty")
        return frames
    first = frames[0]
    if isinstance(first, dict):
        for key in ["fl_x", "fl_y", "cx", "cy", "w", "h"]:
            if key in t and key in first and t[key] != first[key]:
                errors.append(f"transforms.json: top-level '{key}' ({t[key]}) must equal the first frame's ({first[key]})")
    seen = set()
    for i, fr in enumerate(frames):
        check_frame(root, i, fr, errors)
        if isinstance(fr, dict) and isinstance(fr.get("file_path"), str):
            if fr["file_path"] in seen:
                errors.append(f"transforms.json frames[{i}]: duplicate file_path {fr['file_path']!r}")
            seen.add(fr["file_path"])
    return frames


def check_ply(root, errors):
    path = root / "points3d.ply"
    if not path.is_file():
        errors.append("points3d.ply: missing")
        return
    try:
        lines = path.read_text(encoding="ascii").splitlines()
    except UnicodeDecodeError:
        errors.append("points3d.ply: must be ASCII")
        return
    if not lines or lines[0].strip() != "ply":
        errors.append("points3d.ply: must start with 'ply'")
        return
    try:
        end = next(i for i, l in enumerate(lines) if l.strip() == "end_header")
    except StopIteration:
        errors.append("points3d.ply: missing end_header")
        return
    header = [l.split() for l in lines[1:end] if l.strip() and not l.startswith(("comment", "obj_info"))]
    if not header or header[0] != ["format", "ascii", "1.0"]:
        errors.append("points3d.ply: format must be 'ascii 1.0'")
    elements = [h for h in header if h[0] == "element"]
    if len(elements) != 1 or elements[0][1] != "vertex" or len(elements[0]) != 3 or not elements[0][2].isdigit():
        errors.append("points3d.ply: must have exactly one element 'vertex <count>'")
        return
    count = int(elements[0][2])
    props = [tuple(h[1:]) for h in header if h[0] == "property"]
    if props != PLY_PROPS:
        errors.append("points3d.ply: vertex properties must be float x y z, uchar red green blue")
        return
    body = [l for l in lines[end + 1:] if l.strip()]
    if len(body) != count:
        errors.append(f"points3d.ply: header says {count} vertices, found {len(body)}")
        return
    for n, line in enumerate(body):
        parts = line.split()
        try:
            if len(parts) != 6:
                raise ValueError
            [float(p) for p in parts[:3]]
            if not all(0 <= int(p) <= 255 for p in parts[3:]):
                raise ValueError
        except ValueError:
            errors.append(f"points3d.ply: vertex {n} is malformed: {line.strip()!r}")
            return


def validate_dir(root):
    errors = []
    manifest = load_json(root / "manifest.json", errors)
    transforms = load_json(root / "transforms.json", errors)
    if manifest is not None:
        check_manifest(manifest, root, errors)
    frames = check_transforms(transforms, root, errors) if transforms is not None else None
    check_ply(root, errors)
    if manifest is not None and frames is not None and is_int(manifest.get("frame_count")):
        if manifest["frame_count"] != len(frames):
            errors.append(f"manifest.json: frame_count is {manifest['frame_count']}, "
                          f"but transforms.json has {len(frames)} frames")
    return errors, (len(frames) if frames is not None else 0)


def find_root(extracted):
    if (extracted / "manifest.json").is_file():
        return extracted
    subdirs = [p for p in extracted.iterdir() if p.is_dir() and not p.name.startswith("__MACOSX")]
    if len(subdirs) == 1 and (subdirs[0] / "manifest.json").is_file():
        return subdirs[0]
    return extracted


def main(argv):
    if len(argv) != 2:
        print("usage: python validate.py <capture dir or zip>", file=sys.stderr)
        return 2
    target = Path(argv[1])
    if target.is_dir():
        errors, n = validate_dir(target)
    elif target.is_file() and zipfile.is_zipfile(target):
        with tempfile.TemporaryDirectory() as tmp:
            with zipfile.ZipFile(target) as zf:
                zf.extractall(tmp)
            errors, n = validate_dir(find_root(Path(tmp)))
    else:
        print(f"FAIL {target}: not a directory or zip file")
        return 1
    if errors:
        for e in errors:
            print(f"FAIL {e}")
        print(f"{len(errors)} problem(s) found")
        return 1
    print(f"OK {n} frames")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
