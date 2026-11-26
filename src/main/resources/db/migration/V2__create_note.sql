-- Hibernate allocates ids in blocks of 50 for PanacheEntity (ADR 0005).
CREATE SEQUENCE note_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE note (
    id          bigint       PRIMARY KEY,
    owner_id    uuid         NOT NULL REFERENCES account (id),
    title       varchar(200) NOT NULL,                                 -- OQ-11, matches @Size(max = 200)
    body        text         NOT NULL,                                 -- 100 KB limit checked in code (OQ-17)
    course_week smallint     CHECK (course_week BETWEEN 1 AND 26),     -- FR-3
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL
);

-- Every note query filters by owner (FR-9), and the list is sorted updated_at desc, id desc (ADR 0006, FR-6).
-- One index serves both, lets S4 paging read rows in order without a sort, and covers the owner_id foreign key.
CREATE INDEX note_owner_updated_idx ON note (owner_id, updated_at DESC, id DESC);
