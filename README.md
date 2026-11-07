# ambr

Capture scenes on Android for 3D Gaussian splats and textured meshes, then process them on a PC.

- `android/` — Kotlin + Jetpack Compose + ARCore app, with a C++ (NDK) voxel engine
- `capture-format/` — capture package spec, validator and synthetic test capture
- `pc-pipeline/` — Python pipeline (WSL2 + CUDA): gsplat training and Open3D mesh extraction
- `docs/` — setup notes
