# Threading Model & Memory Synchronization

## 1. Concurrency Architecture

The engine runs a multi-threaded architecture designed to decouple high-frequency simulation (100 Hz) from variable-rate rendering (60–120 FPS) and asynchronous mobile UI events.

```
┌──────────────────────────────────────────────────────────────┐
│                    Android UI Thread (Java)                  │
│       Processes MotionEvents, renders PES HUD overlays       │
└──────────────────────────────┬───────────────────────────────┘
                               │ JNI (Atomic Command Queues)
┌──────────────────────────────▼───────────────────────────────┐
│                 Game Main Thread (Thread-1)                  │
│   • Runs GameTask::ProcessPhase() at fixed 100 Hz (10ms)     │
│   • Updates Match, Teams, Referee, Physics, Ball, ODE        │
│   • TrainingManager::Process()                               │
│   • Mutates Animation State & buf_animApplyBuffer            │
└──────────────────────────────┬───────────────────────────────┘
                               │ PreparePutBuffers() [putBufferMutex]
┌──────────────────────────────▼───────────────────────────────┐
│             Skeletal Worker Thread Pool (Thread-6)           │
│   • Runs GameTask::PutPhase()                                │
│   • Calls HumanoidBase::FetchPutBuffers() [putBufferMutex]   │
│   • Interpolates bone matrices for current render time       │
│   • Deforms player vertices for geometry buffers             │
└──────────────────────────────┬───────────────────────────────┘
                               │ Graphics Command Queue
┌──────────────────────────────▼───────────────────────────────┐
│                    OpenGL Render Thread                      │
│   • Executes draw calls, G-Buffer, deferred lighting, post   │
│   • Presents backbuffer (eglSwapBuffers)                     │
└──────────────────────────────────────────────────────────────┘
```

---

## 2. Thread Synchronization Mechanics

### 2.1 The Animation Buffer Race Condition & `putBufferMutex`

#### The Problem
`HumanoidBase` represents the skeletal and geometric foundation for all 22 outfield players and goalkeepers. Each humanoid maintains an internal `AnimApplyBuffer`:

```cpp
struct AnimApplyBuffer {
    std::map<std::string, BiasedOffset> nodeOffsets;
    // ...
};
```

`std::map` is a red-black tree containing dynamically allocated nodes and pointers. During fast gameplay or drill resets:
- **Game Thread (`Thread-1`)** calls `PreparePutBuffers()` to copy calculated bone offsets into `buf_animApplyBuffer`:
  ```cpp
  buf_animApplyBuffer = animApplyBuffer;
  ```
- Simultaneously, **Worker Thread (`Thread-6`)** calls `FetchPutBuffers()` to read `buf_animApplyBuffer` into its private render buffer:
  ```cpp
  fetchedbuf_animApplyBuffer = buf_animApplyBuffer;
  ```

Because `std::map::operator=` iterates and rebalances tree pointers, concurrent read and write operations corrupted the internal tree structure, causing a segmentation fault (`SIGSEGV` in `std::__ndk1::__tree<...>::__assign_multi`).

#### The Solution: Mutex-Guarded Buffer Exchanges
Each `HumanoidBase` instance owns a dedicated mutex:
```cpp
mutable boost::mutex putBufferMutex;
```

Access is strictly synchronized using scoped RAII locks:
1. **Write Phase (Game Thread)**:
   ```cpp
   void HumanoidBase::PreparePutBuffers(unsigned long snapshotTime_ms) {
       boost::mutex::scoped_lock lock(putBufferMutex);
       buf_animApplyBuffer = animApplyBuffer;
   }
   ```
2. **Read Phase (Worker Thread)**:
   ```cpp
   void HumanoidBase::FetchPutBuffers(unsigned long putTime_ms) {
       boost::mutex::scoped_lock lock(putBufferMutex);
       fetchedbuf_animApplyBuffer = buf_animApplyBuffer;
   }
   ```
3. **Reset Phase (Drill Transitions)**:
   ```cpp
   void HumanoidBase::ResetSituation(const Vector3 &focusPos) {
       boost::mutex::scoped_lock lock(putBufferMutex);
       buf_animApplyBuffer.nodeOffsets.clear();
       fetchedbuf_animApplyBuffer.nodeOffsets.clear();
       // ...
   }
   ```

#### Safe Copy Assignment Operator
In addition to the mutex, `AnimApplyBuffer` defines an explicit copy assignment operator:
```cpp
AnimApplyBuffer& operator=(const AnimApplyBuffer &src) {
    if (this != &src) {
        nodeOffsets = src.nodeOffsets;
    }
    return *this;
}
```

---

## 3. Asynchronous JNI Command Decoupling

Android UI interactions (such as tapping pause menu cards or drill pickers) execute on Android's Java UI thread. If JNI called mutating methods on `Match` directly, it would race against `Match::Process()` on the game thread.

To eliminate this class of race conditions, `TrainingManager` implements lock-free atomic command buffers:

```cpp
class TrainingManager {
private:
    std::atomic<int> pendingSetDrill{-1};
    std::atomic<bool> pendingResetRequest{false};
    std::atomic<bool> pendingNextDrill{false};
    std::atomic<bool> pendingPrevDrill{false};
    std::atomic<bool> pendingNextVariation{false};
    // ...
};
```

### Command Lifecycle:
1. **Java Touch Event**: `MobileControlsView.java` triggers `nativeTrainingSetDrill(drillId)`.
2. **JNI Layer**: `Java_..._nativeTrainingSetDrill` calls `trainingManager->RequestSetDrill(drillId)`:
   ```cpp
   void TrainingManager::RequestSetDrill(e_TrainingDrill drill) {
       pendingSetDrill.store((int)drill, std::memory_order_release);
   }
   ```
3. **Game Thread Execution**: At the beginning of `Match::Process()`:
   ```cpp
   void TrainingManager::Process() {
       int req = pendingSetDrill.exchange(-1, std::memory_order_acq_rel);
       if (req >= 0) {
           SetDrill((e_TrainingDrill)req);
       }
       // ...
   }
   ```
This guarantees that all scenario resets, player relocations, and drill state alterations occur strictly within the deterministic simulation step.
