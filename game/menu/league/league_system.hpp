// written by bastiaan konings schuiling 2008 - 2015
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#ifndef _HPP_MENU_LEAGUE_SYSTEM
#define _HPP_MENU_LEAGUE_SYSTEM

#include "engine/ui/windowmanager.hpp"

#include "engine/ui/page.hpp"
#include "engine/ui/widgets/root.hpp"
#include "engine/ui/widgets/image.hpp"
#include "engine/ui/widgets/button.hpp"
#include "engine/ui/widgets/slider.hpp"
#include "engine/ui/widgets/grid.hpp"
#include "engine/ui/widgets/caption.hpp"
#include "engine/ui/widgets/capturekey.hpp"

using namespace blunted;

class LeagueSystemPage : public Gui2Page {

  public:
    LeagueSystemPage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LeagueSystemPage();

  protected:

};

class LeagueSystemSavePage : public Gui2Page {

  public:
    LeagueSystemSavePage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LeagueSystemSavePage();

  protected:

};

class LeagueSystemSettingsPage : public Gui2Page {

  public:
    LeagueSystemSettingsPage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LeagueSystemSettingsPage();

  protected:

};

#endif
