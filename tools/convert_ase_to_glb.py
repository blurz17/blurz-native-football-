import sys
import os
import struct
import json
import re

def parse_ase(ase_path):
    with open(ase_path, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()

    # Parse vertices
    vertices = []
    v_matches = re.findall(r'\*MESH_VERTEX\s+(\d+)\s+([\-\d\.]+)\s+([\-\d\.]+)\s+([\-\d\.]+)', content)
    for idx, x, y, z in sorted(v_matches, key=lambda m: int(m[0])):
        vertices.append((float(x), float(y), float(z)))

    # Parse faces
    faces = []
    f_matches = re.findall(r'\*MESH_FACE\s+(\d+):\s+A:\s+(\d+)\s+B:\s+(\d+)\s+C:\s+(\d+)', content)
    for idx, a, b, c in sorted(f_matches, key=lambda m: int(m[0])):
        faces.append((int(a), int(b), int(c)))

    # Parse texture vertices
    tverts = []
    tv_matches = re.findall(r'\*MESH_TVERT\s+(\d+)\s+([\-\d\.]+)\s+([\-\d\.]+)', content)
    for idx, u, v in sorted(tv_matches, key=lambda m: int(m[0])):
        tverts.append((float(u), float(v)))

    # Parse texture faces
    tfaces = []
    tf_matches = re.findall(r'\*MESH_TFACE\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)', content)
    for idx, a, b, c in sorted(tf_matches, key=lambda m: int(m[0])):
        tfaces.append((int(a), int(b), int(c)))

    # Parse normals
    normals = []
    # Normals per vertex per face
    # In ASE: MESH_FACENORMAL f ... MESH_VERTEXNORMAL v x y z (x 3)
    norm_block = re.findall(r'\*MESH_VERTEXNORMAL\s+\d+\s+([\-\d\.]+)\s+([\-\d\.]+)\s+([\-\d\.]+)', content)
    face_vertex_normals = []
    for nx, ny, nz in norm_block:
        face_vertex_normals.append((float(nx), float(ny), float(nz)))

    # Check bitmap
    bitmap_match = re.search(r'\*BITMAP\s+"([^"]+)"', content)
    bitmap_path = bitmap_match.group(1) if bitmap_match else None

    return {
        'vertices': vertices,
        'faces': faces,
        'tverts': tverts,
        'tfaces': tfaces,
        'face_vertex_normals': face_vertex_normals,
        'bitmap': bitmap_path
    }

def build_glb(data, ase_dir, output_glb_path):
    faces = data['faces']
    verts = data['vertices']
    tfaces = data['tfaces']
    tverts = data['tverts']
    fnorms = data['face_vertex_normals']

    # Unroll into unique vertex tuples: (pos, norm, uv)
    unrolled_pos = []
    unrolled_norm = []
    unrolled_uv = []
    indices = []

    has_uv = len(tfaces) == len(faces) and len(tverts) > 0
    has_fnorms = len(fnorms) == len(faces) * 3

    for f_idx, (va, vb, vc) in enumerate(faces):
        face_v = [va, vb, vc]
        face_tv = tfaces[f_idx] if has_uv else [0, 0, 0]

        for corner in range(3):
            v_idx = face_v[corner]
            p = verts[v_idx]

            if has_fnorms:
                n = fnorms[f_idx * 3 + corner]
            else:
                n = (0.0, 1.0, 0.0)

            if has_uv:
                uv = tverts[face_tv[corner]]
            else:
                uv = (0.0, 0.0)

            indices.append(len(unrolled_pos))
            unrolled_pos.append(p)
            unrolled_norm.append(n)
            unrolled_uv.append((uv[0], 1.0 - uv[1])) # Flip V for glTF standard

    # Pack binary buffers
    pos_bytes = bytearray()
    min_pos = [float('inf')]*3
    max_pos = [float('-inf')]*3
    for p in unrolled_pos:
        pos_bytes.extend(struct.pack('<fff', p[0], p[1], p[2]))
        for i in range(3):
            min_pos[i] = min(min_pos[i], p[i])
            max_pos[i] = max(max_pos[i], p[i])

    norm_bytes = bytearray()
    for n in unrolled_norm:
        norm_bytes.extend(struct.pack('<fff', n[0], n[1], n[2]))

    uv_bytes = bytearray()
    for uv in unrolled_uv:
        uv_bytes.extend(struct.pack('<ff', uv[0], uv[1]))

    index_bytes = bytearray()
    use_uint32 = len(unrolled_pos) > 65535
    fmt = '<I' if use_uint32 else '<H'
    for idx in indices:
        index_bytes.extend(struct.pack(fmt, idx))

    # Pad to 4 bytes
    def pad4(b):
        while len(b) % 4 != 0:
            b.append(0)
        return b

    pos_bytes = pad4(pos_bytes)
    norm_bytes = pad4(norm_bytes)
    uv_bytes = pad4(uv_bytes)
    index_bytes = pad4(index_bytes)

    # Check if we have embedded texture
    image_bytes = bytearray()
    image_mime = "image/jpeg"
    has_image = False
    if data['bitmap']:
        rel_img = os.path.basename(data['bitmap'])
        img_candidates = [
            os.path.join(ase_dir, rel_img),
            os.path.join(ase_dir, data['bitmap']),
            os.path.join("assets", data['bitmap']),
            os.path.join("platform/android/app/src/main/assets", data['bitmap'])
        ]
        for c in img_candidates:
            if os.path.exists(c):
                with open(c, 'rb') as f:
                    image_bytes = bytearray(f.read())
                image_mime = "image/png" if c.lower().endswith(".png") else "image/jpeg"
                has_image = True
                break

    image_bytes = pad4(image_bytes)

    # Concatenate all buffers into single bin chunk
    bufferViews = []
    accessors = []
    bin_data = bytearray()

    # View 0: Indices
    offset = len(bin_data)
    bin_data.extend(index_bytes)
    bufferViews.append({
        "buffer": 0,
        "byteOffset": offset,
        "byteLength": len(index_bytes),
        "target": 34963 # ELEMENT_ARRAY_BUFFER
    })
    accessors.append({
        "bufferView": 0,
        "byteOffset": 0,
        "componentType": 5125 if use_uint32 else 5123, # UNSIGNED_INT / UNSIGNED_SHORT
        "count": len(indices),
        "type": "SCALAR"
    })

    # View 1: Positions
    offset = len(bin_data)
    bin_data.extend(pos_bytes)
    bufferViews.append({
        "buffer": 0,
        "byteOffset": offset,
        "byteLength": len(pos_bytes),
        "target": 34962 # ARRAY_BUFFER
    })
    accessors.append({
        "bufferView": 1,
        "byteOffset": 0,
        "componentType": 5126, # FLOAT
        "count": len(unrolled_pos),
        "type": "VEC3",
        "min": min_pos,
        "max": max_pos
    })

    # View 2: Normals
    offset = len(bin_data)
    bin_data.extend(norm_bytes)
    bufferViews.append({
        "buffer": 0,
        "byteOffset": offset,
        "byteLength": len(norm_bytes),
        "target": 34962
    })
    accessors.append({
        "bufferView": 2,
        "byteOffset": 0,
        "componentType": 5126,
        "count": len(unrolled_norm),
        "type": "VEC3"
    })

    # View 3: UVs
    offset = len(bin_data)
    bin_data.extend(uv_bytes)
    bufferViews.append({
        "buffer": 0,
        "byteOffset": offset,
        "byteLength": len(uv_bytes),
        "target": 34962
    })
    accessors.append({
        "bufferView": 3,
        "byteOffset": 0,
        "componentType": 5126,
        "count": len(unrolled_uv),
        "type": "VEC2"
    })

    images = []
    textures = []
    materials = []
    if has_image:
        offset = len(bin_data)
        bin_data.extend(image_bytes)
        img_view_idx = len(bufferViews)
        bufferViews.append({
            "buffer": 0,
            "byteOffset": offset,
            "byteLength": len(image_bytes)
        })
        images.append({
            "bufferView": img_view_idx,
            "mimeType": image_mime
        })
        textures.append({
            "source": 0
        })
        materials.append({
            "name": "BallMaterial",
            "pbrMetallicRoughness": {
                "baseColorTexture": { "index": 0 },
                "roughnessFactor": 0.8,
                "metallicFactor": 0.1
            }
        })
    else:
        materials.append({
            "name": "DefaultMaterial",
            "pbrMetallicRoughness": {
                "roughnessFactor": 0.8,
                "metallicFactor": 0.1
            }
        })

    gltf_json = {
        "asset": {
            "version": "2.0",
            "generator": "Antigravity ASE to GLB Converter"
        },
        "scenes": [{ "nodes": [0] }],
        "scene": 0,
        "nodes": [{ "mesh": 0, "name": "ball" }],
        "meshes": [{
            "name": "ball_mesh",
            "primitives": [{
                "attributes": {
                    "POSITION": 1,
                    "NORMAL": 2,
                    "TEXCOORD_0": 3
                },
                "indices": 0,
                "material": 0,
                "mode": 4 # TRIANGLES
            }]
        }],
        "materials": materials,
        "accessors": accessors,
        "bufferViews": bufferViews,
        "buffers": [{ "byteLength": len(bin_data) }]
    }

    if has_image:
        gltf_json["images"] = images
        gltf_json["textures"] = textures

    json_str = json.dumps(gltf_json, separators=(',', ':'))
    json_bytes = json_str.encode('utf-8')
    while len(json_bytes) % 4 != 0:
        json_bytes += b' ' # Pad with spaces

    # Total GLB size: 12 (header) + 8 + len(json_bytes) + 8 + len(bin_data)
    total_len = 12 + 8 + len(json_bytes) + 8 + len(bin_data)

    glb = bytearray()
    # GLB Header
    glb.extend(struct.pack('<III', 0x46546C67, 2, total_len)) # magic 0x46546C67 ("glTF"), version 2
    # Chunk 0: JSON
    glb.extend(struct.pack('<II', len(json_bytes), 0x4E4F534A)) # 0x4E4F534A ("JSON")
    glb.extend(json_bytes)
    # Chunk 1: BIN
    glb.extend(struct.pack('<II', len(bin_data), 0x004E4942)) # 0x004E4942 ("BIN\0")
    glb.extend(bin_data)

    with open(output_glb_path, 'wb') as f:
        f.write(glb)

    print(f"Successfully generated {output_glb_path} ({len(glb)} bytes, {len(indices)//3} triangles, embedded texture: {has_image})")

if __name__ == '__main__':
    ase_file = sys.argv[1] if len(sys.argv) > 1 else 'assets/media/objects/balls/generic.ase'
    out_file = sys.argv[2] if len(sys.argv) > 2 else 'assets/media/objects/balls/generic.glb'
    data = parse_ase(ase_file)
    build_glb(data, os.path.dirname(ase_file), out_file)
