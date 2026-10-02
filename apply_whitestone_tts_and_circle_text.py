import os
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFont

def get_script_font(size):
    candidates = [
        r"C:\Windows\Fonts\LCALLIG.TTF",  # Lucida Calligraphy Formal
        r"C:\Windows\Fonts\KUNSTLER.TTF", # Kunstler Formal Script
        r"C:\Windows\Fonts\FRSCRIPT.TTF", # French Script MT
        r"C:\Windows\Fonts\VLADIMIR.TTF", # Vladimir Script
        r"C:\Windows\Fonts\georgiai.ttf"   # Georgia Italic
    ]
    for c in candidates:
        if os.path.exists(c):
            try:
                return ImageFont.truetype(c, size)
            except:
                pass
    return ImageFont.load_default()

def draw_arc_text_supersampled(draw_img, text, center, radius, start_angle, end_angle, font, fill_color, stroke_color=(15, 23, 42, 255), is_bottom=False):
    scale = 4
    high_res_size = (draw_img.width * scale, draw_img.height * scale)
    cx, cy = center[0] * scale, center[1] * scale
    r_text = radius * scale

    high_img = Image.new("RGBA", high_res_size, (0, 0, 0, 0))
    font_scaled_size = int(font.size * scale)
    
    font_path = getattr(font, "path", None)
    if font_path and os.path.exists(font_path):
        try:
            font_scaled = ImageFont.truetype(font_path, font_scaled_size)
        except:
            font_scaled = font
    else:
        font_scaled = font

    num_chars = len(text)
    if num_chars == 0:
        return draw_img

    total_angle = abs(end_angle - start_angle)
    angle_step = total_angle / max(num_chars - 1, 1)

    for i, char in enumerate(text):
        if is_bottom:
            angle_deg = start_angle - i * angle_step
        else:
            angle_deg = start_angle + i * angle_step

        angle_rad = math.radians(angle_deg)

        char_x = cx + r_text * math.cos(angle_rad)
        char_y = cy + r_text * math.sin(angle_rad)

        char_w = int(font_scaled_size * 2.2)
        char_h = int(font_scaled_size * 2.2)
        char_canvas = Image.new("RGBA", (char_w, char_h), (0, 0, 0, 0))
        cdraw = ImageDraw.Draw(char_canvas)

        bbox = font_scaled.getbbox(char)
        cw = bbox[2] - bbox[0]
        ch = bbox[3] - bbox[1]

        # Draw stroke for high legibility
        s_w = int(3 * scale)
        cdraw.text((char_w // 2 - cw // 2, char_h // 2 - ch // 2), char, font=font_scaled, 
                   fill=fill_color, stroke_width=s_w, stroke_fill=stroke_color)

        if is_bottom:
            rot_angle = angle_deg - 90
        else:
            rot_angle = angle_deg + 90

        rotated_char = char_canvas.rotate(rot_angle, expand=False, resample=Image.BICUBIC)
        high_img.paste(rotated_char, (int(char_x - char_w // 2), int(char_y - char_h // 2)), rotated_char)

    smoothed = high_img.resize(draw_img.size, resample=Image.LANCZOS)
    draw_img = Image.alpha_composite(draw_img, smoothed)
    return draw_img

def build_whitestone_tts_logo():
    logo_dir = r"d:\My Applications\RIDERsYNK\Logo"
    
    dest_trans = os.path.join(logo_dir, "tts_app_logo_transparent.png")
    dest_png = os.path.join(logo_dir, "tts_app_logo.png")
    dest_jpg = os.path.join(logo_dir, "tts_app_logo.jpg")
    dest_drawable = r"d:\My Applications\RIDERsYNK\app\src\main\res\drawable\ic_app_logo_badge.png"
    dest_drawable_sq = r"d:\My Applications\RIDERsYNK\app\src\main\res\drawable\ic_app_logo_square.png"
    
    artifact_output = r"C:\Users\Mohammed Ahmed\.gemini\antigravity-ide\brain\b5ecd0cf-4642-4da2-864e-3f41bbb131e0\whitestone_tts_logo.png"

    size = 1024
    cx, cy = size // 2, size // 2

    # 1. Base Canvas
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

    # 3. Dirt Bike Rider Graphic into lower inner circle
    bike_cutout_path = os.path.join(logo_dir, "dirt_bike_rider_cutout.png")
    if os.path.exists(bike_cutout_path):
        bike_cutout = Image.open(bike_cutout_path).convert("RGBA")
        bw, bh = bike_cutout.size
        target_w = 420
        target_h = int(bh * (target_w / float(bw)))
        bike_resized = bike_cutout.resize((target_w, target_h), resample=Image.BICUBIC)
        
        bx = cx - target_w // 2 + 10
        by = cy + 120 - target_h // 2
        img.paste(bike_resized, (bx, by), bike_resized)

    # Inner Gold Circle Border
    draw.ellipse([cx - r_inner_ring, cy - r_inner_ring, cx + r_inner_ring, cy + r_inner_ring], 
                 outline=(245, 158, 11, 255), width=6)

    # 4. WHITESTONE FORMAL CALLIGRAPHIC SCRIPT MONOGRAM FOR "TTS"
    font_tts_script = get_script_font(110)

    text_tts = "TTS"
    bbox = font_tts_script.getbbox(text_tts)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]

    tx = cx - tw // 2
    ty = cy - 120 - th // 2

    # Drop shadow
    for offset in range(6, 0, -1):
        draw.text((tx + offset, ty + offset), text_tts, font=font_tts_script, fill=(5, 10, 20, 240))

    # Whitestone Formal Calligraphic Script style for TTS
    draw.text((tx, ty), text_tts, font=font_tts_script, fill=(245, 158, 11, 255), stroke_width=4, stroke_fill=(255, 255, 255, 255))

    # 5. WHITESTONE FORMAL CALLIGRAPHIC SCRIPT CIRCLE WRAPPED TEXT FOR OUTER RING
    font_script = get_script_font(42)

    r_text_mid = (r_outer_ring + r_inner_ring) // 2

    # Top Arc: "Traveler Travel Synk" (Circle Form Wrapped)
    top_text = "Traveler Travel Synk"
    img = draw_arc_text_supersampled(img, top_text, (cx, cy), r_text_mid + 4, -145, -35, font_script, 
                                     fill_color=(255, 255, 255, 255), stroke_color=(15, 23, 42, 255), is_bottom=False)

    # Bottom Arc: "Synchronized Group Travel" (Circle Form Wrapped)
    bot_text = "Synchronized Group Travel"
    img = draw_arc_text_supersampled(img, bot_text, (cx, cy), r_text_mid - 4, 145, 35, font_script, 
                                     fill_color=(245, 158, 11, 255), stroke_color=(15, 23, 42, 255), is_bottom=True)

    # Save final logo outputs
    img.save(dest_trans, "PNG")
    img.save(dest_png, "PNG")
    img.convert("RGB").save(dest_jpg, "JPEG", quality=95)
    img.resize((512, 512)).save(dest_drawable, "PNG")
    img.resize((512, 512)).save(dest_drawable_sq, "PNG")
    img.save(artifact_output, "PNG")

    print("Successfully built logo with Whitestone Formal TTS monogram & circle wrapped text!")

if __name__ == "__main__":
    build_whitestone_tts_logo()
