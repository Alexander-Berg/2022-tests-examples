from decimal import Decimal

import pytest

from maps_adv.geosmb.landlord.server.lib.tasks import ImportPromotedServicesTask

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def task(config, dm):
    return ImportPromotedServicesTask(config=config, dm=dm)


@pytest.fixture(autouse=True)
def mock_dm_consumer(dm):
    async def gen_consumer(gen):
        async for _ in gen:
            pass

    dm.import_promoted_services_from_yt.side_effect = gen_consumer


async def test_executes_yql(mock_yql, task):
    await task

    mock_yql["query"].assert_called_with(
        """
    pragma yson.disablestrict;

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
        Yson::LookupString(adv.advert_body, 'title'),
        Yson::LookupString(adv.advert_body, 'price'),
        Yson::LookupString(Yson::Lookup(adv.advert_body, 'image'), 'href'),
        Yson::LookupString(adv.advert_body, 'url'),
        Yson::LookupString(adv.advert_body, 'text')
    FROM hahn.`//path/to/yt_promos_table` as adv
    INNER JOIN $biz_ids as geo_data ON geo_data.permalink = adv.permalink
    WHERE adv.advert_type = 'PRODUCT';
    """,
        syntax_version=1,
    )


async def test_sends_data_to_dm(task, dm, mock_yql):
    rows_written = []
    mock_yql["table_get_iterator"].return_value = [
        (100, 9870, "title1", "5 100 500", "image1", "url1", "desc1"),
        (101, 9871, "title2", None, "image2", None, None),
        (102, 9872, "title3", "23,42", None, None, None),
    ]

    async def consumer(generator):
        nonlocal rows_written
        async for records in generator:
            rows_written.extend(records)

        return rows_written

    dm.import_promoted_services_from_yt.side_effect = consumer

    await task()

    assert rows_written == [
        (100, 9870, "title1", Decimal("5100500"), "image1", "url1", "desc1"),
        (101, 9871, "title2", None, "image2", None, None),
        (102, 9872, "title3", Decimal("23.42"), None, None, None),
    ]
