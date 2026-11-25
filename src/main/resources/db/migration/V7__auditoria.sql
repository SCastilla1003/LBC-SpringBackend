-- Registro de auditoría legible: qué pasó en palabras y quién lo hizo (aunque la cuenta se borre).
ALTER TABLE audit_log ADD COLUMN descripcion VARCHAR(400) NULL;
ALTER TABLE audit_log ADD COLUMN usuario VARCHAR(100) NULL;
ALTER TABLE audit_log ADD INDEX ix_audit_fecha (fecha);
