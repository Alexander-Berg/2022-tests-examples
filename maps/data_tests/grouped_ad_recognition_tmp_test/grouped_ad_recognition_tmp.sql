INSERT INTO {grouped_ad_recognition_tmp_out} (ad_id, is_recognized_by, is_not_recognized_by) VALUES
(1, ['RU'], NULL),
(2, NULL, ['RU']),
(3, ['RU', 'TR'], NULL),
(4, NULL, ['RU', 'TR']),
(5, ['RU'], NULL),
(6, NULL, ['RU', 'TR']),
(7, ['TR'], NULL);
