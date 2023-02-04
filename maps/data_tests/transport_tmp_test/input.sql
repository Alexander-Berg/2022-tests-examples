INSERT INTO {ft} (ft_id, p_ft_id, ft_type_id, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi, isocode) VALUES
(1543306266, null, 671, 10, 0, 10, 0, 'RU'),  -- without source_id
(1543308889, null, 671, 5,  0, 5,  0, 'RU'),
(1746412235, null, 671, 5,  0, 5,  0, 'RU'),
(2472242090, null, 671, 5,  0, 5,  0, 'RU');

INSERT INTO {ft_nm_tmp} (ft_id, name, name_lo) VALUES
(1543306266, json('{"ru_RU_LOCAL": "Шоссе Революции"}'), 'Шоссе Революции'),
(1543308889, json('{"ru_RU_LOCAL": "Улица Ломоносова"}'), 'Улица Ломоносова'),
(1746412235, json('{"ru_RU_LOCAL": "Апраксин двор"}'), 'Апраксин двор'),
(2472242090, json('{"ru_RU_LOCAL": "Станция метро Сенная площадь"}'), 'Станция метро Сенная площадь');

INSERT INTO {ft_source} (ft_id, source_type_id, source_id) VALUES
(1543308889, 3, 'stop__10076602'),
(1746412235, 3, 'stop__10189898'),
(2472242090, 3, 'stop__10075347');
