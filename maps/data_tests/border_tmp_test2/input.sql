insert into {node} (node_id, shape) values
(1, Spatial::GeomFromText('POINT (0 0)', 4326)),
(2, Spatial::GeomFromText('POINT (1 0)', 4326)),
(3, Spatial::GeomFromText('POINT (1 1)', 4326)),
(4, Spatial::GeomFromText('POINT (0 1)', 4326)),
(5, Spatial::GeomFromText('POINT (2 0)', 4326)),
(6, Spatial::GeomFromText('POINT (2 2)', 4326)),
(7, Spatial::GeomFromText('POINT (0 2)', 4326));

insert into {edge} (edge_id, f_node_id, t_node_id, f_zlev, t_zlev, shape) values
(1, 1, 2, 0, 0, Spatial::GeomFromText('LINESTRING (0 0, 1 0)', 4326)),
(2, 2, 3, 0, 0, Spatial::GeomFromText('LINESTRING (1 0, 1 1)', 4326)),
(3, 3, 4, 0, 0, Spatial::GeomFromText('LINESTRING (1 1, 0 1)', 4326)),
(4, 4, 1, 0, 0, Spatial::GeomFromText('LINESTRING (0 1, 0 0)', 4326)),
(5, 2, 5, 0, 0, Spatial::GeomFromText('LINESTRING (1 0, 2 0)', 4326)),
(6, 5, 6, 0, 0, Spatial::GeomFromText('LINESTRING (2 0, 2 2)', 4326)),
(7, 6, 7, 0, 0, Spatial::GeomFromText('LINESTRING (2 2, 0 2)', 4326)),
(8, 7, 4, 0, 0, Spatial::GeomFromText('LINESTRING (0 2, 0 1)', 4326));

insert into {face} (face_id) values (1), (2), (3), (4);

insert into {face_edge} (face_id, edge_id) values
(1, 1), (1, 5), (1, 6), (1, 7), (1, 8), (1, 4),
(2, 1), (2, 2), (2, 3), (2, 4),
(3, 2), (3, 3),
(4, 1), (4, 4);

insert into {ad} (ad_id, p_ad_id, level_kind, disp_class, isocode, g_ad_id) values
(1, NULL, 1, 5, 'RS', NULL),  -- Serbia
(2, NULL, 1, 5, 'RS', 1),  -- Serbia without Kosovo
(2215802344, 1, 2, 5, 'RS', NULL),  -- Kosovo as a region in Serbia
(3, NULL, 1, 5, 'XK', 2215802344);  -- Kosovo as a country

insert into {ad_face} (ad_id, face_id, is_interior) values
(1, 1, false),
(2, 1, false),
(2215802344, 2, false),
(3, 2, false);

insert into {ad_face_patch} (ad_id, face_id, is_excluded) values
(2, 3, false),
(2, 4, true);

insert into {ad_recognition} (ad_id, isocode) values
(2, 'FR'),
(2, 'TR'),
(3, 'FR'),
(3, 'TR');

insert into {ad_non_recognition_tmp} (ad_id, isocode) values
(1, 'FR'),
(1, 'TR'),
(2215802344, 'FR'),
(2215802344, 'TR');

insert into {edge_extended_tmp} (edge_id, original_edge_id, geom, is_overland) values
(1, 1, Spatial::Transform(Spatial::GeomFromText('LINESTRING (0 0, 1 0)', 4326), 3395), true),
(2, 2, Spatial::Transform(Spatial::GeomFromText('LINESTRING (1 0, 1 1)', 4326), 3395), true),
(3, 3, Spatial::Transform(Spatial::GeomFromText('LINESTRING (1 1, 0 1)', 4326), 3395), true),
(4, 4, Spatial::Transform(Spatial::GeomFromText('LINESTRING (0 1, 0 0)', 4326), 3395), true),
(5, 5, Spatial::Transform(Spatial::GeomFromText('LINESTRING (1 0, 2 0)', 4326), 3395), true),
(6, 6, Spatial::Transform(Spatial::GeomFromText('LINESTRING (2 0, 2 2)', 4326), 3395), true),
(7, 7, Spatial::Transform(Spatial::GeomFromText('LINESTRING (2 2, 0 2)', 4326), 3395), true),
(8, 8, Spatial::Transform(Spatial::GeomFromText('LINESTRING (0 2, 0 1)', 4326), 3395), true);

insert into {ad_edge_extended_tmp} (ad_id, edge_id) values
(1, 1), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8),
(2, 2), (2, 3), (2, 5), (2, 6), (2, 7), (2, 8),
(2215802344, 1), (2215802344, 2), (2215802344, 3), (2215802344, 4),
(3, 1), (3, 2), (3, 3), (3, 4);
