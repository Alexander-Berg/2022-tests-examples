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

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time("2020-01-01 00:00:01", tick=True),
]


async def test_returns_clients_details(factory, dm):
    client_id = await factory.create_client()

    total_count, got_clients = await dm.list_clients(biz_id=123, limit=100500, offset=0)

    assert total_count == 1
    assert got_clients == [
        dict(
            id=client_id,
            biz_id=123,
            phone=1234567890123,
            email="email@yandex.ru",
            passport_uid=456,
            first_name="client_first_name",
            last_name="client_last_name",
            gender=ClientGender.MALE,
            comment="this is comment",
            cleared_for_gdpr=False,
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
    ]


async def test_returns_source_from_first_revision(factory, dm):
    client_id = await factory.create_client(source=Source.CRM_INTERFACE)
    await factory.create_revision(client_id, source=Source.GEOADV_PHONE_CALL)

    _, got = await dm.list_clients(biz_id=123, limit=100500, offset=0)

    assert got[0]["source"] == Source.CRM_INTERFACE


async def test_not_returns_duplicates_for_clients_with_multiple_revisions(factory, dm):
    client_id = await factory.create_client(source=Source.CRM_INTERFACE)
    await factory.create_revision(client_id, source=Source.GEOADV_PHONE_CALL)
    await factory.create_revision(client_id, source=Source.CRM_INTERFACE)

    _, got = await dm.list_clients(biz_id=123, limit=100500, offset=0)

    assert len(got) == 1


async def test_returns_all_for_passed_biz_id(factory, dm):
    id_1 = await factory.create_empty_client()
    id_2 = await factory.create_empty_client()
    await factory.create_empty_client(biz_id=999)

    _, got = await dm.list_clients(biz_id=123, limit=100500, offset=0)

    assert {row["id"] for row in got} == {id_2, id_1}


async def test_returns_nothing_if_there_are_no_clients(factory, dm):
    await factory.create_empty_client(biz_id=999)

    _, got = await dm.list_clients(biz_id=123, limit=100500, offset=0)

    assert got == []


@pytest.mark.parametrize("segment_type", SegmentType)
async def test_matches_client_to_segment(dm, factory, segment_type):
    await factory.create_empty_client(segments={segment_type})

    _, got = await dm.list_clients(biz_id=123, limit=100500, offset=0)

    assert segment_type in got[0]["segments"]


async def test_match_client_to_no_order_segment_if_has_only_calls(dm, factory):
    client_id = await factory.create_empty_client()
    await factory.create_call_event(client_id, event_type=CallEvent.INITIATED)

    _, got = await dm.list_clients(biz_id=123, limit=100500, offset=0)

    assert SegmentType.NO_ORDERS in got[0]["segments"]


@pytest.mark.parametrize(
    "segment_type", [t for t in SegmentType if t != SegmentType.NO_ORDERS]
)
async def test_doesnt_match_client_to_activity_segment_if_no_orders(
    dm, factory, segment_type
):
    await factory.create_empty_client()

    _, got = await dm.list_clients(biz_id=123, limit=100500, offset=0)

    assert segment_type not in got[0]["segments"]


async def test_doesnt_match_client_to_no_order_segment_if_has_orders(dm, factory):
    await factory.create_empty_client(segments={SegmentType.ACTIVE})

    _, got = await dm.list_clients(biz_id=123, limit=100500, offset=0)

    assert SegmentType.NO_ORDERS not in got[0]["segments"]


async def test_match_client_to_multiple_segments(dm, factory):
    await factory.create_empty_client(
        segments={SegmentType.REGULAR, SegmentType.ACTIVE}
    )

    _, got = await dm.list_clients(biz_id=123, limit=100500, offset=0)

    assert SegmentType.REGULAR in got[0]["segments"]
    assert SegmentType.ACTIVE in got[0]["segments"]


async def test_returns_client_order_statistics(dm, factory):
    client_id = await factory.create_empty_client()
    event_earlies_ts = dt("2020-03-03 00:00:00")
    for i in range(4):
        await factory.create_order_event(
            client_id,
            event_type=OrderEvent.CREATED,
            event_timestamp=event_earlies_ts - timedelta(days=i),
        )
    for _ in range(2):
        await factory.create_order_event(client_id, event_type=OrderEvent.ACCEPTED)
    await factory.create_order_event(client_id, event_type=OrderEvent.REJECTED)
    await factory.create_call_event(client_id, event_type=CallEvent.INITIATED)

    _, got = await dm.list_clients(biz_id=123, limit=100500, offset=0)

    assert got[0]["statistics"] == {
        "orders": {
            "total": 4,
            "successful": 2,
            "unsuccessful": 1,
            "last_order_timestamp": event_earlies_ts,
        }
    }


async def test_does_not_count_call_events_in_statistics(dm, factory):
    client_id = await factory.create_empty_client()
    await factory.create_call_event(client_id, event_type=CallEvent.INITIATED)

    _, got = await dm.list_clients(biz_id=123, limit=100500, offset=0)

    assert got[0]["statistics"] == {
        "orders": {
            "total": 0,
            "successful": 0,
            "unsuccessful": 0,
            "last_order_timestamp": None,
        }
    }


async def test_skips_last_order_timestamps_if_no_created_order_events(dm, factory):
    client_id = await factory.create_client(client_id=111)
    for event_name in (OrderEvent.ACCEPTED, OrderEvent.REJECTED):
        await factory.create_order_event(client_id, event_type=event_name)

    _, got = await dm.list_clients(biz_id=123, limit=100500, offset=0)

    assert got[0]["statistics"]["orders"]["last_order_timestamp"] is None
