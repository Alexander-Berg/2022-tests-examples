INSERT INTO {ft} (ft_id, p_ft_id, ft_type_id, disp_class, disp_class_tweak, disp_class_navi, disp_class_tweak_navi, isocode) VALUES
-- indoor plan and levels
(4000000000, null,       2301, 5, 0, 5, 0, '001'),
(4000000001, 4000000000, 2302, 5, 0, 5, 0, '001'),
(4000000002, 4000000000, 2302, 5, 0, 5, 0, '001'),
-- POI
(4000000030, 4000000001, 2601, 5, 0, 5, 0, '001'),
(4000000031, 4000000002, 2601, 5, 0, 5, 0, '001'),
(4000000032, 4000000002, 224,  5, 0, 5, 0, '001'),
-- Hydro
(4000000040, 4000000003, 553,  5, 0, 5, 0, '001'),
(4000000041, 4000000004, 505,  5, 0, 5, 0, '001');

INSERT INTO {ft_nm} (nm_id, ft_id, lang, is_local, is_auto, name, name_type) VALUES
-- Level names
(4100000000, 4000000001, 'ru',  false, false, 'Этаж 1', 0),
(4100000001, 4000000001, 'en',  false, false, 'Level 1', 0),
(4100000002, 4000000001, 'zxx', false, false, '1', 6),
(4100000003, 4000000002, 'ru',  false, false, 'Этаж 2', 0),
(4100000004, 4000000002, 'en',  false, false, 'Level 2', 0),
(4100000005, 4000000002, 'zxx', false, false, '2', 6),
-- POI names
(4100000006, 4000000030, 'ru',  false, false, 'Вход 1', 0),
(4100000007, 4000000030, 'en',  false, false, 'Entrance 1', 0),
(4100000008, 4000000031, 'ru',  false, false, 'Вход 2', 0),
(4100000009, 4000000031, 'en',  false, false, 'Entrance 2', 0),
(4100000010, 4000000032, 'ru',  false, false, 'Парк Диво остров', 0), -- expect no abbreviation for "остров"
-- Hydro names
(4100000011, 4000000040, 'ru',  false, false, 'река Волга', 0),
(4100000012, 4000000040, 'en',  false, false, 'Volga River', 0),
(4100000013, 4000000041, 'ru',  false, false, 'Ладожское озеро', 0),
(4100000014, 4000000041, 'en',  false, false, 'Lake Ladoga', 0);
