from datetime import datetime, timedelta, timezone

import pytest

from maps_adv.geosmb.promoter.proto.leads_pb2 import ListLeadsInput, ListLeadsOutput
from maps_adv.geosmb.promoter.proto.segments_pb2 import SegmentType
from maps_adv.geosmb.proto.common_pb2 import Pagination

pytestmark = [pytest.mark.asyncio]

now = datetime.now(tz=timezone.utc)

url = "/v1/list_leads/"


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
    segment, expected_total, expected_ids, factory, api
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
        location_sharing=4,
        last_activity_timestamp=now - timedelta(days=91),
    )
    # ACTIVE, DISLOYAL
    await factory.create_lead_with_events(
        lead_id=104,
        passport_uid="5555",
        review_rating=2,
        geoproduct_button_click=4,
        last_activity_timestamp=now - timedelta(days=20),
    )

    got = await api.post(
        url,
        proto=ListLeadsInput(
            biz_id=123,
            filter_by_segment=segment,
            pagination=Pagination(limit=100500, offset=0),
        ),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert got.total_count == expected_total
    assert [lead.lead_id for lead in got.leads] == expected_ids


async def test_returns_nothing_if_no_clients_in_segment(factory, api):
    # ACTIVE, LOYAL
    await factory.create_lead_with_events(
        lead_id=100,
        passport_uid="1111",
        review_rating=5,
        make_routes=4,
        last_activity_timestamp=now,
    )

    got = await api.post(
        url,
        proto=ListLeadsInput(
            biz_id=123,
            filter_by_segment=SegmentType.LOST,
            pagination=Pagination(limit=100500, offset=0),
        ),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert got == ListLeadsOutput(total_count=0, leads=[])
