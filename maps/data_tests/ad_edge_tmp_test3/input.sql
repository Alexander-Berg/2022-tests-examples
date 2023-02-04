-- *------*------*     *------*------*     *------*------*     *------*------*
-- |######|      |     |      |      |     |      |######|     |#############|
-- |######|      |     |      |      |     |      |######|     |#############|
-- |######|   *--*     |      |   *--*     |      |######|     |#############*
-- |######|   |##|  -  |      |   |##|  +  |      |######|  =  |#############|
-- |######|   |##|     |      |   |##|     |      |######|     |#############|
-- |######|   *--*     |      |   *--*     |      |######|     |#############*
-- |######|      |     |      |      |     |      |######|     |#############|
-- |######|      |     |      |      |     |      |######|     |#############|
-- *------*------*     *------*------*     *------*------*     *------*------*

insert into {node} (node_id, shape) values
(201, Spatial::GeomFromText('POINT (0 0)', 4326)),
(202, Spatial::GeomFromText('POINT (1 0)', 4326)),
(203, Spatial::GeomFromText('POINT (3 0)', 4326)),
(204, Spatial::GeomFromText('POINT (0 3)', 4326)),
(205, Spatial::GeomFromText('POINT (1 3)', 4326)),
(206, Spatial::GeomFromText('POINT (3 3)', 4326)),
(207, Spatial::GeomFromText('POINT (2 1)', 4326)),
(208, Spatial::GeomFromText('POINT (3 1)', 4326)),
(209, Spatial::GeomFromText('POINT (2 2)', 4326)),
(210, Spatial::GeomFromText('POINT (3 2)', 4326));

insert into {edge} (edge_id, f_node_id, t_node_id, f_zlev, t_zlev, shape) values
(201, 201, 202, 0, 0, Spatial::GeomFromText('LINESTRING (0 0, 1 0)', 4326)),
(202, 202, 203, 0, 0, Spatial::GeomFromText('LINESTRING (1 0, 3 0)', 4326)),
(203, 204, 205, 0, 0, Spatial::GeomFromText('LINESTRING (0 3, 1 3)', 4326)),
(204, 205, 206, 0, 0, Spatial::GeomFromText('LINESTRING (1 3, 3 3)', 4326)),
(205, 201, 204, 0, 0, Spatial::GeomFromText('LINESTRING (0 0, 0 3)', 4326)),
(206, 202, 205, 0, 0, Spatial::GeomFromText('LINESTRING (1 0, 1 3)', 4326)),
(207, 203, 208, 0, 0, Spatial::GeomFromText('LINESTRING (3 0, 3 1)', 4326)),
(208, 207, 208, 0, 0, Spatial::GeomFromText('LINESTRING (2 1, 3 1)', 4326)),
(209, 207, 209, 0, 0, Spatial::GeomFromText('LINESTRING (2 1, 2 2)', 4326)),
(210, 208, 210, 0, 0, Spatial::GeomFromText('LINESTRING (3 1, 3 2)', 4326)),
(211, 209, 210, 0, 0, Spatial::GeomFromText('LINESTRING (2 2, 3 2)', 4326)),
(212, 210, 206, 0, 0, Spatial::GeomFromText('LINESTRING (3 2, 3 3)', 4326));

insert into {face} (face_id) values
(201), (202), (203);

insert into {face_edge} (face_id, edge_id) values
(201, 201), (201, 205), (201, 206), (201, 203),
(202, 208), (202, 209), (202, 210), (202, 211),
(203, 202), (203, 206), (203, 207), (203, 210), (203, 212), (203, 204);

insert into {ad} (ad_id, p_ad_id, level_kind, disp_class, isocode, g_ad_id) values
(201, NULL, 1, 5, 'AA', NULL),
(202, NULL, 1, 5, 'AA', 201),
(203, NULL, 1, 5, 'BB', NULL),
(204, NULL, 1, 5, 'BB', 203);

insert into {ad_face} (ad_id, face_id, is_interior) values
(201, 201, false),
(201, 202, false),
(203, 203, false),
(203, 202, true);

insert into {ad_face_patch} (ad_id, face_id, is_excluded) values
(202, 202, true),
(202, 203, false),
(204, 202, false),
(204, 203, true);
