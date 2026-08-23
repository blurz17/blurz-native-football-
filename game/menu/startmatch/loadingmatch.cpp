// written by bastiaan konings schuiling 2008 - 2015
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#include "loadingmatch.hpp"

#include "engine/main.hpp"

#include "game/menu/pagefactory.hpp"

using namespace blunted;

LoadingMatchPage::LoadingMatchPage(Gui2WindowManager *windowManager, const Gui2PageData &pageData) : Gui2Page(windowManager, pageData) {

  Gui2Image *loading = new Gui2Image(windowManager, "image_main_loading", 0, 0, 100, 100);
  loading->LoadImage("media/menu/backgrounds/black.png");
  this->AddView(loading);
  loading->Show();

  Gui2Caption *caption1 = new Gui2Caption(windowManager, "main_loading_caption", 0, 48, 100, 5, "LOADING MATCH...");
  caption1->SetColor(Vector3(20, 230, 160)); // FC Mobile Neon Teal
  this->AddView(caption1);
  caption1->Show();

  MatchData *matchData = new MatchData(GetMenuTask()->GetTeamID(0), GetMenuTask()->GetTeamID(1));
  GetMenuTask()->SetMatchData(matchData);

  this->SetFocus();

  this->Show();

  GetMenuTask()->SetActiveJoystickID(0);
  GetMenuTask()->EnableKeyboard();
  windowManager->GetPagePath()->Clear();

  if (Verbose()) printf("hoi\n");
  sentStartGameSignal = false;
}

LoadingMatchPage::~LoadingMatchPage() {
}

void LoadingMatchPage::Process() {
  Gui2Page::Process();

  if (!sentStartGameSignal) {
    sentStartGameSignal = true;
    EnvironmentManager::GetInstance().Pause_ms(100); // make 'sure' the logos have been displayed by the graphics thread. todonow: this is a terrible hax, and you know it. you are a bad person!
    GetMenuTask()->SetMenuAction(e_MenuAction_Game);
  }
}

void LoadingMatchPage::Close() {

  this->Exit();

  Properties properties;
  windowManager->GetPageFactory()->CreatePage((int)e_PageID_Game, properties, 0);

  if (Verbose()) printf("auw\n");
  delete this;

}
