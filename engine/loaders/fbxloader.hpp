// Custom FBX loader using ufbx
#ifndef _HPP_LOADERS_FBX
#define _HPP_LOADERS_FBX

#include "defines.hpp"
#include "base/utils.hpp"
#include "managers/resourcemanager.hpp"
#include "scene/resources/geometrydata.hpp"
#include "scene/objects/geometry.hpp"

namespace blunted {

  class FBXLoader : public Loader<GeometryData> {
    public:
      FBXLoader();
      virtual ~FBXLoader();

      // encapsulating load function
      virtual void Load(std::string filename, boost::intrusive_ptr < Resource <GeometryData> > resource);
  };

}

#endif
