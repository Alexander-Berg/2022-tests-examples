INSERT INTO {ft} (ft_id, ft_type_id, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi) VALUES
(1, 257, 5, 0.0, 5, 0.0),
(2, 257, 5, 0.0, 5, 0.0),
(3, 257, 5, 0.0, 5, 0.0),
(4, 257, 5, 0.0, 5, 0.0);

INSERT INTO {ft_geom} (ft_id, shape) VALUES
(1, Spatial::Transform(Spatial::GeomFromText('LINESTRING (100 100, 100 200)', 3395), 4326)),
(2, Spatial::Transform(Spatial::GeomFromText('LINESTRING (300 300, 300 400)', 3395), 4326)),
(3, Spatial::Transform(Spatial::GeomFromText('LINESTRING (450 450, 450 470)', 3395), 4326)),
(4, Spatial::Transform(Spatial::GeomFromText('LINESTRING (500 500, 500 501)', 3395), 4326)); -- will be removed

INSERT INTO {roadnet_p} (ft_id, ft_type_id, ft_type_name, tags, geom) VALUES
(11, 258, 'urban-roadnet-gate-car', [], Spatial::GeomFromText('POINT (100 150)', 3395)),
(12, 258, 'urban-roadnet-gate-car', [], Spatial::GeomFromText('POINT (301 350)', 3395)),
(13, 259, 'urban-roadnet-gate-pedestrian', [], Spatial::GeomFromText('POINT (450 460)', 3395)),
(14, 258, 'urban-roadnet-gate-car', [], Spatial::GeomFromText('POINT (500 500.5)', 3395));
