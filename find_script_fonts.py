import os
import glob

font_dir = r"C:\Windows\Fonts"
fonts = glob.glob(os.path.join(font_dir, "*.[tT][tT][fF]")) + glob.glob(os.path.join(font_dir, "*.[oO][tT][fF]"))

script_fonts = []
for f in fonts:
    base = os.path.basename(f).lower()
    if any(k in base for k in ["script", "calli", "brush", "corsiva", "edward", "french", "monotype", "kunstler", "palace", "vladimir", "georgiai", "timesi", "playfair", "great", "whitestone"]):
        script_fonts.append(f)

print("Found script/calligraphic fonts:")
for sf in script_fonts:
    print(sf)
