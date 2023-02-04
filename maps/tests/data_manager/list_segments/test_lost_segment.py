from datetime import datetime, timedelta, timezone

import pytest

from maps_adv.geosmb.promoter.server.lib.enums import SegmentType

pytestmark = [pytest.mark.asyncio]

now = datetime.now(tz=timezone.utc)


@pytest.mark.parametrize("events_amount", [1, 5])
async def test_segments_as_lost_if_no_events_in_last_90_days(
    events_amount, factory, dm
):
    await factory.create_lead_with_events(
        site_opens=events_amount, last_activity_timestamp=now - timedelta(days=91)
    )

    got = await dm.list_segments(biz_id=123)

    assert got["total_leads"] == 1
    assert got["segments"][SegmentType.LOST] == 1


@pytest.mark.parametrize("events_amount", [1, 5])
async def test_does_not_segments_active_or_prospective_if_lost(
    events_amount, factory, dm
):
    await factory.create_lead_with_events(
        site_opens=events_amount, last_activity_timestamp=now - timedelta(days=100)
    )

    got = await dm.list_segments(biz_id=123)

    assert got["total_leads"] == 1
    assert got["segments"][SegmentType.ACTIVE] == 0
    assert got["segments"][SegmentType.PROSPECTIVE] == 0


async def test_counts_leads_in_segment_correctly(factory, dm):
    for idx in range(1, 4):
        await factory.create_lead_with_events(
            passport_uid=str(idx),
            site_opens=5,
            last_activity_timestamp=now - timedelta(days=100),
        )

    got = await dm.list_segments(biz_id=123)

    assert got["total_leads"] == 3
    assert got["segments"][SegmentType.LOST] == 3


@pytest.mark.parametrize("review_rating", [2, 5])
async def test_counts_lead_in_total_once_if_belongs_to_several_segments(
    review_rating, factory, dm
):
    await factory.create_lead_with_events(
        review_rating=review_rating,
        site_opens=2,
        last_activity_timestamp=now - timedelta(days=100),
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
        last_activity_timestamp=now - timedelta(days=100),
    )

    got = await dm.list_segments(biz_id=123)

    assert got["segments"][SegmentType.LOST] == 1
    assert got["segments"][loyalty_segment] == 1
