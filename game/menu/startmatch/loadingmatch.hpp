// written by bastiaan konings schuiling 2008 - 2015
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#ifndef _HPP_MENU_LOADINGMATCH
#define _HPP_MENU_LOADINGMATCH

#include "engine/ui/windowmanager.hpp"

#include "engine/ui/widgets/menu.hpp"
#include "engine/ui/widgets/root.hpp"
#include "engine/ui/widgets/grid.hpp"
#include "engine/ui/widgets/image.hpp"

#include "game/match/match.hpp"

using namespace blunted;

class LoadingMatchPage : public Gui2Page {

  public:
    LoadingMatchPage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~LoadingMatchPage();

    virtual void Process();

    void Close();

  protected:
    bool sentStartGameSignal;

};

#endif
