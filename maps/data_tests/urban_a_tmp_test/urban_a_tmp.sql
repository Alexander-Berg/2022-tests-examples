INSERT INTO {urban_a_tmp_out}(ft_type_id, ft_type_name, disp_class, name, tags,
 geom_0_0, geom_1_1, geom_2_2, geom_3_3, geom_4_4, geom_5_5, geom_6_6, geom_7_7, geom_8_8, geom_9_9, geom_10_10, geom_11_11, geom_12_12, geom_13_14, geom_15_19) VALUES

(226, 'urban-leisure-beach', NULL, NULL, ['landscape', 'urban_area', 'beach'],
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 Spatial::Transform(Spatial::GeomFromText('POLYGON((0 0, 1 0, 1 1, 0 1, 0 0))', 4326), 3395)),

(222, 'urban-leisure', NULL, NULL, ['landscape', 'urban_area'],
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 Spatial::Transform(Spatial::GeomFromText('POLYGON((0 0, 1 0, 1 1, 0 1, 0 0))', 4326), 3395)),

(171, 'urban-edu', NULL, NULL, ['landscape', 'urban_area'],
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 Spatial::Transform(Spatial::GeomFromText('POLYGON((0 0, 1 0, 1 1, 0 1, 0 0))', 4326), 3395)),

(251, 'urban-roadnet-parking-lot', 6, NULL, ['landscape', 'urban_area'],
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 Spatial::Transform(Spatial::GeomFromText('POLYGON((0 0, 1 0, 1 1, 0 1, 0 0))', 4326), 3395))
