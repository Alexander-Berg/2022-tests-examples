from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.core.balance_client import BalanceApiError
from maps_adv.billing_proxy.lib.domain.exceptions import OrderDoesNotExist

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture(autouse=True)
def common_orders_dm_mocks(orders_dm):
    orders_dm.find_order.coro.return_value = {
        "id": 5,
        "external_id": 6,
        "client_id": 11,
        "agency_id": 333,
        "service_id": 37,
        "contract_id": 555,
    }


@pytest.fixture(autouse=True)
def common_balance_client_mocks(balance_client):
    balance_client.create_deposit_request.coro.return_value = {
        "user_url": "http://user-balance.example.com/url",
        "admin_url": "http://admin-balance.example.com/url",
        "request_id": 999888,
    }


async def test_uses_dm(orders_domain, orders_dm):
    await orders_domain.create_deposit_request(5, Decimal("123.456"), "ru")

    orders_dm.find_order.assert_called_with(5)


async def test_uses_balance_client(orders_domain, balance_client):
    await orders_domain.create_deposit_request(5, Decimal("123.456"), "ru")

    balance_client.create_deposit_request.assert_called_with(
        client_id=333,
        order_id=6,
        amount=Decimal("123.456"),
        region="ru",
        service_id=37,
        contract_id=555,
    )


async def test_return_data(orders_domain):
    result = await orders_domain.create_deposit_request(5, Decimal("123.456"), "ru")

    assert result == {
        "user_url": "http://user-balance.example.com/url",
        "admin_url": "http://admin-balance.example.com/url",
        "request_id": 999888,
    }


async def test_raises_for_inexistent_orders(orders_domain, orders_dm, balance_client):
    orders_dm.find_order.coro.return_value = None

    with pytest.raises(OrderDoesNotExist) as exc:
        await orders_domain.create_deposit_request(5, Decimal("123.456"), "ru")

    assert exc.value.order_id == 5
    assert not balance_client.create_deposit_request.called


async def test_propagates_balance_api_error(orders_domain, balance_client):
    balance_client.create_deposit_request.coro.side_effect = BalanceApiError()

    with pytest.raises(BalanceApiError):
        await orders_domain.create_deposit_request(5, Decimal("123.456"), "ru")


async def test_uses_negative_one_contract_id_if_none(
    orders_dm, orders_domain, balance_client
):
    orders_dm.find_order.coro.return_value = {
        "id": 5,
        "external_id": 6,
        "client_id": 11,
        "agency_id": 333,
        "service_id": 37,
        "contract_id": None,
    }
    await orders_domain.create_deposit_request(5, Decimal("123.456"), "ru")

    balance_client.create_deposit_request.assert_called_with(
        client_id=333,
        order_id=6,
        amount=Decimal("123.456"),
        region="ru",
        service_id=37,
        contract_id=-1,
    )
