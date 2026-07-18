// written by bastiaan konings schuiling 2008 - 2015
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#ifndef _HPP_MAIN
#define _HPP_MAIN

#include "blunted.hpp"

#ifndef GPFB_HEADLESS
#include "gametask.hpp"
#include "menu/menutask.hpp"
#include "hid/ihidevice.hpp"

#include "systems/graphics/graphics_system.hpp"
#endif

#include "base/properties.hpp"

#include "utils/database.hpp"
#include "sqlite3.h"

enum e_DebugMode {
  e_DebugMode_Off,
  e_DebugMode_Tactical,
  e_DebugMode_AI
};

class Match;

#ifndef GPFB_HEADLESS
void SetGreenDebugPilon(const blunted::Vector3 &pos);
void SetBlueDebugPilon(const blunted::Vector3 &pos);
void SetYellowDebugPilon(const blunted::Vector3 &pos);
void SetRedDebugPilon(const blunted::Vector3 &pos);

void SetSmallDebugCircle1(const blunted::Vector3 &pos);
void SetSmallDebugCircle2(const blunted::Vector3 &pos);
void SetLargeDebugCircle(const blunted::Vector3 &pos);

boost::intrusive_ptr<blunted::Geometry> GetGreenDebugPilon();
boost::intrusive_ptr<blunted::Geometry> GetBlueDebugPilon();
boost::intrusive_ptr<blunted::Geometry> GetYellowDebugPilon();
boost::intrusive_ptr<blunted::Geometry> GetRedDebugPilon();

boost::intrusive_ptr<blunted::Geometry> GetSmallDebugCircle1();
boost::intrusive_ptr<blunted::Geometry> GetSmallDebugCircle2();
boost::intrusive_ptr<blunted::Geometry> GetLargeDebugCircle();

std::string GetConfigFilename();
boost::shared_ptr<blunted::Scene3D> GetScene3D();
#else
std::string GetConfigFilename();
#endif
#ifndef GPFB_HEADLESS
boost::shared_ptr<blunted::Scene2D> GetScene2D();
GraphicsSystem *GetGraphicsSystem();
boost::shared_ptr<GameTask> GetGameTask();
boost::shared_ptr<MenuTask> GetMenuTask();
#endif

bool IsReleaseVersion();
bool Verbose();
bool UpdateNonImportableDB();

blunted::Database *GetDB();
blunted::Properties *GetConfiguration();
std::string GetActiveSaveDirectory();
void SetActiveSaveDirectory(const std::string &dir);
bool SuperDebug();
e_DebugMode GetDebugMode();
#ifndef GPFB_HEADLESS
boost::intrusive_ptr<Image2D> GetDebugImage();
boost::intrusive_ptr<Image2D> GetDebugOverlay();
void GetDebugOverlayCoord(Match *match, const Vector3 &worldPos, int &x, int &y);
#endif

int PredictFrameTimeToGo_ms(int frameCount);

#ifndef GPFB_HEADLESS
const std::vector<IHIDevice*> &GetControllers();
#endif

int main(int argc, const char** argv);

#endif
