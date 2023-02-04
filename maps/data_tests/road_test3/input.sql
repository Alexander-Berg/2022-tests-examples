INSERT INTO {rd_jc} (rd_jc_id, shape) VALUES
(10, Spatial::GeomFromText('POINT(0 10)', 4326)),
(11, Spatial::GeomFromText('POINT(0 11)', 4326)),
(12, Spatial::GeomFromText('POINT(0 12)', 4326)),
(13, Spatial::GeomFromText('POINT(0 13)', 4326));

INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, oneway, isocode, shape,
  fow, speed_cat, speed_limit, f_zlev, t_zlev, access_id, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk, struct_type,
  ferry, dr, toll, srv_ra, srv_uc, subcode, residential, restricted_for_trucks) VALUES
(21,10,11,1,'B','RU',Spatial::GeomFromText('LINESTRING(0 10, 0 11)', 4326),0,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(22,11,12,1,'B','RU',Spatial::GeomFromText('LINESTRING(0 11, 0 12)', 4326),0,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(23,12,13,1,'B','RU',Spatial::GeomFromText('LINESTRING(0 12, 0 13)', 4326),0,64,null,0,0,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0);

INSERT INTO {rd_el_extended_tmp} (rd_el_id, type, subtype, struct_type, min_geometry_zoom, min_label_zoom, geom) VALUES
(21, '1', '', 'none', 0, 0, NULL),
(22, '1_link', '', 'none', 0, 0, NULL),
(23, '1', '', 'none', 0, 0, NULL);
