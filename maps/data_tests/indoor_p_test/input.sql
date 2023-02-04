INSERT INTO {ft} (ft_id, p_ft_id, ft_type_id, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi, isocode) VALUES
-- indoor plan
(4000000000, null,       2301, 5, 0, 5, 0, '001'),
-- indoor levels
(4000000001, 4000000000, 2302, 5, 0, 5, 0, '001'),
(4000000002, 4000000000, 2302, 5, 0, 5, 0, '001'),
-- poi with indoor level binding
(4000000030, 4000000001, 1503, 5, 0, 5, 0, '001'),
(4000000031, 4000000002, 1503, 5, 0, 5, 0, '001'),
-- poi without indoor level binding
(4000000032, null,       1503, 5, 0, 5, 0, '001');

INSERT INTO {ft_geom} (ft_id, shape) VALUES
-- poi geometry
(4000000030, Spatial::GeomFromText('POINT(1 1)', 4326)),
(4000000031, Spatial::GeomFromText('POINT(1 1)', 4326)),
(4000000032, Spatial::GeomFromText('POINT(3 1)', 4326));

INSERT INTO {indoor_level_data} (ft_id, ft_type_id, ft_type_name, p_ft_id, disp_class, name, indoor_plan_id, indoor_level_id, geom) VALUES
(4000000001, 2302, 'indoor-level', 4000000000, 5, json('{"en_001": "Level 1", "ru_001": "Этаж 1"}'), '7F46B130C81DB5A6F315588E94BD6239', '1',
    Spatial::Transform(Spatial::GeomFromText('POLYGON((0 0, 4 0, 4 2, 0 2, 0 0))', 4326), 3395)),
(4000000002, 2302, 'indoor-level', 4000000000, 5, json('{"en_001": "Level 2", "ru_001": "Этаж 2"}'), '7F46B130C81DB5A6F315588E94BD6239', '2',
    Spatial::Transform(Spatial::GeomFromText('POLYGON((0 0, 2 0, 2 2, 0 2, 0 0))', 4326), 3395));
