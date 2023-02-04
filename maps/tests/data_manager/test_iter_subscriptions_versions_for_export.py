import pytest
from smb.common.testing_utils import Any, dt

from maps_adv.geosmb.scenarist.server.lib.enums import ScenarioName, SubscriptionStatus

pytestmark = [pytest.mark.asyncio]


async def test_iterator_is_empty_if_there_are_no_data(dm):
    records = []
    async for chunk in dm.iter_subscriptions_versions_for_export(2):
        records.extend(chunk)

    assert len(records) == 0


async def test_returns_all_records(dm, factory):
    for i in range(10):
        await factory.create_subscription(biz_id=i)

    records = []
    async for chunk in dm.iter_subscriptions_versions_for_export(2):
        records.extend(chunk)

    assert len(records) == 10


@pytest.mark.parametrize("size", range(1, 6))
async def test_returns_records_by_passed_chunks(size, factory, dm):
    for i in range(10):
        await factory.create_subscription(biz_id=i)

    records = []
    async for chunk in dm.iter_subscriptions_versions_for_export(size):
        records.append(chunk)

    assert len(records[0]) == size
    assert len(records[1]) == size


async def test_returns_all_expected_columns(factory, dm):
    sub_id_1 = await factory.create_subscription(
        status=SubscriptionStatus.ACTIVE,
        scenario_name=ScenarioName.DISCOUNT_FOR_LOST,
        biz_id=123,
        coupon_id=456,
        created_at=dt("2020-01-01 11:11:11"),
    )
    sub_id_2 = await factory.create_subscription(
        status=SubscriptionStatus.COMPLETED,
        scenario_name=ScenarioName.ENGAGE_PROSPECTIVE,
        biz_id=987,
        coupon_id=654,
        created_at=dt("2020-02-02 22:22:22"),
    )

    records = []
    async for _recs in dm.iter_subscriptions_versions_for_export(2):
        records.extend(_recs)

    assert records == [
        dict(
            version_id=Any(int),
            subscription_id=sub_id_1,
            biz_id=123,
            scenario_code="DISCOUNT_FOR_LOST",
            coupon_id=456,
            status="ACTIVE",
            created_at=1577877071000000,
        ),
        dict(
            version_id=Any(int),
            subscription_id=sub_id_2,
            biz_id=987,
            scenario_code="ENGAGE_PROSPECTIVE",
            coupon_id=654,
            status="COMPLETED",
            created_at=1580682142000000,
        ),
    ]


async def test_returns_versions_ordered_by_created_dt(factory, dm):
    sub_id = await factory.create_subscription(
        created_at=dt("2021-01-01 11:11:11"),
    )
    await factory.create_subscription_version(
        subscription_id=sub_id, created_at=dt("2023-03-03 03:03:03")
    )
    await factory.create_subscription_version(
        subscription_id=sub_id, created_at=dt("2022-02-02 22:22:22")
    )

    records = []
    async for _recs in dm.iter_subscriptions_versions_for_export(2):
        records.extend(_recs)

    assert [record["created_at"] for record in records] == [
        1609499471000000,  # dt("2021-01-01 11:11:11")
        1643840542000000,  # dt("2022-02-02 22:22:22")
        1677812583000000,  # dt("2023-03-03 03:03:03")
    ]


async def test_does_not_returns_versions_without_status(factory, dm):
    subscription_id = await factory.create_subscription(
        status=SubscriptionStatus.ACTIVE
    )
    await factory.create_subscription_version(
        subscription_id=subscription_id, status=None
    )

    records = []
    async for _recs in dm.iter_subscriptions_versions_for_export(2):
        records.extend(_recs)

    assert records == [
        dict(
            version_id=Any(int),
            subscription_id=subscription_id,
            biz_id=123,
            scenario_code="DISCOUNT_FOR_LOST",
            coupon_id=456,
            status="ACTIVE",
            created_at=Any(int),
        )
    ]
