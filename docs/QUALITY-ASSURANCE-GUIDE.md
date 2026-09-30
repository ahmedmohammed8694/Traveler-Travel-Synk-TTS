# RIDERsYNK Quality Assurance & Testing Guide

> [!NOTE]
> This document details testing strategies, build verification checks, and pre-release QA procedures for RIDERsYNK.

---

## 1. Automated Testing Pyramid

```
       /\
      /  \     End-to-End UI & API Tests (Capybara / Playwright)
     /----\    
    /      \    Integration Tests (Repository <-> Edge API <-> Supabase)
   /--------\   
  /          \  Unit Tests (JUnit 5, Kotlin Coroutine Test Harness)
 /------------\
```

---

## 2. Pre-Release Verification Checklist

Before deploying any backend release or tagging an Android APK:

### 2.1 Android Client Verification
Run the Gradle debug assembly command in PowerShell:
```powershell
./gradlew assembleDebug
```
- **Pass Criteria**: `BUILD SUCCESSFUL` exit code 0.
- **Output Artifact**: `app/build/outputs/apk/debug/app-debug.apk`.

### 2.2 Cloudflare Edge API Verification
Run Cloudflare Worker dry-run build:
```powershell
cd cloudflare-backend
npm run build
```
- **Pass Criteria**: Zero TypeScript compilation errors.

---

## 3. Mandatory Manual Smoke Tests

1. **New User Registration & Google Sign-In**:
   - Verify user record appears in local SharedPreferences, Cloudflare KV, and Supabase `users` table.
2. **Trip Creation & Lobby Code Generation**:
   - Verify trip code (e.g. `X9K2PL`) searches successfully and returns trip info without creating empty duplicates.
3. **Leaving / Exiting Trip**:
   - Verify rider is removed from active convoy roster and trip details are cleared from user view.
4. **GPS Telemetry Pings**:
   - Verify active rider coordinates update in Supabase `convoy_telemetry` table.
