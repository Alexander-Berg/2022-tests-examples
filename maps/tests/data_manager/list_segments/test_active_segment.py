from datetime import datetime, timedelta, timezone

import pytest

from maps_adv.geosmb.promoter.server.lib.enums import SegmentType

pytestmark = [pytest.mark.asyncio]

now = datetime.now(tz=timezone.utc)


async def test_segments_as_active_if_3_or_more_events_in_last_90_days(factory, dm):
    await factory.create_lead_with_events(
        review_rating=3, site_opens=2, last_activity_timestamp=now - timedelta(days=89)
    )

    got = await dm.list_segments(biz_id=123)

    assert got["total_leads"] == 1
    assert got["segments"][SegmentType.ACTIVE] == 1


async def test_does_not_segments_lost_or_prospective_if_active(factory, dm):
    await factory.create_lead_with_events(
        review_rating=3, site_opens=2, last_activity_timestamp=now - timedelta(days=80)
    )

    got = await dm.list_segments(biz_id=123)

    assert got["total_leads"] == 1
    assert got["segments"][SegmentType.LOST] == 0
    assert got["segments"][SegmentType.PROSPECTIVE] == 0


@pytest.mark.parametrize("review_rating", [2, 5])
async def test_counts_lead_in_total_once_if_belongs_to_several_segments(
    review_rating, factory, dm
):
    await factory.create_lead_with_events(
        review_rating=review_rating,
        site_opens=2,
        last_activity_timestamp=now - timedelta(days=80),
    )

    got = await dm.list_segments(biz_id=123)

    assert got["total_leads"] == 1


@pytest.mark.parametrize(
    "review_rating, loyalty_segment",
    [(2, SegmentType.DISLOYAL), (5, SegmentType.LOYAL)],
)
async def test_counts_lead_in_each_segment_it_belongs_to(
    review_rating, loyalty_segment, factory, dm
):
    await factory.create_lead_with_events(
        review_rating=review_rating,
        site_opens=2,
        last_activity_timestamp=now - timedelta(days=80),
    )

    got = await dm.list_segments(biz_id=123)

    assert got["segments"][SegmentType.ACTIVE] == 1
    assert got["segments"][loyalty_segment] == 1
