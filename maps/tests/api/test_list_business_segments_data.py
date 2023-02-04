import pytest

from maps_adv.geosmb.marksman.server.lib.enums import SegmentType

pytestmark = [pytest.mark.asyncio]

URL = "/v1/business/segments_data/"


async def test_return_data(factory, api):
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

    result = await api.get(
        URL + "?biz_id=123",
        expected_status=200,
    )

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


async def test_returns_empty_list_of_segments_if_no_segments_for_business(factory, api):
    business = await factory.create_business(biz_id=123)
    await factory.create_biz_segment(
        business_id=business["id"],
        name="ACTIVE",
        cdp_id=77,
        cdp_size=200,
        type_=SegmentType.LABEL,
    )

    result = await api.get(
        URL + "?biz_id=123",
        expected_status=200,
    )

    assert result == {
        "biz_id": 123,
        "permalink": 56789,
        "counter_id": 3333,
        "segments": [],
        "labels": [{"label_name": "ACTIVE", "cdp_id": 77, "cdp_size": 200}],
    }


async def test_returns_empty_list_of_labels_if_no_labels_for_business(factory, api):
    business = await factory.create_business(biz_id=123)
    await factory.create_biz_segment(
        business_id=business["id"],
        name="ACTIVE",
        cdp_id=77,
        cdp_size=200,
        type_=SegmentType.SEGMENT,
    )

    result = await api.get(
        URL + "?biz_id=123",
        expected_status=200,
    )

    assert result == {
        "biz_id": 123,
        "permalink": 56789,
        "counter_id": 3333,
        "segments": [{"segment_name": "ACTIVE", "cdp_id": 77, "cdp_size": 200}],
        "labels": [],
    }


async def test_does_not_return_segments_for_other_business(factory, api):
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

    result = await api.get(
        URL + "?biz_id=123",
        expected_status=200,
    )

    assert result == {
        "biz_id": 123,
        "permalink": 56789,
        "counter_id": 3333,
        "segments": [{"segment_name": "ACTIVE", "cdp_id": 77, "cdp_size": 200}],
        "labels": [{"label_name": "uploaded", "cdp_id": 78, "cdp_size": 300}],
    }


async def test_returns_404_if_no_business_found_for_biz_id(factory, api):
    await factory.create_business(biz_id=123)

    await api.get(
        URL + "?biz_id=345",
        expected_status=404,
    )


async def test_returns_400_if_no_biz_id_passed(factory, api):
    await factory.create_business(biz_id=123)

    await api.get(
        URL,
        expected_status=400,
    )


async def test_returns_400_if_biz_id_is_not_an_integer(factory, api):
    await factory.create_business(biz_id=123)

    await api.get(
        URL,
        expected_status=400,
    )
