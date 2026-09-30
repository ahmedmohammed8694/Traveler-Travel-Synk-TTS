# RIDERsYNK API Reference

> [!IMPORTANT]
> Base Edge API URL: `https://ahmedmohammed8694-riders-ride-sync.mdahmed08061994.workers.dev`
> Base Supabase REST API URL: `https://oktfyxdrvscmifomtlkp.supabase.co/rest/v1`

---

## 1. Authentication Endpoints

### 1.1 Google Authentication (`POST /api/auth/google`)
Validates a Google OAuth ID Token on the Cloudflare Edge API and returns/registers the user record.

- **Headers**: `Content-Type: application/json`
- **Request Body**:
```json
{
  "idToken": "eyJhbGciOiJSUzI1NiIs..."
}
```
- **Success Response (200 OK)**:
```json
{
  "success": true,
  "provider": "Cloudflare Edge Auth (Google)",
  "user": {
    "uid": "google_102847194729104",
    "email": "rider@example.com",
    "displayName": "Rider",
    "photoUrl": "https://lh3.googleusercontent.com/a/...",
    "provider": "google.com",
    "lastLogin": 1727712345000
  }
}
```

---

### 1.2 User Profile (`GET / POST /api/auth/profile`)
Retrieves or updates rider profile settings, vehicle details, and emergency contacts.

- **GET Parameters**: `?userId=USER_ID&email=EMAIL`
- **POST Request Body**:
```json
{
  "userId": "google_102847194729104",
  "displayName": "Alex Rider",
  "email": "alex@example.com",
  "mobileNumber": "+1234567890",
  "vehicleModel": "Ducati Multistrada V4",
  "tankCapacityLiters": 22.0,
  "privacySettings": {
    "shareLocationWithGroup": true,
    "emergencyContactPhone": "+1987654321"
  }
}
```

---

## 2. Trip Management Endpoints

### 2.1 Create / Sync Trip (`POST /api/trip/create`)
Creates or upserts a trip into Cloudflare D1, KV index, and Supabase Postgres database.

- **Request Body**:
```json
{
  "tripId": "trip_9x8a7b",
  "plannerId": "user_host_123",
  "title": "Mountain Coast Run",
  "originName": "San Francisco",
  "destinationName": "Big Sur",
  "startLatLng": { "latitude": 37.7749, "longitude": -122.4194 },
  "destLatLng": { "latitude": 36.2704, "longitude": -121.8081 },
  "distanceKm": 235.4,
  "durationMinutes": 210,
  "category": "UPCOMING",
  "lobbyCode": "RSS1041",
  "scheduledDate": "2026-10-05"
}
```

---

### 2.2 Lookup Trip by Join Code (`GET /api/trip/by-code`)
Searches for active trip details using a 6-character alphanumeric lobby code.

- **Query Parameters**: `?code=RSS1041`
- **Response (200 OK)**:
```json
{
  "success": true,
  "code": "RSS1041",
  "trip": {
    "trip_id": "trip_9x8a7b",
    "title": "Mountain Coast Run",
    "origin_name": "San Francisco",
    "destination_name": "Big Sur",
    "lobby_code": "RSS1041",
    "category": "UPCOMING",
    "active_riders_count": 3
  }
}
```

---

### 2.3 Join Trip (`POST /api/trip/join`)
Adds a rider to a convoy trip roster and updates total active riders count.

- **Request Body**:
```json
{
  "tripId": "trip_9x8a7b",
  "lobbyCode": "RSS1041",
  "riderProfile": {
    "riderId": "user_rider_456",
    "displayName": "Rider Chris",
    "bikeModel": "BMW R1250GS",
    "role": "MEMBER",
    "status": "Joined & Confirmed"
  }
}
```

---

### 2.4 Leave / Exit Trip (`POST /api/trip/leave`)
Removes a rider from an active trip session.

- **Request Body**:
```json
{
  "tripId": "trip_9x8a7b",
  "riderId": "user_rider_456"
}
```

---

### 2.5 Delete Trip (`POST /api/trip/delete`)
Deletes a trip from Cloudflare D1, KV cache, and Supabase Postgres.

- **Request Body**:
```json
{
  "tripId": "trip_9x8a7b"
}
```

---

## 3. Realtime Convoy Telemetry Endpoints

### 3.1 Convoy GPS Ping (`POST /api/convoy/ping`)
Ingests high-frequency GPS position, speed, and battery metrics.

- **Request Body**:
```json
{
  "sessionId": "trip_9x8a7b",
  "riderId": "user_rider_456",
  "riderName": "Rider Chris",
  "latitude": 37.7750,
  "longitude": -122.4190,
  "speedKmh": 65.5,
  "batteryPct": 88
}
```
