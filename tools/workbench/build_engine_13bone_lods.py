import bpy
import os

print("=== GENERATING 13-BONE ENGINE DUAL LODS ===")

base_dir = r"E:\MY-PROJECTS\android native football\tools\workbench"
src_13b = os.path.join(base_dir, "universal_player_engine_13b.glb")

def generate_lod(source_glb, output_glb, target_tris, lod_name):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=source_glb)
    
    meshes = [o for o in bpy.data.objects if o.type == 'MESH']
    initial_tris = sum(len(o.data.polygons) for o in meshes)
    ratio = min(1.0, target_tris / initial_tris)
    
    for obj in meshes:
        obj_ratio = ratio
        if "Eye" in obj.name:
            obj_ratio = max(0.4, ratio)
        elif "Head" in obj.name:
            obj_ratio = min(1.0, ratio * 1.15)
        else:
            obj_ratio = ratio * 0.92
        obj_ratio = min(1.0, max(0.1, obj_ratio))
        
        mod = obj.modifiers.new(name="Decimate", type='DECIMATE')
        mod.ratio = obj_ratio
        bpy.context.view_layer.objects.active = obj
        bpy.ops.object.modifier_apply(modifier="Decimate")
        
    final_tris = sum(len(o.data.polygons) for o in meshes)
    final_verts = sum(len(o.data.vertices) for o in meshes)
    print(f"[{lod_name}] Result: {final_verts:,} verts | {final_tris:,} triangles (13 bones)")
    
    bpy.ops.export_scene.gltf(
        filepath=output_glb,
        export_format='GLB',
        use_selection=False,
        export_apply=False,
        export_skins=True
    )
    print(f"[{lod_name}] Exported: {output_glb} ({os.path.getsize(output_glb):,} bytes)")

lod0_13b = os.path.join(base_dir, "universal_player_engine_13b_lod0.glb")
generate_lod(src_13b, lod0_13b, 11500, "13-Bone Engine LOD0")

lod1_13b = os.path.join(base_dir, "universal_player_engine_13b_lod1.glb")
generate_lod(src_13b, lod1_13b, 5200, "13-Bone Engine LOD1")
