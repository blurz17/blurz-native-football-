#include "gltfloader.hpp"

#include "base/log.hpp"
#include "base/geometry/triangle.hpp"
#include "base/geometry/trianglemeshutils.hpp"
#include "managers/resourcemanagerpool.hpp"
#include "scene/resources/surface.hpp"
#include <fstream>

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

    bool ret = false;
    
    // Note: Android assets logic might require SDL_RWops integration in tinygltf.
    // For now we just use the built in loader.
    if (filename.find(".glb") != std::string::npos) {
        ret = loader.LoadBinaryFromFile(&model, &err, &warn, filename);
    } else {
        ret = loader.LoadASCIIFromFile(&model, &err, &warn, filename);
    }

    if (!warn.empty()) {
      Log(e_Warning, "GLTFLoader", "Load", "Warning: " + warn);
    }

    if (!err.empty()) {
      Log(e_Error, "GLTFLoader", "Load", "Error: " + err);
    }

    if (!ret) {
      Log(e_Error, "GLTFLoader", "Load", "Failed to load glTF: " + filename);
      return;
    }

    int tmeshsize = 0;
    
    // First pass: count total triangles to allocate rawTriangleMesh.
    // For simplicity in this first iteration, we assume meshes only use triangles (mode 4).
    for (const auto& mesh : model.meshes) {
        for (const auto& primitive : mesh.primitives) {
            if (primitive.mode != TINYGLTF_MODE_TRIANGLES) continue;
            int accessorIdx = primitive.indices;
            if (accessorIdx >= 0) {
                const tinygltf::Accessor& accessor = model.accessors[accessorIdx];
                tmeshsize += accessor.count / 3; // triangles
            } else {
                // non-indexed
                auto it = primitive.attributes.find("POSITION");
                if (it != primitive.attributes.end()) {
                    const tinygltf::Accessor& accessor = model.accessors[it->second];
                    tmeshsize += accessor.count / 3; // triangles
                }
            }
        }
    }
    
    if (tmeshsize == 0) {
        Log(e_Error, "GLTFLoader", "Load", "No triangles found in glTF");
        return;
    }

    std::vector<Triangle*> triangles;

    // Second pass: extract vertices
    for (const auto& mesh : model.meshes) {
        for (const auto& primitive : mesh.primitives) {
            if (primitive.mode != TINYGLTF_MODE_TRIANGLES) continue;

            // Basic attribute accessors
            int posAccIdx = -1, normAccIdx = -1, uvAccIdx = -1;
            int jointsAccIdx = -1, weightsAccIdx = -1;
            if (primitive.attributes.count("POSITION")) posAccIdx = primitive.attributes.at("POSITION");
            if (primitive.attributes.count("NORMAL")) normAccIdx = primitive.attributes.at("NORMAL");
            if (primitive.attributes.count("TEXCOORD_0")) uvAccIdx = primitive.attributes.at("TEXCOORD_0");
            if (primitive.attributes.count("JOINTS_0")) jointsAccIdx = primitive.attributes.at("JOINTS_0");
            if (primitive.attributes.count("WEIGHTS_0")) weightsAccIdx = primitive.attributes.at("WEIGHTS_0");

            if (posAccIdx < 0) continue;

            const tinygltf::Accessor& posAcc = model.accessors[posAccIdx];
            const tinygltf::BufferView& posView = model.bufferViews[posAcc.bufferView];
            const tinygltf::Buffer& posBuffer = model.buffers[posView.buffer];
            const float* positions = reinterpret_cast<const float*>(&posBuffer.data[posView.byteOffset + posAcc.byteOffset]);

            const float* normals = nullptr;
            if (normAccIdx >= 0) {
                const tinygltf::Accessor& normAcc = model.accessors[normAccIdx];
                const tinygltf::BufferView& normView = model.bufferViews[normAcc.bufferView];
                const tinygltf::Buffer& normBuffer = model.buffers[normView.buffer];
                normals = reinterpret_cast<const float*>(&normBuffer.data[normView.byteOffset + normAcc.byteOffset]);
            }

            const float* uvs = nullptr;
            if (uvAccIdx >= 0) {
                const tinygltf::Accessor& uvAcc = model.accessors[uvAccIdx];
                const tinygltf::BufferView& uvView = model.bufferViews[uvAcc.bufferView];
                const tinygltf::Buffer& uvBuffer = model.buffers[uvView.buffer];
                uvs = reinterpret_cast<const float*>(&uvBuffer.data[uvView.byteOffset + uvAcc.byteOffset]);
            }

            auto getJoints = [&](int vertIdx) -> Vector3 {
              if (jointsAccIdx < 0) return Vector3(0, 0, 0);
              const tinygltf::Accessor& acc = model.accessors[jointsAccIdx];
              const tinygltf::BufferView& view = model.bufferViews[acc.bufferView];
              const tinygltf::Buffer& buf = model.buffers[view.buffer];
              size_t stride = view.byteStride ? view.byteStride : (acc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_BYTE ? 4 : (acc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_SHORT ? 8 : 16));
              const unsigned char* ptr = &buf.data[view.byteOffset + acc.byteOffset + vertIdx * stride];
              float j0 = 0, j1 = 0, j2 = 0;
              if (acc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_BYTE) {
                j0 = ptr[0]; j1 = ptr[1]; j2 = ptr[2];
              } else if (acc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_SHORT) {
                const uint16_t* u16 = reinterpret_cast<const uint16_t*>(ptr);
                j0 = u16[0]; j1 = u16[1]; j2 = u16[2];
              } else if (acc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_INT) {
                const uint32_t* u32 = reinterpret_cast<const uint32_t*>(ptr);
                j0 = (float)u32[0]; j1 = (float)u32[1]; j2 = (float)u32[2];
              }
              return Vector3(j0, j1, j2);
            };

            auto getWeights = [&](int vertIdx) -> Vector3 {
              if (weightsAccIdx < 0) return Vector3(1.0f, 0, 0);
              const tinygltf::Accessor& acc = model.accessors[weightsAccIdx];
              const tinygltf::BufferView& view = model.bufferViews[acc.bufferView];
              const tinygltf::Buffer& buf = model.buffers[view.buffer];
              size_t stride = view.byteStride ? view.byteStride : (acc.componentType == TINYGLTF_COMPONENT_TYPE_FLOAT ? 16 : (acc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_SHORT ? 8 : 4));
              const unsigned char* ptr = &buf.data[view.byteOffset + acc.byteOffset + vertIdx * stride];
              float w0 = 1.0f, w1 = 0, w2 = 0;
              if (acc.componentType == TINYGLTF_COMPONENT_TYPE_FLOAT) {
                const float* f = reinterpret_cast<const float*>(ptr);
                w0 = f[0]; w1 = f[1]; w2 = f[2];
              } else if (acc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_BYTE) {
                w0 = ptr[0] / 255.0f; w1 = ptr[1] / 255.0f; w2 = ptr[2] / 255.0f;
              } else if (acc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_SHORT) {
                const uint16_t* u16 = reinterpret_cast<const uint16_t*>(ptr);
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

            auto addTriangle = [&](int i0, int i1, int i2) {
                Triangle* tri = new Triangle();
                tri->SetVertex(0, Vector3(positions[i0*3], positions[i0*3+1], positions[i0*3+2]));
                tri->SetVertex(1, Vector3(positions[i1*3], positions[i1*3+1], positions[i1*3+2]));
                tri->SetVertex(2, Vector3(positions[i2*3], positions[i2*3+1], positions[i2*3+2]));

                if (normals) {
                    tri->SetNormal(0, normals[i0*3], normals[i0*3+1], normals[i0*3+2]);
                    tri->SetNormal(1, normals[i1*3], normals[i1*3+1], normals[i1*3+2]);
                    tri->SetNormal(2, normals[i2*3], normals[i2*3+1], normals[i2*3+2]);
                }
                
                if (uvs) {
                    // GLTF V coordinate is usually flipped compared to what OpenGL/engine expects
                    tri->SetTextureVertex(0, 0, uvs[i0*2], 1.0f - uvs[i0*2+1], 0);
                    tri->SetTextureVertex(0, 1, uvs[i1*2], 1.0f - uvs[i1*2+1], 0);
                    tri->SetTextureVertex(0, 2, uvs[i2*2], 1.0f - uvs[i2*2+1], 0);
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

            if (primitive.indices >= 0) {
                const tinygltf::Accessor& indAcc = model.accessors[primitive.indices];
                const tinygltf::BufferView& indView = model.bufferViews[indAcc.bufferView];
                const tinygltf::Buffer& indBuffer = model.buffers[indView.buffer];
                
                if (indAcc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_SHORT) {
                    const uint16_t* indices = reinterpret_cast<const uint16_t*>(&indBuffer.data[indView.byteOffset + indAcc.byteOffset]);
                    for (size_t i = 0; i < indAcc.count; i += 3) {
                        addTriangle(indices[i], indices[i+1], indices[i+2]);
                    }
                } else if (indAcc.componentType == TINYGLTF_COMPONENT_TYPE_UNSIGNED_INT) {
                    const uint32_t* indices = reinterpret_cast<const uint32_t*>(&indBuffer.data[indView.byteOffset + indAcc.byteOffset]);
                    for (size_t i = 0; i < indAcc.count; i += 3) {
                        addTriangle(indices[i], indices[i+1], indices[i+2]);
                    }
                }
            } else {
                for (size_t i = 0; i < posAcc.count; i += 3) {
                    addTriangle(i, i+1, i+2);
                }
            }
        }
    }

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

    // Process Materials (Just taking the first available material as a basic implementation)
    Material material;
    material.diffuseTexture = ResourceManagerPool::GetInstance().GetManager<Surface>(e_ResourceType_Surface)->Fetch("media/textures/white.png", true, true);
    
    if (!model.materials.empty()) {
        auto& mat = model.materials[0];
        if (mat.pbrMetallicRoughness.baseColorTexture.index >= 0) {
            auto& tex = model.textures[mat.pbrMetallicRoughness.baseColorTexture.index];
            auto& image = model.images[tex.source];
            // In a complete implementation we would load the image bytes directly into a Surface,
            // but for now, we just fallback to the engine's Fetch using the uri if it exists.
            if (!image.uri.empty()) {
                material.diffuseTexture = ResourceManagerPool::GetInstance().GetManager<Surface>(e_ResourceType_Surface)->Fetch(image.uri, true, true);
            }
        }
    }

    resource->resourceMutex.lock();
    std::vector<unsigned int> finalIndices;
    resource->GetResource()->AddTriangleMesh(material, rawTriangleMesh, rawSize, finalIndices);
    resource->resourceMutex.unlock();
    
    Log(e_Notice, "GLTFLoader", "Load", "Successfully parsed glTF geometry: " + filename);
  }
}
