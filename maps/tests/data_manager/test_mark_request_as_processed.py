from datetime import datetime

import pytest

from maps_adv.common.helpers import Any, dt

pytestmark = [pytest.mark.asyncio]


async def test_marks_db_record_as_processed(factory, dm, con):
    record_id = await factory.create_request(passport_uid=111)

    await dm.mark_request_as_processed(
        record_id=record_id, processed_at=dt("2020-01-01 00:00:00")
    )

    rows = await con.fetch("SELECT * FROM delete_requests")
    assert [dict(row) for row in rows] == [
        {
            "id": record_id,
            "external_id": "abc",
            "passport_uid": 111,
            "processed_at": dt("2020-01-01 00:00:00"),
            "created_at": Any(datetime),
        }
    ]


async def test_does_not_affect_other_records(factory, dm, con):
    record_id = await factory.create_request()

    await dm.mark_request_as_processed(
        record_id=record_id + 1, processed_at=dt("2020-01-01 00:00:00")
    )

    rows = await con.fetch("SELECT * FROM delete_requests")
    assert [dict(row) for row in rows] == [
        {
            "id": record_id,
            "external_id": "abc",
            "passport_uid": 123,
            "processed_at": None,
            "created_at": Any(datetime),
        }
    ]


async def test_returns_nothing(factory, dm):
    record_id = await factory.create_request()

    got = await dm.mark_request_as_processed(
        record_id=record_id, processed_at=dt("2020-01-01 00:00:00")
    )

    assert got is None
