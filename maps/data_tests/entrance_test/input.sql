INSERT INTO {bld} (bld_id, ft_type_id, cond, isocode, height) VALUES
(1, 101, 0, 'RU', 5),
(2, 101, 0, 'RU', 5),
(3, 102, 0, 'RU', 5),
(4, 101, 0, 'RU', 5)
;

INSERT INTO {bld_geom} (bld_id, shape) VALUES
(1, Spatial::Transform(Spatial::MakeEnvelope( 0,  0, 10, 10, 3395), 4326)),
(2, Spatial::Transform(Spatial::MakeEnvelope(10, 10, 20, 20, 3395), 4326)),
(3, Spatial::Transform(Spatial::MakeEnvelope(20, 20, 30, 30, 3395), 4326)),
(4, Spatial::Transform(Spatial::MakeEnvelope(30, 30, 40, 40, 3395), 4326))
;

INSERT INTO {ft} (ft_id, ft_type_id, isocode, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi) VALUES
(1, 1904, 'RU', 9, 0.0, 10, 0.0),
(2, 1904, 'RU', 8, 0.0, 10, 0.0),
(3, 1904, 'RU', 7, 0.0, 10, 0.0),
(4, 1904, 'RU', 6, 0.0, 10, 0.0),
(5, 1904, 'RU', 5, 0.0, 10, 0.0),
(6, 1904, 'RU', 4, 0.0, 10, 0.0),
(7, 1904, 'RU', 3, 0.0, 10, 0.0),
(8, 1904, 'RU', 2, 0.0, 10, 0.0),
(9, 1904, 'RU', 1, 0.0, 10, 0.0)
;

INSERT INTO {ft_geom} (ft_id, shape) VALUES
(1, Spatial::Transform(Spatial::GeomFromText("POINT (0 5)", 3395), 4326)), -- bld 1, two ranges
(2, Spatial::Transform(Spatial::GeomFromText("POINT (5 0)", 3395), 4326)), -- bld 1, exact
(3, Spatial::Transform(Spatial::GeomFromText("POINT (10.5 0)", 3395), 4326)), -- bld 1, single flat
(4, Spatial::Transform(Spatial::GeomFromText("POINT (15 15)", 3395), 4326)), -- bld 2, no ranges
(5, Spatial::Transform(Spatial::GeomFromText("POINT (15 15)", 3395), 4326)), -- bld 2, exact, no entrance name
(6, Spatial::Transform(Spatial::GeomFromText("POINT (25 25)", 3395), 4326)), -- bld 3, not a residential
(7, Spatial::Transform(Spatial::GeomFromText("POINT (50 50)", 3395), 4326)), -- no bld
(8, Spatial::Transform(Spatial::GeomFromText("POINT (35 35)", 3395), 4326)), -- bld 4, exact
(9, Spatial::Transform(Spatial::GeomFromText("POINT (35 35)", 3395), 4326))  -- bld 4, not exact
;

INSERT INTO {entrance_flat_range} (flat_range_id, flat_first, flat_last, ft_id, is_exact) VALUES
(1,  "6", "10", 1, true),
(2, "11", "55", 1, true),
(3, "1A", "5A", 1, true),
(4, "1A", "1A", 2, true),
(5, "1B", "1B", 3, true),
(6, "2A", "2A", 5, true),
(7, "2B", "2B", 6, true),
(8,  "3", "30", 7, true),
(9,  "1",  "2", 8, true),
(10, "3",  "4", 9, false)
;

INSERT INTO {ft_nm_tmp} (ft_id, name, name_lo) VALUES
(1, json('{"en": "1", "ru": "1"}'), "1"),
(2, json('{"en": "2", "ru": "2"}'), "2"),
(3, json('{"en": "3", "ru": "3"}'), "3"),
(4, json('{"en": "4", "ru": "4"}'), "4"),
(6, json('{"en": "6", "ru": "6"}'), "6"),
(7, json('{"en": "7", "ru": "7"}'), "7"),
(8, json('{"en": "8", "ru": "8"}'), "8"),
(9, json('{"en": "9", "ru": "9"}'), "9")
;
