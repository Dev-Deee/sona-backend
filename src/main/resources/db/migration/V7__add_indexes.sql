CREATE INDEX idx_venues_owner    ON venues(owner_id);
CREATE INDEX idx_venues_team     ON venues(team_id);
CREATE INDEX idx_shows_venue     ON shows(venue_id);
CREATE INDEX idx_shows_owner     ON shows(owner_id);
CREATE INDEX idx_presets_owner   ON presets(owner_id);
CREATE INDEX idx_presets_tab     ON presets(calculator_tab);
CREATE INDEX idx_share_token     ON share_tokens(token);
CREATE INDEX idx_share_expires   ON share_tokens(expires_at);
