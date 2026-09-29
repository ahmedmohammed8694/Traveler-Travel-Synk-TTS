-- RIDERsYNK Cloudflare D1 Production Database Schema

-- 1. Users & Rider Profiles Table
CREATE TABLE IF NOT EXISTS users (
    user_id TEXT PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    display_name TEXT NOT NULL,
    photo_url TEXT,
    mobile_number TEXT,
    date_of_birth TEXT,
    vehicle_model TEXT,
    tank_capacity_liters REAL DEFAULT 15.0,
    emergency_contact_phone TEXT,
    created_at INTEGER NOT NULL
);

-- 2. Saved Trips Table
CREATE TABLE IF NOT EXISTS saved_trips (
    trip_id TEXT PRIMARY KEY,
    planner_id TEXT NOT NULL,
    title TEXT NOT NULL,
    origin_name TEXT NOT NULL,
    destination_name TEXT NOT NULL,
    start_lat REAL NOT NULL,
    start_lng REAL NOT NULL,
    dest_lat REAL NOT NULL,
    dest_lng REAL NOT NULL,
    distance_km REAL NOT NULL,
    duration_minutes INTEGER NOT NULL,
    category TEXT CHECK(category IN ('ONGOING', 'UPCOMING', 'COMPLETED')) DEFAULT 'UPCOMING',
    lobby_code TEXT UNIQUE,
    scheduled_date TEXT,
    active_riders_count INTEGER DEFAULT 1,
    avg_speed_kmh INTEGER DEFAULT 0,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (planner_id) REFERENCES users(user_id)
);

-- 3. Route Segments Table (Multi-Day Trips)
CREATE TABLE IF NOT EXISTS trip_route_segments (
    segment_id TEXT PRIMARY KEY,
    trip_id TEXT NOT NULL,
    segment_name TEXT NOT NULL,
    origin_name TEXT,
    destination_name TEXT,
    encoded_polyline TEXT,
    distance_km REAL DEFAULT 0.0,
    estimated_duration_minutes INTEGER DEFAULT 0,
    order_index INTEGER DEFAULT 0,
    FOREIGN KEY (trip_id) REFERENCES saved_trips(trip_id) ON DELETE CASCADE
);

-- 4. Itinerary Day Stops Table
CREATE TABLE IF NOT EXISTS itinerary_stops (
    stop_id TEXT PRIMARY KEY,
    trip_id TEXT NOT NULL,
    day_number INTEGER NOT NULL DEFAULT 1,
    stop_name TEXT NOT NULL,
    activity_description TEXT,
    visit_time TEXT,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    status TEXT CHECK(status IN ('PENDING', 'COMPLETED', 'SKIPPED')) DEFAULT 'PENDING',
    order_index INTEGER DEFAULT 0,
    FOREIGN KEY (trip_id) REFERENCES saved_trips(trip_id) ON DELETE CASCADE
);

-- 5. Realtime Convoy GPS Pings Log
CREATE TABLE IF NOT EXISTS convoy_gps_pings (
    ping_id TEXT PRIMARY KEY,
    trip_id TEXT NOT NULL,
    rider_id TEXT NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    speed_kmh REAL DEFAULT 0.0,
    bearing REAL DEFAULT 0.0,
    timestamp INTEGER NOT NULL
);
