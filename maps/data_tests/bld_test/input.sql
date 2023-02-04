INSERT INTO {rd} (rd_id, rd_type) VALUES
(2106071601, 1)
;


INSERT INTO {node} (node_id, shape) VALUES
(1, Spatial::GeomFromText('POINT(10 10)', 4326)),
(2, Spatial::GeomFromText('POINT(2.5 2.5)', 4326)),
(3, Spatial::GeomFromText('POINT(0.5 0.5)', 4326)),
(4, Spatial::GeomFromText('POINT(22.5 22.5)', 4326)),
(5, Spatial::GeomFromText('POINT(31 31)', 4326)),
(6, Spatial::GeomFromText('POINT(40 40)', 4326))
;


INSERT INTO {addr} (isocode, addr_id, node_id, rd_id, disp_class) VALUES
('RU', 1, 1, 2106071601, 5),
('RU', 2, 2, 2106071601, 5),
('RU', 3, 3, 2106071601, 5),
('RU', 4, 4, 2106071601, 5),
('RU', 5, 5, 2106071601, 5),
('RU', 6, 6, 2106071601, 5)
;


INSERT INTO {bld} (bld_id, ft_type_id, cond, isocode, height, p_bld_id) VALUES
(1, 101, 0, 'RU', 5, null),
(2, 101, 0, 'RU', 5, null),
(3, 101, 0, 'RU', 5, null),
(4, 101, 0, 'RU', 5, null),
(5, 101, 0, 'RU', 5, 1),
(6, 101, 0, 'RU', 5, null),
(7, 101, 0, 'RU', 5, null)
;


INSERT INTO {bld_addr} (bld_id, addr_id) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4),
(5, 5),
(6, 6),
(7, 6)
;


INSERT INTO {bld_geom} (bld_id, shape) VALUES
(1, Spatial::MakeEnvelope(0.01, 0.01, 0.02, 0.02, 4326)),
(2, Spatial::MakeEnvelope(0.02, 0.02, 0.03, 0.03, 4326)),
(3, Spatial::MakeEnvelope(0.03, 0.03, 0.04, 0.04, 4326)),
(4, Spatial::MakeEnvelope(0.04, 0.04, 0.05, 0.05, 4326)),
(5, Spatial::MakeEnvelope(0.05, 0.05, 0.06, 0.06, 4326)),
(6, Spatial::MakeEnvelope(0.06, 0.06, 0.07, 0.07, 4326)),
(7, Spatial::MakeEnvelope(0.07, 0.07, 0.08, 0.08, 4326))
;


INSERT INTO {bld_indoor_covered_tmp} (bld_id) VALUES
(1),
(2),
(5),
(6),
(7)
;
