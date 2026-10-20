import struct, json, io, os
import numpy as np
from PIL import Image

yamal_path = r'E:\Downlaod\football asset\players\lamine_yamal_model.glb'
with open(yamal_path, 'rb') as f:
    struct.unpack('<4sII', f.read(12))
    chunk_len, _ = struct.unpack('<I4s', f.read(8))
    gltf = json.loads(f.read(chunk_len).decode('utf-8'))
    bin_len, _ = struct.unpack('<I4s', f.read(8))
    bin_data = f.read(bin_len)

def get_accessor(idx):
    acc = gltf['accessors'][idx]
    bv = gltf['bufferViews'][acc['bufferView']]
    offset = bv.get('byteOffset', 0) + acc.get('byteOffset', 0)
    count = acc['count']
    ctype = acc['componentType']
    atype = acc['type']
    type_counts = {'SCALAR': 1, 'VEC2': 2, 'VEC3': 3, 'VEC4': 4, 'MAT4': 16}
    nc = type_counts[atype]
    dtype_map = {5120: np.int8, 5121: np.uint8, 5122: np.int16, 5123: np.uint16, 5125: np.uint32, 5126: np.float32}
    dtype = dtype_map[ctype]
    arr = np.frombuffer(bin_data, dtype=dtype, offset=offset, count=count * nc)
    if nc > 1:
        arr = arr.reshape((count, nc))
    return arr

print("=== DETAILED TOPOLOGY & PROPORTIONS MEASUREMENT ===")

meshes_info = []
all_vertices = []
for mi, m in enumerate(gltf['meshes']):
    p = m['primitives'][0]
    mat_id = p.get('material')
    mat_name = gltf['materials'][mat_id]['name'] if mat_id is not None else 'None'
    pos = get_accessor(p['attributes']['POSITION'])
    indices = get_accessor(p['indices'])
    tris = indices.reshape(-1, 3)
    
    # Calculate edge connectivity & valence
    edges = {}
    for tri in tris:
        for k in range(3):
            e = tuple(sorted((tri[k], tri[(k+1)%3])))
            edges[e] = edges.get(e, 0) + 1
            
    boundary_edges = sum(1 for c in edges.values() if c == 1)
    non_manifold_edges = sum(1 for c in edges.values() if c > 2)
    manifold_edges = sum(1 for c in edges.values() if c == 2)
    
    # Vertex valence (number of connected edges)
    valences = np.zeros(len(pos), dtype=int)
    for e in edges.keys():
        valences[e[0]] += 1
        valences[e[1]] += 1
    
    min_v, max_v = np.min(pos, axis=0), np.max(pos, axis=0)
    dim = max_v - min_v
    
    info = {
        'index': mi,
        'name': m.get('name'),
        'material': mat_name,
        'verts': len(pos),
        'tris': len(tris),
        'min': min_v,
        'max': max_v,
        'dim': dim,
        'boundary_edges': boundary_edges,
        'non_manifold_edges': non_manifold_edges,
        'avg_valence': np.mean(valences),
        'max_valence': np.max(valences)
    }
    meshes_info.append(info)
    all_vertices.append(pos)

for mi in meshes_info:
    print(f"Mesh {mi['index']} ({mi['name']}) -> Mat: {mi['material']}")
    print(f"  Verts: {mi['verts']}, Tris: {mi['tris']}")
    print(f"  Bounds: X=[{mi['min'][0]:.3f}, {mi['max'][0]:.3f}], Y=[{mi['min'][1]:.3f}, {mi['max'][1]:.3f}], Z=[{mi['min'][2]:.3f}, {mi['max'][2]:.3f}]")
    print(f"  Dimensions: W={mi['dim'][0]:.3f}m, H={mi['dim'][1]:.3f}m, D={mi['dim'][2]:.3f}m")
    print(f"  Topology: Boundary Edges={mi['boundary_edges']}, Non-Manifold={mi['non_manifold_edges']}, Avg Valence={mi['avg_valence']:.1f}, Max Valence={mi['max_valence']}")

# Overall Proportions
head_info = meshes_info[2] # AvatarHead
body_info = meshes_info[0] # AvatarBody
total_height = max(m['max'][1] for m in meshes_info) - min(m['min'][1] for m in meshes_info)
head_height = head_info['dim'][1]
head_to_body_ratio = total_height / head_height

print(f"\nPROPORTION METRICS:")
print(f"  Total Character Height: {total_height:.4f} m (~{total_height*100:.1f} cm)")
print(f"  Head Height: {head_height:.4f} m (~{head_height*100:.1f} cm)")
print(f"  Head-to-Body Ratio: 1 : {head_to_body_ratio:.2f} (Target is 1 : 7.5 - 8.0)")

# Check Arm Span & Leg Length
shoes_info = meshes_info[8] # outfit_shoes
bottom_info = meshes_info[7] # outfit_bottom
top_info = meshes_info[9] # outfit_top
torso_length = top_info['dim'][1]
legs_length = bottom_info['max'][1] - shoes_info['min'][1]
shoulder_width = top_info['dim'][0]
hip_width = bottom_info['dim'][0]

print(f"  Legs Span (Hip to Ground): {legs_length:.4f} m (~{legs_length*100:.1f} cm)")
print(f"  Torso Height: {torso_length:.4f} m (~{torso_length*100:.1f} cm)")
print(f"  Shoulder Width: {shoulder_width:.4f} m")
print(f"  Hip Width: {hip_width:.4f} m")

# Joint Area Analysis (Knee, Elbow, Shoulder)
# AvatarBody vertices around knee: y in [0.4, 0.6]
body_pos = all_vertices[0]
knee_verts = body_pos[(body_pos[:, 1] >= 0.40) & (body_pos[:, 1] <= 0.60)]
elbow_verts = body_pos[(body_pos[:, 0] >= 0.35) & (body_pos[:, 1] >= 1.0) & (body_pos[:, 1] <= 1.25)]
print(f"\nJOINT DENSITY:")
print(f"  Knee region body verts: {len(knee_verts)}")
print(f"  Elbow/Forearm region body verts: {len(elbow_verts)}")

# Texture Analysis for Baked Lighting
print("\n=== TEXTURE BAKED LIGHTING & SHADING ANALYSIS ===")
scratch_dir = r'C:\Users\hp\.gemini\antigravity\brain\c6f8d783-caab-4642-8c0a-b02059060228\scratch\yamal_textures'
for img_name in ['tex_0.jpg', 'tex_2.jpg', 'tex_8.jpg', 'tex_12.jpg']:
    p = os.path.join(scratch_dir, img_name)
    with Image.open(p) as img:
        arr = np.array(img).astype(float)
        # Convert to luminance
        lum = 0.299 * arr[:,:,0] + 0.587 * arr[:,:,1] + 0.114 * arr[:,:,2]
        # Top half vs bottom half brightness to check directional light bake
        top_half = lum[:512, :]
        bot_half = lum[512:, :]
        lum_diff = np.mean(top_half) - np.mean(bot_half)
        print(f"{img_name}: Mean Lum={np.mean(lum):.1f}, Contrast(std)={np.std(lum):.1f}, Top-to-Bottom gradient={lum_diff:.1f}")
