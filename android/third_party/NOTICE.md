# Third-party notices

## ARCore SDK samples: samplerender

- Source: https://github.com/google-ar/arcore-android-sdk, `samples/hello_ar_kotlin/app/src/main/`
- Version: ARCore Android SDK v1.57.0, commit `204ddfd1715eb0cb775bf7528c9aee9d93cd4d4a`
- Licence: Apache License 2.0 (`licenses/arcore-android-sdk-LICENSE.txt`)
- Copied into `app/src/main/java/dev/ambr/ar/samplerender/`:
  `SampleRender`, `Framebuffer`, `Mesh`, `Shader`, `Texture`, `VertexBuffer`, `IndexBuffer`, `GpuBuffer`, `GLError`, `arcore/BackgroundRenderer`
- Copied into `app/src/main/assets/shaders/`: `background_show_camera.vert`, `background_show_camera.frag`
- Changes:
  - Package renamed from `com.google.ar.core.examples.java.common.samplerender` to `dev.ambr.ar.samplerender`.
  - `Mesh.createFromAsset` (Wavefront OBJ loading) and its imports removed, to avoid the `de.javagl:obj` dependency.

## Fonts

Bundled in `app/src/main/res/font/`, with licences in `app/src/main/assets/licenses/`:

| Font | Source | Licence |
|---|---|---|
| Manrope Medium, Bold (v4.504) | https://github.com/googlefonts/manrope, commit `6f81ebecdf65e4463b798cc07b16a4f8d5216917` | SIL Open Font License 1.1 |
| IBM Plex Mono Regular, Medium | https://github.com/google/fonts, `ofl/ibmplexmono` | SIL Open Font License 1.1 |
| Material Symbols Rounded (variable) | https://github.com/google/material-design-icons, `variablefont/` | Apache License 2.0 |
