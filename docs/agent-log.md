# Agent log

One row per Copilot CLI session. Source: local Copilot session store (`/chronicle`), `git log`.
AI credits = sum of `total_nano_aiu` for the session (rounded); cross-check with `/usage` at the end of new sessions.

| Date | Stage | Session goal | Turns | AI credits | Manual edits | Agent mistake | Fix (where) |
|------|-------|--------------|-------|------------|--------------|---------------|-------------|
| 11-11 | docs | requirements interview + 2 rubber-duck reviews | 5 | 125 | no | added "re-enables an account" (FR-2) not in vision; untestable/unclear FR-1 lockout, FR-3 invalid week | requirements.md: Open questions OQ-14..OQ-21 |
| 11-12 | docs | options, ADR 0001-0004, architecture, roadmap | 6 | 278 | no | roadmap: `note.owner_id` needs `account`, but `account` created only in S5 (+10 more gaps) | roadmap.md S1: minimal `account` table; architecture.md |
| 11-19 | S0 | Quarkus skeleton, AGENTS.md | 4 | 77 | `gh auth switch` (env) | - | - |
| 11-19 | S0 | CI workflow + badge, PR #1 | 8 | 86 | test name `learning-hub2` (deliberate CI check) | suggested `gh auth refresh -u` (flag does not exist in this gh) | - |
| 11-26 | plan | data layer plan: ADR 0005, 0006, architecture, AGENTS.md "Database" | 6 | 334 | no | ADR number clash (0005 = hosting and persistence); `account_role` cascade contradicts constraint table | architecture.md, roadmap.md (hosting ADR -> 0007) |
| 11-26 | S1 | notes CRUD (tests first) | 7 | 628 | no | `courseWeek` beyond int range -> 400 instead of 422; tests failed on JVM zone `Europe/Kiev` | test + `ProblemExceptionMappers`; pom: surefire `-Duser.timezone=UTC`, AGENTS.md Stack |
| 11-26 | S2 | tags + filter by tag, N+1 guard, OpenAPI diff in CI | 9 | 543 | no | `page * size` int overflow -> 500; tag deleted while note saved -> error; 2 prompts lost (no response, resent) | tests + `NoteListQuery` cap; N+1 guard test; AGENTS.md "No N+1"; ci.yml OpenAPI diff |
| 11-26 | seed | dev seed `R__dev_seed.sql` | 7 | 325 | no | changed `%dev` Flyway locations on a running dev DB -> "applied migration not resolved locally", 500 | restart dev DB; AGENTS.md: seed only in `db/dev-seed`, only `%dev` |
| 11-26 | MCP | dev profile: fixed port 5433, `mcp_readonly` role | 1 | 76 | no | - | - |
| 11-26 | MCP | fix `learninghub-db`: docker -> podman, host, password | 1 | 30 | no | - | `.mcp.json` |
| 11-26 | MCP | fix connection + data analysis via MCP | 5 | 186 | no | wrong diagnosis first (start order), real cause: old MCP SDK in image; check 4 SQL rebuilt from code, not taken from log | local image `postgres-mcp:0.3.0-mcp1.30`; prompt: show tool output, enable `quarkus.hibernate-orm.log.sql` |

## Week 4 summary

| Stage | Sessions | Turns | AI credits |
|-------|----------|-------|------------|
| S0 skeleton + CI | 2 | 12 | 163 |

Agent share: 3 of 8 commits by the end of week 4 have `Co-authored-by: Copilot`.

## Week 5 summary

### Agent mistakes by category

| Category | Count | Where the fix is recorded |
|----------|-------|---------------------------|
| Data model and migrations (types, constraints, editing an old migration) | 2 | architecture.md (cascade); AGENTS.md seed rule (Flyway locations) |
| JPA (N+1, lazy loading, cascades, transactions) | 1 | test + fix in S2 (tag deleted during note save); N+1 prevented by guard test |
| API and errors (entities in response, 500 instead of 4xx, contract) | 2 | tests only (`courseWeek` 400->422, page overflow 500->422) - **no AGENTS.md rule yet** |
| Tests (H2, `@TestTransaction`, order dependence, changing tests to fit code) | 1 | pom + AGENTS.md Stack (UTC time zone) |
| Data and MCP (explicit ids, non-determinism, invented numbers) | 2 | prompt: show tool output and real SQL log |

### Cost

| Step | Sessions | Turns | AI credits |
|------|----------|-------|------------|
| Data layer plan (ADR, docs) | 1 | 6 | 334 |
| S1 notes | 1 | 7 | 628 |
| S2 tags and filter | 1 | 9 | 543 |
| Seed script | 1 | 7 | 325 |
| MCP: setup and analysis | 3 | 7 | 291 |
| **Total** | **7** | **36** | **2120** |

Agent share: 3 of 14 commits (21%). Week 5 commits (6) have no `Co-authored-by: Copilot` trailer, so agent code is undercounted.
