import bpy

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=r"tools/workbench/universal_humanoid_base_34b.glb")

shirt = bpy.data.objects.get("Mesh_Shirt_Jersey")
print(f"Original Shirt: {len(shirt.data.vertices)} verts, {len(shirt.data.polygons)} polys")

mod = shirt.modifiers.new(name="Decimate", type='DECIMATE')
mod.ratio = 0.35  # decimate to 35%
bpy.context.view_layer.objects.active = shirt
bpy.ops.object.modifier_apply(modifier="Decimate")

print(f"Decimated Shirt: {len(shirt.data.vertices)} verts, {len(shirt.data.polygons)} polys")
print(f"UV layers count: {len(shirt.data.uv_layers)}")
print(f"Vertex groups count: {len(shirt.vertex_groups)}")
