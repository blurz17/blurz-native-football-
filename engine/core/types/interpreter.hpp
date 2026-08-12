// written by bastiaan konings schuiling 2008 - 2014
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#ifndef _HPP_INTERPRETER
#define _HPP_INTERPRETER

#include "engine/core/types/observer.hpp"

#include "engine/framework/systems/isystem.hpp"

#include "engine/core/log.hpp"

namespace blunted {

  class Interpreter : public Observer {

    public:
      virtual e_SystemType GetSystemType() const = 0;
      virtual void OnSynchronize() {
        Log(e_FatalError, "Interpreter", "OnSynchronize", "OnSynchronize not written yet for this object! N00B!");
      }

    protected:

  };

}

#endif
