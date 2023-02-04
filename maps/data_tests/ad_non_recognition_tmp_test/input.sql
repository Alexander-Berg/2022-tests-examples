insert into {ad} (ad_id, p_ad_id, level_kind, disp_class, isocode, g_ad_id) values
(1, NULL, 1, 5, 'RU', NULL),
(2, NULL, 1, 5, 'RU', 1),
(3, NULL, 1, 5, 'UA', NULL),
(4, NULL, 1, 5, 'UA', 3),
(5, 1, 2, 5, 'RU', NULL),
(6, 3, 2, 5, 'UA', 5),
(7, NULL, 1, 5, 'XC', 5),
(8, 5, 3, 5, 'RU', NULL),
(9, NULL, 2, 5, 'GE', NULL);

insert into {ad_recognition} (ad_id, isocode) values
(1, 'RU'),
(3, 'RU'),
(3, 'TR'),
(5, 'RU'),
(7, 'TR');
