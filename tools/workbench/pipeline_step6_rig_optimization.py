import bpy
import os

print("=== STEP 6: SKELETON & RIG OPTIMIZATION (74 -> 34 BONES) ===")
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=r"tools/workbench/yamal_step1_cleaned.glb")

# Ensure Icosphere or any non-character objects are completely removed
for o in list(bpy.data.objects):
    if "Icosphere" in o.name:
        bpy.data.objects.remove(o, do_unlink=True)

for m in list(bpy.data.meshes):
    if "Icosphere" in m.name:
        bpy.data.meshes.remove(m)

# Clean up redundant UV layers
for o in bpy.data.objects:
    if o.type == 'MESH':
        if len(o.data.uv_layers) > 1:
            for uv_layer in list(o.data.uv_layers)[1:]:
                print(f"Removing redundant UV layer '{uv_layer.name}' on {o.name}")
                o.data.uv_layers.remove(uv_layer)

arm_obj = [o for o in bpy.data.objects if o.type == 'ARMATURE'][0]
print(f"Initial armature: {arm_obj.name} with {len(arm_obj.data.bones)} bones")

# 1. Transfer finger vertex weights to Hand bones across all meshes
finger_keywords = ['Thumb', 'Index', 'Middle', 'Ring', 'Pinky']

for obj in bpy.data.objects:
    if obj.type != 'MESH':
        continue
    
    # Check if this mesh has finger vertex groups
    left_hand_vg = obj.vertex_groups.get("LeftHand_017")
    right_hand_vg = obj.vertex_groups.get("RightHand_043")
    
    left_fingers = [vg for vg in obj.vertex_groups if "LeftHand" in vg.name and any(k in vg.name for k in finger_keywords)]
    right_fingers = [vg for vg in obj.vertex_groups if "RightHand" in vg.name and any(k in vg.name for k in finger_keywords)]
    
    if left_fingers or right_fingers:
        print(f"Processing mesh '{obj.name}': collapsing {len(left_fingers)} left and {len(right_fingers)} right finger groups")
        
        # Ensure hand vertex groups exist
        if not left_hand_vg:
            left_hand_vg = obj.vertex_groups.new(name="LeftHand_017")
        if not right_hand_vg:
            right_hand_vg = obj.vertex_groups.new(name="RightHand_043")
            
        for v in obj.data.vertices:
            # Left Hand transfer
            w_left_fingers = 0.0
            for vg in left_fingers:
                try:
                    w_left_fingers += vg.weight(v.index)
                except RuntimeError:
                    pass
            if w_left_fingers > 0.0:
                current_w = 0.0
                try:
                    current_w = left_hand_vg.weight(v.index)
                except RuntimeError:
                    pass
                left_hand_vg.add([v.index], current_w + w_left_fingers, 'REPLACE')
                
            # Right Hand transfer
            w_right_fingers = 0.0
            for vg in right_fingers:
                try:
                    w_right_fingers += vg.weight(v.index)
                except RuntimeError:
                    pass
            if w_right_fingers > 0.0:
                current_w = 0.0
                try:
                    current_w = right_hand_vg.weight(v.index)
                except RuntimeError:
                    pass
                right_hand_vg.add([v.index], current_w + w_right_fingers, 'REPLACE')
                
        # Remove finger groups from mesh
        for vg in left_fingers + right_fingers:
            obj.vertex_groups.remove(vg)

# 2. Prune finger bones from armature in edit mode
bpy.context.view_layer.objects.active = arm_obj
bpy.ops.object.mode_set(mode='EDIT')

edit_bones = arm_obj.data.edit_bones
bones_to_delete = [b.name for b in edit_bones if any(k in b.name for k in finger_keywords)]
print(f"Deleting {len(bones_to_delete)} finger bones from armature...")
for b_name in bones_to_delete:
    b = edit_bones.get(b_name)
    if b:
        edit_bones.remove(b)

bpy.ops.object.mode_set(mode='OBJECT')
print(f"Remaining bones in armature: {len(arm_obj.data.bones)}")

# 3. Standardize bone names to standard humanoid hierarchy
bone_rename_map = {
    "_rootJoint": "Root",
    "Hips_01": "Hips",
    "Spine_02": "Spine",
    "Spine1_03": "Spine1",
    "Spine2_04": "Spine2_Chest",
    "Neck_05": "Neck",
    "Neck1_06": "Neck1",
    "Neck2_07": "Neck2",
    "Head_08": "Head",
    "LeftEye_09": "LeftEye",
    "RightEye_010": "RightEye",
    "HeadTop_End_011": "HeadTop_End",
    "LeftShoulder_012": "LeftShoulder",
    "LeftArm_013": "LeftArm",
    "LeftForeArm_014": "LeftForeArm",
    "LeftForeArm1_015": "LeftForeArm_Twist1",
    "LeftForeArm2_016": "LeftForeArm_Twist2",
    "LeftHand_017": "LeftHand",
    "RightShoulder_038": "RightShoulder",
    "RightArm_039": "RightArm",
    "RightForeArm_040": "RightForeArm",
    "RightForeArm1_041": "RightForeArm_Twist1",
    "RightForeArm2_042": "RightForeArm_Twist2",
    "RightHand_043": "RightHand",
    "LeftUpLeg_063": "LeftUpLeg",
    "LeftLeg_064": "LeftLeg",
    "LeftFoot_065": "LeftFoot",
    "LeftToeBase_066": "LeftToeBase",
    "LeftToe_End_067": "LeftToe_End",
    "RightUpLeg_068": "RightUpLeg",
    "RightLeg_069": "RightLeg",
    "RightFoot_070": "RightFoot",
    "RightToeBase_071": "RightToeBase",
    "RightToe_End_072": "RightToe_End"
}

# Rename armature bones
for old_b, new_b in bone_rename_map.items():
    b = arm_obj.data.bones.get(old_b)
    if b:
        b.name = new_b

# Rename corresponding vertex groups across meshes to maintain skinning link
for obj in bpy.data.objects:
    if obj.type == 'MESH':
        for old_b, new_b in bone_rename_map.items():
            vg = obj.vertex_groups.get(old_b)
            if vg:
                vg.name = new_b

print("\n--- Final Standardized Humanoid Bones ---")
for i, b in enumerate(arm_obj.data.bones):
    p = b.parent.name if b.parent else 'None'
    print(f"  {i:02d}: {b.name:22} (parent: {p})")

# Export standardized base model
out_path = r"tools/workbench/universal_humanoid_base_34b.glb"
bpy.ops.export_scene.gltf(
    filepath=out_path,
    export_format='GLB',
    use_selection=False,
    export_apply=False,
    export_skins=True,
    export_morph=False
)
print(f"\nSuccessfully exported 34-bone Standardized Universal Base to: {out_path} ({os.path.getsize(out_path):,} bytes)")
