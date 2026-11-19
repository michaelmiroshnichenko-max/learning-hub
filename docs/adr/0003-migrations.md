# 3. Database migrations: Flyway 13

Date: 2026-11-12

## Status

Accepted

## Context

The schema lives in PostgreSQL (vision constraints). It will grow over the course:
users and roles, notes, tags, links and progress (phase 2), full-text search indexes (FR-6), and
later vectors for semantic search (weeks 14-15). The agent writes the migrations, and the owner
reviews them, so they should be easy to read. PostgreSQL-specific features such as `tsvector`,
GIN indexes and pgvector will be used, so database independence is not needed.
Migrations must run the same way in dev, CI and production.

Options considered: Flyway 13 and Liquibase 5.

## Decision

Use Flyway 13 (currently 13.12.1) through the `quarkus-flyway` extension, with plain SQL
migrations in `src/main/resources/db/migration` named `V<n>__<description>.sql`.
Migrations run at application start (`quarkus.flyway.migrate-at-start=true`).

## Consequences

- Migrations are plain PostgreSQL SQL. They are short, easy to review, and can use full-text
  search and pgvector directly.
- The Quarkus extension needs only a few settings, and Dev Services gives every test run a real
  PostgreSQL database.
- There are no automatic rollbacks. Undo is a paid Flyway feature. Fixes are made with new forward
  migrations, and backups (NFR-6) protect against bad migrations.
- A migration that has already run must never be changed. The agent may change one or reuse a
  version number. Add this rule to the agent instructions, and let CI fail on checksum errors
  (Flyway `validate`).
- Moving to another database would mean rewriting migrations. This is accepted.
