-- "lanza" pasa a ser solo la mano con la que lanza (todos los jugadores tiran con una).
-- Si es pitcher se marca aparte. Los que ya tenían mano de lanzar eran los lanzadores.
ALTER TABLE player ADD COLUMN es_lanzador BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE player SET es_lanzador = TRUE WHERE lanza IS NOT NULL;
