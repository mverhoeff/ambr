#pragma once

namespace ambr {

/** Preset-dependent parameters, passed in from Preset.kt. */
struct PresetParams {
    float voxelSize;
    float maxDepthM;
    float targetPxPerCm;
    int minDirectionBins;
};

/** Live coverage state for one scan. Skeleton for now: holds the preset only. */
class CoverageEngine {
public:
    explicit CoverageEngine(const PresetParams& preset);

    void reset();

    const PresetParams& preset() const { return preset_; }

private:
    PresetParams preset_;
};

}  // namespace ambr
