-- Refresh tokens are persisted (as SHA-256 hashes, never the raw value) so they can be revoked
-- on logout and rotated on every refresh.
CREATE TABLE refresh_tokens (
  id          BIGSERIAL PRIMARY KEY,
  token_hash  VARCHAR(64) NOT NULL UNIQUE,
  user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  expires_at  TIMESTAMPTZ NOT NULL,
  revoked     BOOLEAN NOT NULL DEFAULT FALSE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
