import json
import struct
import io
import os
import shutil
from PIL import Image

def extract_raw_yamal_textures():
    glb_path = r'E:\Downlaod\football asset\players\lamine_yamal_model.glb'
    target_dirs = [
        r'assets\media\objects\players\textures\yamal',
        r'platform\android\app\src\main\assets\media\objects\players\textures\yamal'
    ]
    for d in target_dirs:
        os.makedirs(d, exist_ok=True)
        
    with open(glb_path, 'rb') as f:
        data = f.read()
    magic, version, length = struct.unpack('<4sII', data[:12])
    chunk_len, chunk_type = struct.unpack('<I4s', data[12:20])
    meta = json.loads(data[20:20+chunk_len].decode('utf-8'))
    bin_data = data[20+chunk_len+8:]

    buffer_views = meta['bufferViews']
    images = meta['images']
    textures = meta['textures']
    materials = meta['materials']

    # Map material to base color image
    mat_to_file = {
        'AvatarHead': 'yamal_head.jpg',
        'AvatarBody': 'yamal_body.jpg',
        'outfit_top': 'yamal_top.jpg',
        'outfit_bottom': 'yamal_bottom.jpg',
        'outfit_shoes': 'yamal_shoes.jpg',
        'AvatarLeftEyeball': 'yamal_eye.jpg',
        'AvatarRightEyeball': 'yamal_eye.jpg',
        'AvatarTeethLower': 'yamal_teeth.jpg',
        'AvatarTeethUpper': 'yamal_teeth.jpg',
    }

    for mat in materials:
        mname = mat.get('name')
        if mname in mat_to_file:
            fname = mat_to_file[mname]
            pbr = mat.get('pbrMetallicRoughness', {})
            base_tex = pbr.get('baseColorTexture')
            if base_tex is not None:
                t_idx = base_tex.get('index')
                img_idx = textures[t_idx].get('source')
                img_meta = images[img_idx]
                bv = buffer_views[img_meta['bufferView']]
                raw_bytes = bin_data[bv['byteOffset']:bv['byteOffset']+bv['byteLength']]
                im = Image.open(io.BytesIO(raw_bytes))
                for td in target_dirs:
                    out_p = os.path.join(td, fname)
                    im.save(out_p, quality=95)
                    print(f"Saved {mname} baseColor ({im.size}) -> {out_p}")

if __name__ == '__main__':
    extract_raw_yamal_textures()
