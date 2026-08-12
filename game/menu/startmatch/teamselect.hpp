// written by bastiaan konings schuiling 2008 - 2015
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#ifndef _HPP_MENU_TEAMSELECT
#define _HPP_MENU_TEAMSELECT

#include "engine/ui/windowmanager.hpp"

#include "engine/ui/widgets/menu.hpp"
#include "engine/ui/widgets/root.hpp"
#include "engine/ui/widgets/grid.hpp"
#include "engine/ui/widgets/button.hpp"
#include "engine/ui/widgets/image.hpp"
#include "engine/ui/widgets/slider.hpp"
#include "engine/ui/widgets/iconselector.hpp"

#include "game/match/match.hpp"

using namespace blunted;

class TeamSelectPage : public Gui2Page {

  public:
    TeamSelectPage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~TeamSelectPage();

    void FocusTeamSelect1();
    void FocusStart1();
    void FocusCompetitionSelect2();
    void FocusTeamSelect2();
    void FocusStart2();
    void SetupTeamSelect1();
    void SetupTeamSelect2();
    void GoOptionsMenu();

    virtual void ProcessWindowingEvent(WindowingEvent *event);

    Gui2Button *buttonStart1;
    Gui2Button *buttonStart2;
    Gui2Caption *p2;
    Gui2Grid *grid2;
    Gui2Image *bg2;

  protected:
    Gui2IconSelector *teamSelect1;
    Gui2IconSelector *teamSelect2;
    Gui2IconSelector *competitionSelect1;
    Gui2IconSelector *competitionSelect2;

};

#endif
