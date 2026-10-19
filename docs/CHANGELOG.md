# Project Changelog & Milestone History

This document chronicles all major features, performance investigations, bug fixes, and architectural upgrades implemented in the project.

---

## [Milestone 6] - Camera-Relative 360° Controls
### Fixed
* **Inverted / 90° Rotated Controls in Player View**:
  * Resolved an issue where swiping UP in 3rd-person view caused the player to run sideways (West) rather than forward toward the goal.
  * Added `camera2DForward` tracking in `Match::UpdateIngameCamera()` across all player cam branches.
  * Implemented `HumanController::_TransformInputDirection()` to project screen-space analog coordinates onto the camera's forward and right vectors:
    $$\vec{D} = \text{screenY} \cdot \vec{V}_{\text{camForward}} + \text{screenX} \cdot \vec{V}_{\text{camRight}}$$
  * Updated `HumanController::GetDirection()` and `Team::Process()` (manual player switching) to respect camera-relative directions.

---

## [Milestone 5] - Free Play Midfield Start & AI Suppression
### Added
* **Center Stadium Kick-off**:
  * Free Play now initializes at the center spot: `Vector3(0.0f, 0.0f, 0.11f)` with ball velocity set to zero (`SetMomentum(0, 0, 0)`).
  * Automatically positions the central playmaker/attacker directly behind the ball inside the center circle facing the opponent half.
* **Full Squad Tactical Deployment**:
  * Teammates are mapped to their tactical formation across the home half and midfield, enabling realistic passing, one-twos, and build-up play across the entire pitch.
* **Opponent AI Suppression**:
  * Parked non-active opponent outfielders completely off the playing field (`y = pitchHalfH + 3.0f`) with AI processing disabled in `ElizaController` and `TeamAIController`.
  * Disabled aggressive goalkeeper rushing in `GoalieDefaultStrategy` during training mode (`awayFromGoalBias = 0.05f`), keeping the keeper on the goal line to guard and save shots without tackling.
  * Added auto-reset on goalkeeper catch/save: "Saved by the keeper! Resetting...".

---

## [Milestone 4] - Multi-Threaded Stability & Crash Fix
### Fixed
* **Null Pointer Dereference (`SIGSEGV`) in `FetchPutBuffers()`**:
  * Diagnosed data race on `HumanoidBase::buf_animApplyBuffer` between the game thread (`PreparePutBuffers`, `ResetSituation`) and render worker `Thread-6` (`FetchPutBuffers`).
  * Added `boost::mutex putBufferMutex;` to `HumanoidBase` protecting all reads and writes to animation buffers.
  * Implemented an explicit, safe copy assignment operator in `AnimApplyBuffer`.
  * Decoupled JNI UI commands (`SetDrill`, `ResetDrill`, `NextDrill`, `NextVariation`, `TogglePlayerCam`) into lock-free atomic queues in `TrainingManager`, executed synchronously on the game thread.

---

## [Milestone 3] - Dedicated Training Mode Subsystem
### Added
* **Decoupled Architecture**:
  * Created `game/training/training_manager.hpp`, `training_manager.cpp`, and `training_mode.hpp`.
  * Supports 4 complete drill types:
    1. **Free Play**: Open-pitch passing, shooting, and dribbling practice.
    2. **Free Kicks**: 4 variations (Center 24m, Left Angle 23m, Right Angle 23m, Long Range 32m) with 3-man wall and goalkeeper.
    3. **Corner Kicks**: Cross deliveries from left and right corners.
    4. **Penalty Shootout**: 11-meter 1v1 spot kick against the goalkeeper.
* **PES Mobile UI Integration**:
  * Created 6-card training pause overlay in `MobileControlsView.java`.
  * Added live HUD banners showing active drill, score, and variation.
  * Added Main Menu launcher with dedicated training drill picker cards in `MainMenuActivity.java`.

---

## [Milestone 2] - Performance Profiling & Shader Investigation
### Investigated
* Profiled framerate on target hardware (Samsung Galaxy A54 / Exynos 1380 / Mali-G68 MP5).
* Evaluated binary glTF asset pipelines and GPU skinning shader paths.
* Reverted unbeneficial experimental shader code to preserve stable OpenGL ES 2.0 / 3.0 baseline compatibility.

---

## [Milestone 1] - Realistic Models & Camera Presets
### Added
* **CR7 3D Model & Al Rihla Ball**:
  * Integrated detailed Cristiano Ronaldo 3D model with authentic Al Nassr kit texturing.
  * Added official FIFA World Cup Al Rihla match ball textures and materials.
* **Camera Preset Customization**:
  * Configured three broadcast camera presets (Preset 1: Zoom 12%, Angle 50%, Height 15%; Preset 2: Zoom 12%, Angle 50%, Height 50%; Preset 3: Action view).
  * Expanded zoom limits and adjusted field-of-view scaling.
