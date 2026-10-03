import math

class Vector3:
    def __init__(self, x=0.0, y=0.0, z=0.0):
        self.coords = [x, y, z]

    def length(self):
        return math.sqrt(self.coords[0]**2 + self.coords[1]**2 + self.coords[2]**2)

    def normalize(self):
        length = self.length()
        if length > 0:
            self.coords[0] /= length
            self.coords[1] /= length
            self.coords[2] /= length

class TouchController:
    def __init__(self):
        self.axis_x = 0.0
        self.axis_y = 0.0
    
    def set_virtual_joystick_axis(self, axis, value):
        if axis == 0:
            self.axis_x = value
        elif axis == 1:
            self.axis_y = value

    def get_direction(self):
        direction = Vector3(self.axis_x, -self.axis_y, 0.0)
        deadzone = 0.15
        
        if direction.length() < deadzone:
            return Vector3(0.0, 0.0, 0.0)
        
        if direction.length() > 1.0:
            direction.normalize()
            
        return direction

def run_tests():
    touch = TouchController()
    
    # Test 1: Deadzone
    touch.set_virtual_joystick_axis(0, 0.1)
    touch.set_virtual_joystick_axis(1, 0.1)
    d1 = touch.get_direction()
    assert d1.length() == 0.0, "Deadzone test failed!"
    print("[PASS] Deadzone test")
    
    # Test 2: Full Forward
    touch.set_virtual_joystick_axis(0, 0.0)
    touch.set_virtual_joystick_axis(1, -1.0)
    d2 = touch.get_direction()
    assert d2.coords[1] > 0.99, "Full forward test failed!"
    print("[PASS] Full forward test")
    
    # Test 3: Analog Diagonal
    touch.set_virtual_joystick_axis(0, 0.5)
    touch.set_virtual_joystick_axis(1, -0.5)
    d3 = touch.get_direction()
    assert abs(d3.coords[0] - 0.5) < 0.01, "Analog diagonal X failed!"
    assert abs(d3.coords[1] - 0.5) < 0.01, "Analog diagonal Y failed!"
    print("[PASS] Analog diagonal test")
    
    print("All Touch Input V&V tests passed!")

if __name__ == "__main__":
    run_tests()
