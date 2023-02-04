----------------------------------- Inserting into indoor_level_tmp -----------------------------------
INSERT INTO {indoor_level_tmp} (ft_id, p_ft_id, ft_type_id, disp_class, indoor_level_id, name, geom) VALUES

-- Level 1
(4000000001, 4000000000, 2302, 5, '1', json('{"en_001": "Level 1", "ru_001": "Этаж 1"}'),
    Spatial::Transform(Spatial::GeomFromText('POLYGON((37.536177 55.748808, 37.541949 55.750636, 37.543022 55.749607, 37.537271 55.747755, 37.536177 55.748808))', 4326), 3395)),

-- Level 2
(4000000002, 4000000000, 2302, 5, '2', json('{"en_001": "Level 2", "ru_001": "Этаж 2"}'),
    Spatial::Transform(Spatial::GeomFromText('POLYGON((37.536177 55.758808, 37.541949 55.760636, 37.543022 55.759607, 37.537271 55.757755, 37.536177 55.758808))', 4326), 3395));

----------------------------------- Inserting into indoor_plan_data -----------------------------------

INSERT INTO {indoor_plan_data} (ft_id, indoor_plan_id, geom) VALUES (
    4000000000,
    '7F46B130C81DB5A6F315588E94BD6239',
    Spatial::Transform(Spatial::GeomFromText('MULTIPOLYGON(
        ((37.536177 55.748808,37.541949 55.750636,37.543022 55.749607,37.537271 55.747755,37.536177 55.748808)),
        ((37.536177 55.758808,37.541949 55.760636,37.543022 55.759607,37.537271 55.757755,37.536177 55.758808)))', 4326), 3395));
