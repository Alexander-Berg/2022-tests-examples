INSERT INTO {ad_nm_tmp_out} WITH TRUNCATE (ad_id, name, name_lo, name_upper) VALUES
(0,
 json('{"en_RU": "Moscow", "en-GB_RU": "Moscow", "ru_RU_LOCAL": "Москва"}'),
 'город Москва',
 json('{"en_RU": "MOSCOW", "en-GB_RU": "MOSCOW", "ru_RU_LOCAL": "МОСКВА"}')),
(1,
 json('{"ru_UK": "Лондон", "en_UK_LOCAL": "London"}'),
 'London',
 json('{"ru_UK": "ЛОНДОН", "en_UK_LOCAL": "LONDON"}')),

(10, json('{"tr_TR_LOCAL": "Osmangazi"}'), 'Osmangazi', json('{"tr_TR_LOCAL": "OSMANGAZİ"}')),
(11, json('{"tr_TR_LOCAL": "Cengizhan"}'), 'Cengizhan', json('{"tr_TR_LOCAL": "CENGİZHAN"}')),
(12, json('{"tr_TR_LOCAL": "İncirköy"}'),  'İncirköy',  json('{"tr_TR_LOCAL": "İNCİRKÖY"}')),
(13, json('{"tr_TR_LOCAL": "Ortabayır"}'), 'Ortabayır', json('{"tr_TR_LOCAL": "ORTABAYIR"}')),

(14,
 json('{"en_TR": "District №\u00A013", "ru_TR": "Район №\u00A013"}'),
 NULL,
 json('{"en_TR": "DISTRICT №\u00A013", "ru_TR": "РАЙОН №\u00A013"}'));
