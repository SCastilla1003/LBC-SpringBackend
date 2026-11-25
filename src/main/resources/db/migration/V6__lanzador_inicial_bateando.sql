-- El lanzador inicial se marca aparte: así puede batear como DH (regla Ohtani) además de como P.
ALTER TABLE lineup_spot ADD COLUMN lanzador_inicial BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE lineup_spot SET lanzador_inicial = TRUE WHERE posicion = 'P' AND titular = TRUE;
