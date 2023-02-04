INSERT INTO {ad} (ad_id, level_kind, disp_class, isocode) VALUES
(0, 4, 5, 'RU'),
(1, 4, 5, 'UK'),

(10, 4, 5, 'TR'),
(11, 4, 5, 'TR'),
(12, 4, 5, 'TR'),
(13, 4, 5, 'TR'),

(14, 4, 5, 'TR');

INSERT INTO {ad_nm} (nm_id, ad_id, lang, extlang, script, region, is_local, is_auto, name, name_type) VALUES
(0, 0, 'ru', NULL, NULL, NULL, true, false, 'город Москва', 0),
(1, 0, 'ru', NULL, NULL, NULL, true, false, 'Москва', 1),
(2, 0, 'en', NULL, NULL, NULL, false, false, 'Moscow', 1),
(3, 0, 'en', NULL, NULL, 'GB', false, false, 'Moscow', 0),
(4, 0, 'ru', NULL, NULL, NULL, false, false, 'мск', 4),
(5, 1, 'ru', NULL, NULL, NULL, false, false, 'город Лондон', 0),
(6, 1, 'ru', NULL, NULL, NULL, false, false, 'Лондон', 1),
(7, 1, 'en', NULL, NULL, NULL, true, false, 'London', 0),

-- MAPSRENDER-2373
(10, 10, 'tr', NULL, NULL, NULL, true, false, 'Osmangazi', 0),
(11, 11, 'tr', NULL, NULL, NULL, true, false, 'Cengizhan', 0),
(12, 12, 'tr', NULL, NULL, NULL, true, false, 'İncirköy', 0),
(13, 13, 'tr', NULL, NULL, NULL, true, false, 'Ortabayır', 0),

-- MAPSRENDER-2765
(14, 14, 'ru', NULL, NULL, NULL, false, false, 'Район № 13', 0),
(15, 14, 'en', NULL, NULL, NULL, false, false, 'District № 13', 0);
