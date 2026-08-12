#ifndef _HPP_MANAGERS_SCENE
#define _HPP_MANAGERS_SCENE

#include "engine/defines.hpp"

#include "engine/core/types/singleton.hpp"
#include "engine/core/types/lockable.hpp"
#include "engine/scene/scene.hpp"

namespace blunted {

  typedef Lockable < std::vector < boost::shared_ptr<IScene> > > vector_Scenes;

  class SceneManager : public Singleton<SceneManager> {

    friend class Singleton<SceneManager>;

    public:
      SceneManager();
      ~SceneManager();
      
      virtual void Exit();

      void RegisterScene(boost::shared_ptr<IScene> scene);
      int GetNumScenes();
      boost::shared_ptr<IScene> GetScene(int index, bool &success); // ATOMIC
      boost::shared_ptr<IScene> GetScene(const std::string &name, bool &success); // ATOMIC
      
    protected:
      vector_Scenes scenes;

    private:

  };

}

#endif
