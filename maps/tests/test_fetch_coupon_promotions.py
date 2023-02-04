import pytest
from aiohttp.web import Response
from smb.common.testing_utils import dt

from maps_adv.geosmb.clients.facade.proto.facade_pb2 import (
    CouponPromotionItem,
    FetchCouponPromotionsInput,
    FetchCouponPromotionsOutput,
    Paging,
    PagingOutput,
)

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(facade_client, mock_fetch_coupon_promotions):
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
            body=FetchCouponPromotionsOutput(
                promotions=[],
                paging=PagingOutput(limit=500, offset=0, total=20),
            ).SerializeToString(),
        )

    mock_fetch_coupon_promotions(facade_handler)

    async for _ in facade_client.fetch_coupon_promotions():
        pass

    assert request_url == "http://facade.server/v1/fetch_coupon_promotions"
    assert request_headers["Content-Type"] == "application/x-protobuf"
    proto_body = FetchCouponPromotionsInput.FromString(request_body)
    assert proto_body == FetchCouponPromotionsInput(paging=Paging(limit=500, offset=0))


async def test_paginates_in_request_correctly(
    facade_client, mock_fetch_coupon_promotions
):
    request_bodies = []

    async def facade_handler(request):
        nonlocal request_bodies
        request_body = await request.read()
        request_bodies.append(FetchCouponPromotionsInput.FromString(request_body))
        return Response(
            status=200,
            body=FetchCouponPromotionsOutput(
                promotions=[
                    CouponPromotionItem(
                        advert_id=111,
                        coupon_id=11100,
                        biz_id=1110011,
                        name="Какая-то акция",
                        date_from=dt("2020-03-16 18:00:00", as_proto=True),
                        date_to=dt("2020-04-17 18:00:00", as_proto=True),
                        description="Какое-то описание",
                        announcement="Какой-то анонс",
                        image_url="http://image.url/promo",
                        coupon_url="http://promotion.url/promo",
                    )
                ],
                paging=PagingOutput(limit=500, offset=0, total=20),
            ).SerializeToString(),
        )

    mock_fetch_coupon_promotions(facade_handler)
    mock_fetch_coupon_promotions(facade_handler)
    mock_fetch_coupon_promotions(facade_handler)
    mock_fetch_coupon_promotions(
        Response(
            status=200,
            body=FetchCouponPromotionsOutput(
                promotions=[], paging=PagingOutput(limit=500, offset=0, total=20)
            ).SerializeToString(),
        )
    )

    async for _ in facade_client.fetch_coupon_promotions():
        pass

    assert request_bodies == [
        FetchCouponPromotionsInput(paging=Paging(limit=500, offset=0)),
        FetchCouponPromotionsInput(paging=Paging(limit=500, offset=500)),
        FetchCouponPromotionsInput(paging=Paging(limit=500, offset=1000)),
    ]


async def test_returns_coupon_promotions_data(
    facade_client, mock_fetch_coupon_promotions
):
    mock_fetch_coupon_promotions(
        Response(
            status=200,
            body=FetchCouponPromotionsOutput(
                promotions=[
                    CouponPromotionItem(
                        advert_id=111,
                        coupon_id=11100,
                        biz_id=1110011,
                        name="Какая-то акция",
                        date_from=dt("2020-03-16 18:00:00", as_proto=True),
                        date_to=dt("2020-04-17 18:00:00", as_proto=True),
                        description="Какое-то описание",
                        announcement="Какой-то анонс",
                        image_url="http://image.url/promo1",
                        coupon_url="http://promotion.url/promo1",
                    ),
                    # no biz_id
                    CouponPromotionItem(
                        advert_id=222,
                        coupon_id=22200,
                        name="Какая-то другая акция",
                        date_from=dt("2020-02-16 18:00:00", as_proto=True),
                        date_to=dt("2020-03-17 18:00:00", as_proto=True),
                        description="Какое-то другое описание",
                        announcement="Какой-то другой анонс",
                        image_url="http://image.url/promo2",
                        coupon_url="http://promotion.url/promo2",
                    ),
                ],
                paging=PagingOutput(limit=1000, offset=0, total=20),
            ).SerializeToString(),
        )
    )
    mock_fetch_coupon_promotions(
        Response(
            status=200,
            body=FetchCouponPromotionsOutput(
                promotions=[
                    CouponPromotionItem(
                        advert_id=333,
                        coupon_id=33300,
                        biz_id=3330033,
                        name="Какая-то ещё акция",
                        date_from=dt("2020-01-16 18:00:00", as_proto=True),
                        date_to=dt("2020-02-17 18:00:00", as_proto=True),
                        description="Какое-то ещё описание",
                        announcement="Какой-то ещё анонс",
                        image_url="http://image.url/promo3",
                        coupon_url="http://promotion.url/promo3",
                    ),
                ],
                paging=PagingOutput(limit=1000, offset=0, total=20),
            ).SerializeToString(),
        )
    )
    mock_fetch_coupon_promotions(
        Response(
            status=200,
            body=FetchCouponPromotionsOutput(
                promotions=[],
                paging=PagingOutput(limit=1000, offset=0, total=20),
            ).SerializeToString(),
        )
    )

    result = []
    async for chunk in facade_client.fetch_coupon_promotions():
        result.append(chunk)

    assert result == [
        [
            dict(
                advert_id=111,
                coupon_id=11100,
                biz_id=1110011,
                name="Какая-то акция",
                date_from=dt("2020-03-16 18:00:00"),
                date_to=dt("2020-04-17 18:00:00"),
                description="Какое-то описание",
                announcement="Какой-то анонс",
                image_url="http://image.url/promo1",
                coupon_url="http://promotion.url/promo1",
            ),
            dict(
                advert_id=222,
                coupon_id=22200,
                name="Какая-то другая акция",
                date_from=dt("2020-02-16 18:00:00"),
                date_to=dt("2020-03-17 18:00:00"),
                description="Какое-то другое описание",
                announcement="Какой-то другой анонс",
                image_url="http://image.url/promo2",
                coupon_url="http://promotion.url/promo2",
            ),
        ],
        [
            dict(
                advert_id=333,
                coupon_id=33300,
                biz_id=3330033,
                name="Какая-то ещё акция",
                date_from=dt("2020-01-16 18:00:00"),
                date_to=dt("2020-02-17 18:00:00"),
                description="Какое-то ещё описание",
                announcement="Какой-то ещё анонс",
                image_url="http://image.url/promo3",
                coupon_url="http://promotion.url/promo3",
            )
        ],
    ]


async def test_returns_empty_list_if_got_nothing(
    facade_client, mock_fetch_coupon_promotions
):
    mock_fetch_coupon_promotions(
        Response(
            status=200,
            body=FetchCouponPromotionsOutput(
                promotions=[], paging=PagingOutput(limit=1000, offset=0, total=20)
            ).SerializeToString(),
        )
    )

    async for chunk in facade_client.fetch_coupon_promotions():
        assert chunk == []
