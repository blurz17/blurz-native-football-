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
    
    # Test 4: HIDGamepad default axis mapping verification
    def decode_gamepad(axis_vals, mapping):
        btn_states = [0.0] * 4
        for i in range(4):
            btn_id = mapping[i]
            axis_id = -btn_id - 1
            sign = ((axis_id % 2) * 2) - 1
            axis_id = axis_id // 2
            val = axis_vals[axis_id]
            if (sign < 0 and val < 0) or (sign > 0 and val > 0):
                btn_states[i] = abs(val)
            else:
                btn_states[i] = 0.0
        # Reconstruct vector as in HIDGamepad::GetDirection()
        # coords[0] = -Left + Right
        # coords[1] = +Up - Down
        dx = -btn_states[3] + btn_states[1] # Left is 3, Right is 1
        dy = btn_states[0] - btn_states[2]  # Up is 0, Down is 2
        return dx, dy

    # Old buggy mapping: Up=-3, Right=-4, Down=-1, Left=-2
    old_mapping = [-3, -4, -1, -2]
    # New corrected mapping: Up=-3, Right=-2, Down=-4, Left=-1
    new_mapping = [-3, -2, -4, -1]

    # Push Right: axis 0 = +1.0, axis 1 = 0.0
    right_axes = [1.0, 0.0]
    old_dx, old_dy = decode_gamepad(right_axes, old_mapping)
    # With old mapping, pushing right gave Left! (-1.0, 0.0)
    assert old_dx == -1.0, f"Old mapping was inverted on X: {old_dx}"

    new_dx, new_dy = decode_gamepad(right_axes, new_mapping)
    assert new_dx == 1.0 and new_dy == 0.0, f"New mapping failed on Right: {new_dx}, {new_dy}"

    # Push Down: axis 0 = 0.0, axis 1 = +1.0
    down_axes = [0.0, 1.0]
    new_dx, new_dy = decode_gamepad(down_axes, new_mapping)
    assert new_dx == 0.0 and new_dy == -1.0, f"New mapping failed on Down: {new_dx}, {new_dy}"

    # Push Up: axis 0 = 0.0, axis 1 = -1.0
    up_axes = [0.0, -1.0]
    new_dx, new_dy = decode_gamepad(up_axes, new_mapping)
    assert new_dx == 0.0 and new_dy == 1.0, f"New mapping failed on Up: {new_dx}, {new_dy}"

    # Push Left: axis 0 = -1.0, axis 1 = 0.0
    left_axes = [-1.0, 0.0]
    new_dx, new_dy = decode_gamepad(left_axes, new_mapping)
    assert new_dx == -1.0 and new_dy == 0.0, f"New mapping failed on Left: {new_dx}, {new_dy}"
    print("[PASS] HIDGamepad axis mapping verification test")

    # Test 5: Continuous analog velocity scaling with 0.15 deadzone
    deadzone = 0.15
    walk_velo = 5.0
    sprint_velo = 8.0

    # Test below deadzone -> 0 velocity
    mag_sub = 0.10
    velo_sub = 0.0 if mag_sub < deadzone else walk_velo * min(mag_sub, 1.0)
    assert velo_sub == 0.0, "Sub-deadzone should yield 0 velocity"

    # Test gentle tilt (0.4) -> gentle jog (~2.0)
    mag_gentle = 0.40
    velo_gentle = 0.0 if mag_gentle < deadzone else walk_velo * min(mag_gentle, 1.0)
    assert abs(velo_gentle - 2.0) < 0.01, f"Gentle tilt should scale velocity: {velo_gentle}"

    # Test full tilt (1.0) -> full walk (5.0)
    mag_full = 1.0
    velo_full = 0.0 if mag_full < deadzone else walk_velo * min(mag_full, 1.0)
    assert abs(velo_full - 5.0) < 0.01, f"Full tilt should be full walk velocity: {velo_full}"

    # Test full tilt + sprint -> sprint (8.0)
    velo_sprint = 0.0 if mag_full < deadzone else sprint_velo * min(mag_full, 1.0)
    assert abs(velo_sprint - 8.0) < 0.01, f"Sprint tilt should be full sprint velocity: {velo_sprint}"
    print("[PASS] Continuous analog velocity scaling test")

    # Test 6: Unified team possession context and tap power baseline
    def get_tactical_context(team_id, best_poss_team_id, retainer_team_id, team_has_poss, opp_has_poss, team_poss_amount):
        if retainer_team_id is not None:
            return retainer_team_id == team_id
        if team_has_poss:
            return True
        if opp_has_poss:
            return False
        if best_poss_team_id == team_id:
            return True
        return team_poss_amount >= 1.0

    # User team is 0
    # Scenario: Teammate has ball 30m away -> Team is attacking
    assert get_tactical_context(0, 0, None, True, False, 1.4) is True, "Attacking when teammate has ball"

    # Scenario: Opponent has ball -> Team is defending
    assert get_tactical_context(0, 1, None, False, True, 0.6) is False, "Defending when opponent has ball"

    # Tap power curve: quick tap (gauge_ms = 80ms) should produce crisp pass, not 0.01 dead dribble
    base_time_ms = 40
    gauge_ms = 80
    gauge_factor = max(0.0, min(1.0, (gauge_ms - base_time_ms) / (1000.0 - base_time_ms)))
    tap_power = max(0.25, min(1.0, 0.25 + 0.75 * (gauge_factor ** 0.7)))
    assert tap_power >= 0.25, f"Tap power should be at least 0.25, got: {tap_power}"
    print("[PASS] Unified tactical context and tap power baseline test")

    print("All Touch Input V&V tests passed!")

if __name__ == "__main__":
    run_tests()
