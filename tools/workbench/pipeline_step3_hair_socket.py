import bpy
import mathutils

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=r"tools/workbench/yamal_step1_cleaned.glb")

# Find the head bone
arm_obj = [o for o in bpy.data.objects if o.type == 'ARMATURE'][0]
head_bone = arm_obj.data.bones.get("Head_08")
print(f"Head bone: {head_bone.name}, head pos: {head_bone.head_local}, tail pos: {head_bone.tail_local}")

# Import hair model
bpy.ops.import_scene.gltf(filepath=r"tools/workbench/hairstyles/hair_modern_buzz_fade.glb")

# The imported hair mesh
hair_objs = [o for o in bpy.context.selected_objects if o.type == 'MESH']
print(f"Imported hair objects: {[o.name for o in hair_objs]}")

for h_obj in hair_objs:
    h_obj.name = "Hair_Buzz_Fade"
    # Inspect bounds
    min_z = min(v.co.z for v in h_obj.data.vertices)
    max_z = max(v.co.z for v in h_obj.data.vertices)
    print(f"Hair Z bounds: [{min_z:.3f}, {max_z:.3f}]")

head_obj = [o for o in bpy.data.objects if "Head" in o.name and o.type == 'MESH'][0]
head_max_z = max(v.co.z for v in head_obj.data.vertices)
head_min_z = min(v.co.z for v in head_obj.data.vertices)
print(f"Head Mesh Z bounds: [{head_min_z:.3f}, {head_max_z:.3f}]")
