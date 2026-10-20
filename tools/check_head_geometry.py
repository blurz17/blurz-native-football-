import struct, json, io, os
import numpy as np

yamal_path = r'E:\Downlaod\football asset\players\lamine_yamal_model.glb'
with open(yamal_path, 'rb') as f:
    struct.unpack('<4sII', f.read(12))
    chunk_len, _ = struct.unpack('<I4s', f.read(8))
    gltf = json.loads(f.read(chunk_len).decode('utf-8'))
    bin_len, _ = struct.unpack('<I4s', f.read(8))
    bin_data = f.read(bin_len)

head_mesh = gltf['meshes'][2]
pos_acc = gltf['accessors'][head_mesh['primitives'][0]['attributes']['POSITION']]
bv = gltf['bufferViews'][pos_acc['bufferView']]
offset = bv.get('byteOffset', 0) + pos_acc.get('byteOffset', 0)
count = pos_acc['count']
pos_arr = np.frombuffer(bin_data, dtype=np.float32, offset=offset, count=count*3).reshape((count, 3))

print("=== AVATAR HEAD MESH ANALYSIS ===")
print("Vertices count:", count)
print("Min:", np.min(pos_arr, axis=0))
print("Max:", np.max(pos_arr, axis=0))
print("Height (Y):", np.max(pos_arr[:, 1]) - np.min(pos_arr[:, 1]))
print("Width (X):", np.max(pos_arr[:, 0]) - np.min(pos_arr[:, 0]))
print("Depth (Z):", np.max(pos_arr[:, 2]) - np.min(pos_arr[:, 2]))

# Check if there are vertices above y = 1.62 (hair region)
top_verts = pos_arr[pos_arr[:, 1] > 1.60]
print("Vertices above y=1.60m:", len(top_verts))
print("Top-most vertex:", pos_arr[np.argmax(pos_arr[:, 1])])
