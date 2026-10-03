// written by bastiaan konings schuiling 2008 - 2014
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#include "imageloader.hpp"

#include "base/log.hpp"

#include <fstream>

namespace blunted {

  ImageLoader::ImageLoader() : Loader<Surface>() {
  }

  ImageLoader::~ImageLoader() {
  }

  // load file into resource
  void ImageLoader::Load(std::string filename, boost::intrusive_ptr < Resource <Surface> > resource) {
    SDL_Surface *surface = IMG_Load(filename.c_str());
    if (!surface) {
      Log(e_Error, "ImageLoader", "Load", "Could not load " + filename + ": " + (IMG_GetError() ? IMG_GetError() : "Unknown error"));
      // fallback to a 2x2 magenta texture so the game doesn't crash
      surface = SDL_CreateRGBSurface(0, 2, 2, 32, 0x000000ff, 0x0000ff00, 0x00ff0000, 0xff000000);
      SDL_FillRect(surface, NULL, SDL_MapRGBA(surface->format, 255, 0, 255, 255));
    }
    
    SDL_Surface *converted = SDL_ConvertSurfaceFormat(surface, SDL_PIXELFORMAT_RGBA32, 0);
    SDL_FreeSurface(surface);
    
    resource->GetResource()->SetData(converted);
  }

}
