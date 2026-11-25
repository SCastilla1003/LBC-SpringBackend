-- Muerte súbita: bases donde salen corredores automáticos en las entradas extra
-- (por ejemplo "2" = uno en segunda, "1,2" = uno en primera y otro en segunda, "" = ninguno).
ALTER TABLE season ADD COLUMN bases_muerte_subita VARCHAR(10) NOT NULL DEFAULT '2';

-- Por qué se detuvo un partido (RETRASADO, POSPUESTO, CANCELADO) y, si terminó con un walk off,
-- cuál fue (WALK_OFF, WALK_OFF_HOME_RUN, WALK_OFF_GRAND_SLAM).
ALTER TABLE game ADD COLUMN motivo_detencion VARCHAR(20) NULL;
ALTER TABLE game ADD COLUMN final_especial VARCHAR(30) NULL;
