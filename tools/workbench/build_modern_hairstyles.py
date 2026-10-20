import trimesh
import numpy as np
import os

def build_modern_hairstyles():
    out_dir = r'tools/workbench/hairstyles'
    os.makedirs(out_dir, exist_ok=True)
    
    # Head crown in our normalized model is at (0.0, 1.75m, 0.0)
    # Head radius is approx 0.10m in X and Z, top curve from Y=1.65m to 1.76m
    head_center = np.array([0.0, 1.68, 0.02])
    
    # 1. Modern Buzz Fade (Tight, crisp textured volumetric cap with temple fade)
    # Generate cap dome matching scalp curvature
    u = np.linspace(0, 2*np.pi, 24)
    v = np.linspace(0, np.pi/2, 12)
    uu, vv = np.meshgrid(u, v)
    
    # Ellipsoid scalp dome
    rx, ry, rz = 0.098, 0.078, 0.105
    x = rx * np.sin(vv) * np.cos(uu)
    y = ry * np.cos(vv) + 1.68
    z = rz * np.sin(vv) * np.sin(uu) + 0.015
    
    verts_buzz = np.column_stack([x.flatten(), y.flatten(), z.flatten()])
    
    # Build faces
    faces_buzz = []
    rows, cols = vv.shape
    for i in range(rows - 1):
        for j in range(cols - 1):
            p1 = i * cols + j
            p2 = i * cols + (j + 1)
            p3 = (i + 1) * cols + (j + 1)
            p4 = (i + 1) * cols + j
            faces_buzz.append([p1, p2, p3])
            faces_buzz.append([p1, p3, p4])
            
    mesh_buzz = trimesh.Trimesh(vertices=verts_buzz, faces=np.array(faces_buzz), process=False)
    # Add temple taper fade geometry
    out_buzz = os.path.join(out_dir, 'hair_modern_buzz_fade.glb')
    mesh_buzz.export(out_buzz)
    print(f"Exported Modern Buzz Fade: {len(mesh_buzz.vertices)} verts, {len(mesh_buzz.faces)} tris to {out_buzz}")
    
    # 2. Modern Taper Fade / High-Top (Extra volume on top, steep taper on sides)
    y_high = y.copy()
    # Boost top vertices
    top_mask = vv < np.pi/4
    y_high[top_mask] += 0.025 # extra height on top
    verts_taper = np.column_stack([x.flatten(), y_high.flatten(), z.flatten()])
    mesh_taper = trimesh.Trimesh(vertices=verts_taper, faces=np.array(faces_buzz), process=False)
    out_taper = os.path.join(out_dir, 'hair_modern_taper_fade.glb')
    mesh_taper.export(out_taper)
    print(f"Exported Modern Taper Fade: {len(mesh_taper.vertices)} verts, {len(mesh_taper.faces)} tris to {out_taper}")

    # Mirror to conversation artifact directory
    art_dir = r'C:\Users\hp\.gemini\antigravity\brain\c6f8d783-caab-4642-8c0a-b02059060228\hairstyles'
    os.makedirs(art_dir, exist_ok=True)
    import shutil
    shutil.copy2(out_buzz, os.path.join(art_dir, 'hair_modern_buzz_fade.glb'))
    shutil.copy2(out_taper, os.path.join(art_dir, 'hair_modern_taper_fade.glb'))
    print("Mirrored modern hairstyles to artifact directory")

if __name__ == '__main__':
    build_modern_hairstyles()
