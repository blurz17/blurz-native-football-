// written by bastiaan konings schuiling 2008 - 2015
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#ifndef _HPP_MENU_LEAGUE_FORWARD
#define _HPP_MENU_LEAGUE_FORWARD

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

class LeagueForwardPage : public Gui2Page {

  public:
    LeagueForwardPage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LeagueForwardPage();

  protected:

};

#endif
