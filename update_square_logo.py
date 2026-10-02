import shutil
import os

logo_badge = r"d:\My Applications\RIDERsYNK\app\src\main\res\drawable\ic_app_logo_badge.png"
logo_square = r"d:\My Applications\RIDERsYNK\app\src\main\res\drawable\ic_app_logo_square.png"

if os.path.exists(logo_badge):
    shutil.copyfile(logo_badge, logo_square)
    print("Successfully updated ic_app_logo_square.png with the new 5-pin curved route background logo badge!")
else:
    print(f"File not found: {logo_badge}")
