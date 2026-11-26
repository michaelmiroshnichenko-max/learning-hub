-- Tags are per owner, lowercase, at most 50 characters (FR-4, OQ-11, ADR 0005).
CREATE SEQUENCE tag_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE tag (
    id       bigint      PRIMARY KEY,
    owner_id uuid        NOT NULL REFERENCES account (id),
    name     varchar(50) NOT NULL,                                     -- matches @Size(max = 50)
    -- Also serves tag lookups by name (filter, find-or-create) and the owner_id foreign key.
    CONSTRAINT tag_owner_name_uk UNIQUE (owner_id, name)
);

-- Join table without an entity (ADR 0005). Deleting a note or a tag removes its links.
CREATE TABLE note_tag (
    note_id bigint NOT NULL REFERENCES note (id) ON DELETE CASCADE,
    tag_id  bigint NOT NULL REFERENCES tag (id) ON DELETE CASCADE,
    PRIMARY KEY (note_id, tag_id)                                      -- tags of a page of notes
);

-- Tag filter (FR-7) and cascade from tag: tag_id -> note_id without touching the table.
CREATE INDEX note_tag_tag_note_idx ON note_tag (tag_id, note_id);
