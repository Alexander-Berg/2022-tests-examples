from shapely.geometry import Polygon

from maps.geoq.hypotheses.lib.filter_secret_regions import (
    _FilterMapper
)

from maps.poi.pylibs.util.geo import geo_to_mercator_polygon


TEST_POLYGON = [
    geo_to_mercator_polygon(
        Polygon([
            (37.590365, 55.733484),
            (37.5901754, 55.733378767),
            (37.589995208, 55.733481059),
            (37.590011293, 55.73349008),
            (37.589673146, 55.733682033),
            (37.589434509, 55.733540333),
            (37.590010168, 55.733259342),
            (37.589791285, 55.733116489),
            (37.589153641, 55.73344335),
            (37.589142942, 55.733436705),
            (37.589102873, 55.733456264),
            (37.589124724, 55.733470516),
            (37.588904481, 55.73357802),
            (37.589466153, 55.733888953),
            (37.589176662, 55.734075789),
            (37.589299893, 55.734136577),
            (37.590365, 55.733484)
        ])
    )
]

TEST_POINTS = [
    {'lat': 55.733674, 'lon': 37.589412},
    {'lat': 55.734237, 'lon': 37.588248}
]

EXPECTED_POINTS = [
    {'lat': 55.734237, 'lon': 37.588248}
]


def test_filter_mapper():
    mapper = _FilterMapper(TEST_POLYGON)

    result = []
    for row in TEST_POINTS:
        result += list(mapper(row))

    assert EXPECTED_POINTS == result
