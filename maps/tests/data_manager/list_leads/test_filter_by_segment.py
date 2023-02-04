from datetime import datetime, timedelta, timezone

import pytest

from maps_adv.geosmb.promoter.server.lib.enums import SegmentType

pytestmark = [pytest.mark.asyncio]


now = datetime.now(tz=timezone.utc)


@pytest.mark.parametrize(
    "segment, expected_total, expected_ids",
    [
        (SegmentType.ACTIVE, 2, [104, 100]),
        (SegmentType.PROSPECTIVE, 1, [101]),
        (SegmentType.LOST, 2, [103, 102]),
        (SegmentType.LOYAL, 3, [101, 100, 103]),
        (SegmentType.DISLOYAL, 1, [104]),
    ],
)
async def test_returns_list_of_leads_in_specified_segment(
    segment, expected_total, expected_ids, factory, dm
):
    # ACTIVE, LOYAL
    await factory.create_lead_with_events(
        lead_id=100,
        passport_uid="1111",
        review_rating=5,
        make_routes=4,
        last_activity_timestamp=now - timedelta(days=80),
    )
    # PROSPECTIVE, LOYAL
    await factory.create_lead_with_events(
        lead_id=101,
        passport_uid="2222",
        review_rating=4,
        site_opens=1,
        last_activity_timestamp=now - timedelta(days=30),
    )
    # LOST
    await factory.create_lead_with_events(
        lead_id=102,
        passport_uid="3333",
        site_opens=4,
        last_activity_timestamp=now - timedelta(days=100),
    )
    # LOST, LOYAL
    await factory.create_lead_with_events(
        lead_id=103,
        passport_uid="4444",
        review_rating=4,
        showcase_product_click=4,
        last_activity_timestamp=now - timedelta(days=91),
    )
    # ACTIVE, DISLOYAL
    await factory.create_lead_with_events(
        lead_id=104,
        passport_uid="5555",
        review_rating=2,
        location_sharing=4,
        last_activity_timestamp=now - timedelta(days=20),
    )

    got = await dm.list_leads(
        biz_id=123, filter_by_segment=segment, limit=100500, offset=0
    )

    assert got["total_count"] == expected_total
    assert [lead["lead_id"] for lead in got["leads"]] == expected_ids


async def test_returns_nothing_if_no_clients_in_segment(factory, dm):
    # ACTIVE, LOYAL
    await factory.create_lead_with_events(
        lead_id=100,
        passport_uid="1111",
        review_rating=5,
        make_routes=4,
        last_activity_timestamp=now,
    )

    got = await dm.list_leads(
        biz_id=123, filter_by_segment=SegmentType.LOST, limit=100500, offset=0
    )

    assert got == dict(total_count=0, leads=[])
