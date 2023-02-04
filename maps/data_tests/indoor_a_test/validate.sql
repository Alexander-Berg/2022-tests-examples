SELECT (ft_type_id = 2302 AND indoor_level_id = '1') FROM {indoor_a} WHERE ft_id = 4000000001

UNION ALL

SELECT (ft_type_id = 2302 AND indoor_level_id = '2') FROM {indoor_a} WHERE ft_id = 4000000002

UNION ALL

SELECT (ft_type_id = 2401 AND indoor_level_id = '1') FROM {indoor_a} WHERE ft_id = 4000000010

UNION ALL

SELECT (ft_type_id = 2401 AND indoor_level_id = '2') FROM {indoor_a} WHERE ft_id = 4000000011
