// Custom GLTF loader using tinygltf
#ifndef _HPP_LOADERS_GLTF
#define _HPP_LOADERS_GLTF

#include "defines.hpp"
#include "base/utils.hpp"
#include "managers/resourcemanager.hpp"
#include "scene/resources/geometrydata.hpp"
#include "scene/objects/geometry.hpp"

namespace blunted {

  class GLTFLoader : public Loader<GeometryData> {
    public:
      GLTFLoader();
      virtual ~GLTFLoader();

      // encapsulating load function
      virtual void Load(std::string filename, boost::intrusive_ptr < Resource <GeometryData> > resource);
  };

}

#endif
