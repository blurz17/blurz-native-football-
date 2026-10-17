import sys
import os
import re

try:
    import bpy
    IN_BLENDER = True
except ImportError:
    IN_BLENDER = False

def run():
    print("=== Modernizing Player Model in Blender ===")
    base_dir = r"E:\MY-PROJECTS\android native football"
    ase_path = os.path.join(base_dir, "assets", "media", "objects", "players", "models", "fullbody.ase")
    out_fbx = os.path.join(base_dir, "assets", "media", "objects", "players", "models", "fullbody.fbx")
    out_android_fbx = os.path.join(base_dir, "platform", "android", "app", "src", "main", "assets", "media", "objects", "players", "models", "fullbody.fbx")

    # Clear blender scene
    bpy.ops.wm.read_factory_settings(use_empty=True)

    with open(ase_path, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()

    geom_blocks = content.split("*GEOMOBJECT {")
    print(f"Found {len(geom_blocks)-1} GEOMOBJECT blocks in fullbody.ase")

    # Setup materials
    def get_or_create_mat(mat_name, tex_rel):
        mat = bpy.data.materials.new(name=mat_name)
        mat.use_nodes = True
        bsdf = mat.node_tree.nodes.get("Principled BSDF")
        if bsdf:
            bsdf.inputs['Roughness'].default_value = 0.6
            bsdf.inputs['Specular IOR Level'].default_value = 0.2
        return mat

    kit_mat = get_or_create_mat("Mat_RealMadrid_Kit", "kit_template.png")
    skin_mat = get_or_create_mat("Mat_Skin", "skin.jpg")
    shoe_mat = get_or_create_mat("Mat_Shoe", "shoe.jpg")
    sole_mat = get_or_create_mat("Mat_Sole", "shoe.jpg")

    created_objs = []

    for block in geom_blocks[1:]:
        name_match = re.search(r'\*NODE_NAME\s+"([^"]+)"', block)
        if not name_match: continue
        node_name = name_match.group(1)

        # Parse vertices
        v_matches = re.findall(r'\*MESH_VERTEX\s+(\d+)\s+([\-\d\.]+)\s+([\-\d\.]+)\s+([\-\d\.]+)', block)
        vertices = [(float(x), float(y), float(z)) for idx, x, y, z in sorted(v_matches, key=lambda m: int(m[0]))]

        # Parse faces
        f_matches = re.findall(r'\*MESH_FACE\s+(\d+):\s+A:\s+(\d+)\s+B:\s+(\d+)\s+C:\s+(\d+)', block)
        faces = [(int(a), int(b), int(c)) for idx, a, b, c in sorted(f_matches, key=lambda m: int(m[0]))]

        # Parse UV
        tv_matches = re.findall(r'\*MESH_TVERT\s+(\d+)\s+([\-\d\.]+)\s+([\-\d\.]+)', block)
        tverts = [(float(u), float(v)) for idx, u, v in sorted(tv_matches, key=lambda m: int(m[0]))]

        tf_matches = re.findall(r'\*MESH_TFACE\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)', block)
        tfaces = [(int(a), int(b), int(c)) for idx, a, b, c in sorted(tf_matches, key=lambda m: int(m[0]))]

        mesh = bpy.data.meshes.new(name=node_name + "_Mesh")
        mesh.from_pydata(vertices, [], faces)
        mesh.update()

        # Enable smooth shading on all polygons
        for poly in mesh.polygons:
            poly.use_smooth = True

        # Assign UVs
        if tverts and tfaces and len(tfaces) == len(faces):
            uv_layer = mesh.uv_layers.new(name="UVMap")
            for poly_idx, poly in enumerate(mesh.polygons):
                tf = tfaces[poly_idx]
                for loop_idx, loop in enumerate(poly.loop_indices):
                    tvert_idx = tf[loop_idx]
                    uv = tverts[tvert_idx]
                    uv_layer.data[loop].uv = (uv[0], uv[1])

        obj = bpy.data.objects.new(name=node_name, object_data=mesh)
        bpy.context.collection.objects.link(obj)

        # Assign material based on node name
        lower_name = node_name.lower()
        if "sole" in lower_name:
            obj.data.materials.append(sole_mat)
        elif "shoe" in lower_name:
            obj.data.materials.append(shoe_mat)
        elif "sock" in lower_name or "body" in lower_name:
            obj.data.materials.append(kit_mat)
        else:
            obj.data.materials.append(skin_mat)

        created_objs.append(obj)
        print(f"Created {node_name}: {len(vertices)} verts, {len(faces)} faces (Smooth Shaded)")

    # Select all created player parts
    bpy.ops.object.select_all(action='DESELECT')
    for obj in created_objs:
        obj.select_set(True)
    bpy.context.view_layer.objects.active = created_objs[-1]

    # Export modern FBX with EDGE smoothing for clean organic normal interpolation
    bpy.ops.export_scene.fbx(
        filepath=out_fbx,
        use_selection=True,
        apply_scale_options='FBX_SCALE_ALL',
        bake_space_transform=True,
        mesh_smooth_type='EDGE'
    )
    print(f"Exported modern FBX to: {out_fbx} (Size: {os.path.getsize(out_fbx)} bytes)")

    # Copy to Android assets
    with open(out_fbx, 'rb') as src, open(out_android_fbx, 'wb') as dst:
        dst.write(src.read())
    print(f"Copied modern FBX to Android assets: {out_android_fbx}")

if __name__ == '__main__':
    run()
