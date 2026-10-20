import bpy

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=r"tools/workbench/yamal_clean_base.glb")

for obj in bpy.data.objects:
    if obj.type == 'MESH':
        f_vgs = [vg.name for vg in obj.vertex_groups if any(k in vg.name for k in ['Thumb', 'Index', 'Middle', 'Ring', 'Pinky'])]
        print(f"Mesh {obj.name} has {len(f_vgs)} finger vertex groups")
