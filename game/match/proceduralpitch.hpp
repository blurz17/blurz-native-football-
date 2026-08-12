#ifndef _HPP_PROCEDURALPITCH
#define _HPP_PROCEDURALPITCH

#include "engine/defines.hpp"
#include "engine/core/sdl_surface.hpp"

using namespace blunted;

#ifndef GPFB_HEADLESS
Uint32 GetPitchDiffuseColor(SDL_Surface *pitchSurf, float xCoord, float yCoord);
Uint32 GetPitchSpecularColor(SDL_Surface *pitchSurf, float xCoord, float yCoord);
Uint32 GetPitchNormalColor(SDL_Surface *pitchSurf, float xCoord, float yCoord);
void DrawLines(SDL_PixelFormat *pixelFormat, Uint32 *diffuseBitmap, int resX, int resY, signed int offsetW, signed int offsetH);
void GeneratePitch(int resX, int resY, int resSpecularX, int resSpecularY, int resNormalX, int resNormalY);
#else
inline void GeneratePitch(int resX, int resY, int resSpecularX, int resSpecularY, int resNormalX, int resNormalY) {}
#endif

#endif
