// written by bastiaan konings schuiling 2008 - 2015
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#ifndef _HPP_MENU_INGAME
#define _HPP_MENU_INGAME

#include "engine/ui/windowmanager.hpp"

#include "game/menu/cameramenu.hpp"

#include "engine/ui/widgets/menu.hpp"
#include "engine/ui/widgets/root.hpp"
#include "engine/ui/widgets/grid.hpp"
#include "engine/ui/widgets/button.hpp"

#include "game/match/match.hpp"

using namespace blunted;

class IngamePage : public Gui2Page {

  public:
    IngamePage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~IngamePage();

    void GoResume();
    void GoGamePlan();
    void GoControllerSelect();
    void GoCameraSettings();
    void GoVisualOptions();
    void GoSystemSettings();
    void GoReplay();
    void GoPreQuit();

    virtual void ProcessWindowingEvent(WindowingEvent *event);

  protected:
    int teamID; // team that activated the ingame menu

};


class PreQuitPage : public Gui2Page {

  public:
    PreQuitPage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~PreQuitPage();

    void GoMenu();

  protected:

};

#endif
