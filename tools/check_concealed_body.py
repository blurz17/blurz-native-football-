import trimesh
import numpy as np

scene = trimesh.load(r'tools/workbench/yamal_original_copy.glb')
body = scene.geometry['AvatarBody_AvatarBody_0']
top = scene.geometry['outfit_top_outfit_top_0']
bottom = scene.geometry['outfit_bottom_outfit_bottom_0']

print("=== CHECKING BODY CONCEALED GEOMETRY ===")
print("Body bounds Y:", body.bounds[:, 1])
print("Top bounds Y:", top.bounds[:, 1])
print("Bottom bounds Y:", bottom.bounds[:, 1])

# In AvatarBody, check vertices in torso region (Y between 0.88 and 1.35)
body_verts = body.vertices
torso_verts = body_verts[(body_verts[:, 1] >= 0.88) & (body_verts[:, 1] <= 1.35) & (np.abs(body_verts[:, 0]) < 0.25)]
pelvis_verts = body_verts[(body_verts[:, 1] >= 0.60) & (body_verts[:, 1] < 0.88) & (np.abs(body_verts[:, 0]) < 0.18)]

print(f"Torso body vertices under jersey: {len(torso_verts)}")
print(f"Pelvis body vertices under shorts: {len(pelvis_verts)}")
print(f"Total concealed body vertices: {len(torso_verts) + len(pelvis_verts)} out of {len(body_verts)} ({((len(torso_verts) + len(pelvis_verts))/len(body_verts)*100):.1f}%)")
