#include "profiler.hpp"
#include "log.hpp"
#include <algorithm>
#include <cstdio>
#include <fstream>

#ifdef __ANDROID__
#include <android/log.h>
#endif

namespace blunted {

  PerformanceProfiler::PerformanceProfiler() {
    hasLastSwapTime = false;
    frameTimeHistory.resize(120, 16.67f);
    historyIndex = 0;
    historyFull = false;

    lastRenderMs = 0.0f;
    lastGeometryMs = 0.0f;
    lastAmbientMs = 0.0f;
    lastLightingMs = 0.0f;
    lastPostprocessMs = 0.0f;
    lastSwapMs = 0.0f;

    lastCpuSkinningMs = 0.0f;
    lastActiveSkinned = 0;
    lastTotalPlayers = 0;

    lastPhysicsMs = 0.0f;

    instantFps = 60.0f;
    avgFps = 60.0f;
    avgFrameTimeMs = 16.67f;
    onePercentLowFps = 60.0f;
    minFps = 60.0f;
    maxFps = 60.0f;

    frameCounter = 0;
    lastLogTime = std::chrono::steady_clock::now();

    totalFrames = 0;
    sumFrameTimeMs = 0.0;
    sumRenderMs = 0.0;
    sumGeometryMs = 0.0;
    sumLightingMs = 0.0;
    sumPostprocessMs = 0.0;
    sumSwapMs = 0.0;
    sumSkinningMs = 0.0;
    sumPhysicsMs = 0.0;
    totalSpikes = 0;
  }

  PerformanceProfiler::~PerformanceProfiler() {
    DumpBenchmarkReport();
  }

  PerformanceProfiler &PerformanceProfiler::GetInstance() {
    static PerformanceProfiler instance;
    return instance;
  }

  void PerformanceProfiler::OnSwapBuffers() {
    auto now = std::chrono::steady_clock::now();

    std::lock_guard<std::mutex> lock(dataMutex);

    if (!hasLastSwapTime) {
      lastSwapTime = now;
      hasLastSwapTime = true;
      lastLogTime = now;
      return;
    }

    float frameTimeMs = std::chrono::duration<float, std::milli>(now - lastSwapTime).count();
    lastSwapTime = now;

    // Ignore extreme outliers from app pause or initial loading (> 500ms)
    if (frameTimeMs < 1.0f || frameTimeMs > 500.0f) {
      return;
    }

    instantFps = (frameTimeMs > 0.001f) ? (1000.0f / frameTimeMs) : 60.0f;

    // Record into rolling history
    frameTimeHistory[historyIndex] = frameTimeMs;
    historyIndex = (historyIndex + 1) % frameTimeHistory.size();
    if (historyIndex == 0) historyFull = true;

    // Compute rolling window stats
    size_t sampleCount = historyFull ? frameTimeHistory.size() : historyIndex;
    if (sampleCount > 0) {
      std::vector<float> sorted(frameTimeHistory.begin(), frameTimeHistory.begin() + sampleCount);
      std::sort(sorted.begin(), sorted.end());

      float sum = 0.0f;
      for (float t : sorted) sum += t;
      avgFrameTimeMs = sum / (float)sampleCount;
      avgFps = (avgFrameTimeMs > 0.001f) ? (1000.0f / avgFrameTimeMs) : 60.0f;

      // True 1% Low FPS = average of slowest 1% of frames in rolling window
      size_t lowCount = std::max((size_t)1, (size_t)(sampleCount * 0.01f));
      float lowSum = 0.0f;
      for (size_t k = sampleCount - lowCount; k < sampleCount; k++) {
        lowSum += sorted[k];
      }
      float avgLowFrameTime = lowSum / (float)lowCount;
      onePercentLowFps = (avgLowFrameTime > 0.001f) ? (1000.0f / avgLowFrameTime) : 60.0f;

      float maxFrameTime = sorted.back();
      float minFrameTime = sorted.front();
      minFps = (maxFrameTime > 0.001f) ? (1000.0f / maxFrameTime) : 60.0f;
      maxFps = (minFrameTime > 0.001f) ? (1000.0f / minFrameTime) : 60.0f;
    }

    // Cumulative session metrics
    totalFrames++;
    sumFrameTimeMs += frameTimeMs;
    sumRenderMs += lastRenderMs;
    sumGeometryMs += lastGeometryMs;
    sumLightingMs += lastLightingMs;
    sumPostprocessMs += lastPostprocessMs;
    sumSwapMs += lastSwapMs;
    sumSkinningMs += lastCpuSkinningMs;
    sumPhysicsMs += lastPhysicsMs;

    if (lifetimeSamples.size() < 10000) {
      lifetimeSamples.push_back(frameTimeMs);
    }

    // Spike detector: frame took longer than 33.33ms (dropped below 30 FPS)
    if (frameTimeMs > 33.33f) {
      totalSpikes++;
      char spikeMsg[256];
      snprintf(spikeMsg, sizeof(spikeMsg),
               "[PERF_SPIKE] Frame #%llu took %.1fms (%.1f FPS)! GPU: %.1fms (Geom: %.1fms, Light: %.1fms, Post: %.1fms) | CPU Skin: %.1fms (%d/%d pl) | Phys: %.1fms",
               (unsigned long long)totalFrames, frameTimeMs, instantFps,
               lastRenderMs, lastGeometryMs, lastLightingMs, lastPostprocessMs,
               lastCpuSkinningMs, lastActiveSkinned, lastTotalPlayers, lastPhysicsMs);
#ifdef __ANDROID__
      __android_log_print(ANDROID_LOG_WARN, "GameplayFootball", "%s", spikeMsg);
#endif
      printf("%s\n", spikeMsg);
    }

    // Periodic Logcat Output every ~1.0 second
    frameCounter++;
    float timeSinceLog = std::chrono::duration<float, std::milli>(now - lastLogTime).count();
    if (timeSinceLog >= 1000.0f) {
      lastLogTime = now;
      char logMsg[256];
      snprintf(logMsg, sizeof(logMsg),
               "[PERF] FPS: %.1f (1%%Low: %.1f, Min: %.1f) | Frame: %.1fms | GPU: %.1fms (G:%.1f L:%.1f P:%.1f) | CPU Skin: %.1fms (%d/%d pl) | Phys: %.1fms",
               avgFps, onePercentLowFps, minFps, avgFrameTimeMs,
               lastRenderMs, lastGeometryMs, lastLightingMs, lastPostprocessMs,
               lastCpuSkinningMs, lastActiveSkinned, lastTotalPlayers, lastPhysicsMs);
#ifdef __ANDROID__
      __android_log_print(ANDROID_LOG_INFO, "GameplayFootball", "%s", logMsg);
#endif
      printf("%s\n", logMsg);
    }
  }

  void PerformanceProfiler::RecordRenderTimes(float totalRenderMs, float geometryMs, float ambientMs, float lightingMs, float postprocessMs, float swapMs) {
    std::lock_guard<std::mutex> lock(dataMutex);
    lastRenderMs = totalRenderMs;
    lastGeometryMs = geometryMs;
    lastAmbientMs = ambientMs;
    lastLightingMs = lightingMs;
    lastPostprocessMs = postprocessMs;
    lastSwapMs = swapMs;
  }

  void PerformanceProfiler::RecordCpuSkinning(float skinningMs, int activeSkinned, int totalPlayers) {
    std::lock_guard<std::mutex> lock(dataMutex);
    lastCpuSkinningMs = skinningMs;
    lastActiveSkinned = activeSkinned;
    lastTotalPlayers = totalPlayers;
  }

  void PerformanceProfiler::RecordPhysics(float physicsMs) {
    std::lock_guard<std::mutex> lock(dataMutex);
    lastPhysicsMs = physicsMs;
  }

  std::string PerformanceProfiler::GetHudString() {
    std::lock_guard<std::mutex> lock(dataMutex);
    char buf[128];
    snprintf(buf, sizeof(buf), "FPS: %.0f [%.0f] | %.1fms (GPU:%.1f CPU:%.1f)",
             avgFps, onePercentLowFps, avgFrameTimeMs, lastRenderMs, lastCpuSkinningMs);
    return std::string(buf);
  }

  float PerformanceProfiler::GetCurrentFps() const {
    std::lock_guard<std::mutex> lock(dataMutex);
    return instantFps;
  }

  float PerformanceProfiler::GetAverageFps() const {
    std::lock_guard<std::mutex> lock(dataMutex);
    return avgFps;
  }

  float PerformanceProfiler::Get1PercentLowFps() const {
    std::lock_guard<std::mutex> lock(dataMutex);
    return onePercentLowFps;
  }

  int PerformanceProfiler::GetHudColorType() const {
    std::lock_guard<std::mutex> lock(dataMutex);
    if (avgFps >= 55.0f) return 0; // Green
    if (avgFps >= 30.0f) return 1; // Yellow
    return 2;                      // Red
  }

  void PerformanceProfiler::ResetBenchmark() {
    std::lock_guard<std::mutex> lock(dataMutex);
    totalFrames = 0;
    sumFrameTimeMs = 0.0;
    sumRenderMs = 0.0;
    sumGeometryMs = 0.0;
    sumLightingMs = 0.0;
    sumPostprocessMs = 0.0;
    sumSwapMs = 0.0;
    sumSkinningMs = 0.0;
    sumPhysicsMs = 0.0;
    totalSpikes = 0;
    lifetimeSamples.clear();
    lastLogTime = std::chrono::steady_clock::now();
    hasLastSwapTime = false;
    historyIndex = 0;
    historyFull = false;
    std::fill(frameTimeHistory.begin(), frameTimeHistory.end(), 16.67f);
    avgFps = 60.0f;
    onePercentLowFps = 60.0f;
    minFps = 60.0f;
    maxFps = 60.0f;
  }

  void PerformanceProfiler::DumpBenchmarkReport() {
    std::lock_guard<std::mutex> lock(dataMutex);
    if (totalFrames == 0) return;

    double avgFrameMs = sumFrameTimeMs / (double)totalFrames;
    double avgFpsVal = (avgFrameMs > 0.001) ? (1000.0 / avgFrameMs) : 0.0;
    double avgGpu = sumRenderMs / (double)totalFrames;
    double avgGeom = sumGeometryMs / (double)totalFrames;
    double avgLight = sumLightingMs / (double)totalFrames;
    double avgPost = sumPostprocessMs / (double)totalFrames;
    double avgSwap = sumSwapMs / (double)totalFrames;
    double avgSkin = sumSkinningMs / (double)totalFrames;
    double avgPhys = sumPhysicsMs / (double)totalFrames;

    float p1Low = avgFpsVal;
    float p01Low = avgFpsVal;
    if (!lifetimeSamples.empty()) {
      std::vector<float> sorted = lifetimeSamples;
      std::sort(sorted.begin(), sorted.end());
      size_t idx99 = (size_t)((double)sorted.size() * 0.99);
      if (idx99 >= sorted.size()) idx99 = sorted.size() - 1;
      p1Low = 1000.0f / sorted[idx99];

      size_t idx999 = (size_t)((double)sorted.size() * 0.999);
      if (idx999 >= sorted.size()) idx999 = sorted.size() - 1;
      p01Low = 1000.0f / sorted[idx999];
    }

    char report[2048];
    snprintf(report, sizeof(report),
      "\n========================== BENCHMARK REPORT ==========================\n"
      "  Total Measured Frames:  %llu\n"
      "  Average FPS:            %.1f FPS\n"
      "  1%% Low FPS:             %.1f FPS (Stutter Indicator)\n"
      "  0.1%% Low FPS:           %.1f FPS (Worst Hiccup)\n"
      "  Average Frame Time:     %.2f ms\n"
      "  Subsystem Breakdown:\n"
      "    - GPU Render Total:   %.2f ms (%.1f%% of frame budget)\n"
      "        * Geometry Pass:  %.2f ms\n"
      "        * Lighting Pass:  %.2f ms\n"
      "        * Postprocess:    %.2f ms\n"
      "        * Swap / VSync:   %.2f ms\n"
      "    - CPU Skinning:       %.2f ms (%.1f%% of frame budget)\n"
      "    - Physics / Game:     %.2f ms (%.1f%% of frame budget)\n"
      "  Total Stutter Spikes:   %llu frames (%.1f%% of total)\n"
      "======================================================================\n",
      (unsigned long long)totalFrames,
      avgFpsVal, p1Low, p01Low, avgFrameMs,
      avgGpu, (avgGpu / avgFrameMs) * 100.0,
      avgGeom, avgLight, avgPost, avgSwap,
      avgSkin, (avgSkin / avgFrameMs) * 100.0,
      avgPhys, (avgPhys / avgFrameMs) * 100.0,
      (unsigned long long)totalSpikes,
      ((double)totalSpikes / (double)totalFrames) * 100.0);

#ifdef __ANDROID__
    __android_log_print(ANDROID_LOG_INFO, "GameplayFootball", "%s", report);
#endif
    printf("%s", report);

    std::ofstream outFile("benchmark_report.txt", std::ios::out | std::ios::app);
    if (outFile.is_open()) {
      outFile << report;
      outFile.close();
    }
  }

}
