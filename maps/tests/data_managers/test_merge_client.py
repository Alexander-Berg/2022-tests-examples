from datetime import datetime, timedelta

import pytest
from freezegun import freeze_time
from smb.common.testing_utils import Any, dt

from maps_adv.geosmb.doorman.server.lib.enums import (
    CallEvent,
    ClientGender,
    OrderEvent,
    SegmentType,
    Source,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.real_db]


merge_kwargs = dict(
    biz_id=123,
    source=Source.BOOKING_YANG,
    metadata={"test_2": 2},
    phone=9000000,
    email="email_updated@yandex.ru",
    passport_uid=888,
    first_name="client_first_name_updated",
    last_name="client_last_name_updated",
    gender=ClientGender.FEMALE,
    comment="this is updated comment",
    initiator_id=778899,
)


empty_merge_kwargs = dict(
    biz_id=123,
    source=Source.BOOKING_YANG,
    metadata=None,
    phone=None,
    email=None,
    passport_uid=None,
    first_name=None,
    last_name=None,
    gender=None,
    comment=None,
    initiator_id=None,
)


async def test_updates_client_details(factory, dm):
    client_id = await factory.create_client()

    await dm.merge_client(client_id=client_id, **merge_kwargs)

    client_details = await factory.retrieve_client(client_id=client_id)
    assert client_details == dict(
        biz_id=123,
        phone="9000000",
        email="email_updated@yandex.ru",
        passport_uid=888,
        first_name="client_first_name_updated",
        last_name="client_last_name_updated",
        gender=ClientGender.FEMALE,
        comment="this is updated comment",
        labels=["mark-2021"],
        cleared_for_gdpr=False,
    )


async def test_creates_merge_revision(factory, con, dm):
    client_id = await factory.create_client()

    await dm.merge_client(client_id=client_id, **merge_kwargs)

    revisions = await factory.retrieve_client_revisions(client_id)
    assert len(revisions) == 2
    assert revisions[0] == dict(
        biz_id=123,
        source="BOOKING_YANG",
        metadata={"test_2": 2},
        phone="9000000",
        email="email_updated@yandex.ru",
        passport_uid=888,
        first_name="client_first_name_updated",
        last_name="client_last_name_updated",
        gender=ClientGender.FEMALE,
        comment="this is updated comment",
        initiator_id=778899,
    )


async def test_returns_merged_client_details(factory, dm):
    client_id = await factory.create_client()

    got = await dm.merge_client(client_id=client_id, **merge_kwargs)

    assert got == dict(
        id=client_id,
        biz_id=123,
        phone=9000000,
        email="email_updated@yandex.ru",
        passport_uid=888,
        first_name="client_first_name_updated",
        last_name="client_last_name_updated",
        gender=ClientGender.FEMALE,
        comment="this is updated comment",
        source=Source.CRM_INTERFACE,
        registration_timestamp=Any(datetime),
        segments=[SegmentType.NO_ORDERS],
        labels=["mark-2021"],
        statistics={
            "orders": {
                "total": 0,
                "successful": 0,
                "unsuccessful": 0,
                "last_order_timestamp": None,
            }
        },
    )


@freeze_time("2020-01-01 00:00:01", tick=True)
async def test_returns_merged_client_segments(dm, factory):
    client_id = await factory.create_client()
    await factory.create_order_event(
        client_id,
        event_type=OrderEvent.CREATED,
        event_timestamp=dt("2018-11-03 00:00:01"),
    )
    for _ in range(3):
        await factory.create_resolved_order_events_pair(
            client_id, OrderEvent.ACCEPTED, event_timestamp=dt("2019-11-03 00:00:01")
        )

    got = await dm.merge_client(client_id=client_id, **merge_kwargs)

    assert got["segments"] == [
        SegmentType.REGULAR,
        SegmentType.ACTIVE,
        SegmentType.UNPROCESSED_ORDERS,
    ]


async def test_calculates_merged_client_order_statistics(dm, factory):
    client_id = await factory.create_client()
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

    got = await dm.merge_client(client_id=client_id, **merge_kwargs)

    assert got["statistics"] == {
        "orders": {
            "total": 4,
            "successful": 2,
            "unsuccessful": 1,
            "last_order_timestamp": event_latest_ts,
        }
    }


async def test_not_replaces_client_details_with_none(factory, dm):
    client_id = await factory.create_client()

    await dm.merge_client(client_id=client_id, **empty_merge_kwargs)

    client_details = await factory.retrieve_client(client_id=client_id)
    assert client_details == dict(
        biz_id=123,
        phone="1234567890123",
        email="email@yandex.ru",
        passport_uid=456,
        first_name="client_first_name",
        last_name="client_last_name",
        gender=ClientGender.MALE,
        comment="this is comment",
        labels=["mark-2021"],
        cleared_for_gdpr=False,
    )


async def test_creates_revision_with_skipped_fields(dm, factory, con):
    client_id = await factory.create_client()

    await dm.merge_client(client_id=client_id, **empty_merge_kwargs)

    last_revision_details = await factory.retrieve_last_revision(client_id=client_id)
    assert await con.fetchval("SELECT COUNT(*) FROM client_revisions") == 2
    assert last_revision_details == dict(
        biz_id=123,
        source="BOOKING_YANG",
        metadata=None,
        phone=None,
        email=None,
        passport_uid=None,
        first_name=None,
        last_name=None,
        gender=None,
        comment=None,
        initiator_id=None,
    )
