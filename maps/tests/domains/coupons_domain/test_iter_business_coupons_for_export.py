import copy

import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.clients.facade import (
    CouponCoverType,
    CouponDistribution,
    CouponModerationStatus,
    Currency,
)

pytestmark = [pytest.mark.asyncio]


def make_coupon_data(item_id: int = 222) -> dict:
    return dict(
        biz_id=111,
        item_id=item_id,
        title="Coupon title",
        services=[
            dict(
                service_id="service_id",
                level=1,
                price=dict(currency=Currency.RUB, cost="100.00"),
                name="service-kek",
                duration=60,
            )
        ],
        products_description="prod desc",
        price=dict(currency=Currency.RUB, cost="111.11"),
        discount=0,
        discounted_price=dict(currency=Currency.RUB, cost="111.11"),
        start_date=dt("2020-01-01 00:00:00"),
        get_until_date=dt("2020-02-02 00:00:00"),
        end_date=dt("2020-03-03 00:00:00"),
        distribution=CouponDistribution.PUBLIC,
        moderation_status=CouponModerationStatus.IN_APPROVED,
        published=True,
        payments_enabled=True,
        conditions="This is condition",
        creator_login="vas_pup",
        creator_uid="12345678",
        created_at=dt("2019-12-12 00:00:00"),
        cover_templates=[
            dict(
                url_template="http://avatar.yandex.ru/get-smth/abcde/%s",
                aliases=["alias_x1", "alias_x2"],
                type=CouponCoverType.DEFAULT,
            ),
        ],
        coupon_showcase_url="https://showcase.yandex/coupon1",
        meta={"some": "json"},
    )


async def test_iterator_is_empty_if_there_are_no_data(domain, facade):
    facade.get_business_coupons_for_snapshot.seq = [[]]

    iter_counts = 0
    async for _ in domain.iter_business_coupons_for_export():
        iter_counts += 1

    assert iter_counts == 0


async def test_returns_all_expected_columns(domain, facade):
    facade.get_business_coupons_for_snapshot.seq = [[make_coupon_data()]]

    records = []
    async for orgs in domain.iter_business_coupons_for_export():
        records.extend(orgs)

    assert records == [
        dict(
            biz_id=111,
            item_id=222,
            title="Coupon title",
            services=[
                dict(
                    service_id="service_id",
                    level=1,
                    price=dict(currency="RUB", cost="100.00"),
                    name="service-kek",
                    duration=60,
                )
            ],
            products_description="prod desc",
            price="111.11",
            currency="RUB",
            discount=0,
            discounted_price="111.11",
            start_date=1577836800000000,
            get_until_date=1580601600000000,
            end_date=1583193600000000,
            distribution="PUBLIC",
            moderation_status="IN_APPROVED",
            published=True,
            payments_enabled=True,
            conditions="This is condition",
            creator_login="vas_pup",
            creator_uid="12345678",
            created_at=1576108800000000,
            cover_templates=[
                dict(
                    url_template="http://avatar.yandex.ru/get-smth/abcde/%s",
                    aliases=["alias_x1", "alias_x2"],
                    type="DEFAULT",
                ),
            ],
            coupon_showcase_url="https://showcase.yandex/coupon1",
            meta={"some": "json"},
        )
    ]


@pytest.mark.parametrize("iter_size", [None, 0, 1, 2, 100])
@pytest.mark.parametrize(
    "facade_data_seq",
    (
        # 1 sequences
        [[make_coupon_data(item_id=idx) for idx in range(1, 4)]],
        # several sequences
        [
            [
                make_coupon_data(item_id=1),
                make_coupon_data(item_id=2),
            ],
            [
                make_coupon_data(item_id=3),
            ],
        ],
    ),
)
async def test_returns_all_records_eventually(
    domain, facade, iter_size, facade_data_seq
):
    facade.get_business_coupons_for_snapshot.seq = copy.deepcopy(facade_data_seq)

    records = []
    async for orgs in domain.iter_business_coupons_for_export(iter_size=iter_size):
        records.extend(orgs)

    assert [record["item_id"] for record in records] == [idx for idx in range(1, 4)]


@pytest.mark.parametrize(
    "facade_data_seq",
    (
        # 1 sequences
        [[make_coupon_data(item_id=idx) for idx in range(1, 4)]],
        # several sequences
        [
            [make_coupon_data(item_id=1), make_coupon_data(item_id=2)],
            [make_coupon_data(item_id=3)],
        ],
    ),
)
@pytest.mark.parametrize(
    "iter_size, expected_chunk_sizes",
    [(None, [3]), (0, [3]), (1, [1, 1, 1]), (2, [2, 1]), (100, [3])],
)
async def test_chunks_data_by_requested_size(
    domain, facade, facade_data_seq, iter_size, expected_chunk_sizes
):
    facade.get_business_coupons_for_snapshot.seq = copy.deepcopy(facade_data_seq)

    chunk_sizes = []
    async for data_chunk in domain.iter_business_coupons_for_export(
        iter_size=iter_size
    ):
        chunk_sizes.append(len(data_chunk))

    assert chunk_sizes == expected_chunk_sizes
