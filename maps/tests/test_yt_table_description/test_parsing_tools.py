import pytz
import datetime as dt
from pymongo.database import Database

from maps.garden.tools.stat_updater.lib.yt_table_descriptions.yt_types import (
    String, Uint64, Timestamp, Interval, Yson
)
from maps.garden.tools.stat_updater.lib.yt_table_descriptions import parsing_tools

KEY_FIELD_NAME = "time"
KEY_FIELD_PATH = ("my", "time")
COLLECTION_NAME = "collection"

YT_TABLE_DESCRIPTION = {
    COLLECTION_NAME: [
        parsing_tools.Field(parsing_tools.Key(KEY_FIELD_NAME), Timestamp, KEY_FIELD_PATH),
        parsing_tools.Field("name_field", String, "name"),
        parsing_tools.Field("lambda_field", Interval, lambda d: d["finished_at"] - d["started_at"]),
        parsing_tools.Field("path_field", String, ("logs", -1, "status")),
        parsing_tools.Field("list_field", Yson, "logs"),
    ],
    None: [
        parsing_tools.Field("some_field", Uint64),
    ]
}


def _get_key_field_mongo(value):
    return {
        KEY_FIELD_PATH[0]: {
            KEY_FIELD_PATH[1]: value
        }
    }


def test_schema():
    return parsing_tools.convert_to_yt_schema(YT_TABLE_DESCRIPTION).to_yson_type()


def test_get_mongo_key_field_name():
    assert parsing_tools.get_mongo_key_field_name(YT_TABLE_DESCRIPTION) == ".".join(KEY_FIELD_PATH)


def test_get_key_field():
    assert parsing_tools.get_key_field(YT_TABLE_DESCRIPTION) == KEY_FIELD_NAME


def test_mongo_mapper(db: Database):
    collection = db[COLLECTION_NAME]
    date_fild = dt.datetime(year=2010, month=12, day=5, tzinfo=pytz.UTC)
    collection.insert_many([
        {
            "name": "name1",
            "started_at": date_fild + dt.timedelta(seconds=10),
            "finished_at": date_fild + dt.timedelta(seconds=20),
            "logs": [{}, {"status": "success"}],
            **_get_key_field_mongo(date_fild),
        },
        {
            "name": "name2",
            "started_at": date_fild + dt.timedelta(hours=1, seconds=10),
            "finished_at": date_fild + dt.timedelta(hours=1, seconds=20),
            "logs": [{}, {"status": "fail"}],
            **_get_key_field_mongo(date_fild + dt.timedelta(hours=1)),
        },
        {
            "name": "name3",
            "started_at": date_fild + dt.timedelta(days=1, hours=1, seconds=10),
            "finished_at": date_fild + dt.timedelta(days=1, hours=1, seconds=20),
            "logs": [{}, {"status": "warning"}],
            **_get_key_field_mongo(date_fild + dt.timedelta(days=1, hours=1))
        },
    ])

    return list(parsing_tools.mongo_mapper(
        db=db,
        export_date=date_fild,
        description=YT_TABLE_DESCRIPTION
    ))
