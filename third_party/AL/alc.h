#pragma once
#include "al.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct ALCdevice_struct ALCdevice;
typedef struct ALCcontext_struct ALCcontext;

inline ALCdevice* alcOpenDevice(const char *devicename) { return (ALCdevice*)1; }
inline ALCdevice* alcGetContextsDevice(ALCcontext *context) { return (ALCdevice*)1; }
inline ALCcontext* alcCreateContext(ALCdevice *device, const ALint* attrlist) { return (ALCcontext*)1; }
inline ALboolean alcMakeContextCurrent(ALCcontext *context) { return AL_TRUE; }
inline void alcDestroyContext(ALCcontext *context) {}
inline ALboolean alcCloseDevice(ALCdevice *device) { return AL_TRUE; }

#ifdef __cplusplus
}
#endif
