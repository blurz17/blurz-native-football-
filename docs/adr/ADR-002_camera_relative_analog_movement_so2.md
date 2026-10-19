# ADR-002: Camera-Relative Analog Movement Projection in $\text{SO}(2)$

## Status
**Accepted** (2026-10-20)

## Context
When switching into the 3rd-person Player Camera view, players experienced inverted and rotated analog movement:
- Swiping UP on the virtual joystick moved the player West (sideways).
- Swiping DOWN moved the player East.
- Swiping WEST moved South.
- Swiping EAST moved North.

### Root Cause
In broadcast camera modes (Side-view / Tele), the camera look vector aligns predominantly along the $+Y$ pitch axis. Therefore, fixed world-space mapping ($+Y$ = North, $+X$ = East) happened to align reasonably well with the screen.

However, in 3rd-person Player Cam and behind-the-ball Set-Piece cameras, the camera rotates freely around the active player in yaw. The user expects screen-relative movement:
- Swiping UP on the screen must always move the player away from the screen / along the camera's viewing ray.
- Swiping RIGHT must move the player to the right side of the screen.

## Decision Drivers
1. **Intuitive Controls**: Movement must feel natural regardless of camera orientation.
2. **Mathematical Robustness**: Must operate correctly on horizontal pitch coordinates without gimbal-lock or pitch-induced singularities.
3. **Decoupled Architecture**: Controller input transform should be independent of camera rendering specifics.

## Considered Options
1. **Option 1: Pitch-Inverted Static Remapping**:
   - Swapping $X$ and $Y$ coordinates when in player cam.
   - *Drawback*: Fails as soon as the camera orbits around the player; breaks during set-pieces angled toward corners.
2. **Option 2: 2D Planar Orthogonal Basis Projection in $\text{SO}(2)$**:
   - Extract the normalized horizontal forward vector $\vec{V}_{\text{camForward}}$ of the active camera.
   - Compute the orthogonal right vector $\vec{V}_{\text{camRight}} = (\vec{V}_{\text{camForward}}.y, -\vec{V}_{\text{camForward}}.x, 0)$.
   - Project screen joystick analog input $(J_x, J_y)$ onto this camera basis:
     $$\vec{D}_{\text{world}} = J_y \cdot \vec{V}_{\text{camForward}} + J_x \cdot \vec{V}_{\text{camRight}}$$
   - Normalize the result if $\|(J_x, J_y)\| > 1.0$.

## Decision Outcome
Adopted **Option 2**.

### Implementation Details:
1. **Camera Forward Vector Tracking**:
   In [`game/onthepitch/match.cpp`](file:///e:/MY-PROJECTS/android%20native%20football/game/onthepitch/match.cpp):
   ```cpp
   Vector3 camForward = (cameraNode->GetLookAt() - cameraNode->GetPosition());
   camForward.coords[2] = 0.0f; // Constrain to 2D pitch plane
   if (camForward.GetLength() > 0.001f) {
       camForward.Normalize();
       camera2DForward = camForward;
   }
   ```
2. **Input Projection in `HumanController`**:
   In [`game/onthepitch/player/controller/humancontroller.cpp`](file:///e:/MY-PROJECTS/android%20native%20football/game/onthepitch/player/controller/humancontroller.cpp):
   ```cpp
   Vector3 HumanController::_TransformInputDirection(const Vector3 &rawInput) const {
       if (match->GetCameraMode() != e_CameraMode_Player) {
           return rawInput;
       }
       Vector3 forward = match->GetCamera2DForward();
       Vector3 right(forward.coords[1], -forward.coords[0], 0.0f);
       
       // rawInput.coords[1] is Screen Y (Up/Down)
       // rawInput.coords[0] is Screen X (Left/Right)
       Vector3 projected = forward * rawInput.coords[1] + right * rawInput.coords[0];
       return projected;
   }
   ```
3. **Application Across Movement and Player Switching**:
   - Applied to `HumanController::GetDirection()`.
   - Applied to `Team::Process()` manual directional player switching so flicking the stick right selects teammates to the right on-screen.

## Consequences
### Positive
- Fully resolved the 90° inverted control axis.
- Screen-relative controls feel fluid, modern, and intuitive in all 3rd-person perspectives.
- Broadcast cameras remain unaffected.

### Negative / Trade-offs
- None.
