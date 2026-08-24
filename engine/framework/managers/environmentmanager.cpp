// written by bastiaan konings schuiling 2008 - 2014
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#include "environmentmanager.hpp"

#ifndef GPFB_HEADLESS
#include <SDL2/SDL.h>
#endif
#include <boost/thread.hpp>
#include "../../core/log.hpp"
#include "../../core/utils.hpp"

namespace blunted {

  template<> EnvironmentManager* Singleton<EnvironmentManager>::singleton = 0;

  EnvironmentManager::EnvironmentManager() {
    quit.SetData(false);
    startTime = boost::posix_time::microsec_clock::local_time();
  };

  EnvironmentManager::~EnvironmentManager() {
  };

  unsigned long EnvironmentManager::GetTime_ms() {
    boost::posix_time::ptime now;
    if (isPaused.load()) {
        now = pauseStartTime;
    } else {
        now = boost::posix_time::microsec_clock::local_time();
    }
    boost::posix_time::time_duration msdiff = now - startTime;
    return msdiff.total_milliseconds();
  }

  void EnvironmentManager::Pause_ms(int duration) {
    boost::this_thread::sleep(boost::posix_time::milliseconds(duration));
  }

  void EnvironmentManager::SignalQuit() {
    quit.SetData(true);
    pauseCondition.notify_all(); // Wake up any sleeping threads if they are paused
  }

  bool EnvironmentManager::GetQuit() {
    return quit.GetData();
  }

  void EnvironmentManager::SetPaused(bool paused) {
    if (paused && !isPaused.load()) {
      pauseStartTime = boost::posix_time::microsec_clock::local_time();
      Log(e_Notice, "EnvironmentManager", "SetPaused", "Engine paused.");
    } else if (!paused && isPaused.load()) {
      boost::posix_time::ptime now = boost::posix_time::microsec_clock::local_time();
      auto pauseDuration = now - pauseStartTime;
      startTime += pauseDuration;
      
      long long ms = std::chrono::duration_cast<std::chrono::milliseconds>(pauseDuration).count();
      Log(e_Notice, "EnvironmentManager", "SetPaused", "Engine resumed. pauseDuration=" + int_to_str((int)ms) + "ms");
    }
    
    // Add lock to ensure visibility and prevent any weird subtle races
    {
      std::lock_guard<std::mutex> lock(pauseMutex);
      isPaused.store(paused);
    }
    
    if (!paused) {
      pauseCondition.notify_all();
    }
  }

  bool EnvironmentManager::IsPaused() const {
    return isPaused.load();
  }

  void EnvironmentManager::WaitIfPaused() {
    if (isPaused.load()) {
      std::unique_lock<std::mutex> lock(pauseMutex);
      pauseCondition.wait(lock, [this]{ return !isPaused.load() || quit.GetData(); });
    }
  }

}
