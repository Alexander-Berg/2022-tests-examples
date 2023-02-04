import pytest
import pytz
import datetime as dt

from yt.wrapper.client import YtClient
from yt.wrapper.ypath import ypath_join

from maps.garden.tools.stat_updater.lib.yt_stat_series import YTStatSeries, DATE_PATTERN

YT_SERVER = "hahn"
BASE_PATH = "//home/stat"
KEY_FIELD_NAME = "id"

SCHEMA = [
    {
        "name": KEY_FIELD_NAME,
        "required": "true",
        "type": "uint64"
    }
]

ROWS = [
    {KEY_FIELD_NAME: 3},
    {KEY_FIELD_NAME: 1},
    {KEY_FIELD_NAME: 2},
]

DATE = dt.datetime(year=2010, month=10, day=1, tzinfo=pytz.utc)


@pytest.mark.use_local_yt("hahn")
def test_scheme(environment_settings: dict, yt_client: YtClient):
    series_name = "my_series"
    yt_stat_series = YTStatSeries(
        yt_series_path=ypath_join(BASE_PATH, series_name),
        yt_client=yt_client,
        schema=SCHEMA,
        key_field=KEY_FIELD_NAME
    )
    yt_stat_series.save_for_date(DATE, ROWS)
    table_path = ypath_join(BASE_PATH, series_name, DATE.strftime(DATE_PATTERN))
    schema_path = ypath_join(table_path, "@schema")
    schema = yt_client.get(schema_path)
    yt_client.remove(table_path)
    return schema


@pytest.mark.use_local_yt("hahn")
def test_yt_stat_series(environment_settings: dict, yt_client: YtClient):
    series_name = "my_series"
    yt_stat_series = YTStatSeries(
        yt_series_path=ypath_join(BASE_PATH, series_name),
        yt_client=yt_client,
        schema=SCHEMA,
        key_field=KEY_FIELD_NAME
    )
    assert yt_stat_series.get_last_table_date() is None
    yt_stat_series.save_for_date(DATE, ROWS)
    yt_stat_series.save_for_date(DATE - dt.timedelta(days=1), ROWS)

    assert yt_stat_series.get_last_table_date() == DATE
    table_path = ypath_join(BASE_PATH, series_name, DATE.strftime(DATE_PATTERN))
    assert yt_client.get(ypath_join(table_path, "@sorted_by")) == [KEY_FIELD_NAME]
    assert yt_client.get(ypath_join(table_path, "@optimize_for")) == "scan"
    return list(yt_client.read_table(table_path))
