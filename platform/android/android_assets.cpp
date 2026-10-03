#ifdef __ANDROID__
#include "android_assets.hpp"

#include <SDL2/SDL.h>
#include <SDL2/SDL_system.h>
#include <sys/stat.h>
#include <unistd.h>
#include <string>

#include "asset_list.hpp"

static void ExtractAsset(const char* assetFile, const std::string& destFile) {
  SDL_RWops* src = SDL_RWFromFile(assetFile, "rb");
  if (src) {
    SDL_RWops* dst = SDL_RWFromFile(destFile.c_str(), "wb");
    if (dst) {
      char buffer[4096];
      size_t read_bytes;
      while ((read_bytes = SDL_RWread(src, buffer, 1, sizeof(buffer))) > 0) {
        SDL_RWwrite(dst, buffer, 1, read_bytes);
      }
      SDL_RWclose(dst);
    }
    SDL_RWclose(src);
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

  // Force delete extracted ambient.frag so it falls back to the updated APK asset
  remove((internalPath + "/media/shaders/ambient.frag").c_str());
  
  chdir(internalPath.c_str());
}
#endif
