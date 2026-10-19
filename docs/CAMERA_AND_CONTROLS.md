# Camera Systems & Camera-Relative Controls

> [!NOTE]
> For the modular subsystem architecture and mathematical formulations, see:
> - **[Touch Input & Camera Systems Architecture](architecture/input_and_camera_systems.md)**
> - **[Mobile Controls & HUD Specification](reference/controls_and_hud_spec.md)**
> - **[ADR-002: Camera-Relative Analog Movement in SO(2)](adr/ADR-002_camera_relative_analog_movement_so2.md)**
> - **[Master Documentation Hub](README.md)**

This document details the camera modes, 3rd-person Player Cam tracking, and the mathematical resolution of screen-relative vs. world-relative analog movement.

---

## 1. Camera Modes

The game supports two primary camera paradigms:

### 1. Broadcast / Tactical Camera (Side Views)
Positioned along the sideline looking across the pitch (toward the **+Y axis**). Provides a broad tactical overview suitable for team play.

* **Preset 1 (Wide Tactical)**: Zoom `12%`, Camera Angle `50%`, Height `15%`.
* **Preset 2 (Elevated Broadcast)**: Zoom `12%`, Camera Angle `50%`, Height `50%`.
* **Preset 3 (Dynamic Action)**: Zoom `0%`, Camera Angle `65%`, Height `35%`.

---

### 2. Player Camera (3rd-Person View)
Positions the camera directly behind the user-controlled player for an immersive, behind-the-back perspective:

* **Free Play Mode**:
  * Tracks `GetDesignatedPossessionPlayer()`.
  * Positioned **8.5 meters behind** the player at an elevation of **3.2 meters** (FOV: `42°`).
  * Dynamically interpolates toward the opponent goal while tracking forward passes and shots.
* **Set Pieces (Free Kicks & Penalties)**:
  * Positioned behind the kicker looking over the wall directly into the goal corners (FOV: `36°`).
* **Corner Kicks**:
  * Positioned behind the corner taker, angled across the 6-yard box and far post (FOV: `40°`).

---

## 2. The 90° Axis Rotation Problem

### The Symptom
When testing the 3rd-person Player View, the virtual joystick felt rotated/inverted:
* Swiping **UP** made the player move **West / Left** (toward the sideline).
* Swiping **DOWN** made the player move **East / Right**.
* Swiping **RIGHT** made the player move **Forward** (toward the goal).
* Swiping **LEFT** made the player move **Backward**.

In broadcast camera mode, controls worked normally.

### The Root Cause
1. **Pitch Coordinate System**:
   * **X Axis**: Length of the pitch from goal to goal (West to East).
   * **Y Axis**: Width of the pitch from sideline to sideline (South to North).
   * **Z Axis**: Altitude (Up).

2. **Camera Orientation**:
   * In **Broadcast Camera**, the camera looks across the pitch along **+Y**.
     * Screen Right ($X$) $\rightarrow$ Pitch $+X$ (East).
     * Screen Up ($Y$) $\rightarrow$ Pitch $+Y$ (North).
     The unrotated joystick output directly matched pitch coordinates.
   * In **Player View**, the camera turns **90°** to look along **+X** (toward the goal).
     * Forward on screen is along the pitch $+X$ axis.
     * Right on screen is along the pitch $-Y$ axis.
     * Left on screen is along the pitch $+Y$ axis.

Because the controller was previously applying raw screen inputs directly to world pitch coordinates without factoring in the camera's angle, controls were orthogonal to what the player saw on screen.

---

## 3. Mathematical Resolution: Camera-Relative SO(2) Projection

To ensure intuitive control across any camera angle, we project the 2D analog stick input into camera-relative pitch coordinates.

### Formulation
Let $\vec{F} = (F_x, F_y, 0)$ be the camera's normalized forward look vector projected onto the 2D pitch plane:
$$\vec{F} = \frac{\vec{V}_{\text{lookTarget}} - \vec{V}_{\text{camPos}}}{\|\vec{V}_{\text{lookTarget}} - \vec{V}_{\text{camPos}}\|}$$

The camera's orthogonal right vector $\vec{R} = (R_x, R_y, 0)$ rotated $90^\circ$ clockwise on the XY pitch plane is:
$$\vec{R} = (F_y, -F_x, 0)$$

For any joystick input $(\text{screenX}, \text{screenY})$ where:
* $\text{screenY} \in [-1, 1]$ (Up is $+1$, Down is $-1$)
* $\text{screenX} \in [-1, 1]$ (Right is $+1$, Left is $-1$)

The resulting world movement direction $\vec{D}_{\text{world}}$ is:
$$\vec{D}_{\text{world}} = \text{screenY} \cdot \vec{F} + \text{screenX} \cdot \vec{R}$$

In scalar coordinates:
$$D_x = \text{screenY} \cdot F_x + \text{screenX} \cdot F_y$$
$$D_y = \text{screenY} \cdot F_y - \text{screenX} \cdot F_x$$

---

### Verification Across Angles

#### Case A: Player View (Attacking East, $\vec{F} = (1, 0, 0)$)
* $\vec{R} = (0, -1, 0)$
* **Swipe UP** ($\text{screenY} = 1, \text{screenX} = 0$):
  $$\vec{D} = 1 \cdot (1, 0, 0) + 0 \cdot (0, -1, 0) = (1, 0, 0) \quad \text{[Forward toward goal]}$$
* **Swipe DOWN** ($\text{screenY} = -1, \text{screenX} = 0$):
  $$\vec{D} = -1 \cdot (1, 0, 0) = (-1, 0, 0) \quad \text{[Backward toward own half]}$$
* **Swipe RIGHT** ($\text{screenY} = 0, \text{screenX} = 1$):
  $$\vec{D} = 1 \cdot (0, -1, 0) = (0, -1, 0) \quad \text{[Right on screen]}$$
* **Swipe LEFT** ($\text{screenY} = 0, \text{screenX} = -1$):
  $$\vec{D} = -1 \cdot (0, -1, 0) = (0, 1, 0) \quad \text{[Left on screen]}$$

#### Case B: Broadcast Camera ($\vec{F} = (0, 1, 0)$)
* $\vec{R} = (1, 0, 0)$
* $D_x = \text{screenY} \cdot 0 + \text{screenX} \cdot 1 = \text{screenX}$
* $D_y = \text{screenY} \cdot 1 - \text{screenX} \cdot 0 = \text{screenY}$
$$\vec{D} = (\text{screenX}, \text{screenY}, 0) \quad \text{[Identity - standard sideline controls preserved]}$$

---

## 4. Systems Benefiting from Camera Projection

The camera projection transform is implemented in [`HumanController::_TransformInputDirection()`](file:///e:/MY-PROJECTS/android%20native%20football/game/onthepitch/player/controller/humancontroller.cpp#L529) and automatically powers:
* **Analog Dribbling & Movement**: Smooth 360° steering relative to the camera view.
* **Pass Aiming (Ground & High Passes)**: Passes travel in the direction pointed on screen.
* **Shot Direction**: Power shots aim toward the corners of the goal as seen on screen.
* **Player Switching**: Flicking the switch stick toward a player on screen correctly selects that player via [`Team::Process()`](file:///e:/MY-PROJECTS/android%20native%20football/game/onthepitch/team.cpp#L451).
