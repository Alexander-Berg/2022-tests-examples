INSERT INTO {ft_nm_tmp_out} (ft_id, name, name_lo, name_upper, name_ll, name_universal) VALUES
(4000000001, json('{"en_001": "Level 1", "ru_001": "Этаж 1"}'), null, json('{"en_001": "LEVEL 1", "ru_001": "ЭТАЖ 1"}'), null, '1'),
(4000000002, json('{"en_001": "Level 2", "ru_001": "Этаж 2"}'), null, json('{"en_001": "LEVEL 2", "ru_001": "ЭТАЖ 2"}'), null, '2'),
(4000000030, json('{"en_001": "Entrance 1", "ru_001": "Вход 1"}'), null, json('{"en_001": "ENTRANCE 1", "ru_001": "ВХОД 1"}'), null, null),
(4000000031, json('{"en_001": "Entrance 2", "ru_001": "Вход 2"}'), null, json('{"en_001": "ENTRANCE 2", "ru_001": "ВХОД 2"}'), null, null),
(4000000032, json('{"ru_001": "Парк Диво остров"}'), null, json('{"ru_001": "ПАРК ДИВО ОСТРОВ"}'), null, null),
(4000000040, json('{"en_001": "Volga River", "ru_001": "р. Волга"}'), null, json('{"en_001": "VOLGA RIVER", "ru_001": "Р. ВОЛГА"}'), null, null),
(4000000041, json('{"en_001": "Lake Ladoga", "ru_001": "Ладожское оз."}'), null, json('{"en_001": "LAKE LADOGA", "ru_001": "ЛАДОЖСКОЕ ОЗ."}'), null, null)
