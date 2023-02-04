from datetime import datetime, timedelta, timezone

import pytest

from maps_adv.geosmb.promoter.proto.segments_pb2 import (
    ListSegmentsInput,
    ListSegmentsOutput,
    SegmentType,
)
from maps_adv.geosmb.promoter.server.lib.enums import EventType
from maps_adv.geosmb.promoter.server.tests.api.list_segments.utils import (
    parse_pb_segment_size,
)

pytestmark = [pytest.mark.asyncio]

now = datetime.now(tz=timezone.utc)

url = "/v1/list_segments/"


@pytest.mark.parametrize("review_rating", [4, 5, 6])
async def test_segments_as_loyal_if_review_rating_is_4_or_greater(
    review_rating, factory, api
):
    await factory.create_lead_with_events(review_rating=review_rating)

    got = await api.post(
        url,
        proto=ListSegmentsInput(biz_id=123),
        decode_as=ListSegmentsOutput,
        expected_status=200,
    )

    assert got.total_leads == 1
    assert parse_pb_segment_size(got, SegmentType.LOYAL) == 1


async def test_does_not_segments_disloyal_if_loyal(factory, api):
    await factory.create_lead_with_events(review_rating=5)

    got = await api.post(
        url,
        proto=ListSegmentsInput(biz_id=123),
        decode_as=ListSegmentsOutput,
        expected_status=200,
    )

    assert got.total_leads == 1
    assert parse_pb_segment_size(got, SegmentType.DISLOYAL) == 0


async def test_counts_each_lead_in_segment_only_once(factory, api):
    lead_id = await factory.create_lead_with_event(
        event_type=EventType.REVIEW, event_value=5
    )
    await factory.create_event(
        lead_id=lead_id, event_type=EventType.REVIEW, event_value=4
    )

    got = await api.post(
        url,
        proto=ListSegmentsInput(biz_id=123),
        decode_as=ListSegmentsOutput,
        expected_status=200,
    )

    assert got.total_leads == 1
    assert parse_pb_segment_size(got, SegmentType.LOYAL) == 1


async def test_counts_leads_in_segment_correctly(factory, api):
    for idx in range(1, 4):
        await factory.create_lead_with_events(passport_uid=str(idx), review_rating=5)

    got = await api.post(
        url,
        proto=ListSegmentsInput(biz_id=123),
        decode_as=ListSegmentsOutput,
        expected_status=200,
    )

    assert got.total_leads == 3
    assert parse_pb_segment_size(got, SegmentType.LOYAL) == 3


@pytest.mark.parametrize(
    "events_params",
    [
        # ACTIVE
        dict(site_opens=2, last_activity_timestamp=now - timedelta(days=80)),
        # PROSPECTIVE
        dict(site_opens=1, last_activity_timestamp=now - timedelta(days=80)),
        # LOST
        dict(site_opens=1, last_activity_timestamp=now - timedelta(days=100)),
    ],
)
async def test_counts_lead_in_total_once_if_belongs_to_several_segments(
    events_params, factory, api
):
    await factory.create_lead_with_events(review_rating=5, **events_params)

    got = await api.post(
        url,
        proto=ListSegmentsInput(biz_id=123),
        decode_as=ListSegmentsOutput,
        expected_status=200,
    )

    assert got.total_leads == 1


@pytest.mark.parametrize(
    "events_params, activity_segment",
    [
        (
            dict(site_opens=2, last_activity_timestamp=now - timedelta(days=80)),
            SegmentType.ACTIVE,
        ),
        (
            dict(site_opens=1, last_activity_timestamp=now - timedelta(days=80)),
            SegmentType.PROSPECTIVE,
        ),
        (
            dict(site_opens=1, last_activity_timestamp=now - timedelta(days=100)),
            SegmentType.LOST,
        ),
    ],
)
async def test_counts_lead_in_each_segment_it_belongs_to(
    events_params, activity_segment, factory, api
):
    await factory.create_lead_with_events(review_rating=5, **events_params)

    got = await api.post(
        url,
        proto=ListSegmentsInput(biz_id=123),
        decode_as=ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_size(got, SegmentType.LOYAL) == 1
    assert parse_pb_segment_size(got, activity_segment) == 1
