import bpy
import os

print("=== STEP 1: CLEANUP & RETOPO ===")
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=r"tools/workbench/yamal_clean_base.glb")

# 1. Objects to permanently delete
del_names = ["Icosphere", "Object_8", "Object_16", "Object_18"]
for name in del_names:
    obj = bpy.data.objects.get(name)
    if obj:
        print(f"Deleting unused object: {obj.name}")
        bpy.data.objects.remove(obj, do_unlink=True)

# Also purge unused meshes
for m in list(bpy.data.meshes):
    if m.users == 0:
        print(f"Purging unused mesh data: {m.name}")
        bpy.data.meshes.remove(m)

# 2. Rename retained meshes cleanly
name_map = {
    "Object_6": "Mesh_Skin_Body",
    "Object_10": "Mesh_Head_Face",
    "Object_12": "Mesh_LeftEye",
    "Object_14": "Mesh_RightEye",
    "Object_22": "Mesh_Shirt_Jersey",
    "Object_19": "Mesh_Shorts",
    "Object_21": "Mesh_Boots_Socks"
}

retained_meshes = []
for old_name, new_name in name_map.items():
    obj = bpy.data.objects.get(old_name)
    if obj:
        obj.name = new_name
        retained_meshes.append(obj)
        print(f"Renamed: {old_name} -> {new_name} ({len(obj.data.vertices)} verts, {len(obj.data.polygons)} polys)")

# 3. Clean up empty hierarchy nodes
# Reparent armature directly to scene root
arm_obj = bpy.data.objects.get("Object_4")
if arm_obj:
    arm_obj.name = "Armature_Yamal"
    arm_obj.parent = None
    arm_obj.matrix_world.identity()

# Parent meshes cleanly to Armature
for obj in retained_meshes:
    obj.parent = arm_obj

# Remove all remaining Empty objects
for obj in list(bpy.data.objects):
    if obj.type == 'EMPTY':
        print(f"Removing empty node: {obj.name}")
        bpy.data.objects.remove(obj, do_unlink=True)

# 4. Check topology metrics on each retained mesh
print("\n--- Mesh Topology Metrics ---")
total_verts = 0
total_polys = 0
for obj in retained_meshes:
    v_cnt = len(obj.data.vertices)
    p_cnt = len(obj.data.polygons)
    total_verts += v_cnt
    total_polys += p_cnt
    print(f"  {obj.name:20}: {v_cnt:6,} verts | {p_cnt:6,} polys | {len(obj.data.materials)} mats")

print(f"\nTOTAL Cleaned Geometry: {total_verts:,} verts | {total_polys:,} polys")

# Export cleaned Step 1 model
out_path = r"tools/workbench/yamal_step1_cleaned.glb"
bpy.ops.export_scene.gltf(
    filepath=out_path,
    export_format='GLB',
    use_selection=False,
    export_apply=False,
    export_skins=True,
    export_morph=False
)
print(f"Successfully exported Step 1 Cleaned Base to: {out_path} ({os.path.getsize(out_path):,} bytes)")
