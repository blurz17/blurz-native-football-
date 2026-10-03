// touch_input_tests.cpp
// V&V file to validate the Touch Controller mapping and analog values.

#include <iostream>
#include <cassert>
#include <cmath>

// Mock Vector3
struct Vector3 {
    float coords[3];
    Vector3(float x, float y, float z) { coords[0] = x; coords[1] = y; coords[2] = z; }
    Vector3(float v) { coords[0] = v; coords[1] = v; coords[2] = v; }
    void Normalize(float unused) {
        float len = sqrt(coords[0]*coords[0] + coords[1]*coords[1] + coords[2]*coords[2]);
        if (len > 0) {
            coords[0] /= len; coords[1] /= len; coords[2] /= len;
        }
    }
    float GetLength() { return sqrt(coords[0]*coords[0] + coords[1]*coords[1] + coords[2]*coords[2]); }
};

// Mock C++ Touch Controller that we WILL build
class TouchController {
public:
    float axis_x = 0.0f;
    float axis_y = 0.0f;
    bool button_pass = false;

    // Simulates the JNI call from Java
    void JNI_setVirtualJoystickAxis(int axis, float value) {
        if (axis == 0) axis_x = value;
        if (axis == 1) axis_y = value;
    }

    // Simulates the Engine reading the input
    Vector3 GetDirection() {
        Vector3 inputDirection(0.0f);
        inputDirection.coords[0] = axis_x;
        inputDirection.coords[1] = -axis_y; // Invert Y axis for screen space to world space if needed

        float deadzone = 0.15f;
        if (inputDirection.GetLength() < deadzone) {
            return Vector3(0.0f);
        }
        
        // Analog scale is preserved, unlike the keyboard which normalizes to 1.0.
        // If length > 1.0, clamp it.
        if (inputDirection.GetLength() > 1.0f) {
            inputDirection.Normalize(0);
        }

        return inputDirection;
    }
};

void run_tests() {
    TouchController touch;

    // Test 1: Deadzone
    touch.JNI_setVirtualJoystickAxis(0, 0.1f);
    touch.JNI_setVirtualJoystickAxis(1, 0.1f);
    Vector3 dir1 = touch.GetDirection();
    assert(dir1.GetLength() == 0.0f && "Deadzone test failed!");
    std::cout << "[PASS] Deadzone test\n";

    // Test 2: Full Forward
    touch.JNI_setVirtualJoystickAxis(0, 0.0f);
    touch.JNI_setVirtualJoystickAxis(1, -1.0f); // Up on joystick
    Vector3 dir2 = touch.GetDirection();
    assert(dir2.coords[1] > 0.99f && "Full forward test failed!");
    std::cout << "[PASS] Full forward test\n";

    // Test 3: Analog Diagonal
    touch.JNI_setVirtualJoystickAxis(0, 0.5f);
    touch.JNI_setVirtualJoystickAxis(1, -0.5f);
    Vector3 dir3 = touch.GetDirection();
    assert(std::abs(dir3.coords[0] - 0.5f) < 0.01f && "Analog diagonal X failed!");
    assert(std::abs(dir3.coords[1] - 0.5f) < 0.01f && "Analog diagonal Y failed!");
    std::cout << "[PASS] Analog diagonal test\n";

    std::cout << "All Touch Input V&V tests passed!\n";
}

int main() {
    run_tests();
    return 0;
}
