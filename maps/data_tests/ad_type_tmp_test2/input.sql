INSERT INTO {ad} (ad_id, level_kind, disp_class, isocode) VALUES
-- DISTRICT
(35100, 3, 5, 'TR'),

-- LOCALITY
(45000, 4, 5, 'RU'),
(45100, 4, 5, 'TR'),
(45010, 4, 5, 'RU'),

-- MICRODISTRICT
(45110, 4, 5, 'TR'),

-- NAMED_AREA
(45001, 4, 5, 'RU'),
(45101, 4, 5, 'TR'),
(45011, 4, 5, 'RU'),
(45111, 4, 5, 'TR'),

-- DISTRICT
(55001, 5, 5, 'RU'),
(550000, 5, 5, 'RU'),
(550001, 5, 5, 'RU'),

-- EMPTY
(55100, 5, 5, 'TR'),
(55101, 5, 5, 'TR'),

-- NAMED_AREA
(65000, 6, 5, 'RU'),
(65001, 6, 5, 'RU'),
(65100, 6, 5, 'TR'),
(65101, 6, 5, 'TR'),

-- LOCALITY
(74000, 7, 4, 'RU'),
(74001, 7, 4, 'RU'),
(74100, 7, 4, 'TR'),
(74101, 7, 4, 'TR'),

-- MICRODISTRICT
(75000, 7, 5, 'RU'),

-- BLOCK
(75001, 7, 5, 'RU'),
(75100, 7, 5, 'TR'),
(75101, 7, 5, 'TR');


INSERT INTO {locality}(ad_id, municipality, informal, town, population_is_approximated) VALUES
-- DISTRICT
(35100,  False, False, False, False),

-- LOCALITY
(45000,  False, False, False, False),
(45100,  False, False, False, False),
(45010,  True,  False, False, False),

-- MICRODISTRICT
(45110,  True,  False, False, False),

-- NAMED_AREA
(45001,  False, True,  False, False),
(45101,  False, True,  False, False),
(45011,  True,  True,  False, False),
(45111,  True,  True,  False, False),

-- DISTRICT
(55001,  False, True,  False, False),
(550000, False, False, False, False),

-- EMPTY
(550001, False, False, False, False),
(55100,  False, False, False, False),
(55101,  False, True,  False, False),

-- NAMED_AREA
(65000, False, False, False, False),
(65001, False, True,  False, False),
(65100, False, False, False, False),
(65101, False, True,  False, False),

-- LOCALITY
(74000, False, False, False, False),
(74001, False, True,  False, False),
(74100, False, False, False, False),
(74101, False, True,  False, False),

-- MICRODISTRICT
(75000, False, False, False, False),

-- BLOCK
(75001, False, True,  False, False),
(75100, False, False, False, False),
(75101, False, True,  False, False);


INSERT INTO {ad_nm_tmp}(ad_id, name, name_upper, name_lo) VALUES
(35100,  json('{}'), json('{}'), ''),
(45000,  json('{}'), json('{}'), ''),
(45100,  json('{}'), json('{}'), ''),
(45010,  json('{}'), json('{}'), ''),
(45110,  json('{}'), json('{}'), ''),
(45001,  json('{}'), json('{}'), ''),
(45101,  json('{}'), json('{}'), ''),
(45011,  json('{}'), json('{}'), ''),
(45111,  json('{}'), json('{}'), ''),
(55001,  json('{}'), json('{}'), ''),
(550000, json('{}'), json('{}'), 'place 2'),
(550001, json('{}'), json('{}'), 'город 1'),
(55100,  json('{}'), json('{}'), ''),
(55101,  json('{}'), json('{}'), ''),
(65000,  json('{}'), json('{}'), ''),
(65001,  json('{}'), json('{}'), ''),
(65100,  json('{}'), json('{}'), ''),
(65101,  json('{}'), json('{}'), ''),
(74000,  json('{}'), json('{}'), ''),
(74001,  json('{}'), json('{}'), ''),
(74100,  json('{}'), json('{}'), ''),
(74101,  json('{}'), json('{}'), ''),
(75000,  json('{}'), json('{}'), ''),
(75001,  json('{}'), json('{}'), ''),
(75100,  json('{}'), json('{}'), ''),
(75101,  json('{}'), json('{}'), '');


INSERT INTO {ad_geom} (ad_id, shape) VALUES
(35100,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(45000,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(45100,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(45010,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(45110,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(45001,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(45101,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(45011,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(45111,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(55001,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(550000, Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(550001, Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(55100,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(55101,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(65000,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(65001,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(65100,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(65101,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(74000,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(74001,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(74100,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(74101,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(75000,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(75001,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(75100,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326)),
(75101,  Spatial::GeomFromText('POLYGON((0 0, 50 0, 50 50, 0 50, 0 0))', 4326));
