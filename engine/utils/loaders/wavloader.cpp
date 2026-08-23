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
    WavData *data = new WavData();
    data->data = new unsigned char[1];
    data->size = 1;
    data->channels = 1;
    data->bits = 8;
    data->frequency = 44100;
    resource->GetResource()->SetData(data);
  }

}
