import pytest
from aiohttp.web import Response

from maps_adv.geosmb.clients.loyalty import BadLoyaltyResponse
from maps_adv.geosmb.clients.loyalty.proto.loyalty_pb2 import (
    CouponForReview,
    CouponsListForReviewRequest,
    CouponsListForReviewResponse,
    Paging,
    PagingOutput,
)

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(loyalty_client, mock_get_coupons_list_for_review):
    request_url = None
    request_headers = None
    request_body = None

    async def loyalty_handler(request):
        nonlocal request_url, request_headers, request_body
        request_url = str(request.url)
        request_headers = request.headers
        request_body = await request.read()
        return Response(
            status=200,
            body=CouponsListForReviewResponse(
                paging=PagingOutput(total=0, offset=0, limit=0), coupons_list=[]
            ).SerializeToString(),
        )

    mock_get_coupons_list_for_review(loyalty_handler)

    async for _ in loyalty_client.get_coupons_list_for_review():
        pass

    assert request_url == "http://loyalty.server/v0/get_coupons_list_for_review"
    assert request_headers["X-Ya-Service-Ticket"] == "KEK_FROM_AIOTVM_PYTEST_PLUGIN"
    assert request_headers["Content-Type"] == "application/x-protobuf"
    proto_body = CouponsListForReviewRequest.FromString(request_body)
    assert proto_body == CouponsListForReviewRequest(paging=Paging(limit=500, offset=0))


async def test_paginates_in_request_correctly(
    loyalty_client, mock_get_coupons_list_for_review
):
    request_bodies = []

    async def loyalty_handler(request):
        nonlocal request_bodies
        request_body = await request.read()
        request_bodies.append(CouponsListForReviewRequest.FromString(request_body))
        return Response(
            status=200,
            body=CouponsListForReviewResponse(
                paging=PagingOutput(total=2, offset=1, limit=1),
                coupons_list=[
                    CouponForReview(
                        biz_id="111",
                        item_id="1111",
                        revision_id=2,
                        title="title",
                        cover_url="cover_url",
                        products_description="description",
                        conditions="conditions",
                    )
                ],
            ).SerializeToString(),
        )

    mock_get_coupons_list_for_review(loyalty_handler)
    mock_get_coupons_list_for_review(loyalty_handler)
    mock_get_coupons_list_for_review(loyalty_handler)
    mock_get_coupons_list_for_review(
        Response(
            status=200,
            body=CouponsListForReviewResponse(
                paging=PagingOutput(total=3, offset=0, limit=1000), coupons_list=[]
            ).SerializeToString(),
        )
    )

    async for _ in loyalty_client.get_coupons_list_for_review():
        pass

    assert request_bodies == [
        CouponsListForReviewRequest(paging=Paging(limit=500, offset=0)),
        CouponsListForReviewRequest(paging=Paging(limit=500, offset=500)),
        CouponsListForReviewRequest(paging=Paging(limit=500, offset=1000)),
    ]


async def test_returns_list_of_coupons_for_review(
    loyalty_client, mock_get_coupons_list_for_review
):
    mock_get_coupons_list_for_review(
        Response(
            status=200,
            body=CouponsListForReviewResponse(
                paging=PagingOutput(total=2, offset=0, limit=1),
                coupons_list=[
                    CouponForReview(
                        biz_id="111",
                        item_id="1111",
                        revision_id=1,
                        title="title_1",
                        cover_url="cover_url_1",
                        products_description="description_1",
                        conditions="conditions_1",
                    ),
                    CouponForReview(
                        biz_id="222",
                        item_id="2222",
                        revision_id=2,
                        title="title_2",
                        cover_url="cover_url_2",
                        products_description="description_2",
                        conditions="conditions_2",
                    ),
                ],
            ).SerializeToString(),
        )
    )
    mock_get_coupons_list_for_review(
        Response(
            status=200,
            body=CouponsListForReviewResponse(
                paging=PagingOutput(total=2, offset=1, limit=1),
                coupons_list=[
                    CouponForReview(
                        biz_id="333",
                        item_id="3333",
                        revision_id=3,
                        title="title_3",
                        cover_url="cover_url_3",
                        products_description="description_3",
                        conditions="conditions_3",
                    )
                ],
            ).SerializeToString(),
        )
    )
    mock_get_coupons_list_for_review(
        Response(
            status=200,
            body=CouponsListForReviewResponse(
                paging=PagingOutput(total=2, offset=2, limit=0), coupons_list=[]
            ).SerializeToString(),
        )
    )

    result = []
    async for coupons_for_review in loyalty_client.get_coupons_list_for_review():
        result.append(coupons_for_review)

    assert result[0] == [
        dict(
            biz_id=111,
            item_id=1111,
            revision_id=1,
            title="title_1",
            cover_url="cover_url_1",
            products_description="description_1",
            conditions="conditions_1",
        ),
        dict(
            biz_id=222,
            item_id=2222,
            revision_id=2,
            title="title_2",
            cover_url="cover_url_2",
            products_description="description_2",
            conditions="conditions_2",
        ),
    ]
    assert result[1] == [
        dict(
            biz_id=333,
            item_id=3333,
            revision_id=3,
            title="title_3",
            cover_url="cover_url_3",
            products_description="description_3",
            conditions="conditions_3",
        )
    ]


async def test_returns_coupon_with_missed_optional_fields(
    loyalty_client, mock_get_coupons_list_for_review
):
    mock_get_coupons_list_for_review(
        Response(
            status=200,
            body=CouponsListForReviewResponse(
                paging=PagingOutput(total=2, offset=0, limit=1),
                coupons_list=[
                    CouponForReview(
                        biz_id="123",
                        item_id="456",
                        revision_id=0,
                        title="title_0",
                        products_description="description_0",
                    )
                ],
            ).SerializeToString(),
        )
    )
    mock_get_coupons_list_for_review(
        Response(
            status=200,
            body=CouponsListForReviewResponse(
                paging=PagingOutput(total=2, offset=2, limit=0), coupons_list=[]
            ).SerializeToString(),
        )
    )

    result = []
    async for coupons_for_review in loyalty_client.get_coupons_list_for_review():
        result.append(coupons_for_review)

    assert result[0] == [
        dict(
            biz_id=123,
            item_id=456,
            revision_id=0,
            title="title_0",
            products_description="description_0",
        )
    ]


@pytest.mark.parametrize(
    "bad_field, expected_field_exception",
    [
        (dict(biz_id=""), {"biz_id": ["Not a valid integer."]}),
        (dict(biz_id="abc"), {"biz_id": ["Not a valid integer."]}),
        (dict(biz_id="-1"), {"biz_id": ["Must be at least 1."]}),
        (dict(biz_id="0"), {"biz_id": ["Must be at least 1."]}),
        (dict(item_id=""), {"item_id": ["Not a valid integer."]}),
        (dict(item_id="abc"), {"item_id": ["Not a valid integer."]}),
        (dict(item_id="-1"), {"item_id": ["Must be at least 1."]}),
        (dict(item_id="0"), {"item_id": ["Must be at least 1."]}),
    ],
)
async def test_raises_for_bad_coupon_data(
    loyalty_client,
    mock_get_coupons_list_for_review,
    bad_field,
    expected_field_exception,
):
    coupon_data = dict(
        biz_id="123",
        item_id="456",
        revision_id=2,
        title="title_1",
        cover_url="cover_url_1",
        products_description="description_1",
        conditions="conditions_1",
    )
    coupon_data.update(**bad_field)
    mock_get_coupons_list_for_review(
        Response(
            status=200,
            body=CouponsListForReviewResponse(
                paging=PagingOutput(total=0, offset=0, limit=0),
                coupons_list=[CouponForReview(**coupon_data)],
            ).SerializeToString(),
        )
    )

    with pytest.raises(BadLoyaltyResponse) as exc:
        async for _ in loyalty_client.get_coupons_list_for_review():
            pass

    assert exc.value.args == ({"coupons_list": {0: expected_field_exception}},)
