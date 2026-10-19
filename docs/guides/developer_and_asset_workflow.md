# Developer & 3D Asset Workflow Guide

This guide details the procedures for extending the game: adding modern 3D models (glTF/FBX), adding team kits and balls, using the Blender MCP pipeline, and implementing new training drills.

---

## 1. Adding New 3D Models (glTF 2.0 / GLB & FBX)

Thanks to the transparent format auto-discovery implemented in `ResourceManager` ([ADR-004](../adr/ADR-004_modern_asset_pipeline_gltf_fbx_auto_discovery.md)), developers can add modern `.glb` or `.fbx` files without modifying C++ source code.

### Step-by-Step Drop-In Workflow:
1. Model your asset in Blender, Maya, or 3ds Max.
2. Export as **glTF Binary (`.glb`)** or **Autodesk FBX (`.fbx`)**:
   - Apply transforms (Position, Rotation, Scale = 1.0).
   - Ensure unit scale is in meters ($1.0 \text{ unit} = 1.0 \text{ meter}$).
   - Forward axis: $-Y$ forward, $+Z$ up (or let the loader auto-transform).
3. Place the exported file in the corresponding asset folder matching the legacy name:
   - For a match ball: `assets/media/objects/balls/generic.glb`
   - For a player model: `assets/media/objects/players/models/fullbody.glb`
   - For a hairstyle: `assets/media/objects/players/hairstyles/short01.glb`
4. Copy the file into the Android assets directory:
   `platform/android/app/src/main/assets/media/...`
5. The engine will automatically detect and load the modern binary format on startup.

---

## 2. Automated Blender MCP Pipeline

The project includes an integrated Blender automation pipeline (`tools/start_blender_mcp.py`):

1. **Launch the Blender MCP Server**:
   ```bash
   python tools/start_blender_mcp.py
   ```
2. The MCP agent can programmatically:
   - Construct multi-part humanoids.
   - Bind textures and normal maps.
   - Adjust skeletal bone bindings.
   - Export optimized GLB and FBX meshes directly into the project repository.

---

## 3. Creating a New Training Drill

The decoupled training subsystem makes adding new practice drills straightforward.

### Step 1: Add Drill Enum
In [`game/training/training_mode.hpp`](file:///e:/MY-PROJECTS/android%20native%20football/game/training/training_mode.hpp):
```cpp
enum e_TrainingDrill {
    e_TrainingDrill_FreePlay = 0,
    e_TrainingDrill_FreeKick,
    e_TrainingDrill_CornerKick,
    e_TrainingDrill_Penalty,
    e_TrainingDrill_CrossAndVolley, // New Drill
    e_TrainingDrill_Count
};
```

### Step 2: Implement Setup in `TrainingManager`
In [`game/training/training_manager.cpp`](file:///e:/MY-PROJECTS/android%20native%20football/game/training/training_manager.cpp):
1. Add case in `TrainingManager::ResetDrill()`:
   ```cpp
   case e_TrainingDrill_CrossAndVolley:
       _SetupCrossAndVolley();
       break;
   ```
2. Implement scenario positioning:
   - Position the ball and ball-carrier at the wing.
   - Position target attackers inside the 18-yard box.
   - Park non-active defenders outside the pitch.
   - Configure camera view (`e_CameraMode_Normal` or `e_CameraMode_Player`).

### Step 3: Register in Java UI
In [`MobileControlsView.java`](file:///e:/MY-PROJECTS/android%20native%20football/platform/android/app/src/main/java/org/libsdl/app/MobileControlsView.java):
Add title string to the `DRILL_NAMES` array:
```java
private static final String[] DRILL_NAMES = {
    "FREE PLAY (MIDFIELD)",
    "FREE KICK",
    "CORNER KICK",
    "PENALTY SHOOTOUT",
    "CROSS & VOLLEY"
};
```
