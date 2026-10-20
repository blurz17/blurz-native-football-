import bpy
import os

print("=== BUILDING DIRECT 13-BONE ENGINE RIG & PERFECT 34-BONE MAPPING ===")

base_dir = r"E:\MY-PROJECTS\android native football\tools\workbench"
src_34b = os.path.join(base_dir, "universal_humanoid_base_34b.glb")

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=src_34b)

arm_obj = [o for o in bpy.data.objects if o.type == 'ARMATURE'][0]

refine_map = {
    "Root": "Root_Hips",
    "LeftEye": "LeftEye_Head",
    "RightEye": "RightEye_Head",
    "LeftLeg": "LeftLowerLeg",
    "RightLeg": "RightLowerLeg",
    "LeftForeArm": "LeftLowerArm",
    "RightForeArm": "RightLowerArm"
}

for old_n, new_n in refine_map.items():
    b = arm_obj.data.bones.get(old_n)
    if b:
        b.name = new_n

for obj in bpy.data.objects:
    if obj.type == 'MESH':
        for old_n, new_n in refine_map.items():
            vg = obj.vertex_groups.get(old_n)
            if vg:
                vg.name = new_n

out_34b_perfect = src_34b
bpy.ops.export_scene.gltf(filepath=out_34b_perfect, export_format='GLB', use_selection=False, export_apply=False, export_skins=True)
print(f"Updated 34-bone base with verified engine keywords: {out_34b_perfect}")

# Generate DIRECT 13-BONE engine model (universal_player_engine_13b.glb)
engine_13_bones = [
    "body",
    "middle",
    "neck",
    "left_shoulder",
    "left_elbow",
    "right_shoulder",
    "right_elbow",
    "left_thigh",
    "left_knee",
    "left_ankle",
    "right_thigh",
    "right_knee",
    "right_ankle"
]

collapse_to_13_map = {
    "Root_Hips": "body",
    "Hips": "body",
    "Spine": "middle",
    "Spine1": "middle",
    "Spine2_Chest": "middle",
    "Neck": "neck",
    "Neck1": "neck",
    "Neck2": "neck",
    "Head": "neck",
    "LeftEye_Head": "neck",
    "RightEye_Head": "neck",
    "HeadTop_End": "neck",
    "LeftShoulder": "left_shoulder",
    "LeftArm": "left_shoulder",
    "LeftLowerArm": "left_elbow",
    "LeftForeArm_Twist1": "left_elbow",
    "LeftForeArm_Twist2": "left_elbow",
    "LeftHand": "left_elbow",
    "RightShoulder": "right_shoulder",
    "RightArm": "right_shoulder",
    "RightLowerArm": "right_elbow",
    "RightForeArm_Twist1": "right_elbow",
    "RightForeArm_Twist2": "right_elbow",
    "RightHand": "right_elbow",
    "LeftUpLeg": "left_thigh",
    "LeftLowerLeg": "left_knee",
    "LeftFoot": "left_ankle",
    "LeftToeBase": "left_ankle",
    "LeftToe_End": "left_ankle",
    "RightUpLeg": "right_thigh",
    "RightLowerLeg": "right_knee",
    "RightFoot": "right_ankle",
    "RightToeBase": "right_ankle",
    "RightToe_End": "right_ankle"
}

# Transfer vertex weights across all meshes to the 13 engine bones
for obj in bpy.data.objects:
    if obj.type != 'MESH':
        continue
    
    for e_bone in engine_13_bones:
        if not obj.vertex_groups.get(e_bone):
            obj.vertex_groups.new(name=e_bone)
            
    for v in obj.data.vertices:
        weights_13 = {b: 0.0 for b in engine_13_bones}
        for old_b, target_13 in collapse_to_13_map.items():
            vg = obj.vertex_groups.get(old_b)
            if vg:
                try:
                    weights_13[target_13] += vg.weight(v.index)
                except RuntimeError:
                    pass
                    
        for e_bone, w in weights_13.items():
            if w > 0.0:
                obj.vertex_groups[e_bone].add([v.index], w, 'REPLACE')
                
    for old_b in collapse_to_13_map.keys():
        vg = obj.vertex_groups.get(old_b)
        if vg and vg.name not in engine_13_bones:
            obj.vertex_groups.remove(vg)

# Edit mode: rebuild armature with exactly 13 bones
bpy.context.view_layer.objects.active = arm_obj
bpy.ops.object.mode_set(mode='EDIT')
edit_bones = arm_obj.data.edit_bones

rename_core = {
    "Hips": "body",
    "Spine": "middle",
    "Head": "neck",
    "LeftArm": "left_shoulder",
    "LeftLowerArm": "left_elbow",
    "RightArm": "right_shoulder",
    "RightLowerArm": "right_elbow",
    "LeftUpLeg": "left_thigh",
    "LeftLowerLeg": "left_knee",
    "LeftFoot": "left_ankle",
    "RightUpLeg": "right_thigh",
    "RightLowerLeg": "right_knee",
    "RightFoot": "right_ankle"
}

keep_names = list(rename_core.keys())
del_bones = [b.name for b in edit_bones if b.name not in keep_names]
for b_name in del_bones:
    b = edit_bones.get(b_name)
    if b:
        edit_bones.remove(b)

for old_n, new_n in rename_core.items():
    b = edit_bones.get(old_n)
    if b:
        b.name = new_n

b_body = edit_bones.get("body")
b_middle = edit_bones.get("middle")
b_neck = edit_bones.get("neck")
b_l_sh = edit_bones.get("left_shoulder")
b_l_el = edit_bones.get("left_elbow")
b_r_sh = edit_bones.get("right_shoulder")
b_r_el = edit_bones.get("right_elbow")
b_l_th = edit_bones.get("left_thigh")
b_l_kn = edit_bones.get("left_knee")
b_l_an = edit_bones.get("left_ankle")
b_r_th = edit_bones.get("right_thigh")
b_r_kn = edit_bones.get("right_knee")
b_r_an = edit_bones.get("right_ankle")

b_body.parent = None
b_middle.parent = b_body
b_neck.parent = b_middle
b_l_sh.parent = b_middle
b_l_el.parent = b_l_sh
b_r_sh.parent = b_middle
b_r_el.parent = b_r_sh

b_l_th.parent = b_body
b_l_kn.parent = b_l_th
b_l_an.parent = b_l_kn

b_r_th.parent = b_body
b_r_kn.parent = b_r_th
b_r_an.parent = b_r_kn

bpy.ops.object.mode_set(mode='OBJECT')

print(f"\nFinal Engine Armature Bones ({len(arm_obj.data.bones)} bones):")
for i, b in enumerate(arm_obj.data.bones):
    p = b.parent.name if b.parent else "ROOT"
    print(f"  {i:02d}: {b.name:15} (parent: {p})")

out_13b = os.path.join(base_dir, "universal_player_engine_13b.glb")
bpy.ops.export_scene.gltf(filepath=out_13b, export_format='GLB', use_selection=False, export_apply=False, export_skins=True)
print(f"Successfully exported NATIVE 13-BONE ENGINE MODEL to: {out_13b} ({os.path.getsize(out_13b):,} bytes)")
