INSERT INTO {ft} (ft_id, ft_type_id, isocode, p_ft_id, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi) VALUES
(1, 30110, 'RU', 2, 0, 0, 0, 0)
;

INSERT INTO {ft_hd_attr} (ft_id, min_zlev, max_zlev, yaw) VALUES
(2, 0, 1, 0)
;

INSERT INTO {ft_geom} (ft_id, shape) VALUES
(1, Spatial::GeomFromText('LINESTRING(0 1, 0 2)', 4326))
;
