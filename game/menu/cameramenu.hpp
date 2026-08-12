// written by bastiaan konings schuiling 2008 - 2015
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#ifndef _HPP_MENU_INGAME_CAMERA
#define _HPP_MENU_INGAME_CAMERA

#include "engine/ui/windowmanager.hpp"

#include "engine/ui/page.hpp"
#include "engine/ui/widgets/root.hpp"
#include "engine/ui/widgets/grid.hpp"
#include "engine/ui/widgets/button.hpp"
#include "engine/ui/widgets/slider.hpp"

using namespace blunted;

class CameraPage : public Gui2Page {

  public:
    CameraPage(Gui2WindowManager *windowManager, const Gui2PageData &pageData);
    virtual ~CameraPage();

    void OnClose();

    void UpdateCamera();

  protected:
    Gui2Slider *sliderZoom;
    Gui2Slider *sliderHeight;
    Gui2Slider *sliderFOV;
    Gui2Slider *sliderAngleFactor;

};

#endif
