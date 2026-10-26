CREATE TABLE IF NOT EXISTS flagged_messages (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    text        TEXT    NOT NULL,
    score       INTEGER NOT NULL,
    category    TEXT    NOT NULL,
    source_url  TEXT,
    reported_by TEXT    NOT NULL,   -- AUTO (flagged by the system) or USER (reported by a person)
    created_at  TEXT    NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_flag
    ON flagged_messages (text, source_url, reported_by);
