INSERT INTO personas.profesion(nombre, nombre_clave) VALUES
('Arquitectura', 'arquitectura'), ('Ingeniería de sistemas', 'ingenieria de sistemas'),
('Docencia', 'docencia'), ('Medicina', 'medicina'), ('Contabilidad', 'contabilidad')
ON CONFLICT (nombre_clave) DO NOTHING;
