#include "training_manager.hpp"

#include "../onthepitch/match.hpp"
#include "../onthepitch/ball.hpp"
#include "../onthepitch/team.hpp"
#include "../onthepitch/player/player.hpp"
#include "../onthepitch/referee.hpp"
#include "../onthepitch/teamAIcontroller.hpp"
#include "../gamedefines.hpp"
#include "managers/usereventmanager.hpp"
#include "hid/ihidevice.hpp"

#include <cmath>

namespace blunted {

  TrainingManager::TrainingManager(Match *match, const TrainingConfig &config)
      : match(match), config(config), pendingReset(false), resetScheduledTime_ms(0),
        setPieceTaken(false), setPieceTakenTime_ms(0), lastActionTime_ms(0),
        drillScore(0), drillAttempts(0) {
    ResetDrill();
  }

  TrainingManager::~TrainingManager() {
  }

  void TrainingManager::ExecuteResetDrill() {
    pendingReset = false;
    setPieceTaken = false;
    setPieceTakenTime_ms = 0;

    switch (config.type) {
      case e_TrainingType_FreeKick:
        SetupFreeKick();
        break;
      case e_TrainingType_Corner:
        SetupCorner();
        break;
      case e_TrainingType_Penalty:
        SetupPenalty();
        break;
      case e_TrainingType_FreePlay:
      default:
        SetupFreePlay();
        break;
    }
  }

  void TrainingManager::ExecuteNextDrill() {
    int next = (int)config.type + 1;
    if (next > (int)e_TrainingType_Penalty) next = (int)e_TrainingType_FreePlay;
    ExecuteSetDrill((e_TrainingType)next);
  }

  void TrainingManager::ExecutePrevDrill() {
    int prev = (int)config.type - 1;
    if (prev < (int)e_TrainingType_FreePlay) prev = (int)e_TrainingType_Penalty;
    ExecuteSetDrill((e_TrainingType)prev);
  }

  void TrainingManager::ExecuteSetDrill(e_TrainingType type) {
    config.type = type;
    drillScore = 0;
    drillAttempts = 0;
    ExecuteResetDrill();
  }

  std::string TrainingManager::GetDrillName() const {
    switch (config.type) {
      case e_TrainingType_FreePlay: return "Free Play";
      case e_TrainingType_FreeKick: return "Free Kick";
      case e_TrainingType_Corner:   return "Corner Kick";
      case e_TrainingType_Penalty:  return "Penalty Kick";
      default:                      return "Training";
    }
  }

  void TrainingManager::ExecuteNextVariation() {
    if (config.type == e_TrainingType_FreeKick) {
      config.freeKickVariation = (config.freeKickVariation + 1) % e_FreeKick_MAX;
      ExecuteResetDrill();
    } else if (config.type == e_TrainingType_Corner) {
      config.cornerSide = (config.cornerSide + 1) % e_Corner_MAX;
      ExecuteResetDrill();
    } else if (config.type == e_TrainingType_FreePlay) {
      ToggleOpponents();
    }
  }

  void TrainingManager::SetFreeKickVariation(int var) {
    config.freeKickVariation = var % e_FreeKick_MAX;
    if (config.type == e_TrainingType_FreeKick) {
      ResetDrill();
    }
  }

  std::string TrainingManager::GetVariationName() const {
    if (config.type == e_TrainingType_FreeKick) {
      switch (config.freeKickVariation) {
        case e_FreeKick_Center:    return "Center (24m)";
        case e_FreeKick_Left:      return "Left Angle";
        case e_FreeKick_Right:     return "Right Angle";
        case e_FreeKick_LongRange: return "Long Range (32m)";
      }
    } else if (config.type == e_TrainingType_Corner) {
      return (config.cornerSide == e_Corner_Left) ? "Left Corner" : "Right Corner";
    } else if (config.type == e_TrainingType_FreePlay) {
      return config.enableOpponentOutfield ? "Defenders: ON" : "Open Pitch (No AI)";
    }
    return "Standard";
  }

  void TrainingManager::SetCornerSide(int side) {
    config.cornerSide = side % e_Corner_MAX;
    if (config.type == e_TrainingType_Corner) {
      ResetDrill();
    }
  }

  void TrainingManager::ToggleOpponents() {
    config.enableOpponentOutfield = !config.enableOpponentOutfield;
    ResetDrill();
    match->SpamMessage(config.enableOpponentOutfield ? "Opponent Defenders: ON" : "Open Pitch: No AI Defenders", 1500);
  }

  void TrainingManager::OnGoalScored(int teamID) {
    drillAttempts++;
    drillScore++;
    std::string msg = "GOAL! (" + std::to_string(drillScore) + " scored)";
    match->SpamMessage(msg, 1500);
    ScheduleReset(1500);
  }

  void TrainingManager::ScheduleReset(unsigned long delay_ms) {
    if (!pendingReset) {
      pendingReset = true;
      resetScheduledTime_ms = match->GetActualTime_ms() + delay_ms;
    }
  }

  void TrainingManager::SetupFreePlay() {
    Team *humanTeam = match->GetTeam(config.humanTeamID);
    Team *oppTeam = match->GetTeam(1 - config.humanTeamID);

    float humanSide = humanTeam->GetSide();
    float oppSide = oppTeam->GetSide();

    // 1. Start in center of the stadium (kick-off spot)
    Vector3 ballStart(0.0f, 0.0f, 0.11f);
    match->ResetSituation(ballStart);

    // 2. Select central attacker/midfielder to start with ball at center
    std::vector<Player*> humanPlayers;
    humanTeam->GetActivePlayers(humanPlayers);

    Player *centerPlayer = nullptr;
    for (size_t i = 0; i < humanPlayers.size(); i++) {
      e_PlayerRole role = humanPlayers[i]->GetFormationEntry().role;
      if (role == e_PlayerRole_CF || role == e_PlayerRole_AM || role == e_PlayerRole_CM) {
        centerPlayer = humanPlayers[i];
        break;
      }
    }
    if (!centerPlayer) {
      for (size_t i = 0; i < humanPlayers.size(); i++) {
        if (humanPlayers[i]->GetFormationEntry().role != e_PlayerRole_GK) {
          centerPlayer = humanPlayers[i];
          break;
        }
      }
    }
    if (!centerPlayer && !humanPlayers.empty()) centerPlayer = humanPlayers[0];

    // Position center player at center circle right behind the ball, facing forward
    Vector3 forwardDir(oppSide, 0.0f, 0.0f);
    if (centerPlayer) {
      Vector3 takerPos(oppSide * -0.7f, 0.0f, 0.0f);
      centerPlayer->ResetPosition(takerPos, takerPos + forwardDir);
      humanTeam->SelectPlayer(centerPlayer);
    }

    // Spread human teammates naturally across their formation in stadium
    for (size_t i = 0; i < humanPlayers.size(); i++) {
      Player *p = humanPlayers[i];
      if (p == centerPlayer) continue;

      if (p->GetFormationEntry().role == e_PlayerRole_GK) {
        Vector3 gkPos(humanSide * (pitchHalfW - 1.5f), 0.0f, 0.0f);
        p->ResetPosition(gkPos, gkPos + forwardDir);
      } else {
        Vector3 formPos = p->GetFormationEntry().position;
        float x = oppSide * (formPos.coords[0] * (pitchHalfW * 0.45f) - (pitchHalfW * 0.15f));
        float y = formPos.coords[1] * (pitchHalfH * 0.65f);
        if (Vector3(x, y, 0.0f).GetLength() < 4.0f) {
          x += (oppSide > 0.0f ? -5.0f : 5.0f);
        }
        Vector3 pPos(x, y, 0.0f);
        p->ResetPosition(pPos, pPos + forwardDir);
      }
    }

    // 3. Opponent team:
    std::vector<Player*> oppPlayers;
    oppTeam->GetActivePlayers(oppPlayers);
    for (size_t i = 0; i < oppPlayers.size(); i++) {
      if (oppPlayers[i]->GetFormationEntry().role == e_PlayerRole_GK) {
        // Goalkeeper stays on goal line guarding net
        oppPlayers[i]->ResetPosition(Vector3(oppSide * (pitchHalfW - 0.5f), 0.0f, 0.0f), Vector3(0.0f, 0.0f, 0.0f));
      } else if (config.enableOpponentOutfield) {
        float yOffset = ((i % 2 == 1) ? 1.0f : -1.0f) * (6.0f + (i / 2) * 5.0f);
        float xOffset = oppSide * (pitchHalfW * 0.45f - (i / 2) * 4.0f);
        oppPlayers[i]->ResetPosition(Vector3(xOffset, yOffset, 0.0f), Vector3(0.0f, 0.0f, 0.0f));
      } else {
        // Park non-active opponent outfielders completely off the playing pitch (bench area)
        float benchX = -oppSide * (pitchHalfW * 0.5f) + (float)i * 2.5f * oppSide;
        oppPlayers[i]->ResetPosition(Vector3(benchX, pitchHalfH + 3.0f, 0.0f), Vector3(benchX, 0.0f, 0.0f));
      }
    }

    // 4. Ball is at center spot, completely stationary (not shot)
    match->GetBall()->SetPosition(ballStart);
    match->GetBall()->SetMomentum(Vector3(0.0f, 0.0f, 0.0f));
    match->StartPlay();
    match->StopSetPiece();
    match->SpamMessage("FREE PLAY: Center kick-off! Pass, dribble & explore freely.", 2500);
  }

  void TrainingManager::SetupFreeKick() {
    float oppSide = match->GetTeam(1 - config.humanTeamID)->GetSide();
    Vector3 restartPos;
    switch (config.freeKickVariation) {
      case e_FreeKick_Left:
        restartPos = Vector3(oppSide * (pitchHalfW - 24.0f), oppSide * 11.0f, 0.0f);
        break;
      case e_FreeKick_Right:
        restartPos = Vector3(oppSide * (pitchHalfW - 24.0f), oppSide * -11.0f, 0.0f);
        break;
      case e_FreeKick_LongRange:
        restartPos = Vector3(oppSide * (pitchHalfW - 32.0f), oppSide * 3.0f, 0.0f);
        break;
      case e_FreeKick_Center:
      default:
        restartPos = Vector3(oppSide * (pitchHalfW - 24.0f), 0.0f, 0.0f);
        break;
    }
    match->GetReferee()->TriggerSetPiece(e_SetPiece_FreeKick, config.humanTeamID, restartPos, 50);
    match->SpamMessage("FREE KICK (" + GetVariationName() + "): Shoot or cross over the wall!", 2500);
    setPieceTaken = false;
  }

  void TrainingManager::SetupCorner() {
    float oppSide = match->GetTeam(1 - config.humanTeamID)->GetSide();
    float y = (config.cornerSide == e_Corner_Left) ? pitchHalfH - 0.5f : -pitchHalfH + 0.5f;
    Vector3 restartPos(oppSide * (pitchHalfW - 0.5f), y, 0.0f);
    match->GetReferee()->TriggerSetPiece(e_SetPiece_Corner, config.humanTeamID, restartPos, 50);
    match->SpamMessage("CORNER KICK (" + std::string(config.cornerSide == e_Corner_Left ? "Left" : "Right") + "): Deliver the cross!", 2500);
    setPieceTaken = false;
  }

  void TrainingManager::SetupPenalty() {
    float oppSide = match->GetTeam(1 - config.humanTeamID)->GetSide();
    Vector3 restartPos(oppSide * (pitchHalfW - 11.0f), 0.0f, 0.0f);
    match->GetReferee()->TriggerSetPiece(e_SetPiece_Penalty, config.humanTeamID, restartPos, 50);
    match->SpamMessage("PENALTY DRILL: Beat the keeper from 11m!", 2500);
    setPieceTaken = false;
  }

  void TrainingManager::Process() {
    // Process pending async drill/variation/reset requests from UI / JNI thread safely on game thread
    if (pendingSetDrill.load() != -1) {
      e_TrainingType target = (e_TrainingType)pendingSetDrill.exchange(-1);
      ExecuteSetDrill(target);
    }
    if (pendingNextDrill.exchange(false)) {
      ExecuteNextDrill();
    }
    if (pendingPrevDrill.exchange(false)) {
      ExecutePrevDrill();
    }
    if (pendingNextVariation.exchange(false)) {
      ExecuteNextVariation();
    }
    if (pendingResetRequest.exchange(false)) {
      ExecuteResetDrill();
    }

    unsigned long now = match->GetActualTime_ms();

    if (pendingReset) {
      if (now >= resetScheduledTime_ms) {
        pendingReset = false;
        ExecuteResetDrill();
      }
      return;
    }

    // Keyboard controls for PC / emulator
#ifndef GPFB_HEADLESS
    if (UserEventManager::GetInstance().GetKeyboardState(SDLK_r)) {
      UserEventManager::GetInstance().SetKeyboardState(SDLK_r, false);
      ExecuteResetDrill();
      return;
    }
    if (UserEventManager::GetInstance().GetKeyboardState(SDLK_t)) {
      UserEventManager::GetInstance().SetKeyboardState(SDLK_t, false);
      ExecuteNextDrill();
      return;
    }
    if (UserEventManager::GetInstance().GetKeyboardState(SDLK_v)) {
      UserEventManager::GetInstance().SetKeyboardState(SDLK_v, false);
      ExecuteNextVariation();
      return;
    }
    if (UserEventManager::GetInstance().GetKeyboardState(SDLK_o)) {
      UserEventManager::GetInstance().SetKeyboardState(SDLK_o, false);
      ToggleOpponents();
      return;
    }
#endif

    // Set piece logic
    if (config.type != e_TrainingType_FreePlay) {
      if (!setPieceTaken && match->IsInPlay() && !match->IsInSetPiece()) {
        setPieceTaken = true;
        setPieceTakenTime_ms = now;
        drillAttempts++;
      }

      if (setPieceTaken) {
        Vector3 ballPos = match->GetBall()->Predict(0);
        float speed = match->GetBall()->GetMovement().GetLength();

        if (match->IsGoalScored()) {
          ScheduleReset(1500);
          return;
        }

        if (std::fabs(ballPos.coords[0]) > pitchHalfW || std::fabs(ballPos.coords[1]) > pitchHalfH) {
          match->SpamMessage("Out of bounds - Try again!", 1200);
          ScheduleReset(1200);
          return;
        }

        if (now > setPieceTakenTime_ms + 2500 && speed < 0.6f) {
          ScheduleReset(1200);
          return;
        }

        if (now > setPieceTakenTime_ms + 7500) {
          ScheduleReset(1000);
          return;
        }
      }
    } else {
      // Free play mode
      Vector3 ballPos = match->GetBall()->Predict(0);
      if (match->IsGoalScored()) {
        ScheduleReset(1500);
        return;
      }
      if (std::fabs(ballPos.coords[0]) > pitchHalfW || std::fabs(ballPos.coords[1]) > pitchHalfH) {
        match->SpamMessage("Ball out of play - Resetting drill", 1200);
        ScheduleReset(1200);
        return;
      }
      if (match->GetBallRetainer() && match->GetBallRetainer() == match->GetTeam(1 - config.humanTeamID)->GetGoalie()) {
        match->SpamMessage("Saved by the keeper! Resetting...", 1200);
        ScheduleReset(1500);
        return;
      }
    }
  }

}
