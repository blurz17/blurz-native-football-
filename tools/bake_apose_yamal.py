import bpy
import os
import shutil
import math
from mathutils import Vector

def build_apose_yamal():
    print("=== STARTING YAMAL A-POSE BAKE PIPELINE ===")
    bpy.ops.wm.read_factory_settings(use_empty=True)

    # 1. Import raw Yamal model
    yamal_glb = r"E:\Downlaod\football asset\players\lamine_yamal_model.glb"
    bpy.ops.import_scene.gltf(filepath=yamal_glb)
    print("Imported raw Yamal model")

    arm = bpy.data.objects['Object_4']
    bpy.context.view_layer.objects.active = arm
    bpy.ops.object.mode_set(mode='POSE')

    # Reset all pose rotations to identity
    for pb in arm.pose.bones:
        pb.rotation_mode = 'XYZ'
        pb.rotation_euler = (0, 0, 0)

    # 2. Lower arms from 90-deg horizontal T-pose into natural 68-deg A-pose
    arm.pose.bones['LeftArm_013'].rotation_euler = (math.radians(68), math.radians(-10), math.radians(-4))
    arm.pose.bones['LeftForeArm_014'].rotation_euler = (math.radians(12), 0, 0)
    arm.pose.bones['RightArm_039'].rotation_euler = (math.radians(68), math.radians(10), math.radians(4))
    arm.pose.bones['RightForeArm_040'].rotation_euler = (math.radians(12), 0, 0)

    bpy.context.view_layer.update()
    bpy.ops.object.mode_set(mode='OBJECT')
    print("Posed arms to natural A-pose")

    # Record world-space bone locations for the new 13-bone armature
    mat_world = arm.matrix_world
    b_heads_world = {
        'body': mat_world @ arm.pose.bones['Hips_01'].head,
        'middle': mat_world @ arm.pose.bones['Spine1_03'].head,
        'neck': mat_world @ arm.pose.bones['Neck_05'].head,
        'left_shoulder': mat_world @ arm.pose.bones['LeftShoulder_012'].head,
        'left_elbow': mat_world @ arm.pose.bones['LeftForeArm_014'].head,
        'right_shoulder': mat_world @ arm.pose.bones['RightShoulder_038'].head,
        'right_elbow': mat_world @ arm.pose.bones['RightForeArm_040'].head,
        'left_thigh': mat_world @ arm.pose.bones['LeftUpLeg_063'].head,
        'left_knee': mat_world @ arm.pose.bones['LeftLeg_064'].head,
        'left_ankle': mat_world @ arm.pose.bones['LeftFoot_065'].head,
        'right_thigh': mat_world @ arm.pose.bones['RightUpLeg_068'].head,
        'right_knee': mat_world @ arm.pose.bones['RightLeg_069'].head,
        'right_ankle': mat_world @ arm.pose.bones['RightFoot_070'].head,
    }

    # Bone mapping dictionary from Yamal 74 bones to Engine 13 bones
    bone_map = {
        'Hips_01': 'body', '_rootJoint': 'body',
        'Spine_02': 'middle', 'Spine1_03': 'middle', 'Spine2_04': 'middle',
        'Neck_05': 'neck', 'Neck1_06': 'neck', 'Neck2_07': 'neck', 'Head_08': 'neck',
        'LeftEye_09': 'neck', 'RightEye_010': 'neck', 'HeadTop_End_011': 'neck',
        'LeftShoulder_012': 'left_shoulder', 'LeftArm_013': 'left_shoulder',
        'LeftForeArm_014': 'left_elbow', 'LeftForeArm1_015': 'left_elbow', 'LeftForeArm2_016': 'left_elbow',
        'LeftHand_017': 'left_elbow',
        'RightShoulder_038': 'right_shoulder', 'RightArm_039': 'right_shoulder',
        'RightForeArm_040': 'right_elbow', 'RightForeArm1_041': 'right_elbow', 'RightForeArm2_042': 'right_elbow',
        'RightHand_043': 'right_elbow',
        'LeftUpLeg_063': 'left_thigh', 'LeftLeg_064': 'left_knee', 'LeftFoot_065': 'left_ankle',
        'LeftToeBase_066': 'left_ankle', 'LeftToe_End_067': 'left_ankle',
        'RightUpLeg_068': 'right_thigh', 'RightLeg_069': 'right_knee', 'RightFoot_070': 'right_ankle',
        'RightToeBase_071': 'right_ankle', 'RightToe_End_072': 'right_ankle',
    }
    for b in arm.data.bones.keys():
        if b not in bone_map:
            if 'Left' in b:
                bone_map[b] = 'left_elbow'
            elif 'Right' in b:
                bone_map[b] = 'right_elbow'
            else:
                bone_map[b] = 'middle'

    engine_bone_names = [
        'body', 'middle', 'neck',
        'left_shoulder', 'left_elbow',
        'right_shoulder', 'right_elbow',
        'left_thigh', 'left_knee', 'left_ankle',
        'right_thigh', 'right_knee', 'right_ankle'
    ]

    yamal_mesh_names = [
        'Object_6',   # AvatarBody
        'Object_8',   # AvatarEyelashes
        'Object_10',  # AvatarHead
        'Object_12',  # AvatarLeftEyeball
        'Object_14',  # AvatarRightEyeball
        'Object_16',  # AvatarTeethLower
        'Object_18',  # AvatarTeethUpper
        'Object_19',  # outfit_bottom
        'Object_21',  # outfit_shoes
        'Object_22',  # outfit_top
    ]

    mesh_objs = []
    for name in yamal_mesh_names:
        obj = bpy.data.objects.get(name)
        if obj and obj.type == 'MESH':
            mesh_objs.append(obj)

    print(f"Found {len(mesh_objs)} Yamal mesh components")

    # CRITICAL: Step 3A: Gather vertex weights from the active armature FIRST
    all_obj_weights = {}
    for obj in mesh_objs:
        v_weights = {eb: {} for eb in engine_bone_names}
        for vg in list(obj.vertex_groups):
            orig_name = vg.name
            target_eb = bone_map.get(orig_name, 'body')
            for v in obj.data.vertices:
                try:
                    w = vg.weight(v.index)
                    if w > 0.001:
                        v_weights[target_eb][v.index] = v_weights[target_eb].get(v.index, 0.0) + w
                except RuntimeError:
                    pass
        all_obj_weights[obj.name] = v_weights

    # Step 3B: Apply armature modifier on all meshes to bake A-pose geometry permanently!
    for obj in mesh_objs:
        bpy.context.view_layer.objects.active = obj
        for mod in list(obj.modifiers):
            if mod.type == 'ARMATURE':
                bpy.ops.object.modifier_apply(modifier=mod.name)

    print("Baked A-pose into vertex geometry of all meshes")

    # Step 3C: Now recreate the 13 engine vertex groups on the meshes
    for obj in mesh_objs:
        obj.vertex_groups.clear()
        v_weights = all_obj_weights[obj.name]
        for eb in engine_bone_names:
            new_vg = obj.vertex_groups.new(name=eb)
            weights_dict = v_weights[eb]
            for v_idx, w in weights_dict.items():
                new_vg.add([v_idx], w, 'REPLACE')

        # Normalize weights so sum is 1.0
        for v in obj.data.vertices:
            total_w = sum(v_weights[eb].get(v.index, 0.0) for eb in engine_bone_names)
            if total_w > 0.001:
                for eb in engine_bone_names:
                    if v.index in v_weights[eb]:
                        normalized_w = v_weights[eb][v.index] / total_w
                        obj.vertex_groups[eb].add([v.index], normalized_w, 'REPLACE')
            else:
                obj.vertex_groups['body'].add([v.index], 1.0, 'REPLACE')

    print("Remapped all vertex groups to the 13 canonical engine bones")

    # 4. Import & Fit 3D Hair from fullbody_backup.glb
    backup_glb = r"E:\MY-PROJECTS\android native football\assets\media\objects\players\models\fullbody_backup.glb"
    bpy.ops.import_scene.gltf(filepath=backup_glb)

    hair_obj = None
    backup_objects_to_delete = []
    for o in bpy.data.objects:
        if 'Hair' in o.name:
            hair_obj = o
        elif o.name not in [arm.name] + [m.name for m in mesh_objs]:
            backup_objects_to_delete.append(o)

    for o in backup_objects_to_delete:
        if o != hair_obj and o.name != 'Object_4':
            bpy.data.objects.remove(o, do_unlink=True)

    if hair_obj:
        print(f"Loaded hair object: {hair_obj.name} ({len(hair_obj.data.vertices)} verts)")
        hair_obj.location.z -= 0.052
        hair_obj.location.y -= 0.010
        hair_obj.scale = (0.98, 0.98, 0.98)
        bpy.context.view_layer.objects.active = hair_obj
        bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)

        hair_obj.vertex_groups.clear()
        for eb in engine_bone_names:
            hair_obj.vertex_groups.new(name=eb)
        hair_obj.vertex_groups['neck'].add(list(range(len(hair_obj.data.vertices))), 1.0, 'REPLACE')

    # Remove old Yamal armature Object_4
    bpy.data.objects.remove(arm, do_unlink=True)

    # 5. Join all player body meshes into one clean unified body mesh
    bpy.ops.object.select_all(action='DESELECT')
    for obj in mesh_objs:
        obj.select_set(True)
    bpy.context.view_layer.objects.active = mesh_objs[0]
    bpy.ops.object.join()
    body_obj = bpy.context.active_object
    body_obj.name = "player_body"

    print(f"Unified body mesh vertices: {len(body_obj.data.vertices)}")

    # 6. Create clean 13-bone Armature at world origin
    bpy.ops.object.armature_add(location=(0, 0, 0))
    new_arm = bpy.context.active_object
    new_arm.name = "PlayerArmature"

    bpy.ops.object.mode_set(mode='EDIT')
    for b in list(new_arm.data.edit_bones):
        new_arm.data.edit_bones.remove(b)

    tails = {
        'body': b_heads_world['middle'],
        'middle': b_heads_world['neck'],
        'neck': Vector((b_heads_world['neck'].x, b_heads_world['neck'].y, b_heads_world['neck'].z + 0.22)),
        'left_shoulder': b_heads_world['left_elbow'],
        'left_elbow': Vector((b_heads_world['left_elbow'].x + 0.05, b_heads_world['left_elbow'].y - 0.03, b_heads_world['left_elbow'].z - 0.22)),
        'right_shoulder': b_heads_world['right_elbow'],
        'right_elbow': Vector((b_heads_world['right_elbow'].x - 0.05, b_heads_world['right_elbow'].y - 0.03, b_heads_world['right_elbow'].z - 0.22)),
        'left_thigh': b_heads_world['left_knee'],
        'left_knee': b_heads_world['left_ankle'],
        'left_ankle': Vector((b_heads_world['left_ankle'].x, b_heads_world['left_ankle'].y - 0.14, b_heads_world['left_ankle'].z - 0.06)),
        'right_thigh': b_heads_world['right_knee'],
        'right_knee': b_heads_world['right_ankle'],
        'right_ankle': Vector((b_heads_world['right_ankle'].x, b_heads_world['right_ankle'].y - 0.14, b_heads_world['right_ankle'].z - 0.06)),
    }

    parents = {
        'middle': 'body',
        'neck': 'middle',
        'left_shoulder': 'middle',
        'left_elbow': 'left_shoulder',
        'right_shoulder': 'middle',
        'right_elbow': 'right_shoulder',
        'left_thigh': 'body',
        'left_knee': 'left_thigh',
        'left_ankle': 'left_knee',
        'right_thigh': 'body',
        'right_knee': 'right_thigh',
        'right_ankle': 'right_knee',
    }

    ebones = {}
    for eb in engine_bone_names:
        b = new_arm.data.edit_bones.new(eb)
        b.head = b_heads_world[eb]
        b.tail = tails[eb]
        ebones[eb] = b

    for child, parent in parents.items():
        ebones[child].parent = ebones[parent]

    bpy.ops.object.mode_set(mode='OBJECT')
    print("Created clean 13-bone PlayerArmature")

    # Parent body and hair to new armature
    export_objects = [body_obj]
    if hair_obj:
        export_objects.append(hair_obj)

    for obj in export_objects:
        obj.parent = new_arm
        mod = obj.modifiers.new(name="Armature", type='ARMATURE')
        mod.object = new_arm

    # 7. Export to GLB
    out_glb_root = r"E:\MY-PROJECTS\android native football\assets\media\objects\players\models\fullbody.glb"
    out_glb_android = r"E:\MY-PROJECTS\android native football\platform\android\app\src\main\assets\media\objects\players\models\fullbody.glb"

    bpy.ops.object.select_all(action='DESELECT')
    for obj in export_objects:
        obj.select_set(True)
    new_arm.select_set(True)
    bpy.context.view_layer.objects.active = new_arm

    bpy.ops.export_scene.gltf(
        filepath=out_glb_root,
        export_format='GLB',
        use_selection=True,
        export_apply=True,
        export_skins=True,
        export_all_influences=False,
        export_def_bones=True
    )
    print(f"Exported A-pose Yamal to {out_glb_root}")

    shutil.copy2(out_glb_root, out_glb_android)
    print(f"Copied to Android assets {out_glb_android}")
    print("=== PIPELINE COMPLETE ===")

if __name__ == "__main__":
    build_apose_yamal()
