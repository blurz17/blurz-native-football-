import bpy
import os
import shutil
import math

def export_ball():
    # Clear scene
    bpy.ops.wm.read_factory_settings(use_empty=True)

    # Ball radius = 0.11m (size 5 match ball)
    # Segments = 48, ring_count = 24 gives a very smooth, round sphere (1,104 verts, 1,152 tris)
    bpy.ops.mesh.primitive_uv_sphere_add(
        segments=48,
        ring_count=24,
        radius=0.11,
        location=(0, 0, 0)
    )
    ball = bpy.context.active_object
    ball.name = "ball"
    ball.data.name = "ball_mesh"

    for poly in ball.data.polygons:
        poly.use_smooth = True

    # Material
    mat = bpy.data.materials.new(name="BallMaterial")
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    links = mat.node_tree.links
    nodes.clear()

    out_n = nodes.new('ShaderNodeOutputMaterial')
    bsdf_n = nodes.new('ShaderNodeBsdfPrincipled')
    links.new(bsdf_n.outputs['BSDF'], out_n.inputs['Surface'])
    bsdf_n.inputs['Roughness'].default_value = 0.40
    bsdf_n.inputs['Metallic'].default_value = 0.02

    tex_path = r"E:\MY-PROJECTS\android native football\scratch\al_rihla_vibrant.png"
    tex_img = bpy.data.images.load(tex_path)
    tex_n = nodes.new('ShaderNodeTexImage')
    tex_n.image = tex_img
    links.new(tex_n.outputs['Color'], bsdf_n.inputs['Base Color'])

    ball.data.materials.append(mat)

    # Export paths
    out_glb = r"E:\MY-PROJECTS\android native football\assets\media\objects\balls\generic.glb"
    bpy.ops.export_scene.gltf(
        filepath=out_glb,
        export_format='GLB',
        use_selection=True,
        export_apply=True
    )
    print(f"Exported generic.glb to {out_glb}")

    # Copy texture and GLB to android assets as well
    android_ball_dir = r"E:\MY-PROJECTS\android native football\platform\android\app\src\main\assets\media\objects\balls"
    assets_ball_dir = r"E:\MY-PROJECTS\android native football\assets\media\objects\balls"

    shutil.copy2(out_glb, os.path.join(android_ball_dir, "generic.glb"))
    shutil.copy2(tex_path, os.path.join(assets_ball_dir, "ball.png"))
    shutil.copy2(tex_path, os.path.join(android_ball_dir, "ball.png"))
    shutil.copy2(tex_path, os.path.join(assets_ball_dir, "ball.jpg"))
    shutil.copy2(tex_path, os.path.join(android_ball_dir, "ball.jpg"))

    print("Copied ball.png, ball.jpg, generic.glb to both assets/ and platform/android/!")

if __name__ == "__main__":
    export_ball()
