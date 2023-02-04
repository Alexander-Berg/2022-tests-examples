from datetime import datetime

import pytest
from asyncpg import ForeignKeyViolationError
from smb.common.testing_utils import Any, dt

from maps_adv.geosmb.doorman.server.lib.enums import OrderEvent, Source

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("event_type", OrderEvent)
@pytest.mark.parametrize("source", Source)
async def test_saves_order_event(event_type, source, factory, dm, con):
    client_id = await factory.create_client()

    await dm.add_order_event(
        client_id=client_id,
        biz_id=123,
        order_id=687,
        event_type=event_type,
        event_timestamp=dt("2020-01-01 00:00:00"),
        source=source,
    )

    rows = await con.fetch("SELECT * FROM order_events")
    assert [dict(row) for row in rows] == [
        dict(
            id=Any(int),
            client_id=client_id,
            biz_id=123,
            order_id=687,
            event_type=event_type,
            event_timestamp=dt("2020-01-01 00:00:00"),
            source=source.value,
            created_at=Any(datetime),
        )
    ]


async def test_raises_for_unknown_client(factory, dm):
    with pytest.raises(ForeignKeyViolationError):
        await dm.add_order_event(
            client_id=999,
            biz_id=999,
            order_id=687,
            event_type=OrderEvent.ACCEPTED,
            event_timestamp=dt("2020-01-01 00:00:00"),
            source=Source.BOOKING_YANG,
        )
