-- =========================================================
-- Sistema de Quejas (QRDS) - Restaurante "Las Delicias"
-- Script completo de base de datos (Postgres)
--
-- Basado en:
--   - Modelo de datos entregado por el usuario (tablas y relaciones)
--   - Casos_de_uso_corregidos.docx (version corregida, CU-00 a CU-15)
--     -> tamanos de campo, formatos y valores de catalogo exactos
--
-- Pensado para correr sobre una base de datos recien creada
-- (DROP DATABASE + CREATE DATABASE). Usa IF NOT EXISTS / ON CONFLICT
-- para que tambien sea seguro volver a ejecutarlo, salvo en usuario
-- y personal, que fallan si ya existen (para detectar reejecuciones
-- accidentales sobre las tablas centrales).
-- =========================================================


-- =========================================================
-- CATALOGOS
-- =========================================================

-- catalogo_rol (CU-13)
CREATE TABLE IF NOT EXISTS rol (
    id_rol       SERIAL PRIMARY KEY,
    nombre       VARCHAR(50) NOT NULL UNIQUE,
    descripcion  VARCHAR(200),
    activo       BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO rol (nombre, descripcion) VALUES
    ('Administrador General', 'Administra usuarios internos, catalogos y toda sucursal'),
    ('Gerente', 'Gestiona casos y personal de su sucursal'),
    ('Operador', 'Atiende y responde casos asignados')
ON CONFLICT (nombre) DO NOTHING;

-- catalogo_sucursal (CU-05, CU-06, CU-12, CU-13) - catalogo dinamico
CREATE TABLE IF NOT EXISTS sucursal (
    id_sucursal  SERIAL PRIMARY KEY,
    nombre       VARCHAR(50) NOT NULL,
    direccion    VARCHAR(150) NOT NULL,
    telefono     VARCHAR(20),
    activo       BOOLEAN NOT NULL DEFAULT TRUE
);

-- catalogo_tipo_caso (CU-05, CU-12): Queja/Reclamo/Denuncia/Sugerencia
CREATE TABLE IF NOT EXISTS tipo_caso (
    id_tipo_caso  SERIAL PRIMARY KEY,
    codigo        VARCHAR(3) NOT NULL UNIQUE,
    nombre        VARCHAR(20) NOT NULL UNIQUE,
    activo        BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO tipo_caso (codigo, nombre) VALUES
    ('QUE', 'Queja'),
    ('REC', 'Reclamo'),
    ('DEN', 'Denuncia'),
    ('SUG', 'Sugerencia')
ON CONFLICT (codigo) DO NOTHING;

-- catalogo_categoria / catalogo_categoria_servicio (CU-05, CU-12)
-- Aplica principalmente a Quejas y Reclamos; puede quedar nula en Denuncia/Sugerencia.
CREATE TABLE IF NOT EXISTS categoria_caso (
    id_categoria  SERIAL PRIMARY KEY,
    codigo        VARCHAR(3) NOT NULL UNIQUE,
    nombre        VARCHAR(30) NOT NULL UNIQUE,
    activo        BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO categoria_caso (codigo, nombre) VALUES
    ('SER', 'Servicio al cliente'),
    ('COM', 'Comida'),
    ('INS', 'Instalaciones'),
    ('LIM', 'Limpieza'),
    ('TIE', 'Tiempo de espera'),
    ('FAC', 'Facturacion/Cobro'),
    ('OTR', 'Otro')
ON CONFLICT (codigo) DO NOTHING;

-- catalogo_estado / catalogo_estado_caso (CU-10, CU-12)
-- "orden" controla la secuencia valida del flujo (CU-10, FA01).
CREATE TABLE IF NOT EXISTS estado_caso (
    id_estado  SERIAL PRIMARY KEY,
    nombre     VARCHAR(25) NOT NULL UNIQUE,
    orden      INTEGER NOT NULL,
    activo     BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO estado_caso (nombre, orden) VALUES
    ('Nuevo', 1),
    ('En espera', 2),
    ('En Proceso', 3),
    ('Resuelto', 4),
    ('Cerrado', 5),
    ('Cancelado por el usuario', 6),
    ('Reapertura solicitada', 7)
ON CONFLICT (nombre) DO NOTHING;


-- =========================================================
-- USUARIOS
-- =========================================================
--
-- usuario (clientes del restaurante: CU-01 Registrarse, CU-00 login, CU-03 Actualizar Perfil)
CREATE TABLE usuario (
    id_usuario        SERIAL PRIMARY KEY,
    nombre_completo   VARCHAR(100) NOT NULL,
    fecha_nacimiento  DATE NOT NULL,
    nacionalidad      VARCHAR(80) NOT NULL,
    correo            VARCHAR(100) NOT NULL UNIQUE,
    codigo_area       VARCHAR(4) NOT NULL,
    telefono          VARCHAR(8) NOT NULL,
    direccion         VARCHAR(150) NOT NULL,
    contrasena_hash   VARCHAR(255) NOT NULL,
    fecha_registro    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activo            BOOLEAN NOT NULL DEFAULT TRUE
);

-- personal (personal administrativo/interno: CU-10, CU-11, CU-12, CU-13)
CREATE TABLE personal (
    id_personal      SERIAL PRIMARY KEY,
    nombre_completo  VARCHAR(100) NOT NULL,
    correo           VARCHAR(100) NOT NULL UNIQUE,
    contrasena_hash  VARCHAR(255) NOT NULL,
    id_rol           INTEGER NOT NULL REFERENCES rol (id_rol),
    id_sucursal      INTEGER REFERENCES sucursal (id_sucursal),
    fecha_creacion   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activo           BOOLEAN NOT NULL DEFAULT TRUE
);

-- recuperacion_contrasena (CU-02): codigo corto de 6 caracteres con expiracion
CREATE TABLE IF NOT EXISTS recuperacion_contrasena (
    id_recuperacion   SERIAL PRIMARY KEY,
    id_usuario        INTEGER NOT NULL REFERENCES usuario (id_usuario),
    codigo_token      VARCHAR(6) NOT NULL,
    fecha_creacion    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion  TIMESTAMP NOT NULL,
    usado             BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_recuperacion_contrasena_usuario ON recuperacion_contrasena (id_usuario);


-- =========================================================
-- CASOS
-- =========================================================
--
-- caso (CU-05 Registrar Nuevo Caso)
CREATE TABLE IF NOT EXISTS caso (
    id_caso                     SERIAL PRIMARY KEY,
    identificador_visible       VARCHAR(20) NOT NULL UNIQUE,
    id_usuario                  INTEGER NOT NULL REFERENCES usuario (id_usuario),
    id_sucursal                 INTEGER NOT NULL REFERENCES sucursal (id_sucursal),
    id_tipo_caso                INTEGER NOT NULL REFERENCES tipo_caso (id_tipo_caso),
    id_categoria                INTEGER REFERENCES categoria_caso (id_categoria),
    id_estado                   INTEGER NOT NULL REFERENCES estado_caso (id_estado),
    descripcion                 VARCHAR(1000) NOT NULL,
    numero_factura              VARCHAR(20),
    nombre_empleado_involucrado VARCHAR(100),
    es_anonimo                  BOOLEAN NOT NULL DEFAULT FALSE,
    id_personal_asignado        INTEGER REFERENCES personal (id_personal),
    fecha_creacion               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_caso_descripcion_min CHECK (char_length(descripcion) >= 10)
);

CREATE INDEX IF NOT EXISTS idx_caso_usuario ON caso (id_usuario);
CREATE INDEX IF NOT EXISTS idx_caso_sucursal ON caso (id_sucursal);
CREATE INDEX IF NOT EXISTS idx_caso_estado ON caso (id_estado);
CREATE INDEX IF NOT EXISTS idx_caso_asignado ON caso (id_personal_asignado);

-- evidencia_caso (CU-05): fotos/PDF, maximo 2 MB por archivo
CREATE TABLE IF NOT EXISTS evidencia_caso (
    id_evidencia   SERIAL PRIMARY KEY,
    id_caso        INTEGER NOT NULL REFERENCES caso (id_caso),
    url_archivo    VARCHAR(500) NOT NULL,
    tipo_archivo   VARCHAR(10) NOT NULL,
    tamano_bytes   INTEGER NOT NULL,
    fecha_carga    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_evidencia_tamano_max CHECK (tamano_bytes <= 2097152)
);

CREATE INDEX IF NOT EXISTS idx_evidencia_caso ON evidencia_caso (id_caso);

-- respuesta_caso (CU-11 Responder Caso)
CREATE TABLE IF NOT EXISTS respuesta_caso (
    id_respuesta          SERIAL PRIMARY KEY,
    id_caso               INTEGER NOT NULL REFERENCES caso (id_caso),
    id_personal           INTEGER NOT NULL REFERENCES personal (id_personal),
    titulo                VARCHAR(100) NOT NULL,
    contenido             VARCHAR(1000) NOT NULL,
    acciones_seguimiento  VARCHAR(300),
    estado_aprobacion     VARCHAR(30) NOT NULL DEFAULT 'Aprobada',
    fecha_respuesta       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_respuesta_contenido_min CHECK (char_length(contenido) >= 20)
);

CREATE INDEX IF NOT EXISTS idx_respuesta_caso ON respuesta_caso (id_caso);

-- evaluacion_caso (CU-08 Evaluar Atencion del Caso) - un caso, una evaluacion
CREATE TABLE IF NOT EXISTS evaluacion_caso (
    id_evaluacion    SERIAL PRIMARY KEY,
    id_caso          INTEGER NOT NULL UNIQUE REFERENCES caso (id_caso),
    calificacion     SMALLINT NOT NULL,
    comentario       VARCHAR(500),
    fecha_evaluacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_evaluacion_calificacion CHECK (calificacion BETWEEN 1 AND 5)
);

-- historial_estado_caso (respaldo de CU-06 / CU-10)
CREATE TABLE IF NOT EXISTS historial_estado_caso (
    id_historial        SERIAL PRIMARY KEY,
    id_caso              INTEGER NOT NULL REFERENCES caso (id_caso),
    id_estado_anterior   INTEGER REFERENCES estado_caso (id_estado),
    id_estado_nuevo      INTEGER NOT NULL REFERENCES estado_caso (id_estado),
    id_personal          INTEGER REFERENCES personal (id_personal),
    observacion          VARCHAR(500),
    fecha_cambio          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_historial_estado_caso ON historial_estado_caso (id_caso);


-- =========================================================
-- PROCESOS AUTOMATICOS
-- =========================================================
--
-- notificacion (CU-14): el destinatario puede ser un usuario o personal interno
CREATE TABLE IF NOT EXISTS notificacion (
    id_notificacion      SERIAL PRIMARY KEY,
    id_caso              INTEGER REFERENCES caso (id_caso),
    id_usuario           INTEGER REFERENCES usuario (id_usuario),
    id_personal          INTEGER REFERENCES personal (id_personal),
    correo_destino       VARCHAR(100) NOT NULL,
    tipo_evento          VARCHAR(50) NOT NULL,
    asunto               VARCHAR(100) NOT NULL,
    contenido            VARCHAR(500) NOT NULL,
    estado_envio         VARCHAR(30) NOT NULL DEFAULT 'Pendiente',
    fecha_envio          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_notificacion_destinatario CHECK (id_usuario IS NOT NULL OR id_personal IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_notificacion_caso ON notificacion (id_caso);

-- bitacora_auditoria (CU-15): trazabilidad transversal, no editable
CREATE TABLE IF NOT EXISTS bitacora_auditoria (
    id_bitacora     SERIAL PRIMARY KEY,
    tipo_actor      VARCHAR(10) NOT NULL,
    id_usuario      INTEGER REFERENCES usuario (id_usuario),
    id_personal     INTEGER REFERENCES personal (id_personal),
    id_caso         INTEGER REFERENCES caso (id_caso),
    accion          VARCHAR(50) NOT NULL,
    modulo_afectado VARCHAR(40),
    ip_origen       VARCHAR(45),
    detalle         VARCHAR(1500),
    fecha_hora      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_bitacora_tipo_actor CHECK (tipo_actor IN ('Usuario', 'Personal', 'Sistema')),
    CONSTRAINT chk_bitacora_actor CHECK (id_usuario IS NOT NULL OR id_personal IS NOT NULL OR tipo_actor = 'Sistema')
);

CREATE INDEX IF NOT EXISTS idx_bitacora_caso ON bitacora_auditoria (id_caso);
CREATE INDEX IF NOT EXISTS idx_bitacora_fecha ON bitacora_auditoria (fecha_hora);
