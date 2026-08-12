// written by bastiaan konings schuiling 2008 - 2015
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#ifndef _HPP_MENU_INGAME_VISUALOPTIONS
#define _HPP_MENU_INGAME_VISUALOPTIONS

#include "engine/ui/windowmanager.hpp"

#include "engine/ui/page.hpp"
#include "engine/ui/widgets/root.hpp"
#include "engine/ui/widgets/grid.hpp"
#include "engine/ui/widgets/button.hpp"
#include "engine/ui/widgets/slider.hpp"
#include "engine/ui/widgets/pulldown.hpp"

using namespace blunted;

class VisualOptionsPage : public Gui2Page {

  public:
    VisualOptionsPage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~VisualOptionsPage();

  protected:
    void OnRandomizeSun();
    void OnChangeKit(Gui2Pulldown *pulldown);

    Gui2Pulldown *kitSelectionPulldown[2];

};

#endif
