-- ============================================================================
-- SCRIPT DE BASE DE DATOS: PORTAL INSTITUCIONAL FEDER (POSTGRESQL)
-- Asignatura: Tecnologia Web (INF513) - Primer Parcial
-- Arquitectura: 3 Capas (EmailApp -> N... -> D... -> DB)
-- ============================================================================

-- Limpieza preventiva de tablas (en orden inverso de dependencias)
DROP TABLE IF EXISTS APORTE CASCADE;
DROP TABLE IF EXISTS CAMPANA CASCADE;
DROP TABLE IF EXISTS GALERIA_MULTIMEDIA CASCADE;
DROP TABLE IF EXISTS NOTICIA CASCADE;
DROP TABLE IF EXISTS TESTIMONIO CASCADE;
DROP TABLE IF EXISTS PROGRAMA_PRODUCTIVO CASCADE;
DROP TABLE IF EXISTS COMUNIDAD CASCADE;
DROP TABLE IF EXISTS RED_SOCIAL CASCADE;
DROP TABLE IF EXISTS PAGINA_INSTITUCIONAL CASCADE;
DROP TABLE IF EXISTS CONSULTA CASCADE;
DROP TABLE IF EXISTS AUDITORIA CASCADE;
DROP TABLE IF EXISTS USUARIO CASCADE;
DROP TABLE IF EXISTS ROL CASCADE;

-- ============================================================================
-- 1. PAQUETE DE SEGURIDAD Y USUARIOS
-- ============================================================================

-- Tabla: ROL
CREATE TABLE ROL (
    id_rol SERIAL PRIMARY KEY,
    nombre_rol VARCHAR(50) UNIQUE NOT NULL,
    descripcion VARCHAR(255) NOT NULL
);

-- Tabla: USUARIO
CREATE TABLE USUARIO (
    id_usuario SERIAL PRIMARY KEY,
    nombre_completo VARCHAR(150) NOT NULL,
    correo_electronico VARCHAR(150) UNIQUE NOT NULL,
    hash_contrasena VARCHAR(255) NOT NULL,
    estado VARCHAR(20) DEFAULT 'Activo' NOT NULL,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    rol_id INTEGER NOT NULL REFERENCES ROL(id_rol) 
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- Tabla: AUDITORIA
CREATE TABLE AUDITORIA (
    id_auditoria SERIAL PRIMARY KEY,
    accion_realizada VARCHAR(100) NOT NULL,
    tabla_afectada VARCHAR(50) NOT NULL,
    fecha_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    usuario_id INTEGER NOT NULL REFERENCES USUARIO(id_usuario) 
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- ============================================================================
-- 2. PAQUETE DE DIFUSIÓN INSTITUCIONAL Y COMUNIDADES (CMS)
-- ============================================================================

-- Tabla: PAGINA_INSTITUCIONAL
CREATE TABLE PAGINA_INSTITUCIONAL (
    id_pagina SERIAL PRIMARY KEY,
    titulo VARCHAR(100) NOT NULL,
    contenido TEXT NOT NULL,
    ultima_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    usuario_id INTEGER NOT NULL REFERENCES USUARIO(id_usuario) 
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- Tabla: RED_SOCIAL
CREATE TABLE RED_SOCIAL (
    id_red_social SERIAL PRIMARY KEY,
    plataforma VARCHAR(50) NOT NULL,
    url_enlace VARCHAR(255) NOT NULL,
    estado VARCHAR(20) DEFAULT 'Activo' NOT NULL,
    usuario_id INTEGER NOT NULL REFERENCES USUARIO(id_usuario) 
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- Tabla: COMUNIDAD
CREATE TABLE COMUNIDAD (
    id_comunidad SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    ubicacion VARCHAR(255) NOT NULL,
    estado VARCHAR(20) DEFAULT 'Activa' NOT NULL,
    usuario_id INTEGER NOT NULL REFERENCES USUARIO(id_usuario) 
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- Tabla: PROGRAMA_PRODUCTIVO
CREATE TABLE PROGRAMA_PRODUCTIVO (
    id_programa SERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT NOT NULL,
    area_productiva VARCHAR(100) NOT NULL,
    poblacion_beneficiaria VARCHAR(150),
    comunidad_id INTEGER NOT NULL REFERENCES COMUNIDAD(id_comunidad) 
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- Tabla: TESTIMONIO
CREATE TABLE TESTIMONIO (
    id_testimonio SERIAL PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    texto TEXT NOT NULL,
    fotografia VARCHAR(255),
    enlace_audiovisual VARCHAR(255),
    estado VARCHAR(20) DEFAULT 'Publicado' NOT NULL,
    comunidad_id INTEGER NOT NULL REFERENCES COMUNIDAD(id_comunidad) 
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- Tabla: NOTICIA
CREATE TABLE NOTICIA (
    id_noticia SERIAL PRIMARY KEY,
    titulo VARCHAR(200) NOT NULL,
    contenido TEXT NOT NULL,
    fecha_publicacion DATE DEFAULT CURRENT_DATE NOT NULL,
    imagen_principal VARCHAR(255) NOT NULL,
    etiquetas VARCHAR(200),
    estado VARCHAR(20) DEFAULT 'Publicado' NOT NULL,
    usuario_id INTEGER NOT NULL REFERENCES USUARIO(id_usuario) 
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- Tabla: GALERIA_MULTIMEDIA
CREATE TABLE GALERIA_MULTIMEDIA (
    id_multimedia SERIAL PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    tipo VARCHAR(20) NOT NULL, -- 'Imagen' o 'Video'
    url_archivo VARCHAR(255) NOT NULL,
    descripcion VARCHAR(255),
    fecha_carga TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    noticia_id INTEGER NOT NULL REFERENCES NOTICIA(id_noticia) 
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- ============================================================================
-- 3. PAQUETE DE RECAUDACIÓN Y FINANCIAMIENTO (CROWDFUNDING)
-- ============================================================================

-- Tabla: CAMPANA (Se estandariza sin tilde/ñ para evitar problemas en JDBC)
CREATE TABLE CAMPANA (
    id_campana SERIAL PRIMARY KEY,
    titulo VARCHAR(200) NOT NULL,
    descripcion TEXT NOT NULL,
    meta_economica DECIMAL(12,2) NOT NULL CHECK (meta_economica > 0),
    monto_recaudado DECIMAL(12,2) DEFAULT 0.00 NOT NULL CHECK (monto_recaudado >= 0),
    fecha_inicio DATE DEFAULT CURRENT_DATE NOT NULL,
    fecha_limite DATE NOT NULL,
    estado VARCHAR(20) DEFAULT 'Activa' NOT NULL,
    comunidad_id INTEGER NOT NULL REFERENCES COMUNIDAD(id_comunidad) 
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_fechas CHECK (fecha_limite >= fecha_inicio)
);

-- Tabla: APORTE
CREATE TABLE APORTE (
    id_aporte SERIAL PRIMARY KEY,
    nombre_donante VARCHAR(150) NOT NULL,
    correo_donante VARCHAR(150) NOT NULL,
    monto DECIMAL(10,2) NOT NULL CHECK (monto > 0),
    modalidad VARCHAR(50) NOT NULL, -- 'Transferencia bancaria', 'QR', 'Efectivo'
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    estado VARCHAR(25) DEFAULT 'Pendiente de Confirmación' NOT NULL,
    campana_id INTEGER NOT NULL REFERENCES CAMPANA(id_campana) 
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- ============================================================================
-- 4. PAQUETE DE INTERACCIÓN Y ADMINISTRACIÓN
-- ============================================================================

-- Tabla: CONSULTA
CREATE TABLE CONSULTA (
    id_consulta SERIAL PRIMARY KEY,
    nombre_remitente VARCHAR(150) NOT NULL,
    correo_remitente VARCHAR(150) NOT NULL,
    asunto VARCHAR(150) NOT NULL,
    mensaje TEXT NOT NULL,
    fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    estado VARCHAR(20) DEFAULT 'Pendiente' NOT NULL,
    usuario_id INTEGER REFERENCES USUARIO(id_usuario) 
        ON DELETE SET NULL ON UPDATE CASCADE
);

-- ============================================================================
-- 5. DATOS SEMILLA (INSERCIONES INICIALES)
-- ============================================================================

-- Roles predefinidos
INSERT INTO ROL (nombre_rol, descripcion) VALUES
('Director', 'Maximo nivel de administracion con control total del sistema'),
('Secretaria', 'Personal administrativo de atencion ciudadana y consultas'),
('Instructor', 'Personal tecnico de campo encargado de talleres y publicaciones');

-- Usuario Director por defecto (Credencial inicial)
INSERT INTO USUARIO (nombre_completo, correo_electronico, hash_contrasena, estado, rol_id) VALUES
('Director General FEDER', 'grupo16sc@tecnoweb.org.bo', 'admin123', 'Activo', 1);

-- Comunidades base de la Chiquitania
INSERT INTO COMUNIDAD (nombre, descripcion, ubicacion, estado, usuario_id) VALUES
('San Jose de Chiquitos', 'Municipio historico con alta produccion artesanal y derivados de totaí', 'Chiquitania, Santa Cruz', 'Activa', 1),
('Robore', 'Comunidad productora de aceites naturales y jaboneria silvestre', 'Chiquitania, Santa Cruz', 'Activa', 1),
('Okinawa', 'Comunidad con programas de desarrollo rural y agroindustria artesanal', 'Chiquitania, Santa Cruz', 'Activa', 1);

-- Paginas institucionales base
INSERT INTO PAGINA_INSTITUCIONAL (titulo, contenido, usuario_id) VALUES
('Mision', 'Ofrecer servicios financieros, asistencia tecnica de negocio a negocio y apertura de mercados.', 1),
('Vision', 'Ser una organizacion solida, rentable y competitiva en el sector rural y microempresario.', 1);