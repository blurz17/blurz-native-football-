#ifndef _HPP_BASE_PROFILER
#define _HPP_BASE_PROFILER

#include <chrono>
#include <vector>
#include <string>
#include <mutex>
#include <cstdint>

namespace blunted {

  struct ScopedTimer {
    std::chrono::steady_clock::time_point start;
    float *outMs;
    ScopedTimer(float *target) : outMs(target), start(std::chrono::steady_clock::now()) {}
    ~ScopedTimer() {
      auto end = std::chrono::steady_clock::now();
      if (outMs) {
        *outMs = std::chrono::duration<float, std::milli>(end - start).count();
      }
    }
  };

  class PerformanceProfiler {
    public:
      static PerformanceProfiler &GetInstance();

      // Called from OpenGL thread on buffer swap
      void OnSwapBuffers();

      // Subsystem timing hooks
      void RecordRenderTimes(float totalRenderMs, float geometryMs, float ambientMs, float lightingMs, float postprocessMs, float swapMs);
      void RecordSwap(float swapMs);
      void RecordCpuSkinning(float skinningMs, int activeSkinned, int totalPlayers);
      void RecordPhysics(float physicsMs);

      // In-Game Dynamic HUD display strings
      std::string GetHudString();
      float GetCurrentFps() const;
      float GetAverageFps() const;
      float Get1PercentLowFps() const;
      int GetHudColorType() const; // 0 = Green (>=55), 1 = Yellow (30-54), 2 = Red (<30)

      // Static session summary & benchmarking
      void DumpBenchmarkReport();
      void ResetBenchmark();

    private:
      PerformanceProfiler();
      ~PerformanceProfiler();

      mutable std::mutex dataMutex;

      // Frame timing
      std::chrono::steady_clock::time_point lastSwapTime;
      bool hasLastSwapTime;

      // Rolling window (last 120 frames for smooth stats)
      std::vector<float> frameTimeHistory;
      size_t historyIndex;
      bool historyFull;

      // Subsystem metrics (most recent frame)
      float lastRenderMs;
      float lastGeometryMs;
      float lastAmbientMs;
      float lastLightingMs;
      float lastPostprocessMs;
      float lastSwapMs;

      float lastCpuSkinningMs;
      int lastActiveSkinned;
      int lastTotalPlayers;

      float lastPhysicsMs;

      // Computed metrics
      float instantFps;
      float avgFps;
      float avgFrameTimeMs;
      float onePercentLowFps;
      float minFps;
      float maxFps;

      // Periodic logging
      uint64_t frameCounter;
      std::chrono::steady_clock::time_point lastLogTime;

      // Lifetime session stats for static benchmarking
      uint64_t totalFrames;
      double sumFrameTimeMs;
      double sumRenderMs;
      double sumGeometryMs;
      double sumLightingMs;
      double sumPostprocessMs;
      double sumSwapMs;
      double sumSkinningMs;
      double sumPhysicsMs;
      uint64_t totalSpikes; // frames taking > 33.33ms (<30 FPS)
      std::vector<float> lifetimeSamples; // capped sample list for accurate percentiles
  };

}

#endif
