# Modern 3D Asset Pipeline: glTF 2.0 / GLB & FBX Integration

## 1. Executive Summary
As part of the engine modernization targeting native Full HD 1080p @ 60 FPS on Android Mali GPUs, the engine's legacy 3D asset pipeline (previously restricted to 1990s 3ds Max `.ase` ASCII files) has been upgraded to natively support industry-standard modern 3D formats:
- **glTF 2.0 / GLB** (binary glTF via `tinygltf` and `GLTFLoader`)
- **Autodesk FBX** (ASCII & Binary FBX via `ufbx` and `FBXLoader`)
- **Transparent Format Auto-Discovery** in `ResourceManager<GeometryData>::Fetch`

---

## 2. Architecture & Implementation

### 2.1 `GLTFLoader` (`engine/loaders/gltfloader.cpp`)
- **Universal Buffer Loading**: Reads `.gltf` and `.glb` files using `SDL_RWops`, allowing transparent loading from Android APK compressed assets (`assets/`), device internal storage, or PC filesystem.
- **Multi-Primitive / Submesh Support**: Iterates through all primitives in each mesh, creating independent `MaterializedTriangleMesh` objects with discrete materials.
- **Embedded Texture Extraction**: Decodes embedded GLB image buffers (PNG/JPEG) into `Surface` resources and registers them into `ResourceManagerPool`.
- **Skeletal Weight Extraction**: Parses `JOINTS_0` and `WEIGHTS_0` accessors directly into vertex texture layers 1 & 2 for hardware skeletal skinning.

### 2.2 `FBXLoader` (`engine/loaders/fbxloader.cpp`)
- **Powered by `ufbx`**: Integrated the lightweight single-unit `ufbx` library (`third_party/ufbx/`), compiled natively via Android NDK clang.
- **Multi-Material Partitioning**: Splits FBX meshes by `ufbx_mesh_part` into individual submeshes.
- **Triangulation**: Uses `ufbx_triangulate_face` to guarantee consistent vertex layout and face winding.
- **Embedded & External Textures**: Automatically resolves embedded texture blobs (`ufbx_blob content`) via `IMG_Load_RW`, and relative external files.
- **Bone Skin Deformers**: Reads `ufbx_skin_deformer` clusters and vertex weights for skeletal meshes.

### 2.3 Transparent Format Auto-Discovery (`engine/managers/resourcemanager.hpp`)
Whenever any subsystem or game entity requests a legacy `.ase` model:
```cpp
// If "path/model.ase" is requested:
1. Check if "path/model.glb" exists (SDL_RWFromFile).
2. If found, automatically route to GLTFLoader.
3. Else check if "path/model.fbx" exists.
4. If found, automatically route to FBXLoader.
5. If neither exists, load the original "path/model.ase".
```
This enables zero-code-change drop-in modernization for any existing ball, player, hair, or stadium mesh.

---

## 3. Verified In-Game Asset
- Converted the official match ball (`generic.ase` + `ball.jpg`) to a self-contained binary `generic.glb` (58 KB, 200 triangles, embedded texture) using `tools/convert_ase_to_glb.py`.
- Converted referee cards (`yellowcard.glb`, `redcard.glb`).
- Both formats verified compiling and bundling into the Android release APK (`app-release.apk`).
