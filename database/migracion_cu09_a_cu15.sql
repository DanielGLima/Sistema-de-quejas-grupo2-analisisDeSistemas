-- Migracion para bases de datos ya creadas (ejecutar una sola vez).
-- Agrega lo necesario para CU-09 a CU-15 sobre el schema.sql original.

ALTER TABLE notificacion ADD COLUMN IF NOT EXISTS reintentos INTEGER NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS reapertura_caso (
    id_reapertura     SERIAL PRIMARY KEY,
    id_caso           INTEGER NOT NULL REFERENCES caso (id_caso),
    motivo            VARCHAR(500) NOT NULL,
    url_evidencia     VARCHAR(500),
    fecha_solicitud   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_reapertura_motivo_min CHECK (char_length(motivo) >= 10)
);
CREATE INDEX IF NOT EXISTS idx_reapertura_caso ON reapertura_caso (id_caso);

CREATE TABLE IF NOT EXISTS reasignacion_caso (
    id_reasignacion        SERIAL PRIMARY KEY,
    id_caso                INTEGER NOT NULL REFERENCES caso (id_caso),
    id_personal_anterior   INTEGER REFERENCES personal (id_personal),
    id_personal_nuevo      INTEGER NOT NULL REFERENCES personal (id_personal),
    id_personal_ejecutor   INTEGER NOT NULL REFERENCES personal (id_personal),
    motivo                 VARCHAR(300) NOT NULL,
    fecha_reasignacion     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_reasignacion_caso ON reasignacion_caso (id_caso);
