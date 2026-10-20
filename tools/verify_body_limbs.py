import trimesh
import numpy as np

scene = trimesh.load(r'tools/workbench/yamal_original_copy.glb')
body = scene.geometry['AvatarBody_AvatarBody_0']
v = body.vertices

arms_left = v[v[:, 0] > 0.25]
arms_right = v[v[:, 0] < -0.25]
legs = v[v[:, 1] < 0.60]

print(f"Total AvatarBody vertices: {len(v)}")
print(f"Left Arm/Hand vertices: {len(arms_left)}")
print(f"Right Arm/Hand vertices: {len(arms_right)}")
print(f"Legs/Knees vertices: {len(legs)}")
print(f"Sum of limbs: {len(arms_left) + len(arms_right) + len(legs)}")
