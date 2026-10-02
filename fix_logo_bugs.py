import os
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def fix_logo():
    logo_dir = r"d:\My Applications\RIDERsYNK\Logo"
    src_merged_bg = os.path.join(logo_dir, "inner_circle_merged_bg.png")
    ring_bg_path = os.path.join(logo_dir, "travel_ring_bg_pattern.png")

    dest_trans = os.path.join(logo_dir, "tts_app_logo_transparent.png")
    dest_png = os.path.join(logo_dir, "tts_app_logo.png")
    dest_jpg = os.path.join(logo_dir, "tts_app_logo.jpg")
    dest_drawable = r"d:\My Applications\RIDERsYNK\app\src\main\res\drawable\ic_app_logo_badge.png"
    artifact_output = r"C:\Users\Mohammed Ahmed\.gemini\antigravity-ide\brain\b5ecd0cf-4642-4da2-864e-3f41bbb131e0\fixed_logo_no_bugs.png"

    size = 1024
    cx, cy = size // 2, size // 2

    # 1. Base Canvas
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    r_outer_border = 490
    r_outer_ring = 485
    r_inner_ring = 310

    # Draw solid outer Amber Gold ring background first (fixes top edge gap/checkerboard bug!)
    draw.ellipse([cx - r_outer_border, cy - r_outer_border, cx + r_outer_border, cy + r_outer_border], 
                 fill=(245, 158, 11, 255))

    # 2. Outer Ring Pattern (15 photo B&W ink painting collage)
    if os.path.exists(ring_bg_path):
        ring_img = Image.open(ring_bg_path).convert("RGBA").resize((size, size))
    else:
        ring_img = Image.new("RGBA", (size, size), (30, 41, 59, 255))

    ring_mask = Image.new("L", (size, size), 0)
    draw_rm = ImageDraw.Draw(ring_mask)
    draw_rm.ellipse([cx - r_outer_ring, cy - r_outer_ring, cx + r_outer_ring, cy + r_outer_ring], fill=255)
    draw_rm.ellipse([cx - r_inner_ring, cy - r_inner_ring, cx + r_inner_ring, cy + r_inner_ring], fill=0)

    img.paste(ring_img, (0, 0), ring_mask)

    # Re-draw clean outer Amber Gold stroke border over ring edge (zero gaps!)
    draw.ellipse([cx - r_outer_ring, cy - r_outer_ring, cx + r_outer_ring, cy + r_outer_ring], 
                 outline=(245, 158, 11, 255), width=8)

    # 3. Inner Circle Background
    inner_circle_size = r_inner_ring * 2
    inner_canvas = Image.new("RGBA", (inner_circle_size, inner_circle_size), (15, 23, 42, 255))

    if os.path.exists(src_merged_bg):
        merged_bg = Image.open(src_merged_bg).convert("RGBA").resize((inner_circle_size, inner_circle_size))
        
        # Clean up thin diagonal/target lines from merged_bg (especially line through pin icon!)
        arr = np.array(merged_bg)
        r_c, g_c, b_c, a_c = arr[:, :, 0], arr[:, :, 1], arr[:, :, 2], arr[:, :, 3]
        
        # Identify dark green/cyan/white thin line pixels (G > R+15 & G > 40 & R < 150)
        line_mask = (g_c > r_c + 15) & (g_c > 40) & (r_c < 150)
        arr[line_mask, 0] = 15
        arr[line_mask, 1] = 23
        arr[line_mask, 2] = 42

        # Clean thin white lines cutting through pin icon
        # Replace non-pin isolated thin line pixels
        merged_bg = Image.fromarray(arr, "RGBA")
        inner_canvas.paste(merged_bg, (0, 0), merged_bg)

    # Inner circular mask matching inner_circle_size
    inner_mask = Image.new("L", (inner_circle_size, inner_circle_size), 0)
    draw_im = ImageDraw.Draw(inner_mask)
    draw_im.ellipse([0, 0, inner_circle_size, inner_circle_size], fill=255)

    img.paste(inner_canvas, (cx - r_inner_ring, cy - r_inner_ring), inner_mask)

    # Re-draw inner Amber Gold circle border stroke
    draw.ellipse([cx - r_inner_ring, cy - r_inner_ring, cx + r_inner_ring, cy + r_inner_ring], 
                 outline=(245, 158, 11, 255), width=6)

    # 4. Motorcycle Emblem overlapping with Mountain Peaks & Location Route Path
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
    # Semi-transparent shield so background mountain peaks & route line subtly show through!
    draw.polygon(shield_pts, fill=(30, 58, 138, 230), outline=(255, 255, 255, 220), width=4)

    # Bike wheels & frame details
    draw.ellipse([sx - 32, sy + 85, sx - 10, sy + 107], outline=(245, 158, 11, 255), width=4)
    draw.ellipse([sx + 10, sy + 85, sx + 32, sy + 107], outline=(245, 158, 11, 255), width=4)
    draw.line([(sx - 21, sy + 96), (sx, sy + 55), (sx + 21, sy + 96)], fill=(255, 255, 255, 255), width=5)
    draw.line([(sx - 14, sy + 48), (sx + 14, sy + 48)], fill=(245, 158, 11, 255), width=4)

    # 5. 3D TTS Tech Monogram in upper inner circle
    try:
        font_tts = ImageFont.truetype("arialbd.ttf", 95)
    except:
        font_tts = ImageFont.load_default()

    text_tts = "TTS"
    bbox = font_tts.getbbox(text_tts)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]

    tx = cx - tw // 2
    ty = cy - 100 - th // 2

    # Drop shadow
    for offset in range(6, 0, -1):
        draw.text((tx + offset, ty + offset), text_tts, font=font_tts, fill=(5, 10, 20, 230))

    # Yellow/Gold fill with white stroke
    draw.text((tx, ty), text_tts, font=font_tts, fill=(245, 158, 11, 255), stroke_width=3, stroke_fill=(255, 255, 255, 255))

    # 6. Curved Text on Outer Ring
    try:
        font_ring = ImageFont.truetype("arialbd.ttf", 42)
    except:
        font_ring = ImageFont.load_default()

    # Top Text
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

    # Bottom Text
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

    # Save final assets
    img.save(dest_trans, "PNG")
    img.save(dest_png, "PNG")
    img.convert("RGB").save(dest_jpg, "JPEG", quality=95)
    img.resize((512, 512)).save(dest_drawable, "PNG")
    img.save(artifact_output, "PNG")

    print("Successfully fixed logo bugs and generated clean overlapping emblem!")

if __name__ == "__main__":
    fix_logo()
