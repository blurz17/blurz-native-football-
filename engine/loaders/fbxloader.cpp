#include "fbxloader.hpp"

#include "base/log.hpp"
#include "base/geometry/triangle.hpp"
#include "base/geometry/trianglemeshutils.hpp"
#include "managers/resourcemanagerpool.hpp"
#include "scene/resources/surface.hpp"

#include <SDL2/SDL.h>
#include <SDL2/SDL_image.h>
#include <vector>
#include <string>
#include <cmath>
#include <algorithm>

#include "ufbx.h"

namespace blunted {

  FBXLoader::FBXLoader() : Loader<GeometryData>() {
  }

  FBXLoader::~FBXLoader() {
  }

  void FBXLoader::Load(std::string filename, boost::intrusive_ptr < Resource <GeometryData> > resource) {
    // Use SDL_RWops to load file from disk, APK asset, or memory
    SDL_RWops *rw = SDL_RWFromFile(filename.c_str(), "rb");
    if (!rw) {
      Log(e_Error, "FBXLoader", "Load", "Could not open file: " + filename + " (" + SDL_GetError() + ")");
      return;
    }

    Sint64 fileSize = SDL_RWsize(rw);
    if (fileSize <= 0) {
      Log(e_Error, "FBXLoader", "Load", "Empty FBX file: " + filename);
      SDL_RWclose(rw);
      return;
    }

    std::vector<unsigned char> fileBuffer(fileSize);
    size_t readBytes = SDL_RWread(rw, fileBuffer.data(), 1, fileSize);
    SDL_RWclose(rw);

    if (readBytes != (size_t)fileSize) {
      Log(e_Error, "FBXLoader", "Load", "Failed to read complete file: " + filename);
      return;
    }

    std::string baseDir = "";
    size_t lastSlash = filename.find_last_of("/\\");
    if (lastSlash != std::string::npos) {
      baseDir = filename.substr(0, lastSlash);
    }

    ufbx_load_opts opts;
    memset(&opts, 0, sizeof(opts));
    opts.target_axes = ufbx_axes_right_handed_z_up;
    opts.target_unit_meters = 1.0f;
    opts.generate_missing_normals = true;

    ufbx_error error;
    ufbx_scene *scene = ufbx_load_memory(fileBuffer.data(), fileBuffer.size(), &opts, &error);
    if (!scene) {
      Log(e_Error, "FBXLoader", "Load", "Failed to parse FBX: " + filename + " - " + std::string(error.description.data));
      return;
    }

    int totalTrianglesLoaded = 0;
    int submeshIndex = 0;

    for (size_t m = 0; m < scene->meshes.count; m++) {
      ufbx_mesh *mesh = scene->meshes.data[m];
      if (!mesh || mesh->num_triangles == 0) continue;

      const ufbx_node *node = mesh->instances.count > 0 ? mesh->instances.data[0] : nullptr;

      size_t numParts = mesh->material_parts.count;
      if (numParts == 0) numParts = 1; // Fallback to single part if no material parts defined

      for (size_t p = 0; p < numParts; p++) {
        std::vector<Triangle*> triangles;
        const ufbx_material *fbxMat = nullptr;

        size_t numFacesInPart = 0;
        const uint32_t *faceIndices = nullptr;

        if (mesh->material_parts.count > 0) {
          const ufbx_mesh_part &part = mesh->material_parts.data[p];
          numFacesInPart = part.num_faces;
          faceIndices = part.face_indices.data;
          if (part.index < mesh->materials.count) {
            fbxMat = mesh->materials.data[part.index];
          }
        } else {
          numFacesInPart = mesh->num_faces;
          if (mesh->materials.count > 0) {
            fbxMat = mesh->materials.data[0];
          }
        }

        for (size_t f = 0; f < numFacesInPart; f++) {
          uint32_t faceIdx = faceIndices ? faceIndices[f] : (uint32_t)f;
          if (faceIdx >= mesh->faces.count) continue;
          ufbx_face face = mesh->faces.data[faceIdx];

          uint32_t tri_indices[128];
          uint32_t num_tris = ufbx_triangulate_face(tri_indices, 128, mesh, face);

          for (uint32_t t = 0; t < num_tris; t++) {
            Triangle *tri = new Triangle();

            for (int v = 0; v < 3; v++) {
              uint32_t cornerIdx = tri_indices[t * 3 + v];

              // Position (transformed by node geometry_to_world)
              ufbx_vec3 pos = ufbx_get_vertex_vec3(&mesh->vertex_position, cornerIdx);
              if (node) {
                pos = ufbx_transform_position(&node->geometry_to_world, pos);
              }
              tri->SetVertex(v, Vector3(pos.x, pos.y, pos.z));

              // Normal (transformed by node geometry_to_world)
              if (mesh->vertex_normal.indices.count > 0) {
                ufbx_vec3 norm = ufbx_get_vertex_vec3(&mesh->vertex_normal, cornerIdx);
                if (node) {
                  norm = ufbx_transform_direction(&node->geometry_to_world, norm);
                }
                tri->SetNormal(v, norm.x, norm.y, norm.z);
              }

              // UV (OpenGL convention: flip Y)
              if (mesh->vertex_uv.indices.count > 0) {
                ufbx_vec2 uv = ufbx_get_vertex_vec2(&mesh->vertex_uv, cornerIdx);
                tri->SetTextureVertex(0, v, uv.x, 1.0f - uv.y, 0.0f);
              }

              // Skeletal Skinning deformers (Joint indices & weights)
              if (mesh->skin_deformers.count > 0) {
                uint32_t vidx = (cornerIdx < mesh->vertex_indices.count) ? mesh->vertex_indices.data[cornerIdx] : 0;
                ufbx_skin_deformer *skin = mesh->skin_deformers.data[0];
                if (vidx < skin->vertices.count) {
                  ufbx_skin_vertex skv = skin->vertices.data[vidx];
                  float j0 = 0, j1 = 0, j2 = 0;
                  float w0 = 1.0f, w1 = 0, w2 = 0;

                  if (skv.num_weights > 0) {
                    j0 = (float)skin->weights.data[skv.weight_begin].cluster_index;
                    w0 = (float)skin->weights.data[skv.weight_begin].weight;
                  }
                  if (skv.num_weights > 1) {
                    j1 = (float)skin->weights.data[skv.weight_begin + 1].cluster_index;
                    w1 = (float)skin->weights.data[skv.weight_begin + 1].weight;
                  }
                  if (skv.num_weights > 2) {
                    j2 = (float)skin->weights.data[skv.weight_begin + 2].cluster_index;
                    w2 = (float)skin->weights.data[skv.weight_begin + 2].weight;
                  }

                  float sum = w0 + w1 + w2;
                  if (sum > 0.0001f) {
                    w0 /= sum; w1 /= sum; w2 /= sum;
                  } else {
                    w0 = 1.0f; w1 = 0; w2 = 0;
                  }

                  tri->SetTextureVertex(1, v, Vector3(j0, j1, j2));
                  tri->SetTextureVertex(2, v, Vector3(w0, w1, w2));
                }
              }
            }

            triangles.push_back(tri);
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

        // Material setup
        Material material;
        material.diffuseTexture = ResourceManagerPool::GetInstance().GetManager<Surface>(e_ResourceType_Surface)->Fetch("media/textures/white.png", true, true);
        material.specular_amount = 0.05f;
        material.shininess = 0.1f;

        auto resolveTexturePath = [&](const std::string &rawPath) -> std::string {
          if (rawPath.empty()) return "";
          std::string baseName = get_file_name(rawPath);
          if (baseName.empty()) return "";

          // 1. Check relative to model base directory
          if (!baseDir.empty()) {
            std::string candidate = baseDir + "/" + baseName;
            SDL_RWops *rw = SDL_RWFromFile(candidate.c_str(), "rb");
            if (rw) { SDL_RWclose(rw); return candidate; }
          }

          // 2. Check player textures directory
          std::string candidate = "media/objects/players/textures/" + baseName;
          SDL_RWops *rw = SDL_RWFromFile(candidate.c_str(), "rb");
          if (rw) { SDL_RWclose(rw); return candidate; }

          // 3. Check general textures directory
          candidate = "media/textures/" + baseName;
          rw = SDL_RWFromFile(candidate.c_str(), "rb");
          if (rw) { SDL_RWclose(rw); return candidate; }

          return "media/objects/players/textures/" + baseName;
        };

        if (fbxMat) {
          // Detect dynamic player material roles (kit, skin, shoe)
          std::string lowerMatName = fbxMat->name.data ? std::string(fbxMat->name.data) : "";
          std::transform(lowerMatName.begin(), lowerMatName.end(), lowerMatName.begin(), ::tolower);

          // Diffuse / Base color
          const ufbx_texture *diffTex = fbxMat->fbx.diffuse_color.texture ? fbxMat->fbx.diffuse_color.texture : fbxMat->pbr.base_color.texture;
          if (diffTex) {
            std::string lowerTexName = diffTex->name.data ? std::string(diffTex->name.data) : "";
            std::transform(lowerTexName.begin(), lowerTexName.end(), lowerTexName.begin(), ::tolower);
            std::string lowerFileName = diffTex->filename.data ? std::string(diffTex->filename.data) : "";
            std::transform(lowerFileName.begin(), lowerFileName.end(), lowerFileName.begin(), ::tolower);

            std::string dynamicIdent = "";
            if (lowerMatName.find("kit") != std::string::npos || lowerTexName.find("kit") != std::string::npos || lowerFileName.find("kit") != std::string::npos) {
              dynamicIdent = "kit_template.png";
            } else if (lowerMatName.find("skin") != std::string::npos || lowerTexName.find("skin") != std::string::npos || lowerFileName.find("skin") != std::string::npos) {
              dynamicIdent = "skin.jpg";
            } else if (lowerMatName.find("shoe") != std::string::npos || lowerTexName.find("shoe") != std::string::npos || lowerFileName.find("shoe") != std::string::npos) {
              dynamicIdent = "shoe.jpg";
            }

            if (diffTex->content.size > 0 && diffTex->content.data) {
              // Embedded texture in FBX
              std::string texIdent = !dynamicIdent.empty() ? dynamicIdent : (filename + "#fbx_tex_" + int_to_str(diffTex->element.element_id));
              bool alreadyThere = false;
              boost::intrusive_ptr<Resource<Surface>> texRes = ResourceManagerPool::GetInstance().GetManager<Surface>(e_ResourceType_Surface)->Fetch(texIdent, false, alreadyThere, true);
              if (!alreadyThere) {
                SDL_RWops *imgRw = SDL_RWFromConstMem(diffTex->content.data, (int)diffTex->content.size);
                if (imgRw) {
                  SDL_Surface *sdlSurf = IMG_Load_RW(imgRw, 1);
                  if (sdlSurf) {
                    texRes->GetResource()->SetData(sdlSurf);
                  }
                }
              }
              material.diffuseTexture = texRes;
            } else if (diffTex->filename.length > 0 || !dynamicIdent.empty()) {
              std::string texPath = !dynamicIdent.empty() ? ("media/objects/players/textures/" + dynamicIdent) : resolveTexturePath(diffTex->filename.data);
              material.diffuseTexture = ResourceManagerPool::GetInstance().GetManager<Surface>(e_ResourceType_Surface)->Fetch(texPath, true, true);
            }
          }

          // Normal Map
          const ufbx_texture *normTex = fbxMat->fbx.normal_map.texture ? fbxMat->fbx.normal_map.texture : fbxMat->pbr.normal_map.texture;
          if (normTex) {
            if (normTex->content.size > 0 && normTex->content.data) {
              std::string normIdent = filename + "#fbx_norm_" + int_to_str(normTex->element.element_id);
              bool alreadyThere = false;
              boost::intrusive_ptr<Resource<Surface>> normRes = ResourceManagerPool::GetInstance().GetManager<Surface>(e_ResourceType_Surface)->Fetch(normIdent, false, alreadyThere, true);
              if (!alreadyThere) {
                SDL_RWops *imgRw = SDL_RWFromConstMem(normTex->content.data, (int)normTex->content.size);
                if (imgRw) {
                  SDL_Surface *sdlSurf = IMG_Load_RW(imgRw, 1);
                  if (sdlSurf) {
                    normRes->GetResource()->SetData(sdlSurf);
                  }
                }
              }
              material.normalTexture = normRes;
            } else if (normTex->filename.length > 0) {
              std::string normPath = resolveTexturePath(normTex->filename.data);
              material.normalTexture = ResourceManagerPool::GetInstance().GetManager<Surface>(e_ResourceType_Surface)->Fetch(normPath, true, true);
            }
          }

          if (fbxMat->pbr.roughness.has_value) {
            material.specular_amount = (float)std::max(0.01, 1.0 - fbxMat->pbr.roughness.value_real);
            material.shininess = (float)std::max(0.01, 1.0 - fbxMat->pbr.roughness.value_real);
          }
        }

        resource->resourceMutex.lock();
        std::vector<unsigned int> emptyIndices;
        resource->GetResource()->AddTriangleMesh(material, rawTriangleMesh, rawSize, emptyIndices);
        resource->resourceMutex.unlock();

        submeshIndex++;
      }
    }

    ufbx_free_scene(scene);

    Log(e_Notice, "FBXLoader", "Load", "Loaded FBX: " + filename + " (" + int_to_str(submeshIndex) + " submeshes, " + int_to_str(totalTrianglesLoaded) + " triangles)");
  }

}
