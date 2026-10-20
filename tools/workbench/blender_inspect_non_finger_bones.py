import bpy

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=r"tools/workbench/yamal_clean_base.glb")

arm_obj = bpy.data.objects.get("Object_4")
if arm_obj and arm_obj.type == 'ARMATURE':
    arm = arm_obj.data
    non_fingers = [b.name for b in arm.bones if not any(k in b.name for k in ['Thumb', 'Index', 'Middle', 'Ring', 'Pinky'])]
    print(f"Total non-finger bones: {len(non_fingers)}")
    for i, b in enumerate(non_fingers):
        p = arm.bones[b].parent.name if arm.bones[b].parent else 'None'
        print(f"  {i:02d}: {b:25} (parent: {p})")
