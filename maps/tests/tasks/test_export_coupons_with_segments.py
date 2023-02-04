import pytest

from maps_adv.geosmb.clients.facade import CouponType, Currency, SegmentType
from maps_adv.geosmb.crane_operator.server.lib.tasks import (
    CouponsWithSegmentsYtExportTask,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def task(config, coupons_domain):
    return CouponsWithSegmentsYtExportTask(config=config, coupons_domain=coupons_domain)


async def test_creates_expected_table(task, mock_yt):
    await task

    assert mock_yt["create"].called
    assert mock_yt["create"].call_args[0][1] == "//path/to/coupons-with-segments-table"


async def test_creates_table_with_expected_schema(task, mock_yt):
    await task

    assert mock_yt["create"].call_args[1] == dict(
        attributes={
            "schema": [
                dict(name="biz_id", type="uint64", required=True),
                dict(name="coupon_id", type="uint64", required=True),
                dict(name="type", type="string", required=True),
                dict(
                    name="segments",
                    type_v3={"item": "string", "type_name": "list"},
                    required=True,
                ),
                dict(name="percent_discount", type="uint32", required=False),
                dict(name="cost_discount", type="string", required=False),
                dict(name="currency", type="string", required=False),
                dict(
                    name="poi_subscript",
                    type_v3={
                        "type_name": "dict",
                        "key": "string",
                        "value": "string",
                    },
                    required=True,
                ),
            ]
        }
    )


async def test_writes_data_as_expected(task, mock_yt, config, facade):
    facade.list_coupons_with_segments.seq = [
        {
            123: [
                dict(
                    coupon_id=11,
                    type=CouponType.SERVICE,
                    segments=[SegmentType.REGULAR, SegmentType.ACTIVE],
                    percent_discount=11,
                )
            ],
            456: [
                dict(
                    coupon_id=22,
                    type=CouponType.FREE,
                    segments=[SegmentType.UNPROCESSED_ORDERS],
                    cost_discount="91.99",
                    currency=Currency.RUB,
                ),
            ],
        },
    ]

    await task

    mock_yt["write_table"].assert_called_with(
        config["COUPONS_WITH_SEGMENTS_YT_EXPORT_TABLE"],
        [
            dict(
                biz_id=123,
                coupon_id=11,
                type="SERVICE",
                segments=["REGULAR", "ACTIVE"],
                percent_discount=11,
                cost_discount=None,
                currency=None,
                poi_subscript=[
                    ("RU", "Сертификат на услугу: Получите скидку 11%"),
                    ("EN", "Service certificate: Get discount 11%"),
                ],
            ),
            dict(
                biz_id=456,
                coupon_id=22,
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
        ],
    )
