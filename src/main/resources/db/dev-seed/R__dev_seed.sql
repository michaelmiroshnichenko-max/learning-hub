-- Dev profile only (%dev.quarkus.flyway.locations); never enabled in test or production.
-- Invented study data for two owners: dev-user (150 notes, 25 tags) and other-user (20 notes, 5 tags).
--
-- Idempotent: accounts are inserted if missing, then all notes and tags of the two seed owners are deleted
-- (note_tag cascades) and inserted again, so every run ends with the same rows. Manual edits of these
-- two owners' data are lost when Flyway reruns this script (only after the file changes).
-- Deterministic: no random(); every value is derived from the row number. Dates are relative to the start
-- of the current UTC day, so created_at always lies within the last 90 days.
-- Ids come from note_seq / tag_seq (each nextval reserves its own block of 50, so no clash with Hibernate).
-- Do not use dollar-brace sequences in this file: Flyway would treat them as placeholders.

INSERT INTO account (id, login) VALUES
    ('00000000-0000-0000-0000-00000000000d', 'dev-user'),
    ('00000000-0000-0000-0000-00000000000e', 'other-user')
ON CONFLICT (id) DO NOTHING;

DELETE FROM note WHERE owner_id IN ('00000000-0000-0000-0000-00000000000d', '00000000-0000-0000-0000-00000000000e');
DELETE FROM tag WHERE owner_id IN ('00000000-0000-0000-0000-00000000000d', '00000000-0000-0000-0000-00000000000e');

DROP TABLE IF EXISTS pg_temp.dev_seed_topic;
DROP TABLE IF EXISTS pg_temp.dev_seed_note;

CREATE TEMP TABLE dev_seed_topic (
    t        int PRIMARY KEY,
    name     text   NOT NULL,
    intro    text   NOT NULL,
    lang     text   NOT NULL,
    code     text   NOT NULL,
    subjects text[] NOT NULL,   -- 10 per topic
    tags     text[] NOT NULL    -- 5 per topic, 25 in total for dev-user
);

CREATE TEMP TABLE dev_seed_note (
    owner_id    uuid        NOT NULL,
    n           int         NOT NULL,
    title       text        NOT NULL,
    body        text        NOT NULL,
    course_week smallint,
    created_at  timestamptz NOT NULL,
    updated_at  timestamptz NOT NULL,
    tags        text[]      NOT NULL,
    PRIMARY KEY (owner_id, n),
    UNIQUE (owner_id, title)    -- note_tag rows are matched by (owner_id, title)
);

INSERT INTO dev_seed_topic (t, name, intro, lang, code, subjects, tags) VALUES
(1, 'Java',
 'Core Java knowledge that every backend task builds on.',
 'java',
 $code$record Lesson(String topic, int week) {
}

Map<Integer, Long> lessonsPerWeek = lessons.stream()
        .collect(Collectors.groupingBy(Lesson::week, TreeMap::new, Collectors.counting()));$code$,
 ARRAY['records and sealed types', 'streams and collectors', 'virtual threads', 'pattern matching for switch',
       'Optional done right', 'equals and hashCode', 'generics and wildcards', 'the Java memory model',
       'garbage collectors', 'CompletableFuture chains'],
 ARRAY['java', 'jvm', 'concurrency', 'records', 'streams']),
(2, 'Quarkus',
 'Quarkus keeps the inner loop fast: dev mode, Dev Services and continuous testing.',
 'java',
 $code$@Path("/api/hello")
public class HelloResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello from dev mode";
    }
}$code$,
 ARRAY['CDI scopes and beans', 'REST endpoints with Jakarta REST', 'Dev Services', 'configuration profiles',
       'Panache entities', 'Flyway at startup', 'Bean Validation errors', 'health checks',
       'continuous testing', 'native image builds'],
 ARRAY['quarkus', 'cdi', 'jakarta-rest', 'dev-services', 'native-image']),
(3, 'PostgreSQL',
 'The database is where correctness and speed are won or lost.',
 'sql',
 $code$EXPLAIN (ANALYZE, BUFFERS)
SELECT id, title
FROM note
WHERE owner_id = :owner_id
ORDER BY updated_at DESC, id DESC
LIMIT 20;$code$,
 ARRAY['B-tree indexes', 'EXPLAIN ANALYZE', 'transaction isolation', 'MVCC and VACUUM', 'window functions',
       'common table expressions', 'JSONB columns', 'constraints and foreign keys', 'sequences and identity',
       'keyset pagination'],
 ARRAY['postgresql', 'sql', 'indexing', 'flyway', 'transactions']),
(4, 'AI agents',
 'An agent is a language model in a loop that can call tools and decide when to stop.',
 'python',
 $code$def run_agent(goal, tools, max_steps=5):
    history = [goal]
    for step in range(max_steps):
        action = plan_next(history, tools)
        if action.name == "finish":
            return action.answer
        history.append(tools[action.name](**action.args))
    return "stopped: step budget used"$code$,
 ARRAY['the agent loop', 'tool calling', 'system prompts', 'planning and reflection',
       'memory and context windows', 'guardrails', 'evaluating agents', 'multi-agent handoffs',
       'token budgets', 'human review of actions'],
 ARRAY['ai-agents', 'llm', 'tool-calling', 'prompting', 'evaluation']),
(5, 'RAG',
 'Retrieval-augmented generation grounds model answers in your own documents.',
 'sql',
 $code$SELECT chunk_id, content
FROM doc_chunk
WHERE collection = 'course-notes'
ORDER BY embedding <=> :query_embedding
LIMIT 5;$code$,
 ARRAY['chunking strategies', 'choosing an embedding model', 'vector search with pgvector', 'hybrid search',
       're-ranking', 'prompts with citations', 'retrieval evaluation', 'metadata filters',
       'keeping the index fresh', 'detecting unsupported answers'],
 ARRAY['rag', 'embeddings', 'vector-search', 'chunking', 'pgvector']);

-- dev-user, generated notes 1..146: i -> topic t (1..5), subject s (1..10), angle a (1..3).
-- Every (t, s, a) is distinct, so titles are unique. Tags: i % 4 = 0 -> none, else i % 4 tags of the topic.
INSERT INTO dev_seed_note (owner_id, n, title, body, course_week, created_at, updated_at, tags)
SELECT '00000000-0000-0000-0000-00000000000d',
       g.i,
       format('%s: %s (%s)', tp.name, tp.subjects[g.s], a.angle),
       format($md$# %1$s: %2$s

## Why it matters
%3$s This note looks at **%2$s** from the "%4$s" angle.

## Key points
%5$s

## Example
```%6$s
%7$s
```

## Next steps
1. Re-read this note in a week and fix anything that is no longer true.
2. Try the example in a scratch project.
3. Bring one open question to the study group.

**Confidence:** %8$s/5$md$,
              tp.name, tp.subjects[g.s], tp.intro, a.angle, format(a.points, tp.subjects[g.s]),
              tp.lang, tp.code, 1 + g.i % 5),
       CASE WHEN g.i % 7 = 0 THEN NULL ELSE 1 + (g.i * 3) % 26 END,
       d.created_at,
       LEAST(d.created_at + make_interval(days => (g.i * 13) % 6), d.anchor),
       ARRAY(SELECT tp.tags[1 + (g.s + j) % 5] FROM generate_series(0, g.i % 4 - 1) AS j ORDER BY j)
FROM (SELECT i, 1 + (i - 1) % 5 AS t, 1 + ((i - 1) / 5) % 10 AS s, 1 + (i - 1) / 50 AS a
      FROM generate_series(1, 146) AS i) AS g
JOIN dev_seed_topic AS tp ON tp.t = g.t
JOIN (VALUES
        (1, 'basics', E'- What %s is and which problem it solves.\n- The smallest example that works.\n- Where the official documentation explains it.'),
        (2, 'common pitfalls', E'- The first mistake I made with %s.\n- How the error message looked.\n- A rule of thumb to avoid it next time.'),
        (3, 'cheat sheet', E'- The three commands or APIs I use most for %s.\n- Default values worth remembering.\n- A link to my longer note.')
     ) AS a (a, angle, points) ON a.a = g.a
CROSS JOIN LATERAL (
    SELECT x.anchor,
           x.anchor - make_interval(days => 1 + (g.i * 37) % 89) + make_interval(secs => (g.i * 7919) % 86400)
               AS created_at
    FROM (SELECT date_trunc('day', now() AT TIME ZONE 'UTC') AT TIME ZONE 'UTC' AS anchor) AS x
) AS d;

-- dev-user edge cases 147..150: exactly 10 tags, 200-character Latin and Cyrillic titles,
-- and an untagged note whose updated_at equals note 147 (exercises the id DESC tie-breaker).
INSERT INTO dev_seed_note (owner_id, n, title, body, course_week, created_at, updated_at, tags)
SELECT '00000000-0000-0000-0000-00000000000d', e.n, e.title, e.body, e.course_week,
       x.anchor - e.created_ago, x.anchor - e.updated_ago, e.tags
FROM (SELECT date_trunc('day', now() AT TIME ZONE 'UTC') AT TIME ZONE 'UTC' AS anchor) AS x
CROSS JOIN (VALUES
(147, 'Capstone: a study-notes assistant with Quarkus, PostgreSQL and RAG',
 $md$# Capstone: a study-notes assistant

## Plan
- Store notes in PostgreSQL, one owner per row.
- Expose a REST API with Quarkus.
- Answer questions with RAG over the notes, citing note ids.

## Retrieval query
```sql
SELECT id, title
FROM note
WHERE owner_id = :owner_id
ORDER BY embedding <=> :question_embedding
LIMIT 5;
```

## Done when
1. Every answer cites at least one note.
2. Another user's notes never appear in answers.$md$,
 26::smallint, interval '30 days', interval '3 hours',
 ARRAY['java', 'concurrency', 'quarkus', 'cdi', 'jakarta-rest', 'postgresql', 'indexing', 'ai-agents', 'rag',
       'embeddings']),
(148, 'A deliberately long note title that checks the two hundred character limit of the title field while covering Quarkus configuration profiles, Flyway repeatable migrations and keyset paging in Postgres.',
 $md$# Long title check

## Why
- The title column is `varchar(200)`; this title uses all 200 characters.
- The list view must truncate it without breaking the layout.

## Properties used in dev mode
```properties
%dev.quarkus.flyway.locations=db/migration,db/dev-seed
```$md$,
 1::smallint, interval '60 days 4 hours', interval '59 days',
 ARRAY['quarkus', 'flyway']),
(149, 'Конспект недели: как устроены транзакции и уровни изоляции в PostgreSQL, зачем нужен VACUUM, как читать план запроса EXPLAIN ANALYZE и почему индекс по владельцу и дате обновления ускоряет наши списки',
 $md$# Транзакции и изоляция в PostgreSQL

## Главное
- По умолчанию уровень изоляции — **READ COMMITTED**.
- `REPEATABLE READ` видит снимок данных на начало транзакции.
- Долгие транзакции мешают VACUUM убирать старые версии строк.

## Пример
```sql
BEGIN ISOLATION LEVEL REPEATABLE READ;
SELECT count(*) FROM note WHERE owner_id = :owner_id;
COMMIT;
```

## Вопросы
1. Когда действительно нужен `SERIALIZABLE`?
2. Как найти долгие транзакции в `pg_stat_activity`?$md$,
 12::smallint, interval '45 days 2 hours', interval '20 days',
 ARRAY['postgresql', 'transactions', 'indexing']),
(150, 'Scratch: untagged quick thoughts',
 $md$# Quick thoughts

- Tags are optional; this note has none.
- Week is not set either.

```text
remember: filter by owner in every query
```$md$,
 NULL::smallint, interval '5 days', interval '3 hours',
 ARRAY[]::text[])
) AS e (n, title, body, course_week, created_ago, updated_ago, tags);

-- other-user: 20 notes, 5 tags. java, postgresql and rag repeat dev-user tag names, and notes 1 and 2
-- repeat dev-user titles, to check that lists, filters and search never mix owners (FR-9).
INSERT INTO dev_seed_note (owner_id, n, title, body, course_week, created_at, updated_at, tags)
SELECT '00000000-0000-0000-0000-00000000000e',
       o.n,
       o.title,
       format($md$# %1$s

## Summary
Personal study note, kept short on purpose.

## Checklist
- [x] Read the material
- [ ] Explain it in my own words
- [ ] Try it on a small example

## Snippet
```text
TODO(%2$s): add a worked example
```$md$, o.title, o.n),
       CASE WHEN o.n % 3 = 0 THEN NULL ELSE 1 + (o.n * 5) % 26 END,
       d.created_at,
       LEAST(d.created_at + make_interval(hours => o.n % 4), d.anchor),
       o.tags
FROM (VALUES
    (1,  'Java: records and sealed types (basics)',             ARRAY['java']),
    (2,  'PostgreSQL: B-tree indexes (basics)',                 ARRAY['postgresql']),
    (3,  'Interview prep: explain equals and hashCode',         ARRAY['interview-prep', 'java']),
    (4,  'Interview prep: what VACUUM does',                    ARRAY['interview-prep', 'postgresql']),
    (5,  'Reading list: retrieval papers to skim',              ARRAY[]::text[]),
    (6,  'RAG: why chunk overlap matters',                      ARRAY['rag']),
    (7,  'Java: try-with-resources and suppressed exceptions',  ARRAY['java']),
    (8,  'PostgreSQL: partial indexes for soft deletes',        ARRAY['postgresql']),
    (9,  'Reading list: chapters on database internals',        ARRAY['reading-list', 'postgresql']),
    (10, 'Interview prep: optimistic and pessimistic locking',  ARRAY[]::text[]),
    (11, 'RAG: answer only from retrieved context',             ARRAY['rag', 'reading-list']),
    (12, 'Java: immutable collections in practice',             ARRAY['java']),
    (13, 'PostgreSQL: lock timeouts in migrations',             ARRAY['postgresql']),
    (14, 'Interview prep: cost of HashMap operations',          ARRAY['interview-prep', 'java']),
    (15, 'Reading list: talks about JVM performance',           ARRAY[]::text[]),
    (16, 'RAG: logging the chunks behind every answer',         ARRAY['rag']),
    (17, 'Java: text blocks for SQL and JSON',                  ARRAY['java', 'postgresql']),
    (18, 'PostgreSQL: generated columns',                       ARRAY['postgresql']),
    (19, 'Interview prep: design a link shortener',             ARRAY['interview-prep', 'rag', 'reading-list']),
    (20, 'Scratch: questions for the next study session',       ARRAY[]::text[])
) AS o (n, title, tags)
CROSS JOIN LATERAL (
    SELECT x.anchor,
           x.anchor - make_interval(days => 1 + (o.n * 41) % 89) + make_interval(secs => (o.n * 4813) % 86400)
               AS created_at
    FROM (SELECT date_trunc('day', now() AT TIME ZONE 'UTC') AT TIME ZONE 'UTC' AS anchor) AS x
) AS d;

INSERT INTO tag (id, owner_id, name)
SELECT nextval('tag_seq'), '00000000-0000-0000-0000-00000000000d', u.name
FROM dev_seed_topic AS tp
CROSS JOIN LATERAL unnest(tp.tags) WITH ORDINALITY AS u (name, pos)
ORDER BY tp.t, u.pos;

INSERT INTO tag (id, owner_id, name)
SELECT nextval('tag_seq'), '00000000-0000-0000-0000-00000000000e', u.name
FROM unnest(ARRAY['java', 'postgresql', 'rag', 'interview-prep', 'reading-list']) WITH ORDINALITY AS u (name, pos)
ORDER BY u.pos;

INSERT INTO note (id, owner_id, title, body, course_week, created_at, updated_at)
SELECT nextval('note_seq'), s.owner_id, s.title, s.body, s.course_week, s.created_at, s.updated_at
FROM dev_seed_note AS s
ORDER BY s.owner_id, s.n;

INSERT INTO note_tag (note_id, tag_id)
SELECT n.id, t.id
FROM dev_seed_note AS s
JOIN note AS n ON n.owner_id = s.owner_id AND n.title = s.title
CROSS JOIN LATERAL unnest(s.tags) AS u (name)
JOIN tag AS t ON t.owner_id = s.owner_id AND t.name = u.name;

-- Fails the migration (and rolls it back) if the data breaks the API rules or the intended shape.
DO $check$
DECLARE
    dev   constant uuid := '00000000-0000-0000-0000-00000000000d';
    other constant uuid := '00000000-0000-0000-0000-00000000000e';
BEGIN
    IF (SELECT count(*) FROM note WHERE owner_id = dev) <> 150
            OR (SELECT count(*) FROM note WHERE owner_id = other) <> 20
            OR (SELECT count(*) FROM tag WHERE owner_id = dev) <> 25
            OR (SELECT count(*) FROM tag WHERE owner_id = other) <> 5 THEN
        RAISE EXCEPTION 'dev seed: unexpected note or tag count';
    END IF;
    IF (SELECT count(*) FROM note_tag nt JOIN note n ON n.id = nt.note_id WHERE n.owner_id IN (dev, other))
            <> (SELECT sum(cardinality(tags)) FROM dev_seed_note) THEN
        RAISE EXCEPTION 'dev seed: a note refers to an unknown tag name';
    END IF;
    IF EXISTS (SELECT 1 FROM tag t WHERE t.owner_id IN (dev, other)
               AND (t.name <> lower(t.name) OR NOT EXISTS (SELECT 1 FROM note_tag nt WHERE nt.tag_id = t.id))) THEN
        RAISE EXCEPTION 'dev seed: tag is not lowercase or not used';
    END IF;
    IF (SELECT max(cardinality(tags)) FROM dev_seed_note) <> 10
            OR (SELECT count(*) FROM dev_seed_note WHERE char_length(title) = 200) <> 2 THEN
        RAISE EXCEPTION 'dev seed: edge cases (10 tags, 200-character titles) missing';
    END IF;
    IF EXISTS (SELECT 1 FROM note WHERE owner_id IN (dev, other)
               AND (created_at < now() - interval '90 days' OR updated_at < created_at OR updated_at > now())) THEN
        RAISE EXCEPTION 'dev seed: dates outside the last 90 days';
    END IF;
END
$check$;

DROP TABLE dev_seed_note;
DROP TABLE dev_seed_topic;
