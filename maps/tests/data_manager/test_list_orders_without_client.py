import pytest
from smb.common.testing_utils import dt

pytestmark = [pytest.mark.asyncio]


@pytest.mark.freeze_time(dt("2020-01-01 01:00:00"))
async def test_returns_order_details(factory, dm):
    order_id = await factory.create_order(
        client_id=None, created_at=dt("2020-01-01 00:59:00")
    )

    got = await dm.list_orders_without_client(order_min_age_sec=60)

    assert got == [
        dict(
            id=order_id,
            biz_id=888,
            customer_name="Иван Петров",
            customer_phone="+7 (000) 000-00-00",
            customer_passport_uid=7654332,
            created_at=dt("2020-01-01 00:59:00"),
        )
    ]


@pytest.mark.freeze_time(dt("2020-01-01 01:00:00"))
@pytest.mark.parametrize(
    "order_params",
    [
        # missed biz_id
        dict(client_id=None, biz_id=None),
        dict(client_id=11, biz_id=None),
        # client_id exists
        dict(client_id=11, biz_id=123),
        # order was created just recently
        dict(client_id=None, biz_id=123, created_at=dt("2020-01-01 00:59:01")),
    ],
)
async def test_returns_nothing_if_no_matched_orders(factory, dm, order_params):
    await factory.create_order(**order_params)

    got = await dm.list_orders_without_client(order_min_age_sec=60)

    assert got == []


async def test_returns_orders_sorted_by_creation_time(factory, dm):
    order_id_1 = await factory.create_order(
        client_id=None, created_at=dt("2020-01-02 00:00:00"), yang_suite_id=None
    )
    order_id_2 = await factory.create_order(
        client_id=None, created_at=dt("2020-01-01 00:00:00"), yang_suite_id=None
    )
    order_id_3 = await factory.create_order(
        client_id=None, created_at=dt("2020-01-03 00:00:00"), yang_suite_id=None
    )

    got = await dm.list_orders_without_client(order_min_age_sec=60)

    assert [order["id"] for order in got] == [order_id_2, order_id_1, order_id_3]
