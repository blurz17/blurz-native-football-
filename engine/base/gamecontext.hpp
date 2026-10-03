#ifndef _HPP_GAMECONTEXT
#define _HPP_GAMECONTEXT

#include <vector>
#include <string>
#include <boost/shared_ptr.hpp>
#include <boost/intrusive_ptr.hpp>

namespace blunted {
  class Properties;
  class Database;
  class Scene2D;
  class Scene3D;
  class Geometry;
  class Image2D;
  class GraphicsSystem;
  class Vector3;
}

class IHIDevice;
class GameTask;
class MenuTask;
class Match;

enum e_DebugMode {
  e_DebugMode_Off,
  e_DebugMode_Tactical,
  e_DebugMode_AI
};

struct GameContext {
  GameContext();
  ~GameContext();

  blunted::Properties* config;
  blunted::Database* db;
  blunted::GraphicsSystem* graphicsSystem;
  
  boost::shared_ptr<blunted::Scene2D> scene2D;
  boost::shared_ptr<blunted::Scene3D> scene3D;
  
  boost::shared_ptr<GameTask> gameTask;
  boost::shared_ptr<MenuTask> menuTask;

  std::vector<IHIDevice*> controllers;

  bool superDebug;
  e_DebugMode debugMode;

  std::string activeSaveDirectory;

  // Debug objects
  boost::intrusive_ptr<blunted::Geometry> greenPilon;
  boost::intrusive_ptr<blunted::Geometry> bluePilon;
  boost::intrusive_ptr<blunted::Geometry> yellowPilon;
  boost::intrusive_ptr<blunted::Geometry> redPilon;

  boost::intrusive_ptr<blunted::Geometry> smallDebugCircle1;
  boost::intrusive_ptr<blunted::Geometry> smallDebugCircle2;
  boost::intrusive_ptr<blunted::Geometry> largeDebugCircle;

  boost::intrusive_ptr<blunted::Image2D> debugImage;
  boost::intrusive_ptr<blunted::Image2D> debugOverlay;
};

GameContext* GetContext();

bool IsReleaseVersion();
bool Verbose();
bool UpdateNonImportableDB();
std::string GetConfigFilename();
std::string GetActiveSaveDirectory();
void SetActiveSaveDirectory(const std::string &dir);
int PredictFrameTimeToGo_ms(int frameCount);
void GetDebugOverlayCoord(Match *match, const blunted::Vector3 &worldPos, int &x, int &y);
boost::intrusive_ptr<blunted::Image2D> GetDebugImage();
boost::intrusive_ptr<blunted::Image2D> GetDebugOverlay();

void SetGreenDebugPilon(const blunted::Vector3 &pos);
void SetBlueDebugPilon(const blunted::Vector3 &pos);
void SetYellowDebugPilon(const blunted::Vector3 &pos);
void SetRedDebugPilon(const blunted::Vector3 &pos);


#endif
