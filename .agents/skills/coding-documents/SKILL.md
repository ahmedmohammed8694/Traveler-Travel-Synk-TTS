---
name: Coding Documents
description: Analyzes project codebases and generates comprehensive, high-quality, production-ready application documentation including AI collaboration workflows, architecture specs, API references, prompt engineering guides, QA test plans, and recovery procedures based on vibecoding principles.
---

# Coding Documents Skill (VibeCoding Standard)

This skill provides a systematic framework for analyzing any application codebase and generating relevant, high-context, production-grade documentation tailored for both human developers and AI coding agents.

It is inspired by the **cpjet64/vibecoding** methodology, which emphasizes structured human-AI collaboration, rich architectural transparency, prompt engineering guides, QA frameworks, and robust recovery procedures.

---

## 1. When to Use This Skill

Activate or invoke this skill whenever you need to:
1. **Generate Comprehensive Project Documentation**: Create missing or full-suite technical documentation for an application.
2. **Audit & Document Architecture**: Produce detailed system design docs (`ARCHITECTURE.md`), database schemas, and dataflow diagrams.
3. **Draft AI Collaboration Guides**: Define `AI-COLLABORATION-WORKFLOW.md` and `PROMPT-ENGINEERING-GUIDE.md` for AI pair programming.
4. **Create API & Security Documentation**: Generate `API-REFERENCE.md` with endpoints, payload schemas, CORS policies, and auth flows.
5. **Establish QA & Recovery Procedures**: Document `QUALITY-ASSURANCE-GUIDE.md` and `RECOVERY-PROCEDURES.md` for error handling and fallback workflows.

---

## 2. Core Documentation Modules

When executing this skill, generate or maintain the following structured document modules:

### Module 1: `ARCHITECTURE.md`
- **System Overview**: High-level topology (Frontend/Client, Backend/Edge, Database layers).
- **Data Model & Schemas**: Database entity tables, column data types, constraints, and relationships.
- **Data Flow & Lifecycle**: Request-response pathways, asynchronous sync loops, fallback logic.
- **Tech Stack Tokens**: Technologies, frameworks, and library versions in use.

### Module 2: `API-REFERENCE.md`
- **Endpoints Catalog**: Comprehensive list of REST/GraphQL/RPC endpoints.
- **Request/Response Specs**: JSON payloads, query params, headers (Auth, CORS, API keys).
- **Error Matrix**: HTTP status codes, error message formats, and retry logic.

### Module 3: `AI-COLLABORATION-WORKFLOW.md`
- **AI Agent Protocols**: Rules for subagent dispatching, code modification constraints, test-driven execution loops.
- **Context Injection**: How to feed repository context, environment variables, and Knowledge Items (KIs) to AI tools.
- **Pair Programming Boundaries**: What AI agents can automate vs. what requires explicit human review.

### Module 4: `PROMPT-ENGINEERING-GUIDE.md`
- **Feature Prompt Templates**: Structured prompt patterns for adding new UI components, backend routes, or DB migrations.
- **Style & Design System Prompts**: Visual aesthetic tokens (glassmorphism, color palettes, typography).
- **Refactoring & Debugging Prompts**: Root-cause investigation prompts and test-driven fixes.

### Module 5: `QUALITY-ASSURANCE-GUIDE.md`
- **Test Strategy**: Unit testing (JUnit/Jest), Integration testing, and E2E testing (Playwright/Capybara).
- **Pre-Release Checklist**: Verification commands (`assembleDebug`, `npm test`, linter checks).
- **Regression Detection**: Procedures to prevent breaking existing API contracts and UI states.

### Module 6: `RECOVERY-PROCEDURES.md`
- **Diagnostic Runbooks**: Step-by-step troubleshooting for offline DBs, network timeouts, or permission errors.
- **Fallback Mechanisms**: Secondary database failovers, cache restoration, and local persistence fallbacks.
- **Emergency Rollback**: Git rollback commands, database migration rollbacks, and release hotfixes.

---

## 3. Step-by-Step Codebase Analysis Workflow

When asked to generate documentation for a project:

1. **Scan Codebase**:
   - Inspect build files (`build.gradle`, `package.json`, `Cargo.toml`, etc.) to identify frameworks and dependencies.
   - Inspect source directories (`src/`, `app/src/`, `backend/`, `api/`) to map system structure.
   - Inspect database files (`schema.sql`, migrations, ORM entities, Firestore rules).

2. **Synthesize Architecture**:
   - Identify primary database vs. secondary/fallback database.
   - Map authentication flows, API routes, and client-server sync channels.

3. **Generate/Update Documents**:
   - Output clean, GitHub-flavored markdown files formatted with alerts, mermaid diagrams, code blocks, and clear table structures.
   - Ensure all file paths and symbols are formatted as markdown links.

---

## 4. Verification Checklist

Before completing documentation generation:
- [ ] Are all API endpoints and data schemas accurate to the current codebase?
- [ ] Are code links clickable and correctly formatted?
- [ ] Do QA commands match the build tools in the repository?
- [ ] Are recovery protocols realistic and verified against code fallbacks?
