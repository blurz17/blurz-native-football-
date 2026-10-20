import struct, json, os
import numpy as np
from PIL import Image, ImageDraw

def generate_uv_wireframes():
    src_glb = r'tools/workbench/yamal_clean_base.glb'
    out_dir = r'tools/workbench/paintable_templates'
    os.makedirs(out_dir, exist_ok=True)
    
    with open(src_glb, 'rb') as f:
        f.read(12)
        chunk_len, _ = struct.unpack('<I4s', f.read(8))
        gltf = json.loads(f.read(chunk_len).decode('utf-8'))
        bin_len, _ = struct.unpack('<I4s', f.read(8))
        bin_data = f.read(bin_len)

    def get_acc(idx):
        acc = gltf['accessors'][idx]
        bv = gltf['bufferViews'][acc['bufferView']]
        offset = bv.get('byteOffset', 0) + acc.get('byteOffset', 0)
        count = acc['count']
        ctype = acc['componentType']
        atype = acc['type']
        nc = {'SCALAR': 1, 'VEC2': 2, 'VEC3': 3, 'VEC4': 4}[atype]
        dt = {5120: np.int8, 5121: np.uint8, 5122: np.int16, 5123: np.uint16, 5125: np.uint32, 5126: np.float32}[ctype]
        arr = np.frombuffer(bin_data, dtype=dt, offset=offset, count=count * nc)
        return arr.reshape((count, nc)) if nc > 1 else arr

    slots_to_export = {
        'outfit_top_outfit_top_0': ('shirt_jersey', gltf['meshes'][9], (255, 61, 0)),
        'outfit_bottom_outfit_bottom_0': ('shorts', gltf['meshes'][7], (255, 214, 0)),
        '1': ('head_face', gltf['meshes'][2], (0, 229, 255)),
        'outfit_shoes_outfit_shoes_0': ('boots_shoes', gltf['meshes'][8], (0, 230, 118)),
        'AvatarBody_AvatarBody_0': ('skin_limbs', gltf['meshes'][0], (255, 128, 171))
    }
    
    W, H = 2048, 2048
    
    for mesh_name, (label, mesh_obj, edge_color) in slots_to_export.items():
        p = mesh_obj['primitives'][0]
        uv_idx = p['attributes']['TEXCOORD_0']
        idx_idx = p['indices']
        
        uvs = get_acc(uv_idx)
        indices = get_acc(idx_idx).reshape(-1, 3)
        
        # Create image with dark background and 64px grid lines
        img = Image.new('RGBA', (W, H), (18, 22, 28, 255))
        draw = ImageDraw.Draw(img)
        
        # Grid lines
        for x in range(0, W, 64):
            draw.line([(x, 0), (x, H)], fill=(28, 34, 42, 255), width=1)
        for y in range(0, H, 64):
            draw.line([(0, y), (W, y)], fill=(28, 34, 42, 255), width=1)
            
        print(f"Generating wireframe for {label} ({len(indices)} tris)...")
        # Draw wireframe lines for each triangle
        for tri in indices:
            pts = []
            for vi in tri:
                u, v = uvs[vi]
                # In glTF, standard texture coordinates have origin at top-left or inverted V
                # We map u -> X, (1.0 - v) -> Y
                x = int(np.clip(u, 0.0, 1.0) * (W - 1))
                y = int(np.clip(v, 0.0, 1.0) * (H - 1))
                pts.append((x, y))
            draw.polygon(pts, outline=edge_color + (140,))
            
        # Add Title and Guidelines
        draw.rectangle([(10, 10), (450, 80)], fill=(10, 14, 20, 220), outline=(255, 255, 255, 100), width=2)
        draw.text((25, 20), f"UV TEMPLATE: {label.upper()}", fill=(255, 255, 255, 255))
        draw.text((25, 45), f"Slot: {mesh_name} | {len(indices)} Triangles", fill=(180, 200, 220, 255))
        
        out_path = os.path.join(out_dir, f'template_{label}_2048.png')
        img.save(out_path)
        print(f"  Saved template to {out_path}")
        
    print("\nAll slot-specific paintable UV templates generated successfully!")

if __name__ == '__main__':
    generate_uv_wireframes()
