INSERT INTO {ft} (ft_id, p_ft_id, ft_type_id, rubric_id, icon_class, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi, search_class, isocode, subcode) VALUES
-- Creating indoor plan and levels
(4000000000, null,       2301, null, null, 5, 0, 5, 0, null, '001', null),
(4000000001, 4000000000, 2302, null, null, 5, 0, 5, 0, null, '001', null),
(4000000002, 4000000000, 2302, null, null, 5, 0, 5, 0, null, '001', null),
-- Creating polygonal objects for every level
(4000000010, 4000000001, 2401, null, null, 5, 0, 5, 0, null, '001', null),
(4000000011, 4000000002, 2401, null, null, 5, 0, 5, 0, null, '001', null),
-- Creating barrier for every level
(4000000020, 4000000001, 2501, null, null, 5, 0, 5, 0, null, '001', null),
(4000000021, 4000000002, 2501, null, null, 5, 0, 5, 0, null, '001', null),
-- Creating poi for every level
(4000000030, 4000000001, 2601, null, null, 5, 0, 5, 0, null, '001', null),
(4000000031, 4000000002, 2601, null, null, 5, 0, 5, 0, null, '001', null);

INSERT INTO {ft_ft} (master_ft_id, slave_ft_id, role) VALUES
-- Default level
(4000000000, 4000000002, 'default-for'),
-- Level order
(4000000001, 4000000002, 'next-for');

INSERT INTO {ft_geom} (ft_id, shape) VALUES
-- Level geometry
(4000000001, Spatial::GeomFromText('POLYGON((37.536177 55.748808, 37.541949 55.750636, 37.543022 55.749607, 37.537271 55.747755, 37.536177 55.748808))', 4326)),
(4000000002, Spatial::GeomFromText('POLYGON((37.536177 55.758808, 37.541949 55.760636, 37.543022 55.759607, 37.537271 55.757755, 37.536177 55.758808))', 4326)),
-- Polygonal geometry
(4000000010, Spatial::GeomFromText('POLYGON((37.538077 55.748910, 37.539740 55.749666, 37.540373 55.749152, 37.538399 55.748656, 37.538077 55.748910))', 4326)),
(4000000011, Spatial::GeomFromText('POLYGON((37.540459 55.749902, 37.541263 55.750156, 37.541767 55.749612, 37.540877 55.749309, 37.540459 55.749902))', 4326)),
-- Barrier geometry
(4000000020, Spatial::GeomFromText('LINESTRING(37.538123 55.749876, 37.539287 55.748566)', 4326)),
(4000000021, Spatial::GeomFromText('LINESTRING(37.538967 55.749315, 37.539815 55.748976)', 4326)),
-- POI geometry
(4000000030, Spatial::GeomFromText('POINT(37.538223 55.749346)', 4326)),
(4000000031, Spatial::GeomFromText('POINT(37.539345 55.748497)', 4326));

INSERT INTO {ft_nm_tmp} (ft_id, name, name_universal) VALUES
(4000000001, json('{"en_001": "Level 1", "ru_001": "Этаж 1"}'), '1'),
(4000000002, json('{"en_001": "Level 2", "ru_001": "Этаж 2"}'), '2'),
(4000000030, json('{"en_001": "Entrance 1", "ru_001": "Вход 1"}'), null),
(4000000031, json('{"en_001": "Entrance 2", "ru_001": "Вход 2"}'), null);
