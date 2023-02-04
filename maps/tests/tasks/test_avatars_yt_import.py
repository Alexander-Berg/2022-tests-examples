import pytest

from maps_adv.geosmb.landlord.server.lib.tasks import ImportAvatarsTask

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def task(config, dm):
    return ImportAvatarsTask(config=config, dm=dm)


@pytest.fixture(autouse=True)
def mock_dm_consumer(dm):
    async def gen_consumer(gen):
        async for _ in gen:
            pass

    dm.import_avatars_from_yt.side_effect = gen_consumer


async def test_executes_yql(mock_yql, task):
    await task

    mock_yql["query"].assert_called_with(
        """
    SELECT source_url, avatars_group_id, avatars_name
    FROM hahn.`//path/to/geoadv_tables/stream_snapshots/arbitrage_photo`
    WHERE Yson::ConvertToString(source_type) = 'site'
    AND avatars_group_id IS NOT NULL
    """,
        syntax_version=1,
    )


async def test_sends_data_to_dm(task, dm, mock_yql):
    rows_written = []
    mock_yql["table_get_iterator"].return_value = [
        ("url-1", 1221434, "name-1"),
        ("url-2", 4563765, "name-2"),
    ]

    async def consumer(generator):
        nonlocal rows_written
        async for records in generator:
            rows_written.extend(records)

        return rows_written

    dm.import_avatars_from_yt.side_effect = consumer

    await task()

    assert rows_written == [
        ("url-1", 1221434, "name-1"),
        ("url-2", 4563765, "name-2"),
    ]
