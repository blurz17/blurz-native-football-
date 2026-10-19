# Training Mode Subsystem

## 1. Overview & Architectural Goals

The Training Mode subsystem was designed to fulfill a critical design requirement: **complete decoupling from the standard match rules and AI state machines**, allowing players to practice fundamentals (passing, dribbling, shooting, set-pieces) with complete autonomy and zero AI interference.

```
┌──────────────────────────────────────────────────────────────┐
│                    MobileControlsView (UI)                   │
│   • 6-Card Training Overlay  • Live Top HUD Drill Banner     │
└──────────────────────────────┬───────────────────────────────┘
                               │ JNI (Atomic Request Queues)
┌──────────────────────────────▼───────────────────────────────┐
│               TrainingManager (game/training/)               │
│   • Drill State Machine (Active Drill, Variation, Scores)    │
│   • Pitch Setup & Scenario Positioning                       │
│   • Goal Detection & Auto-Reset Callbacks                    │
└──────────────────────────────┬───────────────────────────────┘
                               │ Direct Controls & Suppression
┌──────────────────────────────▼───────────────────────────────┐
│                        Match Engine                          │
│   • Match::ResetSituation()                                  │
│   • Opponent Outfielder Suppression (Parked off field)       │
│   • Opponent Goalkeeper Rush Suppression                     │
│   • Full Teammate Squad Tactical Positioning                 │
└──────────────────────────────────────────────────────────────┘
```

---

## 2. Supported Training Drills

### 2.1 Free Play (`e_TrainingDrill_FreePlay`)
- **Objective**: Open-pitch passing, shooting, and dribbling practice across the entire stadium.
- **Ball Initial State**:
  - Placed at the exact center spot: `Vector3(0.0f, 0.0f, 0.11f)`.
  - Velocity and angular momentum set to zero: `SetMomentum(0, 0, 0)`.
- **Player Placement**:
  - User's primary attacker/playmaker positioned inside the center circle directly behind the ball facing North.
  - User's 10 teammates deployed into their authentic tactical formation across the home half and midfield, enabling passing triangles, one-twos, and build-up play.
- **Opponent AI Suppression**:
  - All opponent outfielders are parked outside the field boundary line (`y = pitchHalfH + 3.0f`) with AI processing completely disabled in `ElizaController` and `TeamAIController`.
  - Opponent goalkeeper remains in the goal mouth to guard against shots. Goalkeeper rush AI (`awayFromGoalBias`) is capped at `0.05f` so the keeper stays on the line and will not aggressively charge down dribblers.
  - If the goalkeeper catches or saves the ball, the system displays a banner: `"Saved by the keeper! Resetting..."` and automatically resets after a 2-second delay.

### 2.2 Direct Free Kicks (`e_TrainingDrill_FreeKick`)
Features 4 tactical variations selectable via the training menu or quick toggle:

| Variation | Description | Ball Position $(X, Y)$ | Distance to Goal | Defensive Wall |
| :---: | :--- | :---: | :---: | :---: |
| **0** | Central Free Kick | $(0.0, 31.0)$ | 24 meters | 3-man wall at 9.15m |
| **1** | Left Angle Free Kick | $(-12.0, 32.0)$ | 23 meters | 3-man wall angled |
| **2** | Right Angle Free Kick | $(12.0, 32.0)$ | 23 meters | 3-man wall angled |
| **3** | Long Range Free Kick | $(4.0, 23.0)$ | 32 meters | 3-man wall at 9.15m |

- **Wall Positioning**: Placed along the ray between the ball and the center of the goal, exactly 9.15 meters from the ball.
- **Teammate Placement**: Attackers placed along the penalty box edge to contest rebounds.

### 2.3 Corner Kicks (`e_TrainingDrill_CornerKick`)
- **Variation 0 (Left Corner)**: Ball at $(-33.5, 54.0)$, kicker ready to deliver inswinging/outswinging crosses.
- **Variation 1 (Right Corner)**: Ball at $(33.5, 54.0)$.
- **Tactical Setup**: Multiple attacking players positioned inside the 6-yard box and penalty area for aerial headers and volleys against defensive markers.

### 2.4 Penalty Shootout (`e_TrainingDrill_Penalty`)
- **Ball Position**: Exactly 11 meters from the goal line at $(0.0, 44.0)$.
- **Setup**: Pure 1v1 duel between the penalty taker and the goalkeeper. All other outfielders positioned behind the penalty arc outside the 18-yard box.

---

## 3. Training Mode API Reference

[`game/training/training_manager.hpp`](file:///e:/MY-PROJECTS/android%20native%20football/game/training/training_manager.hpp):

```cpp
class TrainingManager {
public:
    TrainingManager(Match *match);
    ~TrainingManager();

    // Lifecycle
    void Initialize();
    void Process(); // Called from Match::Process()
    void ResetDrill();

    // Drill Selection
    void SetDrill(e_TrainingDrill drill);
    e_TrainingDrill GetCurrentDrill() const;
    void NextDrill();
    void PrevDrill();
    void NextVariation();

    // Thread-safe Atomic Requests from JNI
    void RequestSetDrill(e_TrainingDrill drill);
    void RequestReset();
    void RequestNextDrill();
    void RequestPrevDrill();
    void RequestNextVariation();

    // Scoring & Telemetry
    int GetGoalsScored() const;
    int GetAttempts() const;
    const std::string& GetDrillName() const;
    const std::string& GetVariationName() const;
};
```

---

## 4. UI Integration & Mobile Controls

### 4.1 6-Card PES Mobile Pause Overlay
In `MobileControlsView.java`, the pause overlay dynamically presents a 6-card grid tailored for training:
1. **Change Drill**: Cycles through Free Play, Free Kicks, Corners, and Penalties.
2. **Next Variation**: Switches angles, distances, or sides for the active drill.
3. **Reset Drill**: Instantly re-spots the ball and resets player positions.
4. **Camera View**: Toggles between Normal Broadcast Tele and 3rd-Person Player Cam.
5. **Match Settings**: Visual options, audio volume, and difficulty.
6. **Exit Training**: Returns to the main menu.

### 4.2 Live HUD Status Banner
A clean pill banner is rendered at the top of the screen:
- **Title**: `FREE PLAY (MIDFIELD)`, `FREE KICK (CENTER 24M)`, etc.
- **Score Counter**: Displays total goals vs. attempts: `Goals: 3 / 5`.
