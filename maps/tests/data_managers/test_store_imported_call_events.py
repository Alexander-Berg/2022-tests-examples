from datetime import datetime, timezone

import pytest
from smb.common.testing_utils import Any

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "records, expected_rows",
    [
        # no records
        ([], []),
        # one record
        (
            [
                (
                    111,
                    222,
                    "7900000000",
                    datetime(2019, 7, 1, 12, 0, 0, tzinfo=timezone.utc),
                    190,
                    90,
                    "Success",
                    444,
                    "http://record-url",
                )
            ],
            [
                dict(
                    id=Any(int),
                    geoproduct_id=111,
                    permalink=222,
                    client_phone="7900000000",
                    event_timestamp=datetime(2019, 7, 1, 12, 0, 0, tzinfo=timezone.utc),
                    await_duration=190,
                    talk_duration=90,
                    event_value="Success",
                    session_id=444,
                    record_url="http://record-url",
                )
            ],
        ),
        # without optional fields
        (
            [
                (
                    111,
                    222,
                    "7900000000",
                    datetime(2019, 7, 1, 12, 0, 0, tzinfo=timezone.utc),
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
                    datetime(2020, 1, 1, 11, 11, 11, tzinfo=timezone.utc),
                    None,
                    None,
                    "NoAnswer",
                    None,
                    None,
                ),
            ],
            [
                dict(
                    id=Any(int),
                    geoproduct_id=111,
                    permalink=222,
                    client_phone="7900000000",
                    event_timestamp=datetime(2019, 7, 1, 12, 0, 0, tzinfo=timezone.utc),
                    await_duration=190,
                    talk_duration=90,
                    event_value="Success",
                    session_id=444,
                    record_url="http://record-url",
                ),
                dict(
                    id=Any(int),
                    geoproduct_id=555,
                    permalink=666,
                    client_phone="7911111111",
                    event_timestamp=datetime(
                        2020, 1, 1, 11, 11, 11, tzinfo=timezone.utc
                    ),
                    await_duration=None,
                    talk_duration=None,
                    event_value="NoAnswer",
                    session_id=None,
                    record_url=None,
                ),
            ],
        ),
    ],
)
async def test_stores_records_in_table(factory, con, dm, records, expected_rows):
    await factory.create_import_call_events_tmp_table()

    await dm.store_imported_call_events(records)

    rows = await con.fetch(
        "SELECT * FROM import_call_events_tmp ORDER BY geoproduct_id"
    )
    assert [dict(row) for row in rows] == expected_rows
