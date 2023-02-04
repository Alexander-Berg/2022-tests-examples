SELECT (p_ft_id = 4000000000 AND ft_type_id = 2302 AND indoor_level_id = '1') FROM {indoor_level_tmp} WHERE ft_id = 4000000001

UNION ALL

SELECT (p_ft_id = 4000000000 AND ft_type_id = 2302 AND indoor_level_id = '2') FROM {indoor_level_tmp} WHERE ft_id = 4000000002
