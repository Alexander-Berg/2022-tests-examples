from shapely.geometry import (
    Point,
    Polygon,
    MultiPolygon,
)


from maps.poi.pylibs.util.geo import (
    geo_to_mercator_pair,
    geo_to_mercator_polygon,
    geo_to_mercator_multipolygon,
)

POLYGON = Polygon(
    [
        (-180, 85),
        (180, 85),
        (180, -85),
        (-180, -85),
    ],
    [
        [
            (0, 0),
            (0, 1),
            (1, 1),
            (1, 0),
        ], [
            (0, 0),
            (0, -1),
            (-1, -1),
            (-1, 0),
        ]
    ]
)
CORRECT_MERCATOR_POLYGON = Polygon(
    [
        (-20037508.3427892, 19929239.1133791),
        (20037508.3427892, 19929239.1133791),
        (20037508.3427892, -19929239.1133791),
        (-20037508.3427892, -19929239.1133791),
        (-20037508.3427892, 19929239.1133791),
    ],
    [
        [
            (0, 0),
            (0, 110579.9652218),
            (111319.4907932, 110579.9652218),
            (111319.4907932, 0),
            (0, 0)
        ],
        [
            (0, 0),
            (0, -110579.9652218),
            (-111319.4907932, -110579.9652218),
            (-111319.4907932, 0),
            (0, 0)
        ]
    ]
)


def test_geo_to_mercator_pair():
    assert Point(*geo_to_mercator_pair((37, 55))).almost_equals(Point(4118821.1593511, 7326837.7150455))
    assert Point(*geo_to_mercator_pair((131.885624, 43.115421))).almost_equals(Point(14681440.5066331, 5300343.5672989))
    assert Point(*geo_to_mercator_pair((0, 0))).almost_equals(Point(0, 0))
    assert Point(*geo_to_mercator_pair((0, 85))).almost_equals(Point(0, 19929239.1133791))
    assert Point(*geo_to_mercator_pair((0, -85))).almost_equals(Point(0, -19929239.1133791))
    assert Point(*geo_to_mercator_pair((180, 0))).almost_equals(Point(20037508.3427892, 0))
    assert Point(*geo_to_mercator_pair((-180, 0))).almost_equals(Point(-20037508.3427892, 0))


def test_geo_to_mercator_polygon():
    mercator_polygon = geo_to_mercator_polygon(POLYGON)
    assert CORRECT_MERCATOR_POLYGON.almost_equals(mercator_polygon)


def test_geo_to_mercator_multipolygon():
    multipolygon = MultiPolygon([
        POLYGON
    ])
    correct_mercator_multipolygon = MultiPolygon([
        CORRECT_MERCATOR_POLYGON
    ])
    mercator_multipolygon = geo_to_mercator_multipolygon(multipolygon)
    assert correct_mercator_multipolygon.almost_equals(mercator_multipolygon)
