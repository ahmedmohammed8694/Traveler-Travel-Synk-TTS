# Application UI/UX Frontend Design System & Theme Rules

> **MANDATORY RULE FOR ALL FRONTEND DEVELOPERS AND AGENTS**
> Before designing, building, or modifying ANY page, component, dialog, or UI option in the Traveler Travel Synk (TTS / RideSync) application, you MUST consult and strictly follow this document.

---

## 1. Core Color Palette & Design Tokens

### Canvas & Containers (Dark Obsidian Modern HUD Aesthetic)
- **Primary Canvas Background:** `Color(0xFF090D16)` or `HudColors.ObsidianCanvas` (`Color(0xFF0F172A)`)
- **Card & Surface Container:** `Color(0xFF1E293B)` or `HudColors.ObsidianSurface`
- **Elevated Glass/Modal Surface:** `Color(0xFF0F172A)`
- **Border & Rim Stroke:** `Color(0xFF334155)` or `HudColors.ObsidianBorder` (`Color(0xFFCBD5E1)`)

### Accent & Telemetry Palette
- **Primary Cyan/Sapphire Brand Accent:** `Color(0xFF00E5FF)` / `Color(0xFF38BDF8)` / `Color(0xFF0052CC)`
- **Success & Live Riding Green:** `Color(0xFF22C55E)` / `Color(0xFF16A34A)`
- **Warning & Amber Alert:** `Color(0xFFF59E0B)` / `Color(0xFFFBBF24)`
- **Destructive Action & Emergency Red:** `Color(0xFFEF4444)` / `Color(0xFFDC2626)`

### Text Hierarchy & Contrast Standards
- **Primary Page/Card Titles:** `Color.White` (`Color(0xFFFFFFFF)`) or `Color(0xFFF8FAFC)`
- **Subtitles & Section Labels:** `Color(0xFF94A3B8)` or `HudColors.TextCoolSilver` (`Color(0xFFCBD5E1)`)
- **Muted Hints & Secondary Info:** `Color(0xFF64748B)`
- **Primary Action Text:** `Color.White` or `Color(0xFF00E5FF)`

---

## 2. Typography & Fonts

- **Page Titles:** `FontSize = 20.sp` – `24.sp`, `FontWeight = FontWeight.Black`
- **Section Headers:** `FontSize = 15.sp` – `18.sp`, `FontWeight = FontWeight.Bold`
- **Body / Primary Items:** `FontSize = 13.sp` – `14.sp`, `FontWeight = FontWeight.Bold` or `Medium`
- **Subtext & Metadata:** `FontSize = 11.sp` – `12.sp`, `FontWeight = FontWeight.Normal` or `Medium`
- **Badges & Tags:** `FontSize = 9.sp` – `10.sp`, `FontWeight = FontWeight.Black`

---

## 3. UI Component Standards

### Cards & Surfaces
- **Corner Radius:** `16.dp` to `20.dp` for main cards; `12.dp` for nested surfaces.
- **Borders:** `BorderStroke(1.dp, Color(0xFF334155))` or `BorderStroke(1.5.dp, Color(0xFF0052CC))`.

### Dialogs & Pop-ups
- **Rule:** White/light dialogs are strictly forbidden. All modals must use `containerColor = Color(0xFF1E293B)` or `HudColors.ObsidianSurface` with `Color.White` titles and dark high-contrast styling.

### Buttons & Pill Badges
- **Primary Actions:** Pill-shaped (`CircleShape` or `RoundedCornerShape(12.dp)`), solid background (`Color(0xFF0052CC)` / `Color(0xFF22C55E)`), white bold text.
- **Destructive Actions:** Solid or outlined red pill buttons (`containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)`, `contentColor = Color(0xFFEF4444)`, `border = BorderStroke(1.dp, Color(0xFFEF4444))`).
