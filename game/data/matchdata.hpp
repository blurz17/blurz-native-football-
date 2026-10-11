// written by bastiaan konings schuiling 2008 - 2015
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#ifndef _HPP_MATCHDATA
#define _HPP_MATCHDATA

#include "defines.hpp"

#include "../gamedefines.hpp"

#include "teamdata.hpp"

class MatchData {

  public:
    MatchData(int team1DatabaseID, int team2DatabaseID);
    virtual ~MatchData();

    TeamData *GetTeamData(int id) { return teamData[id]; }
    int GetGoalCount(int id) { return goalCount[id]; }
    void SetGoalCount(int id, int amount) { goalCount[id] = amount; }
    void AddPossessionTime_10ms(int teamID);
    unsigned long GetPossessionTime_ms(int teamID) { return possessionTime_ms[teamID]; }
    float GetPossessionFactor_60seconds() { return possession60seconds / 60.0f * 0.5f + 0.5f; } // REMEMBER THESE ARE IRL INGAME SECONDS (because, I guess the tactics should be based on irl possession time instead of gametime? not sure yet, think about this)
    void AddShot(int teamID) { shots[teamID] += 1; }
    int GetShots(int teamID) const { return shots[teamID]; }

    void AddShotOnTarget(int teamID) { shotsOnTarget[teamID] += 1; }
    int GetShotsOnTarget(int teamID) const { return shotsOnTarget[teamID]; }

    void AddFoul(int teamID) { fouls[teamID] += 1; }
    int GetFouls(int teamID) const { return fouls[teamID]; }

    void AddOffside(int teamID) { offsides[teamID] += 1; }
    int GetOffsides(int teamID) const { return offsides[teamID]; }

    void AddCorner(int teamID) { corners[teamID] += 1; }
    int GetCorners(int teamID) const { return corners[teamID]; }

    void AddFreeKick(int teamID) { freeKicks[teamID] += 1; }
    int GetFreeKicks(int teamID) const { return freeKicks[teamID]; }

    void AddPass(int teamID, bool completed) { passes[teamID] += 1; if (completed) passesCompleted[teamID] += 1; }
    int GetPasses(int teamID) const { return passes[teamID]; }
    int GetPassesCompleted(int teamID) const { return passesCompleted[teamID]; }

    void AddCross(int teamID) { crosses[teamID] += 1; }
    int GetCrosses(int teamID) const { return crosses[teamID]; }

    void AddInterception(int teamID) { interceptions[teamID] += 1; }
    int GetInterceptions(int teamID) const { return interceptions[teamID]; }

    void AddTackle(int teamID) { tackles[teamID] += 1; }
    int GetTackles(int teamID) const { return tackles[teamID]; }

    void AddSave(int teamID) { saves[teamID] += 1; }
    int GetSaves(int teamID) const { return saves[teamID]; }

  protected:
    TeamData *teamData[2];

    int goalCount[2];

    unsigned long possessionTime_ms[2];
    float possession60seconds; // -600 to 600 for possession of team 1 / 2 respectively
    int shots[2];
    int shotsOnTarget[2];
    int fouls[2];
    int offsides[2];
    int corners[2];
    int freeKicks[2];
    int passes[2];
    int passesCompleted[2];
    int crosses[2];
    int interceptions[2];
    int tackles[2];
    int saves[2];

};

#endif
