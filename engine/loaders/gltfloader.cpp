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

#define TINYGLTF_IMPLEMENTATION
#define STB_IMAGE_IMPLEMENTATION
#define STB_IMAGE_WRITE_IMPLEMENTATION
#include "../../third_party/tinygltf/tiny_gltf.h"

namespace blunted {

  GLTFLoader::GLTFLoader() : Loader<GeometryData>() {
  }

  GLTFLoader::~GLTFLoader() {
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

    Sint64 fileSize = SDL_RWsize(rw);
    if (fileSize <= 0) {
      Log(e_Error, "GLTFLoader", "Load", "Empty glTF file: " + filename);
      SDL_RWclose(rw);
      return;
    }

    std::vector<unsigned char> fileBuffer(fileSize);
    size_t readBytes = SDL_RWread(rw, fileBuffer.data(), 1, fileSize);
    SDL_RWclose(rw);

    if (readBytes != (size_t)fileSize) {
      Log(e_Error, "GLTFLoader", "Load", "Failed to read complete file: " + filename);
      return;
    }

    std::string baseDir = "";
    size_t lastSlash = filename.find_last_of("/\\");
    if (lastSlash != std::string::npos) {
      baseDir = filename.substr(0, lastSlash);
    }

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

    // Process each mesh and primitive as an independent submesh (matching engine architecture)
    for (size_t m = 0; m < model.meshes.size(); m++) {
      const auto &mesh = model.meshes[m];

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
            j0 = ptr[0]; j1 = ptr[1]; j2 = ptr[2];
          } else if (acc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_SHORT) {
            const uint16_t *u16 = reinterpret_cast<const uint16_t*>(ptr);
            j0 = u16[0]; j1 = u16[1]; j2 = u16[2];
          } else if (acc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_INT) {
            const uint32_t *u32 = reinterpret_cast<const uint32_t*>(ptr);
            j0 = (float)u32[0]; j1 = (float)u32[1]; j2 = (float)u32[2];
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

          // Diffuse / BaseColor Texture
          if (gltfMat.pbrMetallicRoughness.baseColorTexture.index >= 0) {
            int texIdx = gltfMat.pbrMetallicRoughness.baseColorTexture.index;
            if (texIdx >= 0 && texIdx < (int)model.textures.size()) {
              int imgIdx = model.textures[texIdx].source;
              if (imgIdx >= 0 && imgIdx < (int)model.images.size()) {
                const auto &img = model.images[imgIdx];

                // Detect dynamic player material roles (kit, skin, shoe)
                std::string lowerImgName = img.name;
                std::transform(lowerImgName.begin(), lowerImgName.end(), lowerImgName.begin(), ::tolower);
                std::string lowerMatName = gltfMat.name;
                std::transform(lowerMatName.begin(), lowerMatName.end(), lowerMatName.begin(), ::tolower);
                std::string lowerUri = img.uri;
                std::transform(lowerUri.begin(), lowerUri.end(), lowerUri.begin(), ::tolower);

                std::string dynamicIdent = "";
                if (lowerImgName.find("kit") != std::string::npos || lowerMatName.find("kit") != std::string::npos || lowerUri.find("kit") != std::string::npos) {
                  dynamicIdent = "kit_template.png";
                } else if (lowerImgName.find("skin") != std::string::npos || lowerMatName.find("skin") != std::string::npos || lowerUri.find("skin") != std::string::npos) {
                  dynamicIdent = "skin.jpg";
                } else if (lowerImgName.find("shoe") != std::string::npos || lowerMatName.find("shoe") != std::string::npos || lowerUri.find("shoe") != std::string::npos) {
                  dynamicIdent = "shoe.jpg";
                }

                if (!img.image.empty() && img.width > 0 && img.height > 0) {
                  // Embedded binary image in GLB
                  std::string texIdent = !dynamicIdent.empty() ? dynamicIdent : (filename + "#img_" + int_to_str(imgIdx));
                  bool alreadyThere = false;
                  boost::intrusive_ptr<Resource<Surface>> texRes = ResourceManagerPool::GetInstance().GetManager<Surface>(e_ResourceType_Surface)->Fetch(texIdent, false, alreadyThere, true);
                  if (!alreadyThere) {
                    SDL_Surface *sdlSurf = SDL_CreateRGBSurfaceWithFormat(0, img.width, img.height, 32, SDL_PIXELFORMAT_RGBA32);
                    if (sdlSurf) {
                      SDL_LockSurface(sdlSurf);
                      memcpy(sdlSurf->pixels, img.image.data(), img.width * img.height * 4);
                      SDL_UnlockSurface(sdlSurf);
                      texRes->GetResource()->SetData(sdlSurf);
                    }
                  }
                  material.diffuseTexture = texRes;
                } else if (!img.uri.empty()) {
                  // External texture URI
                  std::string texPath = !dynamicIdent.empty() ? ("media/objects/players/textures/" + dynamicIdent) : (baseDir.empty() ? img.uri : (baseDir + "/" + img.uri));
                  material.diffuseTexture = ResourceManagerPool::GetInstance().GetManager<Surface>(e_ResourceType_Surface)->Fetch(texPath, true, true);
                }
              }
            }
          }

          // Normal Texture
          if (gltfMat.normalTexture.index >= 0) {
            int normIdx = gltfMat.normalTexture.index;
            if (normIdx >= 0 && normIdx < (int)model.textures.size()) {
              int imgIdx = model.textures[normIdx].source;
              if (imgIdx >= 0 && imgIdx < (int)model.images.size()) {
                const auto &img = model.images[imgIdx];
                if (!img.image.empty() && img.width > 0 && img.height > 0) {
                  std::string normIdent = filename + "#norm_" + int_to_str(imgIdx);
                  bool alreadyThere = false;
                  boost::intrusive_ptr<Resource<Surface>> normRes = ResourceManagerPool::GetInstance().GetManager<Surface>(e_ResourceType_Surface)->Fetch(normIdent, false, alreadyThere, true);
                  if (!alreadyThere) {
                    SDL_Surface *sdlSurf = SDL_CreateRGBSurfaceWithFormat(0, img.width, img.height, 32, SDL_PIXELFORMAT_RGBA32);
                    if (sdlSurf) {
                      SDL_LockSurface(sdlSurf);
                      memcpy(sdlSurf->pixels, img.image.data(), img.width * img.height * 4);
                      SDL_UnlockSurface(sdlSurf);
                      normRes->GetResource()->SetData(sdlSurf);
                    }
                  }
                  material.normalTexture = normRes;
                } else if (!img.uri.empty()) {
                  std::string normPath = baseDir.empty() ? img.uri : (baseDir + "/" + img.uri);
                  material.normalTexture = ResourceManagerPool::GetInstance().GetManager<Surface>(e_ResourceType_Surface)->Fetch(normPath, true, true);
                }
              }
            }
          }

          // PBR Roughness to blinn-phong specular mapping
          material.specular_amount = (float)std::max(0.01, 1.0 - gltfMat.pbrMetallicRoughness.roughnessFactor);
          material.shininess = (float)std::max(0.01, 1.0 - gltfMat.pbrMetallicRoughness.roughnessFactor);
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
