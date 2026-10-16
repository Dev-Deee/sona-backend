-- Share tokens can be one-time (consumed on first read) or multi-use.
ALTER TABLE share_tokens ADD COLUMN single_use BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE share_tokens ADD COLUMN used_at TIMESTAMPTZ;
