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

### 3.8 Exact Design System Color Tokens, Typography Scale & Tactical Microcopy
- **Color Palette Tokens (`HudColors` in `Color.kt`)**:
  - Primary Accent (Cyan): `#00F0FF` (Active cockpit rider tag, interactive buttons, focus rings, cursors, waypoint links)
  - Success / Connected: `#10B981` (GPS synced dot, connected status, verified route badges, online mesh health)
  - Warning / Caution: `#F59E0B` (Sweeper role badges, gap alerts, telemetry pace highlight)
  - SOS / Hazard Alert: `#EF4444` (Header emergency SOS trigger button, crash beacons, dropout warnings)
  - Base Surface (Deep Dark): `#0A0F1D` (Global canvas, outer app frame, top app bar & bottom navigation bar background)
  - Elevated Container: `#0F172A` (Cards, search input box, roster sheets)
  - Subtle Container / High: `#161B2A` (Inner cards, table rows, convoy roll call list items, input fields)
  - Structural Border: `#1E293B` (Card outlines, tab dividers, input field borders)
  - Primary Text: `#FFFFFF` (Main trip titles, rider names, live speed & distance figures, primary button labels)
  - Secondary Text: `#94A3B8` (Field labels, bike models, host names, telemetry metadata headers)
  - Muted / Placeholder Text: `#64748B` (Search input placeholder `RSS1041`, inactive bottom nav tabs, timestamps)
- **Font & Typography Scale (`HudTypographyTokens` & `Type.kt`)**:
  - Primary Font Family: Chivo / Roboto (`FontFamily.SansSerif`)
  - Numbers / Telemetry Font: Tabular Monospace (`FontFamily.Monospace`)
  - App Title (RIDERSYNK): 18px - 20px, Bold (700), `+0.05em`, uppercase, `#FFFFFF`
  - Section Eyebrow Tags ([ACTIVE.SESSION_LIVE]): 11px - 12px, SemiBold (600), `+0.08em`, uppercase monospace, `#94A3B8` / `#00F0FF`
  - Trip Headings (Western Ghats Alpine Rally): 18px - 20px, Bold (700), Normal, `#FFFFFF`
  - Metric Figures (266 KM, 420 KM, 84 KPH): 20px - 24px, ExtraBold (800), Monospace / Tabular, `#FFFFFF` / `#00F0FF` / `#F59E0B`
  - Convoy Roster Names (Marcus Vance, Alex Rivera): 14px - 15px, Medium (500) / SemiBold, Normal, `#FFFFFF`
  - Subtitles / Bike Specs (Yamaha Ténéré 700, BMW R1250GS): 12px, Regular (400), Normal, `#94A3B8`
  - Badges / Roles (LEAD, SWEEPER, SLOT #2): 10px - 11px, Bold (700), `+0.05em`, uppercase
  - Bottom Navigation Tabs: 10px - 11px, SemiBold (600), `+0.04em`, uppercase, Active: `#00F0FF` / Inactive: `#64748B`
- **Text Formatting & UI Microcopy Conventions**:
  - System & Tactical Brackets: All technical section headers use tactical bracket syntax: `[SYS.CONVOY.ACCESS]`, `[ACTIVE.SESSION_LIVE]`, `[ICE // EMERGENCY MATRIX]`
  - High-Contrast Input Fields: Background: `#0F172A`, Typed Text: `#FFFFFF`, Placeholder Text: `#64748B`, Focused Caret / Cursor: `#00F0FF`
  - Trip Codes: Formatted as uppercase 7-character alphanumeric string (e.g. `RSS1041`).

---

## 4. Automatic Update Mandate

Whenever ANY code, database schema, endpoint, or feature is updated:
1. **Update `memory.md`**: Add the new milestone, bugfix, or architectural change to Section 3.
2. **Update `docs/`**: Update `docs/ARCHITECTURE.md`, `docs/API-REFERENCE.md`, `docs/QUALITY-ASSURANCE-GUIDE.md`, etc., automatically without prompting the user.

---

## 5. Carry-Forward Agenda & Next Steps

- [x] Integrate Supabase as secondary database with dual-write.
- [x] Configure automatic documentation and persistent memory tracking.
- [ ] Run full end-to-end convoy tracking session simulation test on real hardware.
- [ ] Maintain `memory.md` and `docs/` on all future modifications.
