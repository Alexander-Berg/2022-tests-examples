from datetime import datetime, timedelta, timezone

import pytest

from maps_adv.geosmb.promoter.server.lib.enums import EventType, SegmentType

pytestmark = [pytest.mark.asyncio]

now = datetime.now(tz=timezone.utc)


async def test_returns_all_lead_segments(factory, dm):
    await factory.create_lead_with_events(
        review_rating=3,
        clicks_on_phone=1,
        site_opens=1,
        view_entrances=1,
        showcase_product_click=1,
        last_activity_timestamp=now - timedelta(days=50),
    )

    got = await dm.list_leads(biz_id=123, limit=100500, offset=0)

    lead = got["leads"][0]
    assert lead["segments"] == [SegmentType.ACTIVE, SegmentType.DISLOYAL]


async def test_segments_as_prospective_if_less_than_3_events_in_last_90_days(
    factory, dm
):
    await factory.create_lead_with_events(
        clicks_on_phone=1,
        site_opens=1,
        last_activity_timestamp=now - timedelta(days=89),
    )

    got = await dm.list_leads(biz_id=123, limit=100500, offset=0)

    lead = got["leads"][0]
    assert SegmentType.PROSPECTIVE in lead["segments"]


async def test_segments_as_active_if_3_or_more_events_in_last_90_days(factory, dm):
    await factory.create_lead_with_events(
        review_rating=5,
        clicks_on_phone=1,
        view_working_hours=1,
        last_activity_timestamp=now - timedelta(days=89),
    )

    got = await dm.list_leads(biz_id=123, limit=100500, offset=0)

    lead = got["leads"][0]
    assert SegmentType.ACTIVE in lead["segments"]


async def test_segments_as_lost_if_more_90_days_since_last_activity(factory, dm):
    await factory.create_lead_with_events(
        review_rating=5,
        location_sharing=1,
        site_opens=1,
        last_activity_timestamp=now - timedelta(days=91),
    )

    got = await dm.list_leads(biz_id=123, limit=100500, offset=0)

    lead = got["leads"][0]
    assert SegmentType.LOST in lead["segments"]


async def test_does_not_segment_as_lost_for_history_if_has_any_recent_event(
    factory, dm
):
    # events older than 90 days
    lead_id = await factory.create_lead_with_events(
        clicks_on_phone=1,
        site_opens=1,
        last_activity_timestamp=now - timedelta(days=91),
    )
    # event in last 90 days
    await factory.create_event(
        lead_id=lead_id,
        event_type=EventType.MAKE_ROUTE,
        event_timestamp=now - timedelta(days=80),
    )

    got = await dm.list_leads(biz_id=123, limit=100500, offset=0)

    lead = got["leads"][0]
    assert SegmentType.LOST not in lead["segments"]


@pytest.mark.parametrize("review_rating", [4, 5, 6])
async def test_segments_as_loyal_if_review_rating_is_grater_or_equal_4(
    review_rating, factory, dm
):
    await factory.create_lead_with_events(review_rating=review_rating)

    got = await dm.list_leads(biz_id=123, limit=100500, offset=0)

    lead = got["leads"][0]
    assert SegmentType.LOYAL in lead["segments"]


@pytest.mark.parametrize("review_rating", [1, 2, 3])
async def test_segments_as_disloyal_if_review_rating_is_under_4(
    review_rating, factory, dm
):
    await factory.create_lead_with_events(review_rating=review_rating)

    got = await dm.list_leads(biz_id=123, limit=100500, offset=0)

    lead = got["leads"][0]
    assert SegmentType.DISLOYAL in lead["segments"]


@pytest.mark.parametrize(
    "review_rating, expected_segment, unexpected_segment",
    [
        (2, SegmentType.DISLOYAL, SegmentType.LOYAL),
        (5, SegmentType.LOYAL, SegmentType.DISLOYAL),
    ],
)
async def test_does_not_segment_lead_as_loyal_and_disloyal_simultaneously(
    review_rating, expected_segment, unexpected_segment, factory, dm
):
    await factory.create_lead_with_events(review_rating=review_rating)

    got = await dm.list_leads(biz_id=123, limit=100500, offset=0)

    lead = got["leads"][0]
    assert expected_segment in lead["segments"]
    assert unexpected_segment not in lead["segments"]


@pytest.mark.parametrize(
    "event_details, expected_segment, unexpected_segments",
    [
        (
            dict(site_opens=1, last_activity_timestamp=now - timedelta(days=80)),
            SegmentType.PROSPECTIVE,
            [SegmentType.ACTIVE, SegmentType.LOST],
        ),
        (
            dict(site_opens=5, last_activity_timestamp=now - timedelta(days=80)),
            SegmentType.ACTIVE,
            [SegmentType.PROSPECTIVE, SegmentType.LOST],
        ),
        (
            dict(site_opens=1, last_activity_timestamp=now - timedelta(days=100)),
            SegmentType.LOST,
            [SegmentType.ACTIVE, SegmentType.PROSPECTIVE],
        ),
        (
            dict(site_opens=5, last_activity_timestamp=now - timedelta(days=100)),
            SegmentType.LOST,
            [SegmentType.ACTIVE, SegmentType.PROSPECTIVE],
        ),
    ],
)
async def test_does_not_segment_lead_as_active_prospective_and_lost_simultaneously(
    event_details, expected_segment, unexpected_segments, factory, dm
):
    await factory.create_lead_with_events(**event_details)

    got = await dm.list_leads(biz_id=123, limit=100500, offset=0)

    lead = got["leads"][0]
    assert expected_segment in lead["segments"]
    for unexpected_segment in unexpected_segments:
        assert unexpected_segment not in lead["segments"]


async def test_does_not_considers_another_lead_events(factory, dm):
    lead_id_1 = await factory.create_lead_with_events(
        passport_uid="111",
        review_rating=3,
        last_activity_timestamp=now - timedelta(days=100),
    )
    lead_id_2 = await factory.create_lead_with_events(
        passport_uid="222",
        site_opens=2,
        clicks_on_phone=4,
        last_activity_timestamp=now - timedelta(days=80),
    )

    got = await dm.list_leads(biz_id=123, limit=100500, offset=0)

    lead_1 = [lead for lead in got["leads"] if lead["lead_id"] == lead_id_1][0]
    assert lead_1["segments"] == [SegmentType.LOST, SegmentType.DISLOYAL]

    lead_2 = [lead for lead in got["leads"] if lead["lead_id"] == lead_id_2][0]
    assert lead_2["segments"] == [SegmentType.ACTIVE]
