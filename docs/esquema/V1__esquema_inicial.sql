-- =====================================================================
-- Esquema inicial LBC · MySQL 8.0+ (InnoDB, utf8mb4)
-- PROPUESTA PARA REVISIÓN: todavía no se aplica al backend.
-- Al aprobarla se copia a src/main/resources/db/migration/ (Flyway).
-- Base de datos vacía: no hay migración desde MongoDB.
-- =====================================================================

-- ---------- Usuarios y catálogo ----------

CREATE TABLE user_account (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL,
  email VARCHAR(120) NULL,
  password_hash VARCHAR(100) NULL,              -- NULL en cuentas de Google
  rol ENUM('ADMIN','ANOTADOR','FAN') NOT NULL DEFAULT 'FAN',
  proveedor ENUM('LOCAL','GOOGLE') NOT NULL DEFAULT 'LOCAL',
  provider_id VARCHAR(120) NULL,
  activa BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_username (username),
  UNIQUE KEY uk_user_email (email),
  UNIQUE KEY uk_user_proveedor (proveedor, provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE team (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  abreviatura VARCHAR(5) NOT NULL,
  ciudad VARCHAR(100) NULL,
  color VARCHAR(20) NULL,                       -- clave de la paleta de equipos
  escudo_url VARCHAR(500) NULL,                 -- URL en el bucket de Railway
  UNIQUE KEY uk_team_nombre (nombre),
  UNIQUE KEY uk_team_abreviatura (abreviatura)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE season (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  anio SMALLINT UNSIGNED NOT NULL,
  estado ENUM('DRAFT','ACTIVE','FINISHED') NOT NULL DEFAULT 'DRAFT',
  fecha_inicio DATE NULL,
  fecha_fin DATE NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stadium (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  ubicacion VARCHAR(200) NULL,
  UNIQUE KEY uk_stadium_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE player (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombres VARCHAR(100) NOT NULL,
  apellidos VARCHAR(100) NOT NULL,
  fecha_nacimiento DATE NULL,
  batea ENUM('D','Z','A') NULL,                 -- derecho, zurdo, ambidiestro
  lanza ENUM('D','Z') NULL,
  foto_url VARCHAR(500) NULL                    -- URL en el bucket de Railway
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Jugador en una temporada. team_id NULL = agente libre.
CREATE TABLE roster_entry (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  season_id BIGINT NOT NULL,
  team_id BIGINT NULL,
  player_id BIGINT NOT NULL,
  numero SMALLINT UNSIGNED NULL,
  posicion VARCHAR(10) NULL,
  UNIQUE KEY uk_roster_season_player (season_id, player_id),
  CONSTRAINT fk_roster_season FOREIGN KEY (season_id) REFERENCES season(id),
  CONSTRAINT fk_roster_team FOREIGN KEY (team_id) REFERENCES team(id),
  CONSTRAINT fk_roster_player FOREIGN KEY (player_id) REFERENCES player(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- Partido y alineaciones ----------

CREATE TABLE game (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  season_id BIGINT NOT NULL,
  home_team_id BIGINT NOT NULL,
  away_team_id BIGINT NOT NULL,
  fecha_hora DATETIME NOT NULL,
  stadium_id BIGINT NULL,
  estado ENUM('SCHEDULED','LINEUP_PUBLISHED','LIVE','FINISHED','SUSPENDED') NOT NULL DEFAULT 'SCHEDULED',
  entradas_programadas TINYINT UNSIGNED NOT NULL DEFAULT 9,

  -- Marcador y totales: se recalculan con cada jugada
  carreras_local SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  carreras_visita SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  hits_local SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  hits_visita SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  errores_local SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  errores_visita SMALLINT UNSIGNED NOT NULL DEFAULT 0,

  -- Situación en vivo
  entrada_actual TINYINT UNSIGNED NOT NULL DEFAULT 1,
  mitad ENUM('TOP','BOTTOM') NOT NULL DEFAULT 'TOP',   -- TOP = batea el visitante, BOTTOM = batea el local
  outs TINYINT UNSIGNED NOT NULL DEFAULT 0,
  bolas TINYINT UNSIGNED NOT NULL DEFAULT 0,
  strikes TINYINT UNSIGNED NOT NULL DEFAULT 0,
  bateador_id BIGINT NULL,
  lanzador_id BIGINT NULL,                      -- lanzador actual del equipo que defiende
  corredor_1b_id BIGINT NULL,
  corredor_2b_id BIGINT NULL,
  corredor_3b_id BIGINT NULL,
  proximo_orden_local TINYINT UNSIGNED NOT NULL DEFAULT 1,    -- siguiente turno del orden (1..9)
  proximo_orden_visita TINYINT UNSIGNED NOT NULL DEFAULT 1,

  anotador_id BIGINT NULL,
  version INT UNSIGNED NOT NULL DEFAULT 0,      -- bloqueo optimista

  CONSTRAINT ck_game_equipos CHECK (home_team_id <> away_team_id),
  CONSTRAINT ck_game_outs CHECK (outs <= 2),
  CONSTRAINT ck_game_conteo CHECK (bolas <= 3 AND strikes <= 2),
  CONSTRAINT ck_game_orden CHECK (proximo_orden_local BETWEEN 1 AND 9 AND proximo_orden_visita BETWEEN 1 AND 9),
  CONSTRAINT fk_game_season FOREIGN KEY (season_id) REFERENCES season(id),
  CONSTRAINT fk_game_home FOREIGN KEY (home_team_id) REFERENCES team(id),
  CONSTRAINT fk_game_away FOREIGN KEY (away_team_id) REFERENCES team(id),
  CONSTRAINT fk_game_stadium FOREIGN KEY (stadium_id) REFERENCES stadium(id),
  CONSTRAINT fk_game_bateador FOREIGN KEY (bateador_id) REFERENCES player(id),
  CONSTRAINT fk_game_lanzador FOREIGN KEY (lanzador_id) REFERENCES player(id),
  CONSTRAINT fk_game_c1 FOREIGN KEY (corredor_1b_id) REFERENCES player(id),
  CONSTRAINT fk_game_c2 FOREIGN KEY (corredor_2b_id) REFERENCES player(id),
  CONSTRAINT fk_game_c3 FOREIGN KEY (corredor_3b_id) REFERENCES player(id),
  CONSTRAINT fk_game_anotador FOREIGN KEY (anotador_id) REFERENCES user_account(id),
  INDEX ix_game_season_fecha (season_id, fecha_hora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Alineación inicial. orden_bateo 1..9 = titulares al bate; NULL = banca.
CREATE TABLE lineup_spot (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  game_id BIGINT NOT NULL,
  team_id BIGINT NOT NULL,
  player_id BIGINT NOT NULL,
  orden_bateo TINYINT UNSIGNED NULL,
  posicion VARCHAR(10) NULL,                    -- P, C, 1B, 2B, 3B, SS, LF, CF, RF, DH; NULL = banca
  titular BOOLEAN NOT NULL DEFAULT FALSE,
  UNIQUE KEY uk_lineup_game_player (game_id, player_id),
  UNIQUE KEY uk_lineup_game_orden (game_id, team_id, orden_bateo),
  CONSTRAINT ck_lineup_orden CHECK (orden_bateo IS NULL OR orden_bateo BETWEEN 1 AND 9),
  CONSTRAINT fk_lineup_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE,
  CONSTRAINT fk_lineup_team FOREIGN KEY (team_id) REFERENCES team(id),
  CONSTRAINT fk_lineup_player FOREIGN KEY (player_id) REFERENCES player(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Cambios durante el partido: pinch hitter, pinch runner, defensivo, cambio de lanzador.
CREATE TABLE game_substitution (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  game_id BIGINT NOT NULL,
  team_id BIGINT NOT NULL,
  entrada TINYINT UNSIGNED NOT NULL,
  mitad ENUM('TOP','BOTTOM') NOT NULL,
  jugador_sale_id BIGINT NOT NULL,
  jugador_entra_id BIGINT NOT NULL,
  tipo ENUM('PINCH_HITTER','PINCH_RUNNER','DEFENSIVE','PITCHER') NOT NULL,
  orden_bateo TINYINT UNSIGNED NULL,
  posicion VARCHAR(10) NULL,
  command_id CHAR(36) NOT NULL,
  ocurrido_en DATETIME(3) NOT NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sub_command (game_id, command_id),
  CONSTRAINT fk_sub_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE,
  CONSTRAINT fk_sub_team FOREIGN KEY (team_id) REFERENCES team(id),
  CONSTRAINT fk_sub_sale FOREIGN KEY (jugador_sale_id) REFERENCES player(id),
  CONSTRAINT fk_sub_entra FOREIGN KEY (jugador_entra_id) REFERENCES player(id),
  INDEX ix_sub_game (game_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- Jugadas (fuente de verdad de las estadísticas) ----------

-- Una fila por jugada cerrada (turno al bate o evento de corredores).
CREATE TABLE game_play (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  game_id BIGINT NOT NULL,
  secuencia INT UNSIGNED NOT NULL,
  command_id CHAR(36) NOT NULL,                 -- UUID de la app: evita duplicados si se reintenta
  ocurrido_en DATETIME(3) NOT NULL,             -- hora en que se anotó en el campo (puede llegar tarde por falta de red)
  entrada TINYINT UNSIGNED NOT NULL,
  mitad ENUM('TOP','BOTTOM') NOT NULL,
  bateador_id BIGINT NOT NULL,
  lanzador_id BIGINT NOT NULL,
  resultado ENUM(
    'SINGLE','DOUBLE','TRIPLE','HOME_RUN',
    'WALK','HIT_BY_PITCH','STRIKEOUT',
    'GROUND_OUT','FLY_OUT','LINE_OUT','POP_OUT',
    'FIELDERS_CHOICE','REACHED_ON_ERROR',
    'SACRIFICE_BUNT','SACRIFICE_FLY','DOUBLE_PLAY',
    'STOLEN_BASE','CAUGHT_STEALING','WILD_PITCH','PASSED_BALL','BALK'
  ) NOT NULL,
  jugador_defensa_id BIGINT NULL,               -- quien cometió el error o hizo la out
  carreras TINYINT UNSIGNED NOT NULL DEFAULT 0,
  outs_registrados TINYINT UNSIGNED NOT NULL DEFAULT 0,
  estado_antes JSON NOT NULL,                   -- foto del estado previo; sirve para deshacer
  anotador_id BIGINT NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_play_command (game_id, command_id),
  UNIQUE KEY uk_play_secuencia (game_id, secuencia),
  CONSTRAINT fk_play_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE,
  CONSTRAINT fk_play_bateador FOREIGN KEY (bateador_id) REFERENCES player(id),
  CONSTRAINT fk_play_lanzador FOREIGN KEY (lanzador_id) REFERENCES player(id),
  CONSTRAINT fk_play_defensa FOREIGN KEY (jugador_defensa_id) REFERENCES player(id),
  CONSTRAINT fk_play_anotador FOREIGN KEY (anotador_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Movimiento de cada corredor (y del bateador) dentro de una jugada.
CREATE TABLE game_play_movement (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  play_id BIGINT NOT NULL,
  jugador_id BIGINT NOT NULL,
  base_desde TINYINT UNSIGNED NOT NULL,         -- 0 = bateador, 1..3 = base
  base_hasta TINYINT UNSIGNED NULL,             -- 1..3 = base, 4 = anotó (home); NULL si es out
  es_out BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT ck_mov_desde CHECK (base_desde <= 3),
  CONSTRAINT ck_mov_hasta CHECK (base_hasta IS NULL OR base_hasta BETWEEN 1 AND 4),
  CONSTRAINT ck_mov_out CHECK ((es_out AND base_hasta IS NULL) OR (NOT es_out AND base_hasta IS NOT NULL)),
  CONSTRAINT fk_mov_play FOREIGN KEY (play_id) REFERENCES game_play(id) ON DELETE CASCADE,
  CONSTRAINT fk_mov_jugador FOREIGN KEY (jugador_id) REFERENCES player(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Cada lanzamiento (bola, strike, foul, en juego). Da el conteo de lanzamientos.
CREATE TABLE game_pitch (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  game_id BIGINT NOT NULL,
  play_id BIGINT NULL,                          -- se asigna cuando la jugada se cierra
  entrada TINYINT UNSIGNED NOT NULL,
  mitad ENUM('TOP','BOTTOM') NOT NULL,
  lanzador_id BIGINT NOT NULL,
  bateador_id BIGINT NOT NULL,
  tipo ENUM('BALL','CALLED_STRIKE','SWINGING_STRIKE','FOUL','IN_PLAY') NOT NULL,
  command_id CHAR(36) NOT NULL,
  ocurrido_en DATETIME(3) NOT NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_pitch_command (game_id, command_id),
  INDEX ix_pitch_game (game_id),
  CONSTRAINT fk_pitch_game FOREIGN KEY (game_id) REFERENCES game(id) ON DELETE CASCADE,
  CONSTRAINT fk_pitch_play FOREIGN KEY (play_id) REFERENCES game_play(id) ON DELETE CASCADE,
  CONSTRAINT fk_pitch_lanzador FOREIGN KEY (lanzador_id) REFERENCES player(id),
  CONSTRAINT fk_pitch_bateador FOREIGN KEY (bateador_id) REFERENCES player(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- Estadísticas derivadas (se recalculan desde las jugadas) ----------

CREATE TABLE player_stat (
  season_id BIGINT NOT NULL,
  player_id BIGINT NOT NULL,
  -- Bateo
  juegos INT UNSIGNED NOT NULL DEFAULT 0,
  pa INT UNSIGNED NOT NULL DEFAULT 0,           -- apariciones al plato
  ab INT UNSIGNED NOT NULL DEFAULT 0,           -- turnos oficiales al bate
  h INT UNSIGNED NOT NULL DEFAULT 0,
  dobles INT UNSIGNED NOT NULL DEFAULT 0,
  triples INT UNSIGNED NOT NULL DEFAULT 0,
  hr INT UNSIGNED NOT NULL DEFAULT 0,
  rbi INT UNSIGNED NOT NULL DEFAULT 0,
  carreras INT UNSIGNED NOT NULL DEFAULT 0,
  bb INT UNSIGNED NOT NULL DEFAULT 0,
  hbp INT UNSIGNED NOT NULL DEFAULT 0,
  so INT UNSIGNED NOT NULL DEFAULT 0,
  robos INT UNSIGNED NOT NULL DEFAULT 0,
  -- Lanzador
  juegos_lanzados INT UNSIGNED NOT NULL DEFAULT 0,
  juegos_iniciados INT UNSIGNED NOT NULL DEFAULT 0,
  outs_lanzados INT UNSIGNED NOT NULL DEFAULT 0, -- IP = outs / 3
  h_permitidos INT UNSIGNED NOT NULL DEFAULT 0,
  carreras_permitidas INT UNSIGNED NOT NULL DEFAULT 0,
  carreras_limpias INT UNSIGNED NOT NULL DEFAULT 0,
  bb_permitidos INT UNSIGNED NOT NULL DEFAULT 0,
  so_lanzados INT UNSIGNED NOT NULL DEFAULT 0,
  lanzamientos INT UNSIGNED NOT NULL DEFAULT 0,
  victorias INT UNSIGNED NOT NULL DEFAULT 0,
  derrotas INT UNSIGNED NOT NULL DEFAULT 0,
  -- Defensa
  errores INT UNSIGNED NOT NULL DEFAULT 0,
  actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (season_id, player_id),
  CONSTRAINT fk_pstat_season FOREIGN KEY (season_id) REFERENCES season(id),
  CONSTRAINT fk_pstat_player FOREIGN KEY (player_id) REFERENCES player(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE standing (
  season_id BIGINT NOT NULL,
  team_id BIGINT NOT NULL,
  juegos INT UNSIGNED NOT NULL DEFAULT 0,
  ganados INT UNSIGNED NOT NULL DEFAULT 0,
  perdidos INT UNSIGNED NOT NULL DEFAULT 0,
  pct DECIMAL(4,3) NOT NULL DEFAULT 0.000,
  carreras_favor INT UNSIGNED NOT NULL DEFAULT 0,
  carreras_contra INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (season_id, team_id),
  CONSTRAINT fk_standing_season FOREIGN KEY (season_id) REFERENCES season(id),
  CONSTRAINT fk_standing_team FOREIGN KEY (team_id) REFERENCES team(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- Usuarios: favoritos, dispositivos y auditoría ----------

CREATE TABLE favorite (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  account_id BIGINT NOT NULL,
  target_type ENUM('TEAM','PLAYER','GAME') NOT NULL,
  target_id BIGINT NOT NULL,                    -- sin FK: apunta a tablas distintas
  creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_favorite (account_id, target_type, target_id),
  CONSTRAINT fk_favorite_account FOREIGN KEY (account_id) REFERENCES user_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE device_token (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  account_id BIGINT NOT NULL,
  token_push VARCHAR(255) NOT NULL,
  plataforma ENUM('ANDROID','IOS','WEB') NOT NULL,
  UNIQUE KEY uk_device_token (token_push),
  CONSTRAINT fk_device_account FOREIGN KEY (account_id) REFERENCES user_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  admin_id BIGINT NULL,
  accion VARCHAR(50) NOT NULL,
  entidad VARCHAR(50) NOT NULL,
  entidad_id BIGINT NULL,
  antes JSON NULL,
  despues JSON NULL,
  fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_audit_admin FOREIGN KEY (admin_id) REFERENCES user_account(id) ON DELETE SET NULL,
  INDEX ix_audit_entidad (entidad, entidad_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
