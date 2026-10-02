import os
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFont

def create_logo():
    logo_dir = r"d:\My Applications\RIDERsYNK\Logo"
    output_transparent = os.path.join(logo_dir, "tts_app_logo_transparent.png")
    output_png = os.path.join(logo_dir, "tts_app_logo.png")
    output_jpg = os.path.join(logo_dir, "tts_app_logo.jpg")
    output_drawable = r"d:\My Applications\RIDERsYNK\app\src\main\res\drawable\ic_app_logo_badge.png"
    artifact_output = r"C:\Users\Mohammed Ahmed\.gemini\antigravity-ide\brain\b5ecd0cf-4642-4da2-864e-3f41bbb131e0\smaller_inner_no_yellow_outline_logo.png"

    size = 1024
    cx, cy = size // 2, size // 2

    # Canvas (100% transparent PNG)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))

    # Radii: Outer ring r_outer=470, Inner circle r_inner=240 (making inner circle smaller as requested)
    r_outer = 470
    r_inner = 240

    # 1. Outer Ring Background (15 photo B&W ink painting collage pattern)
    ring_bg_path = os.path.join(logo_dir, "travel_ring_bg_pattern.png")
    if os.path.exists(ring_bg_path):
        ring_img = Image.open(ring_bg_path).convert("RGBA").resize((size, size))
    else:
        ring_img = Image.new("RGBA", (size, size), (30, 41, 59, 255))

    # Ring mask (donut shape: r_inner < r < r_outer)
    ring_mask = Image.new("L", (size, size), 0)
    draw_rm = ImageDraw.Draw(ring_mask)
    draw_rm.ellipse([cx - r_outer, cy - r_outer, cx + r_outer, cy + r_outer], fill=255)
    draw_rm.ellipse([cx - r_inner, cy - r_inner, cx + r_inner, cy + r_inner], fill=0)

    img.paste(ring_img, (0, 0), ring_mask)

    # 2. Pure Clean Inner Circle Background (Dark Midnight Navy #0F172A)
    inner_canvas = Image.new("RGBA", (r_inner * 2, r_inner * 2), (15, 23, 42, 255))
    ic_center = r_inner

    # Load Mountain Peaks sketch (gold mountains)
    mountains_path = os.path.join(logo_dir, "mountain_peaks_sketch.png")
    if os.path.exists(mountains_path):
        m_img = Image.open(mountains_path).convert("RGBA")
        m_w, m_h = int(r_inner * 1.8), int(r_inner * 1.0)
        m_img = m_img.resize((m_w, m_h))
        # Paste mountains centered in inner circle
        inner_canvas.paste(m_img, (ic_center - m_w // 2, ic_center - m_h // 2 - 10), m_img)

    # Load Route Path & Pins sketch (dashed path & waypoint pins)
    route_path = os.path.join(logo_dir, "route_path_pins_sketch.png")
    if os.path.exists(route_path):
        r_img = Image.open(route_path).convert("RGBA")
        r_w, r_h = int(r_inner * 1.6), int(r_inner * 0.9)
        r_img = r_img.resize((r_w, r_h))
        inner_canvas.paste(r_img, (ic_center - r_w // 2, ic_center + 10), r_img)

    # Mask inner canvas into circular shape
    inner_circle_mask = Image.new("L", (r_inner * 2, r_inner * 2), 0)
    draw_icm = ImageDraw.Draw(inner_circle_mask)
    draw_icm.ellipse([0, 0, r_inner * 2, r_inner * 2], fill=255)

    img.paste(inner_canvas, (cx - r_inner, cy - r_inner), inner_circle_mask)

    draw = ImageDraw.Draw(img)

    # ZERO YELLOW/GREEN/CYAN CIRCLE LINES OR OUTLINES ON INNER OR OUTER CIRCLE!

    # 3. 3D TTS Tech Monogram in upper inner circle
    try:
        font_tts = ImageFont.truetype("arialbd.ttf", 80)
    except:
        font_tts = ImageFont.load_default()

    text_tts = "TTS"
    bbox = font_tts.getbbox(text_tts)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]

    tx = cx - tw // 2
    ty = cy - 70 - th // 2

    # Drop shadow
    for offset in range(5, 0, -1):
        draw.text((tx + offset, ty + offset), text_tts, font=font_tts, fill=(5, 10, 20, 230))

    # Solid bright white monogram
    draw.text((tx, ty), text_tts, font=font_tts, fill=(255, 255, 255, 255))

    # 4. Motorcycle Shield Emblem in lower inner circle
    sx = cx
    sy = cy + 45

    shield_pts = [
        (sx, sy),
        (sx + 50, sy + 22),
        (sx + 40, sy + 90),
        (sx, sy + 115),
        (sx - 40, sy + 90),
        (sx - 50, sy + 22)
    ]
    draw.polygon(shield_pts, fill=(30, 58, 138, 240), outline=(255, 255, 255, 220), width=3)

    # Bike emblem details inside shield
    draw.ellipse([sx - 25, sy + 62, sx - 8, sy + 79], outline=(245, 158, 11, 255), width=3)
    draw.ellipse([sx + 8, sy + 62, sx + 25, sy + 79], outline=(245, 158, 11, 255), width=3)
    draw.line([(sx - 16, sy + 70), (sx, sy + 40), (sx + 16, sy + 70)], fill=(255, 255, 255, 255), width=4)
    draw.line([(sx - 10, sy + 36), (sx + 10, sy + 36)], fill=(245, 158, 11, 255), width=3)

    # 5. Curved Ring Text: "• TRAVELER TRAVEL SYNK •"
    try:
        font_ring = ImageFont.truetype("arialbd.ttf", 36)
    except:
        font_ring = ImageFont.load_default()

    ring_text = "• TRAVELER TRAVEL SYNK •"
    r_text = (r_outer + r_inner) // 2 + 5

    total_chars = len(ring_text)
    angle_per_char = 5.6
    start_angle_deg = -90 - ((total_chars - 1) * angle_per_char) / 2

    for i, char in enumerate(ring_text):
        angle_deg = start_angle_deg + i * angle_per_char
        angle_rad = math.radians(angle_deg)

        char_x = cx + r_text * math.cos(angle_rad)
        char_y = cy + r_text * math.sin(angle_rad)

        char_img = Image.new("RGBA", (80, 80), (0, 0, 0, 0))
        cdraw = ImageDraw.Draw(char_img)
        cdraw.text((25, 15), char, font=font_ring, fill=(255, 255, 255, 255))

        rot = char_img.rotate(angle_deg + 90, expand=False, resample=Image.BICUBIC)
        img.paste(rot, (int(char_x - 40), int(char_y - 40)), rot)

    # Save final logo assets
    img.save(output_transparent, "PNG")
    img.save(output_png, "PNG")
    img.convert("RGB").save(output_jpg, "JPEG", quality=95)
    img.resize((512, 512)).save(output_drawable, "PNG")
    img.save(artifact_output, "PNG")

    print("Ultra clean logo created successfully with smaller inner circle and NO circle lines!")

if __name__ == "__main__":
    create_logo()
