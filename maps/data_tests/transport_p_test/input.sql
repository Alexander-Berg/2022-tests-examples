-- bus stops
INSERT INTO {ft} (ft_id, p_ft_id, ft_type_id, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi, isocode) VALUES
(1543306266, null, 671, 9, 0, 9, 0, 'RU'),
(1543308889, null, 671, 5, 0, 5, 0, 'RU'),
(1746412235, null, 671, 5, 0, 5, 0, 'RU'),
(2472242090, null, 671, 5, 0, 5, 0, 'RU');

INSERT INTO {ft_nm_tmp} (ft_id, name, name_lo) VALUES
(1543306266, json('{"ru_RU_LOCAL": "Шоссе Революции"}'), 'Шоссе Революции'),
(1543308889, json('{"ru_RU_LOCAL": "Улица Ломоносова"}'), 'Улица Ломоносова'),
(1746412235, json('{"ru_RU_LOCAL": "Апраксин двор"}'), 'Апраксин двор'),
(2472242090, json('{"ru_RU_LOCAL": "Станция метро Сенная площадь"}'), 'Станция метро Сенная площадь');

INSERT INTO {ft_source} (ft_id, source_type_id, source_id) VALUES
(1543308889, 1, '225710316198'),
(1543308889, 3, 'stop__10076602'),
(1746412235, 1, '12388723225'),
(1746412235, 3, 'stop__10189898'),
(2472242090, 1, '243623184823'),
(2472242090, 3, 'stop__10075347');

INSERT INTO {ft_geom} (ft_id, shape) VALUES
(1543306266, Spatial::GeomFromText('POINT(30.415876 59.958731)', 4326)),
(1543308889, Spatial::GeomFromText('POINT(30.329796 59.931657)', 4326)),
(1746412235, Spatial::GeomFromText('POINT(30.328860 59.931335)', 4326)),
(2472242090, Spatial::GeomFromText('POINT(30.320857 59.927940)', 4326));

-- metro stations and stops
INSERT INTO {ft} (ft_id, p_ft_id, ft_type_id, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi, isocode) VALUES
(2028261156, null,       631, 5, 0, 5, 0, 'RU'),
(100000069,  2028261156, 632, 5, 0, 5, 0, 'RU'),
(100000088,  2028261156, 632, 5, 0, 5, 0, 'RU'),
(100000114,  2028261156, 632, 5, 0, 5, 0, 'RU'),
(100079104,  100000088,  633, 2, 0, 2, 0, 'RU'),
(100079108,  100000114,  633, 2, 0, 2, 0, 'RU'),
(100079120,  100000069,  633, 5, 0, 5, 0, 'RU'),
(100079132,  100000114,  633, 5, 0, 5, 0, 'RU'),
(100079180,  100000088,  633, 2, 0, 2, 0, 'RU'),
(100079184,  100000069,  633, 2, 0, 2, 0, 'RU'),
(100128479,  100079104,  634, 5, 0, 5, 0, 'RU'),
(100128483,  100079108,  634, 5, 0, 5, 0, 'RU'),
(100128491,  100079120,  634, 5, 0, 5, 0, 'RU'),
(100128539,  100079132,  634, 5, 0, 5, 0, 'RU'),
(100128615,  100079184,  634, 5, 0, 5, 0, 'RU'),
(3402688745, 100079184,  634, 9, 0, 9, 0, 'RU');

INSERT INTO {ft_nm_tmp} (ft_id, name, name_lo, name_ll) VALUES
(2028261156, json('{"ru_RU_LOCAL":"Московский метрополитен"}'), 'Московский метрополитен', null),
(100000069,  json('{"ru_RU_LOCAL":"Замоскворецкая линия"}'), 'Замоскворецкая линия', null),
(100000088,  json('{"ru_RU_LOCAL":"Кольцевая линия"}'), 'Кольцевая линия', null),
(100000114,  json('{"ru_RU_LOCAL":"Таганско-Краснопресненская линия"}'), 'Таганско-Краснопресненская линия', null),
(100079104,  json('{"ru_RU_LOCAL":"Краснопресненская"}'), 'метро Краснопресненская', 'Краснопресненская'),
(100079108,  json('{"ru_RU_LOCAL":"Баррикадная"}'), 'метро Баррикадная', 'Баррикадная'),
(100079120,  json('{"ru_RU_LOCAL":"Тверская"}'), 'метро Тверская', 'Тверская'),
(100079132,  json('{"ru_RU_LOCAL":"Пушкинская"}'), 'метро Пушкинская', 'Пушкинская'),
(100079180,  json('{"ru_RU_LOCAL":"Белорусская"}'), 'метро Белорусская', 'Белорусская'),
(100079184,  json('{"ru_RU_LOCAL":"Белорусская"}'), 'метро Белорусская', 'Белорусская'),
(100128479,  json('{"ru_RU_LOCAL":"вход 1"}'), '1', 'вход 1'),
(100128483,  json('{"ru_RU_LOCAL":"вход 2"}'), '2', 'вход 2'),
(100128491,  json('{"ru_RU_LOCAL":"вход 9"}'), '9', 'вход 9'),
(100128539,  json('{"ru_RU_LOCAL":"вход 1"}'), '1', 'вход 1'),
(100128615,  json('{"ru_RU_LOCAL":"вход"}'), null, 'вход'),
(3402688745, json('{"ru_RU_LOCAL":"выход 4"}'), '4', 'выход 4');

INSERT INTO {ft_source} (ft_id, source_type_id, source_id) VALUES
(100000069,  3, '100000069'),
(100000088,  3, '100000088'),
(100000114,  3, '100000114'),
(100079104,  3, 'station__9858831'),
(100079108,  3, 'station__9858783'),
(100079120,  3, 'station__9858918'),
(100079132,  3, 'station__9858894'),
(100079180,  3, 'station__9858787'),
(100079184,  3, 'station__9858786'),
(100128479,  3, 'exit__22769'),
(100128483,  3, 'exit__22622'),
(100128491,  3, 'exit__22997'),
(100128539,  3, 'exit__22942'),
(100128615,  3, 'exit__22628'),
(3402688745, 3, '3402688745');

INSERT INTO {ft_geom} (ft_id, shape) VALUES
(2028261156, Spatial::GeomFromText('POINT(37.615218 55.756460)', 4326)),
(100000069,  Spatial::GeomFromText('LINESTRING(37.480773 55.878275, 37.481213 55.877479)', 4326)),
(100000088,  Spatial::GeomFromText('LINESTRING(37.567425 55.744800, 37.567370 55.744911)', 4326)),
(100000114,  Spatial::GeomFromText('LINESTRING(37.858642 55.674137, 37.852478 55.677138)', 4326)),
(100079104,  Spatial::GeomFromText('POINT(37.577218 55.760213)', 4326)),
(100079108,  Spatial::GeomFromText('POINT(37.581283 55.760819)', 4326)),
(100079120,  Spatial::GeomFromText('POINT(37.605945 55.764458)', 4326)),
(100079132,  Spatial::GeomFromText('POINT(37.603901 55.765751)', 4326)),
(100079180,  Spatial::GeomFromText('POINT(37.585256 55.776775)', 4326)),
(100079184,  Spatial::GeomFromText('POINT(37.582211 55.777391)', 4326)),
(100128479,  Spatial::GeomFromText('POINT(37.576948 55.760380)', 4326)),
(100128483,  Spatial::GeomFromText('POINT(37.581509 55.760692)', 4326)),
(100128491,  Spatial::GeomFromText('POINT(37.605685 55.764341)', 4326)),
(100128539,  Spatial::GeomFromText('POINT(37.604296 55.765978)', 4326)),
(100128615,  Spatial::GeomFromText('POINT(37.582192 55.777263)', 4326)),
(3402688745, Spatial::GeomFromText('POINT(37.582279 55.777428)', 4326));

-- airport
INSERT INTO {ft} (ft_id, p_ft_id, ft_type_id, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi, isocode) VALUES
(1519988663, null, 641, 5, 0, 5, 0, 'RU');

INSERT INTO {ft_nm_tmp} (ft_id, name, name_lo, name_ll) VALUES
(2028261156, json('{"ru_RU_LOCAL":"аэропорт Домодедово"}'), 'аэропорт Домодедово имени М.В. Ломоносова', 'аэропорт Домодедово');

INSERT INTO {ft_source} (ft_id, source_type_id, source_id) VALUES
(1519988663, 1, '45745811341');

INSERT INTO {node} (node_id, shape) VALUES
(1519988663, Spatial::GeomFromText('POINT(37.900487 55.414349)', 4326));

INSERT INTO {ft_center} (ft_id, node_id) VALUES
(1519988663, 1519988663);

INSERT INTO {ft_geom} (ft_id, shape) VALUES
(1519988663, Spatial::GeomFromText('POLYGON((37.880268 55.425139, 37.882441 55.423352, 37.882356 55.423327, 37.880268 55.425139))', 4326));
