----------------------------------- Inserting into ft -----------------------------------
INSERT INTO {ft} (ft_id, p_ft_id, ft_type_id, rubric_id, icon_class, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi, search_class, isocode, subcode) VALUES

-- Creating indoor plan and levels
(4000000000, null,       2301, null, null, 5, 0, 5, 0, null, '001', null),
(4000000001, 4000000000, 2302, null, null, 5, 0, 5, 0, null, '001', null),
(4000000002, 4000000000, 2302, null, null, 5, 0, 5, 0, null, '001', null),

-- Creating polygonal objects for every level
(4000000010, 4000000001, 2401, null, null, 5, 0, 5, 0, null, '001', null),
(4000000011, 4000000002, 2401, null, null, 5, 0, 5, 0, null, '001', null);

----------------------------------- Inserting into ft_geom -----------------------------------
INSERT INTO {ft_geom} (ft_id, shape) VALUES

-- Level geometry
(4000000001, Spatial::GeomFromText('POLYGON((37.536177 55.748808, 37.541949 55.750636, 37.543022 55.749607, 37.537271 55.747755, 37.536177 55.748808))', 4326)),
(4000000002, Spatial::GeomFromText('POLYGON((37.536177 55.758808, 37.541949 55.760636, 37.543022 55.759607, 37.537271 55.757755, 37.536177 55.758808))', 4326)),

-- Polygonal geometry
(4000000010, Spatial::GeomFromText('POLYGON((37.538077 55.748910, 37.539740 55.749666, 37.540373 55.749152, 37.538399 55.748656, 37.538077 55.748910))', 4326)),
(4000000011, Spatial::GeomFromText('POLYGON((37.540459 55.749902, 37.541263 55.750156, 37.541767 55.749612, 37.540877 55.749309, 37.540459 55.749902))', 4326));

----------------------------------- Inserting into indoor_level_data -----------------------------------
INSERT INTO {indoor_level_data} (ft_id, ft_type_id, ft_type_name, p_ft_id, disp_class, name, indoor_plan_id, indoor_level_id, geom) VALUES

(4000000001, 2302, 'indoor-level', 4000000000, 5, json('{"en_001": "Level 1", "ru_001": "Этаж 1"}'), '7F46B130C81DB5A6F315588E94BD6239', '1',
    Spatial::Transform(Spatial::GeomFromText('POLYGON((37.536177 55.748808,37.541949 55.750636,37.543022 55.749607,37.537271 55.747755,37.536177 55.748808))', 4326), 3395)),

(4000000002, 2302, 'indoor-level', 4000000000, 5, json('{"en_001": "Level 2", "ru_001": "Этаж 2"}'), '7F46B130C81DB5A6F315588E94BD6239', '2',
    Spatial::Transform(Spatial::GeomFromText('POLYGON((37.536177 55.758808,37.541949 55.760636,37.543022 55.759607,37.537271 55.757755,37.536177 55.758808))', 4326), 3395));
