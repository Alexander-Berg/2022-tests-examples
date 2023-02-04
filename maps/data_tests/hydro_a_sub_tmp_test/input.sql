INSERT INTO {hydro_a_sub_query_hydro_tmp} (ft_id, ft_type_id, isocode, disp_class, geom) VALUES
(2, 501, 'RU', 5, Spatial::Transform(Spatial::GeomFromText('POLYGON((27 55, 28 55, 28 56, 27 56, 27 55))', 4326), 3395)),
-- Square-shaped river without any holes in the geometry
(3, 558, 'RU', 5, Spatial::Transform(Spatial::GeomFromText('POLYGON((25 25, 25 35, 35 35, 35 25, 25 25))', 4326), 3395)),
-- Another river, expecting it to remain unchanged
(6, 553, 'RU', 5, Spatial::Transform(Spatial::GeomFromText('POLYGON((20 20, 20 21, 21 21, 21 20, 20 20))', 4326), 3395));

-- Island and archipelago objects, both located inside the river
-- Expecting both objects to be subtracted from the ft_id=3
INSERT INTO {hydro_a_sub_query_islands_tmp} (geom) VALUES
(Spatial::Transform(Spatial::GeomFromText('POLYGON ((26 26, 26 27, 27 27, 27 26, 26 26))', 4326), 3395)),
(Spatial::Transform(Spatial::GeomFromText('MULTIPOLYGON (((30 30, 30 31, 31 31, 31 30, 30 30)), ((32 32, 32 33, 33 33, 33 32, 32 32)))', 4326), 3395));
