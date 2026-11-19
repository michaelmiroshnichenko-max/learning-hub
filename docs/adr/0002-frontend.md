# 2. Frontend: server-side Qute templates + HTMX 2

Date: 2026-11-12

## Status

Accepted

## Context

Phase 2 needs simple pages: sign-in, note list with search and filters, note editor and view,
progress view, and account admin (FR-1 to FR-8). The note body is user Markdown and must be
rendered without running scripts or raw HTML (FR-5). Sessions must end immediately on sign-out,
account disable and password reset (FR-1, FR-2). The agent writes the code, and the owner
reviews it, so fewer languages, builds and dependencies are better. One deployable unit is
easiest to host on a free tier (NFR-1).

Options considered: Qute + HTMX 2, a React 19 + Vite SPA, and plain Qute without HTMX.
HTMX 4 exists only as a pre-release (`next` tag, 4.0.1); 2.0.11 is the stable `latest`.

## Decision

Render pages on the server with Quarkus Qute templates. Use HTMX 2.x (currently 2.0.11) for
partial updates such as search-as-you-type, filters, paging and status changes. Pin the HTMX
version and serve it from the application, not a CDN. Markdown is converted to HTML on the server
with raw HTML turned off, and the result is sanitized.

## Consequences

- One Java codebase, one build and one deployment. There is no SPA toolchain, no CORS setup and
  no tokens stored in the browser.
- Qute escapes output by default, and Markdown is rendered on the server. This keeps the FR-5
  rules in one place where tests can check them.
- Server-side sessions with cookies fit the session-ending rules in FR-1 and FR-2.
- Rich client interactions are harder to build. Use small additions only where needed.
- There are fewer Qute and HTMX examples than React examples. The agent may mix HTMX 2 and 4
  syntax, so agent instructions must name HTMX 2.
- UI behaviour is tested through HTTP responses (for example with RestAssured) instead of
  component tests. Add a few browser tests for key flows.
- Later API clients (MCP server, personal agent) will need separate JSON endpoints. These can be
  added next to the HTML endpoints when needed.
