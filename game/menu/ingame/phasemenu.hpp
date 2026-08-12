// written by bastiaan konings schuiling 2008 - 2015
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#ifndef _HPP_MENU_PHASE
#define _HPP_MENU_PHASE

#include "engine/ui/windowmanager.hpp"

#include "engine/ui/page.hpp"
#include "engine/ui/widgets/root.hpp"
#include "engine/ui/widgets/grid.hpp"
#include "engine/ui/widgets/button.hpp"

#include "game/match/match.hpp"

#include "engine/gamedefines.hpp"

using namespace blunted;

class MatchPhasePage : public Gui2Page {

  public:
    MatchPhasePage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~MatchPhasePage();

    void GoGamePlan();

    Gui2Button *buttonNext;

    void ContinueGame();
    virtual void ProcessWindowingEvent(WindowingEvent *event);

  protected:
    Gui2Grid *grid;
    e_MatchPhase nextPhase;

};

#endif
