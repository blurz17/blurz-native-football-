def update_asset_list(path):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    new_files = '''  "media/objects/players/textures/yamal/yamal_body.jpg",
  "media/objects/players/textures/yamal/yamal_bottom.jpg",
  "media/objects/players/textures/yamal/yamal_eye.jpg",
  "media/objects/players/textures/yamal/yamal_head.jpg",
  "media/objects/players/textures/yamal/yamal_shoes.jpg",
  "media/objects/players/textures/yamal/yamal_top.jpg",
  nullptr'''

    if 'media/objects/players/textures/yamal/yamal_body.jpg' not in content:
        content = content.replace('"media/textures/stadium/greenish_floor.png",\n  nullptr', '"media/textures/stadium/greenish_floor.png",\n' + new_files)
        content = content.replace('"media/textures/stadium/greenish_floor.png",\r\n  nullptr', '"media/textures/stadium/greenish_floor.png",\r\n' + new_files)

    new_dirs = '''  "media/objects/players/textures/yamal",
  nullptr'''
    if 'media/objects/players/textures/yamal' not in content:
        content = content.replace('"media/textures/stadium",\n  nullptr', '"media/textures/stadium",\n' + new_dirs)
        content = content.replace('"media/textures/stadium",\r\n  nullptr', '"media/textures/stadium",\r\n' + new_dirs)

    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print('Updated', path)

update_asset_list('platform/android/asset_list.hpp')
update_asset_list('engine/asset_list.hpp')
