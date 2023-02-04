from datetime import datetime

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.marksman.server.lib.enums import SegmentType

pytestmark = [pytest.mark.asyncio, pytest.mark.real_db]


async def test_updates_segments_sizes(factory, dm):
    business1 = await factory.create_business(biz_id=123)
    segment1 = await factory.create_biz_segment(
        business1["id"], cdp_id=15, cdp_size=100, type_=SegmentType.SEGMENT
    )
    segment2 = await factory.create_biz_segment(
        business1["id"], cdp_id=25, cdp_size=200, type_=SegmentType.LABEL
    )
    business2 = await factory.create_business(biz_id=345)
    segment3 = await factory.create_biz_segment(
        business2["id"], cdp_id=35, cdp_size=300, type_=SegmentType.SEGMENT
    )

    await dm.update_segments_sizes(
        {
            15: 400,
            25: 500,
            35: 600,
        }
    )

    assert await factory.list_business_segments(business1["id"]) == [
        {
            "id": segment1["id"],
            "business_id": business1["id"],
            "name": Any(str),
            "cdp_id": 15,
            "cdp_size": 400,
            "type": "segment",
            "created_at": Any(datetime),
        },
        {
            "id": segment2["id"],
            "business_id": business1["id"],
            "name": Any(str),
            "cdp_id": 25,
            "cdp_size": 500,
            "type": "label",
            "created_at": Any(datetime),
        },
    ]
    assert await factory.list_business_segments(business2["id"]) == [
        {
            "id": segment3["id"],
            "business_id": business2["id"],
            "name": Any(str),
            "cdp_id": 35,
            "cdp_size": 600,
            "type": "segment",
            "created_at": Any(datetime),
        },
    ]


async def test_does_not_update_other_segments_sizes(factory, dm):
    business1 = await factory.create_business(biz_id=123)
    segment1 = await factory.create_biz_segment(
        business1["id"], cdp_id=15, cdp_size=100, type_=SegmentType.SEGMENT
    )
    segment2 = await factory.create_biz_segment(
        business1["id"], cdp_id=25, cdp_size=200, type_=SegmentType.LABEL
    )
    business2 = await factory.create_business(biz_id=345)
    segment3 = await factory.create_biz_segment(
        business2["id"], cdp_id=35, cdp_size=300, type_=SegmentType.SEGMENT
    )

    await dm.update_segments_sizes({15: 400})

    assert await factory.list_business_segments(business1["id"]) == [
        {
            "id": segment1["id"],
            "business_id": business1["id"],
            "name": Any(str),
            "cdp_id": 15,
            "cdp_size": 400,
            "type": "segment",
            "created_at": Any(datetime),
        },
        {
            "id": segment2["id"],
            "business_id": business1["id"],
            "name": Any(str),
            "cdp_id": 25,
            "cdp_size": 200,
            "type": "label",
            "created_at": Any(datetime),
        },
    ]
    assert await factory.list_business_segments(business2["id"]) == [
        {
            "id": segment3["id"],
            "business_id": business2["id"],
            "name": Any(str),
            "cdp_id": 35,
            "cdp_size": 300,
            "type": "segment",
            "created_at": Any(datetime),
        },
    ]


async def test_ignores_unknown_segments(factory, dm):
    business = await factory.create_business(biz_id=123)
    segment = await factory.create_biz_segment(business["id"], cdp_id=15, cdp_size=100)

    await dm.update_segments_sizes({15: 400, 225: 1000})

    assert await factory.list_business_segments(business["id"]) == [
        {
            "id": segment["id"],
            "business_id": business["id"],
            "name": Any(str),
            "cdp_id": 15,
            "cdp_size": 400,
            "type": "segment",
            "created_at": Any(datetime),
        },
    ]


async def test_returns_none(factory, dm):
    business = await factory.create_business(biz_id=123)
    await factory.create_biz_segment(business["id"], cdp_id=15, cdp_size=100)

    result = await dm.update_segments_sizes({15: 400})

    assert result is None
