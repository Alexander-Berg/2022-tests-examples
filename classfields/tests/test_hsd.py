import pytest
from prediction_api.common.stat import HierarchicalStatDictHelper


@pytest.fixture(scope="session")
def hsd():
    return HierarchicalStatDictHelper.from_json("tests_data/small_stats.json")


@pytest.mark.parametrize("key,field_name,stat_name,expected",  [
        (["20355206", "20474757", "1"], "price", "mean", 586705.8696),
        (["20355206", "1"], "price", "mean", 593509.7917),
        (["20355206", "20474757", "1", "42"], "price", "mean", 586705.8696)
])
def test_get_stat(hsd, key, field_name, stat_name, expected):
    assert hsd.get_stat_value(key, field_name, stat_name) == expected
