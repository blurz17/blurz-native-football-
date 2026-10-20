import bpy
import os

print("=== STEP 7: GENERATING CLEAN DUAL LODs WITH BLENDER ===")

def generate_lod(source_glb, output_glb, target_tris, lod_name):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=source_glb)
    
    meshes = [o for o in bpy.data.objects if o.type == 'MESH']
    initial_tris = sum(len(o.data.polygons) for o in meshes)
    print(f"\n[{lod_name}] Initial triangles: {initial_tris:,} across {len(meshes)} meshes")
    
    # Calculate ratio
    ratio = min(1.0, target_tris / initial_tris)
    print(f"[{lod_name}] Target: {target_tris:,} tris (Global decimate ratio: {ratio:.3f})")
    
    for obj in meshes:
        # For eyeballs, keep decimation gentler to avoid silhouette collapse
        obj_ratio = ratio
        if "Eye" in obj.name:
            obj_ratio = max(0.4, ratio)
        elif "Head" in obj.name:
            # Preserve more detail on face for likeness
            obj_ratio = min(1.0, ratio * 1.15)
        else:
            # Body/clothes can take more decimation
            obj_ratio = ratio * 0.92
            
        obj_ratio = min(1.0, max(0.1, obj_ratio))
        
        # Apply decimate
        mod = obj.modifiers.new(name="Decimate", type='DECIMATE')
        mod.ratio = obj_ratio
        
        # Ensure it is placed before armature modifier if any, or apply directly
        bpy.context.view_layer.objects.active = obj
        bpy.ops.object.modifier_apply(modifier="Decimate")
        
    final_tris = sum(len(o.data.polygons) for o in meshes)
    final_verts = sum(len(o.data.vertices) for o in meshes)
    print(f"[{lod_name}] Result: {final_verts:,} verts | {final_tris:,} triangles")
    
    bpy.ops.export_scene.gltf(
        filepath=output_glb,
        export_format='GLB',
        use_selection=False,
        export_apply=False,
        export_skins=True,
        export_morph=False
    )
    print(f"[{lod_name}] Exported to: {output_glb} ({os.path.getsize(output_glb):,} bytes)")

source = r"tools/workbench/universal_humanoid_base_34b.glb"

# Generate LOD0 (~11,500 tris)
lod0_path = r"tools/workbench/universal_player_modular_lod0.glb"
generate_lod(source, lod0_path, target_tris=11500, lod_name="LOD0 High-Fidelity")

# Generate LOD1 (~5,200 tris)
lod1_path = r"tools/workbench/universal_player_modular_lod1.glb"
generate_lod(source, lod1_path, target_tris=5200, lod_name="LOD1 Gameplay")
