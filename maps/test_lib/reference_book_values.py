ZONE_NUMBERS_LIMIT = 100
PREFIX_PUBLIC_ZONE = 'public_'

INCOMPATIBLE_ZONES = {
    1: [['zone_1', 'zone_2'], ['zone_3', 'zone_4']],
    2: [['zone_5', 'zone_6'], ['zone_7', 'zone_8']],
}


def _make_polygon(offset):
    return {
        'coordinates': [
            [
                [offset, offset],
                [offset, offset + 1],
                [offset + 1, offset + 1],
                [offset + 1, offset],
                [offset, offset],
            ]
        ],
        'type': 'Polygon',
    }


def _make_zone(id, company_id=None):
    number = f'{PREFIX_PUBLIC_ZONE}zone_{id}' if company_id is None else f'zone_{id}'
    zone = {
        'id': id,
        'number': number,
        'polygon': _make_polygon(id),
        'company_id': company_id,
    }
    return zone


COMPANY_ZONES = {
    1: [
        _make_zone(1, 1),
        _make_zone(2, 1),
        _make_zone(3, 1),
        _make_zone(4, 1),
    ],
    2: [
        _make_zone(5, 2),
        _make_zone(6, 2),
        _make_zone(7, 2),
        _make_zone(8, 2),
    ],
}

PUBLIC_ZONES = [
    _make_zone(101),
    _make_zone(102),
]
