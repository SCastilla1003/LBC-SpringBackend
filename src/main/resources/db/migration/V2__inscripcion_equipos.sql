-- Equipos inscritos en una temporada, aunque todavía no tengan jugadores.
-- Antes un equipo solo "estaba" en la temporada si tenía jugadores en roster_entry.
CREATE TABLE season_team (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  season_id BIGINT NOT NULL,
  team_id BIGINT NOT NULL,
  UNIQUE KEY uk_season_team (season_id, team_id),
  CONSTRAINT fk_season_team_season FOREIGN KEY (season_id) REFERENCES season(id) ON DELETE CASCADE,
  CONSTRAINT fk_season_team_team FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Los equipos que ya tenían jugadores en una temporada quedan inscritos en ella.
INSERT INTO season_team (season_id, team_id)
SELECT DISTINCT season_id, team_id FROM roster_entry WHERE team_id IS NOT NULL;
