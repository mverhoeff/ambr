"""Write a deterministic synthetic capture package that passes validate.py.

Usage: python make_synthetic.py <out_dir>

24 frames on a circle of radius 1.0 m at height 0.3 m, all looking at the origin.
Images are 640x480 solid colour with the frame index drawn on them. Depth is a constant
1000 mm at 160x120 with confidence 255. points3d.ply holds 500 points on a 0.2 m sphere.
"""

import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

SEED = 1234
FRAME_COUNT = 24
RADIUS = 1.0
HEIGHT = 0.3
W, H = 640, 480
DEPTH_W, DEPTH_H = 160, 120
FX = FY = 500.0
CX, CY = 320.0, 240.0
DEPTH_MM = 1000
POINT_COUNT = 500
SPHERE_RADIUS = 0.2
SCAN_ID = "scan-20260101-120000"
CREATED_UTC = "2026-01-01T12:00:00Z"


def look_at_c2w_gl(eye, target, up=np.array([0, 1, 0.0])):
    """Camera-to-world matrix in the OpenGL convention (the camera looks down -Z)."""
    forward = target - eye
    forward /= np.linalg.norm(forward)
    right = np.cross(forward, up)
    right /= np.linalg.norm(right)
    true_up = np.cross(right, forward)
    c2w = np.eye(4)
    c2w[:3, 0], c2w[:3, 1], c2w[:3, 2] = right, true_up, -forward
    c2w[:3, 3] = eye
    return c2w


def write_image(path, index):
    hue = index / FRAME_COUNT
    colour = tuple(int(c) for c in 80 + 120 * (0.5 + 0.5 * np.cos(2 * np.pi * (hue + np.array([0, 1 / 3, 2 / 3])))))
    img = Image.new("RGB", (W, H), colour)
    draw = ImageDraw.Draw(img)
    font = ImageFont.load_default(size=160)
    draw.text((W / 2, H / 2), f"{index:02d}", fill=(255, 255, 255), font=font, anchor="mm")
    img.save(path, format="JPEG", quality=95)


def write_ply(path, rng):
    v = rng.normal(size=(POINT_COUNT, 3))
    v = SPHERE_RADIUS * v / np.linalg.norm(v, axis=1, keepdims=True)
    rgb = rng.integers(0, 256, size=(POINT_COUNT, 3))
    lines = [
        "ply",
        "format ascii 1.0",
        f"element vertex {POINT_COUNT}",
        "property float x",
        "property float y",
        "property float z",
        "property uchar red",
        "property uchar green",
        "property uchar blue",
        "end_header",
    ]
    lines += [f"{x:.6f} {y:.6f} {z:.6f} {r} {g} {b}" for (x, y, z), (r, g, b) in zip(v, rgb)]
    path.write_text("\n".join(lines) + "\n", encoding="ascii", newline="\n")


def main(argv):
    if len(argv) != 2:
        print("usage: python make_synthetic.py <out_dir>", file=sys.stderr)
        return 2
    out = Path(argv[1])
    for sub in ("images", "depth", "confidence"):
        (out / sub).mkdir(parents=True, exist_ok=True)
    rng = np.random.default_rng(SEED)

    depth = np.full((DEPTH_H, DEPTH_W), DEPTH_MM, dtype="<u2")
    confidence = np.full((DEPTH_H, DEPTH_W), 255, dtype="|u1")
    intrinsics = {"fl_x": FX, "fl_y": FY, "cx": CX, "cy": CY, "w": W, "h": H}

    frames = []
    for i in range(FRAME_COUNT):
        angle = 2 * np.pi * i / FRAME_COUNT
        eye = np.array([RADIUS * np.cos(angle), HEIGHT, RADIUS * np.sin(angle)])
        c2w = look_at_c2w_gl(eye, np.zeros(3))
        name = f"{i:06d}"
        write_image(out / "images" / f"{name}.jpg", i)
        np.save(out / "depth" / f"{name}.npy", depth)
        np.save(out / "confidence" / f"{name}.npy", confidence)
        frames.append({
            "file_path": f"images/{name}.jpg",
            "transform_matrix": [[round(float(x), 9) for x in row] for row in c2w],
            "depth_file_path": f"depth/{name}.npy",
            "confidence_file_path": f"confidence/{name}.npy",
            **intrinsics,
            "depth_w": DEPTH_W,
            "depth_h": DEPTH_H,
            "timestamp_ns": i * 33_333_333,
            "sharpness": 100.0,
            "image_source": "arcore_cpu",
        })

    transforms = {
        "camera_model": "OPENCV",
        **intrinsics,
        "k1": 0, "k2": 0, "p1": 0, "p2": 0,
        "ply_file_path": "points3d.ply",
        "frames": frames,
    }
    manifest = {
        "format": "ambr-capture",
        "version": 1,
        "scan_id": SCAN_ID,
        "preset": "object",
        "created_utc": CREATED_UTC,
        "device_model": "synthetic",
        "frame_count": FRAME_COUNT,
        "coordinate_system": "arcore_world_opengl",
        "units": "meters",
        "depth_units": "millimeters",
        "coverage_percent": 100,
        "app_version": "synthetic",
    }
    (out / "transforms.json").write_text(json.dumps(transforms, indent=2) + "\n", encoding="utf-8", newline="\n")
    (out / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8", newline="\n")
    write_ply(out / "points3d.ply", rng)
    print(f"wrote {FRAME_COUNT} frames to {out}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
