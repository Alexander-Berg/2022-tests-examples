INSERT INTO {rd_el_extended_tmp_out} (rd_el_id, type, subtype, struct_type, min_geometry_zoom, min_label_zoom, geom) VALUES
(1, '1', '', 'none', 4, 10, NULL),
(2, '2', '', 'none', 4, 10, NULL),
(3, '3', '', 'none', 7, 11, NULL),
(4, '4', '', 'none', 8, 12, NULL),
(5, '5', '', 'none', 10, 12, NULL),
(6, '6', '', 'none', 12, 13, NULL),
(7, '7', '', 'none', 13, 13, NULL),
(8, '8', '', 'none', 15, 15, NULL),
(9, '9', '', 'none', 14, 15, NULL),

(10, 'ferry', '', 'none', 10, 11, NULL),
(11, 'construction', '', 'none', 13, 15, NULL),

(13, '10_bike', '', 'none', 16, 15, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), 3395)),
(14, '7_pedestrian', '', 'none', 13, 13, NULL),
(14, '10_pedestrian', '7_pedestrian', 'none', 13, NULL, NULL),
(15, '10_pedestrian', '', 'none', 16, 15, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), 3395)),

(16, '10_pedestrian', '', 'none', 16, 15, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), 3395)),
(16, '10_park', '', 'none', 13, 15, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 2, 0 3)', 4326), 3395)),
(17, '10_park', '10_outdoor', 'none', 13, 15, Spatial::Transform(Spatial::GeomFromText('LINESTRING(42.7880 43.8870, 42.7881 43.8870)', 4326), 3395)),

(21, '10_pedestrian', '', 'none', 16, 15, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), 3395)),
(22, '10_pedestrian', '', 'bridge', 16, 15, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), 3395)),
(23, '10_pedestrian', '', 'tunnel', 16, 15, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), 3395)),

(24, 'stairs', '', 'none', 16, 15, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), 3395)),
(25, 'crosswalk', '', 'none', 16, 15, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), 3395)),
(26, 'underpass', '', 'none', 16, 15, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), 3395)),

(31, '1_link', '', 'none', 4, 10, NULL),
(32, '2_link', '', 'none', 4, 10, NULL),
(33, '3_link', '', 'none', 7, 11, NULL),
(34, '4_link', '', 'none', 8, 12, NULL),
(35, '5_link', '', 'none', 10, 12, NULL),
(36, '6_link', '', 'none', 12, 13, NULL),

(37, '10_bike', '', 'none', 16, 15, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), 3395)),
(38, '10_pedestrian', '', 'none', 16, 15, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), 3395));
