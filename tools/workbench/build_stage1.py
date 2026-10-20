import struct, json, io, os
import numpy as np

def build_stage1_proportions():
    src_glb = r'e:\MY-PROJECTS\android native football\tools\workbench\yamal_original_copy.glb'
    out_glb = r'e:\MY-PROJECTS\android native football\tools\workbench\yamal_stage1_proportions.glb'
    
    with open(src_glb, 'rb') as f:
        magic, version, length = struct.unpack('<4sII', f.read(12))
        chunk_len, chunk_type = struct.unpack('<I4s', f.read(8))
        gltf = json.loads(f.read(chunk_len).decode('utf-8'))
        bin_len, bin_type = struct.unpack('<I4s', f.read(8))
        bin_data = bytearray(f.read(bin_len))

    print("=== STAGE 1: GEOMETRY PRUNING & PROPORTION NORMALIZATION ===")
    
    # 1. Identify meshes to prune:
    # 1: AvatarEyelashes, 5: AvatarTeethLower, 6: AvatarTeethUpper
    prune_mesh_names = {'0', '2', 'AvatarTeethUpper_AvatarTeethUpper_0'}
    
    # Check accessors and buffer views
    # Accessor helper
    def get_acc_data(acc_idx):
        acc = gltf['accessors'][acc_idx]
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
        return arr.copy(), offset, count, dtype, nc

    def set_acc_data(acc_idx, new_arr):
        acc = gltf['accessors'][acc_idx]
        bv = gltf['bufferViews'][acc['bufferView']]
        offset = bv.get('byteOffset', 0) + acc.get('byteOffset', 0)
        raw_bytes = new_arr.astype(np.float32).tobytes()
        bin_data[offset:offset+len(raw_bytes)] = raw_bytes
        # update min / max
        acc['min'] = new_arr.min(axis=0).tolist()
        acc['max'] = new_arr.max(axis=0).tolist()

    # Proportions Adjustment:
    # Current Head: Y in [1.337, 1.675], height = 0.3374m
    # Target:
    # Total character height: 1.80m (from 1.675m -> scale height up by 1.075)
    # Head height: scale head by ~0.72 so head height becomes ~0.235m
    # Head neck pivot: ~1.40m on scaled body
    
    # First, let's analyze positions across all meshes
    meshes_to_keep = []
    nodes_to_keep = []
    
    # Meshes map:
    # 0: AvatarBody
    # 1: AvatarEyelashes -> DROP
    # 2: AvatarHead
    # 3: AvatarLeftEyeball
    # 4: AvatarRightEyeball
    # 5: AvatarTeethLower -> DROP
    # 6: AvatarTeethUpper -> DROP
    # 7: outfit_bottom
    # 8: outfit_shoes
    # 9: outfit_top
    
    keep_mesh_indices = [0, 2, 3, 4, 7, 8, 9]
    print(f"Keeping {len(keep_mesh_indices)} meshes: AvatarBody, AvatarHead, Eyes (L/R), Bottom, Shoes, Top")
    print("Pruned: Eyelashes (4,900 tris), Teeth Lower (3,464 tris), Teeth Upper (3,654 tris)")
    
    # Scaling parameters:
    # Body scale factor (height to 1.80m):
    # Old legs + torso height = 1.366m. We want total height = 1.80m with head height 0.235m.
    # So neck junction should be at Y = 1.80 - 0.235 = 1.565m.
    # Original neck junction is around Y = 1.37m.
    # Stature scale factor = 1.565 / 1.370 ≈ 1.142
    stature_scale_y = 1.80 / 1.6749 # ~1.0747
    
    # For head:
    # Head pivot at neck: Y_pivot = 1.37 * stature_scale_y ≈ 1.472m
    # Head local scale: 0.72x
    head_scale = 0.72
    target_head_height = 0.3374 * head_scale # ~0.243m
    target_total_height = 1.80
    
    print(f"Applying Proportion Normalization:")
    print(f"  Target Total Stature: {target_total_height:.2f} m")
    print(f"  Target Head Height: {target_head_height:.3f} m")
    print(f"  Target Head-to-Body Ratio: 1 : {target_total_height/target_head_height:.2f}")

    # Transform vertex positions:
    # For body, top, bottom, shoes: scale stature slightly so total height is 1.80m
    # For head & eyes: scale by head_scale around head center and align with neck
    
    # Calculate new head pivot
    # Original head min_y was 1.337m.
    # Scaled body top neck is around Y = 1.37 * 1.13 = 1.55m
    y_leg_torso_scale = 1.13 # Stature elongation for long athletic legs
    
    for mi in keep_mesh_indices:
        m = gltf['meshes'][mi]
        p = m['primitives'][0]
        pos_acc_idx = p['attributes']['POSITION']
        pos_arr, offset, count, dtype, nc = get_acc_data(pos_acc_idx)
        
        if mi in [2, 3, 4]: # Head, LeftEye, RightEye
            # Head pivot: center of neck/jaw around (0, 1.38, 0)
            pivot = np.array([0.0, 1.38, 0.0])
            scaled_pos = (pos_arr - pivot) * head_scale
            # Place scaled head so chin meets elongated neck at Y = 1.54m
            new_head_base_y = 1.54
            scaled_pos[:, 1] += new_head_base_y
            set_acc_data(pos_acc_idx, scaled_pos)
        else: # Body, Top, Bottom, Shoes
            scaled_pos = pos_arr.copy()
            # Elongate legs and torso to athletic 1.80m
            # Keep shoes at ground (y=0)
            scaled_pos[:, 1] = scaled_pos[:, 1] * y_leg_torso_scale
            # Athletic shoulder widening (1.05x in X for shoulders and chest)
            scaled_pos[:, 0] = scaled_pos[:, 0] * 1.04
            set_acc_data(pos_acc_idx, scaled_pos)

    # Re-build GLTF structure keeping only the retained meshes and nodes
    # Update scenes
    # Find nodes pointing to kept meshes
    new_nodes = []
    old_to_new_node = {}
    for ni, node in enumerate(gltf['nodes']):
        mesh_idx = node.get('mesh')
        if mesh_idx is None or mesh_idx in keep_mesh_indices:
            old_to_new_node[ni] = len(new_nodes)
            new_nodes.append(node)
            
    # Remap children
    for node in new_nodes:
        if 'children' in node:
            node['children'] = [old_to_new_node[c] for c in node['children'] if c in old_to_new_node]

    # Remap root scene nodes
    for scene in gltf['scenes']:
        scene['nodes'] = [old_to_new_node[n] for n in scene['nodes'] if n in old_to_new_node]
        
    # Remap skin joints if present
    for skin in gltf.get('skins', []):
        if 'joints' in skin:
            skin['joints'] = [old_to_new_node[j] for j in skin['joints'] if j in old_to_new_node]

    # Assemble GLB
    json_bytes = json.dumps(gltf).encode('utf-8')
    # Pad JSON to 4-byte boundary
    json_padding = (4 - (len(json_bytes) % 4)) % 4
    json_bytes += b' ' * json_padding
    
    bin_padding = (4 - (len(bin_data) % 4)) % 4
    bin_data += b'\x00' * bin_padding
    
    total_len = 12 + 8 + len(json_bytes) + 8 + len(bin_data)
    
    with open(out_glb, 'wb') as out_f:
        # GLB Header
        out_f.write(struct.pack('<4sII', b'glTF', 2, total_len))
        # JSON Chunk
        out_f.write(struct.pack('<I4s', len(json_bytes), b'JSON'))
        out_f.write(json_bytes)
        # BIN Chunk
        out_f.write(struct.pack('<I4s', len(bin_data), b'BIN\x00'))
        out_f.write(bin_data)
        
    print(f"\nStage 1 output written to {out_glb} ({total_len} bytes)")

if __name__ == '__main__':
    build_stage1_proportions()
