# RIDERsYNK AI Collaboration & Pair Programming Workflow

> [!NOTE]
> This guide defines protocols for AI coding agents (Google Antigravity, Gemini, GitHub Copilot) working on the **RIDERsYNK** codebase alongside human engineers.

---

## 1. Core Operating Principles

1. **Always Inspect Authority Sources**: Never guess column names, variable names, or file paths. Inspect `SavedTrip.kt`, `index.ts`, `supabase_schema.sql`, or `build.gradle` before writing code.
2. **Empirical Verification Required**: Never claim a task or feature is complete without running verification commands (`./gradlew assembleDebug` or `npm test`).
3. **No Superficial Symptom Patches**: Fix root causes; never swallow exceptions silently or delete failing assertions.
4. **Preserve Database Multi-Tier Alignment**: Any edit to data models in Kotlin must be reflected in `index.ts` (Cloudflare Worker) and `supabase_schema.sql` (Supabase Postgres).

---

## 2. AI Subagent & Execution Loops

```mermaid
sequenceDiagram
    autonumber
    actor Developer as Human Engineer
    participant Agent as Antigravity AI Agent
    participant Code as Codebase Files
    participant Build as Gradle / Worker Compiler

    Developer->>Agent: Request feature or bugfix (e.g., "/codingdocuments")
    Agent->>Code: Inspect existing files & schemas via view_file / grep_search
    Agent->>Code: Apply changes via replace_file_content / multi_replace_file_content
    Agent->>Build: Run verification command (e.g. ./gradlew assembleDebug)
    Build-->>Agent: Output logs & exit status
    alt Build Failure
        Agent->>Code: Read exact log traceback & fix root cause
        Agent->>Build: Retry build command
    end
    Agent-->>Developer: Provide clear, concise summary with clickable file links
```

---

## 3. Pair-Programming Boundaries

| Action | Allowed for AI Agent | Requires Human Review |
| :--- | :---: | :---: |
| Modify Kotlin ViewModels & Repositories | Yes | No |
| Add Cloudflare Edge API endpoints | Yes | No |
| Execute `./gradlew assembleDebug` | Yes | No |
| Modify Supabase Security Rules (RLS) | Yes | Yes |
| Delete production D1/Supabase databases | **NO (Blocked)** | **YES (Explicit Consent)** |
