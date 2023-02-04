INSERT INTO {indoor_a} (ft_id, ft_type_id, ft_type_name, disp_class, indoor_plan_id, indoor_level_id, geom)
VALUES
(0, 2404, 'name_0', 5, '0', '0', Spatial::MakeEnvelope(0, 0, 10, 10, 3395)),
(1, 2405, 'name_1', 5, '1', '1', Spatial::MakeEnvelope(5, 5, 15, 15, 3395)),
(2, 2404, 'name_2', 5, '1', '1', Spatial::MakeEnvelope(10, 10, 20, 20, 3395)),
(3, 2404, 'name_3', 5, '1', '1', Spatial::MakeEnvelope(15, 15, 25, 25, 3395)),
(4, 2404, 'name_4', 5, '0', '2', Spatial::MakeEnvelope(20, 20, 30, 30, 3395))
;