import os
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def extract_dirt_bike_rider():
    logo_dir = r"d:\My Applications\RIDERsYNK\Logo"
    bike_src = os.path.join(logo_dir, "Screenshot 2026-09-15 135035.png")
    bike_cutout_path = os.path.join(logo_dir, "dirt_bike_rider_cutout.png")

    if not os.path.exists(bike_src):
        print(f"Source file not found: {bike_src}")
        return None

    img = Image.open(bike_src).convert("RGBA")
    arr = np.array(img, dtype=np.uint8)

    r, g, b, a = arr[:, :, 0], arr[:, :, 1], arr[:, :, 2], arr[:, :, 3]

    # Key out white/light grey grid background (R > 210, G > 210, B > 210)
    bg_mask = (r > 210) & (g > 210) & (b > 210)
    arr[bg_mask, 3] = 0

    cutout = Image.fromarray(arr, "RGBA")
    
    # Crop bounding box of non-transparent pixels
    bbox = cutout.getbbox()
    if bbox:
        cutout = cutout.crop(bbox)

    cutout.save(bike_cutout_path, "PNG")
    print(f"Successfully extracted Dirt Bike Rider cutout to {bike_cutout_path}")
    return cutout

def build_final_logo():
    logo_dir = r"d:\My Applications\RIDERsYNK\Logo"
    
    dest_trans = os.path.join(logo_dir, "tts_app_logo_transparent.png")
    dest_png = os.path.join(logo_dir, "tts_app_logo.png")
    dest_jpg = os.path.join(logo_dir, "tts_app_logo.jpg")
    dest_drawable = r"d:\My Applications\RIDERsYNK\app\src\main\res\drawable\ic_app_logo_badge.png"
    dest_drawable_sq = r"d:\My Applications\RIDERsYNK\app\src\main\res\drawable\ic_app_logo_square.png"
    
    artifact_output = r"C:\Users\Mohammed Ahmed\.gemini\antigravity-ide\brain\b5ecd0cf-4642-4da2-864e-3f41bbb131e0\redesigned_dirt_bike_logo.png"

    # Extract bike cutout
    bike_cutout = extract_dirt_bike_rider()

    size = 1024
    cx, cy = size // 2, size // 2

    # 1. Canvas
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    r_outer_border = 490
    r_outer_ring = 485
    r_inner_ring = 310

    # Solid outer gold border base
    draw.ellipse([cx - r_outer_border, cy - r_outer_border, cx + r_outer_border, cy + r_outer_border], 
                 fill=(245, 158, 11, 255))

    # Outer ring pattern (15 photo B&W ink painting collage)
    ring_bg_path = os.path.join(logo_dir, "travel_ring_bg_pattern.png")
    if os.path.exists(ring_bg_path):
        ring_img = Image.open(ring_bg_path).convert("RGBA").resize((size, size))
    else:
        ring_img = Image.new("RGBA", (size, size), (30, 41, 59, 255))

    ring_mask = Image.new("L", (size, size), 0)
    draw_rm = ImageDraw.Draw(ring_mask)
    draw_rm.ellipse([cx - r_outer_ring, cy - r_outer_ring, cx + r_outer_ring, cy + r_outer_ring], fill=255)
    draw_rm.ellipse([cx - r_inner_ring, cy - r_inner_ring, cx + r_inner_ring, cy + r_inner_ring], fill=0)

    img.paste(ring_img, (0, 0), ring_mask)

    # Clean outer gold stroke border
    draw.ellipse([cx - r_outer_ring, cy - r_outer_ring, cx + r_outer_ring, cy + r_outer_ring], 
                 outline=(245, 158, 11, 255), width=8)

    # 2. Inner Circle Background (Merged Mountains & 5-pin Route Path)
    merged_bg_path = os.path.join(logo_dir, "inner_circle_merged_bg.png")
    if os.path.exists(merged_bg_path):
        inner_bg = Image.open(merged_bg_path).convert("RGBA")
    else:
        inner_bg = Image.new("RGBA", (1000, 1000), (15, 23, 42, 255))

    inner_circle_size = r_inner_ring * 2
    inner_cropped = inner_bg.resize((inner_circle_size, inner_circle_size))

    inner_mask = Image.new("L", (inner_circle_size, inner_circle_size), 0)
    draw_im = ImageDraw.Draw(inner_mask)
    draw_im.ellipse([0, 0, inner_circle_size, inner_circle_size], fill=255)

    img.paste(inner_cropped, (cx - r_inner_ring, cy - r_inner_ring), inner_mask)

    # 3. Paste extracted Dirt Bike Rider Graphic into lower inner circle (replaces old icon!)
    if bike_cutout:
        bw, bh = bike_cutout.size
        target_w = 420
        target_h = int(bh * (target_w / float(bw)))
        bike_resized = bike_cutout.resize((target_w, target_h), resample=Image.BICUBIC)
        
        # Position in lower inner circle area
        bx = cx - target_w // 2 + 10
        by = cy + 120 - target_h // 2
        
        # Paste bike graphic transparently over inner circle background
        img.paste(bike_resized, (bx, by), bike_resized)

    # Re-draw Inner Gold Circle Border
    draw.ellipse([cx - r_inner_ring, cy - r_inner_ring, cx + r_inner_ring, cy + r_inner_ring], 
                 outline=(245, 158, 11, 255), width=6)

    # 4. 3D TTS Tech Monogram in upper inner circle
    try:
        font_tts = ImageFont.truetype("arialbd.ttf", 90)
    except:
        font_tts = ImageFont.load_default()

    text_tts = "TTS"
    bbox = font_tts.getbbox(text_tts)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]

    tx = cx - tw // 2
    ty = cy - 120 - th // 2

    # Drop shadow
    for offset in range(6, 0, -1):
        draw.text((tx + offset, ty + offset), text_tts, font=font_tts, fill=(5, 10, 20, 240))

    # Bold Gold fill with white stroke
    draw.text((tx, ty), text_tts, font=font_tts, fill=(245, 158, 11, 255), stroke_width=4, stroke_fill=(255, 255, 255, 255))

    # 5. REDESIGNED TEXT FORMAT IN OUTER RING (Clean, Premium, High-Legibility Typography)
    try:
        font_ring = ImageFont.truetype("arialbd.ttf", 36)
    except:
        font_ring = ImageFont.load_default()

    # Upper Ring Text: "TRAVELER TRAVEL SYNK" (Clean bold white text with dark drop shadow)
    top_text = "TRAVELER TRAVEL SYNK"
    r_text_top = (r_outer_ring + r_inner_ring) // 2 + 6
    angle_step_top = 5.6
    start_angle_top = -90 - ((len(top_text) - 1) * angle_step_top) / 2

    for i, char in enumerate(top_text):
        angle_deg = start_angle_top + i * angle_step_top
        angle_rad = math.radians(angle_deg)

        char_x = cx + r_text_top * math.cos(angle_rad)
        char_y = cy + r_text_top * math.sin(angle_rad)

        char_img = Image.new("RGBA", (80, 80), (0, 0, 0, 0))
        cdraw = ImageDraw.Draw(char_img)
        
        # Shadow
        cdraw.text((26, 16), char, font=font_ring, fill=(10, 15, 30, 220))
        # White fill
        cdraw.text((24, 14), char, font=font_ring, fill=(255, 255, 255, 255))

        rot = char_img.rotate(angle_deg + 90, expand=False, resample=Image.BICUBIC)
        img.paste(rot, (int(char_x - 40), int(char_y - 40)), rot)

    # Lower Ring Text: "SYNCHRONIZED GROUP TRAVEL" (Clean Gold text with dark shadow)
    bot_text = "SYNCHRONIZED GROUP TRAVEL"
    r_text_bot = (r_outer_ring + r_inner_ring) // 2 - 6
    angle_step_bot = 4.8
    start_angle_bot = 90 + ((len(bot_text) - 1) * angle_step_bot) / 2

    for i, char in enumerate(bot_text):
        angle_deg = start_angle_bot - i * angle_step_bot
        angle_rad = math.radians(angle_deg)

        char_x = cx + r_text_bot * math.cos(angle_rad)
        char_y = cy + r_text_bot * math.sin(angle_rad)

        char_img = Image.new("RGBA", (80, 80), (0, 0, 0, 0))
        cdraw = ImageDraw.Draw(char_img)
        
        # Shadow
        cdraw.text((26, 16), char, font=font_ring, fill=(10, 15, 30, 220))
        # Gold fill
        cdraw.text((24, 14), char, font=font_ring, fill=(245, 158, 11, 255))

        rot = char_img.rotate(angle_deg - 90, expand=False, resample=Image.BICUBIC)
        img.paste(rot, (int(char_x - 40), int(char_y - 40)), rot)

    # Save final logo outputs
    img.save(dest_trans, "PNG")
    img.save(dest_png, "PNG")
    img.convert("RGB").save(dest_jpg, "JPEG", quality=95)
    img.resize((512, 512)).save(dest_drawable, "PNG")
    img.resize((512, 512)).save(dest_drawable_sq, "PNG")
    img.save(artifact_output, "PNG")

    print("Successfully created redesigned logo with Dirt Bike Rider graphic and clean typography!")

if __name__ == "__main__":
    build_final_logo()
