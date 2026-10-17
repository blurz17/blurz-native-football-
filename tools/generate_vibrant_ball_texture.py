from PIL import Image, ImageDraw, ImageFont
import math

def create_al_rihla_texture(width=2048, height=1024):
    # Equirectangular UV texture for a sphere
    im = Image.new("RGB", (width, height), (242, 244, 248)) # crisp football leather white
    draw = ImageDraw.Draw(im)

    # 1. Subtle leather grain texture
    import random
    random.seed(42)
    # We can add subtle hexagonal micro-texture or shading

    # 2. Panel Seams (Al Rihla 20-panel Speedshell inspired curved seams)
    # Draw dark grey/slate seam grooves
    seam_color = (65, 70, 80)
    seam_width = 6

    # Draw curved panel boundaries across the sphere
    # Latitude bands & curved longitudinal arcs representing panels
    for lat in [height * 0.25, height * 0.5, height * 0.75]:
        for x in range(0, width, 8):
            y_wave = lat + math.sin(x * 0.02) * 35.0
            draw.ellipse((x - 2, y_wave - 2, x + 2, y_wave + 2), fill=seam_color)

    # Triangular / triangular-spearhead panels
    num_panels_x = 8
    panel_w = width / num_panels_x

    # Vibrant Colors of Al Rihla:
    # Cyan: (0, 180, 245)
    # Royal Blue: (20, 50, 180)
    # Vibrant Magenta/Pink: (235, 30, 120)
    # Solar Gold/Yellow: (255, 200, 0)
    # Black/Dark Navy: (15, 20, 45)

    for i in range(num_panels_x):
        cx = i * panel_w + panel_w * 0.5
        # Alternating top, middle, bottom motifs
        y_positions = [height * 0.28, height * 0.5, height * 0.72] if i % 2 == 0 else [height * 0.38, height * 0.62]
        
        for cy in y_positions:
            # Draw multi-layer dynamic spearhead ribbon
            size = panel_w * 0.38
            
            # Layer 1: Outer dark navy shadow / outline
            pts_outer = [
                (cx - size * 0.8, cy),
                (cx - size * 0.2, cy - size * 0.45),
                (cx + size * 0.8, cy),
                (cx + size * 0.2, cy + size * 0.45)
            ]
            draw.polygon(pts_outer, fill=(20, 30, 60))

            # Layer 2: Vibrant Cyan
            pts_cyan = [
                (cx - size * 0.7, cy),
                (cx - size * 0.15, cy - size * 0.38),
                (cx + size * 0.7, cy),
                (cx + size * 0.15, cy + size * 0.38)
            ]
            draw.polygon(pts_cyan, fill=(0, 190, 250))

            # Layer 3: Vibrant Magenta ribbon slice
            pts_pink = [
                (cx - size * 0.5, cy - size * 0.05),
                (cx - size * 0.1, cy - size * 0.32),
                (cx + size * 0.4, cy - size * 0.05),
                (cx, cy + size * 0.1)
            ]
            draw.polygon(pts_pink, fill=(240, 25, 115))

            # Layer 4: Solar Gold accent stripe
            pts_gold = [
                (cx - size * 0.3, cy + size * 0.05),
                (cx + size * 0.5, cy + size * 0.05),
                (cx + size * 0.2, cy + size * 0.30),
                (cx - size * 0.1, cy + size * 0.25)
            ]
            draw.polygon(pts_gold, fill=(255, 210, 0))

            # Layer 5: Speed line stripes
            for s in range(4):
                lx = cx - size * 0.4 + s * (size * 0.25)
                draw.line([(lx, cy - size * 0.25), (lx + size * 0.2, cy + size * 0.25)], fill=(255, 255, 255), width=4)

    # 3. Add prominent Adidas 3-Bars Logo on 2 opposite sides
    for logo_x in [width * 0.25, width * 0.75]:
        logo_y = height * 0.5
        # 3 angled bars of increasing height
        bar_color = (20, 25, 35)
        bar_w = 16
        # Bar 1 (short)
        draw.polygon([(logo_x - 45, logo_y + 25), (logo_x - 30, logo_y + 25), (logo_x - 15, logo_y - 5), (logo_x - 30, logo_y - 5)], fill=bar_color)
        # Bar 2 (medium)
        draw.polygon([(logo_x - 20, logo_y + 25), (logo_x - 5, logo_y + 25), (logo_x + 15, logo_y - 25), (logo_x, logo_y - 25)], fill=bar_color)
        # Bar 3 (tall)
        draw.polygon([(logo_x + 5, logo_y + 25), (logo_x + 20, logo_y + 25), (logo_x + 45, logo_y - 45), (logo_x + 30, logo_y - 45)], fill=bar_color)

    # 4. Save to files
    out_path = r"E:\MY-PROJECTS\android native football\scratch\al_rihla_vibrant.png"
    im.save(out_path, quality=95)
    print(f"Generated vibrant match ball texture: {out_path}")

create_al_rihla_texture()
