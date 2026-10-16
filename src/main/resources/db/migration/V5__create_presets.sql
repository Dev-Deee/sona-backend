CREATE TABLE presets (
  id             BIGSERIAL PRIMARY KEY,
  uuid           UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
  name           VARCHAR(255) NOT NULL,
  calculator_tab VARCHAR(100) NOT NULL,
  inputs         JSONB NOT NULL,
  owner_id       BIGINT REFERENCES users(id),
  team_id        BIGINT REFERENCES teams(id),
  is_public      BOOLEAN NOT NULL DEFAULT FALSE,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
