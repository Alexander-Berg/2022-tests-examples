from datetime import datetime, timedelta, timezone

import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.promoter.proto.errors_pb2 import Error
from maps_adv.geosmb.promoter.proto.leads_pb2 import (
    Lead,
    RetrieveLeadInput,
    Source,
    Statistics,
)
from maps_adv.geosmb.promoter.proto.segments_pb2 import SegmentType
from maps_adv.geosmb.promoter.server.lib.enums import Source as SourceEnum

pytestmark = [pytest.mark.asyncio]

now = datetime.now(tz=timezone.utc)

url = "/v1/retrieve_lead/"


async def test_returns_lead_details(factory, api):
    lead_id = await factory.create_lead_with_events(
        passport_uid="1111",
        name="username_1",
        lead_source=SourceEnum.STRAIGHT,
        clicks_on_phone=1,
        last_activity_timestamp=now - timedelta(days=80),
    )

    got = await api.post(
        url,
        proto=RetrieveLeadInput(lead_id=lead_id, biz_id=123),
        decode_as=Lead,
        expected_status=200,
    )

    assert got == Lead(
        lead_id=lead_id,
        biz_id=123,
        name="username_1",
        source=Source.STRAIGHT,
        segments=[SegmentType.PROSPECTIVE],
        statistics=Statistics(
            make_routes=0,
            clicks_on_phone=1,
            site_opens=0,
            view_working_hours=0,
            view_entrances=0,
            cta_button_click=0,
            favourite_click=0,
            location_sharing=0,
            booking_section_interaction=0,
            showcase_product_click=0,
            promo_to_site=0,
            geoproduct_button_click=0,
            last_activity_timestamp=got.statistics.last_activity_timestamp,
        ),
    )


async def test_does_not_return_review_rating_if_none(factory, api):
    lead_id = await factory.create_lead_with_events(
        make_routes=4, clicks_on_phone=1, site_opens=2
    )

    got = await api.post(
        url,
        proto=RetrieveLeadInput(lead_id=lead_id, biz_id=123),
        decode_as=Lead,
        expected_status=200,
    )

    assert not got.statistics.HasField("review_rating")


async def test_errored_if_lead_not_found(factory, api):
    got = await api.post(
        url,
        proto=RetrieveLeadInput(lead_id=100500, biz_id=123),
        decode_as=Error,
        expected_status=404,
    )

    assert got == Error(
        code=Error.UNKNOWN_LEAD,
        description="Unknown lead with biz_id=123, lead_id=100500",
    )


async def test_errored_if_lead_exists_in_another_business(factory, api):
    lead_id = await factory.create_lead_with_events(
        passport_uid="1111",
        name="username_1",
        clicks_on_phone=1,
        last_activity_timestamp=dt("2020-05-01 18:00:00"),
    )

    got = await api.post(
        url,
        proto=RetrieveLeadInput(lead_id=lead_id, biz_id=456),
        decode_as=Error,
        expected_status=404,
    )

    assert got == Error(
        code=Error.UNKNOWN_LEAD,
        description=f"Unknown lead with biz_id=456, lead_id={lead_id}",
    )


@pytest.mark.parametrize(
    "kw, msg",
    (
        [dict(biz_id=0, lead_id=1), "biz_id: ['Must be at least 1.']"],
        [dict(biz_id=1, lead_id=0), "lead_id: ['Must be at least 1.']"],
        [
            dict(biz_id=0, lead_id=0),
            "biz_id: ['Must be at least 1.'], lead_id: ['Must be at least 1.']",
        ],
    ),
)
async def test_errored_for_wrong_input(kw, msg, api):
    got = await api.post(
        url, proto=RetrieveLeadInput(**kw), decode_as=Error, expected_status=400
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=msg)
