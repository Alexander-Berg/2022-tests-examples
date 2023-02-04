import pytest
from aiohttp.web import Response

from maps_adv.geosmb.clients.facade import (
    BadFacadeResponse,
    CouponType as CouponTypeEnum,
    Currency as CurrencyEnum,
    SegmentType as SegmentTypeEnum,
)
from maps_adv.geosmb.clients.facade.proto.facade_pb2 import (
    CouponsWithSegmentsInput,
    CouponsWithSegmentsOutput,
    Currency,
    Paging,
    SegmentType,
)

pytestmark = [pytest.mark.asyncio]


def make_coupon(**overrides):
    coupon = dict(
        id=11,
        type=CouponsWithSegmentsOutput.CouponType.FREE,
        segments=[SegmentType.REGULAR, SegmentType.ACTIVE],
        percent_discount=10,
    )
    coupon.update(**overrides)

    return CouponsWithSegmentsOutput.Coupon(**coupon)


async def test_sends_correct_request(facade_client, mock_list_coupons_with_segments):
    request_url = None
    request_headers = None
    request_body = None

    async def facade_handler(request):
        nonlocal request_url, request_headers, request_body
        request_url = str(request.url)
        request_headers = request.headers
        request_body = await request.read()
        return Response(
            status=200,
            body=CouponsWithSegmentsOutput(businesses=[]).SerializeToString(),
        )

    mock_list_coupons_with_segments(facade_handler)

    async for _ in facade_client.list_coupons_with_segments():
        pass

    assert request_url == "http://facade.server/v1/get_coupons_with_segments"
    assert request_headers["Content-Type"] == "application/x-protobuf"
    proto_body = CouponsWithSegmentsInput.FromString(request_body)
    assert proto_body == CouponsWithSegmentsInput(paging=Paging(limit=200, offset=0))


async def test_paginates_in_request_correctly(
    facade_client, mock_list_coupons_with_segments
):
    request_bodies = []

    async def facade_handler(request):
        nonlocal request_bodies
        request_body = await request.read()
        request_bodies.append(CouponsWithSegmentsInput.FromString(request_body))
        return Response(
            status=200,
            body=CouponsWithSegmentsOutput(
                businesses=[
                    CouponsWithSegmentsOutput.Business(
                        biz_id=123,
                        coupons=[
                            CouponsWithSegmentsOutput.Coupon(
                                id=11,
                                type=CouponsWithSegmentsOutput.CouponType.FREE,
                                segments=["REGULAR", "ACTIVE"],
                                percent_discount=10,
                            ),
                        ],
                    )
                ],
            ).SerializeToString(),
        )

    mock_list_coupons_with_segments(facade_handler)
    mock_list_coupons_with_segments(facade_handler)
    mock_list_coupons_with_segments(facade_handler)
    mock_list_coupons_with_segments(
        Response(
            status=200,
            body=CouponsWithSegmentsOutput(businesses=[]).SerializeToString(),
        )
    )

    async for _ in facade_client.list_coupons_with_segments():
        pass

    assert request_bodies == [
        CouponsWithSegmentsInput(paging=Paging(limit=200, offset=0)),
        CouponsWithSegmentsInput(paging=Paging(limit=200, offset=200)),
        CouponsWithSegmentsInput(paging=Paging(limit=200, offset=400)),
    ]


async def test_returns_coupons_of_businesses(
    facade_client, mock_list_coupons_with_segments
):
    mock_list_coupons_with_segments(
        Response(
            status=200,
            body=CouponsWithSegmentsOutput(
                businesses=[
                    CouponsWithSegmentsOutput.Business(
                        biz_id=123,
                        coupons=[
                            CouponsWithSegmentsOutput.Coupon(
                                id=11,
                                type=CouponsWithSegmentsOutput.CouponType.FREE,
                                segments=[SegmentType.REGULAR, SegmentType.ACTIVE],
                                percent_discount=10,
                            ),
                            CouponsWithSegmentsOutput.Coupon(
                                id=22,
                                type=CouponsWithSegmentsOutput.CouponType.SERVICE,
                                segments=[SegmentType.LOST],
                                percent_discount=20,
                            ),
                        ],
                    )
                ],
            ).SerializeToString(),
        )
    )
    mock_list_coupons_with_segments(
        Response(
            status=200,
            body=CouponsWithSegmentsOutput(
                businesses=[
                    CouponsWithSegmentsOutput.Business(
                        biz_id=456,
                        coupons=[
                            CouponsWithSegmentsOutput.Coupon(
                                id=33,
                                type=CouponsWithSegmentsOutput.CouponType.FREE,
                                segments=[SegmentType.UNPROCESSED_ORDERS],
                                cost_discount=dict(currency=Currency.RUB, cost="91.99"),
                            ),
                        ],
                    ),
                ],
            ).SerializeToString(),
        )
    )
    mock_list_coupons_with_segments(
        Response(
            status=200,
            body=CouponsWithSegmentsOutput(
                businesses=[],
            ).SerializeToString(),
        )
    )

    result = []
    async for chunk in facade_client.list_coupons_with_segments():
        result.append(chunk)

    assert result == [
        {
            123: [
                dict(
                    coupon_id=11,
                    type=CouponTypeEnum.FREE,
                    segments=[SegmentTypeEnum.REGULAR, SegmentTypeEnum.ACTIVE],
                    percent_discount=10,
                ),
                dict(
                    coupon_id=22,
                    type=CouponTypeEnum.SERVICE,
                    segments=[SegmentTypeEnum.LOST],
                    percent_discount=20,
                ),
            ]
        },
        {
            456: [
                dict(
                    coupon_id=33,
                    type=CouponTypeEnum.FREE,
                    segments=[SegmentTypeEnum.UNPROCESSED_ORDERS],
                    cost_discount="91.99",
                    currency=CurrencyEnum.RUB,
                )
            ]
        },
    ]


async def test_returns_empty_list_if_got_nothing(
    facade_client, mock_list_coupons_with_segments
):
    mock_list_coupons_with_segments(
        Response(
            status=200,
            body=CouponsWithSegmentsOutput(businesses=[]).SerializeToString(),
        )
    )

    async for chunk in facade_client.list_coupons_with_segments():
        assert chunk == []


async def test_raises_for_business_without_coupons(
    facade_client, mock_list_coupons_with_segments
):
    mock_list_coupons_with_segments(
        Response(
            status=200,
            body=CouponsWithSegmentsOutput(
                businesses=[
                    CouponsWithSegmentsOutput.Business(biz_id=123, coupons=[]),
                ],
            ).SerializeToString(),
        )
    )

    with pytest.raises(BadFacadeResponse, match="Business biz_id=123 has no coupons"):
        async for _ in facade_client.list_coupons_with_segments():
            pass


async def test_raises_for_coupon_with_bad_data(
    facade_client, mock_list_coupons_with_segments
):
    mock_list_coupons_with_segments(
        Response(
            status=200,
            body=CouponsWithSegmentsOutput(
                businesses=[
                    CouponsWithSegmentsOutput.Business(
                        biz_id=123, coupons=[make_coupon(segments=[])]
                    )
                ],
            ).SerializeToString(),
        )
    )

    with pytest.raises(BadFacadeResponse, match="Coupon coupon_id=11 has no segments"):
        async for _ in facade_client.list_coupons_with_segments():
            pass
