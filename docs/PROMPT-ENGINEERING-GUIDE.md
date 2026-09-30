# RIDERsYNK Prompt Engineering Guide

> [!TIP]
> Use these structured prompt recipes when prompting AI agents to work on RIDERsYNK features, UI screens, API endpoints, or bug fixes.

---

## 1. Feature Development Prompt Template

```markdown
Role: Senior Android & Cloud Systems Engineer
Task: Add [Feature Name] to RIDERsYNK.

Context:
- Client: Android Kotlin app using Jetpack Compose and Coroutines.
- Edge API: Cloudflare Worker in `cloudflare-backend/src/index.ts`.
- Database: Supabase Postgres (`oktfyxdrvscmifomtlkp`).

Requirements:
1. Update client repository `TripRepository.kt` or `AuthRepositoryImpl.kt`.
2. Add corresponding endpoint to `cloudflare-backend/src/index.ts`.
3. Dual-write changes asynchronously to Supabase REST API `/rest/v1/[table]`.
4. Verify by compiling with `./gradlew assembleDebug`.
```

---

## 2. Jetpack Compose UI Prompt Template

```markdown
Task: Build [Screen Name] in Jetpack Compose for RIDERsYNK.

Design & Aesthetic Rules:
- Theme: Dark Mode with vibrant neon accents (Glassmorphism, high contrast).
- Typography: Material3 Typography with custom title badges.
- State: Flow & StateFlow collectAsStateWithLifecycle().
- Edge-to-Edge: Respect system bars insets and predictive back gestures.
```

---

## 3. Systematic Debugging Prompt Template

```markdown
Task: Debug runtime crash / build error in [Component Name].

Instructions:
1. View the exact log output using `view_file` on the task log.
2. Identify line numbers and missing symbols or null pointer dereferences.
3. Fix root cause without swallowing exceptions or deleting tests.
4. Re-run verification command `./gradlew assembleDebug`.
```
