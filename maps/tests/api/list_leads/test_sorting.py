import pytest

from maps_adv.common.helpers import dt
from maps_adv.geosmb.promoter.proto.leads_pb2 import (
    ListLeadsInput,
    ListLeadsOutput,
    OrderBy,
)
from maps_adv.geosmb.promoter.proto.segments_pb2 import SegmentType
from maps_adv.geosmb.proto.common_pb2 import Pagination

pytestmark = [pytest.mark.asyncio]

url = "/v1/list_leads/"


async def test_sorts_by_lead_created_desc_at_as_secondary_sorting_in_default_sorting(
    factory, api
):
    for idx in range(1, 4):
        await factory.create_lead_with_event(
            passport_uid=str(idx),
            lead_id=idx + 100,
            event_timestamp=dt("2020-03-03 00:00:00"),
        )

    got = await api.post(
        url,
        proto=ListLeadsInput(biz_id=123, pagination=Pagination(limit=100500, offset=0)),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert [lead.lead_id for lead in got.leads] == [103, 102, 101]


@pytest.mark.parametrize(
    "direction, expected_ids",
    [
        (OrderBy.OrderDirection.ASC, [101, 102, 103]),
        (OrderBy.OrderDirection.DESC, [103, 102, 101]),
    ],
)
async def test_sorts_by_username(direction, expected_ids, factory, api):
    for idx in range(1, 4):
        await factory.create_lead_with_event(
            passport_uid=str(idx), lead_id=idx + 100, name=f"username_{idx}"
        )

    got = await api.post(
        url,
        proto=ListLeadsInput(
            biz_id=123,
            order_by=OrderBy(field=OrderBy.OrderField.NAME, direction=direction),
            pagination=Pagination(limit=100500, offset=0),
        ),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert [lead.lead_id for lead in got.leads] == expected_ids


@pytest.mark.parametrize(
    "direction", [OrderBy.OrderDirection.ASC, OrderBy.OrderDirection.DESC]
)
async def test_sorts_by_lead_created_at_desc_as_secondary_sort_on_sort_by_username(
    direction, factory, api
):
    for idx in range(1, 4):
        await factory.create_lead_with_event(
            passport_uid=str(idx), lead_id=idx + 100, name="username"
        )

    got = await api.post(
        url,
        proto=ListLeadsInput(
            biz_id=123,
            order_by=OrderBy(field=OrderBy.OrderField.NAME, direction=direction),
            pagination=Pagination(limit=100500, offset=0),
        ),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert [lead.lead_id for lead in got.leads] == [103, 102, 101]


@pytest.mark.parametrize(
    "events_type, order_by_field",
    [
        ("review_rating", OrderBy.OrderField.REVIEW_RATING),
        ("make_routes", OrderBy.OrderField.MAKE_ROUTES),
        ("clicks_on_phone", OrderBy.OrderField.CLICKS_ON_PHONE),
        ("site_opens", OrderBy.OrderField.SITE_OPENS),
        ("view_working_hours", OrderBy.OrderField.VIEW_WORKING_HOURS),
        ("view_entrances", OrderBy.OrderField.VIEW_ENTRANCES),
        ("cta_button_click", OrderBy.OrderField.CTA_BUTTON_CLICK),
        ("favourite_click", OrderBy.OrderField.FAVOURITE_CLICK),
        ("location_sharing", OrderBy.OrderField.LOCATION_SHARING),
        ("booking_section_interaction", OrderBy.OrderField.BOOKING_SECTION_INTERACTION),
        ("showcase_product_click", OrderBy.OrderField.SHOWCASE_PRODUCT_CLICK),
        ("promo_to_site", OrderBy.OrderField.PROMO_TO_SITE),
        ("geoproduct_button_click", OrderBy.OrderField.GEOPRODUCT_BUTTON_CLICK),
    ],
)
@pytest.mark.parametrize(
    "direction, expected_ids",
    [
        (OrderBy.OrderDirection.ASC, [101, 102, 103]),
        (OrderBy.OrderDirection.DESC, [103, 102, 101]),
    ],
)
async def test_sorts_by_stat_fields(
    events_type, order_by_field, direction, expected_ids, factory, api
):
    for idx in range(1, 4):
        await factory.create_lead_with_events(
            passport_uid=str(idx), lead_id=idx + 100, **{events_type: idx}
        )

    got = await api.post(
        url,
        proto=ListLeadsInput(
            biz_id=123,
            order_by=OrderBy(field=order_by_field, direction=direction),
            pagination=Pagination(limit=100500, offset=0),
        ),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert [lead.lead_id for lead in got.leads] == expected_ids


@pytest.mark.parametrize(
    "events_type, order_by_field",
    [
        ("review_rating", OrderBy.OrderField.REVIEW_RATING),
        ("make_routes", OrderBy.OrderField.MAKE_ROUTES),
        ("clicks_on_phone", OrderBy.OrderField.CLICKS_ON_PHONE),
        ("site_opens", OrderBy.OrderField.SITE_OPENS),
        ("view_working_hours", OrderBy.OrderField.VIEW_WORKING_HOURS),
        ("view_entrances", OrderBy.OrderField.VIEW_ENTRANCES),
        ("cta_button_click", OrderBy.OrderField.CTA_BUTTON_CLICK),
        ("favourite_click", OrderBy.OrderField.FAVOURITE_CLICK),
        ("location_sharing", OrderBy.OrderField.LOCATION_SHARING),
        ("booking_section_interaction", OrderBy.OrderField.BOOKING_SECTION_INTERACTION),
        ("showcase_product_click", OrderBy.OrderField.SHOWCASE_PRODUCT_CLICK),
        ("promo_to_site", OrderBy.OrderField.PROMO_TO_SITE),
        ("geoproduct_button_click", OrderBy.OrderField.GEOPRODUCT_BUTTON_CLICK),
    ],
)
@pytest.mark.parametrize(
    "direction", [OrderBy.OrderDirection.ASC, OrderBy.OrderDirection.DESC]
)
async def test_sorts_by_lead_created_at_desc_as_secondary_sort_on_sort_by_stat_fields(
    events_type, order_by_field, direction, factory, api
):
    for idx in range(1, 4):
        await factory.create_lead_with_events(
            passport_uid=str(idx), lead_id=idx + 100, **{events_type: 1}
        )

    got = await api.post(
        url,
        proto=ListLeadsInput(
            biz_id=123,
            order_by=OrderBy(field=order_by_field, direction=direction),
            pagination=Pagination(limit=100500, offset=0),
        ),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert [lead.lead_id for lead in got.leads] == [103, 102, 101]


@pytest.mark.parametrize(
    "direction, expected_ids",
    [
        (OrderBy.OrderDirection.ASC, [101, 103, 102, 104]),
        (OrderBy.OrderDirection.DESC, [104, 102, 103, 101]),
    ],
)
async def test_sorts_by_last_event_timestamp(direction, expected_ids, factory, api):
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

    got = await api.post(
        url,
        proto=ListLeadsInput(
            biz_id=123,
            order_by=OrderBy(
                field=OrderBy.OrderField.LAST_ACTIVITY_TIMESTAMP, direction=direction
            ),
            pagination=Pagination(limit=100500, offset=0),
        ),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert [lead.lead_id for lead in got.leads] == expected_ids


@pytest.mark.parametrize(
    "direction", [OrderBy.OrderDirection.ASC, OrderBy.OrderDirection.DESC]
)
async def test_sorts_by_lead_created_at_desc_as_secondary_sort_on_sort_by_last_activity_ts(  # noqa
    direction, factory, api
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

    got = await api.post(
        url,
        proto=ListLeadsInput(
            biz_id=123,
            order_by=OrderBy(
                field=OrderBy.OrderField.LAST_ACTIVITY_TIMESTAMP, direction=direction
            ),
            pagination=Pagination(limit=100500, offset=0),
        ),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert [lead.lead_id for lead in got.leads] == [104, 103, 102, 101]


@pytest.mark.parametrize(
    "order_field",
    [
        OrderBy.OrderField.NAME,
        OrderBy.OrderField.REVIEW_RATING,
        OrderBy.OrderField.MAKE_ROUTES,
        OrderBy.OrderField.CLICKS_ON_PHONE,
        OrderBy.OrderField.SITE_OPENS,
        OrderBy.OrderField.LAST_ACTIVITY_TIMESTAMP,
        OrderBy.OrderField.VIEW_WORKING_HOURS,
        OrderBy.OrderField.VIEW_ENTRANCES,
        OrderBy.OrderField.CTA_BUTTON_CLICK,
        OrderBy.OrderField.FAVOURITE_CLICK,
        OrderBy.OrderField.LOCATION_SHARING,
        OrderBy.OrderField.BOOKING_SECTION_INTERACTION,
        OrderBy.OrderField.SHOWCASE_PRODUCT_CLICK,
        OrderBy.OrderField.PROMO_TO_SITE,
        OrderBy.OrderField.GEOPRODUCT_BUTTON_CLICK,
    ],
)
@pytest.mark.parametrize(
    "direction, expected_ids",
    [
        (OrderBy.OrderDirection.ASC, [100, 102]),
        (OrderBy.OrderDirection.DESC, [102, 100]),
    ],
)
async def test_sorts_by_specified_field_if_filtered_by_segment(
    order_field, direction, expected_ids, factory, api
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

    got = await api.post(
        url,
        proto=ListLeadsInput(
            biz_id=123,
            filter_by_segment=SegmentType.LOYAL,
            order_by=OrderBy(field=order_field, direction=direction),
            pagination=Pagination(limit=100500, offset=0),
        ),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert got.total_count == 2
    assert [lead.lead_id for lead in got.leads] == expected_ids
