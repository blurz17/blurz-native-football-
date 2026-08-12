// written by bastiaan konings schuiling 2008 - 2014
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#ifndef _HPP_LOADERS_WAVE
#define _HPP_LOADERS_WAVE

#include "engine/defines.hpp"
#include "engine/framework/managers/resourcemanager.hpp"
#include "engine/scene/resources/soundbuffer.hpp"
#include "engine/scene/objects/sound.hpp"

namespace blunted {

  class WAVLoader : public Loader<SoundBuffer> {

    public:
      WAVLoader();
      virtual ~WAVLoader();

      virtual void Load(std::string filename, boost::intrusive_ptr < Resource <SoundBuffer> > resource);

    protected:

  };

}

#endif
