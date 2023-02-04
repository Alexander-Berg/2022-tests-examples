INSERT INTO {rd_jc} (rd_jc_id, shape) VALUES
(1, Spatial::GeomFromText('POINT(0 1)', 4326)),
(2, Spatial::GeomFromText('POINT(0 2)', 4326)),
(3, Spatial::GeomFromText('POINT(0 3)', 4326)),
(4, Spatial::GeomFromText('POINT(0 4)', 4326)),
(5, Spatial::GeomFromText('POINT(0 5)', 4326)),
(6, Spatial::GeomFromText('POINT(0 6)', 4326)),
(7, Spatial::GeomFromText('POINT(0 7)', 4326)),
(8, Spatial::GeomFromText('POINT(0 8)', 4326)),
(9, Spatial::GeomFromText('POINT(0 9)', 4326));

-- merge two-way roads
INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, oneway, isocode, shape,
  fow, speed_cat, speed_limit, f_zlev, t_zlev, access_id, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk, struct_type,
  ferry, dr, toll, srv_ra, srv_uc, subcode, residential, restricted_for_trucks) VALUES
(11,1,2,1,'B','RU',Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326),0,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(12,2,3,1,'B','RU',Spatial::GeomFromText('LINESTRING(0 2, 0 3)', 4326),0,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0);

-- merge one-way roads
INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, oneway, isocode, shape,
  fow, speed_cat, speed_limit, f_zlev, t_zlev, access_id, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk, struct_type,
  ferry, dr, toll, srv_ra, srv_uc, subcode, residential, restricted_for_trucks) VALUES
(21,3,4,1,'T','RU',Spatial::GeomFromText('LINESTRING(0 3, 0 4)', 4326),0,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(22,4,5,1,'T','RU',Spatial::GeomFromText('LINESTRING(0 4, 0 5)', 4326),0,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(23,6,5,1,'F','RU',Spatial::GeomFromText('LINESTRING(0 6, 0 5)', 4326),0,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0);

-- do not merge toll road with a free road
INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, oneway, isocode, shape, toll,
  fow, speed_cat, speed_limit, f_zlev, t_zlev, access_id, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk, struct_type,
  ferry, dr, srv_ra, srv_uc, subcode, residential, restricted_for_trucks) VALUES
(31,7,8,2,'B','RU',Spatial::GeomFromText('LINESTRING(0 7, 0 8)', 4326),0,0,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,null,0,0),
(32,8,9,2,'B','RU',Spatial::GeomFromText('LINESTRING(0 8, 0 9)', 4326),1,0,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,null,0,0);

INSERT INTO {rd_el_extended_tmp} (rd_el_id, type, subtype, struct_type, min_geometry_zoom, min_label_zoom, geom) VALUES
(11, '1', '', 'none', 0, 0, NULL),
(12, '1', '', 'none', 0, 0, NULL),
(21, '1', '', 'none', 0, 0, NULL),
(22, '1', '', 'none', 0, 0, NULL),
(23, '1', '', 'none', 0, 0, NULL),
(31, '2', '', 'none', 0, 0, NULL),
(32, '2', '', 'none', 0, 0, NULL);
