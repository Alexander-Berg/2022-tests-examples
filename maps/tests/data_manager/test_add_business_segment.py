from datetime import datetime

import pytest
from smb.common.testing_utils import Any

from maps_adv.geosmb.marksman.server.lib.enums import SegmentType

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("type_", SegmentType)
async def test_adds_segment(factory, dm, type_):
    business = await factory.create_business(biz_id=123, permalink=111, counter_id=1000)

    await dm.add_business_segment(
        biz_id=123, name="some_name", cdp_id=15, cdp_size=200, type_=type_
    )

    assert await factory.list_business_segments(business["id"]) == [
        {
            "id": Any(int),
            "business_id": business["id"],
            "name": "some_name",
            "cdp_id": 15,
            "cdp_size": 200,
            "type": type_.value,
            "created_at": Any(datetime),
        }
    ]


@pytest.mark.parametrize("type_", SegmentType)
async def test_does_not_remove_other_segments(factory, dm, type_):
    business1 = await factory.create_business(
        biz_id=123, permalink=111, counter_id=1000
    )
    await factory.create_biz_segment(
        business_id=business1["id"],
        name="name1",
        cdp_id=33,
        cdp_size=300,
        type_=SegmentType.SEGMENT,
    )
    business2 = await factory.create_business(
        biz_id=345, permalink=222, counter_id=2000
    )
    await factory.create_biz_segment(
        business_id=business2["id"],
        name="name2",
        cdp_id=44,
        cdp_size=500,
        type_=SegmentType.LABEL,
    )

    await dm.add_business_segment(
        biz_id=123, name="some_name", cdp_id=15, cdp_size=200, type_=type_
    )

    assert await factory.list_business_segments(business1["id"]) == [
        {
            "id": Any(int),
            "business_id": business1["id"],
            "name": "name1",
            "cdp_id": 33,
            "cdp_size": 300,
            "type": "segment",
            "created_at": Any(datetime),
        },
        {
            "id": Any(int),
            "business_id": business1["id"],
            "name": "some_name",
            "cdp_id": 15,
            "cdp_size": 200,
            "type": type_.value,
            "created_at": Any(datetime),
        },
    ]
    assert await factory.list_business_segments(business2["id"]) == [
        {
            "id": Any(int),
            "business_id": business2["id"],
            "name": "name2",
            "cdp_id": 44,
            "cdp_size": 500,
            "type": "label",
            "created_at": Any(datetime),
        }
    ]


@pytest.mark.parametrize("type_", SegmentType)
async def test_does_nothing_if_biz_id_not_found(factory, dm, type_):
    await factory.create_business(biz_id=345, permalink=222, counter_id=2000)

    try:
        await dm.add_business_segment(
            biz_id=123, name="some_name", cdp_id=15, cdp_size=200, type_=type_
        )
    except:
        pytest.fail("Should not raise")


@pytest.mark.parametrize("type_", SegmentType)
async def test_returns_none(factory, dm, type_):
    await factory.create_business(biz_id=123, permalink=111, counter_id=1000)

    result = await dm.add_business_segment(
        biz_id=123, name="some_name", cdp_id=15, cdp_size=200, type_=type_
    )

    assert result is None
