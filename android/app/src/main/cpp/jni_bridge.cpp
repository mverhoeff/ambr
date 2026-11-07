#include <android/log.h>
#include <jni.h>

#include "coverage_engine.h"

#define LOG_TAG "AmbrNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace {

constexpr const char* kVersion = "ambr-native 1";

ambr::CoverageEngine* fromHandle(jlong handle) {
    return reinterpret_cast<ambr::CoverageEngine*>(handle);
}

}  // namespace

extern "C" {

JNIEXPORT jlong JNICALL Java_dev_ambr_coverage_CoverageEngine_nativeCreate(
    JNIEnv*, jclass, jfloat voxelSize, jfloat maxDepthM, jfloat targetPxPerCm, jint minDirectionBins) {
    ambr::PresetParams preset{voxelSize, maxDepthM, targetPxPerCm, minDirectionBins};
    auto* engine = new ambr::CoverageEngine(preset);
    LOGI("engine created: voxel %.3f m, max depth %.1f m", voxelSize, maxDepthM);
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT void JNICALL Java_dev_ambr_coverage_CoverageEngine_nativeReset(JNIEnv*, jclass, jlong handle) {
    if (handle != 0) fromHandle(handle)->reset();
}

JNIEXPORT void JNICALL Java_dev_ambr_coverage_CoverageEngine_nativeDestroy(JNIEnv*, jclass, jlong handle) {
    delete fromHandle(handle);
}

JNIEXPORT jstring JNICALL Java_dev_ambr_coverage_CoverageEngine_nativeVersion(JNIEnv* env, jclass) {
    return env->NewStringUTF(kVersion);
}

}  // extern "C"
