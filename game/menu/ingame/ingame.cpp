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
  Match *match = GetGameTask()->GetMatch();
  match->Pause(true);

  Gui2Root *root = windowManager->GetRoot();

  // Full screen semi-transparent overlay
  Gui2Image *overlayBg = new Gui2Image(windowManager, "image_ingame_overlay", 0, 0, 100, 100);
  overlayBg->LoadImage("media/menu/backgrounds/black.png"); // Ensure this is a semi-transparent image
  this->AddView(overlayBg);
  overlayBg->Show();

  // Top Section: Team Logos, Names, and Score
  // Team 1
  Gui2Image *t1Logo = new Gui2Image(windowManager, "pause_t1_logo", 25, 5, 10, 15);
  t1Logo->LoadImage(match->GetTeam(0)->GetTeamData()->GetLogoUrl());
  this->AddView(t1Logo);
  t1Logo->Show();

  Gui2Caption *t1Name = new Gui2Caption(windowManager, "pause_t1_name", 15, 22, 30, 4, match->GetTeam(0)->GetTeamData()->GetName());
  t1Name->SetColor(255);
  this->AddView(t1Name);
  t1Name->Show();

  // Score
  std::string scoreStr = int_to_str(match->GetMatchData()->GetGoalCount(0)) + " - " + int_to_str(match->GetMatchData()->GetGoalCount(1));
  Gui2Caption *scoreCap = new Gui2Caption(windowManager, "pause_score", 40, 7, 20, 10, scoreStr);
  scoreCap->SetColor(255);
  this->AddView(scoreCap);
  scoreCap->Show();

  // Team 2
  Gui2Image *t2Logo = new Gui2Image(windowManager, "pause_t2_logo", 65, 5, 10, 15);
  t2Logo->LoadImage(match->GetTeam(1)->GetTeamData()->GetLogoUrl());
  this->AddView(t2Logo);
  t2Logo->Show();

  Gui2Caption *t2Name = new Gui2Caption(windowManager, "pause_t2_name", 55, 22, 30, 4, match->GetTeam(1)->GetTeamData()->GetName());
  t2Name->SetColor(255);
  this->AddView(t2Name);
  t2Name->Show();

  // PES21 Mobile Style Layout
  // Left side: Match Info (already set up above, maybe adjust positions)
  // Right side: Vertical stack of wide buttons

  float btnX = 60.0f; // Right side
  float btnW = 35.0f;
  float btnH = 10.0f;
  float spacing = 12.0f;
  float startY = 15.0f;
  
  Gui2Button *buttonResume = new Gui2Button(windowManager, "button_resume", btnX, startY, btnW, btnH, "Resume Match");
  Gui2Button *buttonGamePlan = new Gui2Button(windowManager, "button_gameplan", btnX, startY + spacing * 1, btnW, btnH, "Squad Management");
  Gui2Button *buttonSystemSettings = new Gui2Button(windowManager, "button_systemsettings", btnX, startY + spacing * 2, btnW, btnH, "Match Settings");
  Gui2Button *buttonCameraSettings = new Gui2Button(windowManager, "button_camerasettings", btnX, startY + spacing * 3, btnW, btnH, "Camera Settings");
  Gui2Button *buttonControllerSelect = new Gui2Button(windowManager, "button_controllerselect", btnX, startY + spacing * 4, btnW, btnH, "Command List");
  Gui2Button *buttonPreQuit = new Gui2Button(windowManager, "button_quit", btnX, startY + spacing * 5, btnW, btnH, "Forfeit Match");

  // Visual options isn't usually prominent on mobile pause, we can hide it or put it in Match Settings
  // We'll leave it out of the main stack to keep it clean, or we can add it:
  Gui2Button *buttonVisualOptions = new Gui2Button(windowManager, "button_visualoptions", btnX, startY + spacing * 6, btnW, btnH, "Sound Settings");

  // Wire signals
  buttonResume->sig_OnClick.connect(boost::bind(&IngamePage::GoResume, this));
  buttonGamePlan->sig_OnClick.connect(boost::bind(&IngamePage::GoGamePlan, this));
  buttonControllerSelect->sig_OnClick.connect(boost::bind(&IngamePage::GoControllerSelect, this));
  buttonCameraSettings->sig_OnClick.connect(boost::bind(&IngamePage::GoCameraSettings, this));
  buttonVisualOptions->sig_OnClick.connect(boost::bind(&IngamePage::GoVisualOptions, this));
  buttonSystemSettings->sig_OnClick.connect(boost::bind(&IngamePage::GoSystemSettings, this));
  buttonPreQuit->sig_OnClick.connect(boost::bind(&IngamePage::GoPreQuit, this));

  this->AddView(buttonResume); buttonResume->Show();
  this->AddView(buttonGamePlan); buttonGamePlan->Show();
  this->AddView(buttonSystemSettings); buttonSystemSettings->Show();
  this->AddView(buttonCameraSettings); buttonCameraSettings->Show();
  this->AddView(buttonControllerSelect); buttonControllerSelect->Show();
  this->AddView(buttonVisualOptions); buttonVisualOptions->Show();
  this->AddView(buttonPreQuit); buttonPreQuit->Show();

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
