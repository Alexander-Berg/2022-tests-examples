import pytest

from maps_adv.geosmb.scenarist.server.lib.enums import SubscriptionStatus

pytestmark = [pytest.mark.asyncio]


async def test_iterator_is_empty_if_there_are_no_data(dm):
    records = []
    async for chunk in dm.iter_subscriptions_for_export(2):
        records.extend(chunk)

    assert len(records) == 0


async def test_returns_all_records(dm, factory):
    for i in range(10):
        await factory.create_subscription(biz_id=i)

    records = []
    async for chunk in dm.iter_subscriptions_for_export(2):
        records.extend(chunk)

    assert len(records) == 10


@pytest.mark.parametrize("size", range(1, 6))
async def test_returns_records_by_passed_chunks(size, factory, dm):
    for i in range(10):
        await factory.create_subscription(biz_id=i)

    records = []
    async for chunk in dm.iter_subscriptions_for_export(size):
        records.append(chunk)

    assert len(records[0]) == size
    assert len(records[1]) == size


async def test_returns_all_expected_columns(factory, dm):
    subscription_id = await factory.create_subscription()

    records = []
    async for _recs in dm.iter_subscriptions_for_export(2):
        records.extend(_recs)

    assert records[0] == dict(
        subscription_id=subscription_id,
        scenario_code="DISCOUNT_FOR_LOST",
        segments=["LOST"],
        biz_id=123,
        coupon_id=456,
    )


@pytest.mark.parametrize(
    "status", (SubscriptionStatus.COMPLETED, SubscriptionStatus.PAUSED)
)
async def test_does_not_returns_inactive_subscriptions(status, factory, dm):
    await factory.create_subscription(status=status)

    records = []
    async for _recs in dm.iter_subscriptions_for_export(2):
        records.extend(_recs)

    assert len(records) == 0
