import trimesh
import numpy as np
import os
from PIL import Image
from scipy.spatial import cKDTree

def build_unified_atlas_models():
    diffuse_img_path = r'tools/workbench/texture_system/universal_base_diffuse.png'
    pil_tex = Image.open(diffuse_img_path)
    
    # Load original clean scene to get exact pristine UVs
    scene_orig = trimesh.load(r'tools/workbench/yamal_stage1_clean.glb')
    
    # Quadrant transform mapping:
    # outfit_top: (U*0.5, V*0.5 + 0.5)
    # 1 (Head): (U*0.5 + 0.5, V*0.5 + 0.5)
    # outfit_bottom: (U*0.5, V*0.5)
    # outfit_shoes: (U*0.5 + 0.5, V*0.5)
    quadrants = {
        'outfit_top_outfit_top_0': (0.0, 0.5),
        '1': (0.5, 0.5),
        'AvatarLeftEyeball_AvatarLeftEyeball_0': (0.75, 0.70),
        'AvatarRightEyeball_AvatarRightEyeball_0': (0.75, 0.70),
        'outfit_bottom_outfit_bottom_0': (0.0, 0.0),
        'outfit_shoes_outfit_shoes_0': (0.5, 0.0),
        'AvatarBody_AvatarBody_0': (0.0, 0.0)
    }
    
    scale_factors = {
        'outfit_top_outfit_top_0': (0.5, 0.5),
        '1': (0.5, 0.5),
        'AvatarLeftEyeball_AvatarLeftEyeball_0': (0.08, 0.08),
        'AvatarRightEyeball_AvatarRightEyeball_0': (0.08, 0.08),
        'outfit_bottom_outfit_bottom_0': (0.5, 0.5),
        'outfit_shoes_outfit_shoes_0': (0.5, 0.5),
        'AvatarBody_AvatarBody_0': (0.5, 0.5)
    }
    
    for lod_name in ['lod0', 'lod1']:
        glb_path = f'tools/workbench/universal_player_{lod_name}.glb'
        scene_lod = trimesh.load(glb_path)
        
        print(f"\n=== REMAPPING UVS & PACKING {lod_name.upper()} ===")
        
        combined_verts = []
        combined_faces = []
        combined_uvs = []
        vert_offset = 0
        
        for name, geom in scene_lod.geometry.items():
            verts = geom.vertices
            faces = geom.faces + vert_offset
            
            # Query KDTree from original un-decimated geometry
            orig_geom = scene_orig.geometry[name]
            tree = cKDTree(orig_geom.vertices)
            _, indices = tree.query(verts)
            raw_uvs = orig_geom.visual.uv[indices]
            
            u_offset, v_offset = quadrants.get(name, (0.0, 0.0))
            u_scale, v_scale = scale_factors.get(name, (0.5, 0.5))
            
            # Project into non-overlapping quadrant
            remapped_uvs = raw_uvs.copy()
            remapped_uvs[:, 0] = np.clip(raw_uvs[:, 0] * u_scale + u_offset, 0.0, 1.0)
            remapped_uvs[:, 1] = np.clip(raw_uvs[:, 1] * v_scale + v_offset, 0.0, 1.0)
            
            combined_verts.append(verts)
            combined_faces.append(faces)
            combined_uvs.append(remapped_uvs)
            
            vert_offset += len(verts)
            print(f"  Packed {name}: {len(verts)} verts, UV range: U=[{remapped_uvs[:,0].min():.3f}, {remapped_uvs[:,0].max():.3f}], V=[{remapped_uvs[:,1].min():.3f}, {remapped_uvs[:,1].max():.3f}]")
            
        final_verts = np.vstack(combined_verts)
        final_faces = np.vstack(combined_faces)
        final_uvs = np.vstack(combined_uvs)
        
        # Create unified PBR material
        material = trimesh.visual.material.PBRMaterial(
            name="UniversalCharacterMat",
            baseColorTexture=pil_tex,
            metallicFactor=0.0,
            roughnessFactor=0.65
        )
        
        unified_mesh = trimesh.Trimesh(
            vertices=final_verts,
            faces=final_faces,
            visual=trimesh.visual.TextureVisuals(uv=final_uvs, material=material),
            process=False
        )
        
        out_unified = f'tools/workbench/universal_player_singlemesh_{lod_name}.glb'
        unified_mesh.export(out_unified)
        print(f"SUCCESS: Exported {lod_name.upper()} single-mesh to {out_unified}")
        print(f"  Total Vertices: {len(final_verts)}")
        print(f"  Total Triangles: {len(final_faces)}")
        print(f"  Material Draw Calls: 1 (Single Draw Call)")
        
        # Mirror to artifact directory
        art_path = rf'C:\Users\hp\.gemini\antigravity\brain\c6f8d783-caab-4642-8c0a-b02059060228\universal_character_{lod_name}.glb'
        unified_mesh.export(art_path)
        print(f"  Mirrored to artifact directory: {art_path}")

if __name__ == '__main__':
    build_unified_atlas_models()
