import bpy

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=r"tools/workbench/yamal_step1_cleaned.glb")

meshes = [o for o in bpy.data.objects if o.type == 'MESH']
for o in meshes:
    uv_layers = [uv.name for uv in o.data.uv_layers]
    print(f"Mesh '{o.name:20}': UV layers: {uv_layers}")
    if o.data.uv_layers:
        uv_layer = o.data.uv_layers.active.data
        min_u = min(loop.uv.x for loop in uv_layer)
        max_u = max(loop.uv.x for loop in uv_layer)
        min_v = min(loop.uv.y for loop in uv_layer)
        max_v = max(loop.uv.y for loop in uv_layer)
        print(f"     UV range: U [{min_u:.3f}, {max_u:.3f}], V [{min_v:.3f}, {max_v:.3f}]")
