INSERT INTO {ft} (ft_id, p_ft_id, ft_type_id, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi) VALUES
(0, NULL, 171 /* urban-edu */, 5, 0, 5, 0),
(1, NULL, 171 /* urban-edu */, 5, 0, 5, 0),
(2, 1, 171 /* urban-edu */, 5, 0, 5, 0),
(3, NULL, 402 /* vegetation-park */, 5, 0, 5, 0),
(4, NULL, 221 /* urban-cemetery */, 5, 0, 5, 0),
(5, 4, 221 /* urban-cemetery */, 5, 0, 5, 0);

INSERT INTO {ft_geom} (ft_id, shape) VALUES
(0, Spatial::GeomFromText('POINT(0 0)', 4326)),
(1, Spatial::GeomFromText('POINT(1 1)', 4326)),
(2, Spatial::GeomFromText('POLYGON((0 0, 2 0, 2 2, 0 2, 0 0))', 4326)),
(3, Spatial::GeomFromText('POLYGON((-1 -1, 1 -1, 1 1, -1 1, -1 -1))', 4326)),
(4, Spatial::GeomFromText('POINT(4 4)', 4326)),
(5, Spatial::GeomFromText('POLYGON((4 4, 4 5, 5 5, 5 4, 4 4))', 4326));

INSERT INTO {node} (node_id, shape) VALUES
(3, Spatial::GeomFromText('POINT(3 3)', 4326)),
(4, Spatial::GeomFromText('POINT(4 4)', 4326)),
(5, Spatial::GeomFromText('POINT(4 4)', 4326));

INSERT INTO {ft_center} (ft_id, node_id) VALUES
(3, 3),
(4, 4),
(5, 5);

INSERT INTO {ft_nm_tmp} (ft_id, name, name_lo) VALUES
(3, json('{}'), '');
