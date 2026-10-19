# Architectural Decision Records (ADR)

This directory documents the key architectural decisions made in the Android Native Football project.
Each record follows the standard Michael Nygard format, describing the context, decisions, and consequences.

---

## Index of Architectural Decisions

| ADR ID | Title | Status | Date | Primary Component |
| :--- | :--- | :--- | :--- | :--- |
| **[ADR-001](ADR-001_putbuffer_mutex_synchronization.md)** | Thread Synchronization for Render Worker Buffer Swaps (`putBufferMutex`) | **Accepted** | 2026-10-20 | `HumanoidBase`, `GameTask` |
| **[ADR-002](ADR-002_camera_relative_analog_movement_so2.md)** | Camera-Relative Analog Movement Projection in $\text{SO}(2)$ | **Accepted** | 2026-10-20 | `HumanController`, `Match` |
| **[ADR-003](ADR-003_training_mode_isolation_and_atomic_queues.md)** | Training Mode Decoupling and Lock-Free Atomic JNI Command Queues | **Accepted** | 2026-10-20 | `TrainingManager`, `Match` |
| **[ADR-004](ADR-004_modern_asset_pipeline_gltf_fbx_auto_discovery.md)** | Dual Modern 3D Asset Loaders (glTF 2.0 / FBX) with Transparent Fallback | **Accepted** | 2026-10-17 | `ResourceManager`, `GLTFLoader`, `FBXLoader` |
| **[ADR-005](ADR-005_direct_jni_touch_input_pipeline.md)** | Zero-Latency Direct JNI Touch Input Dispatch vs. SDL Event Polling | **Accepted** | 2026-10-09 | `MobileControlsView`, `UserEventManager` |
| **[ADR-006](ADR-006_display_refresh_rate_and_scheduler_timing.md)** | Android High Refresh Rate Management and Scheduler Frame Timing | **Accepted** | 2026-10-17 | `SDLActivity`, `Scheduler` |

---

## ADR Process & Standards

When introducing new architectural changes or major refactors:
1. Create a new record in `docs/adr/ADR-XXX_descriptive_title.md`.
2. Follow the standardized structure:
   - **Title**: `ADR-XXX: <Short Title>`
   - **Status**: `Proposed` | `Accepted` | `Superseded by ADR-YYY` | `Deprecated`
   - **Context**: Problem statement, background, failure logs, and technical constraints.
   - **Decision Drivers**: Factors that influenced the decision.
   - **Considered Options**: Alternative approaches evaluated.
   - **Decision Outcome**: Selected solution and architecture.
   - **Consequences**: Positive effects, trade-offs, and downstream impacts.
   - **Validation**: Testing evidence or log verification.
3. Update this index table.
