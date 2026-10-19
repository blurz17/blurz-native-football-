#ifndef _HPP_TRAINING_MANAGER
#define _HPP_TRAINING_MANAGER

#include "defines.hpp"
#include "training_mode.hpp"
#include "base/math/vector3.hpp"

#include <string>
#include <atomic>

class Match;
class Player;

namespace blunted {

  class TrainingManager {
    public:
      TrainingManager(Match *match, const TrainingConfig &config);
      virtual ~TrainingManager();

      void Process();

      void ResetDrill() { pendingResetRequest.store(true); }
      void NextDrill() { pendingNextDrill.store(true); }
      void PrevDrill() { pendingPrevDrill.store(true); }
      void SetDrill(e_TrainingType type) { pendingSetDrill.store((int)type); }
      e_TrainingType GetDrill() const { return config.type; }
      std::string GetDrillName() const;

      void NextVariation() { pendingNextVariation.store(true); }
      void SetFreeKickVariation(int var);
      int GetFreeKickVariation() const { return config.freeKickVariation; }
      std::string GetVariationName() const;

      void SetCornerSide(int side);
      int GetCornerSide() const { return config.cornerSide; }

      void ToggleOpponents();
      bool GetOpponentsEnabled() const { return config.enableOpponentOutfield; }

      bool IsPlayerCamActive() const { return config.playerCamActive; }
      void SetPlayerCam(bool active) { config.playerCamActive = active; }
      void TogglePlayerCam() { config.playerCamActive = !config.playerCamActive; }

      void OnGoalScored(int teamID);
      int GetScore() const { return drillScore; }
      int GetAttempts() const { return drillAttempts; }

    private:
      void ExecuteResetDrill();
      void ExecuteNextDrill();
      void ExecutePrevDrill();
      void ExecuteSetDrill(e_TrainingType type);
      void ExecuteNextVariation();

      void SetupFreePlay();
      void SetupFreeKick();
      void SetupCorner();
      void SetupPenalty();

      void ScheduleReset(unsigned long delay_ms);
      Vector3 GetFreeKickPosition() const;
      Vector3 GetCornerPosition() const;
      Vector3 GetPenaltyPosition() const;

      Match *match;
      TrainingConfig config;

      std::atomic<int> pendingSetDrill{-1};
      std::atomic<bool> pendingResetRequest{false};
      std::atomic<bool> pendingNextDrill{false};
      std::atomic<bool> pendingPrevDrill{false};
      std::atomic<bool> pendingNextVariation{false};

      bool pendingReset;
      unsigned long resetScheduledTime_ms;
      bool setPieceTaken;
      unsigned long setPieceTakenTime_ms;
      unsigned long lastActionTime_ms;

      int drillScore;
      int drillAttempts;
  };

}

#endif
