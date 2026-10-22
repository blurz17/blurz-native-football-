"""
build_messi_engine.py — Convert boneless Meshy.ai Messi PSG GLB into
engine-compatible 13-bone rigged fullbody.glb

Uses anatomical zone-based weight painting for precise bone assignment
since the model is a sealed single mesh (Bone Heat fails on watertight meshes).

Input:  E:\Downlaod\Meshy_AI_Paris_Saint_Germain_S_1022190900_texture.glb
Output: assets\media\objects\players\models\fullbody.glb  (+ copies)

Run:  D:\blender\blender.exe -b -P tools\build_messi_engine.py
"""

import bpy
import math
import os
import shutil
from mathutils import Vector, Matrix

# ===========================================================================
# CONFIG
# ===========================================================================
SRC_GLB = r'E:\Downlaod\Meshy_AI_Paris_Saint_Germain_S_1022190900_texture.glb'
TARGET_HEIGHT = 1.92  # engine defaultPlayerHeight

TARGETS = [
    r"assets\media\objects\players\models\fullbody.glb",
    r"platform\android\app\src\main\assets\media\objects\players\models\fullbody.glb",
    r"tools\workbench\universal_player_engine_13b.glb",
]

# Exact engine joint positions from player.object
ENGINE_HEADS = {
    'body':           Vector((0.000,  0.000, 0.960)),
    'middle':         Vector((0.000,  0.000, 1.110)),
    'neck':           Vector((0.000, -0.030, 1.610)),
    'left_shoulder':  Vector((0.160, -0.010, 1.590)),
    'left_elbow':     Vector((0.150, -0.010, 1.260)),
    'right_shoulder': Vector((-0.160, -0.010, 1.590)),
    'right_elbow':    Vector((-0.150, -0.010, 1.260)),
    'left_thigh':     Vector((0.087,  0.000, 0.950)),
    'left_knee':      Vector((0.087,  0.000, 0.530)),
    'left_ankle':     Vector((0.087, -0.040, 0.090)),
    'right_thigh':    Vector((-0.087,  0.000, 0.950)),
    'right_knee':     Vector((-0.087,  0.000, 0.530)),
    'right_ankle':    Vector((-0.087, -0.040, 0.090)),
}

ENGINE_TAILS = {
    'body':           ENGINE_HEADS['middle'],
    'middle':         ENGINE_HEADS['neck'],
    'neck':           Vector((0.000, -0.030, 1.880)),
    'left_shoulder':  ENGINE_HEADS['left_elbow'],
    'left_elbow':     Vector((0.150, -0.010, 0.950)),
    'right_shoulder': ENGINE_HEADS['right_elbow'],
    'right_elbow':    Vector((-0.150, -0.010, 0.950)),
    'left_thigh':     ENGINE_HEADS['left_knee'],
    'left_knee':      ENGINE_HEADS['left_ankle'],
    'left_ankle':     Vector((0.087, -0.180, 0.020)),
    'right_thigh':    ENGINE_HEADS['right_knee'],
    'right_knee':     ENGINE_HEADS['right_ankle'],
    'right_ankle':    Vector((-0.087, -0.180, 0.020)),
}

PARENTS = {
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

ENGINE_13_BONES = list(ENGINE_HEADS.keys())


def anatomical_zone_weights(obj):
    """
    Assign vertex weights based on anatomical zones defined by height (Z),
    lateral position (X), and arm reach. This produces much better deformation
    than simple nearest-bone proximity, especially for the torso/shoulder
    transition and the hip/thigh boundary.
    
    Coordinate reference (after grounding at Z=0, height=1.92m):
      Feet:        Z = 0.00 - 0.09
      Shin:        Z = 0.09 - 0.53
      Thigh:       Z = 0.53 - 0.95
      Hips/Body:   Z = 0.95 - 1.11
      Torso/Mid:   Z = 1.11 - 1.59
      Neck/Head:   Z = 1.59 - 1.92
      
    Lateral (X > 0 = left, X < 0 = right):
      Arms extend from shoulder joint outward beyond |X| > ~0.20
    """
    mesh = obj.data
    
    # Ensure all 13 vertex groups exist
    for bname in ENGINE_13_BONES:
        if not obj.vertex_groups.get(bname):
            obj.vertex_groups.new(name=bname)

    # Anatomical zone thresholds
    Z_ANKLE = 0.09
    Z_KNEE  = 0.53
    Z_THIGH_TOP = 0.95
    Z_BODY_TOP  = 1.11
    Z_SHOULDER  = 1.59
    Z_NECK_TOP  = 1.92

    # Arm lateral threshold: vertices beyond this X are in arm zone
    ARM_X_THRESHOLD = 0.18   # ~shoulder width/2
    # Inner shoulder blend zone
    ARM_X_INNER = 0.12

    # Elbow height boundary
    Z_ELBOW = 1.26

    for v in mesh.vertices:
        x, y, z = v.co.x, v.co.y, v.co.z
        ax = abs(x)
        is_left = x > 0

        weights = {}

        # ---- ARM ZONE ----
        # If vertex is laterally far enough AND above hip level
        if ax > ARM_X_INNER and z > Z_THIGH_TOP:
            # How much in the arm zone (0..1 blend from inner to threshold)
            arm_factor = min(1.0, max(0.0, (ax - ARM_X_INNER) / (ARM_X_THRESHOLD - ARM_X_INNER)))

            if arm_factor > 0 and z > Z_BODY_TOP:
                if is_left:
                    if z >= Z_ELBOW:
                        sh_w = arm_factor
                        # Blend shoulder/elbow near the boundary
                        if z < Z_SHOULDER:
                            blend = (z - Z_ELBOW) / (Z_SHOULDER - Z_ELBOW)
                            weights['left_shoulder'] = sh_w * blend
                            weights['left_elbow'] = sh_w * (1 - blend)
                        else:
                            weights['left_shoulder'] = sh_w
                    else:
                        weights['left_elbow'] = arm_factor
                else:
                    if z >= Z_ELBOW:
                        sh_w = arm_factor
                        if z < Z_SHOULDER:
                            blend = (z - Z_ELBOW) / (Z_SHOULDER - Z_ELBOW)
                            weights['right_shoulder'] = sh_w * blend
                            weights['right_elbow'] = sh_w * (1 - blend)
                        else:
                            weights['right_shoulder'] = sh_w
                    else:
                        weights['right_elbow'] = arm_factor

                # The remaining weight goes to torso bone
                remaining = 1.0 - arm_factor
                if remaining > 0.001:
                    if z >= Z_SHOULDER:
                        weights['neck'] = weights.get('neck', 0) + remaining
                    else:
                        weights['middle'] = weights.get('middle', 0) + remaining
            else:
                arm_factor = 0  # Below torso, not arm

        # ---- BODY ZONE (non-arm or remaining weight) ----
        if not weights:
            # Pure body zone assignment
            if z <= Z_ANKLE:
                # Feet/ankle zone
                if is_left or ax < 0.03:
                    if x >= -0.03:
                        weights['left_ankle'] = 1.0
                    else:
                        weights['right_ankle'] = 1.0
                else:
                    weights['right_ankle'] = 1.0

            elif z <= Z_KNEE:
                # Shin zone - blend between ankle and knee
                t = (z - Z_ANKLE) / (Z_KNEE - Z_ANKLE)
                if x >= 0:
                    weights['left_knee'] = t
                    weights['left_ankle'] = 1.0 - t
                else:
                    weights['right_knee'] = t
                    weights['right_ankle'] = 1.0 - t

            elif z <= Z_THIGH_TOP:
                # Thigh zone - blend between knee and thigh
                t = (z - Z_KNEE) / (Z_THIGH_TOP - Z_KNEE)
                if x >= 0:
                    weights['left_thigh'] = t
                    weights['left_knee'] = 1.0 - t
                else:
                    weights['right_thigh'] = t
                    weights['right_knee'] = 1.0 - t

            elif z <= Z_BODY_TOP:
                # Hip/Body zone
                # Blend between thigh and body based on X-position
                if ax < 0.04:
                    # Center: pure body
                    weights['body'] = 1.0
                else:
                    t = (z - Z_THIGH_TOP) / (Z_BODY_TOP - Z_THIGH_TOP)
                    if x >= 0:
                        weights['body'] = t
                        weights['left_thigh'] = 1.0 - t
                    else:
                        weights['body'] = t
                        weights['right_thigh'] = 1.0 - t

            elif z <= Z_SHOULDER:
                # Torso / middle zone
                t = (z - Z_BODY_TOP) / (Z_SHOULDER - Z_BODY_TOP)
                weights['middle'] = 1.0

            else:
                # Neck & Head
                weights['neck'] = 1.0

        # ---- SMOOTH BOUNDARY BLENDING ----
        # At zone boundaries, create smooth transitions to avoid hard creases
        BLEND_RANGE = 0.04  # 4cm blend zone

        # Body/Middle boundary smoothing
        if abs(z - Z_BODY_TOP) < BLEND_RANGE and 'body' not in weights and 'middle' not in weights:
            pass  # Already handled above

        # Thigh/Body crotch smoothing
        if abs(z - Z_THIGH_TOP) < BLEND_RANGE:
            t = 0.5 + 0.5 * (z - Z_THIGH_TOP) / BLEND_RANGE
            t = max(0, min(1, t))
            if x >= 0:
                cur_thigh = weights.get('left_thigh', 0)
                cur_body = weights.get('body', 0)
                total = cur_thigh + cur_body
                if total > 0:
                    weights['left_thigh'] = total * (1 - t)
                    weights['body'] = total * t
            else:
                cur_thigh = weights.get('right_thigh', 0)
                cur_body = weights.get('body', 0)
                total = cur_thigh + cur_body
                if total > 0:
                    weights['right_thigh'] = total * (1 - t)
                    weights['body'] = total * t

        # ---- NORMALIZE ----
        total_w = sum(weights.values())
        if total_w < 0.001:
            # Safety: assign to nearest bone by height
            weights = {'body': 1.0}
            total_w = 1.0

        # Assign to vertex groups
        for bname, w in weights.items():
            if w > 0.001:
                nw = w / total_w
                vg = obj.vertex_groups.get(bname)
                if vg:
                    vg.add([v.index], nw, 'REPLACE')

    # Stats
    total_verts = len(mesh.vertices)
    weighted = 0
    for v in mesh.vertices:
        tw = sum(g.weight for g in v.groups)
        if tw > 0.001:
            weighted += 1
    pct = 100 * weighted / total_verts
    print(f"      Zone weighting: {weighted}/{total_verts} verts ({pct:.1f}%)")
    
    # Per-bone stats
    for bname in ENGINE_13_BONES:
        vg = obj.vertex_groups.get(bname)
        if vg:
            count = 0
            for v in mesh.vertices:
                try:
                    if vg.weight(v.index) > 0.001:
                        count += 1
                except RuntimeError:
                    pass
            print(f"        {bname:20s}: {count:6d} verts")


def build_messi():
    """Main build pipeline."""
    # ------------------------------------------------------------------
    # 1. Load the Meshy GLB
    # ------------------------------------------------------------------
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=SRC_GLB)
    print(f"[1/7] Loaded {SRC_GLB}")

    mesh_objs = [o for o in bpy.data.objects if o.type == 'MESH']
    print(f"      Meshes: {[m.name for m in mesh_objs]}")

    obj = mesh_objs[0]
    mesh = obj.data

    # ------------------------------------------------------------------
    # 2. Scale uniformly to match engine height (1.92m)
    # ------------------------------------------------------------------
    zs = [v.co.z for v in mesh.vertices]
    raw_height = max(zs) - min(zs)
    scale = TARGET_HEIGHT / raw_height
    obj.scale = (scale, scale, scale)
    bpy.context.view_layer.objects.active = obj
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    print(f"[2/7] Scaled by {scale:.6f}x ({raw_height:.4f}m -> {TARGET_HEIGHT}m)")

    # ------------------------------------------------------------------
    # 3. Ground feet at Z=0, center X at 0
    # ------------------------------------------------------------------
    zs = [v.co.z for v in mesh.vertices]
    z_offset = -min(zs)
    for v in mesh.vertices:
        v.co.z += z_offset

    xs = [v.co.x for v in mesh.vertices]
    x_center = (min(xs) + max(xs)) / 2
    if abs(x_center) > 0.001:
        for v in mesh.vertices:
            v.co.x -= x_center

    mesh.update()
    zs = [v.co.z for v in mesh.vertices]
    xs = [v.co.x for v in mesh.vertices]
    print(f"[3/7] Grounded: Z=[{min(zs):.4f}, {max(zs):.4f}], X=[{min(xs):.4f}, {max(xs):.4f}]")

    # ------------------------------------------------------------------
    # 4. Anatomical zone-based vertex weight assignment
    # ------------------------------------------------------------------
    print("[4/7] Assigning anatomical zone-based vertex weights...")
    anatomical_zone_weights(obj)

    # ------------------------------------------------------------------
    # 5. Create the canonical 13-bone PlayerArmature
    # ------------------------------------------------------------------
    bpy.ops.object.select_all(action='DESELECT')
    bpy.ops.object.armature_add(location=(0, 0, 0))
    arm_obj = bpy.context.active_object
    arm_obj.name = "PlayerArmature"

    bpy.ops.object.mode_set(mode='EDIT')
    for b in list(arm_obj.data.edit_bones):
        arm_obj.data.edit_bones.remove(b)

    ebones = {}
    for bname in ENGINE_13_BONES:
        eb = arm_obj.data.edit_bones.new(bname)
        eb.head = ENGINE_HEADS[bname]
        eb.tail = ENGINE_TAILS[bname]
        ebones[bname] = eb

    for child, parent in PARENTS.items():
        ebones[child].parent = ebones[parent]

    bpy.ops.object.mode_set(mode='OBJECT')
    print("[5/7] Created canonical 13-bone PlayerArmature")

    # ------------------------------------------------------------------
    # 6. Parent mesh to armature (keep existing weights, no auto)
    # ------------------------------------------------------------------
    obj.parent = arm_obj
    mod = obj.modifiers.new(name="Armature", type='ARMATURE')
    mod.object = arm_obj
    print("[6/7] Parented mesh to armature with existing zone weights")

    # ------------------------------------------------------------------
    # 7. Export to all target GLB locations
    # ------------------------------------------------------------------
    bpy.ops.object.select_all(action='DESELECT')
    arm_obj.select_set(True)
    obj.select_set(True)
    bpy.context.view_layer.objects.active = arm_obj

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
    print(f"[7/7] Exported to {primary} ({sz:,} bytes)")

    for t in TARGETS[1:]:
        os.makedirs(os.path.dirname(t), exist_ok=True)
        shutil.copy2(primary, t)
        print(f"      Copied to {t} ({os.path.getsize(t):,} bytes)")

    print("=== MESSI PSG ENGINE 13-BONE MODEL BUILT SUCCESSFULLY ===")


if __name__ == '__main__':
    build_messi()
