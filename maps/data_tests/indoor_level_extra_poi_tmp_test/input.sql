INSERT INTO {poi_geom_tmp} (ft_id, geom) VALUES
(110, Spatial::GeomFromText('POINT(1 1)', 3395)),
(111, Spatial::GeomFromText('POINT(1 1)', 3395)),
(201, Spatial::GeomFromText('POINT(3 1)', 3395)),
(310, Spatial::GeomFromText('POINT(5 1)', 3395)),
(321, Spatial::GeomFromText('POINT(5 1)', 3395)),
(421, Spatial::GeomFromText('POINT(7 1)', 3395));

INSERT INTO {ft} (ft_id, p_ft_id, ft_type_id, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi) VALUES
(110, 4019231360, 1, 5, 0, 5, 0),  -- plan #1 level #-1
(111, 4019231490, 1, 5, 0, 5, 0),  -- plan #1 level #1
(201, NULL,       1, 5, 0, 5, 0),  -- outside of building, above plan #1 level #-1
(310, 4019231360, 1, 5, 0, 5, 0),  -- plan #1 level #-1
(321, 4019187570, 1, 5, 0, 5, 0),  -- plan #2 level #1
(421, 4019187570, 1, 5, 0, 5, 0);  -- plan #2 level #1

INSERT INTO {indoor_level_data} (ft_id, ft_type_id, ft_type_name, p_ft_id, disp_class, name, indoor_plan_id, indoor_level_id, geom) VALUES
(4019231360, 2302, "indoor-level", 4019230920, 5, NULL, "2D052FEB0875A508919F32DD4F774AD8", "-1", Spatial::GeomFromText('POLYGON ((0 0, 6 0, 6 2, 0 2, 0 0))', 3395)),
(4019231490, 2302, "indoor-level", 4019230920, 5, NULL, "2D052FEB0875A508919F32DD4F774AD8", "1", Spatial::GeomFromText('POLYGON ((0 0, 2 0, 2 2, 0 2, 0 0))', 3395)),
(4019187570, 2302, "indoor-level", 4019187040, 5, NULL, "E791FE8AC8641FC7FF1C88FE28C7C1F7", "1", Spatial::GeomFromText('POLYGON ((4 0, 8 0, 8 2, 4 2, 4 0))', 3395));
