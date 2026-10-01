# RIDERsYNK AI Agent Persistent Memory (`memory.md`)

> [!IMPORTANT]
> **AI AGENT MANDATE**: Read this `memory.md` file at the beginning of every interaction to immediately understand the project state, past implementations, key credentials, active architectural decisions, and carry-forward tasks without rescanning the entire repository. Update this file automatically whenever code or architecture changes.

---

## 1. Project Overview & Architecture State

- **Application Name**: RIDERsYNK (RideSync)
- **Primary Goal**: Real-time group motorcycling convoy tracking, multi-segment trip planning, emergency alerts, and rider telemetry sync.
- **Client App**: Android Native (Kotlin, Jetpack Compose Material3, Coroutines Flow, Fused Location Provider, CameraX).
- **Primary Edge API**: Cloudflare Workers Edge API (`https://ahmedmohammed8694-riders-ride-sync.mdahmed08061994.workers.dev`).
  - Database: Cloudflare D1 (SQLite relational DB).
  - Cache: Cloudflare KV (`CONVOY_CACHE`).
  - Bot Defense: Cloudflare Turnstile (`/api/verify-turnstile`).
- **Secondary Database**: Supabase Postgres Database (`https://oktfyxdrvscmifomtlkp.supabase.co`).
  - Tables: `users` (9 columns), `saved_trips` (16 columns), `convoy_telemetry` (8 columns).
  - RLS Policies: Public read/write enabled for all tables.
- **Documentation Standard**: VibeCoding Standard (`docs/` folder & `coding-documents` skill).

---

## 2. Key Credentials & Configurations

- **Cloudflare Edge Base URL**: `https://ahmedmohammed8694-riders-ride-sync.mdahmed08061994.workers.dev`
- **Supabase URL**: `https://oktfyxdrvscmifomtlkp.supabase.co`
- **Supabase Publishable API Key**: `sb_publishable_dF8gDIF6Ahw4wWPRfYOH7Q_AaJrBrO8`
- **Supabase REST Base URL**: `https://oktfyxdrvscmifomtlkp.supabase.co/rest/v1`

---

## 3. Completed Features & Fixed Bugs

### 3.1 Permissions Framework
- **Dynamic Privacy & App Permissions**: Handled runtime permission requests for GPS Location, Live Background Location, Camera, Phone Calling, Battery Unrestriction, Storage, Screen-OFF background, and Screen Overlay. Included inline checks before using Camera or location features.

### 3.2 Trip Exit & Roster Clearing
- **Leave / Exit Trip Bug Fix**: When a rider exits/leaves a trip, the trip details are cleared from their active view, and `active_riders_count` is updated in Cloudflare D1, KV, and Supabase Postgres.

### 3.3 Join Trip Search & Account Display Fix
- **Strict Trip Search & No Dummy Creation**: When a user inputs a trip code (e.g. `RSS1041`) and taps **Inspect & View Trip Details**, the app queries Cloudflare Edge D1/KV and Supabase Postgres. If the trip exists, authentic trip details (title, origin, destination, distance, duration, host name, active riders count) are presented for preview. If not found, a clear error Toast is shown (`"⚠️ Trip code 'RSS1041' not found in database. Please enter a valid trip code (e.g. RSS1041)."`), and **NO dummy trip is created**.
- **Joined Trip Roster Account Sync**: When the user taps **Join Trip**, their profile (`riderId`, `displayName`, `bikeModel`, role = `MEMBER`, status = `"Joined & Confirmed"`) is added to the trip roster, persisted to Cloudflare D1/KV and Supabase Postgres, and the UI automatically navigates to **Saved Trips & History (Tab Index 1)** so the joined trip immediately displays under their account as a trip member.

### 3.4 Supabase Dual-Write Integration
- Connected Android client (`TripRepository.kt`, `AuthRepositoryImpl.kt`) and Cloudflare Workers API (`cloudflare-backend/src/index.ts`) directly to Supabase Postgres.
- Auto-syncs `users`, `saved_trips`, and live `convoy_telemetry` pings across all three database layers.

### 3.5 Global & Workspace `Coding Documents` Skill
- Added `Coding Documents` skill globally (`C:\Users\Mohammed Ahmed\.gemini\config\skills\coding-documents\SKILL.md`) and in workspace (`.agents/skills/coding-documents/SKILL.md`).
- Generated complete documentation suite in `docs/`:
  - `docs/ARCHITECTURE.md`
  - `docs/API-REFERENCE.md`
  - `docs/AI-COLLABORATION-WORKFLOW.md`
  - `docs/PROMPT-ENGINEERING-GUIDE.md`
  - `docs/QUALITY-ASSURANCE-GUIDE.md`
  - `docs/RECOVERY-PROCEDURES.md`

### 3.6 Pure Authentic Stats & High-Contrast Form Inputs
- **Fake Stats Removal**: Removed hardcoded default stats (`+ 3500.0` KM distance and `+ 11` completed rides) from `TripCreationScreen.kt` Rider Activity Summary header. The dashboard now calculates 100% authentic stats strictly from actual user-saved and completed trips.
- **High-Contrast Input Fields & Cursors**: Updated all text fields across `TripCreationScreen.kt`, `EditTripRouteDialog.kt`, and `DayByDayMapLinksDialog.kt` to enforce dark Slate container backgrounds (`Color(0xFF0F172A)`), crisp white input text (`Color(0xFFFFFFFF)`), section-matched labels/placeholders (`Color(0xFF94A3B8)` / `Color(0xFF64748B)`), and bright cyan cursors (`Color(0xFF00F0FF)`). Form input text when typing Trip Title, Start Location, Destination, or Google Maps links is now 100% clearly visible.

### 3.7 Real-Time Convoy Member Sync, Leaderboard, Map Pins & Join Notifications
- **Real-Time Member Roster Sync**: Enforced background real-time polling loop (`3s`) in `MainContainerScreen.kt` and `TripRepository.kt` via Cloudflare Workers (`/api/trip/get` & `/api/trips/public`) and Supabase Postgres. When any user joins a trip (e.g. Host creates `RSS1041` and another rider joins), the `joinedRiders` roster updates automatically across all open user instances in real-time.
- **Dynamic Leaderboard & Map Marker Integration**: Dynamically mapped all `joinedRiders` into `activeConvoyMembers` and `mergedLocations`. `ConvoyRadarEngine` processes every joined member so they appear on Google Maps markers, top position leaderboard overlay (`TopConvoyLeaderboardOverlay`), radar (`ConvoyRadarOverlay`), and roster bottom sheet (`ConvoyStatusBottomSheet`).
- **Real-Time Join Notifications**: Added automatic detection for newly joined riders in `MainContainerScreen.kt`. Whenever a new rider joins the trip, a real-time broadcast notification banner (`"🎉 New Convoy Member Joined: [Name] ([Bike])!"`) and Toast alert are triggered automatically for all other members on that trip.

### 3.8 Saved APK Backup & Codebase Snapshot Restoration
- **Persistent APK Backup**: Saved compiled APK binary (`49.9 MB`) to:
  - Local Backup Directory: `d:\My Applications\RIDERsYNK\apk_backups\app-debug-realtime-sync.apk`
  - Conversation Artifacts Directory: `C:\Users\Mohammed Ahmed\.gemini\antigravity-ide\brain\3dc24f91-a182-4339-a870-f6da37d0f888\app-debug-realtime-sync.apk`
- **Restore Command Mandate**: If instructed to "return to this APK" or "restore files to this version", copy `d:\My Applications\RIDERsYNK\apk_backups\app-debug-realtime-sync.apk` back to `app/build/outputs/apk/debug/app-debug.apk` and reset git tree to this commit snapshot (`3.8 Real-Time Convoy Sync`).

### 3.9 Google Stitch MCP & `/googlestitchappdesing` Skill Integration
- **Stitch MCP Configuration**: Added `stitch` server entry (`https://stitch.googleapis.com/mcp` with `X-Goog-Api-Key`) to `C:\Users\Mohammed Ahmed\.gemini\config\mcp_config.json`.
- **Global & Workspace Skill**: Created `googlestitchappdesing` skill at `C:\Users\Mohammed Ahmed\.gemini\config\skills\googlestitchappdesing\SKILL.md` and `.agents/skills/googlestitchappdesing/SKILL.md` enabling `/googlestitchappdesing` slash command invocation.

### 3.10 Frontend Redesign: Option 1 — Midnight Glassmorphism & Electric Cyan
- **UI Redesign**: Refactored application theme to **Option 1: Midnight Glassmorphism & Electric Cyan**.
- **Android Theme Update**: Updated `Color.kt` and `Theme.kt` with Midnight Navy Canvas (`#0B0E17`), Deep Navy Surface (`#0F1424`), Elevated Glass (`#141C2E`), and Electric Cyan Accents (`#00F3FF`).
- **Web UI Update**: Updated `public/index.html` with `--primary: #00F3FF`, `--bg-dark: #0B0E17`, `--bg-card: rgba(20, 28, 45, 0.75)`, and `--border: rgba(0, 243, 255, 0.2)`.
- **Backend & Data Preservation**: 100% preservation of all database schemas, backend endpoints, real-time Firebase listeners, trip code generation (`RSS1041`), live maps, and authentic stats (0 fake data).
- **APK Build & Backup**: Built debug APK (`assembleDebug`) and saved to:
  - Local Backup Directory: `d:\My Applications\RIDERsYNK\apk_backups\app-debug-midnight-cyan-v1.apk`
  - Artifacts Directory: `C:\Users\Mohammed Ahmed\.gemini\antigravity-ide\brain\3dc24f91-a182-4339-a870-f6da37d0f888\app-debug-midnight-cyan.apk`

### 3.12 3D Glassmorphism & 3D Racing Form Refactor (Option 1)
- **3D Glassmorphism Theme**: Applied Option 1 (Hyper-Neon Cyan `#00F3FF`, Ultramarine `#0066FF`, Hazard Orange `#FF5500`, Dark Obsidian `#080C16`).
- **3D Bevelled Tactile Buttons**: Refactored `TactileGloveButton.kt` with 3D raised tactile bevel gradients (`listOf(Color(0xFF171B26), Color(0xFF0F131D))`), haptic press scaling (`0.96f`), and glowing rims.
- **Backend & Data Preservation**: 0 mutations to Cloudflare Workers Edge API, Supabase Postgres, database schemas, trip code format (`RSS1041`), or button click handlers.
- **APK Rebuilt & Backed Up**: Compiled fresh Android debug APK (`app-debug-3d-glassmorphism-v1.apk`).

### 3.13 3D Glassy Alpine White & Sapphire Azure Frontend Integration (Option 3)
- **3D Glassy Alpine Azure Theme**: Applied Option 3 (Alpine Pearl Canvas `#F8FAFC`, Sapphire Azure Header `#1E40AF`, Cobalt Blue `#2563EB`, Sunburst Amber `#F59E0B`, Fresh Emerald `#10B981`, Deep Slate Text `#0F172A`).
- **3D Bevelled Tactile Buttons**: Configured 3D raised tactile buttons, 3D skeuomorphic speed dials, and translucent frosted glass cards (`rgba(255, 255, 255, 0.85)` with `backdrop-filter: blur(16px)`).
- **Backend & Data Preservation**: 0 mutations to Cloudflare Workers Edge API, Supabase Postgres, database schemas, trip code format (`RSS1041`), or button click handlers.
- **APK Rebuilt & Backed Up**: Compiled fresh Android debug APK (`app-debug-3d-glassy-alpine-azure-v1.apk`).

---

## 4. Automatic Update Mandate

Whenever ANY code, database schema, endpoint, or feature is updated:
1. **Update `memory.md`**: Add the new milestone, bugfix, or architectural change to Section 3.
2. **Update `docs/`**: Update `docs/ARCHITECTURE.md`, `docs/API-REFERENCE.md`, `docs/QUALITY-ASSURANCE-GUIDE.md`, etc., automatically without prompting the user.

---

## 5. Carry-Forward Agenda & Next Steps

- [x] Integrate Supabase as secondary database with dual-write.
- [x] Configure automatic documentation and persistent memory tracking.
- [x] Integrate Google Stitch MCP & `/googlestitchappdesing` skill.
- [x] Apply 3D Glassy Alpine White & Sapphire Azure Theme (`#1E40AF`, `#F8FAFC`, `#F59E0B`, `#10B981`, `#0F172A`).
- [ ] Run full end-to-end convoy tracking session simulation test on real hardware.
- [ ] Maintain `memory.md` and `docs/` on all future modifications.
