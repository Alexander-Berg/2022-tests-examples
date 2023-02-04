import pytest

from maps_adv.points.server.lib.data_managers.forecasts import (
    ForecastsDataManager,
    YtSyncIsNotConfigured,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def config(config):
    _config = config.copy()
    _config.update(
        {
            "YT_TOKEN": "YT_TEST_TOKEN",
            "YT_FORECASTS_TABLE": "YT_TEST_FORECASTS_TABLE",
            "YT_CLUSTER": "YT_TEST_CLUSTER",
        }
    )
    return _config


@pytest.mark.parametrize("key", ["yt_token", "yt_table", "yt_cluster"])
@pytest.mark.usefixtures("yql_table_read_iterator")
async def test_raises_if_yt_configuration_is_not_passed(key, db):
    yt_config = {
        "yt_token": "YT_TOKEN",
        "yt_table": "YT_TABLE",
        "yt_cluster": "YT_CLUSTER",
    }
    yt_config[key] = None
    dm = ForecastsDataManager(db, yt_config=yt_config)

    with pytest.raises(YtSyncIsNotConfigured):
        await dm.sync_forecasts()


async def test_will_update_empty_table(yql_table_read_iterator, forecasts_dm, con):
    yql_table_read_iterator.return_value = iter(
        [("sweev6", 1, 2, 3, 3), ("sweev7", 4, 5, 6, 6), ("sweev8", 7, 8, 9, 9)]
    )

    await forecasts_dm.sync_forecasts()

    sql = "SELECT * FROM shows_forecasts ORDER BY geohash"
    got = [dict(el) for el in await con.fetch(sql)]
    assert got == [
        {
            "geohash": "sweev6",
            "pin_shows": 1,
            "billboard_shows": 2,
            "zsb_shows": 3,
            "overview_shows": 3,
        },
        {
            "geohash": "sweev7",
            "pin_shows": 4,
            "billboard_shows": 5,
            "zsb_shows": 6,
            "overview_shows": 6,
        },
        {
            "geohash": "sweev8",
            "pin_shows": 7,
            "billboard_shows": 8,
            "zsb_shows": 9,
            "overview_shows": 9,
        },
    ]


@pytest.mark.usefixtures("fill_shows_forecasts")
async def test_sync_will_replace_all_data(yql_table_read_iterator, forecasts_dm, con):
    yql_table_read_iterator.return_value = iter(
        [("sweev6", 1, 2, 3, 3), ("sweev7", 4, 5, 6, 6), ("sweev8", 7, 8, 9, 9)]
    )

    await forecasts_dm.sync_forecasts()

    sql = "SELECT * FROM shows_forecasts ORDER BY geohash"
    got = [dict(el) for el in await con.fetch(sql)]
    assert got == [
        {
            "geohash": "sweev6",
            "pin_shows": 1,
            "billboard_shows": 2,
            "zsb_shows": 3,
            "overview_shows": 3,
        },
        {
            "geohash": "sweev7",
            "pin_shows": 4,
            "billboard_shows": 5,
            "zsb_shows": 6,
            "overview_shows": 6,
        },
        {
            "geohash": "sweev8",
            "pin_shows": 7,
            "billboard_shows": 8,
            "zsb_shows": 9,
            "overview_shows": 9,
        },
    ]


@pytest.mark.usefixtures("fill_shows_forecasts")
async def test_sync_does_not_replace_if_yt_has_no_data(
    yql_table_read_iterator, forecasts_dm, con
):
    yql_table_read_iterator.return_value = iter([])

    await forecasts_dm.sync_forecasts()

    assert await con.fetchval("SELECT COUNT(*) FROM shows_forecasts") == 13
