-- *----*----*     *----*----*     *----*----*
-- |####|    |     |    |####|     |#########|
-- |####|    |     |    |####|     |#########|
-- |####|    |     |    |####|     |#########|
-- |####|    |  +  |    |####|  =  |#########|
-- |####|    |     |    |####|     |#########|
-- |####|    |     |    |####|     |#########|
-- |####|    |     |    |####|     |#########|
-- *----*----*     *----*----*     *----*----*

insert into {node} (node_id, shape) values
(1, Spatial::GeomFromText('POINT (0 0)', 4326)),
(2, Spatial::GeomFromText('POINT (1 0)', 4326)),
(3, Spatial::GeomFromText('POINT (2 0)', 4326)),
(4, Spatial::GeomFromText('POINT (0 1)', 4326)),
(5, Spatial::GeomFromText('POINT (1 1)', 4326)),
(6, Spatial::GeomFromText('POINT (2 1)', 4326));

insert into {edge} (edge_id, f_node_id, t_node_id, f_zlev, t_zlev, shape) values
(1, 1, 2, 0, 0, Spatial::GeomFromText('LINESTRING (0 0, 1 0)', 4326)),
(2, 2, 3, 0, 0, Spatial::GeomFromText('LINESTRING (1 0, 2 0)', 4326)),
(3, 1, 4, 0, 0, Spatial::GeomFromText('LINESTRING (0 0, 0 1)', 4326)),
(4, 2, 5, 0, 0, Spatial::GeomFromText('LINESTRING (1 0, 1 1)', 4326)),
(5, 3, 6, 0, 0, Spatial::GeomFromText('LINESTRING (2 0, 2 1)', 4326)),
(6, 4, 5, 0, 0, Spatial::GeomFromText('LINESTRING (0 1, 1 1)', 4326)),
(7, 5, 6, 0, 0, Spatial::GeomFromText('LINESTRING (1 1, 2 1)', 4326));

insert into {face} (face_id) values
(1), (2);

insert into {face_edge} (face_id, edge_id) values
(1, 1), (1, 3), (1, 4), (1, 6),
(2, 2), (2, 4), (2, 5), (2, 7);

insert into {ad} (ad_id, p_ad_id, level_kind, disp_class, isocode, g_ad_id) values
(1, NULL, 1, 5, 'AA', NULL);

insert into {ad_face} (ad_id, face_id, is_interior) values
(1, 1, false),
(1, 2, false);
