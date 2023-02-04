import pytest

from maps_adv.geosmb.crane_operator.server.lib.tasks import CouponPromotionsYtExportTask

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def task(config, coupons_domain):
    return CouponPromotionsYtExportTask(config=config, coupons_domain=coupons_domain)


async def test_creates_expected_table(task, mock_yt):
    await task

    assert mock_yt["create"].called
    assert mock_yt["create"].call_args[0][1] == "//path/to/coupon-promotions-table"


async def test_creates_table_with_expected_schema(task, mock_yt):
    await task

    assert mock_yt["create"].call_args[1] == dict(
        attributes={
            "schema": [
                dict(name="advert_id", type="uint64", required=True),
                dict(name="coupon_id", type="uint64", required=True),
                dict(name="biz_id", type="uint64", required=False),
                dict(name="name", type="string", required=True),
                dict(name="date_from", type="string", required=True),
                dict(name="date_to", type="string", required=True),
                dict(name="description", type="string", required=True),
                dict(name="announcement", type="string", required=True),
                dict(name="image_url", type="string", required=True),
                dict(name="coupon_url", type="string", required=True),
            ]
        }
    )


async def test_writes_data_as_expected(task, mock_yt, config, facade):
    await task

    mock_yt["write_table"].assert_called_with(
        config["COUPON_PROMOTIONS_YT_EXPORT_TABLE"],
        [
            {
                "advert_id": 111,
                "announcement": "Какой-то анонс",
                "biz_id": 1110011,
                "coupon_id": 11100,
                "coupon_url": "http://promotion.url/promo1",
                "date_from": "2020-03-16",
                "date_to": "2020-04-17",
                "description": "Какое-то описание",
                "image_url": "http://image.url/promo1",
                "name": "Какая-то акция",
            },
            {
                "advert_id": 222,
                "announcement": "Какой-то другой анонс",
                "coupon_id": 22200,
                "coupon_url": "http://promotion.url/promo2",
                "date_from": "2020-02-16",
                "date_to": "2020-03-17",
                "description": "Какое-то другое описание",
                "image_url": "http://image.url/promo2",
                "name": "Какая-то другая акция",
            },
        ],
    )
