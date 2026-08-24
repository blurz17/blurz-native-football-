// written by bastiaan konings schuiling 2008 - 2014
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#ifndef _HPP_MANAGERS_ENVIRONMENT
#define _HPP_MANAGERS_ENVIRONMENT

#include "engine/defines.hpp"

#include "engine/core/types/singleton.hpp"
#include "engine/core/types/lockable.hpp"

#include <atomic>
#include <mutex>
#include <condition_variable>

namespace blunted {

  class EnvironmentManager : public Singleton<EnvironmentManager> {

    public:
      EnvironmentManager();
      virtual ~EnvironmentManager();

      void SignalQuit();
      bool GetQuit();

      unsigned long GetTime_ms();
      void Pause_ms(int duration);

      void SetPaused(bool paused);
      bool IsPaused() const;
      void WaitIfPaused();

    protected:
      Lockable<bool> quit;
      unsigned long startTime_ms;
      boost::posix_time::ptime startTime;

      std::atomic<bool> isPaused{false};
      std::mutex pauseMutex;
      std::condition_variable pauseCondition;
      boost::posix_time::ptime pauseStartTime;

  };

}

#endif
