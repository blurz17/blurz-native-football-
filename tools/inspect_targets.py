import os
from PIL import Image

fc_path = r'E:\Downlaod\football asset\players\fc mobile 2027'
for f in sorted(os.listdir(fc_path)):
    p = os.path.join(fc_path, f)
    with Image.open(p) as img:
        print(f'=== {f} ===')
        print(f'Dimensions: {img.size}, Mode: {img.mode}')
        # Check aspect ratio
        w, h = img.size
        print(f'Aspect ratio: {w/h:.3f}')
