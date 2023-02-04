import asyncio
import xmlrpc.client
from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.core.balance_client import (
    BalanceApiError,
    BalanceClient,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def xmlrpc_client_mock(xmlrpc_client):
    xmlrpc_client.request.coro.return_value = [
        {
            "UserPath": "http://user.balance.ru/path",
            "AdminPath": "http://admin.balance.ru/path",
            "RequestID": 322,
        }
    ]


@pytest.fixture
async def create_data(factory):
    order = await factory.create_order()
    return {
        "client_id": order["agency_id"],
        "order_id": order["id"],
        "amount": Decimal("123.456"),
        "region": "ru",
        "contract_id": order["contract_id"],
    }


async def test_return_data(xmlrpc_client, create_data):
    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    result = await balance_client.create_deposit_request(**create_data)

    assert result == {
        "user_url": "http://user.balance.ru/path",
        "admin_url": "http://admin.balance.ru/path",
        "request_id": 322,
    }


async def test_calls_xmlrpc_api(xmlrpc_client, create_data):
    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    await balance_client.create_deposit_request(**create_data)

    xmlrpc_client.request.assert_called_with(
        "Balance.CreateRequest2",
        22,
        str(create_data["client_id"]),
        [
            {
                "ServiceID": 11,
                "ServiceOrderID": str(create_data["order_id"]),
                "Qty": "123.456",
            }
        ],
        {
            "Region": create_data["region"],
            "InvoiceDesireContractID": str(create_data["contract_id"]),
        },
    )


async def calls_xmlrpc_api_with_custom_service_id(xmlrpc_client, create_data):
    create_data["service_id"] = 33

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    await balance_client.create_deposit_request(**create_data)

    xmlrpc_client.request.assert_called_with(
        "Balance.CreateRequest2",
        22,
        str(create_data["client_id"]),
        [
            {
                "ServiceID": 33,
                "ServiceOrderID": str(create_data["order_id"]),
                "Qty": "123.456",
            }
        ],
        {
            "Region": create_data["region"],
            "InvoiceDesireContractID": str(create_data["contract_id"]),
        },
    )


async def test_region_is_not_required(xmlrpc_client, create_data):
    del create_data["region"]

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    await balance_client.create_deposit_request(**create_data)

    xmlrpc_client.request.assert_called_with(
        "Balance.CreateRequest2",
        22,
        str(create_data["client_id"]),
        [
            {
                "ServiceID": 11,
                "ServiceOrderID": str(create_data["order_id"]),
                "Qty": "123.456",
            }
        ],
        {
            "InvoiceDesireContractID": str(create_data["contract_id"]),
        },
    )


async def test_raises_if_xmlrpc_error(xmlrpc_client, create_data):
    xmlrpc_client.request.coro.side_effect = xmlrpc.client.Fault(1, "error")

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.create_deposit_request(**create_data)

    assert isinstance(exc.value.__cause__, xmlrpc.client.Fault)


async def test_raises_if_xmlrpcexception(xmlrpc_client, create_data):
    xmlrpc_client.request.coro.side_effect = xmlrpc.client.ProtocolError(
        "localhost:80/path", 500, "Error", {}
    )

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.create_deposit_request(**create_data)

    assert isinstance(exc.value.__cause__, xmlrpc.client.ProtocolError)


async def test_raises_on_http_timeout(xmlrpc_client, create_data):
    xmlrpc_client.request.coro.side_effect = asyncio.TimeoutError()

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.create_deposit_request(**create_data)

    assert isinstance(exc.value.__cause__, asyncio.TimeoutError)
