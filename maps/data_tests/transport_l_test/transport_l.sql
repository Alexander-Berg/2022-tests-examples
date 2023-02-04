INSERT INTO {transport_l_out} (ft_id, ft_type_name, disp_class, f_zlev, t_zlev, tags,
 geom_0_0, geom_1_1, geom_2_2, geom_3_3, geom_4_4, geom_5_5, geom_6_6, geom_7_7, geom_8_8, geom_9_9, geom_10_10, geom_11_11, geom_12_12, geom_13_14, geom_15_19) VALUES

(0, 'transport-railway', 6, 0, 0, ['transit', 'transit_line'],
  null, null, null, null, null, null, null, null, null, null, null, null, null, null,
  Spatial::Transform(Spatial::GeomFromText('LINESTRING (0 0, 0 1)', 4326), 3395)),
(1, 'transport-railway', 6, 0, 0, ['transit', 'transit_line'],
  null, null, null, null, null, null, null, null, null, null, null, null, null, null,
  Spatial::Transform(Spatial::GeomFromText('LINESTRING (0 1, 1 1)', 4326), 3395)),
(4, 'transport-railway-siding', 6, 1, 0, ['transit', 'transit_line'],
  null, null, null, null, null, null, null, null, null, null, null, null, null, null,
  Spatial::Transform(Spatial::GeomFromText('LINESTRING (1 1, 2 1)', 4326), 3395)),
(7, 'transport-metro-tram-line', 5, 0, 0, ['transit', 'transit_line'],
  null, null, null, null, null, null, null, null, null, null, null, null, null, null,
  Spatial::Transform(Spatial::GeomFromText('LINESTRING (1 0, 0 0)', 4326), 3395))
