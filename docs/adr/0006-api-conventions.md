# 6. JSON API conventions for notes and tags

Date: 2026-11-26

## Status

Accepted

## Context

S1 to S4 deliver a JSON API first, and the note HTML views follow in stage S4H (roadmap). ADR 0002 allows
JSON endpoints next to HTML ones; this ADR changes only the delivery order, not the ADR 0002 decision. MCP clients will use the same API later (L-1). Errors must name
the field or the limit (FR-3, FR-4). Responses must not contain note content in errors or stack
traces (NFR-9). A user must not learn whether another user's data exists (FR-9). Lists show
20 notes per page, newest updated first (FR-6), and tag filters require all selected tags (FR-7).

## Decision

- Paths: `/api/notes`, `/api/notes/{id}`, `/api/tags`, `/api/tags/{id}`. JSON in and out.
- Errors (7): one set of exception mappers in `shared` writes RFC 9457 problem details as
  `application/problem+json` with `type`, `title`, `status`, `detail`, `instance` and
  `errors: [{field, message}]`. Validation errors return 422. No new dependency.
- Ownership: an unknown id and another owner's id both return the same 404.
- Tags (8): a note request sends tag names (`"tags": ["Java", "quarkus"]`). The server normalises
  them to lowercase, removes duplicates, rejects more than 10 distinct tags with 422, and creates
  missing tags for the current owner.
- Paging (9): `?page=0&size=20`, where `page` starts at 0, the default `size` is 20 and the maximum
  is 100. A negative page or a size outside 1–100 returns 422. Response envelope:
  `{items, page, size, totalItems, totalPages}`.
- Sort: `updatedAt desc, id desc`. The sort is fixed and has no parameter.
- Filter: `?tag=<name>`, which can be repeated. A note must have all given tags. An unknown tag
  gives an empty page, not an error. `?week=<1-26>` returns only notes of that course week; any other
  value returns 422. All given filters must match (FR-7).

## Consequences

- The error format is exactly the one in architecture §5. The same mapper can render HTML
  fragments for HTMX later. Tests must check that no stack trace and no input values leak.
  The default Quarkus validation response must be replaced.
- Tag names fit forms and MCP with a single call. Two parallel requests can create the same tag,
  so tag creation uses `INSERT ... ON CONFLICT DO NOTHING`. Each lookup must filter by `owner_id`.
- Typos create new tags. Deleting a tag through `/api/tags/{id}` is the only cleanup.
- Offset paging is supported by Panache (`Page.of`, `count()`). The count query is cheap at the
  NFR-3 size. The `id` tie-breaker prevents duplicates across pages. Rows can shift between pages
  when notes are edited, which is accepted.
- A 0-based `page` is documented in OpenAPI, because an agent or a client may assume 1-based pages.
