// written by bastiaan konings schuiling 2008 - 2014
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#include "openal_renderer.hpp"

#include "engine/core/log.hpp"
#include "engine/core/types/command.hpp"

#include "engine/framework/systems/audio/resources/audio_soundbuffer.hpp"

namespace blunted {

  OpenALRenderer::OpenALRenderer() : context(0) {
  };

  OpenALRenderer::~OpenALRenderer() {
  };


  // init & exit

  bool OpenALRenderer::CreateContext() {
    return true;
  }

  void OpenALRenderer::Exit() {
  }


  int OpenALRenderer::CreateAudioSoundBuffer(const WavData *wavData) {
    return 1;
  }

  void OpenALRenderer::DeleteAudioSoundBuffer(int audioSoundBufferID) {
  }

  void OpenALRenderer::PlayAudioSoundBuffer(int audioSoundBufferID) {
  }

  void OpenALRenderer::SetListenerParameters(const Vector3 &position, const Vector3 &velocity, const Quaternion &orientation) {
  }

  void OpenALRenderer::SetSourceParameter(int audioSoundBufferID, e_AudioRenderer_SourceParameter parameter, float value) {
  }


  // thread main loop

  void OpenALRenderer::operator()() {
    Log(e_Notice, "OpenALRenderer", "operator()()", "Starting OpenALRenderer thread");

    bool quit = false;
    while (!quit) {
      boost::intrusive_ptr<Command> message = messageQueue.WaitForMessage();
      if (!message->Handle(this)) quit = true;
      message.reset();
    }

    Exit();

    Log(e_Notice, "OpenALRenderer", "operator()()", "Shutting down OpenALRenderer thread");

    if (messageQueue.GetPending() > 0) Log(e_Error, "OpenALRenderer", "operator()()", int_to_str(messageQueue.GetPending()) + " messages left on quit!");
  }

}
