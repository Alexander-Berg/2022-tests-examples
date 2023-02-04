from datetime import datetime, timedelta, timezone

import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.promoter.server.lib.enums import SegmentType, Source
from maps_adv.geosmb.promoter.server.lib.exceptions import UnknownLead

pytestmark = [pytest.mark.asyncio]

now = datetime.now(tz=timezone.utc)


async def test_returns_lead_details(factory, dm):
    lead_id = await factory.create_lead_with_events(
        passport_uid="1111",
        name="username_1",
        lead_source=Source.STRAIGHT,
        clicks_on_phone=1,
        last_activity_timestamp=now - timedelta(days=80),
    )

    got = await dm.retrieve_lead(lead_id=lead_id, biz_id=123)

    assert got == dict(
        lead_id=lead_id,
        biz_id=123,
        name="username_1",
        source=Source.STRAIGHT,
        segments=[SegmentType.PROSPECTIVE],
        statistics=dict(
            clicks_on_phone=1,
            site_opens=0,
            make_routes=0,
            review_rating=None,
            view_working_hours=0,
            view_entrances=0,
            showcase_product_click=0,
            promo_to_site=0,
            location_sharing=0,
            geoproduct_button_click=0,
            favourite_click=0,
            cta_button_click=0,
            booking_section_interaction=0,
            last_activity_timestamp=now - timedelta(days=80),
        ),
    )


async def test_errored_if_lead_not_found(factory, dm):
    with pytest.raises(
        UnknownLead, match="Unknown lead with biz_id=123, lead_id=100500"
    ):
        await dm.retrieve_lead(lead_id=100500, biz_id=123)


async def test_errored_if_lead_exist_in_another_business(factory, dm):
    lead_id = await factory.create_lead_with_events(
        passport_uid="1111",
        name="username_1",
        clicks_on_phone=1,
        last_activity_timestamp=dt("2020-05-01 18:00:00"),
    )

    with pytest.raises(
        UnknownLead, match=f"Unknown lead with biz_id=456, lead_id={lead_id}"
    ):
        await dm.retrieve_lead(lead_id=lead_id, biz_id=456)
