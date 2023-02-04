INSERT INTO {indoor_boundary_tmp_out} (indoor_plan_id, indoor_level_id, geom) VALUES
('0', '0', Spatial::GeomFromText('LINESTRING (0 0, 0 10, 10 10, 10 0, 0 0)', 3395)),
('1', '1', Spatial::GeomFromText('MULTILINESTRING ((10 10, 10 20, 15 20), (15 20, 20 20, 20 15), (20 15, 20 10, 10 10), (15 15, 15 20), (15 20, 15 25, 25 25, 25 15, 20 15), (20 15, 15 15))', 3395)),
('0', '2', Spatial::GeomFromText('LINESTRING (20 20, 20 30, 30 30, 30 20, 20 20)', 3395))