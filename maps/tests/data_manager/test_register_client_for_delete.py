from datetime import datetime

import pytest

from maps_adv.common.helpers import Any

pytestmark = [pytest.mark.asyncio]


async def test_saves_request_to_db(dm, con):
    await dm.register_client_for_delete(external_id="abc", passport_uid=999)

    rows = await con.fetch("SELECT * FROM delete_requests")
    assert [dict(row) for row in rows] == [
        {
            "id": Any(int),
            "external_id": "abc",
            "passport_uid": 999,
            "processed_at": None,
            "created_at": Any(datetime),
        }
    ]


async def test_returns_nothing(dm):
    got = await dm.register_client_for_delete(external_id="abc", passport_uid=999)

    assert got is None
