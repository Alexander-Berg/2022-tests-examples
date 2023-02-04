import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.promoter.server.lib.enums import EventType, Source
from maps_adv.geosmb.promoter.server.lib.exceptions import UnknownLead

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize(
    "event_type1, event_type2", zip(list(EventType), reversed(EventType))
)
async def test_returns_lead_events_records(event_type1, event_type2, factory, dm):
    lead_id = await factory.create_lead()
    await factory.create_event(
        lead_id=lead_id,
        event_type=event_type1,
        event_value=5 if event_type1 is EventType.REVIEW else None,
        event_timestamp=dt("2020-06-13 19:00:00"),
        source=Source.STRAIGHT,
    )
    await factory.create_event(
        lead_id=lead_id,
        event_type=event_type2,
        event_value=5 if event_type2 is EventType.REVIEW else None,
        event_timestamp=dt("2020-06-15 19:00:00"),
        source=Source.DISCOVERY_ADVERT,
    )

    got = await dm.list_lead_events(lead_id=lead_id, biz_id=123, offset=0, limit=100500)

    assert got == dict(
        total_events=2,
        events=[
            dict(
                event_type=event_type2,
                event_value="5" if event_type2 is EventType.REVIEW else None,
                event_timestamp=dt("2020-06-15 19:00:00"),
                source=Source.DISCOVERY_ADVERT,
            ),
            dict(
                event_type=event_type1,
                event_value="5" if event_type1 is EventType.REVIEW else None,
                event_timestamp=dt("2020-06-13 19:00:00"),
                source=Source.STRAIGHT,
            ),
        ],
    )


async def test_does_not_return_event_value_if_event_name_is_not_review(factory, dm):
    lead_id = await factory.create_lead()
    for event_type in [en for en in EventType if en != EventType.REVIEW]:
        await factory.create_event(
            lead_id=lead_id, event_type=event_type, events_amount=4
        )

    got = await dm.list_lead_events(lead_id=lead_id, biz_id=123, offset=0, limit=100500)

    for event in got["events"]:
        assert event.get("event_value") is None


async def test_returns_event_value_if_event_name_is_review(factory, dm):
    lead_id = await factory.create_lead()
    await factory.create_event(
        lead_id=lead_id, event_type=EventType.REVIEW, event_value=5
    )

    got = await dm.list_lead_events(lead_id=lead_id, biz_id=123, offset=0, limit=100500)

    event = got["events"][0]
    assert event.get("event_value") is not None


@pytest.mark.parametrize("limit", range(1, 4))
async def test_respects_limit(limit, factory, dm):
    event_timestamps = [
        dt("2020-06-15 19:00:00"),
        dt("2020-06-14 19:00:00"),
        dt("2020-06-13 19:00:00"),
    ]

    lead_id = await factory.create_lead()
    for ts in event_timestamps:
        await factory.create_event(
            lead_id=lead_id, event_type=EventType.MAKE_ROUTE, event_timestamp=ts
        )

    got = await dm.list_lead_events(lead_id=lead_id, biz_id=123, offset=0, limit=limit)

    assert [event["event_timestamp"] for event in got["events"]] == event_timestamps[
        :limit
    ]
    assert got["total_events"] == 3


@pytest.mark.parametrize("offset", range(4))
async def test_respects_offset(offset, factory, dm):
    event_timestamps = [
        dt("2020-06-15 19:00:00"),
        dt("2020-06-14 19:00:00"),
        dt("2020-06-13 19:00:00"),
    ]

    lead_id = await factory.create_lead()
    for ts in event_timestamps:
        await factory.create_event(
            lead_id=lead_id, event_type=EventType.MAKE_ROUTE, event_timestamp=ts
        )

    got = await dm.list_lead_events(lead_id=lead_id, biz_id=123, offset=offset, limit=2)

    assert [event["event_timestamp"] for event in got["events"]] == event_timestamps[
        offset : offset + 2  # noqa
    ]
    assert got["total_events"] == 3


async def test_returns_empty_events_list_if_offset_is_grater_than_total(factory, dm):
    lead_id = await factory.create_lead()
    for _ in range(5):
        await factory.create_event(
            lead_id=lead_id,
            event_type=EventType.OPEN_SITE,
            event_timestamp=dt("2020-06-13 19:00:00"),
        )

    got = await dm.list_lead_events(
        lead_id=lead_id, biz_id=123, offset=10, limit=100500
    )

    assert got == dict(total_events=5, events=[])


async def test_returns_empty_events_list_if_limit_is_0(factory, dm):
    lead_id = await factory.create_lead()
    for _ in range(5):
        await factory.create_event(
            lead_id=lead_id,
            event_type=EventType.OPEN_SITE,
            event_timestamp=dt("2020-06-13 19:00:00"),
        )

    got = await dm.list_lead_events(lead_id=lead_id, biz_id=123, offset=0, limit=0)

    assert got == dict(total_events=5, events=[])


async def test_errored_if_lead_not_found(factory, dm):
    with pytest.raises(UnknownLead) as exc:
        await dm.list_lead_events(lead_id=100500, biz_id=123, offset=0, limit=100500)

    assert exc.value.args == ("Unknown lead with biz_id=123, lead_id=100500",)


async def test_errored_if_lead_exist_in_another_business(factory, dm):
    lead_id = await factory.create_lead_with_events(clicks_on_phone=1)

    with pytest.raises(UnknownLead) as exc:
        await dm.list_lead_events(lead_id=lead_id, biz_id=456, offset=0, limit=100500)

    assert exc.value.args == (f"Unknown lead with biz_id=456, lead_id={lead_id}",)
