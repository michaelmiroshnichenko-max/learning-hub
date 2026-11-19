# Learning Hub — Roadmap (phase 2, weeks 4-7)

Status: Draft v0.2 · Date: 2026-11-19 · Sources: docs/requirements.md, docs/architecture.md, docs/adr/

## Rules

- One stage = one agent session = one PR into `main`. CI must be green before merge (NFR-11).
- Each stage is a vertical slice: Flyway migration + module code + Qute/HTMX view + tests for its FRs.
  Tables and columns are created in the stage that first needs them, not up front (ADR 0003).
- Every test for an FR carries a JUnit tag with the FR id, e.g. `@Tag("FR-3")`, so a stage can be
  verified on its own. CI always runs the full suite.
- Commands are for CI and Linux/macOS. On Windows use `.\mvnw.cmd` with `JAVA_HOME` set to JDK 25.
- A stage starts only when its open questions (see "Decisions before a stage") are answered.
- Stages that touch FR-1, FR-2, FR-9, cookies or tokens get a human review before merge (ADR 0004).
- Record AI credits used per stage in the PR description (NFR-2).
- Until S5, tests use `@TestSecurity` and dev mode uses a dev-profile-only learner seeded from a
  separate Flyway location (architecture §2). Nothing is publicly deployed before S5.

## Stages

| Stage | Goal | FR ids | Week | Verify command | Done when |
|-------|------|--------|------|----------------|-----------|
| S0 | Skeleton: Quarkus 3.40 on Java 25, PostgreSQL via Dev Services, Flyway (`db/migration`, `migrate-at-start`), `shared` module (problem-details mapper, base layout, HTMX 2.0.11 served from `/static`, `htmx.config.responseHandling` for 400/404/422), JSON logging, health check, ArchUnit module rules, `AGENTS.md`, GitHub Actions CI, Dockerfile | — (NFR-5, NFR-9, NFR-11; ADR 0001-0003) | 4 | `./mvnw verify` | CI green on the PR; `/q/health/ready` returns UP in a `@QuarkusTest`; ArchUnit and Flyway `validate` run in CI; `docker build` succeeds in CI; `AGENTS.md` lists build/test commands, module rules, FR test tags, migration rules (naming, never edit a merged migration), no secrets in the repo, no `plan` folder in commits |
| S1 | Create and view a note: minimal `account` table (`id`, `login`, `created_at`), dev/test learner seed, `note` table, new-note form with data-policy notice, show page with safe Markdown rendering | FR-3, FR-5 (view), FR-9 | 5 | `./mvnw verify -Dgroups=FR-3,FR-5,FR-9` | Valid note is saved with owner and times; empty title/body, >100 KB body and week outside 1-26 are rejected with 422 problem details naming the field or limit; `<script>`, raw HTML and `javascript:` links render inert; another user's note id returns 404; the seed location is not enabled in the prod profile |
| S2 | Tags and source links on create and view: `tag`, `note_tag`, `source_link` tables | FR-4, FR-9 | 5 | `./mvnw verify -Dgroups=FR-4,FR-9` | Tags stored lowercase and deduplicated ignoring case; 11 distinct tags rejected; a non-http(s) link rejected and identified; tags are per owner and never visible to another user |
| S3 | Edit and delete own notes | FR-5, FR-9 | 5 | `./mvnw verify -Dgroups=FR-5,FR-9` | Edit applies FR-3 and FR-4 rules and updates `updated_at`; delete removes the note, its links, its tag links and orphan tags; edit/delete of another user's note returns 404; OQ-12 behaviour tested |
| S4 | Note list, text search, tag and week filters, paging | FR-6, FR-7, FR-9 | 5 | `./mvnw verify -Dgroups=FR-6,FR-7,FR-9` | No query shows all own notes newest first, 20 per page; text match on title and body ignores case, per OQ-1; all selected tags required; week filter; combined criteria; "no notes found" message; other users' notes never appear |
| S5 | Sign-in and sign-out: decide ADR 0004 (Accepted or superseded); add sign-in columns to `account`, `account_role`, `auth_session`; first-Admin bootstrap from env; bcrypt; SmallRye JWT in `HttpOnly`/`Secure`/`SameSite=Strict` cookie; short-lived token with renewal; per-request session check; 8 h inactivity; lockout; `@RolesAllowed`; dev seed gets password hashes | FR-1, FR-9 | 6 | `./mvnw verify -Dgroups=FR-1,FR-9` | Valid credentials start a session; wrong credentials give one generic message; 5 failures lock for 15 min, also for valid credentials; expired, inactive (8 h), signed-out or ended sessions are rejected; token renewal stops once a session ends; every page except sign-in and health requires a session; no password or token in logs (NFR-8); human review done |
| S6 | Admin account management: list, create, disable, reset password | FR-2, FR-9 | 6 | `./mvnw verify -Dgroups=FR-2,FR-9` | Admin creates an account with roles; duplicate login rejected with a reason; disable and reset end all sessions of that user at once; non-Admin gets 403; Admin pages show no notes, tags, links or progress; human review done |
| S7 | Hosting and backups: ADR 0005 (free-tier app host and PostgreSQL), provision the production database, daily backup job with 7-day retention, restore test | — (NFR-1, NFR-6) | 6 | `pg_restore --list <latest-backup-file>` | ADR 0005 accepted; a backup is created automatically every day; one backup restored into a fresh database and row counts checked before week 7 ends (NFR-6); cost $0 |
| S8 | Progress by course week: `course_start` and `week_progress` tables, week statuses, note counts via `note.api`, overall %, current-week highlight | FR-8, FR-9 | 7 | `./mvnw verify -Dgroups=FR-8,FR-9` | Invalid start date rejected and the old one kept; 26 week ranges derived; all weeks "not started" by default; invalid week or status changes nothing; overall % = done weeks / 26; current week highlighted only when a start date is set, in the OQ-5 time zone; counts include only own notes |
| S9 | Deploy the app: deploy job in GitHub Actions to the ADR 0005 host, secrets in env, HTTPS only, first Admin created from env | — (NFR-1, NFR-5, NFR-7, NFR-11) | 7 | `curl -fsS https://<public-host>/q/health/ready` | App runs at a public HTTPS URL at $0/month; HTTP redirected or refused; first request after idle answers correctly within 30 s; signing key and DB credentials only in host/CI secrets |
| S10 | Phase 2 acceptance: Maven profile `e2e` with Playwright browser tests and its own CI job; NFR-4 load check with NFR-3 seed data; cost and agent-share report | FR-1, FR-3, FR-6, FR-8 (end-to-end) · NFR-2, NFR-3, NFR-4, NFR-11 | 7 | `./mvnw verify -Pe2e` | `e2e` profile defined in `pom.xml`; CI installs browsers and the job fails when a browser test fails; tests pass for sign-in, create note with a visible validation error, search, set week status; search p95 ≤ 1 s and sign-in, create, edit, list and progress p95 ≤ 2 s (profile per OQ-20); AI credits per week and total recorded; agent code share measured per OQ-8 and > 90% |

## Decisions before a stage

| Stage | Open questions to answer first | Why |
|-------|-------------------------------|-----|
| S1 | OQ-7, OQ-11, OQ-17 | Supported browsers/language; title and link limits; "empty" and 100 KB rules |
| S3 | OQ-12 | Concurrent edits and save after delete |
| S4 | OQ-1 | Search matching rule and whether an index is needed |
| S5 | OQ-2, OQ-15, OQ-16, OQ-21; ADR 0004 decision | Password policy, activity definition, login identifier, lockout counter |
| S6 | OQ-4, OQ-10, OQ-14 | Last-Admin protection, early unlock, re-enable |
| S7 | OQ-18 | Whether a restore may bring back deleted notes |
| S8 | OQ-5, OQ-6 | Time zone for the current week; fixed 26 weeks |
| S10 | OQ-8, OQ-19, OQ-20 | Agent-share metric; capacity limits; load-test profile |

## FR coverage

| FR | Module | Stages |
|----|--------|--------|
| FR-1 Sign in and sign out | `auth` | S5, S10 |
| FR-2 Manage user accounts | `account` | S6 |
| FR-3 Create a note | `note` | S1, S10 |
| FR-4 Tags and source links | `note` | S2 |
| FR-5 View, edit, delete | `note` | S1, S3 |
| FR-6 Search by text | `note` | S4, S10 |
| FR-7 Filter by tags and week | `note` | S4 |
| FR-8 Progress by course week | `progress` | S8, S10 |
| FR-9 Note privacy | all | S1-S8 (every slice adds its own privacy tests) |

## Risks to the schedule

- Week 6 carries all the security code (S5, S6) plus S7. S7 cannot move later, because NFR-6 needs a
  restore before week 7. Choose the host during week 5 so ADR 0005 is ready.
- If S6 slips, it moves to week 7 before S9; S9 must not deploy without S5 and S6 merged.
- The NFR-4 load check is not reduced to search only. If it fails or cannot finish, the gap is written
  into the week-7 report and agreed explicitly; the requirement is not dropped silently.
- S5 depends on the ADR 0004 decision. If Keycloak or form auth is chosen, S5 changes but S1-S4 do not,
  because they only use the current-user lookup in `shared`.

## Later (phases 3-6, not in this roadmap)

| Item | Requirement | Week | Fits into (docs/architecture.md §6) |
|------|-------------|------|-------------------------------------|
| MCP server: Copilot sessions read and add notes; import of learning-log.md | L-1 | 9 | New `mcp` module over `note.api`, bearer JWT |
| Semantic search | L-2 | 14-15 | New `embedding` module, pgvector, `note_chunk` table |
| Ask my notes (RAG) | L-3 | 14-15 | New `ask` module over `embedding` and `note.api` |
| Quiz generation and answer evals | L-4 | 16-17 | New `quiz` module; needs FRs first |
| Personal agent | L-5 | 22-23 | External client via `mcp` |
| Export own notes as Markdown | L-6 | open | `GET /notes/export` in `note` |
| Admin delete account, audit trail, self-service password reset | OQ-3, OQ-9, OQ-13 | open | Not needed for phase 2; new FRs first, then new stages |
