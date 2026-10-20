import bpy

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=r"tools/workbench/yamal_step1_cleaned.glb")

meshes = [o for o in bpy.data.objects if o.type == 'MESH']
min_z = min(v.co.z for o in meshes for v in o.data.vertices)
max_z = max(v.co.z for o in meshes for v in o.data.vertices)
total_height = max_z - min_z

head = bpy.data.objects.get("Mesh_Head_Face")
head_min_z = min(v.co.z for v in head.data.vertices)
head_max_z = max(v.co.z for v in head.data.vertices)
head_height = head_max_z - head_min_z

# Chin to crown (actual visual head, since Mesh_Head_Face base extends down to collar)
# The chin is typically around z = 1.45m in a 1.78m figure
chin_approx_z = min(v.co.z for v in head.data.vertices if abs(v.co.x) < 0.05 and v.co.y > 0.02)
visual_head_height = head_max_z - chin_approx_z

print(f"Total Character Height: {total_height:.3f} m")
print(f"Full Mesh_Head_Face bbox height (includes neck/collar seam): {head_height:.3f} m")
print(f"Visual Cranium to Chin Height: {visual_head_height:.3f} m")
print(f"Effective Visual Proportion (Height / Visual Head): {total_height / visual_head_height:.2f} heads (Natural Athletic Target: 7.2 - 7.5)")
