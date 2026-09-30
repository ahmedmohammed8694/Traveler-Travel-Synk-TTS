export interface Env {
  DB: any;
  STORAGE: any;
  CONVOY_CACHE: any;
  USERS_KV?: any;
  TURNSTILE_SECRET_KEY?: string;
}

export default {
  async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    const url = new URL(request.url);

    // Enable CORS for Android client requests
    const corsHeaders = {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Turnstile-Token',
      'Content-Type': 'application/json',
    };

    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: corsHeaders });
    }

    // In-memory cache fallback for environments without KV bindings
    const memoryCache = (globalThis as any).__ridesync_cache || ((globalThis as any).__ridesync_cache = new Map<string, string>());

    try {
      // 0. Root Welcome Endpoint
      if (url.pathname === '/' || url.pathname === '') {
        return new Response(
          JSON.stringify({
            app: 'RIDERsYNK Cloudflare Edge API',
            status: 'LIVE & ACTIVE ✓',
            provider: 'Cloudflare Workers Edge Network',
            endpoints: {
              health: '/api/health',
              signUp: '/api/auth/signup (POST)',
              signIn: '/api/auth/signin (POST)',
              profile: '/api/auth/profile (GET/POST)',
              turnstileVerify: '/api/verify-turnstile (POST)',
              convoyPing: '/api/convoy/ping (POST)'
            },
            timestamp: Date.now()
          }),
          { status: 200, headers: corsHeaders }
        );
      }

      // 1. Health check endpoint
      if (url.pathname === '/api/health') {
        return new Response(
          JSON.stringify({ status: 'ok', provider: 'Cloudflare Workers Edge Database', timestamp: Date.now() }),
          { status: 200, headers: corsHeaders }
        );
      }

      // 1b. Google ID Token Authentication on Cloudflare Edge
      if (url.pathname === '/api/auth/google' && request.method === 'POST') {
        const body = await request.json() as { idToken?: string };
        const { idToken } = body;

        if (!idToken) {
          return new Response(
            JSON.stringify({ error: 'Google ID token is required' }),
            { status: 400, headers: corsHeaders }
          );
        }

        let tokenPayload: any = {};
        try {
          const parts = idToken.split('.');
          if (parts.length >= 2) {
            const base64Url = parts[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const padded = base64.padEnd(base64.length + (4 - (base64.length % 4)) % 4, '=');
            const jsonPayload = decodeURIComponent(
              atob(padded)
                .split('')
                .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                .join('')
            );
            tokenPayload = JSON.parse(jsonPayload);
          }
        } catch (e: any) {
          return new Response(
            JSON.stringify({ error: 'Invalid Google ID token payload', details: e.message }),
            { status: 400, headers: corsHeaders }
          );
        }

        const email = (tokenPayload.email || '').trim().toLowerCase();
        const displayName = tokenPayload.name || email.split('@')[0] || 'Rider';
        const photoUrl = tokenPayload.picture || '';
        const googleSub = tokenPayload.sub || '';
        const uid = googleSub ? `google_${googleSub}` : `cf_usr_${btoa(email || 'rider').replace(/=/g, '').substring(0, 16)}`;

        const userRecord = {
          uid,
          email,
          displayName,
          photoUrl,
          provider: 'google.com',
          lastLogin: Date.now()
        };

        if (env.CONVOY_CACHE) {
          if (email) await env.CONVOY_CACHE.put(`user:${email}`, JSON.stringify(userRecord));
          await env.CONVOY_CACHE.put(`user_id:${uid}`, JSON.stringify(userRecord));
        } else {
          if (email) memoryCache.set(`user:${email}`, JSON.stringify(userRecord));
          memoryCache.set(`user_id:${uid}`, JSON.stringify(userRecord));
        }

        return new Response(
          JSON.stringify({
            success: true,
            provider: 'Cloudflare Edge Auth (Google)',
            user: userRecord
          }),
          { status: 200, headers: corsHeaders }
        );
      }

      // 2. Cloudflare Edge Auth: User Sign-Up Endpoint
      if (url.pathname === '/api/auth/signup' && request.method === 'POST') {
        const body = await request.json() as { email?: string; password?: string; displayName?: string };
        const { email, password, displayName } = body;

        if (!email || !password) {
          return new Response(
            JSON.stringify({ error: 'Email and password are required' }),
            { status: 400, headers: corsHeaders }
          );
        }

        const cleanEmail = email.trim().toLowerCase();
        const uid = 'cf_usr_' + btoa(cleanEmail).replace(/=/g, '').substring(0, 16);
        const userRecord = {
          uid,
          email: cleanEmail,
          displayName: displayName || cleanEmail.split('@')[0],
          createdAt: Date.now()
        };

        // Cache/Store user in KV or in-memory fallback
        if (env.CONVOY_CACHE) {
          await env.CONVOY_CACHE.put(`user:${cleanEmail}`, JSON.stringify({ ...userRecord, password }));
          await env.CONVOY_CACHE.put(`user_id:${uid}`, JSON.stringify(userRecord));
        } else {
          memoryCache.set(`user:${cleanEmail}`, JSON.stringify({ ...userRecord, password }));
          memoryCache.set(`user_id:${uid}`, JSON.stringify(userRecord));
        }

        return new Response(
          JSON.stringify({
            success: true,
            provider: 'Cloudflare Edge Auth',
            user: userRecord
          }),
          { status: 200, headers: corsHeaders }
        );
      }

      // 3. Cloudflare Edge Auth: User Sign-In Endpoint
      if (url.pathname === '/api/auth/signin' && request.method === 'POST') {
        const body = await request.json() as { email?: string; password?: string };
        const { email, password } = body;

        if (!email || !password) {
          return new Response(
            JSON.stringify({ error: 'Email and password are required' }),
            { status: 400, headers: corsHeaders }
          );
        }

        const cleanEmail = email.trim().toLowerCase();
        let userRecord: any = null;

        if (env.CONVOY_CACHE) {
          const cachedData = await env.CONVOY_CACHE.get(`user:${cleanEmail}`);
          if (cachedData) {
            userRecord = JSON.parse(cachedData);
          }
        } else if (memoryCache.has(`user:${cleanEmail}`)) {
          userRecord = JSON.parse(memoryCache.get(`user:${cleanEmail}`)!);
        }

        // Deterministic UID based on email
        if (!userRecord) {
          const uid = 'cf_usr_' + btoa(cleanEmail).replace(/=/g, '').substring(0, 16);
          userRecord = {
            uid,
            email: cleanEmail,
            displayName: cleanEmail.split('@')[0],
            createdAt: Date.now()
          };
        }

        return new Response(
          JSON.stringify({
            success: true,
            provider: 'Cloudflare Edge Auth',
            user: {
              uid: userRecord.uid,
              email: userRecord.email,
              displayName: userRecord.displayName
            }
          }),
          { status: 200, headers: corsHeaders }
        );
      }

      // 4. Cloudflare User Profile Endpoint (Get/Save)
      if (url.pathname === '/api/auth/profile') {
        if (request.method === 'POST') {
          const profile = await request.json() as any;
          const userId = profile.userId || 'unknown';
          const email = (profile.email || '').trim().toLowerCase();

          // Smart merge with existing profile in cache if fields are missing
          let existing: any = null;
          if (env.CONVOY_CACHE) {
            let cached = await env.CONVOY_CACHE.get(`profile:${userId}`);
            if (!cached && email) cached = await env.CONVOY_CACHE.get(`profile_email:${email}`);
            if (cached) existing = JSON.parse(cached);
          } else {
            let cached = memoryCache.get(`profile:${userId}`);
            if (!cached && email) cached = memoryCache.get(`profile_email:${email}`);
            if (cached) existing = JSON.parse(cached);
          }

          const mergedProfile = {
            ...existing,
            ...profile,
            photoUrl: profile.photoUrl || existing?.photoUrl || '',
            activeVehicleId: profile.activeVehicleId || existing?.activeVehicleId || '',
            vehicles: (profile.vehicles && profile.vehicles.length > 0) ? profile.vehicles : (existing?.vehicles || [])
          };

          const profileJson = JSON.stringify(mergedProfile);
          if (env.CONVOY_CACHE) {
            await env.CONVOY_CACHE.put(`profile:${userId}`, profileJson);
            if (email) {
              await env.CONVOY_CACHE.put(`profile_email:${email}`, profileJson);
            }
          } else {
            memoryCache.set(`profile:${userId}`, profileJson);
            if (email) {
              memoryCache.set(`profile_email:${email}`, profileJson);
            }
          }

          if (env.DB) {
            try {
              await env.DB.prepare(`
                INSERT INTO users (user_id, email, display_name, photo_url, mobile_number, date_of_birth, vehicle_model, tank_capacity_liters, emergency_contact_phone, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(user_id) DO UPDATE SET
                  display_name = excluded.display_name,
                  photo_url = excluded.photo_url,
                  mobile_number = excluded.mobile_number,
                  date_of_birth = excluded.date_of_birth,
                  vehicle_model = excluded.vehicle_model,
                  tank_capacity_liters = excluded.tank_capacity_liters,
                  emergency_contact_phone = excluded.emergency_contact_phone
              `).bind(
                userId,
                email || `${userId}@ridesync.app`,
                mergedProfile.displayName || 'Rider',
                mergedProfile.photoUrl || '',
                mergedProfile.mobileNumber || '',
                mergedProfile.dateOfBirth || '',
                mergedProfile.vehicleModel || '',
                mergedProfile.tankCapacityLiters || 15.0,
                mergedProfile.privacySettings?.emergencyContactPhone || '',
                Date.now()
              ).run();
            } catch (dbErr) {
              console.error('D1 user profile upsert error:', dbErr);
            }
          }

          return new Response(
            JSON.stringify({ success: true, userId, profile: mergedProfile, updated: Date.now() }),
            { status: 200, headers: corsHeaders }
          );
        }

        if (request.method === 'GET') {
          const userId = url.searchParams.get('userId') || 'unknown';
          const email = (url.searchParams.get('email') || '').trim().toLowerCase();
          let profile: any = null;

          if (env.CONVOY_CACHE) {
            let cached = await env.CONVOY_CACHE.get(`profile:${userId}`);
            if (!cached && email) {
              cached = await env.CONVOY_CACHE.get(`profile_email:${email}`);
            }
            if (cached) profile = JSON.parse(cached);
          } else {
            let cached = memoryCache.get(`profile:${userId}`);
            if (!cached && email) {
              cached = memoryCache.get(`profile_email:${email}`);
            }
            if (cached) profile = JSON.parse(cached);
          }

          return new Response(
            JSON.stringify({ success: true, profile }),
            { status: 200, headers: corsHeaders }
          );
        }
      }

      // 5. Turnstile Bot Verification Endpoint
      if (url.pathname === '/api/verify-turnstile' && request.method === 'POST') {
        const body = await request.json() as { token?: string };
        const turnstileToken = body.token;

        if (!turnstileToken) {
          return new Response(
            JSON.stringify({ success: false, error: 'Missing turnstile token' }),
            { status: 400, headers: corsHeaders }
          );
        }

        const verifyResponse = await fetch('https://challenges.cloudflare.com/turnstile/v0/siteverify', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: new URLSearchParams({
            secret: env.TURNSTILE_SECRET_KEY || '1x0000000000000000000000000000000AA',
            response: turnstileToken,
          }),
        });

        const outcome = await verifyResponse.json() as { success: boolean };
        return new Response(JSON.stringify(outcome), { status: 200, headers: corsHeaders });
      }

      // 6. Realtime Convoy GPS Telemetry Ingestion
      if (url.pathname === '/api/convoy/ping' && request.method === 'POST') {
        const ping = await request.json() as any;
        const riderId = ping.riderId || 'unknown_rider';

        if (env.CONVOY_CACHE) {
          await env.CONVOY_CACHE.put(`rider:${riderId}`, JSON.stringify(ping), { expirationTtl: 300 });
        }

        return new Response(
          JSON.stringify({ success: true, riderId, syncedAt: Date.now() }),
          { status: 200, headers: corsHeaders }
        );
      }

      // 7. Multi-Segment Trip Planning & D1 Database Sync Endpoints
      if (url.pathname === '/api/trip/create' && request.method === 'POST') {
        const tripData = await request.json() as any;
        const tripId = tripData.tripId || `trip_${Date.now().toString(36)}`;
        const rawLobby = tripData.lobbyCode || '';
        const lobbyCode = rawLobby ? rawLobby.toUpperCase().trim() : Math.random().toString(36).substring(2, 8).toUpperCase();
        
        const fullTrip = {
          ...tripData,
          tripId,
          lobbyCode,
          createdTimestamp: tripData.createdTimestamp || Date.now(),
          status: tripData.status || 'ACTIVE'
        };

        if (env.CONVOY_CACHE) {
          await env.CONVOY_CACHE.put(`trip:${tripId}`, JSON.stringify(fullTrip));
          await env.CONVOY_CACHE.put(`code:${lobbyCode}`, JSON.stringify(fullTrip));
          const listRaw = await env.CONVOY_CACHE.get('all_trips_index');
          const list: string[] = listRaw ? JSON.parse(listRaw) : [];
          if (!list.includes(tripId)) {
            list.unshift(tripId);
            await env.CONVOY_CACHE.put('all_trips_index', JSON.stringify(list));
          }
        }

        if (env.DB) {
          try {
            await env.DB.prepare(
              `INSERT OR REPLACE INTO saved_trips 
              (trip_id, planner_id, title, origin_name, destination_name, start_lat, start_lng, dest_lat, dest_lng, distance_km, duration_minutes, category, lobby_code, scheduled_date, active_riders_count, created_at) 
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
            ).bind(
              fullTrip.tripId,
              fullTrip.plannerId || 'user_unknown',
              fullTrip.title || 'Untitled Trip',
              fullTrip.originName || '',
              fullTrip.destinationName || '',
              fullTrip.startLatLng?.latitude || fullTrip.startLat || 0,
              fullTrip.startLatLng?.longitude || fullTrip.startLng || 0,
              fullTrip.destLatLng?.latitude || fullTrip.destLat || 0,
              fullTrip.destLatLng?.longitude || fullTrip.destLng || 0,
              fullTrip.distanceKm || 0,
              fullTrip.durationMinutes || 0,
              fullTrip.category || 'UPCOMING',
              fullTrip.lobbyCode,
              fullTrip.scheduledDate || '',
              fullTrip.activeRidersCount || 1,
              fullTrip.createdTimestamp
            ).run();
          } catch (e: any) {
            console.error("D1 Trip Insert Note: ", e?.message);
          }
        }

        return new Response(
          JSON.stringify({ success: true, trip: fullTrip }),
          { status: 200, headers: corsHeaders }
        );
      }

      // 7b. Public Online Trips Listing Endpoint (Fresh Installs Access)
      if (url.pathname === '/api/trips/public' || url.pathname === '/api/trips/all') {
        let trips: any[] = [];
        if (env.DB) {
          try {
            const { results } = await env.DB.prepare("SELECT * FROM saved_trips ORDER BY created_at DESC").all();
            if (results && results.length > 0) trips = results;
          } catch (e: any) {
            console.error("D1 Select Error: ", e?.message);
          }
        }

        if (trips.length === 0 && env.CONVOY_CACHE) {
          const listRaw = await env.CONVOY_CACHE.get('all_trips_index');
          if (listRaw) {
            const tripIds: string[] = JSON.parse(listRaw);
            for (const tid of tripIds) {
              const tRaw = await env.CONVOY_CACHE.get(`trip:${tid}`);
              if (tRaw) trips.push(JSON.parse(tRaw));
            }
          }
        }

        return new Response(
          JSON.stringify({ success: true, count: trips.length, trips }),
          { status: 200, headers: corsHeaders }
        );
      }

      // 7c. Lookup Trip by Lobby Code / QR Code / Deep Link
      if (url.pathname === '/api/trip/by-code' && request.method === 'GET') {
        const rawCode = url.searchParams.get('code') || '';
        const code = rawCode.trim().toUpperCase();

        if (!code) {
          return new Response(
            JSON.stringify({ error: 'Missing join code parameter' }),
            { status: 400, headers: corsHeaders }
          );
        }

        let trip: any = null;
        if (env.CONVOY_CACHE) {
          const cached = await env.CONVOY_CACHE.get(`code:${code}`);
          if (cached) trip = JSON.parse(cached);
        }

        if (!trip && env.DB) {
          try {
            const { results } = await env.DB.prepare("SELECT * FROM saved_trips WHERE UPPER(lobby_code) = ? LIMIT 1").bind(code).all();
            if (results && results.length > 0) trip = results[0];
          } catch (e: any) {
            console.error("D1 Code Lookup Error: ", e?.message);
          }
        }

        return new Response(
          JSON.stringify({ success: true, code, trip }),
          { status: 200, headers: corsHeaders }
        );
      }

      // 7d. Reset All Online Database Trip Entries
      if (url.pathname === '/api/trip/reset' && request.method === 'POST') {
        if (env.CONVOY_CACHE) {
          const listRaw = await env.CONVOY_CACHE.get('all_trips_index');
          if (listRaw) {
            const tripIds: string[] = JSON.parse(listRaw);
            for (const tid of tripIds) {
              await env.CONVOY_CACHE.delete(`trip:${tid}`);
            }
          }
          await env.CONVOY_CACHE.delete('all_trips_index');
        }

        if (env.DB) {
          try {
            await env.DB.prepare("DELETE FROM saved_trips").run();
            await env.DB.prepare("DELETE FROM itinerary_stops").run();
            await env.DB.prepare("DELETE FROM trip_route_segments").run();
            await env.DB.prepare("DELETE FROM convoy_gps_pings").run();
          } catch (e: any) {
            console.error("D1 Reset Error: ", e?.message);
          }
        }

        return new Response(
          JSON.stringify({ success: true, message: 'All online database trip records reset successfully.' }),
          { status: 200, headers: corsHeaders }
        );
      }

      if (url.pathname === '/api/trip/get' && request.method === 'GET') {
        const tripId = url.searchParams.get('tripId');
        if (!tripId) {
          return new Response(
            JSON.stringify({ error: 'Missing tripId parameter' }),
            { status: 400, headers: corsHeaders }
          );
        }

        let trip: any = null;
        if (env.CONVOY_CACHE) {
          const cached = await env.CONVOY_CACHE.get(`trip:${tripId}`);
          if (cached) trip = JSON.parse(cached);
        }

        if (!trip && env.DB) {
          try {
            const { results } = await env.DB.prepare("SELECT * FROM saved_trips WHERE trip_id = ? LIMIT 1").bind(tripId).all();
            if (results && results.length > 0) trip = results[0];
          } catch (e: any) {
            console.error("D1 Get Trip Error: ", e?.message);
          }
        }

        return new Response(
          JSON.stringify({ success: true, trip }),
          { status: 200, headers: corsHeaders }
        );
      }

      if (url.pathname === '/api/trip/segment/select' && request.method === 'POST') {
        const body = await request.json() as { tripId?: string; segmentId?: string };
        const { tripId, segmentId } = body;

        if (!tripId || !segmentId) {
          return new Response(
            JSON.stringify({ error: 'tripId and segmentId are required' }),
            { status: 400, headers: corsHeaders }
          );
        }

        if (env.CONVOY_CACHE) {
          const cached = await env.CONVOY_CACHE.get(`trip:${tripId}`);
          if (cached) {
            const trip = JSON.parse(cached);
            trip.activeSegmentId = segmentId;
            await env.CONVOY_CACHE.put(`trip:${tripId}`, JSON.stringify(trip));
          }
        }

        return new Response(
          JSON.stringify({ success: true, tripId, activeSegmentId: segmentId }),
          { status: 200, headers: corsHeaders }
        );
      }

      // 8. AI Itinerary Extraction & Parsing Endpoint
      if (url.pathname === '/api/trip/parse-itinerary' && request.method === 'POST') {
        const body = await request.json() as { rawText?: string; defaultTitle?: string };
        const rawText = body.rawText || '';
        const defaultTitle = body.defaultTitle || 'Cloudflare AI Trip Plan';

        const lines = rawText.split('\n').map(l => l.trim()).filter(l => l.length > 0);
        let title = defaultTitle;
        let duration = '';
        let dates = '';
        const days: any[] = [];
        let currentDayNum = 1;
        let currentDayTitle = 'Day 1';
        let currentStops: any[] = [];

        for (const line of lines) {
          if (line.toLowerCase().startsWith('trip:') || line.toLowerCase().startsWith('trip plan:')) {
            title = line.substring(line.indexOf(':') + 1).trim();
            continue;
          }
          if (line.toLowerCase().startsWith('duration:')) {
            duration = line.substring(line.indexOf(':') + 1).trim();
            continue;
          }
          if (line.toLowerCase().startsWith('dates:') || line.toLowerCase().startsWith('date:')) {
            dates = line.substring(line.indexOf(':') + 1).trim();
            continue;
          }

          const dayMatch = line.match(/^Day\s*(\d+)[:\-\s]*(.*)$/i);
          if (dayMatch) {
            if (currentStops.length > 0) {
              days.push({ dayNumber: currentDayNum, dayTitle: currentDayTitle, stops: [...currentStops] });
              currentStops = [];
            }
            currentDayNum = parseInt(dayMatch[1], 10) || (days.length + 1);
            const sub = dayMatch[2]?.trim() || '';
            currentDayTitle = sub ? `Day ${currentDayNum}: ${sub}` : `Day ${currentDayNum}`;
            continue;
          }

          // Stop parsing
          let cleanLine = line.replace(/^[-*•\d.]+\s*/, '').trim();
          if (!cleanLine) continue;

          let visitTime = '';
          const timeMatch = cleanLine.match(/(\b\d{1,2}:\d{2}\s*(?:AM|PM|am|pm)?(?:\\s*-\\s*\\d{1,2}:\\d{2}\s*(?:AM|PM|am|pm)?)?\b)/i);
          if (timeMatch) {
            visitTime = timeMatch[0].trim();
            cleanLine = cleanLine.replace(visitTime, '').replace(/^[-:\s]+/, '').trim();
          }

          const parts = cleanLine.includes('|') ? cleanLine.split('|') : cleanLine.includes(' - ') ? cleanLine.split(' - ') : [cleanLine];
          const stopName = parts[0].trim().replace(/[-:]+$/, '').trim();
          const activity = parts.length > 1 ? parts[1].trim() : '';

          currentStops.push({
            stopId: `stop_${Date.now()}_${currentStops.length}`,
            stopName: stopName || `Milestone ${currentStops.length + 1}`,
            activityDescription: activity,
            estimatedVisitTime: visitTime,
            rawLocationText: stopName,
            latitude: 0,
            longitude: 0,
            status: 'PENDING',
            orderIndex: currentStops.length
          });
        }

        if (currentStops.length > 0 || days.length === 0) {
          days.push({ dayNumber: currentDayNum, dayTitle: currentDayTitle, stops: currentStops });
        }

        const plan = {
          planId: `plan_${Date.now().toString(36)}`,
          tripTitle: title,
          totalDuration: duration,
          startDate: dates,
          days
        };

        return new Response(
          JSON.stringify({ success: true, plan }),
          { status: 200, headers: corsHeaders }
        );
      }

      // 9. Stop Status Update & Real-Time Sync Endpoint
      if (url.pathname === '/api/trip/stop/status' && request.method === 'POST') {
        const body = await request.json() as { tripId?: string; dayNumber?: number; stopId?: string; status?: string };
        const { tripId, dayNumber, stopId, status } = body;

        if (!tripId || !stopId || !status) {
          return new Response(
            JSON.stringify({ error: 'tripId, stopId, and status are required' }),
            { status: 400, headers: corsHeaders }
          );
        }

        if (env.CONVOY_CACHE) {
          const cached = await env.CONVOY_CACHE.get(`trip:${tripId}`);
          if (cached) {
            const trip = JSON.parse(cached);
            if (trip.itineraryPlan && trip.itineraryPlan.days) {
              const targetDay = trip.itineraryPlan.days.find((d: any) => d.dayNumber === dayNumber);
              if (targetDay && targetDay.stops) {
                const targetStop = targetDay.stops.find((s: any) => s.stopId === stopId);
                if (targetStop) {
                  targetStop.status = status;
                  await env.CONVOY_CACHE.put(`trip:${tripId}`, JSON.stringify(trip));
                }
              }
            }
          }
        }

        return new Response(
          JSON.stringify({ success: true, tripId, dayNumber, stopId, status, syncedAt: Date.now() }),
          { status: 200, headers: corsHeaders }
        );
      }


      // Default 404 handler
      return new Response(
        JSON.stringify({ error: 'Endpoint not found on Cloudflare Edge' }),
        { status: 404, headers: corsHeaders }
      );
    } catch (err: any) {
      return new Response(
        JSON.stringify({ error: 'Internal Cloudflare Worker Error', details: err.message }),
        { status: 500, headers: corsHeaders }
      );
    }
  },
};
