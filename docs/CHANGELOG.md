# Changelog

All notable changes to the **Android Native Football (GPFB)** project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html) and [Conventional Commits](https://www.conventionalcommits.org/).

---

## [Unreleased] - 2026-10-20

### Highlights of the Last 30 Commits
Over the last 30 commits (`abf0aef` through `4365f41`), the engine has undergone five fundamental transformations:
1. **Touch Controls & Analog Pipeline Overhaul** (`abf0aef` → `7898447`): Replaced 8-way quantized accelerometer inputs with a zero-latency direct JNI bridge, 360° continuous floating-point analog tracking, and dynamic thumb follow.
2. **Gameplay Physics & Ball Retention** (`f01917b` → `51fa632`): Implemented dynamic possession windows, turf-glued dribbling, decoupled turning angles from deceleration animations, and eliminated sprint stutter hiccups.
3. **Mobile UI & In-Match HUD Overhaul** (`7abf05f` → `6db7b86`): Designed and tuned responsive PES Mobile style 5/6-card pause overlays, authentic white sheet sub-screens, and EA Sports FC Mobile style in-match circular action buttons.
4. **Modern 3D Asset Pipeline & Telemetry** (`c41165b` → `7d7910e`): Integrated glTF 2.0 (GLB) and Autodesk FBX loaders via `ufbx` and `tinygltf` with automatic fallback discovery, $O(N)$ spatial hashing for vertex deduplication, CR7 3D model, Al Nassr kit, Adidas Al Rihla ball, and real-time engine telemetry.
5. **Decoupled Training Mode & Multithreaded Stability** (`a3b5247` → `4365f41`): Built an isolated 4-drill Training Mode subsystem with center kick-off Free Play and AI suppression, 3rd-person Player Camera with $\text{SO}(2)$ camera-relative analog controls, and resolved multi-threaded `SIGSEGV` crashes using `putBufferMutex`.

---

## Detailed 30-Commit Chronology

### Milestone 5: Isolated Training Mode, 3rd-Person Player Cam & Thread Synchronization

#### [`4365f41`](https://github.com/example/android-native-football/commit/4365f41) - 2026-10-20
*Author: GameplayFootball Dev*
##### Added
- **Isolated Training Subsystem (`game/training/`)**:
  - Implemented [`TrainingManager`](file:///e:/MY-PROJECTS/android%20native%20football/game/training/training_manager.hpp) and [`TrainingMode`](file:///e:/MY-PROJECTS/android%20native%20football/game/training/training_mode.hpp) decoupled from standard exhibition match rules.
  - **Free Play**: Initializes at the center circle `Vector3(0.0f, 0.0f, 0.11f)` with stationary ball at feet and full 11-player squad tactical formation for pitch-wide passing drills.
  - **Free Kicks**: 4 tactical shooting variations (Center 24m, Left Angle 23m, Right Angle 23m, Long Range 32m) with 3-man defensive wall.
  - **Corner Kicks**: Cross delivery scenarios from left and right corner flags with aerial heading in the box.
  - **Penalty Shootout**: 11-meter spot kick 1v1 duel against the goalkeeper.
- **Dedicated Training Pause Menu & Live HUD**:
  - Implemented 6-card PES Mobile training pause grid in `MobileControlsView.java` featuring Drill Switcher, Variation Toggle, Instant Reset, Camera Selector, Match Settings, and Exit.
  - Added live top HUD pill displaying active drill title, score, and drill variation.
- **3rd-Person Player Camera**:
  - Added over-the-shoulder dynamic follow camera tracking the active user-controlled player.
  - Behind-the-ball trajectory view for set-pieces (Free Kicks and Penalties).
##### Fixed
- **Camera-Relative Analog Input ($\text{SO}(2)$ Projection)**:
  - Resolved 90° inverted control axis in 3rd-person view where swiping Up moved West and swiping East moved North.
  - Tracked `camera2DForward` and projected screen-space joystick vectors onto camera basis vectors:
    $$\vec{D} = \text{screenY} \cdot \vec{V}_{\text{camForward}} + \text{screenX} \cdot \vec{V}_{\text{camRight}}$$
  - Updated `HumanController::GetDirection()` and manual player switching to respect camera-relative directions.
- **Multi-Threaded Race Condition (`SIGSEGV` in `FetchPutBuffers`)**:
  - Diagnosed data race on red-black tree pointers inside `HumanoidBase::buf_animApplyBuffer` between the main game thread (`PreparePutBuffers`) and worker thread `Thread-6` (`FetchPutBuffers`).
  - Added `boost::mutex putBufferMutex;` guarding all animation buffer reads, writes, and resets.
  - Created explicit safe copy assignment operator in `AnimApplyBuffer`.
  - Decoupled asynchronous JNI UI calls into lock-free atomic command queues (`pendingSetDrill`, `pendingResetRequest`) executed synchronously on the game thread.
- **Opponent AI Suppression in Free Play**:
  - Parked opponent outfielders off the field boundary (`y = pitchHalfH + 3.0f`) with AI disabled in `ElizaController` and `TeamAIController`.
  - Disabled aggressive rushing in `GoalieDefaultStrategy` (`awayFromGoalBias = 0.05f`), keeping the keeper on the goal line to guard and save shots without tackling.
  - Added auto-reset notification on goalkeeper catch/save.

#### [`a3b5247`](https://github.com/example/android-native-football/commit/a3b5247) - 2026-10-18
*Author: GameplayFootball Dev*
##### Added
- **Kickoff Camera Centering & Broadcast Presets**:
  - Centered kickoff camera angle directly onto the stadium center spot.
  - Configured 3 broadcast camera presets:
    - **Preset 1**: Balanced Broadcast (Zoom 12%, Angle 50%, Height 15%).
    - **Preset 2**: High Tactical Angle (Zoom 12%, Angle 50%, Height 50%).
    - **Preset 3**: Action Dynamic Follow.
  - Expanded zoom slider boundaries and supported single-player close-up framing.

---

### Milestone 4: High-Fidelity 3D Assets, glTF/FBX Pipeline & Spatial Hashing

#### [`7d7910e`](https://github.com/example/android-native-football/commit/7d7910e) - 2026-10-18
*Author: GameplayFootball Dev*
##### Added
- **CR7 3D Model & Al Nassr Kit**:
  - Integrated high-detail Cristiano Ronaldo 3D model with 13-bone skeletal hierarchy and 2K Al Nassr home kit texture.
- **Official Adidas Al Rihla Match Ball**:
  - Added official match ball 3D mesh with authentic Speedshell panel geometry.
- **glTF Skinning in `GLTFLoader`**:
  - Implemented TinyGLTF skinning, bone index parsing (`JOINTS_0`), weight mapping (`WEIGHTS_0`), and material specular tuning.
##### Performance
- **$O(N)$ Spatial Hash Optimization**:
  - Replaced $O(N^2)$ brute-force duplicate vertex search in `HumanoidBase` with an $O(N)$ 3D spatial hash map (`std::unordered_map`), eliminating multi-second load hitches.

#### [`0eabd45`](https://github.com/example/android-native-football/commit/0eabd45) - 2026-10-17
*Author: GameplayFootball Dev*
##### Fixed
- **16ms Android Graphics Delay**:
  - Eliminated legacy 16ms sleep delay in Android graphics thread, forcing configuration synchronization and preventing unexpected 60Hz mode locks.

#### [`6eb6058`](https://github.com/example/android-native-football/commit/6eb6058) - 2026-10-17
*Author: GameplayFootball Dev*
##### Fixed
- **120Hz Display Modes & Scheduler Refresh**:
  - Configured Android display mode requests to unlock 90Hz and 120Hz display panels.
  - Fixed scheduler `dueEntry` calculation to support 8.33ms and 11.11ms simulation intervals.
  - Added swap buffer profiling to monitor frame pacing.

#### [`a728c30`](https://github.com/example/android-native-football/commit/a728c30) - 2026-10-17
*Author: GameplayFootball Dev*
##### Fixed
- **Scheduler Sleep Cap Removal**:
  - Removed hardcoded 16ms sleep cap in `scheduler.cpp`.
  - Restored verified, stable CPU vertex skinning baseline after driver-specific uniform buffer limits were encountered on Mali GPUs during experimental hardware skinning.
  - Inlined geometry sync between game thread and rendering pipeline.

#### [`80a622e`](https://github.com/example/android-native-football/commit/80a622e) - 2026-10-17
*Author: GameplayFootball Dev*
##### Performance
- Unlocked 120Hz display modes and investigated inlined GPU skinning passes for FBX models.

#### [`df83504`](https://github.com/example/android-native-football/commit/df83504) - 2026-10-17
*Author: GameplayFootball Dev*
##### Added
- **Hardware GPU Vertex Skinning Prototype**:
  - Implemented static VBO vertex shader skinning using uniform matrix arrays (`uniform mat4 u_boneMatrices[64]`) in `simple.vert` and `zphase.vert`.

#### [`44005e7`](https://github.com/example/android-native-football/commit/44005e7) - 2026-10-17
*Author: GameplayFootball Dev*
##### Fixed
- **Inverted FBX UV Coordinates**:
  - Resolved vertically flipped UV coordinates ($1.0 - V$) in `FBXLoader` that caused player kits and textures to sample black borders.
- **1% Low Frametime Stabilization**:
  - Stabilized frametime delta collection in `Profiler` to prevent outlier spikes from distorting 1% low metrics.

#### [`bb57581`](https://github.com/example/android-native-football/commit/bb57581) - 2026-10-17
*Author: GameplayFootball Dev*
##### Fixed
- **FBX Node Transformation**:
  - Applied `ufbx_matrix` node `geometry_to_world` transformation to ensure imported FBX vertices match world-space coordinates.

#### [`187ace4`](https://github.com/example/android-native-football/commit/187ace4) - 2026-10-17
*Author: GameplayFootball Dev*
##### Fixed
- **Path Sanitization & `out_of_range` Exception**:
  - Resolved `std::out_of_range` crash in `get_file_name()` caused by mixed backslashes and forward slashes.
  - Sanitized external texture path parsing in `FBXLoader`.

#### [`e0fec9a`](https://github.com/example/android-native-football/commit/e0fec9a) - 2026-10-17
*Author: GameplayFootball Dev*
##### Fixed
- **Hairstyle Origin Alignment**:
  - Reset local origins of 6 player hairstyles to match the player head bone coordinate frame.
  - Configured `ResourceManager` to prioritize FBX loading when both FBX and ASE exist.

#### [`5fcd6e7`](https://github.com/example/android-native-football/commit/5fcd6e7) - 2026-10-17
*Author: GameplayFootball Dev*
##### Added
- **Modernized Player Models & Hairstyles**:
  - Reconstructed player body mesh (`fullbody.fbx`, `fullbody.glb`) and 6 hairstyles using automated Blender MCP scripts (`tools/start_blender_mcp.py`).

#### [`82d85f1`](https://github.com/example/android-native-football/commit/82d85f1) - 2026-10-17
*Author: GameplayFootball Dev*
##### Added
- **glTF 2.0 (GLB) and Autodesk FBX Asset Pipeline**:
  - Built `GLTFLoader` using `tinygltf` and `FBXLoader` using lightweight C library `ufbx`.
  - Added transparent auto-discovery in `ResourceManager<GeometryData>::Fetch`: automatically checks for `.glb` first, then `.fbx`, before falling back to legacy `.ase`.
  - Converted official match ball (`generic.glb`) and referee cards (`yellowcard.glb`, `redcard.glb`).

#### [`c41165b`](https://github.com/example/android-native-football/commit/c41165b) - 2026-10-17
*Author: GameplayFootball Dev*
##### Added
- **Engine Performance Profiler**:
  - Added `engine/base/profiler.cpp` and `profiler.hpp` measuring GPU passes (Geometry, Ambient, Light, Postprocess), CPU vertex skinning, and physics time.
  - Added real-time in-game HUD telemetry overlay and periodic Logcat reports.
##### Performance
- **CPU Skinning Optimization**:
  - Eliminated redundant tangent/bitangent transforms on meshes without normal maps.
  - Added 60Hz display lock in Java `SDLActivity` to prevent Android OS compositor throttling to 30Hz.

---

### Milestone 3: Mobile UI & In-Match HUD Overhaul

#### [`6db7b86`](https://github.com/example/android-native-football/commit/6db7b86) - 2026-10-11
*Author: GameplayFootball Dev*
##### Fixed
- **Pixel-Accurate Touch Control Layout**:
  - Applied relative offsets and responsive scaling for action buttons and analog joystick based on 1:1 overlay against mobile football game reference screenshots.

#### [`42ed750`](https://github.com/example/android-native-football/commit/42ed750) - 2026-10-11
*Author: GameplayFootball Dev*
##### Fixed
- **Joystick Artifact Removal & Button Ergonomics**:
  - Removed visual moving line connecting joystick center to touch point.
  - Adjusted thumb cursor dimensions and spacing between action buttons for thumb comfort.

#### [`37902f6`](https://github.com/example/android-native-football/commit/37902f6) - 2026-10-11
*Author: GameplayFootball Dev*
##### Added
- **EA Sports FC Mobile Style Gameplay HUD**:
  - Redesigned on-pitch circular action buttons (Pass, Through, Shoot, Sprint) with distinct color gradients, high-contrast typography, and active touch ripple feedback.

#### [`f31a36e`](https://github.com/example/android-native-football/commit/f31a36e) - 2026-10-11
*Author: GameplayFootball Dev*
##### Changed
- **Responsive Card Proportions**:
  - Balanced pause menu card proportions with dynamic DP scaling across mobile aspect ratios (16:9, 19.5:9, 20:9, 21:9).

#### [`31d1e36`](https://github.com/example/android-native-football/commit/31d1e36) - 2026-10-11
*Author: GameplayFootball Dev*
##### Added
- **Compact 6-Card Pause Grid & White Sheet Sub-Screens**:
  - Refined pause layout to 6 cards with dedicated Replay entry and authentic PES-style white sheet sub-screens for Audio, Camera, and Controls.

#### [`06b3a9a`](https://github.com/example/android-native-football/commit/06b3a9a) - 2026-10-11
*Author: GameplayFootball Dev*
##### Changed
- **Pause Grid Touch Handling & Telemetry**:
  - Enhanced touch responsiveness on pause cards and integrated live match telemetry into the pause overlay.

#### [`c15bde1`](https://github.com/example/android-native-football/commit/c15bde1) - 2026-10-11
*Author: GameplayFootball Dev*
##### Added
- **PES 5-Card Settings Grid & Replay Controls**:
  - Implemented team crests, visual options, replay controls (play/pause/scrub), and fixed camera height/angle bounds in `match.cpp`.

#### [`7abf05f`](https://github.com/example/android-native-football/commit/7abf05f) - 2026-10-11
*Author: GameplayFootball Dev*
##### Added
- **PES Mobile Style Pause Menu**:
  - Replaced legacy text menus with touch-friendly card grid.
##### Fixed
- **Engine Pause Freeze**:
  - Ensured pausing cleanly halts match physics simulation time without freezing the rendering loop or UI thread.

---

### Milestone 2: Gameplay Physics & Ball Retention Overhaul

#### [`51fa632`](https://github.com/example/android-native-football/commit/51fa632) - 2026-10-10
*Author: GameplayFootball Dev*
##### Fixed
- **Dash / Sprint Hiccup**:
  - Enforced strict quadrant and 0-velocity steps during straight sprint, excluding `000_decel` braking animations that caused players to stutter and restart running every few strides.
  - Protected sprinting players from mid-stride `ReQueue` downgrades.

#### [`36e89fe`](https://github.com/example/android-native-football/commit/36e89fe) - 2026-10-09
*Author: GameplayFootball Dev*
##### Fixed
- **Ball Loss During Sharp Turns & Sprint Dribbling**:
  - Permitted turning animations (45°, 90°, 135°, 180° decel) during `BallControl` without harsh quadrant pruning.
  - Granted generous reach radius for `BallControl` and removed consecutive touch penalties during continuous dribbling.
  - Bounded sprint dribble impulse power so the ball stays glued to the player's legs without outrunning stride.
  - Restricted defensive `forceMagnet` so pass charging never overrides stick steering.

#### [`f01917b`](https://github.com/example/android-native-football/commit/f01917b) - 2026-10-09
*Author: GameplayFootball Dev*
##### Fixed
- **Dribble Ball Retention**:
  - Implemented dynamic possession window keeping ball under tight control.
  - Prevented `autoBias` in AI support from overriding user analog stick direction.
  - Protected the human-controlled dribbler from automatic player switching for up to 1500ms during physical challenges.

---

### Milestone 1: Touch Input Pipeline & Direct JNI Dispatch

#### [`7898447`](https://github.com/example/android-native-football/commit/7898447) - 2026-10-09
*Author: GameplayFootball Dev*
##### Added
- **Direct JNI Touch Input Pipeline**:
  - Replaced generic SDL event queue polling with a direct JNI call from Java `MobileControlsView` to `UserEventManager::SetMobileDirection(float x, float y)`.
  - Upgraded to 360° continuous floating-point analog vector with dynamic floating joystick anchor following thumb movement.
##### Fixed
- **Ghost Inputs**:
  - Disabled SDL accelerometer hint that was feeding device tilt as joystick axes.

#### [`fbeea89`](https://github.com/example/android-native-football/commit/fbeea89) - 2026-10-09
*Author: GameplayFootball Dev*
##### Removed
- Reverted commit `20bdf23` cleanly to restore known stable baseline.

#### [`20bdf23`](https://github.com/example/android-native-football/commit/20bdf23) - 2026-10-09
*Author: GameplayFootball Dev*
##### Added
- Prototype analog stick determinism and minimalist HUD (superseded and replaced cleanly in `7898447`).

#### [`abf0aef`](https://github.com/example/android-native-football/commit/abf0aef) - 2026-10-09
*Author: GameplayFootball Dev*
##### Fixed
- **Goalkeeper Pass Right-Bias**:
  - Resolved default rightward directional bias in `AIfunctions.cpp` when aiming passes with the goalkeeper.
- **Dribble Movement Interruption**:
  - Prevented pass and shoot charge actions from immediately canceling dribble movement.
