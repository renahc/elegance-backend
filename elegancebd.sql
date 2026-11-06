-- Script SQL completo para la base de datos elegancebd (Élégance Beauty Studio)
-- Se puede importar directamente en MySQL / MariaDB

CREATE DATABASE IF NOT EXISTS elegancebd CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE elegancebd;

-- 1. Tabla de Servicios
CREATE TABLE IF NOT EXISTS servicios (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    price INT NOT NULL,
    duration_minutes INT NOT NULL,
    popular BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Tabla de Estilistas
CREATE TABLE IF NOT EXISTS estilistas (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(150) NOT NULL,
    specialty VARCHAR(255) NOT NULL,
    avatar VARCHAR(500),
    rating DOUBLE DEFAULT 5.0,
    reviews_count INT DEFAULT 0,
    is_available BOOLEAN DEFAULT TRUE,
    shift VARCHAR(100),
    completed_today_count INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Tabla de Clientes (CRM)
CREATE TABLE IF NOT EXISTS clientes (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    avatar VARCHAR(500),
    total_visits INT DEFAULT 0,
    total_spent INT DEFAULT 0,
    last_visit VARCHAR(50),
    tier VARCHAR(50) DEFAULT 'Nuevo',
    notes TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Tabla de Citas & Agenda
CREATE TABLE IF NOT EXISTS citas (
    id VARCHAR(50) PRIMARY KEY,
    client_id VARCHAR(50),
    client_name VARCHAR(255) NOT NULL,
    client_avatar VARCHAR(500),
    service_id VARCHAR(50),
    service_name VARCHAR(255) NOT NULL,
    service_category VARCHAR(100),
    stylist_id VARCHAR(50),
    stylist_name VARCHAR(255) NOT NULL,
    date VARCHAR(50) NOT NULL,
    time VARCHAR(50) NOT NULL,
    duration_minutes INT NOT NULL,
    price INT NOT NULL,
    status VARCHAR(50) DEFAULT 'confirmada',
    notes TEXT,
    CONSTRAINT fk_cita_cliente FOREIGN KEY (client_id) REFERENCES clientes(id) ON DELETE SET NULL,
    CONSTRAINT fk_cita_servicio FOREIGN KEY (service_id) REFERENCES servicios(id) ON DELETE SET NULL,
    CONSTRAINT fk_cita_estilista FOREIGN KEY (stylist_id) REFERENCES estilistas(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Tabla de Notificaciones
CREATE TABLE IF NOT EXISTS notificaciones (
    id VARCHAR(50) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    time VARCHAR(50),
    `read` BOOLEAN DEFAULT FALSE,
    type VARCHAR(50) DEFAULT 'system'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================================================
-- INSERCIÓN DE DATOS INICIALES
-- ==========================================================================

INSERT INTO servicios (id, name, category, price, duration_minutes, popular, active) VALUES
('srv-1', 'Balayage & Iluminación Premium', 'cabello', 95000, 180, TRUE, TRUE),
('srv-2', 'Corte Estilizado + Brushing', 'cabello', 32000, 60, TRUE, TRUE),
('srv-3', 'Tratamiento Reparador Olaplex', 'cabello', 45000, 75, FALSE, TRUE),
('srv-4', 'Manicura Rusa + Esmaltado Permanente', 'uñas', 28000, 90, TRUE, TRUE),
('srv-5', 'Pedicura Spa Hidratante', 'uñas', 24000, 60, FALSE, TRUE),
('srv-6', 'Facial Hidra-Lifting con Ácido Hialurónico', 'facial', 58000, 75, TRUE, TRUE),
('srv-7', 'Diseño de Cejas & Laminado HD', 'facial', 22000, 45, FALSE, TRUE),
('srv-8', 'Maquillaje Social & Novias', 'maquillaje', 65000, 90, TRUE, TRUE),
('srv-9', 'Masaje Relajante Piedras Volcánicas', 'spa', 42000, 60, FALSE, TRUE)
ON DUPLICATE KEY UPDATE name=VALUES(name), price=VALUES(price);

-- Synthetic CRM and staff fixtures are generated only by the explicit Spring demo profile.
