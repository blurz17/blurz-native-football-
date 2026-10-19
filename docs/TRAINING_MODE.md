# Training Mode Subsystem

> [!NOTE]
> For the modular subsystem architecture and user walkthroughs, see:
> - **[Training Mode Subsystem Architecture](architecture/training_mode_subsystem.md)**
> - **[Training Mode User Guide & Drill Strategies](guides/training_mode_user_guide.md)**
> - **[ADR-003: Training Mode Isolation & Atomics](adr/ADR-003_training_mode_isolation_and_atomic_queues.md)**
> - **[Master Documentation Hub](README.md)**

The Training Mode is an isolated, decoupled subsystem designed for targeted football skill development without competing against opponent AI.

---

## 1. Design Principles & Decoupling

* **Zero Interference with Match Mode**: The training system resides in `game/training/` and interacts with `Match` exclusively through well-defined hooks. Match mode logic remains clean and unmodified.
* **Thread-Safe Command Queuing**: In-game UI actions (changing drills, resetting positions, switching camera angles) execute via lock-free atomics on the game thread.
* **Autonomous Reset Mechanics**: Automatically resets after goals, out-of-bounds plays, or goalkeeper catches with friendly HUD banner feedback.

---

## 2. Drill Specifications

### 🟢 1. Free Play (Open Pitch Practice)
* **Start Position**: Exact center spot of the stadium: `Vector3(0.0f, 0.0f, 0.11f)`.
* **Ball Dynamics**: Stationary at the player's feet with zero initial momentum (`SetMomentum(0, 0, 0)`).
* **Teammate Deployment**: Full 11-player squad distributed naturally in their tactical formation across your half and midfield. You can practice one-twos, build-up play, long passes, through-balls, and crosses.
* **Opponent AI Suppression**:
  * Opponent outfield players are parked off the pitch on the sideline benches (`y = pitchHalfH + 3.0f`) with their AI decision routines disabled.
  * The opponent goalkeeper stays on the goal line to guard and save shots if you shoot, but will **never rush 20 meters out** to tackle or dispossess you while you dribble.
* **Reset Conditions**:
  * Ball scored in net $\rightarrow$ Goal message and reset after 1.5s.
  * Ball kicked out of bounds $\rightarrow$ Reset after 1.2s.
  * Ball caught/saved by the goalkeeper $\rightarrow$ "Saved by the keeper!" and reset after 1.5s.
  * Instant manual reset via "RESET" on HUD or pause menu.

---

### 🎯 2. Free Kicks
* **Setup**: Direct dead-ball scenario against a 3-man defensive wall and goalkeeper.
* **4 Realistic Variations**:
  1. **Center (24m)**: Classic direct shooting range outside the penalty arc.
  2. **Left Angle (23m)**: Curled shots and far-post crosses from the left channel.
  3. **Right Angle (23m)**: Curled shots and far-post crosses from the right channel.
  4. **Long Range (32m)**: Power-shot distance outside the 30-meter mark.
* **Camera**: Over-the-shoulder 3rd-person perspective positioned behind the kicker, looking over the defensive wall into the net corners.

---

### 🚩 3. Corner Kicks
* **Setup**: Set-piece delivery from the corner flag into a crowded penalty box.
* **Variations**:
  * **Left Corner**: In-swinging and out-swinging deliveries from the left corner flag.
  * **Right Corner**: In-swinging and out-swinging deliveries from the right corner flag.
* **Camera**: Angled behind the corner taker, looking directly across the 6-yard box and penalty spot.

---

### 🥅 4. Penalty Shootout
* **Setup**: 1v1 spot kick from 11 meters against the goalkeeper.
* **Camera**: Low-angle behind-the-kicker camera (FOV 36°) aligned with the center of the goal.

---

## 3. Training Pause Menu (PES Mobile Style)

Tapping the **Pause** icon while in Training Mode opens a dedicated 6-card overlay:

```
┌─────────────────────────────────────────────────────────────┐
│                 TRAINING & DRILL PRACTICE                   │
├─────────────────┬─────────────────────────┬─────────────────┤
│   [Free Play]   │       [Free Kick]       │  [Corner Kick]  │
│  Open pitch     │   Direct shots vs GK    │ Crosses/headers │
├─────────────────┼─────────────────────────┼─────────────────┤
│ [Penalty Kick]  │      [Angle / Side]     │  [Player Cam]   │
│  1v1 spot kick  │ Switch drill variation  │ Behind Player   │
├─────────────────┴─────────────────────────┴─────────────────┤
│           [ 🔄 RESET DRILL ]      [ ▶ RESUME ]              │
└─────────────────────────────────────────────────────────────┘
```

* **Instant Drill Switching**: Tap any drill card to immediately teleport to that scenario.
* **Angle / Variation Toggle**: Cycles through variations (e.g. Free kick angles, corner sides).
* **Player Cam Toggle**: Toggles between 3rd-person Player View and Broadcast/Tactical View.
* **Reset Drill**: Instantly re-positions ball, kicker, and teammates.
