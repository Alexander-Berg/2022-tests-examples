import pytest
from aiohttp.web_response import Response

from maps_adv.geosmb.clients.loyalty import (
    CouponReviewResolution as CouponReviewResolutionEnum,
    EmptyCouponsList,
    IncorrectCouponData,
)
from maps_adv.geosmb.clients.loyalty.proto.loyalty_pb2 import (
    CouponReviewResolution as CouponReviewResolutionPb,
    SubmitCouponsReviewsListRequest,
)

pytestmark = [pytest.mark.asyncio]


coupons_review_results_input = [
    dict(
        biz_id="biz_id_1",
        item_id="item_id_1",
        revision_id=111,
        resolution=CouponReviewResolutionEnum.APPROVED,
        reason_codes=[],
    ),
    dict(
        biz_id="biz_id_2",
        item_id="item_id_2",
        revision_id=222,
        resolution=CouponReviewResolutionEnum.REJECTED,
        corrected=dict(
            title="new_title",
            products_description="new_products_description",
            conditions="new_conditions",
        ),
        reason_codes=[555, 333],
    ),
]


async def test_sends_correct_request(loyalty_client, mock_submit_coupons_reviews_list):
    request_url = None
    request_headers = None
    request_body = None

    async def loyalty_handler(request):
        nonlocal request_url, request_headers, request_body
        request_url = str(request.url)
        request_headers = request.headers
        request_body = await request.read()
        return Response(status=204)

    mock_submit_coupons_reviews_list(loyalty_handler)

    await loyalty_client.submit_coupons_reviews_list(
        coupons_review_results=coupons_review_results_input
    )

    assert request_url == "http://loyalty.server/v0/submit_coupons_reviews_list"
    assert request_headers["X-Ya-Service-Ticket"] == "KEK_FROM_AIOTVM_PYTEST_PLUGIN"
    assert request_headers["Content-Type"] == "application/x-protobuf"
    proto_body = SubmitCouponsReviewsListRequest.FromString(request_body)
    assert proto_body == SubmitCouponsReviewsListRequest(
        results_list=[
            SubmitCouponsReviewsListRequest.CouponReviewResult(
                biz_id="biz_id_1",
                item_id="item_id_1",
                revision_id=111,
                resolution=CouponReviewResolutionPb.APPROVED,
                reason_codes=[],
            ),
            SubmitCouponsReviewsListRequest.CouponReviewResult(
                biz_id="biz_id_2",
                item_id="item_id_2",
                revision_id=222,
                resolution=CouponReviewResolutionPb.REJECTED,
                corrected=SubmitCouponsReviewsListRequest.CouponReviewCorrected(
                    title="new_title",
                    products_description="new_products_description",
                    conditions="new_conditions",
                ),
                reason_codes=[555, 333],
            ),
        ]
    )


async def test_returns_nothing(loyalty_client, mock_submit_coupons_reviews_list):
    mock_submit_coupons_reviews_list(Response(status=204))

    got = await loyalty_client.submit_coupons_reviews_list(
        coupons_review_results=coupons_review_results_input
    )

    assert got is None


async def test_raises_if_empty_coupons_list(loyalty_client):
    with pytest.raises(EmptyCouponsList):
        await loyalty_client.submit_coupons_reviews_list(coupons_review_results=[])


async def test_raises_if_incorrect_coupons_list(loyalty_client):
    with pytest.raises(IncorrectCouponData):
        await loyalty_client.submit_coupons_reviews_list(
            coupons_review_results=[
                dict(
                    biz_id="biz_id_1",
                    revision_id=111,
                    resolution=CouponReviewResolutionEnum.APPROVED,
                    reason_codes=[],
                )
            ]
        )
