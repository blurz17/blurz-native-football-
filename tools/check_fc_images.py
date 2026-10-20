import os
from PIL import Image
import numpy as np

fc_path = r'E:\Downlaod\football asset\players\fc mobile 2027'
for f in sorted(os.listdir(fc_path)):
    p = os.path.join(fc_path, f)
    with Image.open(p) as img:
        arr = np.array(img)
        print(f"=== {f} ===")
        print(f"Size: {img.size}, Mode: {img.mode}")
        print(f"Mean RGB: {np.mean(arr, axis=(0,1)).round(1)}")
        print(f"Std RGB: {np.std(arr, axis=(0,1)).round(1)}")
