# Mobile Controls & HUD Specification

This document details the visual design, screen layout, button dimensions, and touch response specifications for the mobile HUD in Android Native Football.

---

## 1. Screen Layout Overview

The UI layer is rendered in `MobileControlsView.java` as a transparent, high-performance overlay directly on top of the native `SDLSurfaceView`.

```
┌────────────────────────────────────────────────────────────────────────┐
│ [Score / Radar]        [Training HUD Banner]             [Cam] [Pause] │
│                                                                        │
│                                                                        │
│                                                                        │
│                                                          (Pass)        │
│    ( Floating 360° )                               (Through)    (Shoot)│
│    ( Virtual Stick )                                                   │
│                                                          (Sprint)      │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Touch Control Specifications

### 2.1 Dynamic Virtual Joystick (Left Screen)
- **Active Area**: Left 50% of the screen ($0.0 \le X \le 0.5 \times W$).
- **Outer Radius ($R_{\text{base}}$)**: $64\text{ dp}$ (scaled by device display density).
- **Thumb Cap Radius ($R_{\text{cap}}$)**: $32\text{ dp}$.
- **Max Travel Distance**: $60\text{ dp}$.
- **Anchor Mode**: Dynamic floating anchor. First finger down defines the center. If finger drags beyond $1.3 \times R_{\text{base}}$, the base smoothly slides toward the finger.
- **Deadzone**: $0.08$ ($8\%$ magnitude threshold to prevent resting thumb jitter).

---

### 2.2 Action Buttons (Right Screen)
Buttons are rendered in an ergonomic diamond layout matching modern mobile football standards (EA Sports FC Mobile / PES Mobile):

| Button Glyph | Action (Possession) | Action (Defense) | Theme Color | Position Offset | Diameter |
| :---: | :---: | :---: | :---: | :---: | :---: |
| **PASS** | Short Ground Pass | Press / Contain | Cyan Gradient (`#00E5FF`) | Top of diamond | $56\text{ dp}$ |
| **THROUGH** | Angled Through Ball | Keeper Rush | Yellow Gradient (`#FFD600`) | Left of diamond | $52\text{ dp}$ |
| **SHOOT** | Power Shot | Clearance / Block | Red/Coral Gradient (`#FF3D00`) | Right of diamond | $60\text{ dp}$ |
| **SPRINT** | Dash / Speed Burst | Sprint / Pressure | Green Gradient (`#00E676`) | Bottom of diamond | $56\text{ dp}$ |

- **Touch Feedback**: Radial scaling ($0.92\times$) and high-contrast brightness boost on `ACTION_DOWN`.

---

## 3. Training Mode HUD Banner
When Training Mode is active, a status banner is displayed at the top center of the screen:
- **Dimensions**: $260\text{ dp} \times 38\text{ dp}$, rounded corners ($19\text{ dp}$ radius).
- **Background**: Translucent charcoal (`#D91E242B`) with a subtle teal border stroke (`#00B4D8`).
- **Typography**:
  - Title: Bold Condensed Gothic, white text, 14sp.
  - Subtitle: Variation and score indicator, light cyan (`#90E0EF`), 11sp.

---

## 4. Pause Menu Specifications

### 4.1 PES Mobile 6-Card Grid
When paused, the screen dims with a 60% black scrim, and a 6-card grid is centered:
- **Card Aspect Ratio**: $1.2 : 1.0$.
- **Card Styling**: Frosted glass dark cards (`#E61A222D`) with $12\text{ dp}$ corner radii and subtle $1\text{ dp}$ edge borders (`#334155`).
- **Typography**: Centered bold card title, 13sp uppercase, paired with a distinct vector icon glyph.
- **Touch Interaction**: Smooth alpha feedback and instant modal presentation without stuttering the engine.
