INSERT INTO {ft} (ft_id, p_ft_id, ft_type_id, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi) VALUES
(0, NULL, 226 /* urban-leisure-beach */, 5, 0, 5, 0),
(1, NULL, 223 /* urban-leisure-animalpark */, 5, 0, 5, 0),
(2, 1, 222 /* urban-leisure */, 5, 0, 5, 0),
(3, NULL, 171 /* urban-edu */, 5, 0, 5, 0),
(4, NULL, 169 /* urban-edu-university */, 5, 0, 5, 0),
(5, NULL, 2001 /* urban-roadnet-parking-free */, 6, 0, 6, 0),
(6, 5, 251 /* urban-roadnet-parking-lot */, 5, 0, 5, 0);

INSERT INTO {ft_geom} (ft_id, shape) VALUES
(0, Spatial::GeomFromText('POLYGON((0 0, 1 0, 1 1, 0 1, 0 0))', 4326)),
(2, Spatial::GeomFromText('POLYGON((0 0, 1 0, 1 1, 0 1, 0 0))', 4326)),
(3, Spatial::GeomFromText('POLYGON((0 0, 1 0, 1 1, 0 1, 0 0))', 4326)),
(4, Spatial::GeomFromText('POLYGON((0 0, 1 0, 1 1, 0 1, 0 0))', 4326)),
(6, Spatial::GeomFromText('POLYGON((0 0, 1 0, 1 1, 0 1, 0 0))', 4326));
