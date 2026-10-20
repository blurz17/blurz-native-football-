import bpy

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=r"tools/workbench/yamal_clean_base.glb")

print("--- OBJECTS IN SCENE ---")
for obj in bpy.data.objects:
    parent_name = obj.parent.name if obj.parent else "None"
    print(f"{obj.type:10} | {obj.name:30} | parent: {parent_name}")
    if obj.type == 'MESH':
        mats = [m.name for m in obj.data.materials if m]
        print(f"           verts: {len(obj.data.vertices):,}, edges: {len(obj.data.edges):,}, polys: {len(obj.data.polygons):,}")
        print(f"           materials: {mats}")

if bpy.data.armatures:
    arm = bpy.data.armatures[0]
    print("--- ARMATURE ---")
    print(f"Armature: {arm.name}, total bones: {len(arm.bones)}")
    for b in arm.bones:
        if "Hand" in b.name or "Finger" in b.name or "Thumb" in b.name:
            print(f"  Hand/Finger bone: {b.name} (parent: {b.parent.name if b.parent else 'None'})")
