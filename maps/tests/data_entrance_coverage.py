# TODO: cover more cornercases
ENTRANCE_FLAT_RANGE = [
    {'ft_id': 1001, 'flat_first': b'1', 'flat_last': b'5', 'is_exact': False},
    {'ft_id': 1002, 'flat_first': b'10', 'flat_last': b'19', 'is_exact': False},
    {'ft_id': 1003, 'flat_first': b'25', 'flat_last': b'30', 'is_exact': False},
]

FT_ADDR = [
    {'addr_id': 100, 'ft_id': 1001, 'role': 3},
    {'addr_id': 100, 'ft_id': 1002, 'role': 3},
    {'addr_id': 100, 'ft_id': 1003, 'role': 3},
]

FT_NM = [
    {'ft_id': 1001, 'script': None, 'name': b'23'},
    {'ft_id': 1002, 'script': b'something', 'name': b'24'},
    {'ft_id': 1002, 'script': None, 'name': b'22'},
    {'ft_id': 1003, 'script': None, 'name': b'23'},
]

ADDR = [
    {'addr_id': 100, 'node_id': 203},
]

NODE = [
    {'node_id': 201, 'x': 10.1, 'y': 20.1},
    {'node_id': 202, 'x': 10.2, 'y': 20.2},
    {'node_id': 203, 'x': 10.3, 'y': 20.3},
]

HYPOTHESIS_DATA = [
    {
        'type': 'geoq-flats-entrance-coverage',
        'lat': 20.3,
        'lon': 10.3,
        'text': (
            'В данном доме наблюдается высокая доля неточных квартир по подъездам.\n'
            'Возможно, можно заполнить пропуски.\n'
            'Подъезды:\n'
            'Подъезд 22: квартиры [10-19]\n'
            'Подъезд 23: квартиры [1-5], [25-30]'
        )
    },
]
