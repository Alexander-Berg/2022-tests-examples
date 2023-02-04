from datetime import datetime, timedelta, timezone

import pytest

from maps_adv.geosmb.promoter.server.lib.enums import EventType, SegmentType

pytestmark = [pytest.mark.asyncio]

now = datetime.now(tz=timezone.utc)


@pytest.mark.parametrize("review_rating", [4, 5, 6])
async def test_segments_as_loyal_if_review_rating_is_4_or_grater(
    review_rating, factory, dm
):
    await factory.create_lead_with_events(review_rating=review_rating)

    got = await dm.list_segments(biz_id=123)

    assert got["total_leads"] == 1
    assert got["segments"][SegmentType.LOYAL] == 1


async def test_does_not_segments_disloyal_if_loyal(factory, dm):
    await factory.create_lead_with_events(review_rating=5)

    got = await dm.list_segments(biz_id=123)

    assert got["total_leads"] == 1
    assert got["segments"][SegmentType.DISLOYAL] == 0


async def test_counts_each_lead_in_segment_only_once(factory, dm):
    lead_id = await factory.create_lead_with_event(
        event_type=EventType.REVIEW, event_value=5
    )
    await factory.create_event(
        lead_id=lead_id, event_type=EventType.REVIEW, event_value=4
    )

    got = await dm.list_segments(biz_id=123)

    assert got["total_leads"] == 1
    assert got["segments"][SegmentType.LOYAL] == 1


async def test_counts_leads_in_segment_correctly(factory, dm):
    for idx in range(1, 4):
        await factory.create_lead_with_events(passport_uid=str(idx), review_rating=5)

    got = await dm.list_segments(biz_id=123)

    assert got["total_leads"] == 3
    assert got["segments"][SegmentType.LOYAL] == 3


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
    events_params, factory, dm
):
    await factory.create_lead_with_events(review_rating=5, **events_params)

    got = await dm.list_segments(biz_id=123)

    assert got["total_leads"] == 1


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
    events_params, activity_segment, factory, dm
):
    await factory.create_lead_with_events(review_rating=5, **events_params)

    got = await dm.list_segments(biz_id=123)

    assert got["segments"][SegmentType.LOYAL] == 1
    assert got["segments"][activity_segment] == 1
