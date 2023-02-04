import pytest

from maps_adv.common.helpers import Any, dt
from maps_adv.geosmb.scenarist.server.lib.enums import ScenarioName, SubscriptionStatus
from maps_adv.geosmb.scenarist.server.lib.tasks import SubscriptionsVersionsYtExportTask

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("mock_yt")]


@pytest.fixture
def task(config, dm):
    return SubscriptionsVersionsYtExportTask(config=config, dm=dm)


async def test_creates_table(task, mock_yt):
    await task
    assert mock_yt["create"].called
    assert mock_yt["create"].call_args[0][1] == "//path/to/sub-version-table"


async def test_writes_data_as_expected(factory, task, mock_yt):
    sub_id_1 = await factory.create_subscription(
        scenario_name=ScenarioName.DISCOUNT_FOR_LOST,
        biz_id=123,
        coupon_id=456,
        status=SubscriptionStatus.ACTIVE,
        created_at=dt("2020-01-01 11:11:11"),
    )
    sub_id_2 = await factory.create_subscription(
        scenario_name=ScenarioName.ENGAGE_PROSPECTIVE,
        biz_id=987,
        coupon_id=654,
        status=SubscriptionStatus.COMPLETED,
        created_at=dt("2020-02-02 22:22:22"),
    )

    await task

    assert mock_yt["write_table"].called
    assert mock_yt["write_table"].call_args[0][1] == [
        {
            "version_id": Any(int),
            "subscription_id": sub_id_1,
            "biz_id": 123,
            "scenario_code": "DISCOUNT_FOR_LOST",
            "coupon_id": 456,
            "status": "ACTIVE",
            "created_at": 1577877071000000,
        },
        {
            "version_id": Any(int),
            "subscription_id": sub_id_2,
            "biz_id": 987,
            "scenario_code": "ENGAGE_PROSPECTIVE",
            "coupon_id": 654,
            "status": "COMPLETED",
            "created_at": 1580682142000000,
        },
    ]
