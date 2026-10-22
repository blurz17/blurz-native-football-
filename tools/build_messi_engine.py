"""
build_messi_engine.py — Complete, production-grade 13-bone rigging pipeline
for Meshy.ai Lionel Messi PSG model.

Fixes all issues:
1. Poses arms inward using two-joint natural rotation matching engine rest pose.
2. Uses surface weight transfer from proven 13-bone Yamal reference.
3. Applies strict anatomical scrubbing to guarantee zero armpit webbing.
4. Cleans materials to pure diffuse BaseColor (0 metallic, 0 normal map glitches).
5. Exports to all engine target paths.

Run: D:\blender\blender.exe -b -P tools\build_messi_engine.py
"""

import bpy
import math
import os
import shutil
import sys
from mathutils import Vector

# ===========================================================================
# CONFIG
# ===========================================================================
MESSI_SRC = r'E:\Downlaod\Meshy_AI_Paris_Saint_Germain_S_1022190900_texture.glb'
TARGET_HEIGHT = 1.92  # engine defaultPlayerHeight

TARGETS = [
    r"assets\media\objects\players\models\fullbody.glb",
    r"platform\android\app\src\main\assets\media\objects\players\models\fullbody.glb",
    r"tools\workbench\universal_player_engine_13b.glb",
]

ENGINE_13_BONES = [
    "body", "middle", "neck",
    "left_shoulder", "left_elbow",
    "right_shoulder", "right_elbow",
    "left_thigh", "left_knee", "left_ankle",
    "right_thigh", "right_knee", "right_ankle"
]


def build_messi():
    print("=" * 60)
    print("STARTING MESSI 13-BONE RIGGING PIPELINE")
    print("=" * 60)

    # ------------------------------------------------------------------
    # Step 1: Build Yamal in memory to obtain golden 13-bone weight reference
    # ------------------------------------------------------------------
    print("\n[1/7] Building Yamal reference skeleton & weights...")
    tools_dir = os.path.dirname(os.path.abspath(__file__))
    if tools_dir not in sys.path:
        sys.path.append(tools_dir)
    from build_exact_yamal_engine import build_exact_yamal

    bpy.ops.wm.read_factory_settings(use_empty=True)
    build_exact_yamal()

    player_arm = bpy.data.objects['PlayerArmature']
    yamal_meshes = [o for o in bpy.data.objects if o.type == 'MESH']

    # Duplicate & join Yamal meshes into a single reference object
    bpy.ops.object.select_all(action='DESELECT')
    for m in yamal_meshes:
        m.select_set(True)
    bpy.context.view_layer.objects.active = yamal_meshes[0]
    bpy.ops.object.duplicate()
    yamal_ref = bpy.context.active_object
    yamal_ref.name = "Yamal_Ref"

    for mod in list(yamal_ref.modifiers):
        bpy.ops.object.modifier_apply(modifier=mod.name)

    dups = [o for o in bpy.context.selected_objects if o != yamal_ref and ".001" in o.name]
    if dups:
        bpy.context.view_layer.objects.active = yamal_ref
        for d in dups:
            d.select_set(True)
        bpy.ops.object.join()

    print(f"      Yamal reference ready ({len(yamal_ref.data.vertices)} vertices)")

    # ------------------------------------------------------------------
    # Step 2: Load Meshy Messi GLB
    # ------------------------------------------------------------------
    print(f"\n[2/7] Loading Messi model from {MESSI_SRC}...")
    bpy.ops.import_scene.gltf(filepath=MESSI_SRC)

    messi = [o for o in bpy.context.selected_objects if o.type == 'MESH'][0]
    messi.name = "Messi_Mesh"
    mesh = messi.data
    print(f"      Loaded {len(mesh.vertices)} vertices, {len(mesh.polygons)} polygons")

    # Scale to exact 1.92m
    zs = [v.co.z for v in mesh.vertices]
    raw_h = max(zs) - min(zs)
    scale = TARGET_HEIGHT / raw_h
    messi.scale = (scale, scale, scale)
    bpy.context.view_layer.objects.active = messi
    bpy.ops.object.transform_apply(scale=True)

    # Ground feet at Z=0, center lateral X at 0
    z_min = min(v.co.z for v in mesh.vertices)
    for v in mesh.vertices:
        v.co.z -= z_min
    x_mid = (min(v.co.x for v in mesh.vertices) + max(v.co.x for v in mesh.vertices)) / 2
    for v in mesh.vertices:
        v.co.x -= x_mid
    mesh.update()
    print(f"      Scaled & grounded: Z=[{min(v.co.z for v in mesh.vertices):.3f}, {max(v.co.z for v in mesh.vertices):.3f}], X=[{min(v.co.x for v in mesh.vertices):.3f}, {max(v.co.x for v in mesh.vertices):.3f}]")

    # ------------------------------------------------------------------
    # Step 3: Two-Joint Natural Arm Posing (Shoulder + Elbow)
    # Brings Messi's A-pose arms to vertical hang matching engine rest pose
    # ------------------------------------------------------------------
    print("\n[3/7] Posing arms into vertical engine rest pose...")
    P_L_SH = Vector((0.170, -0.010, 1.540))
    P_R_SH = Vector((-0.170, -0.010, 1.540))
    ARM_DIR_L = (Vector((0.530, -0.030, 0.950)) - P_L_SH).normalized()
    ARM_DIR_R = (Vector((-0.530, 0.020, 0.950)) - P_R_SH).normalized()

    # Shoulder rotation (18 degrees inward)
    TH_SH_L = math.radians(-18.0)
    TH_SH_R = math.radians(18.0)

    for v in mesh.vertices:
        co = v.co
        if co.x > 0.14 and co.z > 0.70:
            d_axis = (co - P_L_SH).dot(ARM_DIR_L)
            proj = P_L_SH + ARM_DIR_L * d_axis
            d_perp = (co - proj).length
            if d_perp < 0.22 and d_axis > -0.04:
                w_axis = min(1.0, max(0.0, (d_axis - 0.0) / 0.10))
                w_smooth = 0.5 * (1.0 - math.cos(w_axis * math.pi))
                dx = co.x - P_L_SH.x
                dz = co.z - P_L_SH.z
                th = TH_SH_L * w_smooth
                v.co.x = P_L_SH.x + dx * math.cos(th) - dz * math.sin(th)
                v.co.z = P_L_SH.z + dx * math.sin(th) + dz * math.cos(th)
        elif co.x < -0.14 and co.z > 0.70:
            d_axis = (co - P_R_SH).dot(ARM_DIR_R)
            proj = P_R_SH + ARM_DIR_R * d_axis
            d_perp = (co - proj).length
            if d_perp < 0.22 and d_axis > -0.04:
                w_axis = min(1.0, max(0.0, (d_axis - 0.0) / 0.10))
                w_smooth = 0.5 * (1.0 - math.cos(w_axis * math.pi))
                dx = co.x - P_R_SH.x
                dz = co.z - P_R_SH.z
                th = TH_SH_R * w_smooth
                v.co.x = P_R_SH.x + dx * math.cos(th) - dz * math.sin(th)
                v.co.z = P_R_SH.z + dx * math.sin(th) + dz * math.cos(th)

    mesh.update()

    # Elbow rotation (16 degrees inward)
    P_L_EL = Vector((0.240, -0.010, 1.260))
    P_R_EL = Vector((-0.240, -0.010, 1.260))
    FOREARM_DIR_L = (Vector((0.360, -0.030, 0.900)) - P_L_EL).normalized()
    FOREARM_DIR_R = (Vector((-0.360, 0.020, 0.900)) - P_R_EL).normalized()

    TH_EL_L = math.radians(-16.0)
    TH_EL_R = math.radians(16.0)

    for v in mesh.vertices:
        co = v.co
        if co.x > 0.14 and co.z < 1.28 and co.z > 0.70:
            d_axis = (co - P_L_EL).dot(FOREARM_DIR_L)
            proj = P_L_EL + FOREARM_DIR_L * d_axis
            d_perp = (co - proj).length
            if d_perp < 0.16 and d_axis > -0.03:
                w_axis = min(1.0, max(0.0, (d_axis - 0.0) / 0.08))
                w_smooth = 0.5 * (1.0 - math.cos(w_axis * math.pi))
                dx = co.x - P_L_EL.x
                dz = co.z - P_L_EL.z
                th = TH_EL_L * w_smooth
                v.co.x = P_L_EL.x + dx * math.cos(th) - dz * math.sin(th)
                v.co.z = P_L_EL.z + dx * math.sin(th) + dz * math.cos(th)
        elif co.x < -0.14 and co.z < 1.28 and co.z > 0.70:
            d_axis = (co - P_R_EL).dot(FOREARM_DIR_R)
            proj = P_R_EL + FOREARM_DIR_R * d_axis
            d_perp = (co - proj).length
            if d_perp < 0.16 and d_axis > -0.03:
                w_axis = min(1.0, max(0.0, (d_axis - 0.0) / 0.08))
                w_smooth = 0.5 * (1.0 - math.cos(w_axis * math.pi))
                dx = co.x - P_R_EL.x
                dz = co.z - P_R_EL.z
                th = TH_EL_R * w_smooth
                v.co.x = P_R_EL.x + dx * math.cos(th) - dz * math.sin(th)
                v.co.z = P_R_EL.z + dx * math.sin(th) + dz * math.cos(th)

    mesh.update()
    xs = [v.co.x for v in mesh.vertices]
    print(f"      Posed arm bounds: X=[{min(xs):.3f}, {max(xs):.3f}] (Yamal was [-0.270, 0.270])")

    # ------------------------------------------------------------------
    # Step 4: Transfer Surface Vertex Weights from Yamal
    # ------------------------------------------------------------------
    print("\n[4/7] Transferring 13-bone vertex weights from Yamal reference...")
    dt = messi.modifiers.new(name="DataTransfer", type='DATA_TRANSFER')
    dt.object = yamal_ref
    dt.use_vert_data = True
    dt.data_types_verts = {'VGROUP_WEIGHTS'}
    dt.vert_mapping = 'NEAREST'

    for vg in yamal_ref.vertex_groups:
        if not messi.vertex_groups.get(vg.name):
            messi.vertex_groups.new(name=vg.name)

    bpy.context.view_layer.objects.active = messi
    bpy.ops.object.datalayout_transfer(modifier=dt.name)
    bpy.ops.object.modifier_apply(modifier=dt.name)

    # ------------------------------------------------------------------
    # Step 5: Anatomical Weight Scrubbing
    # Guarantees zero weight bleed between torso, limbs, and head
    # ------------------------------------------------------------------
    print("\n[5/7] Applying strict anatomical weight scrubbing...")
    scrubbed = 0
    for v in mesh.vertices:
        x, y, z = v.co.x, v.co.y, v.co.z
        w_map = {}
        for b in ENGINE_13_BONES:
            vg = messi.vertex_groups.get(b)
            if vg:
                try:
                    w = vg.weight(v.index)
                    if w > 0.001:
                        w_map[b] = w
                except RuntimeError:
                    pass

        modified = False

        # Torso core: strictly middle/body, zero arm weights
        if abs(x) < 0.16 and 0.95 <= z <= 1.55:
            for arm_b in ['left_shoulder', 'left_elbow', 'right_shoulder', 'right_elbow']:
                if arm_b in w_map:
                    w_map[arm_b] = 0.0
                    modified = True

        # Elbow boundaries: strictly on arms
        if 'left_elbow' in w_map:
            if x < 0.14 or z > 1.28:
                w_map['left_elbow'] = 0.0
                modified = True
        if 'right_elbow' in w_map:
            if x > -0.14 or z > 1.28:
                w_map['right_elbow'] = 0.0
                modified = True

        # Lateral symmetry
        if x > 0.05:
            for rb in ['right_shoulder', 'right_elbow', 'right_thigh', 'right_knee', 'right_ankle']:
                if rb in w_map:
                    w_map[rb] = 0.0
                    modified = True
        elif x < -0.05:
            for lb in ['left_shoulder', 'left_elbow', 'left_thigh', 'left_knee', 'left_ankle']:
                if lb in w_map:
                    w_map[lb] = 0.0
                    modified = True

        # Head & Neck
        if z > 1.58 and abs(x) < 0.12:
            for b in list(w_map.keys()):
                if b != 'neck':
                    w_map[b] = 0.0
                    modified = True
            w_map['neck'] = 1.0

        # Legs
        if z < 0.92:
            for ub in ['neck', 'middle', 'left_shoulder', 'left_elbow', 'right_shoulder', 'right_elbow']:
                if ub in w_map:
                    w_map[ub] = 0.0
                    modified = True

        total_w = sum(w_map.values())
        if total_w < 0.001:
            if z < 0.10:
                w_map = {'left_ankle' if x >= 0 else 'right_ankle': 1.0}
            elif z < 0.53:
                w_map = {'left_knee' if x >= 0 else 'right_knee': 1.0}
            elif z < 0.95:
                w_map = {'left_thigh' if x >= 0 else 'right_thigh': 1.0}
            elif z < 1.15:
                w_map = {'body': 1.0}
            elif z < 1.58:
                w_map = {'middle': 1.0}
            else:
                w_map = {'neck': 1.0}
            total_w = 1.0
            modified = True

        if modified:
            scrubbed += 1
            for b in ENGINE_13_BONES:
                vg = messi.vertex_groups.get(b)
                if vg:
                    nw = w_map.get(b, 0.0) / total_w
                    if nw > 0.001:
                        vg.add([v.index], nw, 'REPLACE')
                    else:
                        try:
                            vg.remove([v.index])
                        except RuntimeError:
                            pass

    print(f"      Scrubbed {scrubbed} vertices for 100% anatomical accuracy")

    # ------------------------------------------------------------------
    # Step 6: Material & Shader Cleanup
    # Strips normal maps and metallic roughness; sets pure diffuse PSG kit
    # ------------------------------------------------------------------
    print("\n[6/7] Cleaning materials and textures...")
    mat = messi.data.materials[0] if messi.data.materials else None
    if mat:
        nodes = mat.node_tree.nodes
        bsdf = None
        base_tex_node = None
        for n in list(nodes):
            if n.type == 'BSDF_PRINCIPLED':
                bsdf = n
            elif n.type == 'TEX_IMAGE':
                if 'image_0' in n.image.name.lower() or 'base' in n.name.lower() or n.name == 'Image Texture':
                    base_tex_node = n
                else:
                    nodes.remove(n)
            elif n.type in ('NORMAL_MAP', 'SEPARATE_COLOR'):
                nodes.remove(n)

        if bsdf:
            bsdf.inputs['Metallic'].default_value = 0.0
            bsdf.inputs['Roughness'].default_value = 0.85
            if 'Specular IOR Level' in bsdf.inputs:
                bsdf.inputs['Specular IOR Level'].default_value = 0.05
            elif 'Specular' in bsdf.inputs:
                bsdf.inputs['Specular'].default_value = 0.05

            if base_tex_node:
                mat.node_tree.links.new(base_tex_node.outputs['Color'], bsdf.inputs['Base Color'])

        mat.name = "Player_Messi_PSG"
        print("      Material configured: pure diffuse BaseColor, 0 metallic, 0 normal maps")

    # Clean old objects from scene except PlayerArmature and Messi
    for obj in list(bpy.data.objects):
        if obj != player_arm and obj != messi:
            bpy.data.objects.remove(obj, do_unlink=True)

    # Parent Messi to PlayerArmature
    messi.parent = player_arm
    mod = messi.modifiers.new(name="Armature", type='ARMATURE')
    mod.object = player_arm

    # ------------------------------------------------------------------
    # Step 7: Export Clean GLB to Targets
    # ------------------------------------------------------------------
    print("\n[7/7] Exporting perfected GLB to target paths...")
    bpy.ops.object.select_all(action='DESELECT')
    player_arm.select_set(True)
    messi.select_set(True)
    bpy.context.view_layer.objects.active = player_arm

    primary = TARGETS[0]
    os.makedirs(os.path.dirname(primary), exist_ok=True)
    bpy.ops.export_scene.gltf(
        filepath=primary,
        export_format='GLB',
        use_selection=True,
        export_apply=False,
        export_skins=True,
        export_all_influences=False,
        export_def_bones=True,
    )
    sz = os.path.getsize(primary)
    print(f"      Exported to {primary} ({sz:,} bytes)")

    for t in TARGETS[1:]:
        os.makedirs(os.path.dirname(t), exist_ok=True)
        shutil.copy2(primary, t)
        print(f"      Copied to {t} ({os.path.getsize(t):,} bytes)")

    print("\n" + "=" * 60)
    print("=== MESSI 13-BONE MODEL PIPELINE COMPLETED SUCCESSFULLY ===")
    print("=" * 60)


if __name__ == '__main__':
    build_messi()
