#include "gltfloader.hpp"

#include "base/log.hpp"
#include "base/geometry/triangle.hpp"
#include "base/geometry/trianglemeshutils.hpp"
#include "managers/resourcemanagerpool.hpp"
#include "scene/resources/surface.hpp"

#include <SDL2/SDL.h>
#include <SDL2/SDL_image.h>
#include <vector>
#include <string>

#ifdef __ANDROID__
#include <sys/stat.h>
#include <unistd.h>
#include <SDL2/SDL_system.h>
#endif

#define TINYGLTF_IMPLEMENTATION
#define STB_IMAGE_IMPLEMENTATION
#define STB_IMAGE_WRITE_IMPLEMENTATION
#include "../../third_party/tinygltf/tiny_gltf.h"

namespace blunted {

  GLTFLoader::GLTFLoader() : Loader<GeometryData>() {
  }

  GLTFLoader::~GLTFLoader() {
  }

  static int MapGLTFBoneNameToJointId(const std::string &boneName) {
    std::string name = boneName;
    std::transform(name.begin(), name.end(), name.begin(), ::tolower);
    if (name.find("neck") != std::string::npos || name.find("head") != std::string::npos || name.find("eye") != std::string::npos || name.find("jaw") != std::string::npos) return 2;
    if (name.find("body") != std::string::npos || name.find("pelvis") != std::string::npos || name.find("hips") != std::string::npos || name.find("root") != std::string::npos) return 0;
    if (name.find("middle") != std::string::npos || name.find("spine") != std::string::npos || name.find("trunk") != std::string::npos || name.find("chest") != std::string::npos) return 1;
    bool isLeft = (name.find("left") != std::string::npos || name.find(".l") != std::string::npos || name.find("_l") != std::string::npos);
    bool isRight = (name.find("right") != std::string::npos || name.find(".r") != std::string::npos || name.find("_r") != std::string::npos);
    if (isLeft) {
      if (name.find("shoulder") != std::string::npos || (name.find("arm") != std::string::npos && name.find("fore") == std::string::npos && name.find("lower") == std::string::npos && name.find("elbow") == std::string::npos)) return 3;
      if (name.find("hand") != std::string::npos || name.find("wrist") != std::string::npos) return 13;
      if (name.find("elbow") != std::string::npos || name.find("forearm") != std::string::npos || name.find("lowerarm") != std::string::npos) return 4;
      if (name.find("upleg") != std::string::npos || name.find("thigh") != std::string::npos || name.find("upperleg") != std::string::npos) return 7;
      if (name.find("knee") != std::string::npos || name.find("lowerleg") != std::string::npos || name.find("calf") != std::string::npos || name.find("shin") != std::string::npos || (name.find("leg") != std::string::npos && name.find("foot") == std::string::npos && name.find("up") == std::string::npos)) return 8;
      if (name.find("toe") != std::string::npos) return 15;
      if (name.find("ankle") != std::string::npos || name.find("foot") != std::string::npos) return 9;
    }
    if (isRight) {
      if (name.find("shoulder") != std::string::npos || (name.find("arm") != std::string::npos && name.find("fore") == std::string::npos && name.find("lower") == std::string::npos && name.find("elbow") == std::string::npos)) return 5;
      if (name.find("hand") != std::string::npos || name.find("wrist") != std::string::npos) return 14;
      if (name.find("elbow") != std::string::npos || name.find("forearm") != std::string::npos || name.find("lowerarm") != std::string::npos) return 6;
      if (name.find("upleg") != std::string::npos || name.find("thigh") != std::string::npos || name.find("upperleg") != std::string::npos) return 10;
      if (name.find("knee") != std::string::npos || name.find("lowerleg") != std::string::npos || name.find("calf") != std::string::npos || name.find("shin") != std::string::npos || (name.find("leg") != std::string::npos && name.find("foot") == std::string::npos && name.find("up") == std::string::npos)) return 11;
      if (name.find("toe") != std::string::npos) return 16;
      if (name.find("ankle") != std::string::npos || name.find("foot") != std::string::npos) return 12;
    }
    return 0;
  }

  void GLTFLoader::Load(std::string filename, boost::intrusive_ptr < Resource <GeometryData> > resource) {
    tinygltf::Model model;
    tinygltf::TinyGLTF loader;
    std::string err;
    std::string warn;

    // Use SDL_RWops to load file from disk, APK asset, or memory
    SDL_RWops *rw = SDL_RWFromFile(filename.c_str(), "rb");
    if (!rw) {
      Log(e_Error, "GLTFLoader", "Load", "Could not open file: " + filename + " (" + SDL_GetError() + ")");
      return;
    }

    std::vector<unsigned char> fileBuffer;
    char chunk[8192];
    size_t readBytes;
    while ((readBytes = SDL_RWread(rw, chunk, 1, sizeof(chunk))) > 0) {
      fileBuffer.insert(fileBuffer.end(), chunk, chunk + readBytes);
    }
    SDL_RWclose(rw);

    if (fileBuffer.empty()) {
#ifdef __ANDROID__
      // Fallback: if empty due to a 0-byte file on disk, remove it and retry from APK asset
      std::string internalPath = SDL_AndroidGetInternalStoragePath();
      std::string diskPath = internalPath + "/" + filename;
      struct stat st;
      if (stat(diskPath.c_str(), &st) == 0 && st.st_size == 0) {
        remove(diskPath.c_str());
        rw = SDL_RWFromFile(filename.c_str(), "rb");
        if (rw) {
          while ((readBytes = SDL_RWread(rw, chunk, 1, sizeof(chunk))) > 0) {
            fileBuffer.insert(fileBuffer.end(), chunk, chunk + readBytes);
          }
          SDL_RWclose(rw);
        }
      }
#endif
    }

    if (fileBuffer.empty()) {
      Log(e_Error, "GLTFLoader", "Load", "Empty glTF file: " + filename);
      return;
    }

    std::string baseDir = "";
    size_t lastSlash = filename.find_last_of("/\\");
    if (lastSlash != std::string::npos) {
      baseDir = filename.substr(0, lastSlash);
    }

    loader.SetImageLoader([](tinygltf::Image *, const int, std::string *, std::string *, int, int, const unsigned char *, int, void *) { return true; }, nullptr);

    bool ret = false;
    if (filename.find(".glb") != std::string::npos) {
      ret = loader.LoadBinaryFromMemory(&model, &err, &warn, fileBuffer.data(), (unsigned int)fileBuffer.size(), baseDir);
    } else {
      ret = loader.LoadASCIIFromString(&model, &err, &warn, reinterpret_cast<const char*>(fileBuffer.data()), (unsigned int)fileBuffer.size(), baseDir);
    }

    if (!warn.empty()) {
      Log(e_Warning, "GLTFLoader", "Load", "Warning: " + warn);
    }

    if (!err.empty()) {
      Log(e_Error, "GLTFLoader", "Load", "Error: " + err);
    }

    if (!ret) {
      Log(e_Error, "GLTFLoader", "Load", "Failed to parse glTF: " + filename);
      return;
    }

    int totalTrianglesLoaded = 0;
    int submeshIndex = 0;

    // Build mapping of mesh index to skin index
    std::vector<int> meshToSkin(model.meshes.size(), -1);
    for (const auto &node : model.nodes) {
      if (node.mesh >= 0 && (size_t)node.mesh < meshToSkin.size()) {
        if (node.skin >= 0) {
          meshToSkin[node.mesh] = node.skin;
        }
      }
    }

    // Process each mesh and primitive as an independent submesh (matching engine architecture)
    for (size_t m = 0; m < model.meshes.size(); m++) {
      const auto &mesh = model.meshes[m];

      int skinIdx = meshToSkin[m];
      if (skinIdx < 0 && !model.skins.empty()) skinIdx = 0;

      auto mapJointIndexToEngineJointId = [&](int jointIdx) -> float {
        if (skinIdx >= 0 && (size_t)skinIdx < model.skins.size()) {
          const auto &skin = model.skins[skinIdx];
          if (jointIdx >= 0 && (size_t)jointIdx < skin.joints.size()) {
            int nodeIdx = skin.joints[jointIdx];
            if (nodeIdx >= 0 && (size_t)nodeIdx < model.nodes.size()) {
              const std::string &bName = model.nodes[nodeIdx].name;
              return (float)MapGLTFBoneNameToJointId(bName);
            }
          }
        }
        return (float)jointIdx;
      };

      for (size_t p = 0; p < mesh.primitives.size(); p++) {
        const auto &primitive = mesh.primitives[p];
        if (primitive.mode != TINYGLTF_MODE_TRIANGLES && primitive.mode != 0 /* default triangles */) continue;

        // Position attribute is mandatory
        if (primitive.attributes.find("POSITION") == primitive.attributes.end()) continue;
        int posAccIdx = primitive.attributes.at("POSITION");
        const tinygltf::Accessor &posAcc = model.accessors[posAccIdx];
        const tinygltf::BufferView &posView = model.bufferViews[posAcc.bufferView];
        const tinygltf::Buffer &posBuffer = model.buffers[posView.buffer];
        const float *positions = reinterpret_cast<const float*>(&posBuffer.data[posView.byteOffset + posAcc.byteOffset]);

        // Optional attributes
        const float *normals = nullptr;
        if (primitive.attributes.find("NORMAL") != primitive.attributes.end()) {
          const tinygltf::Accessor &normAcc = model.accessors[primitive.attributes.at("NORMAL")];
          const tinygltf::BufferView &normView = model.bufferViews[normAcc.bufferView];
          normals = reinterpret_cast<const float*>(&model.buffers[normView.buffer].data[normView.byteOffset + normAcc.byteOffset]);
        }

        const float *uvs = nullptr;
        if (primitive.attributes.find("TEXCOORD_0") != primitive.attributes.end()) {
          const tinygltf::Accessor &uvAcc = model.accessors[primitive.attributes.at("TEXCOORD_0")];
          const tinygltf::BufferView &uvView = model.bufferViews[uvAcc.bufferView];
          uvs = reinterpret_cast<const float*>(&model.buffers[uvView.buffer].data[uvView.byteOffset + uvAcc.byteOffset]);
        }

        int jointsAccIdx = -1;
        int weightsAccIdx = -1;
        if (primitive.attributes.find("JOINTS_0") != primitive.attributes.end()) jointsAccIdx = primitive.attributes.at("JOINTS_0");
        if (primitive.attributes.find("WEIGHTS_0") != primitive.attributes.end()) weightsAccIdx = primitive.attributes.at("WEIGHTS_0");

        auto getJoints = [&](int vertIdx) -> Vector3 {
          if (jointsAccIdx < 0) return Vector3(0, 0, 0);
          const tinygltf::Accessor &acc = model.accessors[jointsAccIdx];
          const tinygltf::BufferView &view = model.bufferViews[acc.bufferView];
          const tinygltf::Buffer &buf = model.buffers[view.buffer];
          size_t stride = view.byteStride ? view.byteStride : (acc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_BYTE ? 4 : (acc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_SHORT ? 8 : 16));
          const unsigned char *ptr = &buf.data[view.byteOffset + acc.byteOffset + vertIdx * stride];
          float j0 = 0, j1 = 0, j2 = 0;
          if (acc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_BYTE) {
            j0 = mapJointIndexToEngineJointId(ptr[0]);
            j1 = mapJointIndexToEngineJointId(ptr[1]);
            j2 = mapJointIndexToEngineJointId(ptr[2]);
          } else if (acc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_SHORT) {
            const uint16_t *u16 = reinterpret_cast<const uint16_t*>(ptr);
            j0 = mapJointIndexToEngineJointId(u16[0]);
            j1 = mapJointIndexToEngineJointId(u16[1]);
            j2 = mapJointIndexToEngineJointId(u16[2]);
          } else if (acc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_INT) {
            const uint32_t *u32 = reinterpret_cast<const uint32_t*>(ptr);
            j0 = mapJointIndexToEngineJointId(u32[0]);
            j1 = mapJointIndexToEngineJointId(u32[1]);
            j2 = mapJointIndexToEngineJointId(u32[2]);
          }
          return Vector3(j0, j1, j2);
        };

        auto getWeights = [&](int vertIdx) -> Vector3 {
          if (weightsAccIdx < 0) return Vector3(1.0f, 0, 0);
          const tinygltf::Accessor &acc = model.accessors[weightsAccIdx];
          const tinygltf::BufferView &view = model.bufferViews[acc.bufferView];
          const tinygltf::Buffer &buf = model.buffers[view.buffer];
          size_t stride = view.byteStride ? view.byteStride : (acc.componentType == TINYGLTF_COMPONENT_TYPE_FLOAT ? 16 : (acc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_SHORT ? 8 : 4));
          const unsigned char *ptr = &buf.data[view.byteOffset + acc.byteOffset + vertIdx * stride];
          float w0 = 1.0f, w1 = 0, w2 = 0;
          if (acc.componentType == TINYGLTF_COMPONENT_TYPE_FLOAT) {
            const float *f = reinterpret_cast<const float*>(ptr);
            w0 = f[0]; w1 = f[1]; w2 = f[2];
          } else if (acc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_BYTE) {
            w0 = ptr[0] / 255.0f; w1 = ptr[1] / 255.0f; w2 = ptr[2] / 255.0f;
          } else if (acc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_SHORT) {
            const uint16_t *u16 = reinterpret_cast<const uint16_t*>(ptr);
            w0 = u16[0] / 65535.0f; w1 = u16[1] / 65535.0f; w2 = u16[2] / 65535.0f;
          }
          float sum = w0 + w1 + w2;
          if (sum > 0.0001f) {
            w0 /= sum; w1 /= sum; w2 /= sum;
          } else {
            w0 = 1.0f; w1 = 0.0f; w2 = 0.0f;
          }
          return Vector3(w0, w1, w2);
        };

        std::vector<Triangle*> triangles;

        auto addTriangle = [&](int i0, int i1, int i2) {
          Triangle *tri = new Triangle();
          // Convert glTF Y-up (+Y up, +Z fwd, +X right) to Engine right-handed Z-up (+Z up, +Y fwd, +X right)
          // engine_x = gltf_x, engine_y = -gltf_z, engine_z = gltf_y (pure 90 deg rotation around X, det=+1)
          tri->SetVertex(0, Vector3(positions[i0 * 3], -positions[i0 * 3 + 2], positions[i0 * 3 + 1]));
          tri->SetVertex(1, Vector3(positions[i1 * 3], -positions[i1 * 3 + 2], positions[i1 * 3 + 1]));
          tri->SetVertex(2, Vector3(positions[i2 * 3], -positions[i2 * 3 + 2], positions[i2 * 3 + 1]));

          if (normals) {
            tri->SetNormal(0, normals[i0 * 3], -normals[i0 * 3 + 2], normals[i0 * 3 + 1]);
            tri->SetNormal(1, normals[i1 * 3], -normals[i1 * 3 + 2], normals[i1 * 3 + 1]);
            tri->SetNormal(2, normals[i2 * 3], -normals[i2 * 3 + 2], normals[i2 * 3 + 1]);
          }

          if (uvs) {
            // Flip V coordinate for standard OpenGL texture conventions
            tri->SetTextureVertex(0, 0, uvs[i0 * 2], 1.0f - uvs[i0 * 2 + 1], 0);
            tri->SetTextureVertex(0, 1, uvs[i1 * 2], 1.0f - uvs[i1 * 2 + 1], 0);
            tri->SetTextureVertex(0, 2, uvs[i2 * 2], 1.0f - uvs[i2 * 2 + 1], 0);
          }

          if (jointsAccIdx >= 0) {
            tri->SetTextureVertex(1, 0, getJoints(i0));
            tri->SetTextureVertex(1, 1, getJoints(i1));
            tri->SetTextureVertex(1, 2, getJoints(i2));
          }

          if (weightsAccIdx >= 0) {
            tri->SetTextureVertex(2, 0, getWeights(i0));
            tri->SetTextureVertex(2, 1, getWeights(i1));
            tri->SetTextureVertex(2, 2, getWeights(i2));
          }

          triangles.push_back(tri);
        };

        // Indexed vs non-indexed triangles
        if (primitive.indices >= 0) {
          const tinygltf::Accessor &indAcc = model.accessors[primitive.indices];
          const tinygltf::BufferView &indView = model.bufferViews[indAcc.bufferView];
          const tinygltf::Buffer &indBuffer = model.buffers[indView.buffer];

          if (indAcc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_SHORT) {
            const uint16_t *indices = reinterpret_cast<const uint16_t*>(&indBuffer.data[indView.byteOffset + indAcc.byteOffset]);
            for (size_t i = 0; i < indAcc.count; i += 3) {
              addTriangle(indices[i], indices[i + 1], indices[i + 2]);
            }
          } else if (indAcc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_INT) {
            const uint32_t *indices = reinterpret_cast<const uint32_t*>(&indBuffer.data[indView.byteOffset + indAcc.byteOffset]);
            for (size_t i = 0; i < indAcc.count; i += 3) {
              addTriangle(indices[i], indices[i + 1], indices[i + 2]);
            }
          } else if (indAcc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_BYTE) {
            const uint8_t *indices = reinterpret_cast<const uint8_t*>(&indBuffer.data[indView.byteOffset + indAcc.byteOffset]);
            for (size_t i = 0; i < indAcc.count; i += 3) {
              addTriangle(indices[i], indices[i + 1], indices[i + 2]);
            }
          }
        } else {
          for (size_t i = 0; i < posAcc.count; i += 3) {
            addTriangle(i, i + 1, i + 2);
          }
        }

        if (triangles.empty()) continue;

        totalTrianglesLoaded += triangles.size();

        int rawSize = triangles.size() * 3 * 3 * GetTriangleMeshElementCount();
        float *rawTriangleMesh = new float[rawSize];
        memset(rawTriangleMesh, 0, rawSize * sizeof(float));

        for (unsigned int t = 0; t < triangles.size(); t++) {
          triangles[t]->CalculateTangents();
          for (int v = 0; v < 3; v++) {
            memcpy(&rawTriangleMesh[0 * triangles.size() * 9 + t * 9 + v * 3], triangles[t]->GetVertex(v).coords, sizeof(float) * 3);
            memcpy(&rawTriangleMesh[1 * triangles.size() * 9 + t * 9 + v * 3], triangles[t]->GetNormal(v).coords, sizeof(float) * 3);
            memcpy(&rawTriangleMesh[2 * triangles.size() * 9 + t * 9 + v * 3], triangles[t]->GetTextureVertex(0, v).coords, sizeof(float) * 3);
            memcpy(&rawTriangleMesh[3 * triangles.size() * 9 + t * 9 + v * 3], triangles[t]->GetTangent(v).coords, sizeof(float) * 3);
            memcpy(&rawTriangleMesh[4 * triangles.size() * 9 + t * 9 + v * 3], triangles[t]->GetBiTangent(v).coords, sizeof(float) * 3);
            if (GetTriangleMeshElementCount() >= 7) {
              memcpy(&rawTriangleMesh[5 * triangles.size() * 9 + t * 9 + v * 3], triangles[t]->GetTextureVertex(1, v).coords, sizeof(float) * 3);
              memcpy(&rawTriangleMesh[6 * triangles.size() * 9 + t * 9 + v * 3], triangles[t]->GetTextureVertex(2, v).coords, sizeof(float) * 3);
            }
          }
        }

        for (unsigned int t = 0; t < triangles.size(); t++) {
          delete triangles.at(t);
        }

        // Material extraction
        Material material;
        material.diffuseTexture = ResourceManagerPool::GetInstance().GetManager<Surface>(e_ResourceType_Surface)->Fetch("media/textures/white.png", true, true);
        material.specular_amount = 0.05f;
        material.shininess = 0.1f;

        if (primitive.material >= 0 && primitive.material < (int)model.materials.size()) {
          const auto &gltfMat = model.materials[primitive.material];

          std::string lowerMatName = gltfMat.name;
          std::transform(lowerMatName.begin(), lowerMatName.end(), lowerMatName.begin(), ::tolower);

          std::string customTexPath = "";
          if (lowerMatName.find("kit") != std::string::npos) {
            customTexPath = "media/objects/players/textures/kit_template.png";
          } else if (lowerMatName.find("hair") != std::string::npos) {
            customTexPath = "media/objects/players/textures/alnassr_cr7_hair.png";
            material.specular_amount = 0.05f;
            material.shininess = 0.1f;
          } else if (lowerMatName.find("player") != std::string::npos ||
                     lowerMatName.find("cr7") != std::string::npos ||
                     lowerMatName.find("ronaldo") != std::string::npos) {
            customTexPath = "media/objects/players/textures/alnassr_cr7.png";
            material.specular_amount = 0.02f;
            material.shininess = 0.15f;
          } else if (lowerMatName.find("ball") != std::string::npos) {
            customTexPath = "media/objects/balls/ball.png";
            material.specular_amount = 0.05f;
            material.shininess = 0.10f;
          } else if (lowerMatName.find("head") != std::string::npos ||
                     lowerMatName.find("face") != std::string::npos) {
            customTexPath = "media/objects/players/textures/yamal/yamal_head.jpg";
            material.specular_amount = 0.03f;
            material.shininess = 0.12f;
          } else if (lowerMatName.find("eye") != std::string::npos) {
            customTexPath = "media/objects/players/textures/yamal/yamal_eye.jpg";
            material.specular_amount = 0.08f;
            material.shininess = 0.3f;
          } else if (lowerMatName.find("top") != std::string::npos ||
                     lowerMatName.find("shirt") != std::string::npos ||
                     lowerMatName.find("jersey") != std::string::npos) {
            customTexPath = "media/objects/players/textures/yamal/yamal_top.jpg";
            material.specular_amount = 0.01f;
            material.shininess = 0.05f;
          } else if (lowerMatName.find("bottom") != std::string::npos ||
                     lowerMatName.find("short") != std::string::npos) {
            customTexPath = "media/objects/players/textures/yamal/yamal_bottom.jpg";
            material.specular_amount = 0.01f;
            material.shininess = 0.05f;
          } else if (lowerMatName.find("shoe") != std::string::npos ||
                     lowerMatName.find("boot") != std::string::npos) {
            customTexPath = "media/objects/players/textures/yamal/yamal_shoes.jpg";
            material.specular_amount = 0.05f;
            material.shininess = 0.2f;
          } else if (lowerMatName.find("sole") != std::string::npos) {
            customTexPath = "media/objects/players/textures/shoe_sole.jpg";
          } else if (lowerMatName.find("skin") != std::string::npos ||
                     lowerMatName.find("arm") != std::string::npos ||
                     lowerMatName.find("body") != std::string::npos ||
                     lowerMatName.find("knee") != std::string::npos) {
            customTexPath = "media/objects/players/textures/yamal/yamal_body.jpg";
            material.specular_amount = 0.02f;
            material.shininess = 0.12f;
          }

          if (!customTexPath.empty()) {
            material.diffuseTexture = ResourceManagerPool::GetInstance().GetManager<Surface>(e_ResourceType_Surface)->Fetch(customTexPath, true, true);
          } else if (gltfMat.pbrMetallicRoughness.baseColorTexture.index >= 0) {
            int texIdx = gltfMat.pbrMetallicRoughness.baseColorTexture.index;
            if (texIdx >= 0 && texIdx < (int)model.textures.size()) {
              int imgIdx = model.textures[texIdx].source;
              if (imgIdx >= 0 && imgIdx < (int)model.images.size()) {
                const auto &img = model.images[imgIdx];
                if (!img.uri.empty()) {
                  std::string texPath = baseDir.empty() ? img.uri : (baseDir + "/" + img.uri);
                  material.diffuseTexture = ResourceManagerPool::GetInstance().GetManager<Surface>(e_ResourceType_Surface)->Fetch(texPath, true, true);
                }
              }
            }
          }

          // Normal maps for player body parts
          if (lowerMatName.find("arm") != std::string::npos) {
            material.normalTexture = ResourceManagerPool::GetInstance().GetManager<Surface>(e_ResourceType_Surface)->Fetch("media/objects/players/textures/arm_normal.png", true, true);
          } else if (lowerMatName.find("knee") != std::string::npos) {
            material.normalTexture = ResourceManagerPool::GetInstance().GetManager<Surface>(e_ResourceType_Surface)->Fetch("media/objects/players/textures/knee_normal.png", true, true);
          } else if (lowerMatName.find("kit") != std::string::npos) {
            material.normalTexture = ResourceManagerPool::GetInstance().GetManager<Surface>(e_ResourceType_Surface)->Fetch("media/objects/players/textures/kit_UVWnormal.png", true, true);
          }

          // PBR Roughness to blinn-phong specular fallback if not already set by custom material profile
          if (customTexPath.empty()) {
            material.specular_amount = (float)std::max(0.01, (1.0 - gltfMat.pbrMetallicRoughness.roughnessFactor) * 0.1);
            material.shininess = (float)std::max(0.01, (1.0 - gltfMat.pbrMetallicRoughness.roughnessFactor) * 0.2);
          }
        }

        resource->resourceMutex.lock();
        std::vector<unsigned int> emptyIndices;
        resource->GetResource()->AddTriangleMesh(material, rawTriangleMesh, rawSize, emptyIndices);
        resource->resourceMutex.unlock();

        submeshIndex++;
      }
    }

    Log(e_Notice, "GLTFLoader", "Load", "Loaded glTF: " + filename + " (" + int_to_str(submeshIndex) + " submeshes, " + int_to_str(totalTrianglesLoaded) + " triangles)");
  }

}
