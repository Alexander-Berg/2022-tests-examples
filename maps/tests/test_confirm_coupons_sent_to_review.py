import pytest
from aiohttp.web_response import Response

from maps_adv.geosmb.clients.loyalty import EmptyCouponsList, IncorrectCouponData
from maps_adv.geosmb.clients.loyalty.proto.loyalty_pb2 import (
    ConfirmCouponsSentToModerationRequest,
)

pytestmark = [pytest.mark.asyncio]


coupons_review_results_input = [
    dict(biz_id=111, item_id=1111, revision_id=1),
    dict(biz_id=222, item_id=2222, revision_id=2),
]


async def test_sends_correct_request(
    loyalty_client, mock_confirm_coupons_sent_to_review
):
    request_url = None
    request_headers = None
    request_body = None

    async def loyalty_handler(request):
        nonlocal request_url, request_headers, request_body
        request_url = str(request.url)
        request_headers = request.headers
        request_body = await request.read()
        return Response(status=204)

    mock_confirm_coupons_sent_to_review(loyalty_handler)

    await loyalty_client.confirm_coupons_sent_to_review(
        sent_coupons=coupons_review_results_input
    )

    assert request_url == "http://loyalty.server/v0/confirm_coupons_sent_to_moderation"
    assert request_headers["X-Ya-Service-Ticket"] == "KEK_FROM_AIOTVM_PYTEST_PLUGIN"
    assert request_headers["Content-Type"] == "application/x-protobuf"
    proto_body = ConfirmCouponsSentToModerationRequest.FromString(request_body)
    assert proto_body == ConfirmCouponsSentToModerationRequest(
        coupons_list=[
            ConfirmCouponsSentToModerationRequest.CouponSentToModeration(
                biz_id="111", item_id="1111", revision_id=1
            ),
            ConfirmCouponsSentToModerationRequest.CouponSentToModeration(
                biz_id="222", item_id="2222", revision_id=2
            ),
        ]
    )


async def test_returns_nothing(loyalty_client, mock_confirm_coupons_sent_to_review):
    mock_confirm_coupons_sent_to_review(Response(status=204))

    got = await loyalty_client.confirm_coupons_sent_to_review(
        sent_coupons=coupons_review_results_input
    )

    assert got is None


async def test_raises_if_empty_coupons_list(loyalty_client):
    with pytest.raises(EmptyCouponsList):
        await loyalty_client.confirm_coupons_sent_to_review(sent_coupons=[])


@pytest.mark.parametrize("bad_field", ("biz_id", "item_id", "revision_id"))
async def test_raises_if_incorrect_coupons_list(loyalty_client, bad_field):
    coupon_data = dict(biz_id=11, item_id=1111, revision_id=1)
    coupon_data[bad_field] = None

    with pytest.raises(IncorrectCouponData):
        await loyalty_client.confirm_coupons_sent_to_review(sent_coupons=[coupon_data])
