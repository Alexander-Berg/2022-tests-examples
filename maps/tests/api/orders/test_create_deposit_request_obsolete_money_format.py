from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.core.balance_client import BalanceApiError
from maps_adv.billing_proxy.proto import common_pb2, orders_pb2

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("db")]

API_URL = "/orders/{}/deposit/"


@pytest.fixture(autouse=True)
def common_balance_client_mocks(balance_client):
    balance_client.create_deposit_request.coro.return_value = {
        "user_url": "http://user-balance.example.com/url",
        "admin_url": "http://admin-balance.example.com/url",
        "request_id": 999888,
    }


async def test_return_data(api, factory):
    order = await factory.create_order()

    input_bp = orders_pb2.OrderDepositRequestCreationInput(
        OBSOLETE__amount=common_pb2.MoneyQuantity(value=1234560), region="ru"
    )
    result = await api.post(
        API_URL.format(order["id"]),
        input_bp,
        decode_as=orders_pb2.OrderDepositRequest,
        allowed_status_codes=[201],
    )

    assert result == orders_pb2.OrderDepositRequest(
        admin_url="http://admin-balance.example.com/url",
        user_url="http://user-balance.example.com/url",
        request_id=999888,
    )


@pytest.mark.parametrize(["service_id"], [(37,), (110,)])
async def test_calls_balance_api(api, factory, balance_client, service_id):
    order = await factory.create_order(service_id=service_id)

    input_bp = orders_pb2.OrderDepositRequestCreationInput(
        OBSOLETE__amount=common_pb2.MoneyQuantity(value=1234560), region="ru"
    )
    await api.post(
        API_URL.format(order["id"]),
        input_bp,
        decode_as=orders_pb2.OrderDepositRequest,
        allowed_status_codes=[201],
    )

    assert balance_client.create_deposit_request.call_args[1] == {
        "client_id": order["agency_id"],
        "order_id": order["external_id"],
        "amount": Decimal("123.456"),
        "region": "ru",
        "service_id": service_id,
        "contract_id": order["contract_id"],
    }


async def test_uses_agency_id_for_balance_if_agency_order(
    api, factory, balance_client, agency, client
):
    order = await factory.create_order(client_id=client["id"], agency_id=agency["id"])

    input_bp = orders_pb2.OrderDepositRequestCreationInput(
        OBSOLETE__amount=common_pb2.MoneyQuantity(value=1234560), region="ru"
    )
    await api.post(
        API_URL.format(order["id"]),
        input_bp,
        decode_as=orders_pb2.OrderDepositRequest,
        allowed_status_codes=[201],
    )

    assert (
        balance_client.create_deposit_request.call_args[1]["client_id"]
        == order["agency_id"]
    )


async def test_uses_client_id_for_balance_if_internal_order(
    api, factory, balance_client, client
):
    order = await factory.create_order(client_id=client["id"], agency_id=None)

    input_bp = orders_pb2.OrderDepositRequestCreationInput(
        OBSOLETE__amount=common_pb2.MoneyQuantity(value=1234560), region="ru"
    )
    await api.post(
        API_URL.format(order["id"]),
        input_bp,
        decode_as=orders_pb2.OrderDepositRequest,
        allowed_status_codes=[201],
    )

    assert (
        balance_client.create_deposit_request.call_args[1]["client_id"]
        == order["client_id"]
    )


async def test_returns_error_for_inexistent_order(api, factory):
    input_bp = orders_pb2.OrderDepositRequestCreationInput(
        OBSOLETE__amount=common_pb2.MoneyQuantity(value=1234560), region="ru"
    )
    await api.post(
        API_URL.format(555),
        input_bp,
        expected_error=(common_pb2.Error.ORDER_DOES_NOT_EXIST, "order_id=555"),
        allowed_status_codes=[422],
    )


async def test_raises_if_balance_fails(api, factory, balance_client):
    order = await factory.create_order()
    balance_client.create_deposit_request.coro.side_effect = BalanceApiError()

    input_bp = orders_pb2.OrderDepositRequestCreationInput(
        OBSOLETE__amount=common_pb2.MoneyQuantity(value=1234560), region="ru"
    )
    await api.post(
        API_URL.format(order["id"]),
        input_bp,
        expected_error=(common_pb2.Error.BALANCE_API_ERROR, "Balance API error"),
        allowed_status_codes=[503],
    )
