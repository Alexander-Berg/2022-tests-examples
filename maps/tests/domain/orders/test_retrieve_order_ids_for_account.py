import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(orders_domain, orders_dm):
    orders_dm.retrieve_order_ids_for_account.coro.return_value = [1, 2]

    result = await orders_domain.retrieve_order_ids_for_account(
        account_manager_id=100500
    )

    orders_dm.retrieve_order_ids_for_account.assert_called_with(100500)

    assert result == [1, 2]
