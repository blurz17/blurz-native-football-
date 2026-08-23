// written by bastiaan konings schuiling 2008 - 2015
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#include "ingame.hpp"

#include "engine/main.hpp"
#include "game/menu/gameplan.hpp"
#include "game/menu/controllerselect.hpp"
#include "game/menu/pagefactory.hpp"

#include "replaymenu.hpp"

#include "game/menu/settings.hpp"

using namespace blunted;

IngamePage::IngamePage(Gui2WindowManager *windowManager, const Gui2PageData &pageData) : Gui2Page(windowManager, pageData) {

  teamID = pageData.properties->GetInt("teamID", 0);

  GetGameTask()->GetMatch()->Pause(true);

  Gui2Root *root = windowManager->GetRoot();

  // Full screen semi-transparent overlay
  Gui2Image *overlayBg = new Gui2Image(windowManager, "image_ingame_overlay", 0, 0, 100, 100);
  overlayBg->LoadImage("media/menu/backgrounds/black.png");
  // Assuming Gui2View has some alpha/color setting; for now it just draws the image.
  // Actually, we can rely on the image's own alpha or just leave it.
  this->AddView(overlayBg);
  overlayBg->Show();

  // Modal Background
  float modalW = 40;
  float modalH = 65;
  float modalX = 50 - modalW/2;
  float modalY = 50 - modalH/2;
  Gui2Image *modalBg = new Gui2Image(windowManager, "image_ingame_modalbg", modalX, modalY, modalW, modalH);
  modalBg->LoadImage("media/menu/scoreboard_bg.png"); // Re-use modern bg
  this->AddView(modalBg);
  modalBg->Show();

  // Menu Title
  Gui2Caption *title = new Gui2Caption(windowManager, "caption_ingame_title", modalX, modalY + 2, modalW, 5, "PAUSE MENU");
  title->SetColor(Vector3(20, 230, 160)); // FC Mobile Neon Teal
  this->AddView(title);
  title->Show();

  // Buttons
  float btnW = modalW * 0.8f;
  float btnH = 4;
  Gui2Button *buttonResume = new Gui2Button(windowManager, "button_resume", 0, 0, btnW, btnH, "RESUME MATCH");
  Gui2Button *buttonGamePlan = new Gui2Button(windowManager, "button_gameplan", 0, 0, btnW, btnH, "GAME PLAN");
  Gui2Button *buttonControllerSelect = new Gui2Button(windowManager, "button_controllerselect", 0, 0, btnW, btnH, "CONTROLLERS");
  Gui2Button *buttonCameraSettings = new Gui2Button(windowManager, "button_camerasettings", 0, 0, btnW, btnH, "CAMERA");
  Gui2Button *buttonVisualOptions = new Gui2Button(windowManager, "button_visualoptions", 0, 0, btnW, btnH, "VISUALS");
  Gui2Button *buttonSystemSettings = new Gui2Button(windowManager, "button_systemsettings", 0, 0, btnW, btnH, "SETTINGS");
  Gui2Button *buttonReplay = new Gui2Button(windowManager, "button_replay", 0, 0, btnW, btnH, "REPLAY");
  Gui2Button *buttonPreQuit = new Gui2Button(windowManager, "button_quit", 0, 0, btnW, btnH, "FORFEIT MATCH");

  buttonResume->sig_OnClick.connect(boost::bind(&IngamePage::GoResume, this));
  buttonGamePlan->sig_OnClick.connect(boost::bind(&IngamePage::GoGamePlan, this));
  buttonControllerSelect->sig_OnClick.connect(boost::bind(&IngamePage::GoControllerSelect, this));
  buttonCameraSettings->sig_OnClick.connect(boost::bind(&IngamePage::GoCameraSettings, this));
  buttonVisualOptions->sig_OnClick.connect(boost::bind(&IngamePage::GoVisualOptions, this));
  buttonSystemSettings->sig_OnClick.connect(boost::bind(&IngamePage::GoSystemSettings, this));
  buttonReplay->sig_OnClick.connect(boost::bind(&IngamePage::GoReplay, this));
  buttonPreQuit->sig_OnClick.connect(boost::bind(&IngamePage::GoPreQuit, this));

  Gui2Grid *grid = new Gui2Grid(windowManager, "grid", modalX + (modalW - btnW)/2, modalY + 10, btnW, modalH - 12);

  grid->AddView(buttonResume, 0, 0);
  grid->AddView(buttonGamePlan, 1, 0);
  grid->AddView(buttonControllerSelect, 2, 0);
  grid->AddView(buttonCameraSettings, 3, 0);
  grid->AddView(buttonVisualOptions, 4, 0);
  grid->AddView(buttonSystemSettings, 5, 0);
  grid->AddView(buttonReplay, 6, 0);
  grid->AddView(buttonPreQuit, 7, 0);

  grid->UpdateLayout(0.5);

  this->AddView(grid);
  grid->Show();

  buttonResume->SetFocus();

  this->Show();
}

IngamePage::~IngamePage() {
}

void IngamePage::GoResume() {
  GetMenuTask()->ReleaseAllButtons();
  GetGameTask()->GetMatch()->Pause(false);
  this->Exit();
  delete this;
}

void IngamePage::GoGamePlan() {
  Properties properties;
  properties.Set("teamID", teamID);
  CreatePage(e_PageID_GamePlan, properties);
}

void IngamePage::GoControllerSelect() {
  Properties properties;
  properties.SetBool("isInGame", true);
  CreatePage(e_PageID_ControllerSelect, properties);
}

void IngamePage::GoCameraSettings() {
  CreatePage(e_PageID_Camera);
}

void IngamePage::GoVisualOptions() {
  CreatePage(e_PageID_VisualOptions);
}

void IngamePage::GoSystemSettings() {
  CreatePage(e_PageID_Settings);
}

void IngamePage::GoReplay() {
  CreatePage(e_PageID_Replay);
}

void IngamePage::GoPreQuit() {
  CreatePage(e_PageID_PreQuit);
}


void IngamePage::ProcessWindowingEvent(WindowingEvent *event) {
  if (event->IsEscape()) {
    GetMenuTask()->ReleaseAllButtons();
    GetGameTask()->GetMatch()->Pause(false);
  }
  Gui2Page::ProcessWindowingEvent(event);
}



PreQuitPage::PreQuitPage(Gui2WindowManager *windowManager, const Gui2PageData &pageData) : Gui2Page(windowManager, pageData) {
  Gui2Root *root = windowManager->GetRoot();
  Gui2Image *bg = new Gui2Image(windowManager, "image_prequit_bg", 30, 42.5, 40, 15);
  bg->LoadImage("media/menu/backgrounds/black.png");
  this->AddView(bg);

  Gui2Caption *restartCaption = new Gui2Caption(windowManager, "caption_prequit_info", 0, 0, 100, 3, "are you sure you want to forfeit?");
  Gui2Button *okButton = new Gui2Button(windowManager, "button_prequit_ok", 10, 0, 30, 3, "OK, forfeit");
  Gui2Button *cancelButton = new Gui2Button(windowManager, "button_prequit_cancel", 10, 0, 30, 3, "Continue match");
  okButton->sig_OnClick.connect(boost::bind(&PreQuitPage::GoMenu, this));
  cancelButton->sig_OnClick.connect(boost::bind(&PreQuitPage::GoBack, this));

  Gui2Grid *grid = new Gui2Grid(windowManager, "grid_prequit", 30, 42.5, 40, 15);

  grid->AddView(restartCaption, 0, 0);
  grid->AddView(okButton, 1, 0);
  grid->AddView(cancelButton, 2, 0);

  grid->UpdateLayout(0.5);

  this->AddView(grid);
  grid->Show();

  cancelButton->SetFocus();

  this->Show();
}

PreQuitPage::~PreQuitPage() {
}

void PreQuitPage::GoMenu() {
  this->Exit();
  GetMenuTask()->SetMenuAction(e_MenuAction_Menu);
  delete this;
}
