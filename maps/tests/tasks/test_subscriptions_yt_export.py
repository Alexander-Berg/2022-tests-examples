import pytest

from maps_adv.geosmb.clients.facade import CouponStatus
from maps_adv.geosmb.scenarist.server.lib.enums import ScenarioName
from maps_adv.geosmb.scenarist.server.lib.tasks import SubscriptionsYtExportTask

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("mock_yt")]


@pytest.fixture
def task(config, domain):
    return SubscriptionsYtExportTask(config=config, domain=domain)


async def test_creates_table(task, mock_yt):
    await task
    assert mock_yt["create"].called
    assert mock_yt["create"].call_args[0][1] == "//path/to/sub-table"


async def test_writes_data_as_expected(factory, task, mock_yt, facade):
    sub_id_1 = await factory.create_subscription(
        scenario_name=ScenarioName.DISCOUNT_FOR_LOST, biz_id=123, coupon_id=456
    )
    sub_id_2 = await factory.create_subscription(
        scenario_name=ScenarioName.ENGAGE_PROSPECTIVE, biz_id=987, coupon_id=654
    )
    facade.list_coupons_statuses.coro.return_value = [
        dict(biz_id=123, coupon_id=456, status=CouponStatus.RUNNING),
        dict(biz_id=987, coupon_id=654, status=CouponStatus.RUNNING),
    ]

    await task

    assert mock_yt["write_table"].called
    assert mock_yt["write_table"].call_args[0][1] == [
        {
            "subscription_id": sub_id_1,
            "scenario_code": "DISCOUNT_FOR_LOST",
            "segments": ["LOST"],
            "biz_id": 123,
            "coupon_id": 456,
        },
        {
            "subscription_id": sub_id_2,
            "scenario_code": "ENGAGE_PROSPECTIVE",
            "segments": ["NO_ORDERS", "PROSPECTIVE"],
            "biz_id": 987,
            "coupon_id": 654,
        },
    ]
