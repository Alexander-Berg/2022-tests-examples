import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.clients.facade import (
    CouponCoverType,
    CouponDistribution,
    CouponModerationStatus,
    Currency,
)
from maps_adv.geosmb.crane_operator.server.lib.tasks import BusinessCouponsYtExportTask

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def task(config, coupons_domain):
    return BusinessCouponsYtExportTask(config=config, coupons_domain=coupons_domain)


async def test_creates_expected_table(task, mock_yt):
    await task

    assert mock_yt["create"].called
    assert mock_yt["create"].call_args[0][1] == "//path/to/business-coupons-table"


async def test_creates_table_with_expected_schema(task, mock_yt):
    await task

    assert mock_yt["create"].call_args[1] == dict(
        attributes={
            "schema": [
                dict(name="biz_id", type="uint64", required=True),
                dict(name="item_id", type="uint64", required=True),
                dict(name="title", type="string", required=True),
                dict(name="services", type="any", required=False),
                dict(name="products_description", type="string", required=False),
                dict(name="price", type="string", required=True),
                dict(name="currency", type="string", required=True),
                dict(name="discount", type="uint32", required=True),
                dict(name="discounted_price", type="string", required=True),
                dict(name="start_date", type="timestamp", required=True),
                dict(name="get_until_date", type="timestamp", required=True),
                dict(name="end_date", type="timestamp", required=True),
                dict(name="distribution", type="string", required=True),
                dict(name="moderation_status", type="string", required=True),
                dict(name="published", type="boolean", required=True),
                dict(name="payments_enabled", type="boolean", required=True),
                dict(name="conditions", type="string", required=False),
                dict(name="creator_login", type="string", required=False),
                dict(name="creator_uid", type="string", required=False),
                dict(name="created_at", type="timestamp", required=True),
                dict(name="cover_templates", type="any", required=False),
                dict(name="coupon_showcase_url", type="string", required=False),
                dict(name="meta", type="any", required=False),
            ]
        }
    )


async def test_writes_data_as_expected(task, mock_yt, config, facade):
    facade.get_business_coupons_for_snapshot.seq = [
        [
            dict(
                biz_id=111,
                item_id=222,
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
        ]
    ]

    await task

    mock_yt["write_table"].assert_called_with(
        config["BUSINESS_COUPONS_YT_EXPORT_TABLE"],
        [
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
        ],
    )
