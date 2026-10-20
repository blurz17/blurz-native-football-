#ifdef __ANDROID__
#include "android_assets.hpp"

#include <SDL2/SDL.h>
#include <SDL2/SDL_system.h>
#include <sys/stat.h>
#include <unistd.h>
#include <string>

#include "asset_list.hpp"

static void ExtractAsset(const char* assetFile, const std::string& destFile) {
  // If destination file already exists, check if it's 0 bytes
  struct stat st;
  if (stat(destFile.c_str(), &st) == 0 && st.st_size == 0) {
    remove(destFile.c_str());
  }

  SDL_RWops* src = SDL_RWFromFile(assetFile, "rb");
  if (!src) return;

  SDL_RWops* dst = SDL_RWFromFile(destFile.c_str(), "wb");
  if (!dst) {
    SDL_RWclose(src);
    return;
  }

  char buffer[8192];
  size_t read_bytes;
  size_t total_written = 0;
  while ((read_bytes = SDL_RWread(src, buffer, 1, sizeof(buffer))) > 0) {
    SDL_RWwrite(dst, buffer, 1, read_bytes);
    total_written += read_bytes;
  }
  SDL_RWclose(dst);
  SDL_RWclose(src);

  // If extraction wrote 0 bytes, remove the file so an empty file is never left on disk
  if (total_written == 0) {
    remove(destFile.c_str());
  }
}

void InitAndroidAssets() {
  std::string internalPath = SDL_AndroidGetInternalStoragePath();
  
  if (access((internalPath + "/football.config").c_str(), F_OK) == -1) {
    for (int i = 0; asset_dirs[i] != nullptr; i++) {
      mkdir((internalPath + "/" + asset_dirs[i]).c_str(), 0777);
    }
    for (int i = 0; asset_files[i] != nullptr; i++) {
      ExtractAsset(asset_files[i], internalPath + "/" + asset_files[i]);
    }
  }

  // Force overwrite critical config, shaders, and player model files so updates in the APK take effect immediately
  mkdir((internalPath + "/media").c_str(), 0777);
  mkdir((internalPath + "/media/objects").c_str(), 0777);
  mkdir((internalPath + "/media/objects/players").c_str(), 0777);
  mkdir((internalPath + "/media/objects/players/models").c_str(), 0777);
  mkdir((internalPath + "/media/objects/players/textures").c_str(), 0777);
  mkdir((internalPath + "/media/objects/players/textures/yamal").c_str(), 0777);

  // Explicitly remove stale or 0-byte disk files before extracting so SDL_RWFromFile reads directly from APK
  remove((internalPath + "/media/objects/players/fullbody.object").c_str());
  remove((internalPath + "/media/objects/players/models/fullbody.fbx").c_str());
  remove((internalPath + "/media/objects/players/models/fullbody.glb").c_str());
  remove((internalPath + "/media/shaders/ambient.frag").c_str());

  ExtractAsset("football.config", internalPath + "/football.config");
  ExtractAsset("media/objects/players/fullbody.object", internalPath + "/media/objects/players/fullbody.object");
  ExtractAsset("media/objects/players/models/fullbody.fbx", internalPath + "/media/objects/players/models/fullbody.fbx");
  ExtractAsset("media/objects/players/models/fullbody.glb", internalPath + "/media/objects/players/models/fullbody.glb");

  const char* yamal_textures[] = {
    "media/objects/players/textures/yamal/yamal_body.jpg",
    "media/objects/players/textures/yamal/yamal_bottom.jpg",
    "media/objects/players/textures/yamal/yamal_eye.jpg",
    "media/objects/players/textures/yamal/yamal_head.jpg",
    "media/objects/players/textures/yamal/yamal_shoes.jpg",
    "media/objects/players/textures/yamal/yamal_top.jpg",
    nullptr
  };
  for (int i = 0; yamal_textures[i] != nullptr; i++) {
    remove((internalPath + "/" + yamal_textures[i]).c_str());
    ExtractAsset(yamal_textures[i], internalPath + "/" + yamal_textures[i]);
  }

  chdir(internalPath.c_str());
}
#endif
