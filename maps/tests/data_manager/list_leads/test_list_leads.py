from datetime import datetime, timedelta, timezone

import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.promoter.server.lib.enums import SegmentType, Source

pytestmark = [pytest.mark.asyncio]

now = datetime.now(tz=timezone.utc)


async def test_returns_list_of_leads_with_stats(factory, dm):
    lead_id_1 = await factory.create_lead_with_events(
        passport_uid="1111",
        name="username_1",
        lead_source=Source.STRAIGHT,
        review_rating=5,
        make_routes=2,
        clicks_on_phone=1,
        site_opens=2,
        view_entrances=2,
        last_activity_timestamp=now - timedelta(days=80),
    )
    lead_id_2 = await factory.create_lead_with_events(
        passport_uid="2222",
        name="username_2",
        lead_source=Source.DISCOVERY_ADVERT,
        clicks_on_phone=1,
        location_sharing=1,
        last_activity_timestamp=now - timedelta(days=60),
    )

    got = await dm.list_leads(biz_id=123, limit=100500, offset=0)

    assert got == dict(
        total_count=2,
        leads=[
            dict(
                lead_id=lead_id_2,
                biz_id=123,
                name="username_2",
                source=Source.DISCOVERY_ADVERT,
                segments=[SegmentType.PROSPECTIVE],
                statistics=dict(
                    review_rating=None,
                    make_routes=0,
                    clicks_on_phone=1,
                    site_opens=0,
                    view_working_hours=0,
                    view_entrances=0,
                    showcase_product_click=0,
                    promo_to_site=0,
                    location_sharing=1,
                    geoproduct_button_click=0,
                    favourite_click=0,
                    cta_button_click=0,
                    booking_section_interaction=0,
                    last_activity_timestamp=now - timedelta(days=60),
                ),
            ),
            dict(
                lead_id=lead_id_1,
                biz_id=123,
                name="username_1",
                segments=[SegmentType.ACTIVE, SegmentType.LOYAL],
                source=Source.STRAIGHT,
                statistics=dict(
                    review_rating="5",
                    make_routes=2,
                    clicks_on_phone=1,
                    site_opens=2,
                    view_working_hours=0,
                    view_entrances=2,
                    showcase_product_click=0,
                    promo_to_site=0,
                    location_sharing=0,
                    geoproduct_button_click=0,
                    favourite_click=0,
                    cta_button_click=0,
                    booking_section_interaction=0,
                    last_activity_timestamp=now - timedelta(days=80),
                ),
            ),
        ],
    )


async def test_sorts_by_last_activity_ts_desc_by_default(factory, dm):
    lead_id_1 = await factory.create_lead_with_events(
        passport_uid="1111",
        site_opens=1,
        last_activity_timestamp=dt("2020-03-03 00:00:00"),
    )
    lead_id_2 = await factory.create_lead_with_events(
        passport_uid="2222",
        site_opens=1,
        last_activity_timestamp=dt("2019-03-03 00:00:00"),
    )

    got = await dm.list_leads(biz_id=123, limit=100500, offset=0)

    assert [lead["lead_id"] for lead in got["leads"]] == [lead_id_1, lead_id_2]


async def test_returns_nothing_if_no_leads(factory, dm):
    got = await dm.list_leads(biz_id=123, limit=100500, offset=0)

    assert got == dict(total_count=0, leads=[])


async def test_does_not_return_other_business_events(factory, dm):
    await factory.create_lead_with_events(biz_id=564, review_rating=5, make_routes=4)

    got = await dm.list_leads(biz_id=123, limit=100500, offset=0)

    assert got == dict(total_count=0, leads=[])
