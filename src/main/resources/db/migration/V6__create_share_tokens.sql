CREATE TABLE share_tokens (
  id           BIGSERIAL PRIMARY KEY,
  token        VARCHAR(20) NOT NULL UNIQUE,
  payload      JSONB NOT NULL,
  created_by   BIGINT REFERENCES users(id),
  expires_at   TIMESTAMPTZ NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
