import trimesh
import os

def generate_lods():
    src_glb = r'tools/workbench/yamal_stage1_clean.glb'
    scene = trimesh.load(src_glb)
    
    # Target face counts for LOD0 and LOD1
    # Original total faces in clean stage 1: 37,051
    # Geometries:
    # 'AvatarBody_AvatarBody_0': 6654 faces
    # '1' (Head): 14212 faces
    # 'AvatarLeftEyeball_AvatarLeftEyeball_0': 791 faces
    # 'AvatarRightEyeball_AvatarRightEyeball_0': 791 faces
    # 'outfit_bottom_outfit_bottom_0': 3557 faces
    # 'outfit_shoes_outfit_shoes_0': 4246 faces
    # 'outfit_top_outfit_top_0': 6800 faces
    
    # LOD0 Targets (Total ~10,500 tris):
    lod0_targets = {
        '1': 3800,                        # Head
        'outfit_top_outfit_top_0': 2500,  # Jersey
        'outfit_bottom_outfit_bottom_0': 1400, # Shorts
        'outfit_shoes_outfit_shoes_0': 1400,   # Boots
        'AvatarBody_AvatarBody_0': 1200,       # Limbs
        'AvatarLeftEyeball_AvatarLeftEyeball_0': 150,
        'AvatarRightEyeball_AvatarRightEyeball_0': 150
    }
    
    # LOD1 Targets (Total ~4,800 tris):
    lod1_targets = {
        '1': 1600,                        # Head
        'outfit_top_outfit_top_0': 1200,  # Jersey
        'outfit_bottom_outfit_bottom_0': 700, # Shorts
        'outfit_shoes_outfit_shoes_0': 700,   # Boots
        'AvatarBody_AvatarBody_0': 600,       # Limbs
        'AvatarLeftEyeball_AvatarLeftEyeball_0': 60,
        'AvatarRightEyeball_AvatarRightEyeball_0': 60
    }
    
    print("=== GENERATING LOD0 (High Fidelity: ~10k Tris) ===")
    scene_lod0 = trimesh.Scene()
    total_l0_faces = 0
    total_l0_verts = 0
    for name, geom in scene.geometry.items():
        orig_f = len(geom.faces)
        target_f = lod0_targets.get(name, orig_f)
        if target_f < orig_f:
            pct = 1.0 - (target_f / orig_f)
            simp = geom.simplify_quadric_decimation(percent=pct)
        else:
            simp = geom.copy()
        scene_lod0.add_geometry(simp, geom_name=name)
        total_l0_faces += len(simp.faces)
        total_l0_verts += len(simp.vertices)
        print(f"  {name}: {orig_f} -> {len(simp.faces)} faces ({len(simp.vertices)} verts)")
        
    print(f"LOD0 Total: {total_l0_faces} faces, {total_l0_verts} vertices")
    out_lod0 = r'tools/workbench/universal_player_lod0.glb'
    scene_lod0.export(out_lod0)
    print(f"Saved LOD0 to {out_lod0} ({os.path.getsize(out_lod0)} bytes)")
    
    print("\n=== GENERATING LOD1 (Performance / 120Hz: ~4.8k Tris) ===")
    scene_lod1 = trimesh.Scene()
    total_l1_faces = 0
    total_l1_verts = 0
    for name, geom in scene.geometry.items():
        orig_f = len(geom.faces)
        target_f = lod1_targets.get(name, orig_f)
        if target_f < orig_f:
            pct = 1.0 - (target_f / orig_f)
            simp = geom.simplify_quadric_decimation(percent=pct)
        else:
            simp = geom.copy()
        scene_lod1.add_geometry(simp, geom_name=name)
        total_l1_faces += len(simp.faces)
        total_l1_verts += len(simp.vertices)
        print(f"  {name}: {orig_f} -> {len(simp.faces)} faces ({len(simp.vertices)} verts)")
        
    print(f"LOD1 Total: {total_l1_faces} faces, {total_l1_verts} vertices")
    out_lod1 = r'tools/workbench/universal_player_lod1.glb'
    scene_lod1.export(out_lod1)
    print(f"Saved LOD1 to {out_lod1} ({os.path.getsize(out_lod1)} bytes)")

if __name__ == '__main__':
    generate_lods()
