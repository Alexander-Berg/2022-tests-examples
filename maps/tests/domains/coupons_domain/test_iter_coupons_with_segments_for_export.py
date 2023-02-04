import pytest

from maps_adv.geosmb.clients.facade import CouponType, Currency, SegmentType

pytestmark = [pytest.mark.asyncio]


async def test_iterator_is_empty_if_there_are_no_data(domain, facade):
    facade.list_coupons_with_segments.seq = [dict()]

    iter_counts = 0
    async for _ in domain.iter_coupons_with_segments_for_export():
        iter_counts += 1

    assert iter_counts == 0


@pytest.mark.parametrize(
    "coupon_fields, expected_record",
    [
        # SERVICE coupon
        (
            dict(
                coupon_id=11,
                type=CouponType.SERVICE,
                segments=[SegmentType.ACTIVE],
                percent_discount=10,
            ),
            dict(
                coupon_id=11,
                type="SERVICE",
                segments=["ACTIVE"],
                percent_discount=10,
                cost_discount=None,
                currency=None,
                poi_subscript=[
                    ("RU", "Сертификат на услугу: Получите скидку 10%"),
                    ("EN", "Service certificate: Get discount 10%"),
                ],
            ),
        ),
        # FREE coupon with percent discount
        (
            dict(
                coupon_id=11,
                type=CouponType.FREE,
                segments=[SegmentType.ACTIVE],
                percent_discount=10,
            ),
            dict(
                coupon_id=11,
                type="FREE",
                segments=["ACTIVE"],
                percent_discount=10,
                cost_discount=None,
                currency=None,
                poi_subscript=[
                    (
                        "RU",
                        "Бесплатный сертификат на %: Получите скидку 10%",
                    ),
                    ("EN", "Free % certificate: Get discount 10%"),
                ],
            ),
        ),
        # FREE coupon with cost discount
        (
            dict(
                coupon_id=33,
                type=CouponType.FREE,
                segments=[SegmentType.UNPROCESSED_ORDERS],
                cost_discount="91.99",
                currency=Currency.RUB,
            ),
            dict(
                coupon_id=33,
                type="FREE",
                segments=["UNPROCESSED_ORDERS"],
                percent_discount=None,
                cost_discount="91.99",
                currency="RUB",
                poi_subscript=[
                    (
                        "RU",
                        "Бесплатный сертификат на сумму: Получите скидку 91.99 руб.",
                    ),
                    ("EN", "Free certificate on amount: Get discount 91.99 rub."),
                ],
            ),
        ),
    ],
)
async def test_returns_all_expected_columns(
    domain, facade, coupon_fields, expected_record
):
    facade.list_coupons_with_segments.seq = [{123: [coupon_fields]}]

    records = []
    async for orgs in domain.iter_coupons_with_segments_for_export():
        records.extend(orgs)

    assert records == [dict(biz_id=123, **expected_record)]


@pytest.mark.parametrize("currency", [c for c in Currency if c != Currency.RUB])
async def test_skips_coupons_with_not_rub_currency(domain, facade, currency):
    facade.list_coupons_with_segments.seq = [
        {
            123: [
                dict(
                    coupon_id=33,
                    type=CouponType.FREE,
                    segments=["UNPROCESSED_ORDERS"],
                    cost_discount="91.99",
                    currency=currency,
                )
            ]
        }
    ]

    records = []
    async for orgs in domain.iter_coupons_with_segments_for_export():
        records.extend(orgs)

    assert records == []


async def test_returns_record_per_coupon(domain, facade):
    facade.list_coupons_with_segments.seq = [
        {
            123: [
                dict(
                    coupon_id=10,
                    type=CouponType.FREE,
                    segments=[SegmentType.REGULAR],
                    percent_discount=11,
                ),
                dict(
                    coupon_id=11,
                    type=CouponType.FREE,
                    segments=[SegmentType.REGULAR],
                    percent_discount=11,
                ),
            ],
        },
    ]

    records = []
    async for biz_chunk in domain.iter_coupons_with_segments_for_export():
        records.extend(biz_chunk)

    assert [(record["biz_id"], record["coupon_id"]) for record in records] == [
        (123, 10),
        (123, 11),
    ]


@pytest.mark.parametrize("iter_size", [None, 0, 1, 2, 100])
@pytest.mark.parametrize(
    "facade_data_seq",
    (
        # 1 sequence
        [
            {
                idx: [
                    dict(
                        coupon_id=idx * 10,
                        type=CouponType.FREE,
                        segments=[SegmentType.REGULAR],
                        percent_discount=11,
                    )
                ]
                for idx in range(3)
            }
        ],
        # several sequences
        [
            {
                0: [
                    dict(
                        coupon_id=10,
                        type=CouponType.FREE,
                        segments=[SegmentType.REGULAR],
                        percent_discount=11,
                    )
                ],
                1: [
                    dict(
                        coupon_id=11,
                        type=CouponType.FREE,
                        segments=[SegmentType.REGULAR],
                        percent_discount=11,
                    )
                ],
            },
            {
                2: [
                    dict(
                        coupon_id=13,
                        type=CouponType.FREE,
                        segments=[SegmentType.REGULAR],
                        percent_discount=11,
                    )
                ]
            },
        ],
    ),
)
async def test_returns_all_records_eventually(
    domain, facade, iter_size, facade_data_seq
):
    facade.list_coupons_with_segments.seq = facade_data_seq

    records = []
    async for biz_chunk in domain.iter_coupons_with_segments_for_export(
        iter_size=iter_size
    ):
        records.extend(biz_chunk)

    assert [record["biz_id"] for record in records] == [idx for idx in range(3)]


@pytest.mark.parametrize(
    "facade_data_seq",
    (
        # 1 sequence
        [
            {
                idx: [
                    dict(
                        coupon_id=idx * 10,
                        type=CouponType.FREE,
                        segments=[SegmentType.REGULAR],
                        percent_discount=11,
                    )
                ]
                for idx in range(3)
            }
        ],
        # several sequences
        [
            {
                0: [
                    dict(
                        coupon_id=10,
                        type=CouponType.FREE,
                        segments=[SegmentType.REGULAR],
                        percent_discount=11,
                    )
                ],
                1: [
                    dict(
                        coupon_id=11,
                        type=CouponType.FREE,
                        segments=[SegmentType.REGULAR],
                        percent_discount=11,
                    )
                ],
            },
            {
                2: [
                    dict(
                        coupon_id=13,
                        type=CouponType.FREE,
                        segments=[SegmentType.REGULAR],
                        percent_discount=11,
                    )
                ]
            },
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
    facade.list_coupons_with_segments.seq = facade_data_seq

    chunk_sizes = []
    async for data_chunk in domain.iter_coupons_with_segments_for_export(
        iter_size=iter_size
    ):
        chunk_sizes.append(len(data_chunk))

    assert chunk_sizes == expected_chunk_sizes
