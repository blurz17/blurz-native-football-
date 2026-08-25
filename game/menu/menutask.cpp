// written by bastiaan konings schuiling 2008 - 2015
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#include "menutask.hpp"

#include "game/match/match.hpp"
#include "engine/data/matchdata.hpp"

#include "pagefactory.hpp"

#include "mainmenu.hpp"
#include "ingame/ingame.hpp"
#include "visualoptions.hpp"
#include "ingame/replaymenu.hpp"
#include "ingame/phasemenu.hpp"
#include "ingame/gameover.hpp"

#include "engine/gametask.hpp"

#include "engine/main.hpp"

#include "engine/framework/scheduler.hpp"
#include "engine/framework/managers/resourcemanagerpool.hpp"

using namespace blunted;

void SetActiveController(int side, bool keyboard) {
  bool keyboardActive = true;
  const std::vector<SideSelection> sides = GetMenuTask()->GetControllerSetup();
  int menuControllerID = -1;
  for (unsigned int i = 0; i < sides.size(); i++) {
    if (sides.at(i).side == side) {
      if (GetControllers().at(sides.at(i).controllerID)->GetDeviceType() == e_HIDeviceType_Gamepad) {
        menuControllerID = static_cast<HIDGamepad*>(GetControllers().at(sides.at(i).controllerID))->GetGamepadID();
        keyboardActive = false;
      }
      break;
    }
    if (i == sides.size() - 1) menuControllerID = 0; // AI opponent, so allow choosing their team with controller
  }

  GetMenuTask()->SetActiveJoystickID(menuControllerID);
  if (keyboard) {
    if (keyboardActive) {
      GetMenuTask()->EnableKeyboard();
    } else {
      GetMenuTask()->DisableKeyboard();
    }
  } else {
    GetMenuTask()->EnableKeyboard();
  }
}

MenuTask::MenuTask(float aspectRatio, float margin, TTF_Font *defaultFont, TTF_Font *defaultOutlineFont) : Gui2Task(GetScene2D(), aspectRatio, margin) {

  Gui2Style *style = windowManager->GetStyle();

  style->SetFont(e_TextType_Default, defaultFont);
  style->SetFont(e_TextType_DefaultOutline, defaultOutlineFont);
  style->SetFont(e_TextType_Caption, defaultFont);
  style->SetFont(e_TextType_Title, defaultFont);
  style->SetFont(e_TextType_ToolTip, defaultFont);

/* previous colorset
  style->SetColor(e_DecorationType_Dark1, Vector3(0, 0, 0));
  style->SetColor(e_DecorationType_Dark2, Vector3(63, 63, 63));
  style->SetColor(e_DecorationType_Bright1, Vector3(240, 255, 210));
  style->SetColor(e_DecorationType_Bright2, Vector3(214, 194, 154));
  style->SetColor(e_DecorationType_Toggled, Vector3(255, 20, 70));
*/

  // huisstijl:
  // blauw: 0, 100, 220
  // orange: 240, 100, 0
  style->SetColor(e_DecorationType_Dark1, Vector3(20, 35, 55));
  style->SetColor(e_DecorationType_Dark2, Vector3(60, 35, 20));
  style->SetColor(e_DecorationType_Bright1, Vector3(150, 180, 220));
  style->SetColor(e_DecorationType_Bright2, Vector3(240, 150, 100));
  style->SetColor(e_DecorationType_Toggled, Vector3(240, 60, 60));

  windowManager->SetTimeStep_ms(10);

  Gui2Root *root = windowManager->GetRoot();
  root->Show();

  PageFactory *pageFactory = new PageFactory();
  windowManager->SetPageFactory(pageFactory);

  m_quickStarted = QuickStart();
  if (!m_quickStarted) {

    queuedFixture->team1KitNum = 1;
    queuedFixture->team2KitNum = 2;

    menuAction = e_MenuAction_Menu;

  } else {

    int size = GetControllers().size();
    for (int i = 0; i < size; i++) {
      SideSelection side;
      side.controllerID = i;
      if ((size > 1 && i == 1) || (size == 1 && i == 0)) {
        side.side = -1;
      } else {
        side.side = 0;
      }
      queuedFixture->sides.push_back(side);
    }

    // 1 == ajax
    // 2 == arsenal
    // 3 == barcelona
    // 4 == bayern
    // 5 == borussia
    // 6 == man utd
    // 7 == psv
    // 8 == real madrid
    const char* team1Env = getenv("TEAM1_ID");
    const char* team2Env = getenv("TEAM2_ID");
    queuedFixture->teamID1 = team1Env ? std::string(team1Env) : "3";
    queuedFixture->teamID2 = team2Env ? std::string(team2Env) : "8";
    queuedFixture->team1KitNum = 1;
    queuedFixture->team2KitNum = 2;

    // Read match settings from Android UI (passed via env vars)
    const char* diffEnv = getenv("MATCH_DIFFICULTY");
    if (diffEnv) {
      GetConfiguration()->Set("match_difficulty", atof(diffEnv));
      GetConfiguration()->SaveFile(GetConfigFilename());
    }
    const char* durEnv = getenv("MATCH_DURATION");
    if (durEnv) {
      GetConfiguration()->Set("match_duration", atof(durEnv));
      GetConfiguration()->SaveFile(GetConfigFilename());
    }
    const char* volEnv = getenv("AUDIO_VOLUME");
    if (volEnv) {
      GetConfiguration()->Set("audio_volume", atof(volEnv));
      GetConfiguration()->SaveFile(GetConfigFilename());
    }

    menuAction = e_MenuAction_Menu;

  }

}

MenuTask::~MenuTask() {
  if (Verbose()) printf("exiting menutask.. ");

  delete windowManager->GetPageFactory();

  if (Verbose()) printf("done\n");
}

void MenuTask::ProcessPhase() {

  Gui2Task::ProcessPhase();

  if (menuAction == e_MenuAction_Menu) {

    windowManager->GetPagePath()->Clear();

    GetGameTask()->Action(e_GameTaskMessage_StopMatch);

    Properties properties;
#ifdef __ANDROID__
    if (!m_quickStarted) {
      GetGameTask()->Action(e_GameTaskMessage_StartMenuScene);
      EnvironmentManager::GetInstance().SignalQuit();
    } else {
      m_quickStarted = false;
      
      MatchData *matchData = new MatchData(GetTeamID(0), GetTeamID(1));
      SetMatchData(matchData);
      
      windowManager->GetPageFactory()->CreatePage((int)e_PageID_Game, properties, 0);
      
      menuAction = e_MenuAction_Game; // Instantly start game next frame
    }
#else
    if (!m_quickStarted) {
      if (!IsReleaseVersion()) {
        windowManager->GetPageFactory()->CreatePage((int)e_PageID_MainMenu, properties, 0);
      } else {
        windowManager->GetPageFactory()->CreatePage((int)e_PageID_Intro, properties, 0);
      }
    } else {
      m_quickStarted = false;
      windowManager->GetPageFactory()->CreatePage((int)e_PageID_LoadingMatch, properties, 0);
    }
#endif

  } 
  
  if (menuAction == e_MenuAction_Game) {

    GetGameTask()->Action(e_GameTaskMessage_StopMenuScene);
    GetGameTask()->Action(e_GameTaskMessage_StartMatch);

  }

  menuAction = e_MenuAction_None;
}

bool MenuTask::QuickStart() {
  const char* skipMenu = getenv("SKIP_MAIN_MENU");
  if (skipMenu && std::string(skipMenu) == "1") {
#ifdef __ANDROID__
    unsetenv("SKIP_MAIN_MENU");
#endif
    return true;
  }
  
  return !IsReleaseVersion() && EnvironmentManager::GetInstance().GetTime_ms() < 10000;
}

void MenuTask::QuitGame() {
  EnvironmentManager::GetInstance().SignalQuit();
}

void MenuTask::ReleaseAllButtons() {
  // when going back to game, depress all buttons, so we don't go around doing passes we don't want
  for (int joyID = 0; joyID < UserEventManager::GetInstance().GetJoystickCount(); joyID++) {
    for (unsigned int buttonID = 0; buttonID < blunted::_JOYSTICK_MAXBUTTONS; buttonID++) {
      UserEventManager::GetInstance().SetJoyButtonState(joyID, buttonID, false);
    }
  }
  UserEventManager::GetInstance().SetKeyboardState(SDLK_ESCAPE, false);
}
