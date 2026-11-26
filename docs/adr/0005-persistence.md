# 5. Persistence: Hibernate ORM with Panache on PostgreSQL

Date: 2026-11-26

## Status

Accepted

## Context

Stages S1 and S2 add the tables `note`, `tag`, `note_tag` and `source_link` (FR-3, FR-4, FR-5, FR-9).
Flyway owns the schema (ADR 0003). The agent writes most of the code, so the agent should follow the
Quarkus guides' default style, keep the code small and make few choices. The data is small
(NFR-3: 10 users, 5,000 notes each), so performance does not decide anything here.
Tags differ only by case must be one tag and are stored in lowercase (FR-4). Times are shown in a
time zone that is not decided yet (OQ-5).

## Decision

1. IDs: the `note`, `tag` and `source_link` entities extend `PanacheEntity` (`Long` id from a Hibernate
   sequence). Flyway creates `<table>_seq` with `INCREMENT BY 50`. Tables with a `uuid` or composite key
   (`account`, `account_role`, `auth_session`, `week_progress`, `course_start`) extend `PanacheEntityBase`
   with their own `@Id` or `@EmbeddedId` and have no sequence. `note_tag` is a join table without an entity.
2. Times: `timestamptz` columns mapped to `java.time.Instant`.
3. Strings: `varchar(n)` columns with matching `@Size(max = n)`. The limits come from OQ-11.
   The note body stays `text`, and the 100 KB limit is checked in code (OQ-17).
4. Tag names: trimmed and lowercased with `Locale.ROOT` before validation and storage.
   The table has `UNIQUE (owner_id, name)`.
5. Deleting a tag removes its `note_tag` rows (`ON DELETE CASCADE`). The notes stay.
6. Data access uses the active record pattern: public fields and static finders on the entity.
   `quarkus.hibernate-orm.schema-management.strategy=validate`.

## Consequences

- Least code, and it matches the examples in the Quarkus 3.40 Panache guide that the agent knows.
- Sequence names and the increment must match what Hibernate expects. `validate` and tests catch drift.
- Ids can be guessed. FR-9 still holds because every query filters by `owner_id`, and a note of
  another owner returns 404.
- Ids differ from `uuid` in architecture §4. Architecture §4 must be updated.
- `Instant` is an absolute time. The display zone is applied only in the view (OQ-5).
  Tests truncate times to microseconds, because PostgreSQL stores microseconds.
- Each limit is set in two places (column and `@Size`), and changing one needs a migration.
  This replaces the "limits are config values" rule in architecture §5 for string lengths.
- One `CHECK (name = lower(name))` is not added, so all writes must go through the normalising code.
- Deleting a tag changes notes silently, and there is no undo (FR-5).
  The `updated_at` of the affected notes is not changed.
- Static finders can be called from anywhere. Every finder takes `ownerId`. Resources must not
  call inherited `findById`, `listAll` or `count` directly. Review and an ArchUnit rule check this (FR-9).
  Tests that mock entities need `quarkus-panache-mock`.
