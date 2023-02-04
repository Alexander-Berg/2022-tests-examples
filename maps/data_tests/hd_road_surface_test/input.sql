INSERT INTO {ft} (ft_id, ft_type_id, isocode, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi) VALUES
(1, 10010, 'RU', 0, 0, 0, 0),
(2, 10020, 'RU', 0, 0, 0, 0),
(3, 20010, 'RU', 0, 0, 0, 0)
;

INSERT INTO {ft_hd_attr} (ft_id, min_zlev, max_zlev, yaw) VALUES
(1, 0, 1, 0)
;

INSERT INTO {ft_geom} (ft_id, shape) VALUES
(1, Spatial::MakeEnvelope(0.01, 0.01, 0.02, 0.02, 4326)),
(2, Spatial::MakeEnvelope(0.02, 0.02, 0.03, 0.03, 4326)),
(3, Spatial::MakeEnvelope(0.03, 0.03, 0.04, 0.04, 4326))
;
