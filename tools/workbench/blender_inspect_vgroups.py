import bpy

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=r"tools/workbench/yamal_clean_base.glb")

body = bpy.data.objects.get("Object_6")
if body:
    print(f"Object_6 vertex groups: {len(body.vertex_groups)}")
    for vg in body.vertex_groups:
        if "Thumb" in vg.name or "Index" in vg.name or "Middle" in vg.name or "Ring" in vg.name or "Pinky" in vg.name or "Hand" in vg.name:
            print(f"  {vg.name}")
