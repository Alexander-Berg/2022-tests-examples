INSERT INTO {poi_geom_tmp_out} (ft_id, geom, area) VALUES
(0, Spatial::Transform(Spatial::GeomFromText('POINT(0 0)', 4326), 3395), NULL),
(1, Spatial::Transform(Spatial::GeomFromText('POINT(1 1)', 4326), 3395), Spatial::Area(Spatial::Transform(Spatial::GeomFromText('POLYGON((0 0, 2 0, 2 2, 0 2, 0 0))', 4326), 3395))),
(3, Spatial::Transform(Spatial::GeomFromText('POINT(3 3)', 4326), 3395), Spatial::Area(Spatial::Transform(Spatial::GeomFromText('POLYGON((-1 -1, 1 -1, 1 1, -1 1, -1 -1))', 4326), 3395))),
(4, Spatial::Transform(Spatial::GeomFromText('POINT(4 4)', 4326), 3395), Spatial::Area(Spatial::Transform(Spatial::GeomFromText('POLYGON((4 4, 4 5, 5 5, 5 4, 4 4))', 4326), 3395)));
