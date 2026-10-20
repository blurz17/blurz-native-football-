import struct, json

with open(r'tools/workbench/yamal_clean_base.glb', 'rb') as f:
    f.read(12)
    chunk_len, _ = struct.unpack('<I4s', f.read(8))
    gltf = json.loads(f.read(chunk_len).decode('utf-8'))

def list_nodes(node_idx, depth=0):
    node = gltf['nodes'][node_idx]
    mesh_idx = node.get('mesh')
    indent = '  ' * depth
    if mesh_idx is not None:
        m_name = gltf['meshes'][mesh_idx]['name']
        print(f"{indent}[Mesh {mesh_idx}] '{node.get('name')}' -> '{m_name}'")
    for child in node.get('children', []):
        list_nodes(child, depth + 1)

print("Active hierarchy in clean base model:")
for root in gltf['scenes'][0]['nodes']:
    list_nodes(root)
