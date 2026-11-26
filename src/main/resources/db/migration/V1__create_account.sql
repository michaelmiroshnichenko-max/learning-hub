-- Minimal account so that note.owner_id can reference it (architecture section 2).
-- Sign-in columns are added in S5 by a new migration. login rules: OQ-16 (case-sensitive for now).
CREATE TABLE account (
    id         uuid        PRIMARY KEY,
    login      text        NOT NULL UNIQUE,
    created_at timestamptz NOT NULL DEFAULT now()
);
