import os
from PIL import Image, ImageDraw
import numpy as np
import trimesh

def build_referee_demo():
    out_dir = r'tools/workbench/texture_system'
    os.makedirs(out_dir, exist_ok=True)
    
    W = 2048
    H = 2048
    
    print("=== BUILDING REFEREE DEMONSTRATION TEXTURE ATLAS ===")
    
    # Start with universal base diffuse
    base_diffuse_path = os.path.join(out_dir, 'universal_base_diffuse.png')
    referee_atlas = Image.open(base_diffuse_path).copy()
    draw = ImageDraw.Draw(referee_atlas)
    
    # 1. Transform Zone A (Top-Left: 0,0 to 1024,1024) into Official Black Referee Kit
    # Create dark charcoal/black referee jersey texture
    ref_jersey = Image.new('RGB', (1024, 1024), (28, 30, 33))
    j_draw = ImageDraw.Draw(ref_jersey)
    # Subtle vertical pinstripes
    for x in range(80, 950, 40):
        j_draw.line([(x, 100), (x, 900)], fill=(40, 44, 48), width=3)
    # White referee collar
    j_draw.polygon([(400, 100), (512, 280), (624, 100)], fill=(240, 240, 240))
    # Chest referee official pocket & badge
    j_draw.rectangle([(320, 360), (440, 480)], fill=(18, 20, 22), outline=(200, 200, 200), width=2)
    j_draw.text((345, 410), "REFEREE\nOFFICIAL", fill=(255, 214, 0))
    referee_atlas.paste(ref_jersey, (0, 0))
    
    # 2. Transform Zone C (Bottom-Left: 0,1024 to 1024,2048) into Official Black Referee Shorts
    ref_shorts = Image.new('RGB', (1024, 1024), (20, 22, 24))
    s_draw = ImageDraw.Draw(ref_shorts)
    # Side seams
    s_draw.line([(100, 50), (100, 950)], fill=(35, 38, 42), width=4)
    s_draw.line([(924, 50), (924, 950)], fill=(35, 38, 42), width=4)
    referee_atlas.paste(ref_shorts, (0, 1024))
    
    # 3. Transform Zone D (Bottom-Right: 1024,1024 to 2048,2048) into Black Referee Cleats & Socks
    ref_boots = Image.new('RGB', (1024, 1024), (18, 18, 20))
    b_draw = ImageDraw.Draw(ref_boots)
    # Black leather boots with clean white laces
    b_draw.ellipse([(200, 200), (824, 824)], fill=(12, 12, 14), outline=(40, 40, 45), width=4)
    for y in range(400, 650, 40):
        b_draw.line([(450, y), (574, y)], fill=(220, 220, 220), width=4)
    referee_atlas.paste(ref_boots, (1024, 1024))
    
    referee_atlas_path = os.path.join(out_dir, 'referee_diffuse_atlas.png')
    referee_atlas.save(referee_atlas_path)
    print(f"Saved Referee Texture Atlas to {referee_atlas_path}")
    
    # 4. Swappable 3D Referee Collar & Whistle Attachment
    # Positioned at chest/neck (0.0, 1.48m, 0.12m)
    collar_verts = np.array([
        [-0.08, 1.50, 0.10],
        [ 0.08, 1.50, 0.10],
        [ 0.00, 1.42, 0.12], # V-collar tip
        [-0.04, 1.42, 0.11],
        [ 0.04, 1.42, 0.11]
    ])
    collar_faces = np.array([
        [0, 1, 2],
        [0, 2, 3],
        [1, 4, 2]
    ])
    collar_mesh = trimesh.Trimesh(vertices=collar_verts, faces=collar_faces, process=False)
    collar_path = r'tools/workbench/hairstyles/referee_collar_badge.glb'
    collar_mesh.export(collar_path)
    print(f"Exported Swappable Referee Collar Badge to {collar_path}")
    
    # Mirror to conversation artifact directory
    art_dir = r'C:\Users\hp\.gemini\antigravity\brain\c6f8d783-caab-4642-8c0a-b02059060228\texture_system'
    import shutil
    shutil.copy2(referee_atlas_path, os.path.join(art_dir, 'referee_diffuse_atlas.png'))
    print("Mirrored referee atlas to artifact directory")

if __name__ == '__main__':
    build_referee_demo()
