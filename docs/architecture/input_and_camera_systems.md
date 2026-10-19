# Touch Input & Camera Systems

## 1. Zero-Latency Touch Input Architecture

The mobile touch system was engineered to deliver responsive, console-quality control without the latency and quantization limitations of generic game framework event loops.

```
┌────────────────────────────────────────────────────────┐
│             Android MotionEvent (Java Touch)           │
│   • Multi-touch pointer tracking                       │
│   • Dynamic floating analog joystick base             │
│   • Circular action buttons (Pass, Through, Shoot, Dash)│
└───────────────────────────┬────────────────────────────┘
                            │ Direct JNI Method Calls
┌───────────────────────────▼────────────────────────────┐
│                  Native UserEventManager               │
│   • Atomic float axes: mobileDirectionX, Y             │
│   • Bitmask button flags: mobileButtonsDown            │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                    HumanController                     │
│   • Reads UserEventManager                             │
│   • Evaluates 360° magnitude and direction             │
│   • Applies SO(2) camera-relative basis projection     │
│   • Drives humanoid locomotion state machine           │
└────────────────────────────────────────────────────────┘
```

### 1.1 Dynamic Follow Joystick
Unlike fixed virtual joysticks that penalize players when their thumb drifts across the glass, the joystick in `MobileControlsView.java` uses dynamic anchor tracking:
- **Touch Down**: Initial contact on the left screen half defines the joystick center anchor $(C_x, C_y)$.
- **Touch Drag**: Vector $\Delta = P_{\text{touch}} - C$ determines direction and normalized magnitude:
  $$M = \min\left(1.0, \frac{\|\Delta\|}{R_{\text{max}}}\right), \quad \hat{u} = \frac{\Delta}{\|\Delta\|}$$
- **Anchor Dragging**: If the thumb exceeds $1.3 \times R_{\text{max}}$, the anchor $C$ smoothly follows the touch position, ensuring zero lost input.

---

## 2. 3rd-Person Player Camera & $\text{SO}(2)$ Directional Projection

### 2.1 The Camera System
The engine supports multiple camera modes (`e_CameraMode`):
- `e_CameraMode_Normal`: Classic broadcast tele camera with dynamic zoom, tracking the centroid of play.
- `e_CameraMode_Player`: 3rd-person dynamic tracking camera positioned behind the user's active player.
- Set-Piece Cameras: Behind-the-kicker views for Free Kicks and Penalty Kicks.

### 2.2 The 90° Inverted Axis Problem & Mathematical Solution

#### Problem Formulation
In world pitch coordinates:
- $+Y$ represents North (toward opponent goal for Home team).
- $+X$ represents East (toward touchline).

In 3rd-person view, when the camera is behind the player looking forward along the pitch, an analog stick swipe UP ($J_y = 1.0$) was mistakenly treated as world $+Y$. Depending on camera azimuth, this caused the player to run 90° sideways or backwards.

#### $\text{SO}(2)$ Planar Basis Projection
To guarantee that swiping **UP** always moves forward away from the screen, and swiping **RIGHT** moves right across the screen, the input vector must be projected onto the horizontal camera frame.

1. **Extract 2D Horizontal Camera Forward Vector**:
   Let $\vec{P}_{\text{cam}}$ be camera eye position and $\vec{L}_{\text{cam}}$ be camera look-at position in world space:
   $$\vec{F}_{\text{3D}} = \vec{L}_{\text{cam}} - \vec{P}_{\text{cam}}$$
   Project onto the 2D pitch plane ($z = 0$):
   $$\vec{F}_{\text{2D}} = \frac{(F_x, F_y, 0)}{\sqrt{F_x^2 + F_y^2}}$$

2. **Construct Orthogonal Right Vector**:
   Using the 2D rotation matrix for $-90^\circ$ (or perpendicular cross product with $+Z$ up vector):
   $$\vec{R}_{\text{2D}} = (F_y, -F_x, 0)$$

3. **Project User Screen Input $(J_x, J_y)$**:
   $$\vec{D}_{\text{world}} = J_y \cdot \vec{F}_{\text{2D}} + J_x \cdot \vec{R}_{\text{2D}}$$

```
                ▲ Screen UP (Jy = 1)
                │
                │   F_2D (Camera Forward)
                │  ▲
                │ /
                │/
  ◄─────────────┼─────────────► Screen RIGHT (Jx = 1)
 Screen LEFT    │\
                │ \
                │  ▼ R_2D (Camera Right)
                │
                ▼ Screen DOWN (Jy = -1)
```

### 2.3 Implementation in `HumanController`

```cpp
Vector3 HumanController::_TransformInputDirection(const Vector3 &rawInput) const {
    if (match->GetCameraMode() != e_CameraMode_Player) {
        return rawInput;
    }
    Vector3 forward = match->GetCamera2DForward();
    Vector3 right(forward.coords[1], -forward.coords[0], 0.0f);
    
    Vector3 projected = forward * rawInput.coords[1] + right * rawInput.coords[0];
    if (projected.GetLength() > 1.0f) {
        projected.Normalize();
    }
    return projected;
}
```

This transform is uniformly applied to:
1. `HumanController::GetDirection()` (Player movement locomotion).
2. `Team::Process()` (Manual player switching using flick gestures).

---

## 3. Broadcast Camera Presets

Three tuned broadcast camera presets are available via the in-game settings:

| Preset Name | Description | Zoom | Angle | Height | Typical Usage |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **Preset 1 (Balanced)** | Standard TV broadcast angle | 12% | 50% | 15% | Ideal for mobile screens, high field visibility |
| **Preset 2 (Tactical)** | High vantage tactical view | 12% | 50% | 50% | Wide tactical overview of player runs |
| **Preset 3 (Action)** | Low dynamic action follow | Dynamic | 35% | 10% | Dynamic close-up framing |
