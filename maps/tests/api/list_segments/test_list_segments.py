from datetime import datetime, timedelta, timezone

import pytest

from maps_adv.geosmb.promoter.proto.segments_pb2 import (
    ListSegmentsInput,
    ListSegmentsOutput,
    Segment,
    SegmentType,
)

pytestmark = [pytest.mark.asyncio]

now = datetime.now(tz=timezone.utc)

url = "/v1/list_segments/"


async def test_returns_zero_size_for_empty_segments(api):
    got = await api.post(
        url,
        proto=ListSegmentsInput(biz_id=123),
        decode_as=ListSegmentsOutput,
        expected_status=200,
    )

    assert got == ListSegmentsOutput(
        total_leads=0,
        segments=[
            Segment(type=SegmentType.ACTIVE, size=0),
            Segment(type=SegmentType.DISLOYAL, size=0),
            Segment(type=SegmentType.LOST, size=0),
            Segment(type=SegmentType.LOYAL, size=0),
            Segment(type=SegmentType.PROSPECTIVE, size=0),
        ],
    )


async def test_returns_sizes_of_all_segments(factory, api):
    # PROSPECTIVE
    await factory.create_lead_with_events(
        passport_uid="100",
        site_opens=1,
        last_activity_timestamp=now - timedelta(days=80),
    )
    for idx in range(2):
        # ACTIVE
        await factory.create_lead_with_events(
            passport_uid=str(200 + idx),
            site_opens=4,
            last_activity_timestamp=now - timedelta(days=50),
        )
    for idx in range(3):
        # LOST
        await factory.create_lead_with_events(
            passport_uid=str(300 + idx),
            site_opens=4,
            last_activity_timestamp=now - timedelta(days=100),
        )
    for idx in range(4):
        # DISLOYAL (and PROSPECTIVE as side effect)
        await factory.create_lead_with_events(
            passport_uid=str(400 + idx),
            review_rating=2,
            last_activity_timestamp=now - timedelta(days=80),
        )
    for idx in range(6):
        # LOYAL (and LOST as side effect)
        await factory.create_lead_with_events(
            passport_uid=str(500 + idx),
            review_rating=5,
            last_activity_timestamp=now - timedelta(days=120),
        )

    got = await api.post(
        url,
        proto=ListSegmentsInput(biz_id=123),
        decode_as=ListSegmentsOutput,
        expected_status=200,
    )

    assert got == ListSegmentsOutput(
        total_leads=16,
        segments=[
            Segment(type=SegmentType.ACTIVE, size=2),
            Segment(type=SegmentType.DISLOYAL, size=4),
            Segment(type=SegmentType.LOST, size=9),
            Segment(type=SegmentType.LOYAL, size=6),
            Segment(type=SegmentType.PROSPECTIVE, size=5),
        ],
    )


async def test_does_not_counts_another_business_leads(factory, api):
    # ACTIVE & LOYAL
    await factory.create_lead_with_events(
        passport_uid="100",
        biz_id=555,
        review_rating=5,
        site_opens=2,
        last_activity_timestamp=now - timedelta(days=80),
    )
    # DISLOYAL & PROSPECTIVE
    await factory.create_lead_with_events(
        passport_uid="200",
        biz_id=555,
        review_rating=2,
        last_activity_timestamp=now - timedelta(days=50),
    )
    # LOST
    await factory.create_lead_with_events(
        passport_uid="300",
        biz_id=555,
        make_routes=2,
        last_activity_timestamp=now - timedelta(days=100),
    )

    got = await api.post(
        url,
        proto=ListSegmentsInput(biz_id=123),
        decode_as=ListSegmentsOutput,
        expected_status=200,
    )

    assert got == ListSegmentsOutput(
        total_leads=0,
        segments=[
            Segment(type=SegmentType.ACTIVE, size=0),
            Segment(type=SegmentType.DISLOYAL, size=0),
            Segment(type=SegmentType.LOST, size=0),
            Segment(type=SegmentType.LOYAL, size=0),
            Segment(type=SegmentType.PROSPECTIVE, size=0),
        ],
    )
