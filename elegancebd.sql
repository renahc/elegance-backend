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

INSERT INTO estilistas (id, name, role, specialty, avatar, rating, reviews_count, is_available, shift, completed_today_count) VALUES
('sty-1', 'Valentina Morales', 'Master Hair Specialist', 'Colorimetría & Balayage', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=300', 4.9, 142, TRUE, '09:00 - 18:00', 4),
('sty-2', 'Camila Rosselot', 'Senior Nail Artist', 'Manicura Rusa & Soft Gel', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=300', 4.8, 98, TRUE, '10:00 - 19:00', 5),
('sty-3', 'Isadora Benítez', 'Cosmiatra & Skin Expert', 'Faciales Avanzados & Peeling', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=300', 5.0, 115, FALSE, '11:00 - 20:00', 3),
('sty-4', 'Mateo Fuentes', 'Senior Hair Stylist', 'Cortes Tendencia & Peinados', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=300', 4.7, 76, TRUE, '09:30 - 18:30', 2)
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO clientes (id, name, email, phone, avatar, total_visits, total_spent, last_visit, tier, notes) VALUES
('cli-1', 'Sofia Larraín', 'sofia.larrain@gmail.com', '+56 9 8765 4321', 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&q=80&w=300', 14, 480000, '2026-08-28', 'VIP', 'Prefiere café americano con leches vegetales.'),
('cli-2', 'Fernanda Valenzuela', 'fer.valenzuela@outlook.com', '+56 9 7654 3210', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=300', 8, 260000, '2026-08-20', 'Frecuente', 'Cliente puntual.'),
('cli-3', 'Mariana Silva', 'marianasilva.design@gmail.com', '+56 9 6543 2109', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=300', 3, 125000, '2026-08-15', 'Regular', NULL),
('cli-4', 'Constanza Silva', 'coni.silva@empresa.cl', '+56 9 5432 1098', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=300', 1, 58000, '2026-08-30', 'Nuevo', 'Primera visita aprobada.'),
('cli-5', 'Antonia Edwards', 'antonia.edwards@gmail.com', '+56 9 4321 0987', 'https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&q=80&w=300', 22, 890000, '2026-08-25', 'VIP', 'Asiste cada 3 semanas.')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO citas (id, client_id, client_name, client_avatar, service_id, service_name, service_category, stylist_id, stylist_name, date, time, duration_minutes, price, status, notes) VALUES
('apt-101', 'cli-1', 'Sofia Larraín', 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&q=80&w=300', 'srv-1', 'Balayage & Iluminación Premium', 'cabello', 'sty-1', 'Valentina Morales', '2026-08-31', '10:00', 180, 95000, 'en_proceso', 'Tono rubio ceniza cálido.'),
('apt-102', 'cli-2', 'Fernanda Valenzuela', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=300', 'srv-4', 'Manicura Rusa + Esmaltado', 'uñas', 'sty-2', 'Camila Rosselot', '2026-08-31', '11:30', 90, 28000, 'confirmada', 'Tono Rose Gold brillante.'),
('apt-103', 'cli-3', 'Mariana Silva', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=300', 'srv-6', 'Facial Hidra-Lifting', 'facial', 'sty-3', 'Isadora Benítez', '2026-08-31', '14:00', 75, 58000, 'confirmada', NULL)
ON DUPLICATE KEY UPDATE service_name=VALUES(service_name);

INSERT INTO notificaciones (id, title, message, time, `read`, type) VALUES
('notif-1', 'Nueva Cita Confirmada', 'Sofia Larraín reservó Balayage Premium.', 'Hace 10 min', FALSE, 'appointment'),
('notif-2', 'Cliente VIP Agendado', 'Antonia Edwards reservó Maquillaje Social.', 'Hace 35 min', FALSE, 'client')
ON DUPLICATE KEY UPDATE title=VALUES(title);
