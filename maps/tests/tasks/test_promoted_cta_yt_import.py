import pytest

from maps_adv.geosmb.landlord.server.lib.tasks import ImportPromotedCtaTask

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def task(config, dm):
    return ImportPromotedCtaTask(config=config, dm=dm)


@pytest.fixture(autouse=True)
def mock_dm_consumer(dm):
    async def gen_consumer(gen):
        async for _ in gen:
            pass

    dm.import_promoted_cta_from_yt.side_effect = gen_consumer


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

    $cta_adverts = (
        SELECT permalink, advert_id
        , Yson::YPathString(advert_body, '/link') as link
        , Yson::YPathString(advert_body, '/title') as title
        FROM hahn.`//path/to/yt_promos_table`
        WHERE advert_type = 'CLICK_TO_ACTION'
    );

    SELECT adv.advert_id as advert_id
    , biz.biz_id as biz_id
    , adv.title as title
    , if(adv.link != '', adv.link, 'http://widget_url/' || cast(biz.permalink as String)) as link
    FROM $biz_ids as biz
    JOIN $cta_adverts as adv on adv.permalink = biz.permalink
    """,  # noqa
        syntax_version=1,
    )


async def test_sends_data_to_dm(task, dm, mock_yql):
    rows_written = []
    mock_yql["table_get_iterator"].return_value = [
        (100, 1221434, "title1", "link1"),
        (101, 4563765, "title2", "link2"),
        (102, 2314534, "title3", "link3"),
    ]

    async def consumer(generator):
        nonlocal rows_written
        async for records in generator:
            rows_written.extend(records)

        return rows_written

    dm.import_promoted_cta_from_yt.side_effect = consumer

    await task()

    assert rows_written == [
        (100, 1221434, "title1", "link1"),
        (101, 4563765, "title2", "link2"),
        (102, 2314534, "title3", "link3"),
    ]
