----------------------------------- Inserting into ft -----------------------------------
-- Creating indoor plan and levels
INSERT INTO {ft} (ft_id, p_ft_id, ft_type_id, rubric_id, icon_class, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi, search_class, isocode, subcode) VALUES
(4000000000, null,       2301, null, null, 5, 0, 5, 0, null, '001', null),
(4000000001, 4000000000, 2302, null, null, 5, 0, 5, 0, null, '001', null),
(4000000002, 4000000000, 2302, null, null, 5, 0, 5, 0, null, '001', null);

----------------------------------- Inserting into ft_ft -----------------------------------
INSERT INTO {ft_ft} (master_ft_id, slave_ft_id, role) VALUES
-- Default level
(4000000000, 4000000002, 'default-for'),
-- Level order
(4000000001, 4000000002, 'next-for');

----------------------------------- Inserting into indoor_level_tmp -----------------------------------
INSERT INTO {indoor_level_tmp} (ft_id, p_ft_id, ft_type_id, disp_class, indoor_level_id, name, geom) VALUES
-- Level 1
(4000000001, 4000000000, 2302, 5, '1', json('{"en_001": "Level 1", "ru_001": "Этаж 1"}'),
    Spatial::Transform(Spatial::GeomFromText('POLYGON((37.536177 55.748808, 37.541949 55.750636, 37.543022 55.749607, 37.537271 55.747755, 37.536177 55.748808))', 4326), 3395)),
-- Level 2
(4000000002, 4000000000, 2302, 5, '2', json('{"en_001": "Level 2", "ru_001": "Этаж 2"}'),
    Spatial::Transform(Spatial::GeomFromText('POLYGON((37.536177 55.758808, 37.541949 55.760636, 37.543022 55.759607, 37.537271 55.757755, 37.536177 55.758808))', 4326), 3395));
