import pytest
import pytz
import datetime as dt
from maps.garden.tools.stat_updater.lib.yt_table_descriptions import yt_types

DATE = dt.datetime(year=2010, month=12, day=5, tzinfo=pytz.UTC)


@pytest.mark.parametrize(
    "test_case",
    [{
        "value": 10,
        "type": yt_types.String,
        "result": "10"
    }, {
        "value": "10",
        "type": yt_types.Uint64,
        "result": 10
    }, {
        "value": 10,
        "type": yt_types.Bool,
        "result": True
    }, {
        "value": None,
        "type": yt_types.Bool,
        "result": False
    }, {
        "value": DATE,
        "type": yt_types.Timestamp,
        "result": int(DATE.timestamp() * 10**6)
    }, {
        "value": int(DATE.timestamp() * 10**6),
        "type": yt_types.Timestamp,
        "result": int(DATE.timestamp() * 10**6)
    }, {
        "value": None,
        "type": yt_types.Timestamp,
        "result": 0
    }, {
        "value": None,
        "type": yt_types.Interval,
        "result": 0
    }, {
        "value": 10,
        "type": yt_types.Interval,
        "result": 10
    }, {
        "value": dt.timedelta(microseconds=10),
        "type": yt_types.Interval,
        "result": 10
    }, {
        "value": None,
        "type": yt_types.String,
        "result": ""
    }, {
        "value": None,
        "type": yt_types.Uint64,
        "result": 0
    }, {
        "value": "some non-integer string",
        "type": yt_types.Uint64,
        "result": 0
    }, {
        "value": {"A": 1, "B": 2},
        "type": yt_types.Yson,
        "result": {"A": 1, "B": 2}
    }, {
        "value": [1, 2, 3],
        "type": yt_types.Yson,
        "result": [1, 2, 3]
    }]
)
def test_type_conversion(test_case):
    result = yt_types.convert_to_yt_type(test_case["type"], test_case["value"])
    assert result == test_case["result"], test_case
    assert isinstance(result, type(test_case["result"])), test_case
