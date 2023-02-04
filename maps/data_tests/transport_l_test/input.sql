INSERT INTO {ft} (ft_id, p_ft_id, ft_type_id, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi)
VALUES
(0, NULL, 611, 6, 0, 5, 0),
(1, NULL, 611, 6, 0, 5, 0),
(2, NULL, 612, 7, 0, 5, 0), -- filtered, same edge as 4, higher disp_class
(3, NULL, 626, 5, 0, 5, 0), -- filtered, transport.color is not null
(4, NULL, 612, 6, 0, 5, 0),
(5, NULL, 625, 10, 0, 5, 0), -- filtered, disp_class = 10
(6, NULL, 259, 5, 0, 5, 0), -- filtered, ft_type no a transport line
(7, NULL, 626, 5, 0, 5, 0),
(8, NULL, 626, 5, 0, 5, 0) -- filtered, same edge as 7, same ft_type_id, same disp_class
;


INSERT INTO {node} (node_id, shape) VALUES
(0, Spatial::GeomFromText('POINT(10 10)', 4326)),
(1, Spatial::GeomFromText('POINT(20 20)', 4326))
;


INSERT INTO {edge} (edge_id, f_node_id, t_node_id, f_zlev, t_zlev, shape) VALUES
(0, 0, 1, 0, 0, Spatial::GeomFromText('LINESTRING (0 0, 0 1)', 4326)),
(1, 0, 1, 0, 0, Spatial::GeomFromText('LINESTRING (0 1, 1 1)', 4326)),
(2, 0, 1, 1, 0, Spatial::GeomFromText('LINESTRING (1 1, 2 1)', 4326)),
(3, 0, 1, 0, 1, Spatial::GeomFromText('LINESTRING (2 1, 3 1)', 4326)),
(4, 0, 1, 0, 0, Spatial::GeomFromText('LINESTRING (3 1, 3 0)', 4326)),
(5, 0, 1, 0, 0, Spatial::GeomFromText('LINESTRING (3 0, 2 0)', 4326)),
(6, 0, 1, 0, 0, Spatial::GeomFromText('LINESTRING (2 0, 1 0)', 4326)),
(7, 0, 1, 0, 0, Spatial::GeomFromText('LINESTRING (1 0, 0 0)', 4326))
;


INSERT INTO {ft_edge} (ft_id, edge_id) VALUES
(0, 0),
(1, 1),
(2, 2),
(3, 3),
(4, 2),
(5, 5),
(6, 6),
(7, 7),
(8, 7)
;


INSERT INTO {boundary} (geom, wkt) VALUES
(Spatial::Transform(Spatial::GeomFromText('POLYGON((50 50, 50 -50, -50 -50, -50 50, 50 50))', 4326), 3395), '')
;


INSERT INTO {transport_tmp} (ft_id, network, legacy_network, line, legacy_line, color, mtr_types) VALUES
(3, 'fake_network', '', '', '', '#000000', []),
(6, '',             '', '', '', NULL,      [])
;
