import pytz
import pytest
import datetime as dt
from collections import defaultdict
from collections.abc import Iterator
from pymongo.database import Database

from maps.garden.sdk.yt import utils as yt_utils
from maps.garden.tools.stat_updater.lib.yt_table_descriptions.parsing_tools import Key, Field as F
from maps.garden.tools.stat_updater.lib.yt_table_descriptions.yt_types import (
    String, Uint64, Timestamp, Yson
)
from maps.garden.tools.stat_updater.lib import export_to_yt
from maps.garden.tools.stat_updater.lib.yt_stat_series import DATE_PATTERN

KEY_FIELD_NAME = "time"
COLLECTION_NAME = "collection"
BASE_PATH = "//home/stat"
BASE_DATE = dt.datetime(year=2010, month=12, day=5, tzinfo=pytz.UTC)

YT_TABLE_DESCRIPTIONS = {
    f"series{i}": {
        f"collection{i}": [
            F(Key(KEY_FIELD_NAME), Timestamp),
            F(f"other_field{i}", String, "other_field"),
        ],
        None: [
            F(f"some_field{i}", Uint64),
            F(f"dict_field{i}", Yson),
        ]
    }
    for i in range(2)
}

BUILDS_STATISTICS_DESCRIPTION = {
    None: [
        F(Key("some_field"), String),
    ]
}


class FakeYtStatSeries:
    Storage = defaultdict(dict)

    def __init__(
        self,
        yt_series_path,
        yt_client,
        schema,
        key_field
    ):
        self.yt_series_path = yt_series_path

    def get_last_table_date(self, *args, **kwargs):
        return None

    def save_for_date(self, date: dt.datetime, rows: Iterator[dict]):
        rows = list(rows)
        FakeYtStatSeries.Storage[self.yt_series_path][date.strftime(DATE_PATTERN)] = rows


class FakeBuildsStatisticsCreator:
    def __init__(self, *args, **kwargs):
        pass

    def execute(self, export_date):
        if export_date == BASE_DATE - dt.timedelta(days=3):
            yield {"some_field": "usefull_value"}


@pytest.mark.freeze_time(BASE_DATE)
def test_export_to_yt(db: Database, mocker, environment_settings):
    for i in range(2):
        collection = db[f"{COLLECTION_NAME}{i}"]
        collection.insert_many([
            {
                KEY_FIELD_NAME: BASE_DATE - dt.timedelta(days=2),
                "other_field": f"name{i*2}-{1}",
            }, {
                KEY_FIELD_NAME: BASE_DATE - dt.timedelta(days=1),
                "other_field": f"name{i*2}-{2}",
            }, {
                KEY_FIELD_NAME: BASE_DATE - dt.timedelta(hours=23),
                "other_field": f"name{i*2}-{3}",
            }, {
                # not displayed in the result because don't process today's logs
                KEY_FIELD_NAME: BASE_DATE,
                "other_field": f"name{i*2}-{4}",
            },
        ])

    yt_config = yt_utils.get_server_settings(yt_utils.get_yt_settings(environment_settings), "hahn")["yt_config"]
    mocker.patch(
        "maps.garden.tools.stat_updater.lib.export_to_yt.YT_TABLE_DESCRIPTIONS",
        YT_TABLE_DESCRIPTIONS
    )
    mocker.patch(
        "maps.garden.tools.stat_updater.lib.export_to_yt.BUILDS_STATISTICS_DESCRIPTION",
        BUILDS_STATISTICS_DESCRIPTION
    )
    mocker.patch(
        "maps.garden.tools.stat_updater.lib.export_to_yt.YTStatSeries",
        FakeYtStatSeries
    )
    mocker.patch(
        "maps.garden.tools.stat_updater.lib.export_to_yt.BuildsStatisticsCreator",
        FakeBuildsStatisticsCreator
    )
    export_to_yt._export_to_yt(db, yt_config, BASE_PATH, max_days_ago=10)

    return FakeYtStatSeries.Storage
