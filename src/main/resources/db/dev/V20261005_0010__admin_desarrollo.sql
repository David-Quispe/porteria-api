-- Exclusivo del perfil dev. Credenciales públicas de demostración: admin.dev / PorteriaDev-2026!
-- En producción solo se carga db/migration; este usuario nunca debe crearse allí.
INSERT INTO usuario (username, password_hash, nombre, rol)
VALUES ('admin.dev', '$2a$10$XoPj0FbKvjzm0diV6NqtHuAI/dYSnTz1kNnpAAHRKYgR4NmMQ7Pae',
        'Administrador de desarrollo', 'ADMIN')
ON CONFLICT (username) DO NOTHING;
