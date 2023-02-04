INSERT INTO {hd_road_surface} (ft_id, ft_type_id, ft_type_name, tags, f_zlev, t_zlev, min_zlev, max_zlev, geom, experiment_id) VALUES
(1, 10010, 'hdmap-road-surface-road', ['road'], 0, 1, 0, 1, Spatial::Transform(Spatial::MakeEnvelope(0.01, 0.01, 0.02, 0.02, 4326), 3395), 'hd_closed_beta'),
(2, 10020, 'hdmap-road-surface-bridge', ['road'], 0, 0, 0, 0, Spatial::Transform(Spatial::MakeEnvelope(0.02, 0.02, 0.03, 0.03, 4326), 3395), 'hd_closed_beta')
;
