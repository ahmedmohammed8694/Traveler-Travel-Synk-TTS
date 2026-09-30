-- ==========================================================
-- RIDERsYNK Supabase Postgres Database Migration Schema
-- Project: oktfyxdrvscmifomtlkp
-- ==========================================================

-- 1. Users Table
CREATE TABLE IF NOT EXISTS public.users (
    uid TEXT PRIMARY KEY,
    email TEXT UNIQUE,
    display_name TEXT NOT NULL,
    photo_url TEXT DEFAULT '',
    auth_provider TEXT DEFAULT 'google',
    vehicle_model TEXT DEFAULT '',
    active_vehicle_id TEXT DEFAULT '',
    emergency_contact TEXT DEFAULT '',
    created_at BIGINT DEFAULT (extract(epoch from now())*1000)
);

-- 2. Saved Trips Table
CREATE TABLE IF NOT EXISTS public.saved_trips (
    trip_id TEXT PRIMARY KEY,
    planner_id TEXT NOT NULL DEFAULT 'user_host',
    title TEXT NOT NULL,
    origin_name TEXT NOT NULL,
    destination_name TEXT NOT NULL,
    start_lat DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    start_lng DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    dest_lat DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    dest_lng DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    distance_km DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    duration_minutes INT NOT NULL DEFAULT 0,
    category TEXT DEFAULT 'UPCOMING',
    lobby_code TEXT UNIQUE NOT NULL,
    scheduled_date TEXT DEFAULT '',
    active_riders_count INT DEFAULT 1,
    created_at BIGINT DEFAULT (extract(epoch from now())*1000)
);

-- 3. Convoy Telemetry Table
CREATE TABLE IF NOT EXISTS public.convoy_telemetry (
    session_id TEXT NOT NULL,
    rider_id TEXT NOT NULL,
    rider_name TEXT NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    speed_kmh DOUBLE PRECISION DEFAULT 0.0,
    battery_pct INT DEFAULT 100,
    updated_at BIGINT DEFAULT (extract(epoch from now())*1000),
    PRIMARY KEY (session_id, rider_id)
);

-- 4. Enable Row Level Security (RLS) & Public Access Policies for API
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.saved_trips ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.convoy_telemetry ENABLE ROW LEVEL SECURITY;

-- Policies for saved_trips
DROP POLICY IF EXISTS "Allow all on saved_trips" ON public.saved_trips;
CREATE POLICY "Allow all on saved_trips" ON public.saved_trips FOR ALL USING (true) WITH CHECK (true);

-- Policies for users
DROP POLICY IF EXISTS "Allow all on users" ON public.users;
CREATE POLICY "Allow all on users" ON public.users FOR ALL USING (true) WITH CHECK (true);

-- Policies for convoy_telemetry
DROP POLICY IF EXISTS "Allow all on convoy_telemetry" ON public.convoy_telemetry;
CREATE POLICY "Allow all on convoy_telemetry" ON public.convoy_telemetry FOR ALL USING (true) WITH CHECK (true);
