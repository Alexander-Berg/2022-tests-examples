from datetime import datetime, timedelta, timezone

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.promoter.server.lib.enums import EventType
from maps_adv.geosmb.promoter.server.tests import (
    make_import_event_data,
    make_import_events_generator,
)

pytestmark = [pytest.mark.asyncio]

now = datetime.now(tz=timezone.utc)


@pytest.mark.parametrize(
    ("event_type", "stats_field"),
    [
        ("CLICK_ON_PHONE", "total_clicks_on_phone"),
        ("OPEN_SITE", "total_site_opens"),
        ("MAKE_ROUTE", "total_make_routes"),
        ("VIEW_WORKING_HOURS", "total_view_working_hours"),
        ("VIEW_ENTRANCES", "total_view_entrances"),
        ("CTA_BUTTON_CLICK", "total_cta_button_click"),
        ("FAVOURITE_CLICK", "total_favourite_click"),
        ("LOCATION_SHARING", "total_location_sharing"),
        ("BOOKING_SECTION_INTERACTION", "total_booking_section_interaction"),
        ("SHOWCASE_PRODUCT_CLICK", "total_showcase_product_click"),
        ("PROMO_TO_SITE", "total_promo_to_site"),
        ("GEOPRODUCT_BUTTON_CLICK", "total_geoproduct_button_click"),
    ],
)
async def test_calculates_stat_by_events(dm, factory, event_type, stats_field):
    lead_id = await factory.create_lead(biz_id=123, passport_uid="111")

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(
                    biz_id=123,
                    passport_uid="111",
                    event_type=event_type,
                    event_amount=3,
                    event_timestamp=now - timedelta(hours=2),
                ),
                make_import_event_data(
                    biz_id=123,
                    passport_uid="111",
                    event_type=event_type,
                    event_amount=1,
                    event_timestamp=now - timedelta(hours=1),
                ),
            ]
        )
    )

    expected_lead_stats = {
        "id": Any(int),
        "biz_id": 123,
        "lead_id": lead_id,
        "last_activity_timestamp": now - timedelta(hours=1),
        "last_3_events_timestamp": now - timedelta(hours=2),
        "last_review_rating": Any(object),
        "total_clicks_on_phone": 0,
        "total_make_routes": 0,
        "total_site_opens": 0,
        "total_view_working_hours": 0,
        "total_view_entrances": 0,
        "total_showcase_product_click": 0,
        "total_promo_to_site": 0,
        "total_location_sharing": 0,
        "total_geoproduct_button_click": 0,
        "total_favourite_click": 0,
        "total_cta_button_click": 0,
        "total_booking_section_interaction": 0,
    }
    expected_lead_stats[stats_field] = 4

    events_stat = await factory.fetch_all_events_stat()
    assert events_stat == [expected_lead_stats]


@pytest.mark.parametrize(
    ("event_type", "stats_field"),
    [
        ("CLICK_ON_PHONE", "total_clicks_on_phone"),
        ("OPEN_SITE", "total_site_opens"),
        ("MAKE_ROUTE", "total_make_routes"),
        ("VIEW_WORKING_HOURS", "total_view_working_hours"),
        ("VIEW_ENTRANCES", "total_view_entrances"),
        ("CTA_BUTTON_CLICK", "total_cta_button_click"),
        ("FAVOURITE_CLICK", "total_favourite_click"),
        ("LOCATION_SHARING", "total_location_sharing"),
        ("BOOKING_SECTION_INTERACTION", "total_booking_section_interaction"),
        ("SHOWCASE_PRODUCT_CLICK", "total_showcase_product_click"),
        ("PROMO_TO_SITE", "total_promo_to_site"),
        ("GEOPRODUCT_BUTTON_CLICK", "total_geoproduct_button_click"),
    ],
)
async def test_respects_existing_events(factory, dm, event_type, stats_field):
    lead_id = await factory.create_lead(biz_id=123, passport_uid="111")
    await factory.create_event(
        lead_id=lead_id,
        event_type=EventType[event_type],
        event_timestamp=now - timedelta(hours=3),
        events_amount=2,
    )

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(
                    biz_id=123,
                    passport_uid="111",
                    event_type=event_type,
                    event_amount=3,
                    event_timestamp=now - timedelta(hours=2),
                ),
            ]
        )
    )

    expected_lead_stats = {
        "id": Any(int),
        "biz_id": 123,
        "lead_id": lead_id,
        "last_activity_timestamp": now - timedelta(hours=2),
        "last_3_events_timestamp": now - timedelta(hours=2),
        "last_review_rating": Any(object),
        "total_clicks_on_phone": 0,
        "total_make_routes": 0,
        "total_site_opens": 0,
        "total_view_working_hours": 0,
        "total_view_entrances": 0,
        "total_showcase_product_click": 0,
        "total_promo_to_site": 0,
        "total_location_sharing": 0,
        "total_geoproduct_button_click": 0,
        "total_favourite_click": 0,
        "total_cta_button_click": 0,
        "total_booking_section_interaction": 0,
    }
    expected_lead_stats[stats_field] = 5

    events_stat = await factory.fetch_all_events_stat()
    assert events_stat == [expected_lead_stats]


async def test_calculates_last_activity_timestamp_correctly(factory, dm):
    lead_id = await factory.create_lead(biz_id=123, passport_uid="111")

    await factory.create_event(
        lead_id=lead_id,
        event_type=EventType.MAKE_ROUTE,
        event_timestamp=now - timedelta(hours=5),
        events_amount=2,
    )

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(
                    biz_id=123,
                    passport_uid="111",
                    event_type="OPEN_SITE",
                    event_amount=3,
                    event_timestamp=now - timedelta(hours=3),
                ),
                make_import_event_data(
                    biz_id=123,
                    passport_uid="111",
                    event_type="LOCATION_SHARING",
                    event_amount=3,
                    event_timestamp=now - timedelta(hours=4),
                ),
            ]
        )
    )

    events_stat = await factory.fetch_all_events_stat()
    assert events_stat[0]["last_activity_timestamp"] == now - timedelta(hours=3)


async def test_calculates_last_3_event_timestamp_respecting_event_timestamp(
    factory, dm
):
    lead_id = await factory.create_lead(biz_id=123, passport_uid="111")

    await factory.create_event(
        lead_id=lead_id,
        event_type=EventType.MAKE_ROUTE,
        event_timestamp=now - timedelta(hours=5),
        events_amount=1,
    )

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(
                    biz_id=123,
                    passport_uid="111",
                    event_type="MAKE_ROUTE",
                    event_amount=1,
                    event_timestamp=now - timedelta(hours=7),
                ),
                make_import_event_data(
                    biz_id=123,
                    passport_uid="111",
                    event_type="MAKE_ROUTE",
                    event_amount=1,
                    event_timestamp=now - timedelta(hours=6),
                ),
                make_import_event_data(
                    biz_id=123,
                    passport_uid="111",
                    event_type="MAKE_ROUTE",
                    event_amount=1,
                    event_timestamp=now - timedelta(hours=3),
                ),
            ]
        )
    )

    events_stat = await factory.fetch_all_events_stat()
    assert events_stat[0]["last_3_events_timestamp"] == now - timedelta(hours=6)


@pytest.mark.parametrize(
    ("events_amount", "expected_value"),
    [
        (1, None),
        (2, now - timedelta(hours=5)),
        (3, now - timedelta(hours=3)),
        (4, now - timedelta(hours=3)),
    ],
)
async def test_calculates_last_3_event_timestamp_respecting_events_amount(
    factory, dm, events_amount, expected_value
):
    lead_id = await factory.create_lead(biz_id=123, passport_uid="111")

    await factory.create_event(
        lead_id=lead_id,
        event_type=EventType.MAKE_ROUTE,
        event_timestamp=now - timedelta(hours=5),
        events_amount=1,
    )

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(
                    biz_id=123,
                    passport_uid="111",
                    event_type="OPEN_SITE",
                    event_amount=events_amount,
                    event_timestamp=now - timedelta(hours=3),
                ),
            ]
        )
    )

    events_stat = await factory.fetch_all_events_stat()
    assert events_stat[0]["last_3_events_timestamp"] == expected_value


async def test_calculates_stats_for_all_leads(factory, dm):
    lead_id_1 = await factory.create_lead(biz_id=123, passport_uid="111")
    lead_id_2 = await factory.create_lead(biz_id=123, passport_uid="222")
    lead_id_3 = await factory.create_lead(biz_id=456, passport_uid="111")

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(
                    biz_id=123,
                    passport_uid="111",
                    event_type="MAKE_ROUTE",
                    event_amount=1,
                    event_timestamp=now - timedelta(hours=1),
                ),
                make_import_event_data(
                    biz_id=123,
                    passport_uid="222",
                    event_type="CLICK_ON_PHONE",
                    event_amount=2,
                    event_timestamp=now - timedelta(hours=2),
                ),
                make_import_event_data(
                    biz_id=456,
                    passport_uid="111",
                    event_type="VIEW_ENTRANCES",
                    event_amount=3,
                    event_timestamp=now - timedelta(hours=3),
                ),
            ]
        )
    )

    events_stat = await factory.fetch_all_events_stat()
    assert events_stat == [
        {
            "id": Any(int),
            "biz_id": 123,
            "lead_id": lead_id_1,
            "last_activity_timestamp": now - timedelta(hours=1),
            "last_3_events_timestamp": None,
            "last_review_rating": Any(object),
            "total_clicks_on_phone": 0,
            "total_make_routes": 1,
            "total_site_opens": 0,
            "total_view_working_hours": 0,
            "total_view_entrances": 0,
            "total_showcase_product_click": 0,
            "total_promo_to_site": 0,
            "total_location_sharing": 0,
            "total_geoproduct_button_click": 0,
            "total_favourite_click": 0,
            "total_cta_button_click": 0,
            "total_booking_section_interaction": 0,
        },
        {
            "id": Any(int),
            "biz_id": 123,
            "lead_id": lead_id_2,
            "last_activity_timestamp": now - timedelta(hours=2),
            "last_3_events_timestamp": None,
            "last_review_rating": Any(object),
            "total_clicks_on_phone": 2,
            "total_make_routes": 0,
            "total_site_opens": 0,
            "total_view_working_hours": 0,
            "total_view_entrances": 0,
            "total_showcase_product_click": 0,
            "total_promo_to_site": 0,
            "total_location_sharing": 0,
            "total_geoproduct_button_click": 0,
            "total_favourite_click": 0,
            "total_cta_button_click": 0,
            "total_booking_section_interaction": 0,
        },
        {
            "id": Any(int),
            "biz_id": 456,
            "lead_id": lead_id_3,
            "last_activity_timestamp": now - timedelta(hours=3),
            "last_3_events_timestamp": now - timedelta(hours=3),
            "last_review_rating": Any(object),
            "total_clicks_on_phone": 0,
            "total_make_routes": 0,
            "total_site_opens": 0,
            "total_view_working_hours": 0,
            "total_view_entrances": 3,
            "total_showcase_product_click": 0,
            "total_promo_to_site": 0,
            "total_location_sharing": 0,
            "total_geoproduct_button_click": 0,
            "total_favourite_click": 0,
            "total_cta_button_click": 0,
            "total_booking_section_interaction": 0,
        },
    ]


@pytest.mark.parametrize("event_value", ["1", "3", "5"])
async def test_calculates_last_review_rating(factory, dm, event_value):
    lead_id = await factory.create_lead(biz_id=123, passport_uid="111")

    await factory.create_event(
        lead_id=lead_id,
        event_type=EventType.REVIEW,
        event_value=2,
        event_timestamp=now - timedelta(hours=5),
        events_amount=1,
    )

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(
                    biz_id=123,
                    passport_uid="111",
                    event_value=event_value,
                    event_type="REVIEW",
                    event_amount=1,
                    event_timestamp=now - timedelta(hours=3),
                ),
            ]
        )
    )

    events_stat = await factory.fetch_all_events_stat()
    assert events_stat[0]["last_review_rating"] == event_value


async def test_calculates_last_review_rating_as_none_if_no_review_events(factory, dm):
    lead_id = await factory.create_lead(biz_id=123, passport_uid="111")

    await factory.create_event(
        lead_id=lead_id,
        event_type=EventType.OPEN_SITE,
        event_value="223",
        event_timestamp=now - timedelta(hours=5),
        events_amount=1,
    )

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(
                    biz_id=123,
                    passport_uid="111",
                    event_value="322",
                    event_type="MAKE_ROUTE",
                    event_amount=1,
                    event_timestamp=now - timedelta(hours=3),
                ),
            ]
        )
    )

    events_stat = await factory.fetch_all_events_stat()
    assert events_stat[0]["last_review_rating"] is None


async def test_last_review_rating_as_none_not_changed_by_other_events(factory, dm):
    lead_id = await factory.create_lead(biz_id=123, passport_uid="111")

    await factory.create_event(
        lead_id=lead_id,
        event_type=EventType.REVIEW,
        event_value="4",
        event_timestamp=now - timedelta(hours=5),
        events_amount=1,
    )

    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(
                    biz_id=123,
                    passport_uid="111",
                    event_value="322",
                    event_type="MAKE_ROUTE",
                    event_amount=1,
                    event_timestamp=now - timedelta(hours=3),
                ),
            ]
        )
    )

    events_stat = await factory.fetch_all_events_stat()
    assert events_stat[0]["last_review_rating"] == "4"


async def test_calculates_stats_for_new_created_lead(factory, dm):
    await dm.import_events_from_generator(
        make_import_events_generator(
            [
                make_import_event_data(
                    biz_id=123,
                    passport_uid="111",
                    event_type="MAKE_ROUTE",
                    event_amount=3,
                    event_timestamp=now - timedelta(hours=3),
                ),
            ]
        )
    )

    events_stat = await factory.fetch_all_events_stat()
    assert events_stat == [
        {
            "id": Any(int),
            "biz_id": 123,
            "lead_id": Any(int),
            "last_activity_timestamp": now - timedelta(hours=3),
            "last_3_events_timestamp": now - timedelta(hours=3),
            "last_review_rating": Any(object),
            "total_clicks_on_phone": 0,
            "total_make_routes": 3,
            "total_site_opens": 0,
            "total_view_working_hours": 0,
            "total_view_entrances": 0,
            "total_showcase_product_click": 0,
            "total_promo_to_site": 0,
            "total_location_sharing": 0,
            "total_geoproduct_button_click": 0,
            "total_favourite_click": 0,
            "total_cta_button_click": 0,
            "total_booking_section_interaction": 0,
        }
    ]
