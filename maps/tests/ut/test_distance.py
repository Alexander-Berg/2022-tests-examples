import pytest
from ya_courier_backend.util.distance import distance
import geopy.distance

MOSCOW_LOCATIONS = [
    [55.756567, 37.602243],
    [55.754776, 37.611298]
]

VLADIVOSTOK_LOCATIONS = [
    [43.118605, 131.921211],
    [43.124930, 131.922686]
]

MOSCOW_LOCATIONS_SHORT = [
    [55.756567, 37.602243],
    [55.754776, 37.601298]
]

VLADIVOSTOK_LOCATIONS_SHORT = [
    [43.118605, 131.921211],
    [43.119930, 131.922686]
]


def test_moscow():
    assert geopy.distance.distance(*MOSCOW_LOCATIONS).m == pytest.approx(602.4813990551567)
    assert geopy.distance.vincenty(*MOSCOW_LOCATIONS).m == pytest.approx(602.4813990551567)
    assert geopy.distance.great_circle(*MOSCOW_LOCATIONS).m == pytest.approx(600.5705061970231)
    assert distance(*MOSCOW_LOCATIONS) == pytest.approx(600.5696578018213)


def test_vladivostok():
    assert geopy.distance.distance(*VLADIVOSTOK_LOCATIONS).m == pytest.approx(712.8553790856823)
    assert geopy.distance.vincenty(*VLADIVOSTOK_LOCATIONS).m == pytest.approx(712.8553790856823)
    assert geopy.distance.great_circle(*VLADIVOSTOK_LOCATIONS).m == pytest.approx(713.4246270137558)
    assert distance(*VLADIVOSTOK_LOCATIONS) == pytest.approx(713.4236191952092)


def test_moscow_short():
    assert geopy.distance.distance(*MOSCOW_LOCATIONS_SHORT).m == pytest.approx(208.04521255578126)
    assert geopy.distance.vincenty(*MOSCOW_LOCATIONS_SHORT).m == pytest.approx(208.04521255578126)
    assert geopy.distance.great_circle(*MOSCOW_LOCATIONS_SHORT).m == pytest.approx(207.7433641121064)
    assert distance(*MOSCOW_LOCATIONS_SHORT) == pytest.approx(207.7430706443338)


def test_vladivostok_short():
    assert geopy.distance.distance(*VLADIVOSTOK_LOCATIONS_SHORT).m == pytest.approx(189.94139029910184)
    assert geopy.distance.vincenty(*VLADIVOSTOK_LOCATIONS_SHORT).m == pytest.approx(189.94139029910184)
    assert geopy.distance.great_circle(*VLADIVOSTOK_LOCATIONS_SHORT).m == pytest.approx(189.8410138723597)
    assert distance(*VLADIVOSTOK_LOCATIONS_SHORT) == pytest.approx(189.8407456935189)
