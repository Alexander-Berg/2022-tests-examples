INSERT INTO {rd_jc} (rd_jc_id, shape) VALUES
(0, Spatial::GeomFromText('POINT(0 1)', 4326)),
(1, Spatial::GeomFromText('POINT(0 1)', 4326)),
(2, Spatial::GeomFromText('POINT(0 2)', 4326)),
(3, Spatial::GeomFromText('POINT(0 3)', 4326)),
(4, Spatial::GeomFromText('POINT(0 4)', 4326)),
(5, Spatial::GeomFromText('POINT(0 5)', 4326)),
(6, Spatial::GeomFromText('POINT(0 6)', 4326));

INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, oneway, isocode, shape, f_zlev, t_zlev,
  fow, speed_cat, speed_limit, access_id, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk, struct_type,
  ferry, dr, toll, srv_ra, srv_uc, subcode, residential, restricted_for_trucks) VALUES
(11,0,1,1,'T','RU',Spatial::GeomFromText('LINESTRING(0 0, 0 1)', 4326),0,0,0,64,null,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(12,1,2,1,'T','RU',Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326),0,1,0,64,null,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(13,2,3,1,'T','RU',Spatial::GeomFromText('LINESTRING(0 2, 0 3)', 4326),1,1,0,64,null,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(14,4,3,1,'F','RU',Spatial::GeomFromText('LINESTRING(0 4, 0 3)', 4326),1,1,0,64,null,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(15,5,4,1,'F','RU',Spatial::GeomFromText('LINESTRING(0 5, 0 4)', 4326),2,1,0,64,null,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(16,6,5,1,'F','RU',Spatial::GeomFromText('LINESTRING(0 6, 0 5)', 4326),2,2,0,64,null,0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0);

INSERT INTO {rd_el_extended_tmp} (rd_el_id, type, subtype, struct_type, min_geometry_zoom, min_label_zoom, geom) VALUES
(11, '1', '', 'none', 0, 0, NULL),
(12, '1', '', 'none', 0, 0, NULL),
(13, '1', '', 'none', 0, 0, NULL),
(14, '1', '', 'none', 0, 0, NULL),
(15, '1', '', 'none', 0, 0, NULL),
(16, '1', '', 'none', 0, 0, NULL);
