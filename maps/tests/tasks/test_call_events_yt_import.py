from datetime import datetime

import pytest
import pytz
from smb.common.testing_utils import Any

from maps_adv.common.helpers import coro_mock
from maps_adv.geosmb.doorman.server.lib.enums import CallEvent
from maps_adv.geosmb.doorman.server.lib.tasks import CallEventsImportTask

pytestmark = [pytest.mark.asyncio]

moscow_timezone = pytz.timezone("Europe/Moscow")


@pytest.fixture
def mock_domain(domain):
    async def gen_consumer(gen):
        async for _ in gen:
            pass

    class MockDomain:
        import_call_events = coro_mock()

    _mock_domain = MockDomain()
    _mock_domain.import_call_events.side_effect = gen_consumer

    return _mock_domain


@pytest.fixture
def task(request, config, dm):
    domain_fixture_name = (
        "mock_domain" if request.node.get_closest_marker("mock_domain") else "domain"
    )

    return CallEventsImportTask(
        config=config, dm=dm, domain=request.getfixturevalue(domain_fixture_name)
    )


@pytest.mark.mock_domain
async def test_executes_yql(mock_yql, dm, task):
    await task

    mock_yql["query"].assert_called_with(
        """
        DECLARE $last_imported_id AS Int64;

        $campaigns = (
            SELECT campaign_organization.campaign_id as campaign_id
            FROM fake_cluster.`//fake/campaign-org` AS campaign_organization
            JOIN fake_cluster.`//fake/campaign` AS campaign
                ON campaign.id=campaign_organization.campaign_id
            where campaign.chain_permalink IS NULL
            GROUP BY campaign_organization.campaign_id
            HAVING count(*) = 1
        );

        SELECT
            calls_stat.id AS geoproduct_id,
            advert_orgs.permalink,
            calls_stat.source_number AS client_phone,
            calls_stat.time AS event_timestamp,
            calls_stat.await_duration,
            calls_stat.talk_duration,
            calls_stat.call_result,
            calls_stat.session_id,
            calls_stat.record_url
        FROM fake_cluster.`//fake/calls-stat` AS calls_stat
        JOIN fake_cluster.`//fake/calls-tracking` AS calls_tracking
            ON calls_tracking.tracking_number=calls_stat.masked_number
            AND calls_tracking.original_number=calls_stat.original_number
        JOIN fake_cluster.`//fake/advert-orgs` AS advert_orgs
            ON advert_orgs.advert_id=calls_tracking.external_id
        JOIN fake_cluster.`//fake/advert` AS advert
            ON advert.id=calls_tracking.external_id
        JOIN $campaigns AS camp ON camp.campaign_id = advert.campaign_id
        WHERE calls_stat.id > $last_imported_id
    """,
        syntax_version=1,
    )


@pytest.mark.mock_dm
@pytest.mark.mock_domain
@pytest.mark.parametrize(
    "last_imported_id, expected_param", [(None, "0"), (123, "123")]
)
async def test_runs_yql_with_expected_params(
    mock_yql, dm, task, last_imported_id, expected_param
):
    dm.fetch_max_geoproduct_id_for_call_events.coro.return_value = last_imported_id

    await task

    mock_yql["request_run"].assert_called_with(
        parameters={"$last_imported_id": f'{{"Data": "{expected_param}"}}'}
    )


@pytest.mark.mock_domain
async def test_sends_data_to_domain(task, dm, mock_yql, mock_domain):
    rows_written = []
    mock_yql["table_get_iterator"].return_value = [
        (
            111,
            222,
            "7900000000",
            "2019-07-01T12:00:00Z",
            190,
            90,
            "Success",
            444,
            "http://record-url",
        ),
        (
            555,
            666,
            "7911111111",
            "2020-01-01T11:11:11Z",
            None,
            None,
            "NoAnswer",
            None,
            None,
        ),
    ]

    async def consumer(generator):
        nonlocal rows_written
        async for records in generator:
            rows_written.extend(records)

        return rows_written

    mock_domain.import_call_events.side_effect = consumer

    await task()

    assert rows_written == [
        (
            111,
            222,
            "7900000000",
            moscow_timezone.localize(datetime(2019, 7, 1, 12, 0, 0)),
            190,
            90,
            "Success",
            444,
            "http://record-url",
        ),
        (
            555,
            666,
            "7911111111",
            moscow_timezone.localize(datetime(2020, 1, 1, 11, 11, 11)),
            None,
            None,
            "NoAnswer",
            None,
            None,
        ),
    ]


async def test_stored_imported_events_in_db(mock_yql, con, bvm, task):
    bvm.fetch_biz_id_by_permalink.coro.side_effect = [123, 456]

    mock_yql["table_get_iterator"].return_value = [
        (
            111,
            222,
            "7900000000",
            "2019-01-01T11:11:11Z",
            190,
            90,
            "Success",
            444,
            "http://record-url",
        ),
        (
            555,
            666,
            "7911111111",
            "2020-02-02T22:22:22Z",
            None,
            None,
            "NoAnswer",
            None,
            None,
        ),
    ]

    await task()

    events = await con.fetch("SELECT * FROM call_events ORDER BY biz_id")

    assert [dict(event) for event in events] == [
        dict(
            id=Any(int),
            client_id=Any(int),
            biz_id=123,
            event_type=CallEvent.FINISHED,
            event_value="Success",
            event_timestamp=moscow_timezone.localize(datetime(2019, 1, 1, 11, 11, 11)),
            source="GEOADV_PHONE_CALL",
            session_id=444,
            record_url="http://record-url",
            await_duration=190,
            talk_duration=90,
            geoproduct_id=111,
            created_at=Any(datetime),
        ),
        dict(
            id=Any(int),
            client_id=Any(int),
            biz_id=456,
            event_type=CallEvent.FINISHED,
            event_value="NoAnswer",
            event_timestamp=moscow_timezone.localize(datetime(2020, 2, 2, 22, 22, 22)),
            source="GEOADV_PHONE_CALL",
            session_id=None,
            record_url=None,
            await_duration=None,
            talk_duration=None,
            geoproduct_id=555,
            created_at=Any(datetime),
        ),
    ]


async def test_creates_clients_for_imported_events(mock_yql, con, bvm, task):
    bvm.fetch_biz_id_by_permalink.coro.side_effect = [123, 456]

    mock_yql["table_get_iterator"].return_value = [
        (
            111,
            222,
            "7900000000",
            "2019-01-01T11:11:11Z",
            190,
            90,
            "Success",
            444,
            "http://record-url",
        ),
        (
            555,
            666,
            "7911111111",
            "2020-02-02T22:22:22Z",
            None,
            None,
            "NoAnswer",
            None,
            None,
        ),
    ]

    await task()

    clients = await con.fetch("SELECT * FROM clients ORDER BY biz_id")

    assert [dict(client) for client in clients] == [
        dict(
            id=Any(int),
            biz_id=123,
            phone="7900000000",
            first_name=None,
            last_name=None,
            passport_uid=None,
            email=None,
            gender=None,
            comment=None,
            labels=[],
            cleared_for_gdpr=False,
            ts_storage=Any(str),
            created_at=Any(datetime),
        ),
        dict(
            id=Any(int),
            biz_id=456,
            phone="7911111111",
            first_name=None,
            last_name=None,
            passport_uid=None,
            email=None,
            gender=None,
            comment=None,
            labels=[],
            cleared_for_gdpr=False,
            ts_storage=Any(str),
            created_at=Any(datetime),
        ),
    ]


async def test_not_duplicates_events_for_single_client(task, con, mock_yql):
    mock_yql["table_get_iterator"].return_value = [
        (
            1000,
            222,  # permalink
            "7911111111",
            "2020-01-01T11:11:11Z",
            10,
            0,
            "NoAnswer",
            444,
            "http://record-url-1",
        ),
        (
            2000,
            222,  # permalink
            "7911111111",
            "2020-01-01T12:12:12Z",
            10,
            20,
            "Success",
            555,
            "http://record-url-2",
        ),
    ]

    await task()

    events = await con.fetch("SELECT * FROM call_events ORDER BY event_timestamp")
    assert [dict(event) for event in events] == [
        dict(
            id=Any(int),
            client_id=Any(int),
            biz_id=15,
            event_type=CallEvent.FINISHED,
            event_value="NoAnswer",
            event_timestamp=moscow_timezone.localize(datetime(2020, 1, 1, 11, 11, 11)),
            source="GEOADV_PHONE_CALL",
            session_id=444,
            record_url="http://record-url-1",
            await_duration=10,
            talk_duration=0,
            geoproduct_id=1000,
            created_at=Any(datetime),
        ),
        dict(
            id=Any(int),
            client_id=Any(int),
            biz_id=15,
            event_type=CallEvent.FINISHED,
            event_value="Success",
            event_timestamp=moscow_timezone.localize(datetime(2020, 1, 1, 12, 12, 12)),
            source="GEOADV_PHONE_CALL",
            session_id=555,
            record_url="http://record-url-2",
            await_duration=10,
            talk_duration=20,
            geoproduct_id=2000,
            created_at=Any(datetime),
        ),
    ]
