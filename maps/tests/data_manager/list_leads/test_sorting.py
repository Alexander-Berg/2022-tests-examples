import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.promoter.server.lib.enums import (
    OrderByField,
    OrderDirection,
    SegmentType,
)
from maps_adv.geosmb.promoter.server.lib.exceptions import BadSortingParams

pytestmark = [pytest.mark.asyncio]


async def test_sorts_by_lead_created_at_desc_as_secondary_sorting_in_default_sorting(
    factory, dm
):
    for idx in range(1, 4):
        await factory.create_lead_with_event(
            passport_uid=str(idx),
            lead_id=idx + 100,
            event_timestamp=dt("2020-03-03 00:00:00"),
        )

    got = await dm.list_leads(biz_id=123, limit=100500, offset=0)

    assert [lead["lead_id"] for lead in got["leads"]] == [103, 102, 101]


@pytest.mark.parametrize(
    "direction, expected_ids",
    [(OrderDirection.ASC, [101, 102, 103]), (OrderDirection.DESC, [103, 102, 101])],
)
async def test_sorts_by_username(direction, expected_ids, factory, dm):
    for idx in range(1, 4):
        await factory.create_lead_with_event(
            passport_uid=str(idx), lead_id=idx + 100, name=f"username_{idx}"
        )

    got = await dm.list_leads(
        biz_id=123,
        order_by_field=OrderByField.NAME,
        order_direction=direction,
        limit=100500,
        offset=0,
    )

    assert [lead["lead_id"] for lead in got["leads"]] == expected_ids


@pytest.mark.parametrize("direction", OrderDirection)
async def test_sorts_by_lead_created_at_desc_as_secondary_sort_on_sort_by_username(
    direction, factory, dm
):
    for idx in range(1, 4):
        await factory.create_lead_with_event(
            passport_uid=str(idx), lead_id=idx + 100, name="username"
        )

    got = await dm.list_leads(
        biz_id=123,
        order_by_field=OrderByField.NAME,
        order_direction=direction,
        limit=100500,
        offset=0,
    )

    assert [lead["lead_id"] for lead in got["leads"]] == [103, 102, 101]


@pytest.mark.parametrize(
    "events_type, order_by_field",
    [
        ("review_rating", OrderByField.REVIEW_RATING),
        ("make_routes", OrderByField.MAKE_ROUTES),
        ("clicks_on_phone", OrderByField.CLICKS_ON_PHONE),
        ("site_opens", OrderByField.SITE_OPENS),
        ("view_working_hours", OrderByField.VIEW_WORKING_HOURS),
        ("view_entrances", OrderByField.VIEW_ENTRANCES),
        ("cta_button_click", OrderByField.CTA_BUTTON_CLICK),
        ("favourite_click", OrderByField.FAVOURITE_CLICK),
        ("location_sharing", OrderByField.LOCATION_SHARING),
        ("booking_section_interaction", OrderByField.BOOKING_SECTION_INTERACTION),
        ("showcase_product_click", OrderByField.SHOWCASE_PRODUCT_CLICK),
        ("promo_to_site", OrderByField.PROMO_TO_SITE),
        ("geoproduct_button_click", OrderByField.GEOPRODUCT_BUTTON_CLICK),
    ],
)
@pytest.mark.parametrize(
    "direction, expected_ids",
    [(OrderDirection.ASC, [101, 102, 103]), (OrderDirection.DESC, [103, 102, 101])],
)
async def test_sorts_by_stat_fields(
    events_type, order_by_field, direction, expected_ids, factory, dm
):
    for idx in range(1, 4):
        await factory.create_lead_with_events(
            passport_uid=str(idx), lead_id=idx + 100, **{events_type: idx}
        )

    got = await dm.list_leads(
        biz_id=123,
        order_by_field=order_by_field,
        order_direction=direction,
        limit=100500,
        offset=0,
    )

    assert [lead["lead_id"] for lead in got["leads"]] == expected_ids


@pytest.mark.parametrize(
    "events_type, order_by_field",
    [
        ("review_rating", OrderByField.REVIEW_RATING),
        ("make_routes", OrderByField.MAKE_ROUTES),
        ("clicks_on_phone", OrderByField.CLICKS_ON_PHONE),
        ("site_opens", OrderByField.SITE_OPENS),
        ("view_working_hours", OrderByField.VIEW_WORKING_HOURS),
        ("view_entrances", OrderByField.VIEW_ENTRANCES),
        ("cta_button_click", OrderByField.CTA_BUTTON_CLICK),
        ("favourite_click", OrderByField.FAVOURITE_CLICK),
        ("location_sharing", OrderByField.LOCATION_SHARING),
        ("booking_section_interaction", OrderByField.BOOKING_SECTION_INTERACTION),
        ("showcase_product_click", OrderByField.SHOWCASE_PRODUCT_CLICK),
        ("promo_to_site", OrderByField.PROMO_TO_SITE),
        ("geoproduct_button_click", OrderByField.GEOPRODUCT_BUTTON_CLICK),
    ],
)
@pytest.mark.parametrize("direction", OrderDirection)
async def test_sorts_by_lead_created_at_desc_as_secondary_sort_on_sort_by_stat_fields(
    events_type, order_by_field, direction, factory, dm
):
    for idx in range(1, 4):
        await factory.create_lead_with_events(
            passport_uid=str(idx), lead_id=idx + 100, **{events_type: 1}
        )

    got = await dm.list_leads(
        biz_id=123,
        order_by_field=order_by_field,
        order_direction=direction,
        limit=100500,
        offset=0,
    )

    assert [lead["lead_id"] for lead in got["leads"]] == [103, 102, 101]


@pytest.mark.parametrize(
    "direction, expected_ids",
    [
        (OrderDirection.ASC, [101, 103, 102, 104]),
        (OrderDirection.DESC, [104, 102, 103, 101]),
    ],
)
async def test_sorts_by_last_activity_timestamp(direction, expected_ids, factory, dm):
    await factory.create_lead_with_events(
        lead_id=101,
        passport_uid="1111",
        site_opens=1,
        last_activity_timestamp=dt("2020-05-03 18:00:00"),
    )
    await factory.create_lead_with_events(
        lead_id=102,
        passport_uid="2222",
        make_routes=1,
        last_activity_timestamp=dt("2020-05-20 18:00:00"),
    )
    await factory.create_lead_with_events(
        lead_id=103,
        passport_uid="3333",
        clicks_on_phone=1,
        last_activity_timestamp=dt("2020-05-15 18:00:00"),
    )
    await factory.create_lead_with_events(
        lead_id=104,
        passport_uid="4444",
        review_rating=4,
        last_activity_timestamp=dt("2020-05-29 18:00:00"),
    )

    got = await dm.list_leads(
        biz_id=123,
        order_by_field=OrderByField.LAST_ACTIVITY_TIMESTAMP,
        order_direction=direction,
        limit=100500,
        offset=0,
    )

    assert [lead["lead_id"] for lead in got["leads"]] == expected_ids


@pytest.mark.parametrize("direction", OrderDirection)
async def test_sorts_by_lead_created_at_desc_as_secondary_sort_on_sort_by_last_activity_ts(  # noqa
    direction, factory, dm
):
    event_ts = dt("2020-05-03 18:00:00")
    await factory.create_lead_with_events(
        lead_id=101, passport_uid="1111", site_opens=1, last_activity_timestamp=event_ts
    )
    await factory.create_lead_with_events(
        lead_id=102,
        passport_uid="2222",
        make_routes=1,
        last_activity_timestamp=event_ts,
    )
    await factory.create_lead_with_events(
        lead_id=103,
        passport_uid="3333",
        clicks_on_phone=1,
        last_activity_timestamp=event_ts,
    )
    await factory.create_lead_with_events(
        lead_id=104,
        passport_uid="4444",
        review_rating=4,
        last_activity_timestamp=event_ts,
    )

    got = await dm.list_leads(
        biz_id=123,
        order_by_field=OrderByField.LAST_ACTIVITY_TIMESTAMP,
        order_direction=direction,
        limit=100500,
        offset=0,
    )

    assert [lead["lead_id"] for lead in got["leads"]] == [104, 103, 102, 101]


@pytest.mark.parametrize("order_field", list(OrderByField))
@pytest.mark.parametrize(
    "direction, expected_ids",
    [(OrderDirection.ASC, [100, 102]), (OrderDirection.DESC, [102, 100])],
)
async def test_sorts_by_specified_field_if_filtered_by_segment(
    order_field, direction, expected_ids, factory, dm
):
    # LOYAL
    await factory.create_lead_with_events(
        lead_id=100,
        passport_uid="1111",
        name="Иван",
        review_rating=4,
        last_activity_timestamp=dt("2020-05-01 18:00:00"),
    )
    await factory.create_lead_with_events(
        lead_id=101,
        passport_uid="2222",
        review_rating=3,
        last_activity_timestamp=dt("2020-04-01 18:00:00"),
    )
    # LOYAL
    await factory.create_lead_with_events(
        lead_id=102,
        name="Пётр",
        passport_uid="5555",
        review_rating=5,
        make_routes=4,
        site_opens=1,
        clicks_on_phone=1,
        view_working_hours=1,
        view_entrances=1,
        cta_button_click=1,
        favourite_click=1,
        location_sharing=1,
        booking_section_interaction=1,
        showcase_product_click=1,
        promo_to_site=1,
        geoproduct_button_click=1,
        last_activity_timestamp=dt("2020-06-01 18:00:00"),
    )

    got = await dm.list_leads(
        biz_id=123,
        order_by_field=order_field,
        order_direction=direction,
        filter_by_segment=SegmentType.LOYAL,
        limit=100500,
        offset=0,
    )

    assert got["total_count"] == 2
    assert [lead["lead_id"] for lead in got["leads"]] == expected_ids


@pytest.mark.parametrize("order_by_field", OrderByField)
async def test_raises_for_for_incomplete_sorting_params(order_by_field, factory, dm):
    with pytest.raises(BadSortingParams):
        await dm.list_leads(
            biz_id=123,
            order_by_field=order_by_field,
            order_direction=None,
            limit=100500,
            offset=0,
        )
