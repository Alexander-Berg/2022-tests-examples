INSERT INTO {bld} (bld_id, ft_type_id, cond, isocode, height) VALUES
(1, 101, 0, 'RU', 5),
(2, 101, 0, 'RU', 5),
(3, 101, 0, 'RU', 5),
(4, 101, 0, 'RU', 5),
(5, 101, 0, 'RU', 5),
(6, 101, 0, 'RU', 5),
(7, 101, 0, 'RU', 5),
(8, 101, 0, 'RU', 5)
;


INSERT INTO {bld_geom} (bld_id, shape) VALUES
(1, Spatial::Transform(Spatial::MakeEnvelope(2, 2, 4, 4, 3395), 4326)),
(2, Spatial::Transform(Spatial::MakeEnvelope(9, 9, 11, 11, 3395), 4326)),
(3, Spatial::Transform(Spatial::MakeEnvelope(12, 12, 13, 13, 3395), 4326)),
(4, Spatial::Transform(Spatial::MakeEnvelope(15, 15, 17, 17, 3395), 4326)),
(5, Spatial::Transform(Spatial::MakeEnvelope(20, 20, 21, 21, 3395), 4326)),
(6, Spatial::Transform(Spatial::MakeEnvelope(22, 22, 25, 25, 3395), 4326)),
(7, Spatial::Transform(Spatial::MakeEnvelope(28, 28, 30, 30, 3395), 4326)),
(8, Spatial::Transform(Spatial::MakeEnvelope(20, 25, 30, 35, 3395), 4326))
;


INSERT INTO {indoor_plan_data} (ft_id, indoor_plan_id, geom)
VALUES
(11, '1', Spatial::MakeEnvelope(0, 0, 10, 10, 3395)),
(12, '2', Spatial::MakeEnvelope(20, 20, 30, 30, 3395)),
(12, '2', Spatial::MakeEnvelope(20, 30, 30, 40, 3395))
;
