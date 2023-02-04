import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.promoter.proto.errors_pb2 import Error
from maps_adv.geosmb.promoter.proto.leads_pb2 import (
    Event,
    ListLeadEventsInput,
    ListLeadEventsOutput,
    Source,
)
from maps_adv.geosmb.promoter.server.lib.enums import EventType, Source as SourceEnum
from maps_adv.geosmb.proto.common_pb2 import Pagination

pytestmark = [pytest.mark.asyncio]

url = "/v1/list_lead_events/"


async def test_returns_lead_events_records(factory, api):
    lead_id = await factory.create_lead()
    await factory.create_event(
        lead_id=lead_id,
        event_type=EventType.REVIEW,
        event_value=5,
        event_timestamp=dt("2020-06-14 19:00:00"),
        source=SourceEnum.STRAIGHT,
    )
    await factory.create_event(
        lead_id=lead_id,
        event_type=EventType.OPEN_SITE,
        event_timestamp=dt("2020-06-13 19:00:00"),
        source=SourceEnum.DISCOVERY_ADVERT,
    )
    await factory.create_event(
        lead_id=lead_id,
        event_type=EventType.MAKE_ROUTE,
        event_timestamp=dt("2020-06-15 19:00:00"),
        source=SourceEnum.STRAIGHT,
    )
    await factory.create_event(
        lead_id=lead_id,
        event_type=EventType.CLICK_ON_PHONE,
        event_timestamp=dt("2020-06-12 19:00:00"),
        source=None,
    )

    got = await api.post(
        url,
        proto=ListLeadEventsInput(
            lead_id=lead_id, biz_id=123, pagination=Pagination(offset=0, limit=100500)
        ),
        decode_as=ListLeadEventsOutput,
        expected_status=200,
    )

    assert got == ListLeadEventsOutput(
        total_events=4,
        events=[
            Event(
                type=Event.MAKE_ROUTE,
                type_str="MAKE_ROUTE",
                timestamp=dt("2020-06-15 19:00:00", as_proto=True),
                source=Source.STRAIGHT,
            ),
            Event(
                type=Event.REVIEW,
                type_str="REVIEW",
                timestamp=dt("2020-06-14 19:00:00", as_proto=True),
                value="5",
                source=Source.STRAIGHT,
            ),
            Event(
                type=Event.OPEN_SITE,
                type_str="OPEN_SITE",
                timestamp=dt("2020-06-13 19:00:00", as_proto=True),
                source=Source.DISCOVERY_ADVERT,
            ),
            Event(
                type=Event.CLICK_ON_PHONE,
                type_str="CLICK_ON_PHONE",
                timestamp=dt("2020-06-12 19:00:00", as_proto=True),
            ),
        ],
    )


async def test_does_not_return_event_value_if_event_name_is_not_review(factory, api):
    lead_id = await factory.create_lead()
    for event_type in [en for en in EventType if en != EventType.REVIEW]:
        await factory.create_event(
            lead_id=lead_id, event_type=event_type, events_amount=4
        )

    got = await api.post(
        url,
        proto=ListLeadEventsInput(
            lead_id=lead_id, biz_id=123, pagination=Pagination(offset=0, limit=100500)
        ),
        decode_as=ListLeadEventsOutput,
        expected_status=200,
    )

    for event in got.events:
        assert not event.HasField("value")


async def test_returns_event_value_if_event_name_is_review(factory, api):
    lead_id = await factory.create_lead()
    await factory.create_event(
        lead_id=lead_id, event_type=EventType.REVIEW, event_value=5
    )

    got = await api.post(
        url,
        proto=ListLeadEventsInput(
            lead_id=lead_id, biz_id=123, pagination=Pagination(offset=0, limit=100500)
        ),
        decode_as=ListLeadEventsOutput,
        expected_status=200,
    )

    event = got.events[0]
    assert event.HasField("value")


@pytest.mark.parametrize("limit", range(1, 4))
async def test_respects_limit(limit, factory, api):
    event_timestamps = [
        "2020-06-15 19:00:00",
        "2020-06-14 19:00:00",
        "2020-06-13 19:00:00",
    ]
    event_timestamps_as_proto = [dt(ts, as_proto=True) for ts in event_timestamps]

    lead_id = await factory.create_lead()
    for ts in event_timestamps:
        await factory.create_event(
            lead_id=lead_id, event_type=EventType.MAKE_ROUTE, event_timestamp=dt(ts)
        )

    got = await api.post(
        url,
        proto=ListLeadEventsInput(
            lead_id=lead_id, biz_id=123, pagination=Pagination(offset=0, limit=limit)
        ),
        decode_as=ListLeadEventsOutput,
        expected_status=200,
    )

    assert [event.timestamp for event in got.events] == event_timestamps_as_proto[
        :limit
    ]
    assert got.total_events == 3


@pytest.mark.parametrize("offset", range(4))
async def test_respects_offset(offset, factory, api):
    event_timestamps = [
        "2020-06-15 19:00:00",
        "2020-06-14 19:00:00",
        "2020-06-13 19:00:00",
    ]
    event_timestamps_as_proto = [dt(ts, as_proto=True) for ts in event_timestamps]

    lead_id = await factory.create_lead()
    for ts in event_timestamps:
        await factory.create_event(
            lead_id=lead_id, event_type=EventType.MAKE_ROUTE, event_timestamp=dt(ts)
        )

    got = await api.post(
        url,
        proto=ListLeadEventsInput(
            lead_id=lead_id, biz_id=123, pagination=Pagination(offset=offset, limit=2)
        ),
        decode_as=ListLeadEventsOutput,
        expected_status=200,
    )

    assert [event.timestamp for event in got.events] == event_timestamps_as_proto[
        offset : offset + 2  # noqa
    ]
    assert got.total_events == 3


async def test_returns_empty_events_list_if_offset_is_greater_than_total(factory, api):
    lead_id = await factory.create_lead()
    for _ in range(5):
        await factory.create_event(
            lead_id=lead_id,
            event_type=EventType.OPEN_SITE,
            event_timestamp=dt("2020-06-13 19:00:00"),
        )

    got = await api.post(
        url,
        proto=ListLeadEventsInput(
            lead_id=lead_id, biz_id=123, pagination=Pagination(offset=10, limit=100500)
        ),
        decode_as=ListLeadEventsOutput,
        expected_status=200,
    )

    assert got == ListLeadEventsOutput(total_events=5, events=[])


async def test_returns_empty_events_list_if_limit_is_0(factory, api):
    lead_id = await factory.create_lead()
    for _ in range(5):
        await factory.create_event(
            lead_id=lead_id,
            event_type=EventType.OPEN_SITE,
            event_timestamp=dt("2020-06-13 19:00:00"),
        )

    got = await api.post(
        url,
        proto=ListLeadEventsInput(
            lead_id=lead_id, biz_id=123, pagination=Pagination(offset=0, limit=0)
        ),
        decode_as=ListLeadEventsOutput,
        expected_status=200,
    )

    assert got == ListLeadEventsOutput(total_events=5, events=[])


async def test_errored_if_lead_not_found(factory, api):
    got = await api.post(
        url,
        proto=ListLeadEventsInput(
            lead_id=100500, biz_id=123, pagination=Pagination(offset=0, limit=100500)
        ),
        decode_as=Error,
        expected_status=404,
    )

    assert got == Error(
        code=Error.UNKNOWN_LEAD,
        description="Unknown lead with biz_id=123, lead_id=100500",
    )


async def test_errored_if_lead_exists_in_another_business(factory, api):
    lead_id = await factory.create_lead_with_events(clicks_on_phone=1)

    got = await api.post(
        url,
        proto=ListLeadEventsInput(
            lead_id=lead_id, biz_id=456, pagination=Pagination(offset=0, limit=100500)
        ),
        decode_as=Error,
        expected_status=404,
    )

    assert got == Error(
        code=Error.UNKNOWN_LEAD,
        description=f"Unknown lead with biz_id=456, lead_id={lead_id}",
    )


@pytest.mark.parametrize(
    "kw, msg",
    (
        [dict(biz_id=0, lead_id=1), "biz_id: ['Must be at least 1.']"],
        [dict(biz_id=1, lead_id=0), "lead_id: ['Must be at least 1.']"],
        [
            dict(biz_id=0, lead_id=0),
            "biz_id: ['Must be at least 1.'], lead_id: ['Must be at least 1.']",
        ],
    ),
)
async def test_errored_for_wrong_input(kw, msg, api):
    got = await api.post(
        url,
        proto=ListLeadEventsInput(pagination=Pagination(limit=100500, offset=0), **kw),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=msg)
