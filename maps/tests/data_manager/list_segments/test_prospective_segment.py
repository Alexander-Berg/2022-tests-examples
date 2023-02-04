from datetime import datetime, timedelta, timezone

import pytest

from maps_adv.geosmb.promoter.server.lib.enums import SegmentType

pytestmark = [pytest.mark.asyncio]

now = datetime.now(tz=timezone.utc)


async def test_segments_as_prospective_if_less_than_3_events_in_last_90_days(
    factory, dm
):
    await factory.create_lead_with_events(
        clicks_on_phone=1,
        site_opens=1,
        last_activity_timestamp=now - timedelta(days=89),
    )

    got = await dm.list_segments(biz_id=123)

    assert got["total_leads"] == 1
    assert got["segments"][SegmentType.PROSPECTIVE] == 1


async def test_does_not_segments_lost_or_active_if_prospective(factory, dm):
    await factory.create_lead_with_events(
        clicks_on_phone=1,
        site_opens=1,
        last_activity_timestamp=now - timedelta(days=80),
    )

    got = await dm.list_segments(biz_id=123)

    assert got["total_leads"] == 1
    assert got["segments"][SegmentType.LOST] == 0
    assert got["segments"][SegmentType.ACTIVE] == 0


async def test_counts_leads_in_segment_correctly(factory, dm):
    for idx in range(1, 4):
        await factory.create_lead_with_events(
            passport_uid=str(idx),
            site_opens=1,
            last_activity_timestamp=now - timedelta(days=80),
        )

    got = await dm.list_segments(biz_id=123)

    assert got["total_leads"] == 3
    assert got["segments"][SegmentType.PROSPECTIVE] == 3


@pytest.mark.parametrize("review_rating", [2, 5])
async def test_counts_lead_in_total_once_if_belongs_to_several_segments(
    review_rating, factory, dm
):
    await factory.create_lead_with_events(
        review_rating=review_rating,
        site_opens=1,
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
        site_opens=1,
        last_activity_timestamp=now - timedelta(days=80),
    )

    got = await dm.list_segments(biz_id=123)

    assert got["segments"][SegmentType.PROSPECTIVE] == 1
    assert got["segments"][loyalty_segment] == 1
