import struct, json, io, os
from PIL import Image
import numpy as np

yamal_path = r'E:\Downlaod\football asset\players\lamine_yamal_model.glb'
with open(yamal_path, 'rb') as f:
    struct.unpack('<4sII', f.read(12))
    chunk_len, _ = struct.unpack('<I4s', f.read(8))
    gltf = json.loads(f.read(chunk_len).decode('utf-8'))
    bin_len, _ = struct.unpack('<I4s', f.read(8))
    bin_data = f.read(bin_len)

scratch_dir = r'C:\Users\hp\.gemini\antigravity\brain\c6f8d783-caab-4642-8c0a-b02059060228\scratch\yamal_textures'
os.makedirs(scratch_dir, exist_ok=True)

print("=== EXTRACTING & ANALYZING YAMAL TEXTURES ===")
for ii, img_info in enumerate(gltf.get('images', [])):
    bv_id = img_info.get('bufferView')
    bv = gltf['bufferViews'][bv_id]
    offset = bv.get('byteOffset', 0)
    length = bv['byteLength']
    img_bytes = bin_data[offset:offset+length]
    
    # Save to scratch
    ext = 'jpg' if img_info.get('mimeType') == 'image/jpeg' else 'png'
    out_path = os.path.join(scratch_dir, f'tex_{ii}.{ext}')
    with open(out_path, 'wb') as out_f:
        out_f.write(img_bytes)
        
    with Image.open(io.BytesIO(img_bytes)) as pil_img:
        arr = np.array(pil_img)
        mean_val = np.mean(arr, axis=(0,1))
        std_val = np.std(arr, axis=(0,1))
        print(f"Image {ii}: {out_path}")
        print(f"  Shape={arr.shape}, Mean={mean_val.round(1)}, Std={std_val.round(1)}")

# Check material bindings to textures
print("\n=== MATERIAL TO TEXTURE MAPPING ===")
for mi, mat in enumerate(gltf.get('materials', [])):
    pbr = mat.get('pbrMetallicRoughness', {})
    base_idx = pbr.get('baseColorTexture', {}).get('index')
    norm_idx = mat.get('normalTexture', {}).get('index')
    print(f"Material {mi} ({mat.get('name')}): BaseColorTex={base_idx}, NormalTex={norm_idx}")
