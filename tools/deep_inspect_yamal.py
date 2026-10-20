import struct, json, io, os
import numpy as np

yamal_path = r'E:\Downlaod\football asset\players\lamine_yamal_model.glb'
with open(yamal_path, 'rb') as f:
    magic, version, length = struct.unpack('<4sII', f.read(12))
    chunk_len, chunk_type = struct.unpack('<I4s', f.read(8))
    gltf = json.loads(f.read(chunk_len).decode('utf-8'))
    bin_len, bin_type = struct.unpack('<I4s', f.read(8))
    bin_data = f.read(bin_len)

def get_accessor_data(acc_idx):
    acc = gltf['accessors'][acc_idx]
    bv = gltf['bufferViews'][acc['bufferView']]
    offset = bv.get('byteOffset', 0) + acc.get('byteOffset', 0)
    count = acc['count']
    ctype = acc['componentType']
    atype = acc['type']
    
    # Types
    type_counts = {'SCALAR': 1, 'VEC2': 2, 'VEC3': 3, 'VEC4': 4, 'MAT4': 16}
    num_components = type_counts[atype]
    
    # Component types
    # 5120: BYTE, 5121: UBYTE, 5122: SHORT, 5123: USHORT, 5125: UINT, 5126: FLOAT
    dtype_map = {
        5120: np.int8,
        5121: np.uint8,
        5122: np.int16,
        5123: np.uint16,
        5125: np.uint32,
        5126: np.float32
    }
    dtype = dtype_map[ctype]
    stride = bv.get('byteStride')
    if stride is None:
        arr = np.frombuffer(bin_data, dtype=dtype, offset=offset, count=count * num_components)
        if num_components > 1:
            arr = arr.reshape((count, num_components))
        return arr
    else:
        # Strided read
        element_size = num_components * np.dtype(dtype).itemsize
        raw = []
        for i in range(count):
            start = offset + i * stride
            raw.append(np.frombuffer(bin_data, dtype=dtype, offset=start, count=num_components))
        return np.array(raw)

print("=== SKELETON & JOINTS ANALYSIS ===")
skins = gltf.get('skins', [])
for si, s in enumerate(skins):
    joints = s.get('joints', [])
    print(f"Skin {si}: '{s.get('name')}', {len(joints)} joints")
    joint_nodes = [gltf['nodes'][j] for j in joints]
    print("Joint names:")
    for j_idx, jn in zip(joints, joint_nodes):
        print(f"  [{j_idx}] {jn.get('name')} (children: {jn.get('children', [])})")

print("\n=== UV & WEIGHT CHECKS PER PRIMITIVE ===")
for mi, m in enumerate(gltf.get('meshes', [])):
    for pi, p in enumerate(m.get('primitives', [])):
        mat_id = p.get('material')
        mat_name = gltf['materials'][mat_id].get('name') if mat_id is not None else 'None'
        p_name = f"{m.get('name')}_p{pi} ({mat_name})"
        
        # Check UVs
        if 'TEXCOORD_0' in p['attributes']:
            uvs = get_accessor_data(p['attributes']['TEXCOORD_0'])
            uv_min = np.min(uvs, axis=0)
            uv_max = np.max(uvs, axis=0)
            # check if outside [0, 1]
            out_of_bounds = np.any((uvs < -0.01) | (uvs > 1.01))
            print(f"{p_name}: UV min={uv_min.round(3)}, max={uv_max.round(3)}, OutOfBounds={out_of_bounds}")
        else:
            print(f"{p_name}: NO UVs!")
            
        # Check Skin Weights
        if 'WEIGHTS_0' in p['attributes']:
            weights = get_accessor_data(p['attributes']['WEIGHTS_0'])
            joints = get_accessor_data(p['attributes']['JOINTS_0'])
            weight_sums = np.sum(weights, axis=1)
            unweighted = np.sum(weight_sums < 0.001)
            not_normalized = np.sum(np.abs(weight_sums - 1.0) > 0.02)
            max_influences = np.sum(weights > 0.001, axis=1)
            print(f"  Weights: Unweighted verts={unweighted}, NotNormalized={not_normalized}, MaxInfluences/vert={np.max(max_influences)}, MinInfluences={np.min(max_influences)}")
        else:
            print(f"  Weights: NO WEIGHTS!")
