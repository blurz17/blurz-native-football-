import bpy
import math
import os
import shutil
from mathutils import Vector, Euler, Matrix

def build_exact_yamal():
    src_glb = r'E:\Downlaod\football asset\players\lamine_yamal_model.glb'
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=src_glb)
    print(f"Loaded {src_glb}")

    # Remove helper object Icosphere
    for obj in list(bpy.data.objects):
        if obj.type == 'MESH' and 'icosphere' in obj.name.lower():
            bpy.data.objects.remove(obj, do_unlink=True)

    arm = bpy.data.objects['Object_4']
    
    # 1. Rename retained meshes cleanly
    name_map = {
        "Object_10": "Mesh_Head_Face",       # Yamal face + curly fade hair
        "Object_12": "Mesh_LeftEye",         
        "Object_14": "Mesh_RightEye",        
        "Object_22": "Mesh_Shirt_Jersey",    # Yellow jersey
        "Object_19": "Mesh_Shorts",          # Navy shorts
        "Object_21": "Mesh_Boots_Socks",     # Blue Adidas cleats
        "Object_6":  "Mesh_Skin_Body",       # Yamal skin tone
        "Object_8":  "Mesh_Eyelashes",       # Facial micro-details
        "Object_16": "Mesh_TeethLower",      
        "Object_18": "Mesh_TeethUpper",      
    }
    for old_n, new_n in name_map.items():
        o = bpy.data.objects.get(old_n)
        if o:
            o.name = new_n

    mesh_objs = [o for o in bpy.data.objects if o.type == 'MESH']
    print(f"Retained meshes ({len(mesh_objs)}): {[m.name for m in mesh_objs]}")

    # 2. POSE MODE ON Object_4 WHILE ORIGINAL VERTEX GROUPS STILL EXIST!
    bpy.context.view_layer.objects.active = arm
    bpy.ops.object.mode_set(mode='POSE')

    pb_l_sh = arm.pose.bones.get('LeftArm_013')
    pb_l_el = arm.pose.bones.get('LeftForeArm_014')
    pb_r_sh = arm.pose.bones.get('RightArm_039')
    pb_r_el = arm.pose.bones.get('RightForeArm_040')

    for pb in [pb_l_sh, pb_l_el, pb_r_sh, pb_r_el]:
        if pb:
            pb.rotation_mode = 'XYZ'

    if pb_l_sh:
        pb_l_sh.rotation_euler = (math.radians(86), 0, math.radians(-5))
    if pb_l_el:
        pb_l_el.rotation_euler = (math.radians(4), 0, 0)

    if pb_r_sh:
        pb_r_sh.rotation_euler = (math.radians(86), 0, math.radians(5))
    if pb_r_el:
        pb_r_el.rotation_euler = (math.radians(4), 0, 0)

    # Legs: natural parallel vertical stance
    pb_l_th = arm.pose.bones.get('LeftUpLeg_063')
    pb_r_th = arm.pose.bones.get('RightUpLeg_068')

    if pb_l_th:
        pb_l_th.rotation_mode = 'XYZ'
        pb_l_th.rotation_euler = (0, 0, math.radians(-3.5))

    if pb_r_th:
        pb_r_th.rotation_mode = 'XYZ'
        pb_r_th.rotation_euler = (0, 0, math.radians(3.5))

    bpy.context.view_layer.update()
    bpy.ops.object.mode_set(mode='OBJECT')
    print("Posed arms to vertical hang and legs to parallel stance")

    # 3. Permanently bake posed vertices into all meshes
    for obj in mesh_objs:
        bpy.context.view_layer.objects.active = obj
        for mod in list(obj.modifiers):
            if mod.type == 'ARMATURE':
                bpy.ops.object.modifier_apply(modifier=mod.name)

    print("Baked posed coordinates permanently into all mesh vertices")

    # Verify baked bounds
    for obj in mesh_objs:
        xs = [v.co.x for v in obj.data.vertices]
        print(f"  Posed {obj.name:22}: X=[{min(xs):.3f}, {max(xs):.3f}]")

    # 4. Canonical 13-bone mapping
    engine_13_bones = [
        "body", "middle", "neck",
        "left_shoulder", "left_elbow",
        "right_shoulder", "right_elbow",
        "left_thigh", "left_knee", "left_ankle",
        "right_thigh", "right_knee", "right_ankle"
    ]

    collapse_to_13_map = {
        # Torso & Spine
        "Hips_01": "body",
        "_rootJoint": "body",
        "Spine_02": "middle",
        "Spine1_03": "middle",
        "Spine2_04": "middle",
        # Clavicles -> LOCKED TO MIDDLE (TORSO) to eliminate shoulder horn mutation
        "LeftShoulder_012": "middle",
        "RightShoulder_038": "middle",
        # Neck & Head
        "Neck_05": "neck",
        "Neck1_06": "neck",
        "Neck2_07": "neck",
        "Head_08": "neck",
        "LeftEye_09": "neck",
        "RightEye_010": "neck",
        "HeadTop_End_011": "neck",
        # Left Arm
        "LeftArm_013": "left_shoulder",
        "LeftForeArm_014": "left_elbow",
        "LeftForeArm1_015": "left_elbow",
        "LeftForeArm2_016": "left_elbow",
        "LeftHand_017": "left_elbow",
        "LeftHandThumb1_018": "left_elbow", "LeftHandThumb2_019": "left_elbow", "LeftHandThumb3_020": "left_elbow", "LeftHandThumb4_021": "left_elbow",
        "LeftHandIndex1_022": "left_elbow", "LeftHandIndex2_023": "left_elbow", "LeftHandIndex3_024": "left_elbow", "LeftHandIndex4_025": "left_elbow",
        "LeftHandMiddle1_026": "left_elbow", "LeftHandMiddle2_027": "left_elbow", "LeftHandMiddle3_028": "left_elbow", "LeftHandMiddle4_029": "left_elbow",
        "LeftHandRing1_030": "left_elbow", "LeftHandRing2_031": "left_elbow", "LeftHandRing3_032": "left_elbow", "LeftHandRing4_033": "left_elbow",
        "LeftHandPinky1_034": "left_elbow", "LeftHandPinky2_035": "left_elbow", "LeftHandPinky3_036": "left_elbow", "LeftHandPinky4_037": "left_elbow",
        # Right Arm
        "RightArm_039": "right_shoulder",
        "RightForeArm_040": "right_elbow",
        "RightForeArm1_041": "right_elbow",
        "RightForeArm2_042": "right_elbow",
        "RightHand_043": "right_elbow",
        "RightHandThumb1_044": "right_elbow", "RightHandThumb2_045": "right_elbow", "RightHandThumb3_046": "right_elbow", "RightHandThumb4_047": "right_elbow",
        "RightHandIndex1_048": "right_elbow", "RightHandIndex2_049": "right_elbow", "RightHandIndex3_050": "right_elbow", "RightHandIndex4_051": "right_elbow",
        "RightHandMiddle1_052": "right_elbow", "RightHandMiddle2_00": "right_elbow", "RightHandMiddle3_053": "right_elbow", "RightHandMiddle4_054": "right_elbow",
        "RightHandRing1_055": "right_elbow", "RightHandRing2_056": "right_elbow", "RightHandRing3_057": "right_elbow", "RightHandRing4_058": "right_elbow",
        "RightHandPinky1_059": "right_elbow", "RightHandPinky2_060": "right_elbow", "RightHandPinky3_061": "right_elbow", "RightHandPinky4_062": "right_elbow",
        # Left Leg
        "LeftUpLeg_063": "left_thigh",
        "LeftLeg_064": "left_knee",
        "LeftFoot_065": "left_ankle",
        "LeftToeBase_066": "left_ankle",
        "LeftToe_End_067": "left_ankle",
        # Right Leg
        "RightUpLeg_068": "right_thigh",
        "RightLeg_069": "right_knee",
        "RightFoot_070": "right_ankle",
        "RightToeBase_071": "right_ankle",
        "RightToe_End_072": "right_ankle",
    }

    for obj in mesh_objs:
        for eb in engine_13_bones:
            if not obj.vertex_groups.get(eb):
                obj.vertex_groups.new(name=eb)

        for v in obj.data.vertices:
            w13 = {eb: 0.0 for eb in engine_13_bones}
            for old_b, target_13 in collapse_to_13_map.items():
                vg = obj.vertex_groups.get(old_b)
                if vg:
                    try:
                        w13[target_13] += vg.weight(v.index)
                    except RuntimeError:
                        pass
            for eb, w in w13.items():
                if w > 0.0:
                    obj.vertex_groups[eb].add([v.index], w, 'REPLACE')

        # Clavicle is already cleanly locked to torso via LeftShoulder_012 -> middle
        # No artificial X-coordinate threshold that corrupts inner arm vertices into armpit webbing.

        # Remove old vertex groups that are not part of the 13 engine bones
        for vg in list(obj.vertex_groups):
            if vg.name not in engine_13_bones:
                obj.vertex_groups.remove(vg)

    # Remove temporary raw armature
    bpy.data.objects.remove(arm, do_unlink=True)
    print("Transferred vertex weights to 13 canonical engine bones")

    # 5. Scale character uniformly to match engine defaultPlayerHeight (1.92m)
    SCALE = 1.198
    for obj in mesh_objs:
        obj.scale = (SCALE, SCALE, SCALE)
        bpy.context.view_layer.objects.active = obj
        bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)

    print(f"Scaled all meshes uniformly by {SCALE:.4f}x")

    # 6. Build the canonical 13-bone PlayerArmature with exact engine origPos
    bpy.ops.object.armature_add(location=(0, 0, 0))
    new_arm = bpy.context.active_object
    new_arm.name = "PlayerArmature"

    bpy.ops.object.mode_set(mode='EDIT')
    for b in list(new_arm.data.edit_bones):
        new_arm.data.edit_bones.remove(b)

    # Exact engine joint positions from player.object
    engine_heads = {
        'body': Vector((0.000, 0.000, 0.960)),
        'middle': Vector((0.000, 0.000, 1.110)),
        'neck': Vector((0.000, -0.030, 1.610)),
        'left_shoulder': Vector((0.160, -0.010, 1.590)),
        'left_elbow': Vector((0.150, -0.010, 1.260)),
        'right_shoulder': Vector((-0.160, -0.010, 1.590)),
        'right_elbow': Vector((-0.150, -0.010, 1.260)),
        'left_thigh': Vector((0.087, 0.000, 0.950)),
        'left_knee': Vector((0.087, 0.000, 0.530)),
        'left_ankle': Vector((0.087, -0.040, 0.090)),
        'right_thigh': Vector((-0.087, 0.000, 0.950)),
        'right_knee': Vector((-0.087, 0.000, 0.530)),
        'right_ankle': Vector((-0.087, -0.040, 0.090)),
    }

    engine_tails = {
        'body': engine_heads['middle'],
        'middle': engine_heads['neck'],
        'neck': Vector((0.000, -0.030, 1.880)),
        'left_shoulder': engine_heads['left_elbow'],
        'left_elbow': Vector((0.150, -0.010, 0.950)),
        'right_shoulder': engine_heads['right_elbow'],
        'right_elbow': Vector((-0.150, -0.010, 0.950)),
        'left_thigh': engine_heads['left_knee'],
        'left_knee': engine_heads['left_ankle'],
        'left_ankle': Vector((0.087, -0.180, 0.020)),
        'right_thigh': engine_heads['right_knee'],
        'right_knee': engine_heads['right_ankle'],
        'right_ankle': Vector((-0.087, -0.180, 0.020)),
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
    for bname, head_pos in engine_heads.items():
        eb = new_arm.data.edit_bones.new(bname)
        eb.head = head_pos
        eb.tail = engine_tails[bname]
        ebones[bname] = eb

    for child, parent in parents.items():
        ebones[child].parent = ebones[parent]

    bpy.ops.object.mode_set(mode='OBJECT')
    print("Created canonical 13-bone PlayerArmature")

    # 7. Parent all meshes to new PlayerArmature with Armature modifier
    for obj in mesh_objs:
        obj.parent = new_arm
        mod = obj.modifiers.new(name="Armature", type='ARMATURE')
        mod.object = new_arm

    # 8. Export to target GLB locations
    targets = [
        r"assets\media\objects\players\models\fullbody.glb",
        r"platform\android\app\src\main\assets\media\objects\players\models\fullbody.glb",
        r"tools\workbench\universal_player_engine_13b.glb",
    ]

    bpy.ops.object.select_all(action='DESELECT')
    new_arm.select_set(True)
    for obj in mesh_objs:
        obj.select_set(True)
    bpy.context.view_layer.objects.active = new_arm

    out_file = targets[0]
    bpy.ops.export_scene.gltf(
        filepath=out_file,
        export_format='GLB',
        use_selection=True,
        export_apply=False,
        export_skins=True,
        export_all_influences=False,
        export_def_bones=True
    )
    print(f"Exported clean perfected GLB to {out_file} ({os.path.getsize(out_file):,} bytes)")

    for t in targets[1:]:
        shutil.copy2(out_file, t)
        print(f"Copied to {t} ({os.path.getsize(t):,} bytes)")

    print("=== EXACT YAMAL ENGINE 13-BONE MODEL BUILT SUCCESSFULLY ===")

if __name__ == '__main__':
    build_exact_yamal()
