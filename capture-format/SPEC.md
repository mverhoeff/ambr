# ambr capture package, version 1

A capture package is the only contract between the phone and the PC. It is a folder, or a `.zip` of that folder, produced by the app and consumed by `pc-pipeline`. `validate.py` checks every rule in this document.

## Layout

```
<scan_id>/                      scan_id = "scan-YYYYMMDD-HHMMSS"
  manifest.json
  transforms.json
  images/000000.jpg ...         JPEG q95, CPU image, sensor-native (landscape) orientation, never rotated
  depth/000000.npy ...          uint16, millimetres, 0 = invalid, depth-image resolution
  confidence/000000.npy ...     uint8 0..255, same size as depth
  points3d.ply                  ASCII PLY, vertex x y z (float, metres) + red green blue (uchar)
  recording.mp4                 optional, ARCore session recording
  preview/mesh.ply              optional, written by the app's on-device preview
  preview/splat.ply             optional, written by the app's on-device preview
```

Frame files are numbered from `000000` with six digits, in capture order. Image, depth and confidence files for one frame share the same number.

A `.zip` package contains the files either at the archive root or inside a single top-level `<scan_id>/` folder.

## Coordinate system

- The world frame is ARCore's world frame: gravity-aligned, +Y up, metres. It is fixed for the whole session but has an arbitrary origin and heading.
- Camera poses are **camera-to-world** in the **OpenGL camera convention**: +X right, +Y up, the camera looks down **−Z**.
- Intrinsics describe the JPEG exactly as stored (sensor-native orientation, pixel units, origin at the top-left pixel corner).

## `manifest.json`

| key | type | value |
|---|---|---|
| `format` | string | `"ambr-capture"` |
| `version` | int | `1` |
| `scan_id` | string | `scan-YYYYMMDD-HHMMSS`, equal to the folder name when the package is a folder named like a scan |
| `preset` | string | `"object"`, `"room"` or `"outdoor"` |
| `created_utc` | string | ISO-8601 timestamp with a UTC offset, e.g. `2026-11-07T12:00:00Z` |
| `device_model` | string | e.g. `"SM-S948B"` |
| `frame_count` | int | number of entries in `transforms.json` `frames` |
| `coordinate_system` | string | `"arcore_world_opengl"` |
| `units` | string | `"meters"` |
| `depth_units` | string | `"millimeters"` |
| `coverage_percent` | int | 0–100 |
| `app_version` | string | app `versionName` |

## `transforms.json`

The [nerfstudio](https://docs.nerf.studio/quickstart/data_conventions.html) dataset format, with extra per-frame keys.

Top level:

| key | type | value |
|---|---|---|
| `camera_model` | string | `"OPENCV"` |
| `fl_x`, `fl_y`, `cx`, `cy` | number | intrinsics of the first frame |
| `w`, `h` | int | image size of the first frame |
| `k1`, `k2`, `p1`, `p2` | number | `0` (images are undistorted) |
| `ply_file_path` | string | `"points3d.ply"` |
| `frames` | list | one object per frame, in capture order |

Each frame:

| key | type | value |
|---|---|---|
| `file_path` | string | `images/NNNNNN.jpg` |
| `transform_matrix` | 4×4 list of lists | camera-to-world, row-major, OpenGL convention; bottom row `[0, 0, 0, 1]`, rotation orthonormal |
| `depth_file_path` | string | `depth/NNNNNN.npy` |
| `confidence_file_path` | string | `confidence/NNNNNN.npy` |
| `fl_x`, `fl_y`, `cx`, `cy` | number | this frame's intrinsics |
| `w`, `h` | int | this frame's image size; the JPEG must have exactly this size |
| `depth_w`, `depth_h` | int | depth and confidence array size |
| `timestamp_ns` | int | ARCore frame timestamp, nanoseconds |
| `sharpness` | number | image sharpness score (higher is sharper) |
| `image_source` | string | `"arcore_cpu"` |

Per-frame intrinsics and `image_source` exist so that other image sources with different resolutions can be added in later versions.

## Depth and confidence arrays

- NumPy `.npy` files, format version 1.0, C order (`fortran_order: False`), little-endian.
- Depth: dtype `<u2`, shape `(depth_h, depth_w)`, millimetres, `0` = no valid depth.
- Confidence: dtype `|u1`, shape `(depth_h, depth_w)`, 0 (lowest) to 255 (highest).
- Both arrays use the same orientation as the JPEG (sensor-native).

## `points3d.ply`

ASCII PLY (`format ascii 1.0`) with one `vertex` element whose properties are, in order: `float x`, `float y`, `float z` (world frame, metres) and `uchar red`, `uchar green`, `uchar blue`. It may contain zero vertices.
