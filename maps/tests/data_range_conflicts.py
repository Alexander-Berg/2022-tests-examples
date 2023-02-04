FT_ADDR = [
    {'addr_id': 1, 'ft_id': 1001, 'role': 3},
    {'addr_id': 1, 'ft_id': 1002, 'role': 3},
    {'addr_id': 1, 'ft_id': 1003, 'role': 1},
    {'addr_id': 1, 'ft_id': 1010, 'role': 3},
    {'addr_id': 2, 'ft_id': 2001, 'role': 3},
    {'addr_id': 3, 'ft_id': 2001, 'role': 3},
    {'addr_id': 4, 'ft_id': 4001, 'role': 3},
    {'addr_id': 4, 'ft_id': 4002, 'role': 3},
]

FT_NM = [
    {'ft_id': 1001, 'script': None, 'name': b'1'},
    {'ft_id': 1002, 'script': b'EN', 'name': b'Second'},
    {'ft_id': 1002, 'script': None, 'name': b'2'},
    {'ft_id': 1003, 'script': None, 'name': b'3'},
    {'ft_id': 1010, 'script': None, 'name': b'10'},
]

NMAPS_RANGES = [
    {'flat_range_id': 1, 'ft_id': 1001, 'flat_first': b'1', 'flat_last': b'10', 'is_exact': True},
    {'flat_range_id': 2, 'ft_id': 1002, 'flat_first': b'11', 'flat_last': b'20', 'is_exact': True},
    {'flat_range_id': 3, 'ft_id': 1010, 'flat_first': b'91', 'flat_last': b'100', 'is_exact': True},
    {'flat_range_id': 4, 'ft_id': 4001, 'flat_first': b'1', 'flat_last': b'10', 'is_exact': True},
    {'flat_range_id': 5, 'ft_id': 4002, 'flat_first': b'1', 'flat_last': b'10', 'is_exact': True},
]

GEOQ_RANGES = [
    {'flat_range_id': 1, 'ft_id': 1001, 'flat_first': b'1', 'flat_last': b'16', 'is_exact': False},
    {'flat_range_id': 2, 'ft_id': 1001, 'flat_first': b'95', 'flat_last': b'96', 'is_exact': False},
    {'flat_range_id': 3, 'ft_id': 1002, 'flat_first': b'17', 'flat_last': b'32', 'is_exact': False},
]

MATCHER_OUTPUT = [
    {
        'ft_id': 1001,
        'address_office': b'11',
        'delivered': 2,
        'not_delivered': 2,
        'geocoded_lat': 51.178781,
        'geocoded_lon': 71.404558
    },
    {
        'ft_id': 1001,
        'address_office': b'16',
        'delivered': 9,
        'not_delivered': 1,
        'geocoded_lat': 51.178781,
        'geocoded_lon': 71.404558
    },
    {
        'ft_id': 1001,
        'address_office': b'95',
        'delivered': 0,
        'not_delivered': 13,
        'geocoded_lat': 51.178781,
        'geocoded_lon': 71.404558
    },
    {
        'ft_id': 1001,
        'address_office': b'96',
        'delivered': 0,
        'not_delivered': 2,
        'geocoded_lat': 51.178781,
        'geocoded_lon': 71.404558
    },
    {
        'ft_id': 1002,
        'address_office': b'32',
        'delivered': 3,
        'not_delivered': 0,
        'geocoded_lat': 51.179035,
        'geocoded_lon': 71.404414
    },
]


HYPOTHESIS = [
    {
        'type': 'geoq-flats-range-conflicts',
        'lat': 51.178781,
        'lon': 71.404558,
        'text': (
            'В доме есть подъезды, диапазоны квартир в которых могут быть некорректными.\n'
            'У ряда квартир в Народной карте проставлен один подъезд,\n'
            'но в заказах из Я.Еды и Я.Лавки стоит другой подъезд.\n'
            'В заказах к данному подъезду (номер 1) также относятся следующие квартиры:\n'
            '11-16, которые в Народной Карте относятся к подъезду номер 2\n'
            '95-96, которые в Народной Карте относятся к подъезду номер 10\n'
            'В Народной карте размечены диапазоны:\n'
            'Подъезд 1: квартиры [1-10]\n'
            'Подъезд 2: квартиры [11-20]\n'
            'Подъезд 10: квартиры [91-100]'
        ),
    },
]
