-- DELETE /api/users/me removes everything the user owns. Team-scoped rows survive the team
-- itself being deleted (they fall back to being owned only by their owner).
ALTER TABLE teams DROP CONSTRAINT teams_created_by_fkey,
  ADD CONSTRAINT teams_created_by_fkey FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE venues DROP CONSTRAINT venues_owner_id_fkey,
  ADD CONSTRAINT venues_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE venues DROP CONSTRAINT venues_team_id_fkey,
  ADD CONSTRAINT venues_team_id_fkey FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL;

ALTER TABLE shows DROP CONSTRAINT shows_owner_id_fkey,
  ADD CONSTRAINT shows_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE shows DROP CONSTRAINT shows_venue_id_fkey,
  ADD CONSTRAINT shows_venue_id_fkey FOREIGN KEY (venue_id) REFERENCES venues(id) ON DELETE CASCADE;

ALTER TABLE presets DROP CONSTRAINT presets_owner_id_fkey,
  ADD CONSTRAINT presets_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE presets DROP CONSTRAINT presets_team_id_fkey,
  ADD CONSTRAINT presets_team_id_fkey FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL;

ALTER TABLE share_tokens DROP CONSTRAINT share_tokens_created_by_fkey,
  ADD CONSTRAINT share_tokens_created_by_fkey FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE;
