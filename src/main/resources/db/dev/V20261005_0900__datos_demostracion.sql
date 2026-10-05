-- Datos ficticios exclusivos del perfil dev. Nunca cargar en prod.
WITH demo AS (
    SELECT n, '90' || lpad(n::text, 6, '0') AS dni,
        CASE WHEN n <= 5 THEN 'ESTUDIANTE'
             WHEN n <= 10 THEN 'DOCENTE'
             WHEN n <= 15 THEN 'PERSONAL' ELSE 'VISITANTE' END AS tipo,
        (ARRAY['Lucia','Mateo','Valeria','Diego','Camila','Sofia','Luis','Elena','Rafael','Paula',
               'Martin','Gabriela','Andres','Carla','Jorge','Marta','Alonso','Rosa','Pablo','Irene'])[n] AS nombres,
        (ARRAY['Quispe','Rojas','Flores','Vargas','Huaman','Torres','Mamani','Salazar','Paredes','Castillo',
               'Rivera','Medina','Condori','Sanchez','Chavez','Morales','Herrera','Cruz','Reyes','Lopez'])[n] AS apellidos
    FROM generate_series(1, 20) n
)
INSERT INTO persona (tipo, dni, nombres, apellidos, codigo, area, vigencia_inicio, vigencia_fin, activo)
SELECT tipo, dni, nombres, apellidos,
       CASE WHEN n <= 15 THEN 'DEM-' || lpad(n::text, 3, '0') END,
       CASE tipo WHEN 'ESTUDIANTE' THEN 'Ingeniería de sistemas'
                 WHEN 'DOCENTE' THEN 'Docencia'
                 WHEN 'PERSONAL' THEN 'Administración' ELSE 'Visita' END,
       current_date - 30,
       CASE WHEN n = 20 THEN current_date - 1 END,
       n <> 19
FROM demo
ON CONFLICT (dni) DO NOTHING;

INSERT INTO credencial (persona_id, tipo, valor)
SELECT p.id, CASE WHEN n % 2 = 1 THEN 'NFC' ELSE 'QR' END,
       CASE WHEN n % 2 = 1 THEN upper('04AA' || lpad(to_hex(n), 4, '0'))
            ELSE 'QR-DEMO-' || lpad(n::text, 2, '0') END
FROM generate_series(1, 20) n
JOIN persona p ON p.dni = '90' || lpad(n::text, 6, '0')
ON CONFLICT DO NOTHING;

INSERT INTO dispositivo (nombre, punto, token_hash)
VALUES ('Puerta peatonal demo', 'PEATONAL',
        '82c1f8f19fc6de068b5878f5392c64ab99ece5b7b9ccc9ee6e150e377c578c93'),
       ('Puerta vehicular demo', 'VEHICULAR',
        '2e848a08079450012a9d9bb8010baa8aa6ff9a4f7fbcadb2e2fbad102a2a50c7')
ON CONFLICT (token_hash) DO NOTHING;

INSERT INTO regla_acceso (tipo_persona, punto, dias, hora_inicio, hora_fin, descripcion)
VALUES ('ESTUDIANTE', 'PEATONAL', '1,2,3,4,5,6,7', '06:00', '22:00', 'Demo: estudiantes'),
       ('DOCENTE', 'PEATONAL', '1,2,3,4,5,6,7', '06:00', '23:00', 'Demo: docentes'),
       ('PERSONAL', 'PEATONAL', '1,2,3,4,5,6,7', '06:00', '23:00', 'Demo: personal'),
       ('VISITANTE', 'VEHICULAR', '1,2,3,4,5,6,7', '08:00', '18:00', 'Demo: visitantes en vehículo');
