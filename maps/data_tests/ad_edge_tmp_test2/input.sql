-- *------*------*     *------*------*     *------*------*     *------*------*
-- |######|      |     |      |      |     |      |######|     |#############|
-- |######|      |     |      |      |     |      |######|     |#############|
-- |######| *--* |     |      | *--* |     |      |######|     |#############|
-- |######| |##| |  -  |      | |##| |  +  |      |######|  =  |#############|
-- |######| |##| |     |      | |##| |     |      |######|     |#############|
-- |######| *--* |     |      | *--* |     |      |######|     |#############|
-- |######|      |     |      |      |     |      |######|     |#############|
-- |######|      |     |      |      |     |      |######|     |#############|
-- *------*------*     *------*------*     *------*------*     *------*------*

insert into {node} (node_id, shape) values
(101, Spatial::GeomFromText('POINT (0 0)', 4326)),
(102, Spatial::GeomFromText('POINT (1 0)', 4326)),
(103, Spatial::GeomFromText('POINT (4 0)', 4326)),
(104, Spatial::GeomFromText('POINT (0 3)', 4326)),
(105, Spatial::GeomFromText('POINT (1 3)', 4326)),
(106, Spatial::GeomFromText('POINT (4 3)', 4326)),
(107, Spatial::GeomFromText('POINT (2 1)', 4326)),
(108, Spatial::GeomFromText('POINT (3 1)', 4326)),
(109, Spatial::GeomFromText('POINT (2 2)', 4326)),
(110, Spatial::GeomFromText('POINT (3 2)', 4326));

insert into {edge} (edge_id, f_node_id, t_node_id, f_zlev, t_zlev, shape) values
(101, 101, 102, 0, 0, Spatial::GeomFromText('LINESTRING (0 0, 1 0)', 4326)),
(102, 102, 103, 0, 0, Spatial::GeomFromText('LINESTRING (1 0, 3 0)', 4326)),
(103, 101, 104, 0, 0, Spatial::GeomFromText('LINESTRING (0 0, 0 3)', 4326)),
(104, 102, 105, 0, 0, Spatial::GeomFromText('LINESTRING (1 0, 1 3)', 4326)),
(105, 103, 106, 0, 0, Spatial::GeomFromText('LINESTRING (4 0, 4 3)', 4326)),
(106, 104, 105, 0, 0, Spatial::GeomFromText('LINESTRING (0 3, 1 3)', 4326)),
(107, 105, 106, 0, 0, Spatial::GeomFromText('LINESTRING (1 3, 4 3)', 4326)),
(108, 107, 108, 0, 0, Spatial::GeomFromText('LINESTRING (2 1, 3 1)', 4326)),
(109, 107, 109, 0, 0, Spatial::GeomFromText('LINESTRING (2 1, 2 2)', 4326)),
(110, 108, 110, 0, 0, Spatial::GeomFromText('LINESTRING (3 1, 3 2)', 4326)),
(111, 109, 110, 0, 0, Spatial::GeomFromText('LINESTRING (2 2, 3 2)', 4326));

insert into {face} (face_id) values
(101), (102), (103);

insert into {face_edge} (face_id, edge_id) values
(101, 101), (101, 103), (101, 104), (101, 106),
(102, 108), (102, 109), (102, 110), (102, 111),
(103, 102), (103, 104), (103, 105), (103, 107);

insert into {ad} (ad_id, p_ad_id, level_kind, disp_class, isocode, g_ad_id) values
(101, NULL, 1, 5, 'AA', NULL),
(102, NULL, 1, 5, 'AA', 101),
(103, NULL, 1, 5, 'BB', NULL),
(104, NULL, 1, 5, 'BB', 103);

insert into {ad_face} (ad_id, face_id, is_interior) values
(101, 101, false),
(101, 102, false),
(103, 103, false),
(103, 102, true);

insert into {ad_face_patch} (ad_id, face_id, is_excluded) values
(102, 102, true),
(102, 103, false),
(104, 102, false),
(104, 103, true);
