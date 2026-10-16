CREATE TABLE users (
  id          BIGSERIAL PRIMARY KEY,
  uuid        UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
  email       VARCHAR(255) NOT NULL UNIQUE,
  password    VARCHAR(255) NOT NULL,
  role        VARCHAR(50)  NOT NULL DEFAULT 'TOURING',
  preferences JSONB        NOT NULL DEFAULT '{}',
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
