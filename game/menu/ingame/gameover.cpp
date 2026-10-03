// written by bastiaan konings schuiling 2008 - 2015
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#include "gameover.hpp"

#include "base/gamecontext.hpp"

#include "gametask.hpp"
#include "menu/menutask.hpp"
#include "hid/ihidevice.hpp"
#include "hid/gamepad.hpp"
#include "framework/scheduler.hpp"
#include "onthepitch/match.hpp"
#include "managers/usereventmanager.hpp"
#include "../pagefactory.hpp"

using namespace blunted;

GameOverPage::GameOverPage(Gui2WindowManager *windowManager, const Gui2PageData &pageData) : Gui2Page(windowManager, pageData) {

  match = GetContext()->gameTask->GetMatch();
  match->Pause(true);

  float possession1 = match->GetMatchData()->GetPossessionTime_ms(0);
  float possession2 = match->GetMatchData()->GetPossessionTime_ms(1);
  int shots1 = match->GetMatchData()->GetShots(0);
  int shots2 = match->GetMatchData()->GetShots(1);

  blunted::UserEventManager::GetInstance().NotifyGameOver(
      match->GetMatchData()->GetGoalCount(0),
      match->GetMatchData()->GetGoalCount(1),
      (int)round(possession1 / (possession1 + possession2) * 100.0f),
      (int)round(possession2 / (possession1 + possession2) * 100.0f),
      shots1, shots2
  );

  // Hide the button so it's not visible but can still receive focus and trigger GoMainMenu
  buttonOkay = new Gui2Button(windowManager, "button_gameover_ok", 200, 200, 20, 3, "OK");
  this->AddView(buttonOkay);
  buttonOkay->Show();
  buttonOkay->sig_OnClick.connect(boost::bind(&GameOverPage::GoMainMenu, this));


  buttonOkay->SetFocus();

  this->Show();
}

GameOverPage::~GameOverPage() {
}

void GameOverPage::GoRematch() {
  windowManager->GetPagePath()->Clear();

  GetContext()->gameTask->Action(e_GameTaskMessage_StopMatch);
  GetContext()->gameTask->Action(e_GameTaskMessage_StartMatch);

  this->Exit();
  Properties properties;
  windowManager->GetPageFactory()->CreatePage((int)e_PageID_Game, properties, 0);
  delete this;
}

void GameOverPage::GoMainMenu() {
  this->Exit();
  GetContext()->menuTask->SetMenuAction(e_MenuAction_Menu);
  delete this;
}
