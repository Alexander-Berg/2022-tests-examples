INSERT INTO {hd_road_marking_l} (ft_id, ft_type_id, ft_type_name, f_zlev, t_zlev, min_zlev, max_zlev, geom, tags, width, dash, color, side, zmin, zmax, experiment_id) VALUES
(1, 30110, 'hdmap-road-marking-linear-lane-11', 1, 1, 1, 1, Spatial::Transform(Spatial::GeomFromText('LINESTRING(-0.0000013474729262 1, -0.0000013474729262 2)', 4326), 3395), ['road'], "15", "90_30", "white", "left", 0, 21, 'hd_closed_beta'),
(1, 30110, 'hdmap-road-marking-linear-lane-11', 1, 1, 1, 1, Spatial::Transform(Spatial::GeomFromText('LINESTRING(0.0000013474729262 1, 0.0000013474729262 2)', 4326), 3395), ['road'], "15", null, "white", "right", 0, 21, 'hd_closed_beta')
;
