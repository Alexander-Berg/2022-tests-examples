from datetime import datetime

import pytest
from asyncpg import ForeignKeyViolationError
from smb.common.testing_utils import Any, dt

from maps_adv.geosmb.doorman.server.lib.enums import CallEvent, Source

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "params, expected",
    [
        # full
        (
            dict(
                client_id=111,
                biz_id=123,
                event_type=CallEvent.INITIATED,
                event_value="abc",
                event_timestamp=dt("2020-01-01 00:00:00"),
                source=Source.GEOADV_PHONE_CALL,
                session_id=456,
            ),
            dict(
                id=Any(int),
                client_id=111,
                biz_id=123,
                event_type=CallEvent.INITIATED,
                event_value="abc",
                event_timestamp=dt("2020-01-01 00:00:00"),
                source="GEOADV_PHONE_CALL",
                created_at=Any(datetime),
                session_id=456,
                record_url=None,
                await_duration=None,
                talk_duration=None,
                geoproduct_id=None,
            ),
        ),
        # without optional
        (
            dict(
                client_id=111,
                biz_id=123,
                event_type=CallEvent.INITIATED,
                event_timestamp=dt("2020-01-01 00:00:00"),
                source=Source.GEOADV_PHONE_CALL,
            ),
            dict(
                id=Any(int),
                client_id=111,
                biz_id=123,
                event_type=CallEvent.INITIATED,
                event_value=None,
                event_timestamp=dt("2020-01-01 00:00:00"),
                source="GEOADV_PHONE_CALL",
                created_at=Any(datetime),
                session_id=None,
                record_url=None,
                await_duration=None,
                talk_duration=None,
                geoproduct_id=None,
            ),
        ),
    ],
)
async def test_adds_call_event(factory, dm, con, params, expected):
    await factory.create_client(client_id=111)

    await dm.add_call_event(**params)

    rows = await con.fetch("SELECT * FROM call_events")
    assert [dict(row) for row in rows] == [expected]


async def test_raises_for_unknown_client(factory, dm):
    with pytest.raises(ForeignKeyViolationError):
        await dm.add_call_event(
            client_id=999,
            biz_id=999,
            event_type=CallEvent.INITIATED,
            event_timestamp=dt("2020-01-01 00:00:00"),
            source=Source.GEOADV_PHONE_CALL,
            session_id=456,
        )
