INSERT INTO {fence_l_gates_tmp} (ft_id, geom) VALUES
(1, Spatial::GeomFromText('MULTILINESTRING ((100 100, 100 148.75), (100 151.25, 100 200))', 3395)),
(2, Spatial::GeomFromText('MULTILINESTRING ((300 300, 300 348.75), (300 351.25, 300 400))', 3395)),
(3, Spatial::GeomFromText('MULTILINESTRING ((450 450, 450 459.25), (450 460.75, 450 470))', 3395));
