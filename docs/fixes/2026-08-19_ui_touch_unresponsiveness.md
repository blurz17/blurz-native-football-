# Android System UI Stealing Touches & Input Responsiveness Fix

**Date:** 2026-08-19

## The Problem
The user reported that the game had a "darkness" (black letterboxing) on the left and right edges, and that the top notification bar and side navigation bar were persistently visible on modern phones. 
Additionally, the Pause button was occasionally ignoring taps and required "spam tapping" to register.

## The Cause
1. **Disabled Immersive Mode:** The original developer explicitly disabled the standard `setSystemUiVisibility` call in `SDLActivity.java` due to a crash on older Samsung devices running Android 14. This left the game running in a non-immersive mode, which caused the letterboxing and kept the system UI bars visible.
2. **System UI Stealing Touches:** Because the game was letterboxed and the navigation bar was present on the right, the Pause button (located in the top-right corner) was sitting directly underneath the system's invisible gesture zone. Android was intercepting these touches to check for "back" gestures.
3. **Rapid Tap Polling Bug:** The Pause button logic in `MobileControlsView.java` was sending a `KEYUP` event immediately after a `KEYDOWN` event when the user released their finger. If a quick tap occurred between two engine frames, the event pump processed both the down and up states in the same frame, causing the engine's `GetKeyboardState()` polling to completely miss the input.

## The Fix
1. **Modern Immersive Mode:** Added modern `WindowInsetsController` logic to `SDLActivity.java` for Android 11+ to safely hide the status and navigation bars without triggering the Samsung Android 14 crash. 
2. **Display Cutout Support:** Added `layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` to `onCreate` to allow the game to render fully into the notch area, removing the black borders.
3. **Enlarged Pause Button:** Increased the Pause button radius in `MobileControlsView.java` by roughly 37.5% (from `btnRad * 0.8f` to `btnRad * 1.1f`) to give it a larger hit area.
4. **Delayed Virtual Key Release:** Added a 50ms `postDelayed` runnable in `MobileControlsView.java` when sending a virtual `KEYUP` event to `SDLActivity.setVirtualKey()`. This guarantees that the C++ engine has at least a few frames to poll and detect the rapid tap before the virtual key state is reset to false.
