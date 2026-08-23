// written by bastiaan konings schuiling 2008 - 2014
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#include "wavloader.hpp"

#include "engine/core/log.hpp"

#include <fstream>
#include <SDL2/SDL.h>

namespace blunted {

  WAVLoader::WAVLoader() : Loader<SoundBuffer>() {
  }

  WAVLoader::~WAVLoader() {
  }

  // load file into resource
  void WAVLoader::Load(std::string filename, boost::intrusive_ptr < Resource <SoundBuffer> > resource) {

    int channels, bits;
    unsigned int frequency;
    long BUFFER_SIZE = 1024 * 1024 * 4;

    SDL_RWops *f = SDL_RWFromFile(filename.c_str(), "rb");
    if (!f) {
      Log(e_FatalError, "WAVLoader", "Load", "Could not load " + filename + ": file not found");
      return;
    }

    unsigned char *buf;
    buf = new unsigned char[BUFFER_SIZE];

    SDL_RWread(f, buf, 1, 12); // ignore wave header
    SDL_RWread(f, buf, 1, 8); // 4 identifier, 4 chunk size (ignore)
    if (buf[0] != 'f' || buf[1] != 'm' || buf[2] != 't' || buf[3] != ' ') {
      SDL_RWclose(f);
      Log(e_FatalError, "WAVLoader", "Load", "Could not load " + filename + ": format information header incorrect");
      delete [] buf;
      return;
    }

    SDL_RWread(f, buf, 1, 2);

    if (buf[0] != 1 || buf[1] != 0) {
      SDL_RWclose(f);
      Log(e_FatalError, "WAVLoader", "Load", "Could not load " + filename + ": not PCM");
      delete [] buf;
      return;
    }

    SDL_RWread(f, buf, 1, 2);
    channels  = buf[1] << 8;
    channels |= buf[0];

    SDL_RWread(f, buf, 1, 4);
    frequency  = buf[3] << 24;
    frequency |= buf[2] << 16;
    frequency |= buf[1] << 8;
    frequency |= buf[0];

    SDL_RWread(f, buf, 1, 6); // blocksize / bps (ignore)

    SDL_RWread(f, buf, 1, 2);
    bits  = buf[1] << 8;
    bits |= buf[0];

    SDL_RWread(f, buf, 1, 8); // 4 identifier, 4 chunk size (ignore)
    if (buf[0] != 'd' || buf[1] != 'a' || buf[2] != 't' || buf[3] != 'a') {
      SDL_RWclose(f);
      Log(e_FatalError, "WAVLoader", "Load", "Could not load " + filename + ": data chunk not found");
      delete [] buf;
      return;
    }

    int size = SDL_RWread(f, buf, 1, BUFFER_SIZE);

    WavData *data = new WavData();
    data->data = new unsigned char[size];
    memcpy(data->data, buf, size * sizeof(unsigned char));
    data->size = size;
    data->channels = channels;
    data->bits = bits;
    data->frequency = frequency;

    //printf("wav data: %i bytes, %i channels, %i bits, %u freq\n", size * sizeof(unsigned char), channels, bits, frequency);

    resource->GetResource()->SetData(data);

    SDL_RWclose(f);
    delete [] buf;
  }

}
