INSERT INTO {rd} (rd_id, rd_type, isocode) VALUES
(0, 2, 'RU'),
(1, 2, 'FR'),
(2, 2, 'US'),
(3, 2, 'UK'),
(4, 2, 'NZ'),
(5, 2, 'NZ'),
(6, 2, 'NZ'),
(7, 2, 'NZ'),
(8, 2, 'RU');

INSERT INTO {rd_nm} (nm_id, rd_id, lang, extlang, script, region, is_local, is_auto, name, name_type) VALUES
(0, 0, 'ru', NULL, NULL, NULL, true, false, 'Р-254 Иртыш', 0),
(1, 0, 'ru', NULL, 'Latn', NULL, true, true, 'M-51 Baykal', 5),
(2, 0, 'ru', NULL, NULL, NULL, true, false, 'Р-254', 1),
(3, 0, 'ru', NULL, NULL, NULL, true, false, 'М-51 Байкал', 5),
(4, 0, 'ru', NULL, NULL, NULL, true, false, 'Челябинск - Новосибирск', 4),
(5, 0, 'ru', NULL, 'Latn', NULL, true, true, 'Chelyabinsk - Novosibirsk', 4),
(6, 0, 'ru', NULL, 'Latn', NULL, true, true, 'R-254 Irtysh', 0),

(7, 1, 'ru', NULL, NULL, NULL, false, false, 'бульвар Сен-Жермен', 0),
(8, 1, 'en', NULL, NULL, NULL, false, false, 'Boulevard Saint-Germain', 0),
(9, 1, 'fr', NULL, NULL, NULL, false, false, 'Boulevard Saint-Germain', 0),

(10,2, 'en', NULL, NULL, NULL, false, false, '7th Street', 0),

(11,3, 'en', NULL, NULL, NULL, false, false, 'Fore Street Avenue', 0),

(12,4, 'en', NULL, NULL, NULL, false, false, 'The Avenue', 0),
(12,5, 'en', NULL, NULL, NULL, false, false, '7th The Avenue', 0),
(12,6, 'en', NULL, NULL, NULL, false, false, 'the Avenue', 0),
(12,7, 'en', NULL, NULL, NULL, false, false, 'Hythe Avenue', 0),

(13,8, 'ru', NULL, NULL, NULL, false, false, 'улица Зеленая аллея', 0);
