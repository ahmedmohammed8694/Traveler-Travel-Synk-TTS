import os
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFont

def catmull_rom_spline(pts, num_samples=300):
    # Smooth Catmull-Rom spline generator through control points
    if len(pts) < 2:
        return pts

    # Duplicate end points for smooth boundary tangents
    extended_pts = [pts[0]] + pts + [pts[-1]]
    curve = []

    for i in range(1, len(extended_pts) - 2):
        p0, p1, p2, p3 = extended_pts[i-1], extended_pts[i], extended_pts[i+1], extended_pts[i+2]
        
        for t in np.linspace(0, 1, num_samples // (len(pts) - 1)):
            # Catmull-Rom matrix
            f0 = -0.5*t**3 + t**2 - 0.5*t
            f1 = 1.5*t**3 - 2.5*t**2 + 1.0
            f2 = -1.5*t**3 + 2.0*t**2 + 0.5*t
            f3 = 0.5*t**3 - 0.5*t**2
            
            x = p0[0]*f0 + p1[0]*f1 + p2[0]*f2 + p3[0]*f3
            y = p0[1]*f0 + p1[1]*f1 + p2[1]*f2 + p3[1]*f3
            curve.append((x, y))
            
    return curve

def draw_location_pin(draw, x, y, scale=1.0, color=(0, 0, 0, 255), outline_color=(255, 255, 255, 255)):
    r = int(20 * scale)
    h = int(50 * scale)
    
    # White outline pin background
    draw.ellipse([x - r - 4, y - h - 4, x + r + 4, y - h + 2 * r + 4], fill=outline_color)
    draw.polygon([(x - r - 3, y - h + r), (x + r + 3, y - h + r), (x, y + 4)], fill=outline_color)
    
    # Main black pin body
    draw.ellipse([x - r, y - h, x + r, y - h + 2 * r], fill=color)
    draw.polygon([(x - r, y - h + r), (x + r, y - h + r), (x, y)], fill=color)
    
    # Center white dot
    dot_r = int(7 * scale)
    draw.ellipse([x - dot_r, y - h + r - dot_r, x + dot_r, y - h + r + dot_r], fill=outline_color)

def draw_dashed_path(draw, pts, color, width, dash_len=22, gap_len=14):
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

    # STEP 1: CREATE STANDALONE MERGED INNER BACKGROUND WITH SMOOTH CURVED PATH & 5 LOCATION PINS
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

    # 1B. Create SMOOTH WINDING CURVED PATH through 5 Control Points spanning across and outside mountains
    control_points = [
        (60, 710),    # Point 1: Start left outside
        (260, 600),   # Point 2: Climbing lower mountain slope
        (500, 410),   # Point 3: Top mountain peak loop
        (740, 560),   # Point 4: Descending right slope
        (940, 680)    # Point 5: Exit right outside
    ]

    # Generate smooth curve points
    smooth_curve = catmull_rom_spline(control_points, num_samples=400)

    draw_bg = ImageDraw.Draw(inner_bg)
    
    # Outer white stroke outline for high visibility
    draw_dashed_path(draw_bg, smooth_curve, (255, 255, 255, 255), width=14, dash_len=24, gap_len=14)
    # Inner bold black dashed line
    draw_dashed_path(draw_bg, smooth_curve, (0, 0, 0, 255), width=8, dash_len=24, gap_len=14)

    # 1C. Draw EXACTLY 5 Bold Black Location Waypoint Pins at the 5 control points!
    pin_scales = [1.1, 1.15, 1.25, 1.15, 1.1]
    for (px, py), scale in zip(control_points, pin_scales):
        draw_location_pin(draw_bg, px, py, scale=scale, color=(0, 0, 0, 255), outline_color=(255, 255, 255, 255))

    # Save standalone background image assets
    inner_bg.save(dest_inner_bg_png, "PNG")
    inner_bg.save(dest_merged_bg_png, "PNG")
    inner_bg.convert("RGB").save(dest_inner_bg_jpg, "JPEG", quality=95)
    inner_bg.save(bg_artifact_output, "PNG")

    print("Successfully created standalone merged background image with smooth curved path & 5 location pins!")

    # STEP 2: BUILD FULL LOGO BADGE USING THE 5-PIN CURVED ROUTE MERGED BACKGROUND
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

    # Inner Circle Content using the newly created 5-pin curved route background!
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

    print("Successfully built logo with smooth curved route path & 5 location pins!")

if __name__ == "__main__":
    build_merged_bg_and_logo()
