INSERT INTO {ft} (ft_id, p_ft_id, ft_type_id, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi) VALUES
(0, NULL, 2901, 6, 0, 5, 0),
(1, NULL, 2902, 6, 0, 5, 0);

INSERT INTO {ft_rd_el} (ft_id, rd_el_id) VALUES
(0, 1),
(0, 2),
(0, 3),
(1, 4),
(1, 2), -- common piece for both tracks
(1, 5);

INSERT INTO {ft_nm_tmp} (ft_id, name) VALUES
(0,  json('{"ru_RU_LOCAL":"Трасса А"}')),
(1,  json('{"ru_RU_LOCAL":"Трасса Б"}'));

INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, isocode, shape,
  fow, speed_cat, speed_limit, f_zlev, t_zlev, oneway, access_id, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk, struct_type,
  ferry, dr, toll, srv_ra, srv_uc, subcode, residential, restricted_for_trucks) VALUES
(1, 1, 2, 1, 'RU', Spatial::GeomFromText('LINESTRING(0 0, 2 2)', 4326), 0,0,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(2, 1, 2, 1, 'RU', Spatial::GeomFromText('LINESTRING(2 2, 4 2)', 4326), 0,0,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(3, 1, 2, 1, 'RU', Spatial::GeomFromText('LINESTRING(4 2, 8 2)', 4326), 0,0,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(4, 1, 2, 1, 'RU', Spatial::GeomFromText('LINESTRING(0 2, 2 2)', 4326), 0,0,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(5, 1, 2, 1, 'RU', Spatial::GeomFromText('LINESTRING(2 4, 2 8)', 4326), 0,0,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0);
