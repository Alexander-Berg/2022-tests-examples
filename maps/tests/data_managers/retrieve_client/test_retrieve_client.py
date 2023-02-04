from datetime import datetime, timedelta

import pytest
from smb.common.testing_utils import Any, dt

from maps_adv.geosmb.doorman.server.lib.enums import (
    CallEvent,
    ClientGender,
    OrderEvent,
    SegmentType,
    Source,
)
from maps_adv.geosmb.doorman.server.lib.exceptions import UnknownClient

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("cleared_for_gdpr", [True, False])
async def test_returns_client_details(dm, factory, cleared_for_gdpr):
    client_id = await factory.create_client(cleared_for_gdpr=cleared_for_gdpr)

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert got == dict(
        id=client_id,
        biz_id=123,
        gender=ClientGender.MALE,
        phone=1234567890123,
        email="email@yandex.ru",
        passport_uid=456,
        first_name="client_first_name",
        last_name="client_last_name",
        comment="this is comment",
        cleared_for_gdpr=cleared_for_gdpr,
        labels=["mark-2021"],
        segments=[SegmentType.NO_ORDERS],
        statistics={
            "orders": {
                "total": 0,
                "successful": 0,
                "unsuccessful": 0,
                "last_order_timestamp": None,
            }
        },
        source=Source.CRM_INTERFACE,
        registration_timestamp=Any(datetime),
    )


async def test_returns_source_from_first_revision(factory, dm):
    client_id = await factory.create_client(source=Source.CRM_INTERFACE)
    await factory.create_revision(client_id, source=Source.GEOADV_PHONE_CALL)

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert got["source"] == Source.CRM_INTERFACE


async def test_calculates_client_order_statistics(dm, factory):
    client_id = await factory.create_empty_client()
    event_latest_ts = dt("2020-03-03 00:00:00")
    for i in range(4):
        await factory.create_order_event(
            client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=event_latest_ts - timedelta(days=i),
        )
    for _ in range(2):
        await factory.create_order_event(client_id, event_type=OrderEvent.ACCEPTED)
    await factory.create_order_event(client_id, event_type=OrderEvent.REJECTED)
    await factory.create_call_event(client_id, event_type=CallEvent.INITIATED)

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert got["statistics"] == {
        "orders": {
            "total": 4,
            "successful": 2,
            "unsuccessful": 1,
            "last_order_timestamp": event_latest_ts,
        }
    }


async def test_call_events_does_not_affect_statistics(dm, factory):
    client_id = await factory.create_empty_client()
    event_latest_ts = dt("2020-03-03 00:00:00")
    for i in range(4):
        await factory.create_order_event(
            client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=event_latest_ts - timedelta(days=i),
        )
    for _ in range(2):
        await factory.create_order_event(client_id, event_type=OrderEvent.ACCEPTED)
    await factory.create_order_event(client_id, event_type=OrderEvent.REJECTED)
    await factory.create_call_event(client_id, event_type=CallEvent.INITIATED)
    for _ in range(5):
        await factory.create_call_event(client_id)

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert got["statistics"] == {
        "orders": {
            "total": 4,
            "successful": 2,
            "unsuccessful": 1,
            "last_order_timestamp": event_latest_ts,
        }
    }


async def test_returns_none_as_last_order_timestamp_if_no_order_created_events(
    dm, factory
):
    client_id = await factory.create_client(client_id=111)
    for event_type in (OrderEvent.ACCEPTED, OrderEvent.REJECTED, CallEvent.INITIATED):
        await factory.create_event(client_id, event_type=event_type)

    got = await dm.retrieve_client(biz_id=123, client_id=client_id)

    assert got["statistics"]["orders"]["last_order_timestamp"] is None


@pytest.mark.parametrize("biz_id, client_id", [(123, 999), (999, 534)])
async def test_raises_error_for_unknown_client(biz_id, client_id, factory, dm):
    await factory.create_client(client_id=534)

    with pytest.raises(UnknownClient) as exc:
        await dm.retrieve_client(biz_id=biz_id, client_id=client_id)

    assert exc.value.search_fields == {"biz_id": biz_id, "client_id": client_id}
