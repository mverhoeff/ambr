package dev.ambr.capture

/**
 * Scene presets. Every preset-dependent number lives here (mirrored by `ambr_pipeline/presets.py`);
 * don't hard-code these values anywhere else.
 *
 * @property voxelSize coverage and on-device TSDF voxel edge, metres
 * @property maxDepthM depth beyond this is ignored, metres
 * @property keyframeStepM a new keyframe after this much camera translation, metres
 * @property keyframeStepDeg ...or after this much camera rotation, degrees
 * @property targetPxPerCm image resolution on the surface needed for "enough"
 * @property minDirectionBins distinct view directions needed for "enough"
 */
enum class Preset(
    val voxelSize: Float,
    val maxDepthM: Float,
    val keyframeStepM: Float,
    val keyframeStepDeg: Float,
    val targetPxPerCm: Float,
    val minDirectionBins: Int,
    val label: String,
    val icon: String,
) {
    OBJECT(0.02f, 1.5f, 0.05f, 10f, 10f, 4, "Object", "deployed_code"),
    ROOM(0.05f, 5.0f, 0.15f, 10f, 4f, 3, "Room", "chair"),
    OUTDOOR(0.20f, 8.0f, 0.40f, 10f, 1.5f, 3, "Outdoor", "landscape"),
}
