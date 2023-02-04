from datetime import datetime, timezone

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.doorman.server.lib.enums import CallEvent

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
async def fill_db(factory, con):
    await factory.create_client(biz_id=123, client_id=456)

    await factory.create_import_call_events_tmp_table()
    await con.copy_records_to_table(
        "import_call_events_tmp",
        records=[
            (
                111,
                2222222,  # permalink
                "79011111111",  # phone
                datetime(2019, 1, 1, 11, 11, 11, tzinfo=timezone.utc),
                110,
                10,
                "Success",
                444,
                "http://record-url-1",
            ),
            (
                555,
                666666,  # permalink
                "7922222222",  # phone
                datetime(2019, 2, 2, 22, 22, 22, tzinfo=timezone.utc),
                220,
                20,
                "NoAnswer",
                777,
                "http://record-url-2",
            ),
        ],
        columns=[
            "geoproduct_id",
            "permalink",
            "client_phone",
            "event_timestamp",
            "await_duration",
            "talk_duration",
            "event_value",
            "session_id",
            "record_url",
        ],
    )


@pytest.mark.parametrize(
    "clients",
    [
        [],
        [
            (
                9999999,
                "79011111111",
                123,
                456,
            ),
        ],  # permalink  # phone
        [
            (
                2222222,
                "79099999999",
                123,
                456,
            ),
        ],  # permalink  # phone
    ],
)
async def test_uploads_nothing_if_no_matches(factory, con, dm, clients):
    await dm.upload_imported_call_events(clients)

    got = await con.fetchval("SELECT COUNT(*) FROM call_events")
    assert got == 0


async def test_uploads_matched_call_events(con, dm):
    await dm.upload_imported_call_events(
        [
            (
                2222222,  # permalink
                "79011111111",  # phone
                123,  # biz_id
                456,  # client_id
            )
        ]
    )

    rows = await con.fetch("SELECT * FROM call_events")
    assert [dict(row) for row in rows] == [
        dict(
            id=Any(int),
            client_id=456,
            biz_id=123,
            event_type=CallEvent.FINISHED,
            event_value="Success",
            event_timestamp=datetime(2019, 1, 1, 11, 11, 11, tzinfo=timezone.utc),
            source="GEOADV_PHONE_CALL",
            session_id=444,
            record_url="http://record-url-1",
            await_duration=110,
            talk_duration=10,
            geoproduct_id=111,
            created_at=Any(datetime),
        )
    ]


async def test_uploads_call_events_of_the_same_client(con, dm):
    await con.copy_records_to_table(
        "import_call_events_tmp",
        records=[
            (
                3,
                12345,  # permalink
                "7988888888",  # phone
                datetime(2019, 3, 3, 3, 3, 3, tzinfo=timezone.utc),
                330,
                30,
                "Error",
                33,
                "http://record-url-3",
            ),
            (
                4,
                12345,  # permalink
                "7988888888",  # phone
                datetime(2019, 4, 4, 4, 4, 4, tzinfo=timezone.utc),
                440,
                40,
                "Unknown",
                44,
                "http://record-url-4",
            ),
        ],
        columns=[
            "geoproduct_id",
            "permalink",
            "client_phone",
            "event_timestamp",
            "await_duration",
            "talk_duration",
            "event_value",
            "session_id",
            "record_url",
        ],
    )

    await dm.upload_imported_call_events(
        [
            (
                12345,
                "7988888888",
                123,
                456,
            )
        ]  # permalink  # phone  # biz_id  # client_id
    )

    rows = await con.fetch("SELECT * FROM call_events ORDER BY geoproduct_id")
    assert [dict(row) for row in rows] == [
        dict(
            id=Any(int),
            client_id=456,
            biz_id=123,
            event_type=CallEvent.FINISHED,
            event_value="Error",
            event_timestamp=datetime(2019, 3, 3, 3, 3, 3, tzinfo=timezone.utc),
            source="GEOADV_PHONE_CALL",
            session_id=33,
            record_url="http://record-url-3",
            await_duration=330,
            talk_duration=30,
            geoproduct_id=3,
            created_at=Any(datetime),
        ),
        dict(
            id=Any(int),
            client_id=456,
            biz_id=123,
            event_type=CallEvent.FINISHED,
            event_value="Unknown",
            event_timestamp=datetime(2019, 4, 4, 4, 4, 4, tzinfo=timezone.utc),
            source="GEOADV_PHONE_CALL",
            session_id=44,
            record_url="http://record-url-4",
            await_duration=440,
            talk_duration=40,
            geoproduct_id=4,
            created_at=Any(datetime),
        ),
    ]
