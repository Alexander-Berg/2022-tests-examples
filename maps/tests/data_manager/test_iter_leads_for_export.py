from datetime import datetime, timedelta, timezone

import pytest

from maps_adv.geosmb.promoter.server.lib.enums import EventType

pytestmark = [pytest.mark.asyncio]

now = datetime.now(tz=timezone.utc)


async def test_iterator_is_empty_if_there_are_no_data(dm):
    records = []
    async for record in dm.iter_leads_for_export(2):
        records.extend(record)

    assert len(records) == 0


async def test_returns_all_records_for_all_biz_ids(dm, factory):
    for i in range(10):
        await factory.create_lead(biz_id=i, passport_uid=str(i), name=f"username_{i}")

    records = []
    async for record in dm.iter_leads_for_export(2):
        records.extend(record)

    assert len(records) == 10


@pytest.mark.parametrize("size", range(1, 6))
async def test_returns_records_by_passed_chunks(size, factory, dm):
    for i in range(10):
        await factory.create_lead(passport_uid=str(i), name=f"username_{i}")

    records = []
    async for record in dm.iter_leads_for_export(size):
        records.append(record)

    assert len(records[0]) == size
    assert len(records[1]) == size


async def test_returns_all_expected_columns(factory, dm):
    lead_id = await factory.create_lead_with_events(
        biz_id=123,
        passport_uid="456",
        yandex_uid="789",
        device_id="abc",
        name="отец онотолий",
    )

    records = []
    async for _recs in dm.iter_leads_for_export(2):
        records.extend(_recs)

    assert records[0] == dict(
        promoter_id=lead_id,
        biz_id=123,
        passport_uid="456",
        yandex_uid="789",
        device_id="abc",
        name="отец онотолий",
        segments=[],
    )


@pytest.mark.parametrize(
    "kw, segment",
    (
        [  # prospective if less than 3 events in last 90 days
            dict(
                clicks_on_phone=1,
                site_opens=1,
                last_activity_timestamp=now - timedelta(days=89),
            ),
            "PROSPECTIVE",
        ],
        [  # active if 3 or more events in last 90 days
            dict(
                review_rating=5,
                clicks_on_phone=1,
                site_opens=1,
                last_activity_timestamp=now - timedelta(days=89),
            ),
            "ACTIVE",
        ],
        [  # lost if more 90 days since last activity
            dict(
                review_rating=5,
                clicks_on_phone=1,
                site_opens=1,
                last_activity_timestamp=now - timedelta(days=100),
            ),
            "LOST",
        ],
        [dict(review_rating=3), "DISLOYAL"],  # disloyal if review rating lt 4
        [dict(review_rating=4), "LOYAL"],  # loyal if review rating eq 4
        [dict(review_rating=5), "LOYAL"],  # loyal if review rating gt 4
    ),
)
async def test_returns_valid_segments(kw, segment, factory, dm):
    await factory.create_lead_with_events(**kw)

    records = []
    async for _recs in dm.iter_leads_for_export(2):
        records.extend(_recs)

    assert segment in records[0]["segments"]


async def test_calculates_segment_as_event_stats_by_event_amount(factory, dm):
    await factory.create_lead_with_event(
        event_type=EventType.OPEN_SITE,
        events_amount=3,
        event_timestamp=now - timedelta(days=80),
    )

    records = []
    async for _recs in dm.iter_leads_for_export(2):
        records.extend(_recs)

    assert "ACTIVE" in records[0]["segments"]


async def test_does_not_segment_as_lost_has_any_recent_event(factory, dm):
    # events older than 90 days
    lead_id = await factory.create_lead_with_events(
        clicks_on_phone=1,
        site_opens=1,
        last_activity_timestamp=now - timedelta(days=100),
    )
    # event in last 90 days
    await factory.create_event(
        lead_id=lead_id,
        event_type=EventType.MAKE_ROUTE,
        event_timestamp=now - timedelta(days=80),
    )

    records = []
    async for _recs in dm.iter_leads_for_export(2):
        records.extend(_recs)

    assert "LOST" not in records[0]["segments"]


@pytest.mark.parametrize(
    "review_rating, expected_segment, unexpected_segment",
    [(2, "DISLOYAL", "LOYAL"), (5, "LOYAL", "DISLOYAL")],
)
async def test_does_not_segments_loyal_and_disloyal_simultaneously(
    review_rating, expected_segment, unexpected_segment, factory, dm
):
    await factory.create_lead_with_events(review_rating=review_rating)

    records = []
    async for _recs in dm.iter_leads_for_export(2):
        records.extend(_recs)

    assert expected_segment in records[0]["segments"]
    assert unexpected_segment not in records[0]["segments"]


@pytest.mark.parametrize(
    "event_details, expected_segment, unexpected_segments",
    [
        (
            dict(site_opens=1, last_activity_timestamp=now - timedelta(days=80)),
            "PROSPECTIVE",
            {"ACTIVE", "LOST"},
        ),
        (
            dict(site_opens=5, last_activity_timestamp=now - timedelta(days=80)),
            "ACTIVE",
            {"PROSPECTIVE", "LOST"},
        ),
        (
            dict(site_opens=1, last_activity_timestamp=now - timedelta(days=100)),
            "LOST",
            {"ACTIVE", "PROSPECTIVE"},
        ),
        (
            dict(site_opens=5, last_activity_timestamp=now - timedelta(days=100)),
            "LOST",
            {"ACTIVE", "PROSPECTIVE"},
        ),
    ],
)
async def test_no_conflict_segmentation(
    event_details, expected_segment, unexpected_segments, factory, dm
):
    await factory.create_lead_with_events(**event_details)

    records = []
    async for _recs in dm.iter_leads_for_export(2):
        records.extend(_recs)

    got = records[0]["segments"]
    assert expected_segment in got
    assert not unexpected_segments.intersection(got)


async def test_does_not_consider_another_lead_events(factory, dm):
    await factory.create_lead_with_events(
        passport_uid="111",
        review_rating=3,
        last_activity_timestamp=now - timedelta(days=100),
    )
    await factory.create_lead_with_events(
        passport_uid="222",
        site_opens=2,
        clicks_on_phone=4,
        last_activity_timestamp=now - timedelta(days=80),
    )

    records = []
    async for _recs in dm.iter_leads_for_export(2):
        records.extend(_recs)

    records[0]["segments"] = ["DISLOYAL", "LOST"]  # lead 1
    records[1]["segments"] = ["ACTIVE"]  # lead 2
