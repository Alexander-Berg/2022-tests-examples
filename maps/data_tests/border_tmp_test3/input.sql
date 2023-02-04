insert into {node} (node_id, shape) values
(1, Spatial::GeomFromText('POINT (0 0)', 4326)),
(2, Spatial::GeomFromText('POINT (0 2)', 4326)),
(3, Spatial::GeomFromText('POINT (1 0)', 4326)),
(4, Spatial::GeomFromText('POINT (1 2)', 4326)),
(5, Spatial::GeomFromText('POINT (2 0)', 4326)),
(6, Spatial::GeomFromText('POINT (2 2)', 4326)),
(7, Spatial::GeomFromText('POINT (3 0)', 4326)),
(8, Spatial::GeomFromText('POINT (3 1)', 4326)),
(9, Spatial::GeomFromText('POINT (3 2)', 4326)),
(10, Spatial::GeomFromText('POINT (4 0)', 4326)),
(11, Spatial::GeomFromText('POINT (4 1)', 4326)),
(12, Spatial::GeomFromText('POINT (4 2)', 4326));

insert into {edge} (edge_id, f_node_id, t_node_id, f_zlev, t_zlev, shape) values
(1, 1, 2, 0, 0, Spatial::GeomFromText('LINESTRING (0 0, 0 2)', 4326)),
(2, 1, 3, 0, 0, Spatial::GeomFromText('LINESTRING (0 0, 1 0)', 4326)),
(3, 2, 4, 0, 0, Spatial::GeomFromText('LINESTRING (0 2, 1 2)', 4326)),
(4, 3, 4, 0, 0, Spatial::GeomFromText('LINESTRING (1 0, 1 2)', 4326)),
(5, 3, 5, 0, 0, Spatial::GeomFromText('LINESTRING (1 0, 2 0)', 4326)),
(6, 4, 6, 0, 0, Spatial::GeomFromText('LINESTRING (1 2, 2 2)', 4326)),
(7, 5, 6, 0, 0, Spatial::GeomFromText('LINESTRING (2 0, 2 2)', 4326)),
(8, 5, 7, 0, 0, Spatial::GeomFromText('LINESTRING (2 0, 3 0)', 4326)),
(9, 7, 8, 0, 0, Spatial::GeomFromText('LINESTRING (3 0, 3 1)', 4326)),
(10, 8, 9, 0, 0, Spatial::GeomFromText('LINESTRING (3 1, 3 2)', 4326)),
(11, 6, 9, 0, 0, Spatial::GeomFromText('LINESTRING (2 2, 3 2)', 4326)),
(12, 7, 10, 0, 0, Spatial::GeomFromText('LINESTRING (3 0, 4 0)', 4326)),
(13, 10, 11, 0, 0, Spatial::GeomFromText('LINESTRING (4 0, 4 1)', 4326)),
(14, 8, 11, 0, 0, Spatial::GeomFromText('LINESTRING (3 1, 4 1)', 4326)),
(15, 11, 12, 0, 0, Spatial::GeomFromText('LINESTRING (4 1, 4 2)', 4326)),
(16, 9, 12, 0, 0, Spatial::GeomFromText('LINESTRING (3 2, 4 2)', 4326));

insert into {face} (face_id) values
(1), (2), (3), (4), (5), (6), (7), (8), (9), (10);

insert into {face_edge} (face_id, edge_id) values
(1, 1), (1, 2), (1, 3),
(2, 4),
(3, 5), (3, 6),
(4, 7),
(5, 8), (5, 11),
(6, 9),
(7, 10),
(8, 12), (8, 13),
(9, 14),
(10, 15), (10, 16);

insert into {ad} (ad_id, p_ad_id, level_kind, disp_class, isocode, g_ad_id) values
(1, NULL, 1, 5, 'IL', NULL),  -- Israel (general)
(2, NULL, 1, 5, 'SY', NULL),  -- Syria (general)
(3, 1, 2, 5, 'IL', NULL),  -- Israel region
(4, 2, 2, 5, 'SY', NULL),  -- Golan Heights
(5, 2, 2, 5, 'SY', NULL),  -- Buffer zone
(6, 2, 2, 5, 'SY', NULL),  -- Syria region Quneitra Governorate (includes Golan Heights and buffer zone)
(7, 2, 2, 5, 'SY', NULL),  -- Syria region Daraa (has border with buffer zone)
(8, NULL, 1, 5, 'IL', 1),  -- Israel (alternative)
(9, NULL, 1,5,  'SY', 2),  -- Syria (alternative)
(10, 8, 2, 5, 'IL', 4),  -- Golan Heights (alternative)
(11, 9, 2, 5, 'SY', 6);  -- Syria region Quneitra Governorate (alternative)

insert into {ad_face} (ad_id, face_id, is_interior) values
(1, 1, false), (1, 2, false),
(2, 2, false), (2, 3, false), (2, 5, false), (2, 8, false), (2, 10, false),
(3, 1, false), (3, 2, false),
(4, 2, false), (4, 3, false), (4, 4, false),
(5, 4, false), (5, 5, false), (5, 6, false), (5, 7, false),
(6, 2, false), (6, 3, false), (6, 5, false), (6, 6, false), (6, 9, false), (6, 10, false),
(7, 6, false), (7, 8, false), (7, 9, false);

insert into {ad_face_patch} (ad_id, face_id, is_excluded) values
(8, 2, true), (8, 3, false), (8, 4, false),
(9, 2, true), (9, 3, true), (9, 4, false),
-- 10 is same with general version
(11, 2, true), (11, 3, true), (11, 5, true), (11, 6, true), (11, 7, false);

insert into {ad_recognition} (ad_id, isocode) values
(8, 'IL'),
(9, 'IL'),
(10, 'IL'),
(11, 'IL');

insert into {ad_non_recognition_tmp} (ad_id, isocode) values
(1, 'IL'),
(2, 'IL'),
(4, 'IL'),
(6, 'IL');

insert into {edge_extended_tmp} (edge_id, original_edge_id, geom, is_overland) values
(1, 1, Spatial::Transform(Spatial::GeomFromText('LINESTRING (0 0, 0 2)', 4326), 3395), true),
(2, 2, Spatial::Transform(Spatial::GeomFromText('LINESTRING (0 0, 1 0)', 4326), 3395), true),
(3, 3, Spatial::Transform(Spatial::GeomFromText('LINESTRING (0 2, 1 2)', 4326), 3395), true),
(4, 4, Spatial::Transform(Spatial::GeomFromText('LINESTRING (1 0, 1 2)', 4326), 3395), true),
(5, 5, Spatial::Transform(Spatial::GeomFromText('LINESTRING (1 0, 2 0)', 4326), 3395), true),
(6, 6, Spatial::Transform(Spatial::GeomFromText('LINESTRING (1 2, 2 2)', 4326), 3395), true),
(7, 7, Spatial::Transform(Spatial::GeomFromText('LINESTRING (2 0, 2 2)', 4326), 3395), true),
(8, 8, Spatial::Transform(Spatial::GeomFromText('LINESTRING (2 0, 3 0)', 4326), 3395), true),
(9, 9, Spatial::Transform(Spatial::GeomFromText('LINESTRING (3 0, 3 1)', 4326), 3395), true),
(10, 10, Spatial::Transform(Spatial::GeomFromText('LINESTRING (3 1, 3 2)', 4326), 3395), true),
(11, 11, Spatial::Transform(Spatial::GeomFromText('LINESTRING (2 2, 3 2)', 4326), 3395), true),
(12, 12, Spatial::Transform(Spatial::GeomFromText('LINESTRING (3 0, 4 0)', 4326), 3395), true),
(13, 13, Spatial::Transform(Spatial::GeomFromText('LINESTRING (4 0, 4 1)', 4326), 3395), true),
(14, 14, Spatial::Transform(Spatial::GeomFromText('LINESTRING (3 1, 4 1)', 4326), 3395), true),
(15, 15, Spatial::Transform(Spatial::GeomFromText('LINESTRING (4 1, 4 2)', 4326), 3395), true),
(16, 16, Spatial::Transform(Spatial::GeomFromText('LINESTRING (3 2, 4 2)', 4326), 3395), true);

insert into {ad_edge_extended_tmp} (ad_id, edge_id) values
(1, 1), (1, 2), (1, 3), (1, 4),
(2, 4), (2, 5), (2, 6), (2, 8), (2, 11), (2, 12), (2, 13), (2, 15), (2, 16),
(3, 1), (3, 2), (3, 3), (3, 4),
(4, 4), (4, 5), (4, 6), (4, 7),
(5, 7), (5, 8), (5, 9), (5, 10), (5, 11),
(6, 4), (6, 5), (6, 6), (6, 8), (6, 11), (6, 9), (6, 14), (6, 15), (6, 16),
(7, 9), (7, 12), (7, 13), (7, 14),
(8, 1), (8, 2), (8, 3), (8, 5), (8, 6), (8, 7),
(9, 7), (9, 8), (9, 11), (9, 12), (9, 13), (9, 15), (9, 16),
(10, 4), (10, 5), (10, 6), (10, 7),
(11, 10), (11, 14), (11, 15), (11, 16);
