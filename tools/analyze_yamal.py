import struct, json, io, os
from PIL import Image

yamal_path = r'E:\Downlaod\football asset\players\lamine_yamal_model.glb'
with open(yamal_path, 'rb') as f:
    magic, version, length = struct.unpack('<4sII', f.read(12))
    chunk_len, chunk_type = struct.unpack('<I4s', f.read(8))
    gltf = json.loads(f.read(chunk_len).decode('utf-8'))
    
    bin_len, bin_type = struct.unpack('<I4s', f.read(8))
    bin_data = f.read(bin_len)

print('=== YAMAL GLTF STRUCTURE ===')
print('Asset:', gltf.get('asset'))
print('Nodes count:', len(gltf.get('nodes', [])))
print('Meshes count:', len(gltf.get('meshes', [])))
print('Materials count:', len(gltf.get('materials', [])))
print('Textures count:', len(gltf.get('textures', [])))
print('Images count:', len(gltf.get('images', [])))
print('Skins count:', len(gltf.get('skins', [])))
print('Animations count:', len(gltf.get('animations', [])))

# Inspect nodes
print('\n=== NODES ===')
for i, n in enumerate(gltf.get('nodes', [])):
    print(f'Node {i}: name="{n.get("name")}", mesh={n.get("mesh")}, skin={n.get("skin")}, children={n.get("children")}')

# Inspect skins
print('\n=== SKINS ===')
for i, s in enumerate(gltf.get('skins', [])):
    joints = s.get('joints', [])
    print(f'Skin {i}: name="{s.get("name")}", joints count={len(joints)}, skeleton root={s.get("skeleton")}')
    joint_names = [gltf['nodes'][j].get('name') for j in joints if j < len(gltf['nodes'])]
    print(f'  Joint names ({len(joint_names)}): {joint_names}')

# Inspect meshes and primitives
print('\n=== MESHES & PRIMITIVES ===')
total_verts = 0
total_tris = 0
mesh_bounds = []

for mi, m in enumerate(gltf.get('meshes', [])):
    print(f'Mesh {mi}: "{m.get("name")}"')
    for pi, p in enumerate(m.get('primitives', [])):
        mat_id = p.get('material')
        mat_name = gltf['materials'][mat_id].get('name') if mat_id is not None and mat_id < len(gltf['materials']) else 'None'
        idx_acc = gltf['accessors'][p['indices']]
        pos_acc = gltf['accessors'][p['attributes']['POSITION']]
        num_v = pos_acc['count']
        num_idx = idx_acc['count']
        num_t = num_idx // 3
        total_verts += num_v
        total_tris += num_t
        attrs = list(p['attributes'].keys())
        min_pos = pos_acc.get('min')
        max_pos = pos_acc.get('max')
        mesh_bounds.append((min_pos, max_pos))
        print(f'  Prim {pi}: Material[{mat_id}]="{mat_name}", Verts={num_v}, Tris={num_t}, Attrs={attrs}')
        print(f'         Min: {min_pos}, Max: {max_pos}')

print(f'\nTOTAL: Vertices={total_verts}, Triangles={total_tris}')

# Overall Bounding Box
all_mins = [b[0] for b in mesh_bounds if b[0]]
all_maxs = [b[1] for b in mesh_bounds if b[1]]
if all_mins and all_maxs:
    overall_min = [min(m[i] for m in all_mins) for i in range(3)]
    overall_max = [max(m[i] for m in all_maxs) for i in range(3)]
    dimensions = [overall_max[i] - overall_min[i] for i in range(3)]
    print(f'\nOVERALL BOUNDING BOX:')
    print(f'  Min: {overall_min}')
    print(f'  Max: {overall_max}')
    print(f'  Dimensions (X, Y, Z / Width, Depth, Height): {dimensions}')

# Inspect materials and textures
print('\n=== MATERIALS ===')
for mi, mat in enumerate(gltf.get('materials', [])):
    pbr = mat.get('pbrMetallicRoughness', {})
    base_tex = pbr.get('baseColorTexture')
    normal_tex = mat.get('normalTexture')
    mr_tex = pbr.get('metallicRoughnessTexture')
    emiss_tex = mat.get('emissiveTexture')
    print(f'Material {mi}: "{mat.get("name")}"')
    print(f'  BaseColorFactor: {pbr.get("baseColorFactor")}')
    print(f'  MetallicFactor: {pbr.get("metallicFactor")}, RoughnessFactor: {pbr.get("roughnessFactor")}')
    print(f'  BaseColorTex: {base_tex}, NormalTex: {normal_tex}, MetallicRoughnessTex: {mr_tex}, EmissiveTex: {emiss_tex}')

# Inspect images
print('\n=== IMAGES ===')
for ii, img_info in enumerate(gltf.get('images', [])):
    bv_id = img_info.get('bufferView')
    bv = gltf['bufferViews'][bv_id]
    offset = bv.get('byteOffset', 0)
    length = bv['byteLength']
    img_bytes = bin_data[offset:offset+length]
    with Image.open(io.BytesIO(img_bytes)) as pil_img:
        print(f'Image {ii}: name="{img_info.get("name")}", mime={img_info.get("mimeType")}, Size={pil_img.size}, Mode={pil_img.mode}, Bytes={length}')
