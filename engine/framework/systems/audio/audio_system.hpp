// written by bastiaan konings schuiling 2008 - 2014
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#ifndef _HPP_SYSTEMS_AUDIO_SYSTEM
#define _HPP_SYSTEMS_AUDIO_SYSTEM

#include "engine/defines.hpp"

#include "engine/framework/systems/isystem.hpp"
#include "engine/framework/systems/isystemscene.hpp"
#include "engine/framework/systems/audio/rendering/openal_renderer.hpp"

#include "engine/scene/iscene.hpp"

#include "resources/audio_soundbuffer.hpp"

#include "engine/framework/managers/resourcemanager.hpp"

namespace blunted {

  class AudioRenderer;

  class AudioSystem : public ISystem {

    public:
      AudioSystem();
      virtual ~AudioSystem();

      virtual void Initialize(const Properties &config);
      virtual void Exit();

      e_SystemType GetSystemType() const;

      virtual ISystemScene *CreateSystemScene(boost::shared_ptr<IScene> scene);

      virtual ISystemTask *GetTask();
      virtual AudioRenderer *GetAudioRenderer();

      boost::shared_ptr < ResourceManager<AudioSoundBuffer> > GetAudioSoundBufferResourceManager();

      virtual std::string GetName() const { return "audio"; }

    protected:
      const e_SystemType systemType;

      AudioRenderer *rendererTask;

      boost::shared_ptr < ResourceManager<AudioSoundBuffer> > audioSoundBufferResourceManager;

  };

}

#endif
