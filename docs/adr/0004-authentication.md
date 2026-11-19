# 4. Authentication: Quarkus built-in JWT

Date: 2026-11-12

## Status

Proposed. The final decision will be made in week 6.

## Context

FR-1 and FR-2 need username or email and password sign-in, a lockout after 5 failed attempts
for 15 minutes, an 8-hour inactivity timeout, and sign-out. Admins can create accounts, disable
them and reset passwords. Disabling an account or resetting a password must end the user's
active sessions. There is no self-registration and no SSO or social login (out of scope).
Passwords must never be stored or logged in plain text (NFR-8). Everything must run on free tiers
(NFR-1), and the first request after an idle period must get a response within 30 s (NFR-5).
Later, MCP clients (week 9) and a personal agent will call the hub without a browser.

Options considered: OIDC with Keycloak 26.8, Quarkus built-in JWT (SmallRye JWT), and Quarkus
form authentication with `security-jpa`. Keycloak covers most of FR-1 and FR-2 itself. However,
it is a second service that needs about 0.5–1 GB of memory, has its own cold start and needs its
own backups. This does not fit the free-tier and cold-start limits.

## Decision

Use Quarkus built-in security with SmallRye JWT. The application keeps its own user accounts
with bcrypt password hashes, signs short-lived JWTs after sign-in, and checks them on every request.
For the browser, the token is kept in an `HttpOnly`, `Secure`, `SameSite` cookie.
Role checks use `@RolesAllowed` (`learner`, `admin`).

## Consequences

- No extra service to host, back up or warm up. This keeps the project at $0 and within NFR-5.
- The same tokens can later authenticate MCP and agent clients.
- The application must implement login, lockout, password hashing, the inactivity timeout and
  account admin itself. This is security-critical code written by the agent, so each FR-1 and FR-2
  rule needs explicit tests. Security changes always get a human review.
- A JWT cannot be revoked on its own. To end sessions immediately (sign-out, disable, password reset),
  each request must also be checked against the database, for example a per-user token version or
  a denylist. Without this, FR-1 and FR-2 are not met.
- The signing key must be a secret in deployment and in CI. It must never be committed.
- Open questions OQ-2, OQ-10, OQ-15 and OQ-21 affect the design and should be answered before week 6.
- If this approach proves too costly or risky in week 6, the alternatives are form authentication
  or Keycloak. If either is chosen, this ADR is superseded.
