INSERT INTO {rd_jc} (rd_jc_id, shape) VALUES
(1, Spatial::GeomFromText('POINT(0 1)', 4326)),
(2, Spatial::GeomFromText('POINT(0 2)', 4326)),
(3, Spatial::GeomFromText('POINT(0 3)', 4326));

INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, isocode, shape,
  fow, speed_cat, speed_limit, f_zlev, t_zlev, oneway, access_id, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk, struct_type,
  ferry, dr, toll, srv_ra, srv_uc, subcode, residential, restricted_for_trucks) VALUES
(1, 1, 2, 1, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(2, 1, 2, 2, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(3, 1, 2, 3, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(4, 1, 2, 4, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(5, 1, 2, 5, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(6, 1, 2, 6, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(7, 1, 2, 7, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(8, 1, 2, 8, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(9, 1, 2, 9, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0);

-- ferry
INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, ferry, isocode, shape,
  fow, speed_cat, speed_limit, f_zlev, t_zlev, oneway, access_id, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk, struct_type,
  dr, toll, srv_ra, srv_uc, subcode, residential, restricted_for_trucks) VALUES
(10, 1, 2, 1, 1, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,null,0,0);

-- construction
INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, srv_uc, isocode, shape,
  fow, speed_cat, speed_limit, f_zlev, t_zlev, oneway, access_id, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk, struct_type,
  ferry, dr, toll, srv_ra, subcode, residential, restricted_for_trucks) VALUES
(11, 1, 2, 1, 1, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,null,0,0);

-- bike
INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, access_id, isocode, shape,
  fow, speed_cat, speed_limit, f_zlev, t_zlev, oneway, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk, struct_type,
  ferry, dr, toll, srv_ra, srv_uc, subcode, residential, restricted_for_trucks) VALUES
(13, 1, 2, 10, 16, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0);

-- 7_pedestrian
INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, access_id, isocode, shape,
  fow, speed_cat, speed_limit, f_zlev, t_zlev, oneway, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk, struct_type,
  ferry, dr, toll, srv_ra, srv_uc, subcode, residential, restricted_for_trucks) VALUES
(14, 1, 2, 7, 1, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0);

-- 10_pedestrian
INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, access_id, isocode, shape,
  fow, speed_cat, speed_limit, f_zlev, t_zlev, oneway, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk, struct_type,
  ferry, dr, toll, srv_ra, srv_uc, subcode, residential, restricted_for_trucks) VALUES
(15, 1, 2, 10, 1, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0);

-- 10_park + 10_pedestrian
INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, access_id, isocode, shape,
  fow, speed_cat, speed_limit, f_zlev, t_zlev, oneway, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk, struct_type,
  ferry, dr, toll, srv_ra, srv_uc, subcode, residential, restricted_for_trucks) VALUES
(16, 1, 3, 10, 1, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 3)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(17, 1, 3, 10, 1, 'RU', Spatial::GeomFromText('LINESTRING(42.7880 43.8870, 42.7881 43.8870)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0);

INSERT INTO {ft} (ft_id, ft_type_id,
  icon_class, isocode, p_ft_id, rubric_id, search_class, subcode,
  disp_class, disp_class_navi, disp_class_tweak, disp_class_tweak_navi) VALUES
(1, 402 /*vegetation-park*/, null, null, null, null, null, null, 5, 5, 0.0, 0.0);

INSERT INTO {ft_geom} (ft_id, shape) VALUES
(1, Spatial::GeomFromText('POLYGON((-1 2, 1 2, 1 4, -1 4, -1 2))', 4326));

-- struct
INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, struct_type, access_id, isocode, shape,
  fow, speed_cat, speed_limit, f_zlev, t_zlev, oneway, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk,
  ferry, dr, toll, srv_ra, srv_uc, subcode, residential, restricted_for_trucks) VALUES
(21, 1, 2, 10, 0, 1, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,'N',0,0,0,0,0,null,0,0),
(22, 1, 2, 10, 1, 1, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,'N',0,0,0,0,0,null,0,0),
(23, 1, 2, 10, 2, 1, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,'N',0,0,0,0,0,null,0,0);

-- stairs, crosswalk, underpath
INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, stairs, access_id, isocode, shape,
  fow, speed_cat, speed_limit, f_zlev, t_zlev, oneway, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, sidewalk, struct_type,
  ferry, dr, toll, srv_ra, srv_uc, subcode, residential, restricted_for_trucks) VALUES
(24, 1, 2, 10, 1, 1, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0);

INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, fow, f_zlev, t_zlev, struct_type, access_id, isocode, shape,
  speed_cat, speed_limit, oneway, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk,
  ferry, dr, toll, srv_ra, srv_uc, subcode, residential, restricted_for_trucks) VALUES
(25, 1, 2, 10, 18, 0,  0, 0, 1, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 64,null,'B',0,0,0,0,0,0,0,'N',0,0,0,0,0,null,0,0),
(26, 1, 2, 10, 18, 1, -1, 0, 1, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 64,null,'B',0,0,0,0,0,0,0,'N',0,0,0,0,0,null,0,0);

-- links
INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, fow, isocode, shape,
  speed_cat, speed_limit, f_zlev, t_zlev, oneway, access_id, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk, struct_type,
  ferry, dr, toll, srv_ra, srv_uc, subcode, residential, restricted_for_trucks) VALUES
(31, 1, 2, 1,  4, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 64,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(32, 1, 2, 2, 10, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 64,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(33, 1, 2, 3, 16, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 64,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(34, 1, 2, 4, 17, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 64,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(35, 1, 2, 5, 10, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 64,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(36, 1, 2, 6, 10, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 64,null,0,0,'B',0,0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0);

-- scooter
INSERT INTO {rd_el} (rd_el_id, f_rd_jc_id, t_rd_jc_id, fc, access_id, isocode, shape,
  fow, speed_cat, speed_limit, f_zlev, t_zlev, oneway, back_bus, forward_bus,
  back_taxi, forward_taxi, paved, poor_condition, stairs, sidewalk, struct_type,
  ferry, dr, toll, srv_ra, srv_uc, subcode, residential, restricted_for_trucks) VALUES
(37, 1, 2, 10, 64, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0),
(38, 1, 2, 10, 81, 'RU', Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326), /* default -> */ 0,64,null,0,0,'B',0,0,0,0,0,0,0,'N',0,0,0,0,0,0,null,0,0);
