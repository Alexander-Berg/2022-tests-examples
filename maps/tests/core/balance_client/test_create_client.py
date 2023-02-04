import asyncio
import xmlrpc.client

import pytest

from maps_adv.billing_proxy.lib.core.balance_client import (
    BalanceApiError,
    BalanceClient,
)

pytestmark = [pytest.mark.asyncio]


async def test_return_data(xmlrpc_client):
    xmlrpc_client.request.coro.return_value = [0, "", 55]

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    created_id = await balance_client.create_client(
        "Имя клиента", "email@example.com", "8(499)123-45-67"
    )

    assert created_id == 55


async def test_calls_xmlrpc_api(xmlrpc_client):
    xmlrpc_client.request.coro.return_value = [0, "", 55]

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    await balance_client.create_client(
        "Имя клиента", "email@example.com", "8(499)123-45-67"
    )

    assert xmlrpc_client.request.call_args == (
        (
            "Balance.CreateClient",
            22,
            {
                "NAME": "Имя клиента",
                "EMAIL": "email@example.com",
                "PHONE": "8(499)123-45-67",
                "IS_AGENCY": False,
                "SERVICE_ID": 11,
            },
        ),
        {},
    )


async def test_raises_if_xmlrpc_error(xmlrpc_client):
    xmlrpc_client.request.coro.side_effect = xmlrpc.client.Fault(1, "error")

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.create_client(
            "Имя клиента", "email@example.com", "8(499)123-45-67"
        )

    assert isinstance(exc.value.__cause__, xmlrpc.client.Fault)


async def test_raises_if_bad_response_code(xmlrpc_client):
    xmlrpc_client.request.coro.return_value = [1, "Some error", 55]

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.create_client(
            "Имя клиента", "email@example.com", "8(499)123-45-67"
        )

    assert (
        str(exc.value)
        == "Balance.CreateClient returned nonzero status code: 1 (Some error)"
    )


async def test_raises_if_xmlrpcexception(xmlrpc_client):
    xmlrpc_client.request.coro.side_effect = xmlrpc.client.ProtocolError(
        "localhost:80/path", 500, "Error", {}
    )

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.find_client(22)

    assert isinstance(exc.value.__cause__, xmlrpc.client.ProtocolError)


async def test_raises_on_http_timeout(xmlrpc_client):
    xmlrpc_client.request.coro.side_effect = asyncio.TimeoutError()

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.find_client(22)

    assert isinstance(exc.value.__cause__, asyncio.TimeoutError)
