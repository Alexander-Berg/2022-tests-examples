INSERT INTO {border_tmp_out} (edge_id, level_kind, isocodes, is_recognized_by, is_not_recognized_by) VALUES

-- Serbia own border
(5, 1, ['RS'], [], []),
(6, 1, ['RS'], [], []),
(7, 1, ['RS'], [], []),
(8, 1, ['RS'], [], []),

-- Serbia/Kosovo external border
(1, 1, ['RS'], [], ['FR', 'TR']),
(4, 1, ['RS'], [], ['FR', 'TR']),

-- Serbia/Kosovo external border
(1, 1, ['XK'], ['FR'], []),
(4, 1, ['XK'], ['FR'], []),
(4, 1, ['XK'], ['TR'], []),
(1, 1, ['XK'], ['TR'], []),

-- Serbia-Kosovo border as disputed
(3, 2, ['RS'], [], ['FR', 'TR']),
(2, 2, ['RS'], [], ['FR', 'TR']),

-- Serbia-Kosovo border as not disputed
(3, 1, ['RS', 'XK'], ['FR'], []),
(2, 1, ['RS', 'XK'], ['FR'], []),
(3, 1, ['RS', 'XK'], ['TR'], []),
(2, 1, ['RS', 'XK'], ['TR'], []);
