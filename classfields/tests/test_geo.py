import pytest
from prediction_api.common.geo import GeoExportHelper, GeoType


@pytest.fixture(scope="session")
def geobase():
    return GeoExportHelper.from_json("tests_data/small_geobase.json")


@pytest.mark.parametrize("rid,level,expected_rid",  [
    (213, GeoType.COUNTRY, 225),
    (213, GeoType.CITY, 213),
    (213, GeoType.VILLAGE, 213),
    (213, GeoType.FEDERATION_ENTITY, 1)
])
def test_round_region_by_id(geobase, rid, level, expected_rid):
    assert geobase.round_region_by_id(rid, level) == expected_rid
