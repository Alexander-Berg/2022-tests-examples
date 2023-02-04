import pytest
from aiohttp.web_response import Response

from maps_adv.geosmb.clients.facade import (
    CouponStatus as CouponStatusEnum,
    InvalidParams,
)
from maps_adv.geosmb.clients.facade.proto.facade_pb2 import (
    CouponsStatusesListRequest,
    CouponsStatusesListResponse,
    CouponStatus as CouponStatusPb,
)

pytestmark = [pytest.mark.asyncio]

COUPON_STATUS_PB_TO_ENUM = {
    CouponStatusPb.NOT_RUNNING_YET: CouponStatusEnum.NOT_RUNNING_YET,
    CouponStatusPb.RUNNING: CouponStatusEnum.RUNNING,
    CouponStatusPb.FINISHED: CouponStatusEnum.FINISHED,
}


async def test_sends_correct_request(facade_client, mock_list_coupons):
    request_url = None
    request_headers = None
    request_body = None

    async def facade_handler(request):
        nonlocal request_url, request_headers, request_body
        request_url = str(request.url)
        request_headers = request.headers
        request_body = await request.read()
        return Response(
            status=200, body=CouponsStatusesListResponse().SerializeToString()
        )

    mock_list_coupons(facade_handler)

    await facade_client.list_coupons_statuses(coupon_ids=[111, 222])

    assert request_url == "http://facade.server/v1/get_coupons_statuses_list"
    assert request_headers["X-Ya-Service-Ticket"] == "KEK_FROM_AIOTVM_PYTEST_PLUGIN"
    assert request_headers["Content-Type"] == "application/x-protobuf"
    assert (
        request_body
        == CouponsStatusesListRequest(items_ids_list=["111", "222"]).SerializeToString()
    )


@pytest.mark.parametrize("status", [s for s in CouponStatusPb.values()])
async def test_returns_coupon_details(facade_client, mock_list_coupons, status):
    mock_list_coupons(
        Response(
            status=200,
            body=CouponsStatusesListResponse(
                coupons_statuses_list=[
                    CouponsStatusesListResponse.CouponStatusInfo(
                        biz_id="123", item_id="111", status=status
                    )
                ]
            ).SerializeToString(),
        )
    )

    got = await facade_client.list_coupons_statuses(coupon_ids=[111, 222])

    assert got == [
        dict(biz_id=123, coupon_id=111, status=COUPON_STATUS_PB_TO_ENUM[status])
    ]


async def test_returns_list_of_coupons_statuses(facade_client, mock_list_coupons):
    mock_list_coupons(
        Response(
            status=200,
            body=CouponsStatusesListResponse(
                coupons_statuses_list=[
                    CouponsStatusesListResponse.CouponStatusInfo(
                        biz_id="123", item_id="111", status=CouponStatusPb.RUNNING
                    ),
                    CouponsStatusesListResponse.CouponStatusInfo(
                        biz_id="456", item_id="222", status=CouponStatusPb.FINISHED
                    ),
                ]
            ).SerializeToString(),
        )
    )

    got = await facade_client.list_coupons_statuses(coupon_ids=[111, 222])

    assert got == [
        dict(biz_id=123, coupon_id=111, status=CouponStatusEnum.RUNNING),
        dict(biz_id=456, coupon_id=222, status=CouponStatusEnum.FINISHED),
    ]


@pytest.mark.parametrize("coupon_ids", [None, []])
async def test_raises_for_invalid_params(facade_client, coupon_ids):
    with pytest.raises(InvalidParams) as exc:
        await facade_client.list_coupons_statuses(coupon_ids=coupon_ids)

    assert exc.value.args == ("coupon_ids param can't be empty",)
