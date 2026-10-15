-- Corrige el catalogo de estados de los casos para que coincida con el flujo del sistema.
--
-- Sintoma: al gestionar un caso aparece "El estado indicado no existe en el catalogo".
-- Causa: el catalogo estado_caso de la base tiene nombres antiguos (por ejemplo "Pendiente"
-- o "En proceso") y el sistema busca "Nuevo", "En espera", "En Proceso", "Resuelto",
-- "Cerrado", "Cancelado por el usuario" y "Reapertura solicitada".
--
-- Es seguro ejecutarlo varias veces: renombra conservando el id (los casos mantienen su
-- estado) y solo crea lo que falte.

-- 1) Renombrar nombres antiguos o con otra capitalizacion.
DO $$
DECLARE
    par TEXT[];
BEGIN
    FOREACH par SLICE 1 IN ARRAY ARRAY[
        ['pendiente', 'Nuevo'],
        ['nuevo', 'Nuevo'],
        ['en espera', 'En espera'],
        ['en proceso', 'En Proceso'],
        ['resuelto', 'Resuelto'],
        ['cerrado', 'Cerrado'],
        ['cancelado', 'Cancelado por el usuario'],
        ['cancelado por el usuario', 'Cancelado por el usuario'],
        ['reapertura solicitada', 'Reapertura solicitada']
    ]
    LOOP
        UPDATE estado_caso
           SET nombre = par[2]
         WHERE lower(trim(nombre)) = par[1]
           AND nombre <> par[2]
           AND NOT EXISTS (SELECT 1 FROM estado_caso e WHERE e.nombre = par[2]);
    END LOOP;
END $$;

-- 2) Crear los estados que todavia falten.
INSERT INTO estado_caso (nombre, orden) VALUES
    ('Nuevo', 1),
    ('En espera', 2),
    ('En Proceso', 3),
    ('Resuelto', 4),
    ('Cerrado', 5),
    ('Cancelado por el usuario', 6),
    ('Reapertura solicitada', 7)
ON CONFLICT (nombre) DO NOTHING;

-- 3) Dejar activos los estados del flujo.
UPDATE estado_caso
   SET activo = TRUE
 WHERE nombre IN ('Nuevo', 'En espera', 'En Proceso', 'Resuelto', 'Cerrado',
                  'Cancelado por el usuario', 'Reapertura solicitada');

-- 4) Resultado: deben aparecer los 7 estados (si queda alguno antiguo, como "Rechazado",
--    no afecta al flujo).
SELECT id_estado, nombre, orden, activo FROM estado_caso ORDER BY orden, id_estado;
