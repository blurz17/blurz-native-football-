import bpy
import math
import os
import shutil
from mathutils import Vector, Matrix

def build_perfect_engine_player():
    print("=== BUILDING PERFECT ENGINE PLAYER FROM universal_player_engine_13b.glb ===")
    
    src_glb = r"tools\workbench\universal_player_engine_13b.glb"
    if not os.path.exists(src_glb):
        raise FileNotFoundError(f"Source GLB not found: {src_glb}")

    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=src_glb)
    print(f"Imported {src_glb}")

    # Remove any stray helper objects like Icosphere
    for obj in list(bpy.data.objects):
        if obj.type == 'MESH' and len(obj.data.vertices) < 100:
            print(f"Removing helper object: {obj.name}")
            bpy.data.objects.remove(obj, do_unlink=True)

    arm = bpy.data.objects['Armature_Yamal']
    mesh_objs = [obj for obj in bpy.data.objects if obj.type == 'MESH']
    print(f"Found {len(mesh_objs)} meshes: {[m.name for m in mesh_objs]}")

    # 1. Fix Clavicle/Chest Vertex Weights on Jersey and Skin
    # Any vertex on collarbone / chest with |X| < 0.15 should not move with shoulder
    for obj in mesh_objs:
        vg_lsh = obj.vertex_groups.get('left_shoulder')
        vg_rsh = obj.vertex_groups.get('right_shoulder')
        vg_mid = obj.vertex_groups.get('middle')
        if not vg_mid:
            vg_mid = obj.vertex_groups.new(name='middle')

        for v in obj.data.vertices:
            # Left shoulder transfer
            if vg_lsh:
                try:
                    w = vg_lsh.weight(v.index)
                    if w > 0.0:
                        if v.co.x < 0.13: # Pure chest / sternum
                            vg_lsh.remove([v.index])
                            vg_mid.add([v.index], w, 'ADD')
                        elif v.co.x < 0.17: # Smooth transition zone
                            factor = (v.co.x - 0.13) / 0.04
                            vg_lsh.add([v.index], w * factor, 'REPLACE')
                            vg_mid.add([v.index], w * (1.0 - factor), 'ADD')
                except RuntimeError:
                    pass

            # Right shoulder transfer
            if vg_rsh:
                try:
                    w = vg_rsh.weight(v.index)
                    if w > 0.0:
                        if v.co.x > -0.13: # Pure chest / sternum
                            vg_rsh.remove([v.index])
                            vg_mid.add([v.index], w, 'ADD')
                        elif v.co.x > -0.17: # Smooth transition zone
                            factor = (-v.co.x - 0.13) / 0.04
                            vg_rsh.add([v.index], w * factor, 'REPLACE')
                            vg_mid.add([v.index], w * (1.0 - factor), 'ADD')
                except RuntimeError:
                    pass

    print("Fixed clavicle/chest weights: locked collarbone to middle (torso)")

    # 2. Enter POSE mode to rotate arms to vertical hang
    bpy.context.view_layer.objects.active = arm
    bpy.ops.object.mode_set(mode='POSE')

    arm.pose.bones['left_shoulder'].rotation_mode = 'XYZ'
    arm.pose.bones['left_shoulder'].rotation_euler = (math.radians(86), 0, math.radians(-5))
    arm.pose.bones['left_elbow'].rotation_mode = 'XYZ'
    arm.pose.bones['left_elbow'].rotation_euler = (math.radians(4), 0, 0)

    arm.pose.bones['right_shoulder'].rotation_mode = 'XYZ'
    arm.pose.bones['right_shoulder'].rotation_euler = (math.radians(86), 0, math.radians(5))
    arm.pose.bones['right_elbow'].rotation_mode = 'XYZ'
    arm.pose.bones['right_elbow'].rotation_euler = (math.radians(4), 0, 0)

    bpy.context.view_layer.update()
    bpy.ops.object.mode_set(mode='OBJECT')
    print("Posed arms to engine vertical hang")

    # 3. Apply armature modifier to permanently bake posed vertices
    for obj in mesh_objs:
        bpy.context.view_layer.objects.active = obj
        for mod in list(obj.modifiers):
            if mod.type == 'ARMATURE':
                bpy.ops.object.modifier_apply(modifier=mod.name)

    print("Permanently baked vertical pose into all mesh vertices")

    # 4. Remove old armature
    bpy.data.objects.remove(arm, do_unlink=True)

    # 5. Scale character uniformly to match engine defaultPlayerHeight (1.92m)
    # Target shoulder height in engine is 1.59m. Posed shoulder was at ~1.327m.
    # Scale factor = 1.59 / 1.3273 = 1.1979
    SCALE = 1.198
    for obj in mesh_objs:
        obj.scale = (SCALE, SCALE, SCALE)
        bpy.context.view_layer.objects.active = obj
        bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)

    print(f"Scaled all meshes by {SCALE:.4f}x to match engine player height (1.92m)")

    # 6. Build the canonical 13-bone PlayerArmature with exact engine origPos
    bpy.ops.object.armature_add(location=(0, 0, 0))
    new_arm = bpy.context.active_object
    new_arm.name = "PlayerArmature"

    bpy.ops.object.mode_set(mode='EDIT')
    for b in list(new_arm.data.edit_bones):
        new_arm.data.edit_bones.remove(b)

    # Exact engine joint positions from player.object:
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
    print("Created clean 13-bone PlayerArmature matching engine origPos")

    # 7. Parent all meshes to new armature and add Armature modifier
    for obj in mesh_objs:
        obj.parent = new_arm
        mod = obj.modifiers.new(name="Armature", type='ARMATURE')
        mod.object = new_arm

    # 8. Export to GLB targets
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
    print(f"Exported perfected GLB to {out_file} ({os.path.getsize(out_file):,} bytes)")

    for t in targets[1:]:
        shutil.copy2(out_file, t)
        print(f"Copied perfected GLB to {t} ({os.path.getsize(t):,} bytes)")

    print("=== PERFECT ENGINE PLAYER PIPELINE COMPLETED SUCCESSFULLY ===")

if __name__ == "__main__":
    build_perfect_engine_player()
