# ADR-003: Isolated Training Subsystem and Lock-Free Atomic JNI Command Queues

## Status
**Accepted** (2026-10-20)

## Context
The user requested a dedicated, isolated Training Mode to practice passing, shooting, free kicks, corners, and penalties without the constraints of an active match or opponent AI interference.

### Requirements:
1. **Isolation & Decoupling**: The training feature must be decoupled from the standard 90-minute match state machine to allow independent evolution, extension, and maintenance.
2. **AI Defense Suppression**: In Free Play, the user's squad must have complete tactical freedom across the pitch without opponent outfielders tackling or intercepting passes. The opponent goalkeeper should guard the net without aggressively charging out.
3. **Thread Safety across Android JNI & Native C++**: Drill changes and resets are triggered from Android Java UI threads (`MobileControlsView.java`) while match simulation executes on native C++ `Thread-1`. Calling mutating native methods directly across threads causes race conditions.

## Decision Drivers
1. Modularity: No invasive spaghetti code inside `match.cpp`.
2. Concurrency Safety: Complete absence of race conditions between UI and simulation.
3. Responsive UX: Instant scenario resets and feedback.

## Decision Outcome
Created a dedicated subsystem in `game/training/`:
- `game/training/training_mode.hpp`: Drill enumeration (`e_TrainingDrill_FreePlay`, `FreeKick`, `CornerKick`, `PenaltyShootout`) and drill states.
- `game/training/training_manager.hpp` & `.cpp`: Central orchestrator managing drill logic, wall setup, scoring, and situation resets.

### 1. Lock-Free Atomic JNI Command Queues
Instead of executing UI events synchronously in native methods called by JNI, requests are queued atomically:
```cpp
// In TrainingManager
std::atomic<int> pendingSetDrill{-1};
std::atomic<bool> pendingResetRequest{false};
std::atomic<bool> pendingNextDrill{false};
std::atomic<bool> pendingPrevDrill{false};
std::atomic<bool> pendingNextVariation{false};
```
During the match loop (`Match::Process()`), the game thread polls and executes pending atomic requests before simulation ticks, guaranteeing thread safety:
```cpp
void TrainingManager::Process() {
    int reqDrill = pendingSetDrill.exchange(-1);
    if (reqDrill >= 0) {
        SetDrill((e_TrainingDrill)reqDrill);
    }
    if (pendingResetRequest.exchange(false)) {
        ResetDrill();
    }
    // ...
}
```

### 2. Tactical Free Play Layout & Opponent AI Suppression
- **Center Stadium Start**:
  - Ball placed at `(0.0f, 0.0f, 0.11f)` with zero momentum.
  - Active attacker placed directly behind the ball inside the center circle facing the opponent half.
- **Full Squad Deployment**:
  - Teammates positioned in standard formation across the midfield and home half.
- **Opponent Outfielder Parking**:
  - Non-active opponent outfielders moved off the pitch (`y = pitchHalfH + 3.0f`) with AI disabled in `ElizaController` and `TeamAIController`.
- **Goalkeeper Rushing Disabled**:
  - In `GoalieDefaultStrategy`, `awayFromGoalBias` reduced to `0.05f` during training, anchoring the keeper to the goal line.

## Consequences
### Positive
- Robust, decoupled architecture with clear extension points for future drills.
- Zero thread crashes during UI button mashing.
- Pure practice environment without AI interference.

### Negative / Trade-offs
- UI drill switches take up to one simulation frame (10ms) to take effect, which is imperceptible to users.
