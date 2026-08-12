// written by bastiaan konings schuiling 2008 - 2014
// this work is public domain. the code is undocumented, scruffy, untested, and should generally not be used for anything important.
// i do not offer support, so don't ask. to be used for inspiration :)

#ifndef _HPP_LOADERS_IMAGE
#define _HPP_LOADERS_IMAGE

#include "engine/defines.hpp"
#include "engine/framework/managers/resourcemanager.hpp"
#include "engine/scene/resources/surface.hpp"
#include "engine/scene/objects/image2d.hpp"

namespace blunted {

  class ImageLoader : public Loader<Surface> {

    public:
      ImageLoader();
      virtual ~ImageLoader();

      virtual void Load(std::string filename, boost::intrusive_ptr < Resource <Surface> > resource);

    protected:

  };

}

#endif
