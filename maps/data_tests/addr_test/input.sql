-- Create tables for id matching

INSERT INTO {rd} (rd_id, rd_type) VALUES
(2106071601, 1);

INSERT INTO {node} (node_id, shape) VALUES
(1, Spatial::Transform(Spatial::GeomFromText('POINT(100 100)', 3395), 4326)),
(2, Spatial::Transform(Spatial::GeomFromText('POINT(250 250)', 3395), 4326)),
(3, Spatial::Transform(Spatial::GeomFromText('POINT(50 50)', 3395), 4326)),
(4, Spatial::Transform(Spatial::GeomFromText('POINT(500 500)', 3395), 4326)),
(5, Spatial::Transform(Spatial::GeomFromText('POINT(700 700)', 3395), 4326)),
(6, Spatial::Transform(Spatial::GeomFromText('POINT(800 800)', 3395), 4326)),
(7, Spatial::Transform(Spatial::GeomFromText('POINT(1000 1000)', 3395), 4326)),
(8, Spatial::Transform(Spatial::GeomFromText('POINT(2200 2200)', 3395), 4326))
;

INSERT INTO {bld} (bld_id, ft_type_id, cond, isocode, height) VALUES
(1, 101, 0, 'RU', 5),
(2, 101, 0, 'RU', 5),
(3, 101, 0, 'RU', 5),
(4, 101, 0, 'RU', 5),
(5, 101, 0, 'RU', 5),
(6, 101, 0, 'RU', 5),
(7, 101, 0, 'RU', 5),
(8, 101, 0, 'RU', 5),
(9, 102, 0, 'RU', 5), -- urban-industrial
(10,106, 0, 'RU', 5), -- urban-structure
(11,101, 0, 'RU', 5),
(12,101, 0, 'RU', 5)
;

-- Create source tables

INSERT INTO {addr} (isocode, addr_id, node_id, rd_id, disp_class) VALUES
('RU', 1, 1, 2106071601, 5), -- contains no pois
('RU', 2, 2, 2106071601, 5), -- contains poi far from building number
('RU', 3, 3, 2106071601, 5), -- contains poi at same position as number
('RU', 4, 4, 2106071601, 5), -- contains several pois all far from number 
('RU', 5, 5, 2106071601, 5), -- contains several pois and one close to number
('RU', 6, 6, 2106071601, 5), -- has two touching buildings, addr overlaps with poi, not copied
('RU', 7, 7, 2106071601, 5), -- has three buildings, addr overlaps with poi and should not be moved from urban-residental
('RU', 8, 8, 2106071601, 5)  -- has three buildings, two touching, addr duplicates
;

INSERT INTO {bld_addr} (bld_id, addr_id) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4),
(5, 5),
(6, 6),
(7, 6),
(8, 7),
(9, 7),
(10,7),
(11,8),
(12,8),
(13,8)
;

INSERT INTO {bld_geom} (bld_id, shape) VALUES
(1, Spatial::Transform(Spatial::MakeEnvelope(90, 90, 110, 110, 3395), 4326)),
(2, Spatial::Transform(Spatial::MakeEnvelope(200, 200, 300, 300, 3395), 4326)),
(3, Spatial::Transform(Spatial::MakeEnvelope(10, 10, 90, 90, 3395), 4326)),
(4, Spatial::Transform(Spatial::MakeEnvelope(400, 400, 600, 600, 3395), 4326)),
(5, Spatial::Transform(Spatial::MakeEnvelope(650, 650, 750, 750, 3395), 4326)),
(6, Spatial::Transform(Spatial::MakeEnvelope(750, 750, 850, 850, 3395), 4326)),
(7, Spatial::Transform(Spatial::MakeEnvelope(850, 850, 950, 950, 3395), 4326)),
(8, Spatial::Transform(Spatial::MakeEnvelope(950, 950, 1050, 1050, 3395), 4326)),
(9, Spatial::Transform(Spatial::MakeEnvelope(1100, 1100, 1300, 1300, 3395), 4326)),
(10,Spatial::Transform(Spatial::MakeEnvelope(1320, 1320, 1400, 1400, 3395), 4326)),
(11,Spatial::Transform(Spatial::MakeEnvelope(2000, 2000, 2400, 2400, 3395), 4326)),
(12,Spatial::Transform(Spatial::MakeEnvelope(2500, 2500, 2700, 2700, 3395), 4326)),
(13,Spatial::Transform(Spatial::MakeEnvelope(2700, 2700, 2800, 2800, 3395), 4326))
;

INSERT INTO {addr_nm_tmp} (addr_id) VALUES
(1),
(2),
(3),
(4),
(5),
(6),
(7),
(8)
;

INSERT INTO {ft} (ft_id, ft_type_id, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi) VALUES
(0, 1600, 10, 0.0, 10, 0.0),
(1, 1600, 10, 0.0, 5,  0.0),
(2, 1600, 5,  0.0, 10, 0.0),
(3, 1600, 5,  0.0, 5,  0.0),
(4, 1600, 5,  0.0, 5,  0.0),
(5, 1600, 5,  0.0, 5,  0.0),
(6, 1600, 5,  0.0, 5,  0.0),
(7, 1600, 5,  0.0, 5,  0.0),
(8, 1600, 5,  0.0, 5,  0.0),
(9, 1600, 5,  0.0, 5,  0.0)
;

INSERT INTO {poi_geom_tmp} (ft_id, geom) VALUES
(0, Spatial::GeomFromText('POINT(100 100)', 3395)), -- in addr 1, should be ignored
(1, Spatial::GeomFromText('POINT(210 210)', 3395)), -- in addr 2
(2, Spatial::GeomFromText('POINT(50 50)', 3395)), -- in addr 3
(3, Spatial::GeomFromText('POINT(400 400)', 3395)), -- in addr 4
(4, Spatial::GeomFromText('POINT(450 600)', 3395)), -- in addr 4
(5, Spatial::GeomFromText('POINT(550 450)', 3395)), -- in addr 4
(6, Spatial::GeomFromText('POINT(650 650)', 3395)), -- in addr 5
(7, Spatial::GeomFromText('POINT(705 705)', 3395)), -- in addr 5
(8, Spatial::GeomFromText('POINT(800 800)', 3395)), -- in addr 6
(9, Spatial::GeomFromText('POINT(1000 1000)', 3395)) -- in addr 7
;
