from datetime import datetime, timedelta, timezone

import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.promoter.proto.errors_pb2 import Error
from maps_adv.geosmb.promoter.proto.leads_pb2 import (
    Lead,
    ListLeadsInput,
    ListLeadsOutput,
    Source,
    Statistics,
)
from maps_adv.geosmb.promoter.proto.segments_pb2 import SegmentType
from maps_adv.geosmb.promoter.server.lib.enums import Source as SourceEnum
from maps_adv.geosmb.proto.common_pb2 import Pagination

pytestmark = [pytest.mark.asyncio]

now = datetime.now(tz=timezone.utc)

url = "/v1/list_leads/"


@pytest.mark.freeze_time("2020-01-01 00:00:01")
async def test_returns_list_of_leads_with_stats(factory, api):
    lead_id_1 = await factory.create_lead_with_events(
        passport_uid="1111",
        name="username_1",
        lead_source=SourceEnum.STRAIGHT,
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
        lead_source=SourceEnum.DISCOVERY_ADVERT,
        clicks_on_phone=1,
        location_sharing=1,
        last_activity_timestamp=now - timedelta(days=60),
    )

    got = await api.post(
        url,
        proto=ListLeadsInput(biz_id=123, pagination=Pagination(limit=100500, offset=0)),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert got == ListLeadsOutput(
        total_count=2,
        leads=[
            Lead(
                lead_id=lead_id_2,
                biz_id=123,
                name="username_2",
                source=Source.DISCOVERY_ADVERT,
                segments=[SegmentType.PROSPECTIVE],
                statistics=Statistics(
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
                    last_activity_timestamp=got.leads[
                        0
                    ].statistics.last_activity_timestamp,
                ),
            ),
            Lead(
                lead_id=lead_id_1,
                biz_id=123,
                name="username_1",
                source=Source.STRAIGHT,
                segments=[SegmentType.ACTIVE, SegmentType.LOYAL],
                statistics=Statistics(
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
                    last_activity_timestamp=got.leads[
                        1
                    ].statistics.last_activity_timestamp,
                ),
            ),
        ],
    )


async def test_returns_lead_last_activity_timestamp_correctly(factory, api):
    lead_id_1 = await factory.create_lead_with_events(
        passport_uid="1111",
        clicks_on_phone=1,
        last_activity_timestamp=dt("2020-05-02 18:00:00"),
    )
    lead_id_2 = await factory.create_lead_with_events(
        passport_uid="2222",
        site_opens=1,
        last_activity_timestamp=dt("2020-06-02 18:00:00"),
    )

    got = await api.post(
        url,
        proto=ListLeadsInput(biz_id=123, pagination=Pagination(limit=100500, offset=0)),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    lead1_stat = [lead.statistics for lead in got.leads if lead.lead_id == lead_id_1][0]
    assert lead1_stat.last_activity_timestamp == dt(
        "2020-05-02 18:00:00", as_proto=True
    )
    lead2_stat = [lead.statistics for lead in got.leads if lead.lead_id == lead_id_2][0]
    assert lead2_stat.last_activity_timestamp == dt(
        "2020-06-02 18:00:00", as_proto=True
    )


async def test_sorts_by_last_activity_ts_desc_by_default(factory, api):
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

    got = await api.post(
        url,
        proto=ListLeadsInput(biz_id=123, pagination=Pagination(limit=100500, offset=0)),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert [lead.lead_id for lead in got.leads] == [lead_id_1, lead_id_2]


async def test_does_not_return_review_rating_in_stats_if_review_event_did_not_happen(
    factory, api
):
    await factory.create_lead_with_events(
        make_routes=4, clicks_on_phone=1, site_opens=2
    )

    got = await api.post(
        url,
        proto=ListLeadsInput(biz_id=123, pagination=Pagination(limit=100500, offset=0)),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    lead_stat = got.leads[0].statistics
    assert not lead_stat.HasField("review_rating")


async def test_returns_nothing_if_no_leads(factory, api):
    got = await api.post(
        url,
        proto=ListLeadsInput(biz_id=123, pagination=Pagination(limit=100500, offset=0)),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert got == ListLeadsOutput(total_count=0, leads=[])


async def test_does_not_return_other_business_leads(factory, api):
    await factory.create_lead_with_events(
        biz_id=564, review_rating=5, make_routes=4, clicks_on_phone=1, site_opens=2
    )

    got = await api.post(
        url,
        proto=ListLeadsInput(biz_id=123, pagination=Pagination(limit=100500, offset=0)),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert got == ListLeadsOutput(total_count=0, leads=[])


async def test_returns_error_for_wrong_biz_id(api):
    got = await api.post(
        url,
        proto=ListLeadsInput(biz_id=0, pagination=Pagination(limit=100500, offset=0)),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.VALIDATION_ERROR, description="biz_id: ['Must be at least 1.']"
    )
