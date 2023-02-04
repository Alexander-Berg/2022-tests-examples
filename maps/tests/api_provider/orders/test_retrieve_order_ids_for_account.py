import pytest

from maps_adv.manul.lib.api_providers import OrdersApiProvider
from maps_adv.manul.proto import orders_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def provider(orders_dm):
    return OrdersApiProvider(orders_dm)


async def test_returns_orders_data(orders_dm, provider):
    orders_dm.retrieve_order_ids_for_account.coro.return_value = [1, 3, 5]

    input_data = orders_pb2.AccountManagerIdFilter(
        account_manager_id=100500
    ).SerializeToString()

    raw_got = await provider.retrieve_order_ids_for_account(input_data)
    got_pb = orders_pb2.OrdersFilter.FromString(raw_got)

    assert got_pb == orders_pb2.OrdersFilter(ids=[1, 3, 5])


async def test_data_manager_called_ok_for_account_manager(orders_dm, provider):
    orders_dm.retrieve_order_ids_for_account.coro.return_value = []

    input_data = orders_pb2.AccountManagerIdFilter(
        account_manager_id=100500
    ).SerializeToString()
    await provider.retrieve_order_ids_for_account(input_data)

    orders_dm.retrieve_order_ids_for_account.assert_called_with(
        account_manager_id=100500
    )
