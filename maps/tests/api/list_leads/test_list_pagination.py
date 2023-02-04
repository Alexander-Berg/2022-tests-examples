from datetime import datetime, timedelta, timezone

import pytest

from maps_adv.geosmb.promoter.proto.leads_pb2 import (
    ListLeadsInput,
    ListLeadsOutput,
    OrderBy,
)
from maps_adv.geosmb.promoter.proto.segments_pb2 import SegmentType
from maps_adv.geosmb.proto.common_pb2 import Pagination

pytestmark = [pytest.mark.asyncio]

now = datetime.now(tz=timezone.utc)

url = "/v1/list_leads/"


@pytest.mark.parametrize("limit", range(1, 4))
async def test_returns_empty_leads_list_if_offset_greater_than_total(
    limit, factory, api
):
    await factory.create_lead_with_events(passport_uid="1111", make_routes=4)
    await factory.create_lead_with_events(passport_uid="2222", site_opens=4)

    got = await api.post(
        url,
        proto=ListLeadsInput(
            biz_id=123, pagination=Pagination(limit=limit, offset=100500)
        ),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert got == ListLeadsOutput(total_count=2, leads=[])


async def test_returns_empty_leads_list_if_limit_0(factory, api):
    await factory.create_lead_with_events(passport_uid="1111", make_routes=4)

    got = await api.post(
        url,
        proto=ListLeadsInput(biz_id=123, pagination=Pagination(limit=0, offset=0)),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert got == ListLeadsOutput(total_count=1, leads=[])


@pytest.mark.parametrize("limit", range(1, 4))
async def test_respects_limit(limit, factory, api):
    lead_ids = list(
        reversed(
            [
                await factory.create_lead_with_events(
                    passport_uid=str(idx), make_routes=4
                )
                for idx in range(1, 4)
            ]
        )
    )

    got = await api.post(
        url,
        proto=ListLeadsInput(biz_id=123, pagination=Pagination(limit=limit, offset=0)),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert [lead.lead_id for lead in got.leads] == lead_ids[:limit]
    assert got.total_count == 3


@pytest.mark.parametrize("offset", range(4))
async def test_respects_offset(offset, factory, api):
    lead_ids = list(
        reversed(
            [
                await factory.create_lead_with_events(
                    passport_uid=str(idx), make_routes=4
                )
                for idx in range(1, 4)
            ]
        )
    )

    got = await api.post(
        url,
        proto=ListLeadsInput(biz_id=123, pagination=Pagination(limit=2, offset=offset)),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert [lead.lead_id for lead in got.leads] == lead_ids[offset : offset + 2]  # noqa
    assert got.total_count == 3


@pytest.mark.parametrize("limit", range(1, 4))
async def test_respects_limit_with_segment_filter(limit, factory, api):
    await factory.create_lead_with_events(passport_uid="100500", site_opens=1)
    active_lead_ids = [
        await factory.create_lead_with_events(
            passport_uid=str(idx),
            make_routes=4,
            last_activity_timestamp=now - timedelta(days=idx),
        )
        for idx in range(1, 4)
    ]

    got = await api.post(
        url,
        proto=ListLeadsInput(
            biz_id=123,
            pagination=Pagination(limit=limit, offset=0),
            filter_by_segment=SegmentType.ACTIVE,
        ),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert [lead.lead_id for lead in got.leads] == active_lead_ids[:limit]
    assert got.total_count == 3


@pytest.mark.parametrize("offset", range(4))
async def test_respects_offset_with_segment_filter(offset, factory, api):
    await factory.create_lead_with_events(passport_uid="100500", site_opens=1)
    active_lead_ids = [
        await factory.create_lead_with_events(
            passport_uid=str(idx),
            make_routes=4,
            last_activity_timestamp=now - timedelta(days=idx),
        )
        for idx in range(1, 4)
    ]

    got = await api.post(
        url,
        proto=ListLeadsInput(
            biz_id=123,
            pagination=Pagination(limit=2, offset=offset),
            filter_by_segment=SegmentType.ACTIVE,
        ),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert [lead.lead_id for lead in got.leads] == active_lead_ids[
        offset : offset + 2  # noqa
    ]
    assert got.total_count == 3


@pytest.mark.parametrize(
    "direction, lead_ids",
    [
        (OrderBy.OrderDirection.ASC, [101, 102, 103]),
        (OrderBy.OrderDirection.DESC, [103, 102, 101]),
    ],
)
@pytest.mark.parametrize("limit", range(1, 4))
async def test_respects_limit_when_ordered_by_field(
    direction, lead_ids, limit, factory, api
):
    for idx in range(1, 4):
        await factory.create_lead_with_events(
            passport_uid=str(idx), lead_id=100 + idx, make_routes=idx
        )

    got = await api.post(
        url,
        proto=ListLeadsInput(
            biz_id=123,
            pagination=Pagination(limit=limit, offset=0),
            order_by=OrderBy(field=OrderBy.MAKE_ROUTES, direction=direction),
        ),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert [lead.lead_id for lead in got.leads] == lead_ids[:limit]  # noqa
    assert got.total_count == 3


@pytest.mark.parametrize(
    "direction, lead_ids",
    [
        (OrderBy.OrderDirection.ASC, [101, 102, 103]),
        (OrderBy.OrderDirection.DESC, [103, 102, 101]),
    ],
)
@pytest.mark.parametrize("offset", range(4))
async def test_respects_offset_when_ordered_by_field(
    direction, lead_ids, offset, factory, api
):
    for idx in range(1, 4):
        await factory.create_lead_with_events(
            passport_uid=str(idx), lead_id=100 + idx, make_routes=idx
        )

    got = await api.post(
        url,
        proto=ListLeadsInput(
            biz_id=123,
            pagination=Pagination(limit=2, offset=offset),
            order_by=OrderBy(field=OrderBy.MAKE_ROUTES, direction=direction),
        ),
        decode_as=ListLeadsOutput,
        expected_status=200,
    )

    assert [lead.lead_id for lead in got.leads] == lead_ids[offset : offset + 2]  # noqa
    assert got.total_count == 3
