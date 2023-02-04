import pytest

from maps_adv.geosmb.marksman.server.lib.enums import SegmentType

pytestmark = [pytest.mark.asyncio]


async def test_return_data(factory, dm):
    business = await factory.create_business(biz_id=123)
    await factory.create_biz_segment(
        business_id=business["id"],
        name="ACTIVE",
        cdp_id=77,
        cdp_size=200,
        type_=SegmentType.SEGMENT,
    )
    await factory.create_biz_segment(
        business_id=business["id"],
        name="uploaded",
        cdp_id=78,
        cdp_size=300,
        type_=SegmentType.LABEL,
    )
    await factory.create_biz_segment(
        business_id=business["id"],
        name="LOST",
        cdp_id=79,
        cdp_size=400,
        type_=SegmentType.SEGMENT,
    )

    result = await dm.list_business_segments_data(biz_id=123)

    assert result == {
        "biz_id": 123,
        "permalink": 56789,
        "counter_id": 3333,
        "segments": [
            {"segment_name": "ACTIVE", "cdp_id": 77, "cdp_size": 200},
            {"segment_name": "LOST", "cdp_id": 79, "cdp_size": 400},
        ],
        "labels": [
            {"label_name": "uploaded", "cdp_id": 78, "cdp_size": 300},
        ],
    }


async def test_returns_empty_list_of_segments_if_no_segments_for_business(factory, dm):
    business = await factory.create_business(biz_id=123)
    await factory.create_biz_segment(
        business_id=business["id"],
        name="ACTIVE",
        cdp_id=77,
        cdp_size=200,
        type_=SegmentType.LABEL,
    )

    result = await dm.list_business_segments_data(biz_id=123)

    assert result == {
        "biz_id": 123,
        "permalink": 56789,
        "counter_id": 3333,
        "segments": [],
        "labels": [{"label_name": "ACTIVE", "cdp_id": 77, "cdp_size": 200}],
    }


async def test_returns_empty_list_of_labels_if_no_labels_for_business(factory, dm):
    business = await factory.create_business(biz_id=123)
    await factory.create_biz_segment(
        business_id=business["id"],
        name="ACTIVE",
        cdp_id=77,
        cdp_size=200,
        type_=SegmentType.SEGMENT,
    )

    result = await dm.list_business_segments_data(biz_id=123)

    assert result == {
        "biz_id": 123,
        "permalink": 56789,
        "counter_id": 3333,
        "segments": [{"segment_name": "ACTIVE", "cdp_id": 77, "cdp_size": 200}],
        "labels": [],
    }


async def test_does_not_return_segments_for_other_business(factory, dm):
    business = await factory.create_business(biz_id=123)
    await factory.create_biz_segment(
        business_id=business["id"],
        name="ACTIVE",
        cdp_id=77,
        cdp_size=200,
        type_=SegmentType.SEGMENT,
    )
    await factory.create_biz_segment(
        business_id=business["id"],
        name="uploaded",
        cdp_id=78,
        cdp_size=300,
        type_=SegmentType.LABEL,
    )
    another_business = await factory.create_business(biz_id=567)
    await factory.create_biz_segment(
        business_id=another_business["id"],
        name="LOST",
        cdp_id=88,
        cdp_size=300,
        type_=SegmentType.SEGMENT,
    )
    await factory.create_biz_segment(
        business_id=another_business["id"],
        name="downloaded",
        cdp_id=89,
        cdp_size=300,
        type_=SegmentType.LABEL,
    )

    result = await dm.list_business_segments_data(biz_id=123)

    assert result == {
        "biz_id": 123,
        "permalink": 56789,
        "counter_id": 3333,
        "segments": [{"segment_name": "ACTIVE", "cdp_id": 77, "cdp_size": 200}],
        "labels": [{"label_name": "uploaded", "cdp_id": 78, "cdp_size": 300}],
    }


async def test_return_none_if_no_business_found_for_biz_id(factory, dm):
    await factory.create_business(biz_id=123)

    result = await dm.list_business_segments_data(biz_id=345)

    assert result is None
