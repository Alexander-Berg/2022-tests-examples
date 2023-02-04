-- *----*-----*    *----*-----*   *----*-----*   *----*-----*   *----*-----*
-- |####|     |    |    |#####|   |    |     |   |    |     |   |##########|
-- |####*--*  |    |    *--*##|   |    *--*  |   |    *--*  |   |####*--*##|
-- |####|  |  |    |    |##|##|   |    |##|  |   |    |  |  |   |####|  |##|
-- |####|  |  |  + |    |##|##| - |    |##|  | - |    |  |  | = |####|  |##|
-- |####*--*  |    |    *--*##|   |    *--*  |   |    *--*  |   |####*  *##|
-- |####|  |  |    |    |##|##|   |    |  |  |   |    |##|  |   |####|  |##|
-- |####|  |  |    |    |##|##|   |    |  |  |   |    |##|  |   |####|  |##|
-- *----*--*--*    *----*--*--*   *----*--*--*   *----*--*--*   *----*  *--*

insert into {node} (node_id, shape) values
(301, Spatial::GeomFromText('POINT (0 0)', 4326)),
(302, Spatial::GeomFromText('POINT (1 0)', 4326)),
(303, Spatial::GeomFromText('POINT (2 0)', 4326)),
(304, Spatial::GeomFromText('POINT (3 0)', 4326)),
(305, Spatial::GeomFromText('POINT (1 1)', 4326)),
(306, Spatial::GeomFromText('POINT (2 1)', 4326)),
(307, Spatial::GeomFromText('POINT (1 2)', 4326)),
(308, Spatial::GeomFromText('POINT (2 2)', 4326)),
(309, Spatial::GeomFromText('POINT (0 3)', 4326)),
(310, Spatial::GeomFromText('POINT (1 3)', 4326)),
(311, Spatial::GeomFromText('POINT (3 3)', 4326));

insert into {edge} (edge_id, f_node_id, t_node_id, f_zlev, t_zlev, shape) values
(301, 301, 302, 0, 0, Spatial::GeomFromText('LINESTRING (0 0, 1 0)', 4326)),
(302, 302, 303, 0, 0, Spatial::GeomFromText('LINESTRING (1 0, 2 0)', 4326)),
(303, 303, 304, 0, 0, Spatial::GeomFromText('LINESTRING (2 0, 3 0)', 4326)),
(304, 301, 309, 0, 0, Spatial::GeomFromText('LINESTRING (0 0, 0 3)', 4326)),
(305, 302, 305, 0, 0, Spatial::GeomFromText('LINESTRING (1 0, 1 1)', 4326)),
(306, 303, 306, 0, 0, Spatial::GeomFromText('LINESTRING (2 0, 2 1)', 4326)),
(307, 304, 311, 0, 0, Spatial::GeomFromText('LINESTRING (3 0, 3 3)', 4326)),
(308, 305, 306, 0, 0, Spatial::GeomFromText('LINESTRING (1 1, 2 1)', 4326)),
(309, 305, 307, 0, 0, Spatial::GeomFromText('LINESTRING (1 1, 1 2)', 4326)),
(310, 306, 308, 0, 0, Spatial::GeomFromText('LINESTRING (2 1, 2 2)', 4326)),
(311, 307, 308, 0, 0, Spatial::GeomFromText('LINESTRING (1 2, 2 2)', 4326)),
(312, 307, 310, 0, 0, Spatial::GeomFromText('LINESTRING (1 2, 1 3)', 4326)),
(313, 309, 310, 0, 0, Spatial::GeomFromText('LINESTRING (0 3, 1 3)', 4326)),
(314, 310, 311, 0, 0, Spatial::GeomFromText('LINESTRING (1 3, 3 3)', 4326));

insert into {face} (face_id) values
(301), (302), (303), (304);

insert into {face_edge} (face_id, edge_id) values
(301, 301), (301, 304), (301, 305), (301, 309), (301, 312), (301, 313),
(302, 302), (302, 303), (302, 305), (302, 307), (302, 309), (302, 312), (302, 314),
(303, 302), (303, 305), (303, 306), (303, 308),
(304, 308), (304, 309), (304, 310), (304, 311);

insert into {ad} (ad_id, p_ad_id, level_kind, disp_class, isocode, g_ad_id) values
(301, NULL, 1, 5, 'AA', NULL),
(302, NULL, 1, 5, 'AA', 301);

insert into {ad_face} (ad_id, face_id, is_interior) values
(301, 301, false);

insert into {ad_face_patch} (ad_id, face_id, is_excluded) values
(302, 302, false),
(302, 303, true),
(302, 304, true);
