import pytest

from maps_adv.geosmb.landlord.server.lib.tasks import SyncPermalinksTask

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def task(config, dm):
    return SyncPermalinksTask(config=config, dm=dm)


@pytest.fixture(autouse=True)
def mock_dm_consumer(dm):
    async def gen_consumer(gen):
        async for _ in gen:
            pass

    dm.sync_permalinks_from_yt.side_effect = gen_consumer


async def test_executes_yql(mock_yql, task):
    await task

    mock_yql["query"].assert_called_with(
        """
    select biz_id, bs.permalink as permalink
    from hahn.`//path/to/landlord_tables/streaming_snapshots/biz_state` as ll
    join hahn.`//path/to/geoadv_tables/stream_snapshots/geoadv_business_snapshot` as bs on bs.campaign_id = ll.biz_id
    where bs.permalink != 0
    and (ll.permalink = '0' or (cast(ll.permalink as int64) = bs.id and bs.id != bs.permalink));
    """,
        syntax_version=1,
    )


async def test_sends_data_to_dm(task, dm, mock_yql):
    rows_written = []
    mock_yql["table_get_iterator"].return_value = [
        (501, 701),
        (502, 702),
    ]

    async def consumer(generator):
        nonlocal rows_written
        async for records in generator:
            rows_written.extend(records)

        return rows_written

    dm.sync_permalinks_from_yt.side_effect = consumer

    await task()

    assert rows_written == [
        (501, 701),
        (502, 702),
    ]
