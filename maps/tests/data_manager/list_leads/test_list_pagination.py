from datetime import datetime, timedelta, timezone

import pytest

from maps_adv.geosmb.promoter.server.lib.enums import (
    OrderByField,
    OrderDirection,
    SegmentType,
)

pytestmark = [pytest.mark.asyncio]

now = datetime.now(tz=timezone.utc)


async def test_returns_empty_leads_list_if_offset_grater_than_total(factory, dm):
    await factory.create_lead_with_events(passport_uid="1111", make_routes=4)
    await factory.create_lead_with_events(passport_uid="2222", site_opens=4)

    got = await dm.list_leads(biz_id=123, limit=2, offset=100500)

    assert got == dict(total_count=2, leads=[])


async def test_returns_empty_leads_list_if_limit_0(factory, dm):
    await factory.create_lead_with_events(passport_uid="1111", make_routes=4)

    got = await dm.list_leads(biz_id=123, limit=0, offset=0)

    assert got == dict(total_count=1, leads=[])


@pytest.mark.parametrize("limit", range(1, 4))
async def test_respects_limit(limit, factory, dm):
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

    got = await dm.list_leads(biz_id=123, limit=limit, offset=0)

    assert [lead["lead_id"] for lead in got["leads"]] == lead_ids[:limit]
    assert got["total_count"] == 3


@pytest.mark.parametrize("offset", range(4))
async def test_respects_offset(offset, factory, dm):
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

    got = await dm.list_leads(biz_id=123, limit=2, offset=offset)

    assert [lead["lead_id"] for lead in got["leads"]] == lead_ids[
        offset : offset + 2  # noqa
    ]
    assert got["total_count"] == 3


@pytest.mark.parametrize("limit", range(1, 4))
async def test_respects_limit_with_segment_filter(limit, factory, dm):
    await factory.create_lead_with_events(passport_uid="100500", site_opens=1)
    active_lead_ids = [
        await factory.create_lead_with_events(
            passport_uid=str(idx),
            make_routes=4,
            last_activity_timestamp=now - timedelta(days=idx),
        )
        for idx in range(1, 4)
    ]

    got = await dm.list_leads(
        biz_id=123, limit=limit, offset=0, filter_by_segment=SegmentType.ACTIVE
    )

    assert [lead["lead_id"] for lead in got["leads"]] == active_lead_ids[:limit]
    assert got["total_count"] == 3


@pytest.mark.parametrize("offset", range(4))
async def test_respects_offset_with_segment_filter(offset, factory, dm):
    await factory.create_lead_with_events(passport_uid="100500", site_opens=1)
    active_lead_ids = [
        await factory.create_lead_with_events(
            passport_uid=str(idx),
            make_routes=4,
            last_activity_timestamp=now - timedelta(days=idx),
        )
        for idx in range(1, 4)
    ]

    got = await dm.list_leads(
        biz_id=123, limit=2, offset=offset, filter_by_segment=SegmentType.ACTIVE
    )

    assert [lead["lead_id"] for lead in got["leads"]] == active_lead_ids[
        offset : offset + 2  # noqa
    ]
    assert got["total_count"] == 3


@pytest.mark.parametrize(
    "direction, lead_ids",
    [(OrderDirection.ASC, [101, 102, 103]), (OrderDirection.DESC, [103, 102, 101])],
)
@pytest.mark.parametrize("limit", range(1, 4))
async def test_respects_limit_when_ordered_by_field(
    direction, lead_ids, limit, factory, dm
):
    for idx in range(1, 4):
        await factory.create_lead_with_events(
            passport_uid=str(idx), lead_id=100 + idx, make_routes=idx
        )

    got = await dm.list_leads(
        biz_id=123,
        limit=limit,
        offset=0,
        order_by_field=OrderByField.MAKE_ROUTES,
        order_direction=direction,
    )

    assert [lead["lead_id"] for lead in got["leads"]] == lead_ids[:limit]
    assert got["total_count"] == 3


@pytest.mark.parametrize(
    "direction, lead_ids",
    [(OrderDirection.ASC, [101, 102, 103]), (OrderDirection.DESC, [103, 102, 101])],
)
@pytest.mark.parametrize("offset", range(4))
async def test_respects_offset_when_ordered_by_field(
    direction, lead_ids, offset, factory, dm
):
    for idx in range(1, 4):
        await factory.create_lead_with_events(
            passport_uid=str(idx), lead_id=100 + idx, make_routes=idx
        )

    got = await dm.list_leads(
        biz_id=123,
        limit=2,
        offset=offset,
        order_by_field=OrderByField.MAKE_ROUTES,
        order_direction=direction,
    )

    assert [lead["lead_id"] for lead in got["leads"]] == lead_ids[
        offset : offset + 2  # noqa
    ]
    assert got["total_count"] == 3
