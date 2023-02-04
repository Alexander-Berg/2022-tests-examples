import yatest

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from ya_courier_backend.util.geobase import Geobase
from ya_courier_backend.util.time_zone import get_time_zone


@skip_if_remote
def test_get_time_zone_moscow():
    Geobase.init(yatest.common.binary_path('geobase/data/v6/geodata6.bin'))
    moscow_lat_lon = {"lat": 55.750001, "lon": 37.616667}
    assert "Europe/Moscow" == get_time_zone(**moscow_lat_lon)


@skip_if_remote
def test_get_time_zone_berlin():
    Geobase.init(yatest.common.binary_path('geobase/data/v6/geodata6.bin'))
    berlin_lat_lon = {"lat": 52.516667, "lon": 13.388889}
    assert "Europe/Berlin" == get_time_zone(**berlin_lat_lon)


@skip_if_remote
def test_get_time_zone_incorrect():
    Geobase.init(yatest.common.binary_path('geobase/data/v6/geodata6.bin'))
    invalid_lat_lon = {"lat": 352.516667, "lon": 13.388889}
    assert "Europe/Moscow" == get_time_zone(**invalid_lat_lon)
