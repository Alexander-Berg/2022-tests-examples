INSERT INTO {poi_geom_tmp} (ft_id, geom) VALUES
(1, Spatial::GeomFromText('POINT(0 0)', 3395)),
(2, Spatial::GeomFromText('POINT(5 5)', 3395)),
(3, Spatial::GeomFromText('POINT(10 10)', 3395)),
(4, Spatial::GeomFromText('POINT(12 12)', 3395)),
(5, Spatial::GeomFromText('POINT(18 18)', 3395)),
(6, Spatial::GeomFromText('POINT(22 20)', 3395)),
(7, Spatial::GeomFromText('POINT(25 25)', 3395)),
(8, Spatial::GeomFromText('POINT(30 31)', 3395))
;


INSERT INTO {indoor_plan_data} (ft_id, indoor_plan_id, geom)
VALUES
(11, '1', Spatial::MakeEnvelope(0, 0, 10, 10, 3395)),
(12, '2', Spatial::MakeEnvelope(20, 20, 30, 30, 3395))
;
