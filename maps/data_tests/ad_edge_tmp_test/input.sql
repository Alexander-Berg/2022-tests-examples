insert into {node} (node_id, shape) values
(1, Spatial::GeomFromText('POINT (0 0)', 4326)),
(2, Spatial::GeomFromText('POINT (0 1)', 4326)),
(3, Spatial::GeomFromText('POINT (1 1)', 4326)),
(4, Spatial::GeomFromText('POINT (2 1)', 4326)),
(5, Spatial::GeomFromText('POINT (3 1)', 4326)),
(6, Spatial::GeomFromText('POINT (3 0)', 4326)),
(7, Spatial::GeomFromText('POINT (2 0)', 4326)),
(8, Spatial::GeomFromText('POINT (1 0)', 4326)),
(9, Spatial::GeomFromText('POINT (-1 0)', 4326)),
(10, Spatial::GeomFromText('POINT (0 -1)', 4326));

insert into {edge} (edge_id, f_node_id, t_node_id, f_zlev, t_zlev, shape) values
(1, 1, 2, 0, 0, Spatial::GeomFromText('LINESTRING (0 0, 0 1)', 4326)),
(2, 2, 3, 0, 0, Spatial::GeomFromText('LINESTRING (0 1, 1 1)', 4326)),
(3, 3, 4, 0, 0, Spatial::GeomFromText('LINESTRING (1 1, 2 1)', 4326)),
(4, 4, 5, 0, 0, Spatial::GeomFromText('LINESTRING (2 1, 3 1)', 4326)),
(5, 5, 6, 0, 0, Spatial::GeomFromText('LINESTRING (3 1, 3 0)', 4326)),
(6, 6, 7, 0, 0, Spatial::GeomFromText('LINESTRING (3 0, 2 0)', 4326)),
(7, 7, 8, 0, 0, Spatial::GeomFromText('LINESTRING (2 0, 1 0)', 4326)),
(8, 8, 1, 0, 0, Spatial::GeomFromText('LINESTRING (1 0, 0 0)', 4326)),
(9, 3, 8, 0, 0, Spatial::GeomFromText('LINESTRING (1 1, 0 1)', 4326)),
(10, 4, 7, 0, 0, Spatial::GeomFromText('LINESTRING (2 1, 2 0)', 4326)),
(11, 1, 9, 0, 0, Spatial::GeomFromText('LINESTRING (0 0, -1 0)', 4326)),
(12, 1, 10, 0, 0, Spatial::GeomFromText('LINESTRING (0 0, 0 -1)', 4326)),
(13, 9, 10, 0, 0, Spatial::GeomFromText('LINESTRING (-1 0, 0 -1)', 4326));

insert into {face} (face_id) values (1), (2), (3), (4), (5);

insert into {face_edge} (face_id, edge_id) values
(1, 1), (1, 2), (1, 9), (1, 8),
(2, 9), (2, 3), (2, 10), (2, 7),
(3, 10), (3, 4), (3, 5), (3, 6),
(4, 9), (4, 3), (4, 4), (4, 5), (4, 6), (4, 7),
(5, 11), (5, 12), (5, 13);

insert into {ad} (ad_id, p_ad_id, level_kind, disp_class, isocode, g_ad_id) values
(1, NULL, 1, 5, 'RU', NULL),
(2, NULL, 1, 5, 'RU', 1),
(3, NULL, 1, 5, 'UA', NULL),
(4, NULL, 1, 5, 'UA', 3),
(5, 1, 2, 5, 'RU', NULL),
(6, 3, 2, 5, 'UA', 5),
(7, NULL, 1, 5, 'XC', 5),
(8, 5, 3, 5, 'RU', NULL),
(9, NULL, 2, 5, 'GE', NULL);

insert into {ad_face} (ad_id, face_id, is_interior) values
(1, 4, false),
(3, 1, false),
(5, 2, false),
(8, 2, false),
(9, 5, false);

insert into {ad_face_patch} (ad_id, face_id, is_excluded) values
(2, 2, true),
(4, 2, false);
