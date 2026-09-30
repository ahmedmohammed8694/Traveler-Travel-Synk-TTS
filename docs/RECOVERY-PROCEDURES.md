# RIDERsYNK System Recovery & Fallback Procedures

> [!WARNING]
> Follow these runbooks in case of network outages, database synchronization failures, or corrupted local caches.

---

## 1. Cloudflare Edge API Outage Runbook

**Symptom**: Cloudflare Workers return HTTP 500/503 or network requests time out.

**Automated Fallback Sequence**:
1. `TripRepository.kt` catches `HttpURLConnection` exception.
2. App automatically falls back to **Direct Supabase REST API** (`https://oktfyxdrvscmifomtlkp.supabase.co/rest/v1/saved_trips`).
3. If Supabase is also unreachable, app falls back to **Local SharedPreferences Cache**.

**Manual Recovery Action**:
Verify Worker health status by opening:
`https://ahmedmohammed8694-riders-ride-sync.mdahmed08061994.workers.dev/api/health`

---

## 2. Supabase Secondary Database Outage Runbook

**Symptom**: Supabase REST API returns error or `401 Unauthorized`.

**Automated Fallback Sequence**:
1. Cloudflare Edge Worker logs `Supabase Secondary Sync Note: [Error]` silently without breaking client response.
2. Primary Cloudflare D1 database and KV cache continue serving live trip requests.

**Manual Recovery Action**:
1. Verify API Key and URL in `cloudflare-backend/.env` and `TripRepository.kt`:
   - `SUPABASE_URL`: `https://oktfyxdrvscmifomtlkp.supabase.co`
   - `SUPABASE_PUBLISHABLE_KEY`: `sb_publishable_dF8gDIF6Ahw4wWPRfYOH7Q_AaJrBrO8`
2. Check Table Row Level Security (RLS) policies in Supabase SQL Editor:
   ```sql
   CREATE POLICY "Allow all on saved_trips" ON public.saved_trips FOR ALL USING (true) WITH CHECK (true);
   ```

---

## 3. Corrupted Local Cache Cleanup Runbook

**Symptom**: User sees outdated trips or incorrect join state after app reinstall.

**Manual Recovery Action**:
1. Clear App Data via Android Settings -> Apps -> RIDERsYNK -> Storage -> Clear Data.
2. Or trigger online reset endpoint via curl:
   ```bash
   curl -X POST https://ahmedmohammed8694-riders-ride-sync.mdahmed08061994.workers.dev/api/trip/reset
   ```
