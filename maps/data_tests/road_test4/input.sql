--  ^
--  |
-- 2|     *---*        <- fow=11
--  |    /     \       <- fow=10
-- 1| *-*---*---*-*    <- fow=2
--  |       |          <- fow=17
-- 0| *-----*-----*    <- fow=2
--  o--------------------------->
--    0 1 2 3 4 5 6

INSERT INTO {rd_jc} (rd_jc_id, shape) VALUES
(1, Spatial::GeomFromText('POINT(0 0)', 4326)),
(2, Spatial::GeomFromText('POINT(0 3)', 4326)),
(3, Spatial::GeomFromText('POINT(0 6)', 4326)),
(4, Spatial::GeomFromText('POINT(1 0)', 4326)),
(5, Spatial::GeomFromText('POINT(1 1)', 4326)),
(6, Spatial::GeomFromText('POINT(1 3)', 4326)),
(7, Spatial::GeomFromText('POINT(1 5)', 4326)),
(8, Spatial::GeomFromText('POINT(1 6)', 4326)),
(9, Spatial::GeomFromText('POINT(2 2)', 4326)),
(10, Spatial::GeomFromText('POINT(2 4)', 4326));

INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, oneway, isocode, shape,
  fow, speed_cat, speed_limit, f_zlev, t_zlev, access_id, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk, struct_type,
  ferry, dr, toll, srv_ra, srv_uc, subcode, residential, restricted_for_trucks) VALUES
-- fow=2 (two-lane road)
(11,1, 2,1,'B','RU',Spatial::GeomFromText('LINESTRING(0 0, 0 3)', 4326), 2,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(12,2, 3,1,'B','RU',Spatial::GeomFromText('LINESTRING(0 3, 0 6)', 4326), 2,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
-- fow=17 (u-turn)
(21,2, 6,1,'B','RU',Spatial::GeomFromText('LINESTRING(0 3, 1 3)', 4326),17,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
-- fow=2 (two-lane road)
(31,4, 5,1,'B','RU',Spatial::GeomFromText('LINESTRING(1 0, 1 1)', 4326), 2,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(32,5, 6,1,'B','RU',Spatial::GeomFromText('LINESTRING(1 1, 1 3)', 4326), 2,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(33,6, 7,1,'B','RU',Spatial::GeomFromText('LINESTRING(1 3, 1 5)', 4326), 2,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(34,7, 8,1,'B','RU',Spatial::GeomFromText('LINESTRING(1 5, 1 6)', 4326), 2,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
-- fow=10 (link)
(41,5, 9,1,'B','RU',Spatial::GeomFromText('LINESTRING(1 1, 2 2)', 4326),10,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(42,7,10,1,'B','RU',Spatial::GeomFromText('LINESTRING(1 5, 2 4)', 4326),10,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
-- fow=11 (doubler)
(51,9,10,1,'B','RU',Spatial::GeomFromText('LINESTRING(2 2, 2 4)', 4326),11,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0);

INSERT INTO {rd_el_extended_tmp} (rd_el_id, type, subtype, struct_type, min_geometry_zoom, min_label_zoom, geom) VALUES
(11, '1', '', 'none', 0, 0, NULL),
(12, '1', '', 'none', 0, 0, NULL),
(21, '1', '', 'none', 0, 0, NULL),
(31, '1', '', 'none', 0, 0, NULL),
(32, '1', '', 'none', 0, 0, NULL),
(33, '1', '', 'none', 0, 0, NULL),
(34, '1', '', 'none', 0, 0, NULL),
(41, '1', '', 'none', 0, 0, NULL),
(42, '1', '', 'none', 0, 0, NULL),
(51, '1', '', 'none', 0, 0, NULL);
