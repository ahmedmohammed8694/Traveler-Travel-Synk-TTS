# Live Backend Database Sync & Schema Provisioning Policy

> **MANDATORY BACKEND PERSISTENCE RULE FOR ALL DATA OPERATIONS**

---

## 1. Direct Backend Connectivity Rule
- Every data creation, edit, deletion, or user action MUST directly communicate with live backend storage targets:
  1. **Supabase PostgreSQL REST API:** `https://oktfyxdrvscmifomtlkp.supabase.co/rest/v1/`
  2. **Cloudflare Edge Services (D1 SQL & KV):** `/api/trips`, `/api/auth/profile`, `/api/convoy`
  3. **Google Cloud Firestore:** Real-time convoy location sync and profile code lookup.
- Local memory / cache layers (`_tripsFlow`, `_knownTravelers`) act strictly as instant reactive UI caches and MUST always trigger parallel asynchronous network persistence.

---

## 2. Automatic Schema & Storage Provisioning
- Whenever a new data feature or extra metadata field (e.g. `profile_code`, `waypoint_lat_lngs`, `stop_status`, `itinerary_plan`) is added to models:
  1. You MUST check `schema.sql` (Cloudflare D1) and `supabase_schema.sql` (Supabase Postgres) to ensure table definitions include the column.
  2. Add missing column DDL statements (`ALTER TABLE ... ADD COLUMN IF NOT EXISTS ...`) to `schema.sql` and `supabase_schema.sql`.
  3. Ensure serializer/deserializer functions map the new column cleanly.

---

## 3. Backend Health & Active Connection Assurance
- Always maintain active network fallback handling (timeout handling, automatic retry, dual-write to Supabase + Cloudflare).
- Never default to dummy mock data when real backend service calls are required.
