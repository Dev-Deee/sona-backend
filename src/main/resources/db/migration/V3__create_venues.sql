CREATE TABLE venues (
  id          BIGSERIAL PRIMARY KEY,
  uuid        UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
  name        VARCHAR(255) NOT NULL,
  owner_id    BIGINT REFERENCES users(id),
  team_id     BIGINT REFERENCES teams(id),
  dimensions  JSONB,        -- { length, width, height, unit }
  surfaces    JSONB,        -- absorption coefficients
  notes       TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
