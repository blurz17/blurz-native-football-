import bpy
import os
import shutil
import math

def generate_textures():
    textures_src = r"E:\MY-PROJECTS\android native football\scratch\alnassr_cr7_ColourMap.png"
    assets_tex_dir = r"E:\MY-PROJECTS\android native football\assets\media\objects\players\textures"
    android_tex_dir = r"E:\MY-PROJECTS\android native football\platform\android\app\src\main\assets\media\objects\players\textures"

    os.makedirs(assets_tex_dir, exist_ok=True)
    os.makedirs(android_tex_dir, exist_ok=True)

    # Copy master player texture
    shutil.copy2(textures_src, os.path.join(assets_tex_dir, "alnassr_cr7.png"))
    shutil.copy2(textures_src, os.path.join(android_tex_dir, "alnassr_cr7.png"))

    # Generate hair texture using bpy
    hair_img = bpy.data.images.new("alnassr_cr7_hair", width=64, height=64)
    # Dark espresso color (0.08, 0.06, 0.04, 1.0)
    pixels = [0.08, 0.06, 0.04, 1.0] * (64 * 64)
    hair_img.pixels = pixels
    hair_path_assets = os.path.join(assets_tex_dir, "alnassr_cr7_hair.png")
    hair_path_android = os.path.join(android_tex_dir, "alnassr_cr7_hair.png")
    hair_img.filepath_raw = hair_path_assets
    hair_img.file_format = 'PNG'
    hair_img.save()
    shutil.copy2(hair_path_assets, hair_path_android)
    print("Player and Hair textures generated successfully!")

def export_player():
    blend_path = r"E:\MY-PROJECTS\android native football\models_blender\realistic_player_showcase.blend"
    bpy.ops.wm.open_mainfile(filepath=blend_path)

    arm = bpy.data.objects['Armature']
    bpy.context.view_layer.objects.active = arm
    bpy.ops.object.mode_set(mode='POSE')

    for pb in arm.pose.bones:
        pb.rotation_mode = 'XYZ'
        pb.rotation_euler = (0, 0, 0)

    # Naturally lower arms from horizontal T-pose to natural relaxed standing A-pose
    arm.pose.bones['mixamorig5:LeftArm'].rotation_euler = (math.radians(68), 0, math.radians(-16))
    arm.pose.bones['mixamorig5:LeftForeArm'].rotation_euler = (math.radians(14), 0, 0)
    arm.pose.bones['mixamorig5:RightArm'].rotation_euler = (math.radians(68), 0, math.radians(16))
    arm.pose.bones['mixamorig5:RightForeArm'].rotation_euler = (math.radians(14), 0, 0)

    bpy.ops.object.mode_set(mode='OBJECT')

    # Record world-space bone heads for new armature
    mat = arm.matrix_world
    b_heads_world = {
        'body': mat @ arm.pose.bones['mixamorig5:Hips'].head,
        'middle': mat @ arm.pose.bones['mixamorig5:Spine1'].head,
        'neck': mat @ arm.pose.bones['mixamorig5:Neck'].head,
        'left_shoulder': mat @ arm.pose.bones['mixamorig5:LeftShoulder'].head,
        'left_elbow': mat @ arm.pose.bones['mixamorig5:LeftForeArm'].head,
        'right_shoulder': mat @ arm.pose.bones['mixamorig5:RightShoulder'].head,
        'right_elbow': mat @ arm.pose.bones['mixamorig5:RightForeArm'].head,
        'left_thigh': mat @ arm.pose.bones['mixamorig5:LeftUpLeg'].head,
        'left_knee': mat @ arm.pose.bones['mixamorig5:LeftLeg'].head,
        'left_ankle': mat @ arm.pose.bones['mixamorig5:LeftFoot'].head,
        'right_thigh': mat @ arm.pose.bones['mixamorig5:RightUpLeg'].head,
        'right_knee': mat @ arm.pose.bones['mixamorig5:RightLeg'].head,
        'right_ankle': mat @ arm.pose.bones['mixamorig5:RightFoot'].head,
    }

    # Bone mapping dictionary from Mixamo 65 bones to Engine 13 bones
    bone_map = {
        'mixamorig5:Hips': 'body',
        'mixamorig5:Spine': 'middle',
        'mixamorig5:Spine1': 'middle',
        'mixamorig5:Spine2': 'middle',
        'mixamorig5:Neck': 'neck',
        'mixamorig5:Head': 'neck',
        'mixamorig5:HeadTop_End': 'neck',
        'mixamorig5:LeftShoulder': 'left_shoulder',
        'mixamorig5:LeftArm': 'left_shoulder',
        'mixamorig5:LeftForeArm': 'left_elbow',
        'mixamorig5:LeftHand': 'left_elbow',
        'mixamorig5:RightShoulder': 'right_shoulder',
        'mixamorig5:RightArm': 'right_shoulder',
        'mixamorig5:RightForeArm': 'right_elbow',
        'mixamorig5:RightHand': 'right_elbow',
        'mixamorig5:LeftUpLeg': 'left_thigh',
        'mixamorig5:LeftLeg': 'left_knee',
        'mixamorig5:LeftFoot': 'left_ankle',
        'mixamorig5:LeftToeBase': 'left_ankle',
        'mixamorig5:LeftToe_End': 'left_ankle',
        'mixamorig5:RightUpLeg': 'right_thigh',
        'mixamorig5:RightLeg': 'right_knee',
        'mixamorig5:RightFoot': 'right_ankle',
        'mixamorig5:RightToeBase': 'right_ankle',
        'mixamorig5:RightToe_End': 'right_ankle',
    }
    for bname in arm.data.bones.keys():
        if 'LeftHand' in bname: bone_map[bname] = 'left_elbow'
        elif 'RightHand' in bname: bone_map[bname] = 'right_elbow'

    engine_bones = [
        'body', 'middle', 'neck',
        'left_shoulder', 'left_elbow',
        'right_shoulder', 'right_elbow',
        'left_thigh', 'left_knee', 'left_ankle',
        'right_thigh', 'right_knee', 'right_ankle'
    ]

    body_mesh_names = ['Ch38_Body', 'Ch38_Shirt', 'Ch38_Shorts', 'Ch38_Socks', 'Ch38_Shoes']
    hair_mesh_name = 'Ch38_Hair'

    # Remove unwanted objects
    for obj in list(bpy.data.objects):
        if obj.type == 'MESH' and obj.name not in body_mesh_names and obj.name != hair_mesh_name:
            bpy.data.objects.remove(obj)
        elif obj.type in ['LIGHT', 'CAMERA']:
            bpy.data.objects.remove(obj)

    target_objs = [bpy.data.objects[n] for n in body_mesh_names + [hair_mesh_name] if n in bpy.data.objects]

    # 1. Apply Armature modifier on meshes in current posed state
    for obj in target_objs:
        bpy.context.view_layer.objects.active = obj
        for mod in list(obj.modifiers):
            if mod.type == 'ARMATURE':
                bpy.ops.object.modifier_apply(modifier=mod.name)

    # 2. Unparent meshes keeping world transform!
    bpy.ops.object.select_all(action='DESELECT')
    for obj in target_objs:
        obj.select_set(True)
    bpy.context.view_layer.objects.active = target_objs[0]
    bpy.ops.object.parent_clear(type='CLEAR_KEEP_TRANSFORM')

    # 3. Apply all transforms to permanently bake world scale (meters) and orientation into vertices!
    bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)

    # 4. Remove old Mixamo armature
    bpy.data.objects.remove(arm)

    # 5. Map vertex groups to 13 engine bones
    for obj in target_objs:
        orig_vg_names = {vg.index: vg.name for vg in obj.vertex_groups}
        v_weights = []
        for v in obj.data.vertices:
            w_dict = {eb: 0.0 for eb in engine_bones}
            for g in v.groups:
                if g.group in orig_vg_names:
                    src_name = orig_vg_names[g.group]
                    target_bone = bone_map.get(src_name, None)
                    if target_bone and target_bone in w_dict:
                        w_dict[target_bone] += g.weight
            v_weights.append(w_dict)

        obj.vertex_groups.clear()
        new_vgs = {eb: obj.vertex_groups.new(name=eb) for eb in engine_bones}

        for v_idx, w_dict in enumerate(v_weights):
            total_w = sum(w_dict.values())
            if total_w > 0.0001:
                for eb, w in w_dict.items():
                    if w > 0.001:
                        new_vgs[eb].add([v_idx], w / total_w, 'REPLACE')
            else:
                fb = 'neck' if obj.name == hair_mesh_name else 'body'
                new_vgs[fb].add([v_idx], 1.0, 'REPLACE')

    # 6. Decimate hair (~1,200 verts)
    hair_obj = bpy.data.objects[hair_mesh_name]
    bpy.context.view_layer.objects.active = hair_obj
    mod_hair = hair_obj.modifiers.new(name="DecimateHair", type='DECIMATE')
    mod_hair.ratio = 0.18
    bpy.ops.object.modifier_apply(modifier="DecimateHair")
    print(f"Hair vertices after decimation: {len(hair_obj.data.vertices)}")

    # 7. Join body parts into unified player body mesh
    body_objs = [bpy.data.objects[n] for n in body_mesh_names if n in bpy.data.objects]
    bpy.ops.object.select_all(action='DESELECT')
    for bo in body_objs:
        bo.select_set(True)
    bpy.context.view_layer.objects.active = body_objs[0]
    bpy.ops.object.join()
    body_obj = bpy.context.active_object
    body_obj.name = "player_body"

    # 8. Decimate body (~7,500 verts) for optimal mobile FPS
    mod_body = body_obj.modifiers.new(name="DecimateBody", type='DECIMATE')
    mod_body.ratio = 0.42
    bpy.ops.object.modifier_apply(modifier="DecimateBody")
    print(f"Body vertices after decimation: {len(body_obj.data.vertices)}")

    # 9. Materials
    # Player Material
    mat_player = bpy.data.materials.new(name="PlayerMaterial")
    mat_player.use_nodes = True
    p_bsdf = mat_player.node_tree.nodes.get('Principled BSDF')
    p_bsdf.inputs['Roughness'].default_value = 0.45
    p_tex = mat_player.node_tree.nodes.new('ShaderNodeTexImage')
    p_tex.image = bpy.data.images.load(r"E:\MY-PROJECTS\android native football\assets\media\objects\players\textures\alnassr_cr7.png")
    mat_player.node_tree.links.new(p_tex.outputs['Color'], p_bsdf.inputs['Base Color'])
    body_obj.data.materials.clear()
    body_obj.data.materials.append(mat_player)

    # Hair Material
    mat_hair = bpy.data.materials.new(name="PlayerHairMaterial")
    mat_hair.use_nodes = True
    h_bsdf = mat_hair.node_tree.nodes.get('Principled BSDF')
    h_bsdf.inputs['Roughness'].default_value = 0.50
    h_tex = mat_hair.node_tree.nodes.new('ShaderNodeTexImage')
    h_tex.image = bpy.data.images.load(r"E:\MY-PROJECTS\android native football\assets\media\objects\players\textures\alnassr_cr7_hair.png")
    mat_hair.node_tree.links.new(h_tex.outputs['Color'], h_bsdf.inputs['Base Color'])
    hair_obj.data.materials.clear()
    hair_obj.data.materials.append(mat_hair)

    # 10. Create clean 13-bone Armature at world origin
    bpy.ops.object.armature_add(location=(0, 0, 0))
    new_arm = bpy.context.active_object
    new_arm.name = "PlayerArmature"
    bpy.ops.object.mode_set(mode='EDIT')
    for b in list(new_arm.data.edit_bones):
        new_arm.data.edit_bones.remove(b)

    tails = {
        'body': b_heads_world['middle'],
        'middle': b_heads_world['neck'],
        'neck': (b_heads_world['neck'].x, b_heads_world['neck'].y, b_heads_world['neck'].z + 0.25),
        'left_shoulder': b_heads_world['left_elbow'],
        'left_elbow': (b_heads_world['left_elbow'].x, b_heads_world['left_elbow'].y, b_heads_world['left_elbow'].z - 0.25),
        'right_shoulder': b_heads_world['right_elbow'],
        'right_elbow': (b_heads_world['right_elbow'].x, b_heads_world['right_elbow'].y, b_heads_world['right_elbow'].z - 0.25),
        'left_thigh': b_heads_world['left_knee'],
        'left_knee': b_heads_world['left_ankle'],
        'left_ankle': (b_heads_world['left_ankle'].x, b_heads_world['left_ankle'].y - 0.15, b_heads_world['left_ankle'].z - 0.08),
        'right_thigh': b_heads_world['right_knee'],
        'right_knee': b_heads_world['right_ankle'],
        'right_ankle': (b_heads_world['right_ankle'].x, b_heads_world['right_ankle'].y - 0.15, b_heads_world['right_ankle'].z - 0.08),
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
    for eb in engine_bones:
        b = new_arm.data.edit_bones.new(eb)
        b.head = b_heads_world[eb]
        b.tail = tails[eb]
        ebones[eb] = b

    for child, parent in parents.items():
        ebones[child].parent = ebones[parent]

    bpy.ops.object.mode_set(mode='OBJECT')

    # Parent meshes to new armature
    for obj in [body_obj, hair_obj]:
        obj.parent = new_arm
        mod = obj.modifiers.new(name="Armature", type='ARMATURE')
        mod.object = new_arm

    out_glb = r"E:\MY-PROJECTS\android native football\assets\media\objects\players\models\fullbody.glb"
    android_glb = r"E:\MY-PROJECTS\android native football\platform\android\app\src\main\assets\media\objects\players\models\fullbody.glb"

    bpy.ops.object.select_all(action='DESELECT')
    body_obj.select_set(True)
    hair_obj.select_set(True)
    new_arm.select_set(True)
    bpy.context.view_layer.objects.active = new_arm

    bpy.ops.export_scene.gltf(
        filepath=out_glb,
        export_format='GLB',
        use_selection=True,
        export_apply=True,
        export_skins=True,
        export_all_influences=False,
        export_def_bones=True
    )
    print(f"Exported fullbody.glb to {out_glb}")
    shutil.copy2(out_glb, android_glb)
    print(f"Copied fullbody.glb to {android_glb}")

if __name__ == "__main__":
    generate_textures()
    export_player()
