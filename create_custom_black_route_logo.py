import os
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFont

def draw_location_pin(draw, x, y, scale=1.0, color=(0, 0, 0, 255), outline_color=(255, 255, 255, 255)):
    # Draw a bold map pin icon at (x, y)
    r = int(22 * scale)
    h = int(55 * scale)
    
    # White outline pin background
    draw.ellipse([x - r - 4, y - h - 4, x + r + 4, y - h + 2 * r + 4], fill=outline_color)
    draw.polygon([(x - r - 3, y - h + r), (x + r + 3, y - h + r), (x, y + 4)], fill=outline_color)
    
    # Main black pin body
    draw.ellipse([x - r, y - h, x + r, y - h + 2 * r], fill=color)
    draw.polygon([(x - r, y - h + r), (x + r, y - h + r), (x, y)], fill=color)
    
    # Center white dot
    dot_r = int(8 * scale)
    draw.ellipse([x - dot_r, y - h + r - dot_r, x + dot_r, y - h + r + dot_r], fill=outline_color)

def draw_dashed_curve(draw, points, color=(0, 0, 0, 255), outline_color=(255, 255, 255, 255), width=8, dash_len=25, gap_len=15):
    # Interpolate smooth points along path
    path_pts = []
    for i in range(len(points) - 1):
        p0, p1 = points[i], points[i+1]
        steps = 40
        for t in range(steps):
            s = t / float(steps)
            x = (1 - s) * p0[0] + s * p1[0]
            y = (1 - s) * p0[1] + s * p1[1]
            path_pts.append((x, y))
            
    # Draw outer white stroke first
    draw_dashed_line_pts(draw, path_pts, outline_color, width + 6, dash_len, gap_len)
    # Draw inner black dashed stroke
    draw_dashed_line_pts(draw, path_pts, color, width, dash_len, gap_len)

def draw_dashed_line_pts(draw, pts, color, width, dash_len, gap_len):
    accum_dist = 0
    drawing = True
    dash_target = dash_len
    
    for i in range(len(pts) - 1):
        p0, p1 = pts[i], pts[i+1]
        dist = math.hypot(p1[0] - p0[0], p1[1] - p0[1])
        
        if drawing:
            draw.line([p0, p1], fill=color, width=width)
            accum_dist += dist
            if accum_dist >= dash_target:
                drawing = False
                accum_dist = 0
                dash_target = gap_len
        else:
            accum_dist += dist
            if accum_dist >= dash_target:
                drawing = True
                accum_dist = 0
                dash_target = dash_len

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

    # STEP 1: CREATE CRISP STANDALONE MERGED INNER BACKGROUND IMAGE WITH BLACK DASHED ROUTE & PINS
    bg_size = 1000
    inner_bg = Image.new("RGBA", (bg_size, bg_size), (15, 23, 42, 255)) # Dark navy #0F172A

    # 1A. Load & Colorize Mountain Peaks (Sunburst Amber Gold #F59E0B)
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

    # 1B. Draw Custom BOLD BLACK Dashed Route Path spanning ACROSS and OUTSIDE the mountains!
    draw_bg = ImageDraw.Draw(inner_bg)
    
    # Path coordinates: Start left outside (80, 720) -> climb mountain slope (280, 580) -> loop over peak (540, 430) -> exit right outside (920, 680)
    curve_points = [
        (60, 720),
        (220, 640),
        (380, 520),
        (540, 420),
        (700, 530),
        (840, 620),
        (940, 690)
    ]
    
    # Draw bold black dashed curve with crisp white outline
    draw_dashed_curve(draw_bg, curve_points, color=(0, 0, 0, 255), outline_color=(255, 255, 255, 255), width=9, dash_len=24, gap_len=16)

    # Draw 4 prominent Black Map Location Waypoint Pins along the route!
    pin_locations = [
        (80, 715, 1.1),    # Start Pin outside left
        (350, 540, 1.2),   # Pin climbing mountain slope
        (560, 425, 1.25),  # Pin near mountain peak
        (910, 675, 1.1)    # Exit Pin outside right
    ]
    
    for px, py, scale in pin_locations:
        draw_location_pin(draw_bg, px, py, scale=scale, color=(0, 0, 0, 255), outline_color=(255, 255, 255, 255))

    # Save standalone background image assets
    inner_bg.save(dest_inner_bg_png, "PNG")
    inner_bg.save(dest_merged_bg_png, "PNG")
    inner_bg.convert("RGB").save(dest_inner_bg_jpg, "JPEG", quality=95)
    inner_bg.save(bg_artifact_output, "PNG")

    print("Successfully created standalone merged background image with BOLD BLACK route path & location pins!")

    # STEP 2: BUILD FULL LOGO BADGE USING THE BLACK ROUTE MERGED BACKGROUND
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

    # Inner Circle Content using the newly created black route merged background!
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
    draw.polygon(shield_pts, fill=(30, 58, 138, 235), outline=(255, 255, 255, 220), width=4)

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

    print("Successfully built logo with BOLD BLACK route path & pins!")

if __name__ == "__main__":
    build_merged_bg_and_logo()
