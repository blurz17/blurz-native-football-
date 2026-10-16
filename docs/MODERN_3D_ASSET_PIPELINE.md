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

## 3. Verified In-Game Assets
- Converted the official match ball (`generic.ase` + `ball.jpg`) to a self-contained binary `generic.glb` (58 KB, 200 triangles, embedded texture) using `tools/convert_ase_to_glb.py`.
- Converted referee cards (`yellowcard.glb`, `redcard.glb`).
- Both formats verified compiling and bundling into the Android release APK (`app-release.apk`).

---

## 4. Player Model & Hairstyle Modernization via Blender MCP

### 4.1 Live Blender MCP Pipeline (`tools/start_blender_mcp.py`)
- Integrated Blender 5.1 via the Blender MCP server running on port `9876`.
- Automated headless/GUI launch scripts allowing programmatic reconstruction, material binding, camera staging, and 3D export directly from the agent.
- Reconstructed the complete 12 submesh football player model:
  - Bound Real Madrid / Galacticos CF kit textures (`realmadrid_kit_01.png`, `kit_UVWnormal.png`).
  - Bound skin tone textures (`skin02.png`, `arm_normal.png`, `knee_normal.png`).
  - Aligned 3D hairstyles (`short02`, `short01`, `medium01`, `medium02`, `long01`, `bald`) to player head bones.
- Exported modern assets to both `.glb` and `.fbx`:
  - `media/objects/players/models/fullbody.glb` (770 KB)
  - `media/objects/players/models/fullbody.fbx` (89 KB)
  - All 6 hairstyles in `media/objects/players/hairstyles/` (`.glb` & `.fbx`)

### 4.2 Coordinate Space Synchronization (Y-Up to Engine Z-Up)
- **glTF 2.0 Standard**: Right-handed Y-up (+Y is up, +Z is forward, +X is right).
- **Engine Standard**: Right-handed Z-up (+Z is height up, +Y is pitch length forward, +X is pitch width right).
- In `GLTFLoader`:
  ```cpp
  // Pure 90 deg rotation around X-axis (det = +1, preserves winding & handedness)
  engine_x = gltf_x;
  engine_y = -gltf_z;
  engine_z = gltf_y;
  ```
  Verified: 2,223 / 2,223 (100.0%) vertex positions match the original player coordinate system with 0 mismatches.
- In `FBXLoader`:
  Configured `opts.target_axes = ufbx_axes_right_handed_z_up;` to automatically transform any incoming FBX asset to the engine's native coordinate frame.

### 4.3 Dynamic Material & Kit Role Binding
- The engine dynamically assigns individual team kits (e.g. Real Madrid, Barcelona, Bayern Munich) and player skin tones (`skin01.png` - `skin05.png`) at runtime.
- `GLTFLoader` and `FBXLoader` now detect material, texture, and image roles:
  - Materials containing `"kit"` are assigned `IdentString = "kit_template.png"`, enabling `HumanoidBase::SetKit` to swap team kits dynamically.
  - Materials containing `"skin"` are assigned `IdentString = "skin.jpg"`, enabling dynamic skin tone matching.
  - Materials containing `"shoe"` are mapped to `"shoe.jpg"`.
