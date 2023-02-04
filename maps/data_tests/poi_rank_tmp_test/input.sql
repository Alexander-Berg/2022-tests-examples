INSERT INTO {ft}
(
  ft_id, ft_type_id, disp_class, disp_class_tweak, disp_class_navi,
  disp_class_tweak_navi
)
VALUES
(1, 171, 4, -0.125, 5, -0.25),
(2, 171, 5, 0.0, 4, 0.0);

INSERT INTO {ft_experiment}
(
  ft_id, experiment_id, disp_class, disp_class_tweak, disp_class_navi,
  disp_class_tweak_navi
)
VALUES
(1, 'exp1', 4, -0.25, 3, -0.125);

INSERT INTO {ft_source_tmp} (ft_id, source) VALUES
(1, json('{"org": "1"}')),
(2, json('{}'));

INSERT INTO {poi_geom_tmp} (ft_id, geom) VALUES
(1, Spatial::GeomFromText('POINT(0 0)', 3395)),
(2, Spatial::GeomFromText('POINT(0.1 0.1)', 3395));
