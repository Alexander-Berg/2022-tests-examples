INSERT INTO {border_tmp_out} (edge_id, level_kind, isocodes, is_recognized_by, is_not_recognized_by) VALUES

-- Israel own border (1)
(1,  1, ['IL'], [], []),
(2,  1, ['IL'], [], []),
(3,  1, ['IL'], [], []),

-- Syria own border (4, 5, 6, 7)
(12, 1, ['SY'], [], []),
(13, 1, ['SY'], [], []),
(15, 1, ['SY'], [], []),
(16, 1, ['SY'], [], []),

-- Syria region border (6-7)
(14, 2, ['SY'], [], []),


-- Israel-Syria border by IL
(7,  1, ['IL', 'SY'], ['IL'], []),
(5,  1, ['IL'], ['IL'], []),
(6,  1, ['IL'], ['IL'], []),
(8,  1, ['SY'], ['IL'], []),
(11, 1, ['SY'], ['IL'], []),

-- Syria region border (5-6, 5-7) by IL
(9,  2, ['SY'], ['IL'], []),
(10, 2, ['SY'], ['IL'], []),

-- Israel region border (3-4) by IL
(4,  2, ['IL'], ['IL'], []),


-- Israel-Syria border by SY
(4,  1, ['IL', 'SY'], [], ['IL']),
(8,  1, ['SY'], [], ['IL']),
(11, 1, ['SY'], [], ['IL']),
(6,  1, ['SY'], [], ['IL']),
(5,  1, ['SY'], [], ['IL']),

-- Syria region border (4-5, 5-6, 5-7) by SY
(9,  2, ['SY'], [], ['IL']),
(7,  2, ['SY'], [], ['IL']),
(10, 2, ['SY'], [], ['IL']);
