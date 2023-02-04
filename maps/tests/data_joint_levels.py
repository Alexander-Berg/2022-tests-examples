ENTRANCE_LEVEL_FLAT_RANGE = [
    {'ft_id': 1001, 'level_universal_id': b'3', 'flat_first': b'1b', 'flat_last': b'1b', 'is_exact': False},
    {'ft_id': 1001, 'level_universal_id': b'3', 'flat_first': b'2', 'flat_last': b'4', 'is_exact': False},
    {'ft_id': 1001, 'level_universal_id': b'4', 'flat_first': b'5', 'flat_last': b'10', 'is_exact': False},
    {'ft_id': 1001, 'level_universal_id': b'5', 'flat_first': b'11', 'flat_last': b'20', 'is_exact': False},
    {'ft_id': 1001, 'level_universal_id': b'20', 'flat_first': b'201', 'flat_last': b'210', 'is_exact': False},
    {'ft_id': 1002, 'level_universal_id': b'11', 'flat_first': b'1001', 'flat_last': b'2000', 'is_exact': False},
]

FT_NM = [
    {'ft_id': 1001, 'script': None, 'name': b'23'},
    {'ft_id': 1002, 'script': None, 'name': b'22'},
]

FT_CENTER = [
    {'ft_id': 1001, 'node_id': 201},
    {'ft_id': 1002, 'node_id': 202},
    {'ft_id': 1003, 'node_id': 203},
]

NODE = [
    {'node_id': 201, 'x': 10.1, 'y': 20.1},
    {'node_id': 202, 'x': 10.2, 'y': 20.2},
    {'node_id': 203, 'x': 10.3, 'y': 20.3},
]

HYPOTHESIS_DATA = [
    {
        'type': 'geoq-flats-joint-levels',
        'lat': 20.1,
        'lon': 10.1,
        'text': (
            'В подъезде 23 этаж 4 подпёрт снизу этажом 3, а сверху - этажом 5.\n'
            'Если количество квартир на этаже одинаковое, можно разметить весь подъезд.\n'
            'Известные неточные квартирные интервалы в подъезде:\n'
            'Этаж 3: квартиры [2-4], [1b-1b]\n'
            'Этаж 4: квартиры [5-10]\n'
            'Этаж 5: квартиры [11-20]\n'
            'Этаж 20: квартиры [201-210]'
        ),
    },
]
