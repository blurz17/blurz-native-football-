// written by bastiaan konings schuiling 2008 - 2015
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#ifndef _HPP_MENU_CONTROLLERSELECT
#define _HPP_MENU_CONTROLLERSELECT

#include "engine/ui/windowmanager.hpp"

#include "engine/ui/widgets/menu.hpp"
#include "engine/ui/widgets/root.hpp"
#include "engine/ui/widgets/grid.hpp"
#include "engine/ui/widgets/button.hpp"
#include "engine/ui/widgets/image.hpp"

#include "game/match/match.hpp"

using namespace blunted;

class ControllerSelectPage : public Gui2Page {

  public:
    ControllerSelectPage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~ControllerSelectPage();

    void SetImagePositions();

    virtual void Process();
    virtual void ProcessKeyboardEvent(KeyboardEvent *event);
    virtual void ProcessJoystickEvent(JoystickEvent *event);
    virtual void ProcessWindowingEvent(WindowingEvent *event);

  protected:
    std::vector<SideSelection> sides;
    std::vector<unsigned long> delay;
    bool inGame;

};

#endif
