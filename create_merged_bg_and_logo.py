import os
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFont

def build_merged_bg_and_logo():
    logo_dir = r"d:\My Applications\RIDERsYNK\Logo"
    
    dest_inner_bg_png = os.path.join(logo_dir, "inner_circle_bg_pattern.png")
    dest_inner_bg_jpg = os.path.join(logo_dir, "inner_circle_bg_pattern.jpg")
    dest_merged_bg_png = os.path.join(logo_dir, "inner_circle_merged_bg.png")
    
    dest_trans = os.path.join(logo_dir, "tts_app_logo_transparent.png")
    dest_png = os.path.join(logo_dir, "tts_app_logo.png")
    dest_jpg = os.path.join(logo_dir, "tts_app_logo.jpg")
    dest_drawable = r"d:\My Applications\RIDERsYNK\app\src\main\res\drawable\ic_app_logo_badge.png"
    
    bg_artifact_output = r"C:\Users\Mohammed Ahmed\.gemini\antigravity-ide\brain\b5ecd0cf-4642-4da2-864e-3f41bbb131e0\standalone_inner_circle_merged_bg.png"
    logo_artifact_output = r"C:\Users\Mohammed Ahmed\.gemini\antigravity-ide\brain\b5ecd0cf-4642-4da2-864e-3f41bbb131e0\final_merged_bg_logo.png"

    # STEP 1: CREATE CRISP STANDALONE MERGED INNER BACKGROUND IMAGE WITH BIGGER LOCATION PINS & ZERO PURPLE LINES
    bg_size = 1000
    inner_bg = Image.new("RGBA", (bg_size, bg_size), (15, 23, 42, 255)) # Dark navy #0F172A

    # 1A. Load & Colorize Mountain Peaks (Sunburst Gold #F59E0B)
    mountains_path = os.path.join(logo_dir, "mountain_peaks_sketch.png")
    if os.path.exists(mountains_path):
        m_img = Image.open(mountains_path).convert("RGBA")
        m_arr = np.array(m_img)
        r_m, g_m, b_m, a_m = m_arr[:, :, 0], m_arr[:, :, 1], m_arr[:, :, 2], m_arr[:, :, 3]
        dark_mask = (a_m > 50) & (r_m < 180) & (g_m < 180) & (b_m < 180)
        
        m_arr[dark_mask, 0] = 245  # R
        m_arr[dark_mask, 1] = 158  # G
        m_arr[dark_mask, 2] = 11   # B
        m_arr[dark_mask, 3] = 255  # A
        
        m_img = Image.fromarray(m_arr, "RGBA")
        m_w, m_h = 880, 540
        m_img = m_img.resize((m_w, m_h))
        inner_bg.paste(m_img, (bg_size // 2 - m_w // 2, 140), m_img)

    # 1B. Load & Clean Route Path & Location Pins (REMOVE PURPLE LINES & MAKE BOLD/BIGGER!)
    route_path = os.path.join(logo_dir, "route_path_pins_sketch.png")
    if os.path.exists(route_path):
        r_img = Image.open(route_path).convert("RGBA")
        r_arr = np.array(r_img)
        
        r_c, g_c, b_c, a_c = r_arr[:, :, 0], r_arr[:, :, 1], r_arr[:, :, 2], r_arr[:, :, 3]

        # Identify and ERASE purple line pixels (e.g. B > R + 20 or bluish/purple hues near straight lines)
        purple_line_mask = (b_c > r_c + 10) & (b_c > 60)
        r_arr[purple_line_mask, :] = 0

        # Erase top and bottom straight line rows
        h_r, w_r, _ = r_arr.shape
        for y in range(h_r):
            row_filled = np.sum(r_arr[y, :, 3] > 50)
            if row_filled > w_r * 0.25:  # Erase horizontal straight lines
                r_arr[y, :, :] = 0

        # Colorize remaining location pin icons & dashed route lines to bright Sunburst Amber Gold
        gold_pins_mask = (r_arr[:, :, 3] > 50)
        r_arr[gold_pins_mask, 0] = 245 # R
        r_arr[gold_pins_mask, 1] = 158 # G
        r_arr[gold_pins_mask, 2] = 11  # B
        r_arr[gold_pins_mask, 3] = 255 # A
        
        r_img = Image.fromarray(r_arr, "RGBA")
        
        # MAKE BIGGER SIZE! Scale up route path & pins (e.g. 960x550 instead of 800x420)
        r_w, r_h = 960, 540
        r_img = r_img.resize((r_w, r_h), resample=Image.BICUBIC)
        
        # Paste big route path & location pins overlapping with lower slopes of mountain peaks!
        inner_bg.paste(r_img, (bg_size // 2 - r_w // 2, 340), r_img)

    # Clean up any leftover background tint pixels
    arr_bg = np.array(inner_bg)
    r_bg, g_bg, b_bg = arr_bg[:, :, 0], arr_bg[:, :, 1], arr_bg[:, :, 2]
    purple_teal_mask = ((b_bg > r_bg + 20) & (b_bg > 50)) | ((g_bg > r_bg + 20) & (g_bg > 50) & (r_bg < 140))
    arr_bg[purple_teal_mask, 0] = 15
    arr_bg[purple_teal_mask, 1] = 23
    arr_bg[purple_teal_mask, 2] = 42
    inner_bg = Image.fromarray(arr_bg, "RGBA")

    # Save standalone background image assets
    inner_bg.save(dest_inner_bg_png, "PNG")
    inner_bg.save(dest_merged_bg_png, "PNG")
    inner_bg.convert("RGB").save(dest_inner_bg_jpg, "JPEG", quality=95)
    inner_bg.save(bg_artifact_output, "PNG")

    print("Successfully created standalone merged background image with BIGGER location pins and ZERO purple lines!")

    # STEP 2: BUILD FULL LOGO BADGE USING THE BIGGER LOCATION PIN MERGED BACKGROUND
    size = 1024
    cx, cy = size // 2, size // 2

    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    r_outer_border = 490
    r_outer_ring = 485
    r_inner_ring = 310

    # Solid outer gold border
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

    # Inner Circle Content using the newly created bigger location pin background!
    inner_circle_size = r_inner_ring * 2
    inner_cropped = inner_bg.resize((inner_circle_size, inner_circle_size))

    inner_mask = Image.new("L", (inner_circle_size, inner_circle_size), 0)
    draw_im = ImageDraw.Draw(inner_mask)
    draw_im.ellipse([0, 0, inner_circle_size, inner_circle_size], fill=255)

    img.paste(inner_cropped, (cx - r_inner_ring, cy - r_inner_ring), inner_mask)

    # Inner Gold Circle Border
    draw.ellipse([cx - r_inner_ring, cy - r_inner_ring, cx + r_inner_ring, cy + r_inner_ring], 
                 outline=(245, 158, 11, 255), width=6)

    # 3D TTS Monogram
    try:
        font_tts = ImageFont.truetype("arialbd.ttf", 95)
    except:
        font_tts = ImageFont.load_default()

    text_tts = "TTS"
    bbox = font_tts.getbbox(text_tts)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]

    tx = cx - tw // 2
    ty = cy - 105 - th // 2

    # Drop shadow
    for offset in range(6, 0, -1):
        draw.text((tx + offset, ty + offset), text_tts, font=font_tts, fill=(5, 10, 20, 230))

    # Gold fill with white outline
    draw.text((tx, ty), text_tts, font=font_tts, fill=(245, 158, 11, 255), stroke_width=3, stroke_fill=(255, 255, 255, 255))

    # Rider Bike Shield Emblem overlapping lower inner circle
    sx = cx
    sy = cy + 40

    shield_pts = [
        (sx, sy),
        (sx + 65, sy + 30),
        (sx + 55, sy + 120),
        (sx, sy + 155),
        (sx - 55, sy + 120),
        (sx - 65, sy + 30)
    ]
    draw.polygon(shield_pts, fill=(30, 58, 138, 230), outline=(255, 255, 255, 220), width=4)

    draw.ellipse([sx - 32, sy + 85, sx - 10, sy + 107], outline=(245, 158, 11, 255), width=4)
    draw.ellipse([sx + 10, sy + 85, sx + 32, sy + 107], outline=(245, 158, 11, 255), width=4)
    draw.line([(sx - 21, sy + 96), (sx, sy + 55), (sx + 21, sy + 96)], fill=(255, 255, 255, 255), width=5)
    draw.line([(sx - 14, sy + 48), (sx + 14, sy + 48)], fill=(245, 158, 11, 255), width=4)

    # Curved Text
    try:
        font_ring = ImageFont.truetype("arialbd.ttf", 42)
    except:
        font_ring = ImageFont.load_default()

    top_text = "• TRAVELER TRAVEL SYNK •"
    r_text_top = (r_outer_ring + r_inner_ring) // 2 + 10
    angle_step_top = 5.2
    start_angle_top = -90 - ((len(top_text) - 1) * angle_step_top) / 2

    for i, char in enumerate(top_text):
        angle_deg = start_angle_top + i * angle_step_top
        angle_rad = math.radians(angle_deg)

        char_x = cx + r_text_top * math.cos(angle_rad)
        char_y = cy + r_text_top * math.sin(angle_rad)

        char_img = Image.new("RGBA", (90, 90), (0, 0, 0, 0))
        cdraw = ImageDraw.Draw(char_img)
        cdraw.text((28, 18), char, font=font_ring, fill=(255, 255, 255, 255))

        rot = char_img.rotate(angle_deg + 90, expand=False, resample=Image.BICUBIC)
        img.paste(rot, (int(char_x - 45), int(char_y - 45)), rot)

    bot_text = "• SYNCHRONIZED GROUP TRAVEL •"
    r_text_bot = (r_outer_ring + r_inner_ring) // 2 - 10
    angle_step_bot = 4.6
    start_angle_bot = 90 + ((len(bot_text) - 1) * angle_step_bot) / 2

    for i, char in enumerate(bot_text):
        angle_deg = start_angle_bot - i * angle_step_bot
        angle_rad = math.radians(angle_deg)

        char_x = cx + r_text_bot * math.cos(angle_rad)
        char_y = cy + r_text_bot * math.sin(angle_rad)

        char_img = Image.new("RGBA", (90, 90), (0, 0, 0, 0))
        cdraw = ImageDraw.Draw(char_img)
        cdraw.text((28, 18), char, font=font_ring, fill=(245, 158, 11, 255))

        rot = char_img.rotate(angle_deg - 90, expand=False, resample=Image.BICUBIC)
        img.paste(rot, (int(char_x - 45), int(char_y - 45)), rot)

    # Save final logo outputs
    img.save(dest_trans, "PNG")
    img.save(dest_png, "PNG")
    img.convert("RGB").save(dest_jpg, "JPEG", quality=95)
    img.resize((512, 512)).save(dest_drawable, "PNG")
    img.save(logo_artifact_output, "PNG")

    print("Successfully built logo with BIGGER map location pins & zero purple lines!")

if __name__ == "__main__":
    build_merged_bg_and_logo()
