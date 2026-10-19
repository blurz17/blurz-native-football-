// Isolated Training Mode Definitions
#ifndef _HPP_TRAINING_MODE
#define _HPP_TRAINING_MODE

namespace blunted {

  enum e_TrainingType {
    e_TrainingType_None = 0,
    e_TrainingType_FreePlay = 1,   // Open play / 1v1 / passing, shooting & dribbling
    e_TrainingType_FreeKick = 2,   // Direct free kick with defensive wall and GK
    e_TrainingType_Corner = 3,     // Corner kick cross into the penalty box
    e_TrainingType_Penalty = 4     // Penalty spot kick 1v1 vs GK
  };

  enum e_FreeKickVariation {
    e_FreeKick_Center = 0,    // 24m center (direct shooting range)
    e_FreeKick_Left = 1,      // 23m left angle
    e_FreeKick_Right = 2,     // 23m right angle
    e_FreeKick_LongRange = 3, // 31m center (power shot range)
    e_FreeKick_MAX = 4
  };

  enum e_CornerSide {
    e_Corner_Right = 0,
    e_Corner_Left = 1,
    e_Corner_MAX = 2
  };

  struct TrainingConfig {
    e_TrainingType type = e_TrainingType_FreePlay;
    int humanTeamID = 0;
    bool enableOpponentOutfield = false;
    int freeKickVariation = e_FreeKick_Center;
    int cornerSide = e_Corner_Right;
    bool playerCamActive = true;
  };

}

#endif
