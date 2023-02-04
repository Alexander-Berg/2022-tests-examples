from datetime import datetime

import asyncpg
import pytest

from maps_adv.common.helpers import Any

pytestmark = [pytest.mark.asyncio]


async def test_creates_operation_record_in_db(dm, con, factory):
    request_id = await factory.create_request()

    await dm.create_operation(
        request_id=request_id,
        service_name="miracle",
        metadata={"smth": "nothing"},
        is_success=True,
    )

    rows = await con.fetch("SELECT * FROM delete_operations")
    assert [dict(row) for row in rows] == [
        {
            "id": Any(int),
            "request_id": request_id,
            "service_name": "miracle",
            "metadata": {"smth": "nothing"},
            "is_success": True,
            "created_at": Any(datetime),
        }
    ]


async def test_returns_nothing(dm, factory):
    request_id = await factory.create_request()

    got = await dm.create_operation(
        request_id=request_id,
        service_name="miracle",
        metadata={"smth": "nothing"},
        is_success=True,
    )

    assert got is None


async def test_raises_for_unknown_request(dm):
    with pytest.raises(asyncpg.ForeignKeyViolationError):
        await dm.create_operation(
            request_id=111,
            service_name="miracle",
            metadata={"smth": "nothing"},
            is_success=True,
        )
