package dev.ambr.coverage

import dev.ambr.capture.Preset

/** Kotlin side of the native coverage engine. Call [destroy] when the scan ends. */
class CoverageEngine private constructor(
    val preset: Preset,
    private var handle: Long,
) {
    fun reset() {
        check(handle != 0L) { "engine destroyed" }
        nativeReset(handle)
    }

    fun destroy() {
        if (handle != 0L) {
            nativeDestroy(handle)
            handle = 0L
        }
    }

    companion object {
        init {
            System.loadLibrary("ambr_native")
        }

        fun create(preset: Preset): CoverageEngine = CoverageEngine(
            preset,
            nativeCreate(preset.voxelSize, preset.maxDepthM, preset.targetPxPerCm, preset.minDirectionBins),
        )

        fun version(): String = nativeVersion()

        @JvmStatic private external fun nativeCreate(
            voxelSize: Float,
            maxDepthM: Float,
            targetPxPerCm: Float,
            minDirectionBins: Int,
        ): Long

        @JvmStatic private external fun nativeReset(handle: Long)

        @JvmStatic private external fun nativeDestroy(handle: Long)

        @JvmStatic private external fun nativeVersion(): String
    }
}
