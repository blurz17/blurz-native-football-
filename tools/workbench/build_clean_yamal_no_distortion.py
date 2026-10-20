import struct, json, os

def build_clean_yamal():
    src_glb = r'tools/workbench/yamal_original_copy.glb'
    out_glb = r'tools/workbench/yamal_clean_base.glb'
    
    with open(src_glb, 'rb') as f:
        magic, version, length = struct.unpack('<4sII', f.read(12))
        chunk_len, chunk_type = struct.unpack('<I4s', f.read(8))
        gltf = json.loads(f.read(chunk_len).decode('utf-8'))
        bin_len, bin_type = struct.unpack('<I4s', f.read(8))
        bin_data = bytearray(f.read(bin_len))

    print("=== BUILDING CLEAN YAMAL BASE (ZERO GEOMETRIC DISTORTION) ===")
    
    # Meshes in original:
    # 0: AvatarBody
    # 1: AvatarEyelashes -> DROP (4,900 tris)
    # 2: AvatarHead -> KEEP
    # 3: AvatarLeftEyeball -> KEEP
    # 4: AvatarRightEyeball -> KEEP
    # 5: AvatarTeethLower -> DROP (3,464 tris)
    # 6: AvatarTeethUpper -> DROP (3,654 tris)
    # 7: outfit_bottom -> KEEP
    # 8: outfit_shoes -> KEEP
    # 9: outfit_top -> KEEP
    
    drop_mesh_indices = {1, 5, 6}
    
    # In gltf['nodes'], find which nodes point to dropped meshes
    drop_node_indices = set()
    for ni, node in enumerate(gltf['nodes']):
        if node.get('mesh') in drop_mesh_indices:
            drop_node_indices.add(ni)
            print(f"Dropping node {ni}: {node.get('name')} (mesh {node.get('mesh')})")

    # Filter scenes to remove dropped nodes
    for scene in gltf['scenes']:
        scene['nodes'] = [n for n in scene['nodes'] if n not in drop_node_indices]
        
    # Remove dropped nodes from any parent's children array
    for node in gltf['nodes']:
        if 'children' in node:
            node['children'] = [c for c in node['children'] if c not in drop_node_indices]

    # Leave all remaining vertex data, normals, tangents, UVs, textures, and bone matrices 100% UNTOUCHED!
    json_bytes = json.dumps(gltf).encode('utf-8')
    json_padding = (4 - (len(json_bytes) % 4)) % 4
    json_bytes += b' ' * json_padding
    
    bin_padding = (4 - (len(bin_data) % 4)) % 4
    bin_data += b'\x00' * bin_padding
    
    total_len = 12 + 8 + len(json_bytes) + 8 + len(bin_data)
    
    with open(out_glb, 'wb') as out_f:
        out_f.write(struct.pack('<4sII', b'glTF', 2, total_len))
        out_f.write(struct.pack('<I4s', len(json_bytes), b'JSON'))
        out_f.write(json_bytes)
        out_f.write(struct.pack('<I4s', len(bin_data), b'BIN\x00'))
        out_f.write(bin_data)
        
    print(f"Clean model successfully exported to {out_glb} ({total_len} bytes)")

if __name__ == '__main__':
    build_clean_yamal()
