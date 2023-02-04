----------------------------------- Inserting into ft -----------------------------------
INSERT INTO {ft} (ft_id, p_ft_id, ft_type_id, rubric_id, icon_class, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi, search_class, isocode, subcode) VALUES

-- Creating indoor plan and levels
(4000000000, null,       2301, null, null, 5, 0, 5, 0, null, '001', null),
(4000000001, 4000000000, 2302, null, null, 5, 0, 5, 0, null, '001', null),
(4000000002, 4000000000, 2302, null, null, 5, 0, 5, 0, null, '001', null),

-- Creating barrier for every level
(4000000020, 4000000001, 2501, null, null, 5, 0, 5, 0, null, '001', null),
(4000000021, 4000000002, 2501, null, null, 5, 0, 5, 0, null, '001', null);

----------------------------------- Inserting into ft_geom -----------------------------------
INSERT INTO {ft_geom} (ft_id, shape) VALUES

-- Barrier geometry
(4000000020, Spatial::GeomFromText('LINESTRING(37.538123 55.749876, 37.539287 55.748566)', 4326)),
(4000000021, Spatial::GeomFromText('LINESTRING(37.538967 55.749315, 37.539815 55.748976)', 4326));

----------------------------------- Inserting into indoor_level_data -----------------------------------
INSERT INTO {indoor_level_data} (ft_id, ft_type_id, ft_type_name, p_ft_id, disp_class, name, indoor_plan_id, indoor_level_id, geom) VALUES

(4000000001, 2302, 'indoor-level', 4000000000, 5, json('{"en_001": "Level 1", "ru_001": "Этаж 1"}'), '7F46B130C81DB5A6F315588E94BD6239', '1',
    Spatial::Transform(Spatial::GeomFromText('POLYGON((37.536177 55.748808,37.541949 55.750636,37.543022 55.749607,37.537271 55.747755,37.536177 55.748808))', 4326), 3395)),

(4000000002, 2302, 'indoor-level', 4000000000, 5, json('{"en_001": "Level 2", "ru_001": "Этаж 2"}'), '7F46B130C81DB5A6F315588E94BD6239', '2',
    Spatial::Transform(Spatial::GeomFromText('POLYGON((37.536177 55.758808,37.541949 55.760636,37.543022 55.759607,37.537271 55.757755,37.536177 55.758808))', 4326), 3395));
