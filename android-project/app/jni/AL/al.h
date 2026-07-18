#pragma once
#include <math.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef unsigned int ALuint;
typedef int ALint;
typedef float ALfloat;
typedef int ALenum;
typedef char ALboolean;
typedef int ALsizei;
typedef void ALvoid;

#define AL_NONE 0
#define AL_FALSE 0
#define AL_TRUE 1
#define AL_NO_ERROR 0
#define AL_SOURCE_RELATIVE 0x202
#define AL_POSITION 0x1004
#define AL_VELOCITY 0x1006
#define AL_ORIENTATION 0x100F
#define AL_PITCH 0x1003
#define AL_GAIN 0x100A
#define AL_LOOPING 0x1007
#define AL_BUFFER 0x1009
#define AL_FORMAT_MONO8 0x1100
#define AL_FORMAT_MONO16 0x1101
#define AL_FORMAT_STEREO8 0x1102
#define AL_FORMAT_STEREO16 0x1103
#define AL_PLAYING 0x1015
#define AL_SOURCE_STATE 0x1010

inline ALenum alGetError() { return AL_NO_ERROR; }
inline void alGenSources(ALsizei n, ALuint *sources) { for(int i=0;i<n;++i) sources[i]=1; }
inline void alDeleteSources(ALsizei n, const ALuint *sources) {}
inline void alGenBuffers(ALsizei n, ALuint *buffers) { for(int i=0;i<n;++i) buffers[i]=1; }
inline void alDeleteBuffers(ALsizei n, const ALuint *buffers) {}
inline void alBufferData(ALuint buffer, ALenum format, const ALvoid *data, ALsizei size, ALsizei freq) {}
inline void alSourcei(ALuint source, ALenum param, ALint value) {}
inline void alSourcef(ALuint source, ALenum param, ALfloat value) {}
inline void alSourcefv(ALuint source, ALenum param, const ALfloat *values) {}
inline void alSourcePlay(ALuint source) {}
inline void alSourceStop(ALuint source) {}
inline void alGetSourcei(ALuint source, ALenum param, ALint *value) {}
inline void alListenerfv(ALenum param, const ALfloat *values) {}

#ifdef __cplusplus
}
#endif
