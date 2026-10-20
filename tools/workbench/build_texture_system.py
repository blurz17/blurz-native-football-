import os
import numpy as np
from PIL import Image, ImageDraw, ImageFont

def build_texture_system():
    out_dir = r'tools/workbench/texture_system'
    os.makedirs(out_dir, exist_ok=True)
    
    W = 2048
    H = 2048
    
    print("=== BUILDING UV TEMPLATE & RGB MASK SYSTEM (2048x2048) ===")
    
    # 1. UV Template Paintable
    # Background dark grid
    template = Image.new('RGBA', (W, H), (26, 34, 45, 255))
    draw = ImageDraw.Draw(template)
    
    # Draw quadrant division lines
    mid_x = W // 2
    mid_y = H // 2
    
    # Grid lines every 128 px
    for x in range(0, W, 128):
        draw.line([(x, 0), (x, H)], fill=(40, 50, 65, 255), width=1)
    for y in range(0, H, 128):
        draw.line([(0, y), (W, y)], fill=(40, 50, 65, 255), width=1)
        
    # Main quadrant separators
    draw.line([(mid_x, 0), (mid_x, H)], fill=(0, 229, 255, 255), width=3)
    draw.line([(0, mid_y), (W, mid_y)], fill=(0, 229, 255, 255), width=3)
    
    # Zone Labels & Boxes
    # Top-Left: Zone A - Shirt / Jersey (U: 0.0 - 0.5, V: 0.5 - 1.0 -> Pixels: X: 0-1024, Y: 0-1024)
    # Top-Right: Zone B - Head & Face (U: 0.5 - 1.0, V: 0.5 - 1.0 -> Pixels: X: 1024-2048, Y: 0-1024)
    # Bottom-Left: Zone C - Shorts & Limbs (U: 0.0 - 0.5, V: 0.0 - 0.5 -> Pixels: X: 0-1024, Y: 1024-2048)
    # Bottom-Right: Zone D - Boots & Accessories (U: 0.5 - 1.0, V: 0.0 - 0.5 -> Pixels: X: 1024-2048, Y: 1024-2048)
    
    # Load original extracted textures to composite into a base diffuse map
    raw_tex_dir = r'C:\Users\hp\.gemini\antigravity\brain\c6f8d783-caab-4642-8c0a-b02059060228\scratch\yamal_textures'
    base_diffuse = Image.new('RGB', (W, H), (128, 128, 128))
    
    # Paste outfit_top (tex_12.jpg) in Top-Left
    top_path = os.path.join(raw_tex_dir, 'tex_12.jpg')
    if os.path.exists(top_path):
        with Image.open(top_path) as img:
            base_diffuse.paste(img.resize((1024, 1024), Image.Resampling.LANCZOS), (0, 0))
            
    # Paste AvatarHead (tex_2.jpg) in Top-Right
    head_path = os.path.join(raw_tex_dir, 'tex_2.jpg')
    if os.path.exists(head_path):
        with Image.open(head_path) as img:
            base_diffuse.paste(img.resize((1024, 1024), Image.Resampling.LANCZOS), (1024, 0))
            
    # Paste outfit_bottom (tex_8.jpg) in Bottom-Left
    bottom_path = os.path.join(raw_tex_dir, 'tex_8.jpg')
    if os.path.exists(bottom_path):
        with Image.open(bottom_path) as img:
            base_diffuse.paste(img.resize((1024, 1024), Image.Resampling.LANCZOS), (0, 1024))
            
    # Paste outfit_shoes (tex_10.jpg) in Bottom-Right
    shoes_path = os.path.join(raw_tex_dir, 'tex_10.jpg')
    if os.path.exists(shoes_path):
        with Image.open(shoes_path) as img:
            base_diffuse.paste(img.resize((1024, 1024), Image.Resampling.LANCZOS), (1024, 1024))
            
    base_diffuse_path = os.path.join(out_dir, 'universal_base_diffuse.png')
    base_diffuse.save(base_diffuse_path)
    print(f"Saved Base Diffuse Atlas to {base_diffuse_path}")
    
    # 2. Build the RGB Color Tinting Mask
    # Red   = Primary Kit color
    # Green = Secondary Accent color
    # Blue  = Skin Tone
    # Black = Static (Studs, eyes, sponsor, numbers)
    
    mask_arr = np.zeros((H, W, 3), dtype=np.uint8)
    
    # Zone A (Top-Left: Jersey):
    # Most of jersey is Primary (Red), collar and sleeve cuffs are Secondary (Green)
    mask_arr[100:924, 100:924, 0] = 255 # Primary Red
    # Sleeve stripes & collar band
    mask_arr[40:100, 100:924, 1] = 255  # Secondary Green collar
    mask_arr[850:924, 100:400, 1] = 255 # Secondary Green sleeve trim
    mask_arr[850:924, 624:924, 1] = 255 # Secondary Green sleeve trim
    
    # Zone B (Top-Right: Head & Skin):
    # Face and neck are Skin (Blue)
    mask_arr[50:980, 1074:1990, 2] = 255 # Skin Tone Blue
    # Eye sockets / pupils left neutral black (so whites and pupils are preserved)
    mask_arr[350:450, 1300:1450, :] = 0
    mask_arr[350:450, 1600:1750, :] = 0
    # Scalp hairline
    mask_arr[50:200, 1200:1850, :] = 0 # Hair region is black or alpha
    
    # Zone C (Bottom-Left: Shorts & Limbs):
    # Main shorts body is Primary (Red)
    mask_arr[1050:1800, 50:980, 0] = 255 # Shorts Red
    # Shorts side trim is Secondary (Green)
    mask_arr[1050:1800, 50:150, 1] = 255
    mask_arr[1050:1800, 880:980, 1] = 255
    # Exposed leg/knee skin zone in bottom-left
    mask_arr[1820:2020, 50:980, 2] = 255 # Knee/Calf skin
    
    # Zone D (Bottom-Right: Boots & Socks):
    # Socks upper is Primary (Red)
    mask_arr[1050:1450, 1050:2000, 0] = 255
    # Socks stripes is Secondary (Green)
    mask_arr[1050:1150, 1050:2000, 1] = 255
    # Boot body is neutral or accent
    mask_arr[1500:2000, 1050:2000, 1] = 180 # Boots accent
    
    rgb_mask_img = Image.fromarray(mask_arr, 'RGB')
    rgb_mask_path = os.path.join(out_dir, 'universal_rgb_mask.png')
    rgb_mask_img.save(rgb_mask_path)
    print(f"Saved RGB Mask to {rgb_mask_path}")
    
    # 3. Draw Annotations on the Paintable UV Template
    draw.rectangle([(20, 20), (1004, 1004)], outline=(255, 61, 0, 200), width=4)
    draw.text((40, 40), "ZONE A: SHIRT / JERSEY\n(Front, Back, Sleeves, Collar)\nTints: Red=Primary, Green=Trim", fill=(255, 255, 255, 255))
    
    draw.rectangle([(1044, 20), (2028, 1004)], outline=(0, 229, 255, 200), width=4)
    draw.text((1064, 40), "ZONE B: HEAD & FACE\n(Facial features, Neck, Scalp, Ears)\nTints: Blue=Skin Tone", fill=(255, 255, 255, 255))
    
    draw.rectangle([(20, 1044), (1004, 2028)], outline=(255, 214, 0, 200), width=4)
    draw.text((40, 1064), "ZONE C: SHORTS & LEGS\n(Shorts panels, Inner thighs, Knees)\nTints: Red=Main Shorts, Green=Side Stripe", fill=(255, 255, 255, 255))
    
    draw.rectangle([(1044, 1044), (2028, 2028)], outline=(0, 230, 118, 200), width=4)
    draw.text((1064, 1064), "ZONE D: FOOTWEAR & SOCKS\n(Socks, Cleats, Shin guards, Badges)\nTints: Red=Socks Main, Green=Accents", fill=(255, 255, 255, 255))
    
    template_path = os.path.join(out_dir, 'uv_template_paintable.png')
    template.save(template_path)
    print(f"Saved Paintable UV Template to {template_path}")
    
    # Copy generated assets to conversation artifact directory so they are viewable
    art_dir = r'C:\Users\hp\.gemini\antigravity\brain\c6f8d783-caab-4642-8c0a-b02059060228\texture_system'
    os.makedirs(art_dir, exist_ok=True)
    import shutil
    shutil.copy2(base_diffuse_path, os.path.join(art_dir, 'universal_base_diffuse.png'))
    shutil.copy2(rgb_mask_path, os.path.join(art_dir, 'universal_rgb_mask.png'))
    shutil.copy2(template_path, os.path.join(art_dir, 'uv_template_paintable.png'))
    print("Mirrored texture assets to artifact directory for display")

if __name__ == '__main__':
    build_texture_system()
