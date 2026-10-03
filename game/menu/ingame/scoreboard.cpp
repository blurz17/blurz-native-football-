// written by bastiaan konings schuiling 2008 - 2015
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#include "scoreboard.hpp"

#include "utils/gui2/windowmanager.hpp"

#include <SDL2/SDL.h>

#include "../../onthepitch/match.hpp"

using namespace blunted;

Gui2ScoreBoard::Gui2ScoreBoard(Gui2WindowManager *windowManager, Match *match) : Gui2View(windowManager, "scoreboard", 28, 4, 44, 4), match(match) {
  x_percent = 28;
  y_percent = 4;
  width_percent = 44;
  height_percent = 4;

  Vector3 textColor = 255;
  Vector3 textOutlineColor = 0;

  goalCount[0] = 0;
  goalCount[1] = 0;
  int x, y, w, h;
  windowManager->GetCoordinates(x_percent, y_percent, width_percent, height_percent, x, y, w, h);

  // percentages
  // EPL Style Flat Block Percentages
  float xOffset[9];
  xOffset[0] = 0; // league logo (purple block start)
  xOffset[1] = 6; // t1 name (red block start)
  xOffset[2] = 12; // t1 score (white block start)
  xOffset[3] = 15; // dash or gap
  xOffset[4] = 16; // t2 score (white block start)
  xOffset[5] = 19; // t2 name (blue block start)
  xOffset[6] = 25; // time (dark gray block start)
  xOffset[7] = 33; // end

  float relToAbsMultiplier = w / (width_percent * 1.0f);
  content_yOffset = 0.2f;
  float content_xOffset = 0.2f;

  float bgAlpha = 255.0f; // flat opaque colors

  Gui2Image *bg = new Gui2Image(windowManager, "image_scoreboard_bg", 0, 0, xOffset[7], height_percent);
  bg->LoadImage("media/menu/scoreboard_bg_epl.png");
  this->AddView(bg);
  bg->Show();

  leagueLogo = new Gui2Image(windowManager, "game_scoreboard_leaguelogo", xOffset[0], 0, height_percent / windowManager->GetAspectRatio(), height_percent);
  this->AddView(leagueLogo);
  leagueLogo->LoadImage("media/menu/league.png");
  leagueLogo->Show();

  teamNameCaption[0] = new Gui2Caption(windowManager, "game_scoreboard_team1name", xOffset[1] + content_xOffset + 0.5f, 0, 5, height_percent * 0.9f, match->GetTeam(0)->GetTeamData()->GetShortName());
  goalCountCaption[0] = new Gui2Caption(windowManager, "game_scoreboard_team1goals", xOffset[2] + content_xOffset + 0.5f, 0, 3, height_percent * 0.9f, "0");
  
  goalCountCaption[1] = new Gui2Caption(windowManager, "game_scoreboard_team2goals", xOffset[4] + content_xOffset + 0.5f, 0, 3, height_percent * 0.9f, "0");
  teamNameCaption[1] = new Gui2Caption(windowManager, "game_scoreboard_team2name", xOffset[5] + content_xOffset + 0.5f, 0, 5, height_percent * 0.9f, match->GetTeam(1)->GetTeamData()->GetShortName());
  
  timeCaption = new Gui2Caption(windowManager, "game_scoreboard_timecaption", xOffset[6] + content_xOffset + 1.0f, 0, 6, height_percent * 0.9f, "0:00");

  Vector3 scoreColor = Vector3(0, 0, 0); // Black for score on white block
  timeCaption->SetColor(Vector3(255, 255, 255));
  timeCaption->SetOutlineColor(textOutlineColor);
  teamNameCaption[0]->SetColor(Vector3(255, 255, 255));
  teamNameCaption[0]->SetOutlineColor(textOutlineColor);
  teamNameCaption[1]->SetColor(Vector3(255, 255, 255));
  teamNameCaption[1]->SetOutlineColor(textOutlineColor);
  goalCountCaption[0]->SetColor(scoreColor);
  goalCountCaption[0]->SetOutlineColor(scoreColor);
  goalCountCaption[1]->SetColor(scoreColor);
  goalCountCaption[1]->SetOutlineColor(scoreColor);

  // tvLogo removed for cleaner layout

  this->AddView(timeCaption);
  timeCaption->Show();
  this->AddView(teamNameCaption[0]);
  teamNameCaption[0]->Show();
  this->AddView(teamNameCaption[1]);
  teamNameCaption[1]->Show();
  this->AddView(goalCountCaption[0]);
  goalCountCaption[0]->Show();
  this->AddView(goalCountCaption[1]);
  goalCountCaption[1]->Show();

  SetGoalCount(0, 0);
  SetGoalCount(1, 0);

  // EPL style doesn't use massive team logos on the scoreboard bug, just text
  /*
  teamLogo[0] = new Gui2Image(windowManager, "game_scoreboard_team1logo", xOffset[2], 0, height_percent / windowManager->GetAspectRatio(), height_percent);
  this->AddView(teamLogo[0]);
  teamLogo[0]->LoadImage(match->GetTeam(0)->GetTeamData()->GetLogoUrl());
  teamLogo[0]->Show();

  teamLogo[1] = new Gui2Image(windowManager, "game_scoreboard_team2logo", xOffset[5], 0, height_percent / windowManager->GetAspectRatio(), height_percent);
  this->AddView(teamLogo[1]);
  teamLogo[1]->LoadImage(match->GetTeam(1)->GetTeamData()->GetLogoUrl());
  teamLogo[1]->Show();
  */

  this->Show();
}

Gui2ScoreBoard::~Gui2ScoreBoard() {
  /* will be cleaned up while deleting gui2 tree automatically
  bg->Exit();
  delete bg;
  leagueLogo->Exit();
  delete leagueLogo;
  timeCaption->Exit();
  delete timeCaption;
  teamNameCaption[0]->Exit();
  delete teamNameCaption[0];
  teamNameCaption[1]->Exit();
  delete teamNameCaption[1];
  goalCountCaption[0]->Exit();
  delete goalCountCaption[0];
  goalCountCaption[1]->Exit();
  delete goalCountCaption[1];
  teamLogo[0]->Exit();
  delete teamLogo[0];
  teamLogo[1]->Exit();
  delete teamLogo[1];
  tvLogo->Exit();
  delete tvLogo;
  */
}

void Gui2ScoreBoard::GetImages(std::vector < boost::intrusive_ptr<Image2D> > &target) {
  Gui2View::GetImages(target);
}

void Gui2ScoreBoard::Redraw() {
}

void Gui2ScoreBoard::SetTimeStr(const std::string &timeStr) {
  this->timeStr = timeStr;
  timeCaption->SetCaption(timeStr);
}

void Gui2ScoreBoard::SetGoalCount(int teamID, int goalCount) {
  this->goalCount[teamID] = goalCount;
  std::string goalStr = "";
  if (goalCount < 10) goalStr.append(" ");
  goalStr.append(int_to_str(goalCount));
  goalCountCaption[teamID]->SetCaption(goalStr);
}
