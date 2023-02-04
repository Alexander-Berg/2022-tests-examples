INSERT INTO {hd_road_surface_border} (ft_id, ft_type_id, ft_type_name, tags, zlev, geom, experiment_id) VALUES
(1, 10010, 'hdmap-road-surface-road', ['road'], 0, Spatial::Transform(Spatial::GeomFromText('LINESTRING (0.01 0.01, 0.02 0.01, 0.02 0.02, 0.01 0.02, 0.01 0.01)', 4326), 3395), 'hd_closed_beta'),
(2, 10020, 'hdmap-road-surface-bridge', ['road'], 0, Spatial::Transform(Spatial::GeomFromText('LINESTRING (0.02 0.02, 0.03 0.02, 0.03 0.03, 0.02 0.03, 0.02 0.02)', 4326), 3395), 'hd_closed_beta')
;
