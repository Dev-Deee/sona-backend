CREATE TABLE shows (
  id                BIGSERIAL PRIMARY KEY,
  uuid              UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
  name              VARCHAR(255) NOT NULL,
  show_date         DATE,
  venue_id          BIGINT REFERENCES venues(id),
  owner_id          BIGINT REFERENCES users(id),
  calculator_states JSONB NOT NULL DEFAULT '{}',
  input_list        JSONB NOT NULL DEFAULT '[]',
  notes             TEXT,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
