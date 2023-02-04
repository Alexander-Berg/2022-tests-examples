import pytest

from maps_adv.geosmb.landlord.server.lib.tasks import ImportCallTrackingTask

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def task(config, dm):
    return ImportCallTrackingTask(config=config, dm=dm)


@pytest.fixture(autouse=True)
def mock_dm_consumer(dm):
    async def gen_consumer(gen):
        async for _ in gen:
            pass

    dm.import_call_tracking_from_yt.side_effect = gen_consumer


async def test_executes_yql(mock_yql, task):
    await task

    mock_yql["query"].assert_called_with(
        """
    $biz_ids = (
        SELECT
            MIN(gco.campaign_id) as biz_id,
            gco.permalink as permalink
        FROM hahn.`//path/to/yt_orgs_table` as gco
        INNER JOIN hahn.`//path/to/yt_campaigns_table` as gc
            ON gco.campaign_id = gc.id
        WHERE Yson::ConvertToString(gc.type) = 'SMBCRM'
        GROUP BY gco.permalink as permalink
    );

    SELECT
        adv.advert_id,
        geo_data.biz_id,
        Yson::LookupString(Yson::Lookup(adv.advert_body, 'tracking'), 'formatted')
    FROM hahn.`//path/to/yt_promos_table` as adv
    INNER JOIN $biz_ids as geo_data ON geo_data.permalink = adv.permalink
    WHERE adv.advert_type = 'CALL_TRACKING' AND adv.advert_body IS NOT NULL;
    """,  # noqa
        syntax_version=1,
    )


async def test_sends_data_to_dm(task, dm, mock_yql):
    rows_written = []
    mock_yql["table_get_iterator"].return_value = [
        (100, 1221434, "phone1"),
        (101, 4563765, "phone2"),
        (102, 2314534, "phone3"),
    ]

    async def consumer(generator):
        nonlocal rows_written
        async for records in generator:
            rows_written.extend(records)

        return rows_written

    dm.import_call_tracking_from_yt.side_effect = consumer

    await task()

    assert rows_written == [
        (100, 1221434, "phone1"),
        (101, 4563765, "phone2"),
        (102, 2314534, "phone3"),
    ]
