INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, oneway, isocode, shape,
  fow, speed_cat, speed_limit, f_zlev, t_zlev, access_id, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk, struct_type,
  ferry, dr, toll, srv_ra, srv_uc, subcode, residential, restricted_for_trucks) VALUES
(11,1,1,1,'B','RU',Spatial::GeomFromText('LINESTRING(0 0, 0 3)', 4326),2,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(12,1,1,1,'B','RU',Spatial::GeomFromText('LINESTRING(0 0, 0 3)', 4326),2,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(13,1,1,1,'B','RU',Spatial::GeomFromText('LINESTRING(0 0, 0 3)', 4326),2,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(14,1,1,1,'B','RU',Spatial::GeomFromText('LINESTRING(0 0, 0 3)', 4326),2,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(15,1,1,1,'B','RU',Spatial::GeomFromText('LINESTRING(0 0, 0 3)', 4326),2,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(16,1,1,1,'B','RU',Spatial::GeomFromText('LINESTRING(0 0, 0 3)', 4326),2,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(17,1,1,1,'B','RU',Spatial::GeomFromText('LINESTRING(0 0, 0 3)', 4326),2,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(18,1,1,1,'B','RU',Spatial::GeomFromText('LINESTRING(0 0, 0 3)', 4326),2,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(19,1,1,1,'B','RU',Spatial::GeomFromText('LINESTRING(0 0, 0 3)', 4326),2,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(20,1,1,1,'B','RU',Spatial::GeomFromText('LINESTRING(0 0, 0 3)', 4326),2,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0);

INSERT INTO {rd_el_extended_tmp} (rd_el_id, type, subtype, struct_type, min_geometry_zoom, min_label_zoom, geom) VALUES
(11,         'ferry',             '',   'none', 0, 0, NULL),
(12, '10_pedestrian',             '',   'none', 0, 0, NULL),
(13, '10_pedestrian', '7_pedestrian',   'none', 0, 0, NULL),
(14,             '7',             '',   'none', 0, 0, NULL),
(15,  '7_pedestrian',             '',   'none', 0, 0, NULL),
(16,        '1_link',             '',   'none', 0, 0, NULL),
(17,             '1',             '',   'none', 0, 0, NULL),
(18,             '1',             '', 'bridge', 0, 0, NULL),
(19,             '1',             '', 'tunnel', 0, 0, NULL),
(20,       '10_bike',             '',   'none', 0, 0, NULL);
