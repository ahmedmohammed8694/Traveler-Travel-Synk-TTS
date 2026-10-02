# Traveler Travel Synk (TTS) – UI/UX Design System & Database Persistence Spec

## Overview
This specification serves as the master authority for all UI/UX frontend styling, typography, component guidelines, dark mode standards, and database backend persistence rules for the **Traveler Travel Synk (TTS / RideSync)** application.

Whenever building new screens, adding options, or modifying existing features, refer to this document to ensure 100% theme consistency and automated database synchronization.

---

## Part I: UI/UX Frontend Design System

### 1. Palette & Color Tokens (`HudColors.kt`)

| Token | Color Code / Value | Usage Description |
| :--- | :--- | :--- |
| **ObsidianCanvas** | `Color(0xFF090D16)` / `Color(0xFF0F172A)` | Main screen background for dark HUD aesthetic |
| **ObsidianSurface** | `Color(0xFF1E293B)` | Surface container for cards, dialogs, and sections |
| **ObsidianElevated** | `Color(0xFF0F172A)` | Elevated sub-cards, text inputs, and milestone rows |
| **ObsidianBorder** | `Color(0xFF334155)` / `Color(0xFFCBD5E1)` | Fine slate border strokes (`1.dp` to `1.5.dp`) |
| **CyanPrimary** | `Color(0xFF00E5FF)` / `Color(0xFF38BDF8)` | Primary brand accent, titles, highlights, active icons |
| **Success / Riding** | `Color(0xFF22C55E)` / `Color(0xFF16A34A)` | Completed milestones, live status, join trip buttons |
| **Warning / Amber** | `Color(0xFFF59E0B)` / `Color(0xFFFBBF24)` | Upcoming categories, intermediate warnings |
| **Emergency / Red** | `Color(0xFFEF4444)` / `Color(0xFFDC2626)` | Destructive options (Remove Member, Delete Trip, SOS) |

---

### 2. Typography & Contrast Guidelines

- **WCAG AAA High-Contrast Standard:** On Dark Obsidian background, text must always use `Color.White` (`#FFFFFF`) for primary titles, `Color(0xFF94A3B8)` for labels/subtitles, and `Color(0xFF00E5FF)` for key accents. Light/gray-on-light-gray is strictly forbidden.
- **Font Weights:**
  - Screen Titles: `20.sp` - `24.sp`, `FontWeight.Black`
  - Card Titles: `15.sp` - `18.sp`, `FontWeight.Bold`
  - Subtext & Labels: `11.sp` - `12.sp`, `FontWeight.Medium`
  - Badges & Tags: `9.sp` - `10.sp`, `FontWeight.Black`

---

### 3. Component & Dialog Standards

1. **Dialogs & Pop-ups:**
   - Container color must be `Color(0xFF1E293B)` or `HudColors.ObsidianSurface`.
   - Title text must be `Color.White` with optional icon prefix.
   - Action buttons must be rounded pill buttons with clear contrast.

2. **Member Management Buttons:**
   - `+ Add Member`: Cyan or Sapphire Blue button with `PersonAdd` icon.
   - `Remove`: Explicit red pill button (`containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)`, `contentColor = Color(0xFFEF4444)`, `border = BorderStroke(1.dp, Color(0xFFEF4444))`) with `PersonRemove` or `Delete` icon.

3. **Stops & Milestones:**
   - Display order index, stop name, activity description, status tag (**PENDING**, **COMPLETED**, **SKIPPED**), status toggle checkbox, and **Open in Google Maps** URL launcher.

---

## Part II: Database & Backend Server Persistence Architecture

### 1. Live Backend Connectivity Policy
- Every user data action (creating trips, updating routes, toggling stop statuses, searching travelers, adding/removing members) MUST directly write to live backend databases:
  - **Supabase REST API:** `https://oktfyxdrvscmifomtlkp.supabase.co/rest/v1/`
  - **Cloudflare Edge D1 SQL / KV:** `/api/trips`, `/api/auth/profile`, `/api/convoy`
  - **Google Cloud Firestore:** Real-time convoy location sync and profile search.

### 2. Auto-Provisioning Schema & Storage Rules
- When new data columns or model fields are introduced:
  1. Add schema DDL `ALTER TABLE ... ADD COLUMN IF NOT EXISTS ...` in `schema.sql` and `supabase_schema.sql`.
  2. Ensure serializers store and parse the new fields in JSON payloads.
- Dual-write pattern guarantees data integrity even during offline/network transitions.
