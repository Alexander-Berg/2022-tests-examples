from datetime import datetime, timedelta, timezone

import pytest

from maps_adv.geosmb.promoter.proto.segments_pb2 import (
    ListSegmentsInput,
    ListSegmentsOutput,
    SegmentType,
)
from maps_adv.geosmb.promoter.server.tests.api.list_segments.utils import (
    parse_pb_segment_size,
)

pytestmark = [pytest.mark.asyncio]

now = datetime.now(tz=timezone.utc)

url = "/v1/list_segments/"


async def test_segments_as_active_if_3_or_more_events_in_last_90_days(factory, api):
    await factory.create_lead_with_events(
        review_rating=3, site_opens=2, last_activity_timestamp=now - timedelta(days=89)
    )

    got = await api.post(
        url,
        proto=ListSegmentsInput(biz_id=123),
        decode_as=ListSegmentsOutput,
        expected_status=200,
    )

    assert got.total_leads == 1
    assert parse_pb_segment_size(got, SegmentType.ACTIVE) == 1


async def test_does_not_segments_lost_or_prospective_if_active(factory, api):
    await factory.create_lead_with_events(
        review_rating=3, site_opens=2, last_activity_timestamp=now - timedelta(days=89)
    )

    got = await api.post(
        url,
        proto=ListSegmentsInput(biz_id=123),
        decode_as=ListSegmentsOutput,
        expected_status=200,
    )

    assert got.total_leads == 1
    assert parse_pb_segment_size(got, SegmentType.LOST) == 0
    assert parse_pb_segment_size(got, SegmentType.PROSPECTIVE) == 0


async def test_counts_leads_in_segment_correctly(factory, api):
    for idx in range(1, 4):
        await factory.create_lead_with_events(
            passport_uid=str(idx),
            site_opens=5,
            last_activity_timestamp=now - timedelta(days=89),
        )

    got = await api.post(
        url,
        proto=ListSegmentsInput(biz_id=123),
        decode_as=ListSegmentsOutput,
        expected_status=200,
    )

    assert got.total_leads == 3
    assert parse_pb_segment_size(got, SegmentType.ACTIVE) == 3


@pytest.mark.parametrize("review_rating", [2, 5])
async def test_counts_lead_in_total_once_if_belongs_to_several_segments(
    review_rating, factory, api
):
    await factory.create_lead_with_events(
        review_rating=review_rating,
        site_opens=2,
        last_activity_timestamp=now - timedelta(days=89),
    )

    got = await api.post(
        url,
        proto=ListSegmentsInput(biz_id=123),
        decode_as=ListSegmentsOutput,
        expected_status=200,
    )

    assert got.total_leads == 1


@pytest.mark.parametrize(
    "review_rating, loyalty_segment",
    [(2, SegmentType.DISLOYAL), (5, SegmentType.LOYAL)],
)
async def test_counts_lead_in_each_segment_it_belongs_to(
    review_rating, loyalty_segment, factory, api
):
    await factory.create_lead_with_events(
        review_rating=review_rating,
        site_opens=2,
        last_activity_timestamp=now - timedelta(days=89),
    )

    got = await api.post(
        url,
        proto=ListSegmentsInput(biz_id=123),
        decode_as=ListSegmentsOutput,
        expected_status=200,
    )

    assert parse_pb_segment_size(got, SegmentType.ACTIVE) == 1
    assert parse_pb_segment_size(got, loyalty_segment) == 1
