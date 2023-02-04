from datetime import datetime, timedelta, timezone

import pytest

from maps_adv.geosmb.promoter.server.lib.enums import SegmentType

pytestmark = [pytest.mark.asyncio]

now = datetime.now(tz=timezone.utc)


async def test_returns_zero_size_for_empty_segments(dm):
    got = await dm.list_segments(biz_id=123)

    assert got == dict(
        total_leads=0,
        segments={
            SegmentType.PROSPECTIVE: 0,
            SegmentType.ACTIVE: 0,
            SegmentType.LOST: 0,
            SegmentType.LOYAL: 0,
            SegmentType.DISLOYAL: 0,
        },
    )


async def test_returns_sizes_of_all_segments(factory, dm):
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

    got = await dm.list_segments(biz_id=123)

    assert got == dict(
        total_leads=16,
        segments={
            SegmentType.PROSPECTIVE: 5,
            SegmentType.ACTIVE: 2,
            SegmentType.LOST: 9,
            SegmentType.LOYAL: 6,
            SegmentType.DISLOYAL: 4,
        },
    )


async def test_does_not_counts_another_business_leads(factory, dm):
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

    got = await dm.list_segments(biz_id=123)

    assert got == dict(
        total_leads=0,
        segments={
            SegmentType.PROSPECTIVE: 0,
            SegmentType.ACTIVE: 0,
            SegmentType.LOST: 0,
            SegmentType.LOYAL: 0,
            SegmentType.DISLOYAL: 0,
        },
    )
