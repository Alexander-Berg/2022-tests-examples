import pytest
from aiohttp.web import Response
from smb.common.testing_utils import dt

from maps_adv.geosmb.clients.facade import (
    BadFacadeResponse,
    CouponCoverType as CouponCoverTypeEnum,
    CouponDistribution as CouponDistributionEnum,
    CouponModerationStatus as CouponModerationStatusEnum,
    Currency as CurrencyEnum,
)
from maps_adv.geosmb.clients.facade.proto.facade_pb2 import (
    BusinessCouponsForSnapshotRequest,
    BusinessCouponsForSnapshotResponse,
    Cost,
    CouponCoverTemplate,
    CouponCoverType,
    CouponDistribution,
    CouponModerationStatus,
    Currency,
    Paging,
    PagingOutput,
)

pytestmark = [pytest.mark.asyncio]


def make_coupon_item(**overrides):
    item_params = dict(
        biz_id=111,
        item_id=222,
        title="Coupon title",
        services=[
            BusinessCouponsForSnapshotResponse.CouponService(
                service_id="service_id",
                level=1,
                price=Cost(currency=Currency.RUB, cost="100.00"),
                name="service-kek",
                duration=60,
            )
        ],
        products_description="desc",
        price=Cost(currency=Currency.RUB, cost="111.11"),
        discount=0,
        discounted_price=Cost(currency=Currency.RUB, cost="111.11"),
        start_date=dt("2020-01-01 00:00:00", as_proto=True),
        get_until_date=dt("2020-02-02 00:00:00", as_proto=True),
        end_date=dt("2020-03-03 00:00:00", as_proto=True),
        distribution=CouponDistribution.PUBLIC,
        moderation_status=CouponModerationStatus.IN_APPROVED,
        published=True,
        payments_enabled=True,
        conditions="This is conditions",
        creator_login="vas_pup",
        creator_uid="12345678",
        created_at=dt("2019-12-12 00:00:00", as_proto=True),
        cover_templates=[
            CouponCoverTemplate(
                url_template="http://avatar.yandex.ru/get-smth/abcde/%s",
                aliases=["alias_x1", "alias_x2"],
                type=CouponCoverType.DEFAULT,
            ),
            CouponCoverTemplate(
                url_template="http://avatar.yandex.ru/get-smth/abcde/%s",
                aliases=["square_x1", "square_x2"],
                type=CouponCoverType.SQUARE,
            ),
        ],
        coupon_showcase_url="https://showcase.yandex/coupon",
        meta='{"some": "json"}',
    )
    item_params.update(**overrides)

    return BusinessCouponsForSnapshotResponse.CouponItemForSnapshot(**item_params)


async def test_sends_correct_request(
    facade_client, mock_get_business_coupons_for_snapshot
):
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
            body=BusinessCouponsForSnapshotResponse(
                items_list=[], paging=PagingOutput(limit=1000, offset=0, total=20)
            ).SerializeToString(),
        )

    mock_get_business_coupons_for_snapshot(facade_handler)

    async for _ in facade_client.get_business_coupons_for_snapshot():
        pass

    assert request_url == "http://facade.server/v1/get_business_coupons_for_snapshot"
    assert request_headers["Content-Type"] == "application/x-protobuf"
    proto_body = BusinessCouponsForSnapshotRequest.FromString(request_body)
    assert proto_body == BusinessCouponsForSnapshotRequest(
        paging=Paging(limit=1000, offset=0)
    )


async def test_paginates_in_request_correctly(
    facade_client, mock_get_business_coupons_for_snapshot
):
    request_bodies = []

    async def facade_handler(request):
        nonlocal request_bodies
        request_body = await request.read()
        request_bodies.append(
            BusinessCouponsForSnapshotRequest.FromString(request_body)
        )
        return Response(
            status=200,
            body=BusinessCouponsForSnapshotResponse(
                items_list=[make_coupon_item()],
                paging=PagingOutput(limit=1000, offset=0, total=20),
            ).SerializeToString(),
        )

    mock_get_business_coupons_for_snapshot(facade_handler)
    mock_get_business_coupons_for_snapshot(facade_handler)
    mock_get_business_coupons_for_snapshot(facade_handler)
    mock_get_business_coupons_for_snapshot(
        Response(
            status=200,
            body=BusinessCouponsForSnapshotResponse(
                items_list=[], paging=PagingOutput(limit=1000, offset=0, total=20)
            ).SerializeToString(),
        )
    )

    async for _ in facade_client.get_business_coupons_for_snapshot():
        pass

    assert request_bodies == [
        BusinessCouponsForSnapshotRequest(paging=Paging(limit=1000, offset=0)),
        BusinessCouponsForSnapshotRequest(paging=Paging(limit=1000, offset=1000)),
        BusinessCouponsForSnapshotRequest(paging=Paging(limit=1000, offset=2000)),
    ]


async def test_returns_coupon_items_list(
    facade_client, mock_get_business_coupons_for_snapshot
):
    mock_get_business_coupons_for_snapshot(
        Response(
            status=200,
            body=BusinessCouponsForSnapshotResponse(
                items_list=[
                    BusinessCouponsForSnapshotResponse.CouponItemForSnapshot(
                        biz_id=1,
                        item_id=11,
                        title="Coupon title 1",
                        services=[
                            BusinessCouponsForSnapshotResponse.CouponService(
                                service_id="service_id_1",
                                level=1,
                                price=Cost(currency=Currency.RUB, cost="100.00"),
                                name="service-kek-1",
                                duration=10,
                            )
                        ],
                        products_description="desc 1",
                        price=Cost(currency=Currency.RUB, cost="111.11"),
                        discount=10,
                        discounted_price=Cost(currency=Currency.RUB, cost="99.99"),
                        start_date=dt("2020-01-01 01:01:01", as_proto=True),
                        get_until_date=dt("2020-01-02 01:01:01", as_proto=True),
                        end_date=dt("2020-01-03 01:01:01", as_proto=True),
                        distribution=CouponDistribution.PUBLIC,
                        moderation_status=CouponModerationStatus.IN_APPROVED,
                        published=True,
                        payments_enabled=True,
                        conditions="This is conditions 1",
                        creator_login="vas_pup_01",
                        creator_uid="111111",
                        created_at=dt("2019-12-12 01:01:01", as_proto=True),
                        cover_templates=[
                            CouponCoverTemplate(
                                url_template="http://avatar.yandex.ru/get-smth/abcde/%s",
                                aliases=["alias_x1", "alias_x2"],
                                type=CouponCoverType.DEFAULT,
                            ),
                        ],
                        coupon_showcase_url="https://showcase.yandex/coupon1",
                        meta='{"some1": "data1"}',
                    ),
                    BusinessCouponsForSnapshotResponse.CouponItemForSnapshot(
                        biz_id=2,
                        item_id=22,
                        title="Coupon title 2",
                        services=[
                            BusinessCouponsForSnapshotResponse.CouponService(
                                service_id="service_id_2",
                                level=2,
                                price=Cost(currency=Currency.RUB, cost="200.00"),
                                name="service-kek-2",
                                duration=20,
                            )
                        ],
                        products_description="desc 2",
                        price=Cost(currency=Currency.RUB, cost="222.22"),
                        discount=20,
                        discounted_price=Cost(currency=Currency.RUB, cost="177.77"),
                        start_date=dt("2020-01-01 02:02:02", as_proto=True),
                        get_until_date=dt("2020-01-02 02:02:02", as_proto=True),
                        end_date=dt("2020-01-03 02:02:02", as_proto=True),
                        distribution=CouponDistribution.PRIVATE_FOR_SEGMENTS,
                        moderation_status=CouponModerationStatus.IN_REVIEW,
                        published=True,
                        payments_enabled=True,
                        conditions="This is conditions 2",
                        creator_login="vas_pup_02",
                        creator_uid="222222",
                        created_at=dt("2019-12-12 02:02:02", as_proto=True),
                        cover_templates=[
                            CouponCoverTemplate(
                                url_template="http://avatar.yandex.ru/get-smth/abcdef1/%s",
                                aliases=["alias_x1", "alias_x2"],
                                type=CouponCoverType.DEFAULT,
                            ),
                            CouponCoverTemplate(
                                url_template="http://avatar.yandex.ru/get-smth/abcdef2/%s",
                                aliases=["square_x1", "square_x2"],
                                type=CouponCoverType.SQUARE,
                            ),
                        ],
                        coupon_showcase_url="https://showcase.yandex/coupon2",
                        meta='{"some2": "data2"}',
                    ),
                ],
                paging=PagingOutput(limit=1000, offset=0, total=20),
            ).SerializeToString(),
        )
    )
    mock_get_business_coupons_for_snapshot(
        Response(
            status=200,
            body=BusinessCouponsForSnapshotResponse(
                items_list=[
                    BusinessCouponsForSnapshotResponse.CouponItemForSnapshot(
                        biz_id=3,
                        item_id=33,
                        title="Coupon title 3",
                        services=[
                            BusinessCouponsForSnapshotResponse.CouponService(
                                service_id="service_id_3",
                                level=3,
                                price=Cost(currency=Currency.RUB, cost="300.00"),
                                name="service-kek-3",
                                duration=30,
                            )
                        ],
                        products_description="desc 3",
                        price=Cost(currency=Currency.RUB, cost="333.33"),
                        discount=30,
                        discounted_price=Cost(currency=Currency.RUB, cost="233.33"),
                        start_date=dt("2020-01-01 03:03:03", as_proto=True),
                        get_until_date=dt("2020-01-02 03:03:03", as_proto=True),
                        end_date=dt("2020-01-03 03:03:03", as_proto=True),
                        distribution=CouponDistribution.PRIVATE_BY_LINK,
                        moderation_status=CouponModerationStatus.IN_REJECTED,
                        published=False,
                        payments_enabled=False,
                        conditions="This is conditions 3",
                        creator_login="vas_pup_03",
                        creator_uid="333333",
                        created_at=dt("2019-12-12 03:03:03", as_proto=True),
                        cover_templates=[],
                        coupon_showcase_url="https://showcase.yandex/coupon3",
                        meta='{"some3": "data3"}',
                    ),
                ],
                paging=PagingOutput(limit=1000, offset=0, total=20),
            ).SerializeToString(),
        )
    )
    mock_get_business_coupons_for_snapshot(
        Response(
            status=200,
            body=BusinessCouponsForSnapshotResponse(
                items_list=[], paging=PagingOutput(limit=1000, offset=0, total=20)
            ).SerializeToString(),
        )
    )

    result = []
    async for coupon_items in facade_client.get_business_coupons_for_snapshot():
        result.append(coupon_items)

    assert result == [
        [
            dict(
                biz_id=1,
                item_id=11,
                title="Coupon title 1",
                services=[
                    dict(
                        service_id="service_id_1",
                        level=1,
                        price=dict(currency=CurrencyEnum.RUB, cost="100.00"),
                        name="service-kek-1",
                        duration=10,
                    )
                ],
                products_description="desc 1",
                price=dict(currency=CurrencyEnum.RUB, cost="111.11"),
                discount=10,
                discounted_price=dict(currency=CurrencyEnum.RUB, cost="99.99"),
                start_date=dt("2020-01-01 01:01:01"),
                get_until_date=dt("2020-01-02 01:01:01"),
                end_date=dt("2020-01-03 01:01:01"),
                distribution=CouponDistributionEnum.PUBLIC,
                moderation_status=CouponModerationStatusEnum.IN_APPROVED,
                published=True,
                payments_enabled=True,
                conditions="This is conditions 1",
                creator_login="vas_pup_01",
                creator_uid="111111",
                created_at=dt("2019-12-12 01:01:01"),
                cover_templates=[
                    dict(
                        url_template="http://avatar.yandex.ru/get-smth/abcde/%s",
                        aliases=["alias_x1", "alias_x2"],
                        type=CouponCoverTypeEnum.DEFAULT,
                    ),
                ],
                coupon_showcase_url="https://showcase.yandex/coupon1",
                meta={"some1": "data1"},
            ),
            dict(
                biz_id=2,
                item_id=22,
                title="Coupon title 2",
                services=[
                    dict(
                        service_id="service_id_2",
                        level=2,
                        price=dict(currency=CurrencyEnum.RUB, cost="200.00"),
                        name="service-kek-2",
                        duration=20,
                    )
                ],
                products_description="desc 2",
                price=dict(currency=CurrencyEnum.RUB, cost="222.22"),
                discount=20,
                discounted_price=dict(currency=CurrencyEnum.RUB, cost="177.77"),
                start_date=dt("2020-01-01 02:02:02"),
                get_until_date=dt("2020-01-02 02:02:02"),
                end_date=dt("2020-01-03 02:02:02"),
                distribution=CouponDistributionEnum.PRIVATE_FOR_SEGMENTS,
                moderation_status=CouponModerationStatusEnum.IN_REVIEW,
                published=True,
                payments_enabled=True,
                conditions="This is conditions 2",
                creator_login="vas_pup_02",
                creator_uid="222222",
                created_at=dt("2019-12-12 02:02:02"),
                cover_templates=[
                    dict(
                        url_template="http://avatar.yandex.ru/get-smth/abcdef1/%s",
                        aliases=["alias_x1", "alias_x2"],
                        type=CouponCoverTypeEnum.DEFAULT,
                    ),
                    dict(
                        url_template="http://avatar.yandex.ru/get-smth/abcdef2/%s",
                        aliases=["square_x1", "square_x2"],
                        type=CouponCoverTypeEnum.SQUARE,
                    ),
                ],
                coupon_showcase_url="https://showcase.yandex/coupon2",
                meta={"some2": "data2"},
            ),
        ],
        [
            dict(
                biz_id=3,
                item_id=33,
                title="Coupon title 3",
                services=[
                    dict(
                        service_id="service_id_3",
                        level=3,
                        price=dict(currency=CurrencyEnum.RUB, cost="300.00"),
                        name="service-kek-3",
                        duration=30,
                    )
                ],
                products_description="desc 3",
                price=dict(currency=CurrencyEnum.RUB, cost="333.33"),
                discount=30,
                discounted_price=dict(currency=CurrencyEnum.RUB, cost="233.33"),
                start_date=dt("2020-01-01 03:03:03"),
                get_until_date=dt("2020-01-02 03:03:03"),
                end_date=dt("2020-01-03 03:03:03"),
                distribution=CouponDistributionEnum.PRIVATE_BY_LINK,
                moderation_status=CouponModerationStatusEnum.IN_REJECTED,
                published=False,
                payments_enabled=False,
                conditions="This is conditions 3",
                creator_login="vas_pup_03",
                creator_uid="333333",
                created_at=dt("2019-12-12 03:03:03"),
                cover_templates=[],
                coupon_showcase_url="https://showcase.yandex/coupon3",
                meta={"some3": "data3"},
            )
        ],
    ]


async def test_returns_empty_list_if_got_nothing(
    facade_client, mock_get_business_coupons_for_snapshot
):
    mock_get_business_coupons_for_snapshot(
        Response(
            status=200,
            body=BusinessCouponsForSnapshotResponse(
                items_list=[], paging=PagingOutput(limit=1000, offset=0, total=20)
            ).SerializeToString(),
        )
    )

    async for orgs_with_coupons in facade_client.get_business_coupons_for_snapshot():
        assert orgs_with_coupons == []


async def test_returns_coupon_item_without_optional_fields(
    facade_client, mock_get_business_coupons_for_snapshot
):
    mock_get_business_coupons_for_snapshot(
        Response(
            status=200,
            body=BusinessCouponsForSnapshotResponse(
                items_list=[
                    make_coupon_item(
                        products_description=None,
                        conditions=None,
                        creator_login=None,
                        creator_uid=None,
                        cover_templates=[],
                        coupon_showcase_url=None,
                        meta="{}",
                    )
                ],
                paging=PagingOutput(limit=1000, offset=0, total=20),
            ).SerializeToString(),
        )
    )
    mock_get_business_coupons_for_snapshot(
        Response(
            status=200,
            body=BusinessCouponsForSnapshotResponse(
                items_list=[], paging=PagingOutput(limit=1000, offset=0, total=20)
            ).SerializeToString(),
        )
    )

    result = []
    async for coupon_items in facade_client.get_business_coupons_for_snapshot():
        result.append(coupon_items)

    assert result == [
        [
            dict(
                biz_id=111,
                item_id=222,
                title="Coupon title",
                services=[
                    dict(
                        service_id="service_id",
                        level=1,
                        price=dict(currency=CurrencyEnum.RUB, cost="100.00"),
                        name="service-kek",
                        duration=60,
                    )
                ],
                price=dict(currency=CurrencyEnum.RUB, cost="111.11"),
                discount=0,
                discounted_price=dict(currency=CurrencyEnum.RUB, cost="111.11"),
                start_date=dt("2020-01-01 00:00:00"),
                get_until_date=dt("2020-02-02 00:00:00"),
                end_date=dt("2020-03-03 00:00:00"),
                distribution=CouponDistributionEnum.PUBLIC,
                moderation_status=CouponModerationStatusEnum.IN_APPROVED,
                published=True,
                payments_enabled=True,
                created_at=dt("2019-12-12 00:00:00"),
                cover_templates=[],
                meta={},
            ),
        ],
    ]


async def test_raises_if_meta_is_not_json(
    facade_client, mock_get_business_coupons_for_snapshot
):
    mock_get_business_coupons_for_snapshot(
        Response(
            status=200,
            body=BusinessCouponsForSnapshotResponse(
                items_list=[
                    make_coupon_item(
                        products_description=None,
                        conditions=None,
                        creator_login=None,
                        creator_uid=None,
                        cover_templates=[],
                        coupon_showcase_url=None,
                        meta="This is not json, is it?",
                    )
                ],
                paging=PagingOutput(limit=1000, offset=0, total=20),
            ).SerializeToString(),
        )
    )
    mock_get_business_coupons_for_snapshot(
        Response(
            status=200,
            body=BusinessCouponsForSnapshotResponse(
                items_list=[], paging=PagingOutput(limit=1000, offset=0, total=20)
            ).SerializeToString(),
        )
    )

    with pytest.raises(BadFacadeResponse, match=r".*meta must be a valid JSON.*"):
        async for _ in facade_client.get_business_coupons_for_snapshot():
            pass
