import os
import numpy as np
from PIL import Image, ImageFilter

def remove_green_line():
    logo_dir = r"d:\My Applications\RIDERsYNK\Logo"
    src_logo = r"C:\Users\Mohammed Ahmed\.gemini\antigravity-ide\brain\b5ecd0cf-4642-4da2-864e-3f41bbb131e0\no_green_outline_logo.png"

    dest_trans = os.path.join(logo_dir, "tts_app_logo_transparent.png")
    dest_png = os.path.join(logo_dir, "tts_app_logo.png")
    dest_jpg = os.path.join(logo_dir, "tts_app_logo.jpg")
    dest_drawable = r"d:\My Applications\RIDERsYNK\app\src\main\res\drawable\ic_app_logo_badge.png"
    artifact_output = r"C:\Users\Mohammed Ahmed\.gemini\antigravity-ide\brain\b5ecd0cf-4642-4da2-864e-3f41bbb131e0\no_green_line_logo.png"

    img = Image.open(src_logo).convert("RGBA")
    arr = np.array(img, dtype=np.uint8)

    r, g, b, a = arr[:, :, 0], arr[:, :, 1], arr[:, :, 2], arr[:, :, 3]

    # Detect green line pixels inside the inner circle area (center cx=512, cy=512, radius r~300)
    # The green line in user screenshot is dark teal/green (e.g., R<100, G>100, B<160 or G > R + 25)
    # Let's inspect green mask:
    # Green lines are characterized by G > R + 15 and G > B - 40, or bluish-green cyan
    
    h, w, _ = arr.shape
    y_grid, x_grid = np.ogrid[:h, :w]
    dist_from_center = np.sqrt((x_grid - 512)**2 + (y_grid - 512)**2)

    # Restrict green line detection to inside inner circle (dist < 320)
    inner_mask = dist_from_center < 320

    # Green line condition: G is dominant over R (green tint), but not the bike shield (which has blue/white/amber)
    # Green line stroke is typically R in [0..120], G in [80..220], B in [50..180] where G > R + 20
    green_line_mask = inner_mask & (g > r + 20) & (g > 60) & (r < 140)

    # Exclude TTS text outline if it has yellow/gold (R>180, G>180, B<100)
    # Replace green line pixels with dark background color #0E1626 (R=14, G=22, B=38)
    arr[green_line_mask, 0] = 14  # R
    arr[green_line_mask, 1] = 22  # G
    arr[green_line_mask, 2] = 38  # B
    arr[green_line_mask, 3] = 255 # A

    cleaned_img = Image.fromarray(arr, "RGBA")

    # Apply light median filter only over replaced areas to blend seamlessly
    cleaned_img.save(dest_trans, "PNG")
    cleaned_img.save(dest_png, "PNG")
    cleaned_img.convert("RGB").save(dest_jpg, "JPEG", quality=95)
    cleaned_img.resize((512, 512)).save(dest_drawable, "PNG")
    cleaned_img.save(artifact_output, "PNG")

    print(f"Successfully removed green line pixels! Output saved to {artifact_output}")

if __name__ == "__main__":
    remove_green_line()
