import pytest

from maps_adv.geosmb.landlord.server.lib.tasks import ImportPromosTask

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def task(config, dm):
    return ImportPromosTask(config=config, dm=dm)


@pytest.fixture(autouse=True)
def mock_dm_consumer(dm):
    async def gen_consumer(gen):
        async for _ in gen:
            pass

    dm.import_promos_from_yt.side_effect = gen_consumer


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
        Yson::LookupString(adv.advert_body, 'announcement'),
        Yson::LookupString(adv.advert_body, 'description'),
        Yson::LookupString(adv.advert_body, 'date_from'),
        Yson::LookupString(adv.advert_body, 'date_to'),
        Yson::LookupString(COALESCE(Yson::Lookup(adv.advert_body, 'image'),
                                    Yson::Lookup(adv.advert_body, 'user_squared_image')),
                           'href'),
        CASE WHEN Yson::LookupString(adv.advert_body, 'link') = ''
            THEN NULL
            ELSE Yson::LookupString(adv.advert_body, 'link')
        END
    FROM hahn.`//path/to/yt_promos_table` as adv
    INNER JOIN $biz_ids as geo_data ON geo_data.permalink = adv.permalink
    WHERE adv.advert_type = 'PROMOTION';
    """,
        syntax_version=1,
    )


async def test_sends_data_to_dm(task, dm, mock_yql):
    rows_written = []
    mock_yql["table_get_iterator"].return_value = [
        (
            100,
            1221434,
            "announcement1",
            "description1",
            "date_from1",
            "date_to1",
            None,
            None,
        ),
        (
            101,
            4563765,
            "announcement2",
            "description2",
            "date_from2",
            "date_to2",
            "image_url2",
            None,
        ),
        (
            102,
            2314534,
            "announcement3",
            "description3",
            "date_from3",
            "date_to3",
            "image_url3",
            "link3",
        ),
    ]

    async def consumer(generator):
        nonlocal rows_written
        async for records in generator:
            rows_written.extend(records)

        return rows_written

    dm.import_promos_from_yt.side_effect = consumer

    await task()

    assert rows_written == [
        (
            100,
            1221434,
            "announcement1",
            "description1",
            "date_from1",
            "date_to1",
            None,
            None,
        ),
        (
            101,
            4563765,
            "announcement2",
            "description2",
            "date_from2",
            "date_to2",
            "image_url2",
            None,
        ),
        (
            102,
            2314534,
            "announcement3",
            "description3",
            "date_from3",
            "date_to3",
            "image_url3",
            "link3",
        ),
    ]
