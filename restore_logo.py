import shutil
import os
from PIL import Image

src_logo = r"C:\Users\Mohammed Ahmed\.gemini\antigravity-ide\brain\b5ecd0cf-4642-4da2-864e-3f41bbb131e0\no_green_outline_logo.png"

logo_dir = r"d:\My Applications\RIDERsYNK\Logo"
dest_trans = os.path.join(logo_dir, "tts_app_logo_transparent.png")
dest_png = os.path.join(logo_dir, "tts_app_logo.png")
dest_jpg = os.path.join(logo_dir, "tts_app_logo.jpg")
dest_drawable = r"d:\My Applications\RIDERsYNK\app\src\main\res\drawable\ic_app_logo_badge.png"

if os.path.exists(src_logo):
    shutil.copyfile(src_logo, dest_trans)
    shutil.copyfile(src_logo, dest_png)
    
    img = Image.open(src_logo).convert("RGB")
    img.save(dest_jpg, "JPEG", quality=95)
    
    img_draw = Image.open(src_logo).convert("RGBA").resize((512, 512))
    img_draw.save(dest_drawable, "PNG")
    
    print("Successfully restored the last previous logo version!")
else:
    print(f"Source file not found: {src_logo}")
