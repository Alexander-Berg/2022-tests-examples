INSERT INTO {hydro_a_sub_tmp_out} (ft_id, ft_type_id, isocode, disp_class, geom) VALUES

(2, 501, 'RU', 5, Spatial::Transform(Spatial::GeomFromText(
  'POLYGON((27 55, 28 55, 28 56, 27 56, 27 55))', 4326), 3395)),
(3, 558, 'RU', 5, Spatial::Transform(Spatial::GeomFromText(
  'POLYGON((25 25, 25 35, 35 35, 35 25, 25 25),
           (26 26, 26 27, 27 27, 27 26, 26 26),
           (30 30, 30 31, 31 31, 31 30, 30 30),
           (32 32, 32 33, 33 33, 33 32, 32 32))', 4326), 3395)),
(6, 553, 'RU', 5, Spatial::Transform(Spatial::GeomFromText(
  'POLYGON((20 20, 20 21, 21 21, 21 20, 20 20))', 4326), 3395));
