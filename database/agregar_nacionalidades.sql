-- Catalogo de nacionalidades (CU-01 y CU-03).
--
-- El backend valida que la nacionalidad de un cliente sea una de las activas de esta tabla.
-- Es seguro ejecutarlo varias veces: solo crea lo que falte. No modifica a los usuarios existentes.
--
-- IMPORTANTE: ejecutarlo ANTES de iniciar el backend con esta version (hibernate valida que la tabla exista).
-- Para agregar otra nacionalidad: INSERT INTO nacionalidad (nombre) VALUES ('Peruana');
-- Para retirar una de la lista sin borrarla: UPDATE nacionalidad SET activo = FALSE WHERE nombre = 'Otra';

CREATE TABLE IF NOT EXISTS nacionalidad (
    id_nacionalidad SERIAL PRIMARY KEY,
    nombre          VARCHAR(50) NOT NULL UNIQUE,
    activo          BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO nacionalidad (nombre) VALUES
    ('Guatemalteca'),
    ('Mexicana'),
    ('Salvadoreña'),
    ('Hondureña'),
    ('Nicaragüense'),
    ('Costarricense'),
    ('Panameña'),
    ('Colombiana'),
    ('Estadounidense'),
    ('Otra')
ON CONFLICT (nombre) DO NOTHING;

SELECT id_nacionalidad, nombre, activo FROM nacionalidad ORDER BY id_nacionalidad;
