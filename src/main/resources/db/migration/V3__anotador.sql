-- Bitácora del anotador: cada acción (inicio, lanzamiento, jugada, cambio, fin) con la foto
-- del estado del partido antes de aplicarla. Sirve para deshacer la última acción y como
-- "jugada a jugada" del partido.
CREATE TABLE game_action (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  game_id BIGINT NOT NULL,
  tipo VARCHAR(20) NOT NULL,                    -- INICIO, LANZAMIENTO, JUGADA, CAMBIO, FIN, SUSPENSION, REANUDACION
  descripcion VARCHAR(400) NOT NULL,
  entrada TINYINT UNSIGNED NOT NULL,
  mitad ENUM('TOP','BOTTOM') NOT NULL,
  play_id BIGINT NULL,                          -- filas que creó la acción (se borran al deshacerla)
  pitch_id BIGINT NULL,
  substitution_id BIGINT NULL,
  estado_antes JSON NOT NULL,
  command_id CHAR(36) NOT NULL,
  anotador_id BIGINT NULL,
  creado_en TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_action_command (game_id, command_id),
  INDEX ix_action_game (game_id, id),
  CONSTRAINT fk_action_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE,
  CONSTRAINT fk_action_anotador FOREIGN KEY (anotador_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
