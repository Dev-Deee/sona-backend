CREATE TABLE teams (
  id         BIGSERIAL PRIMARY KEY,
  uuid       UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
  name       VARCHAR(255) NOT NULL,
  created_by BIGINT REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE team_members (
  team_id    BIGINT REFERENCES teams(id) ON DELETE CASCADE,
  user_id    BIGINT REFERENCES users(id) ON DELETE CASCADE,
  role       VARCHAR(50) NOT NULL DEFAULT 'VIEWER',
  joined_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (team_id, user_id)
);
