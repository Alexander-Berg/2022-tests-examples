import asyncio
import xmlrpc.client

import pytest

from maps_adv.billing_proxy.lib.core.balance_client import (
    BalanceApiError,
    BalanceClient,
)

pytestmark = [pytest.mark.asyncio]


async def test_calls_xmlrpc_api(xmlrpc_client):
    xmlrpc_client.request.coro.return_value = [[{"Uid": 111}, {"Uid": 222}]]
    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    result = await balance_client.list_client_passports(123)

    assert xmlrpc_client.request.call_args == (
        ("Balance.ListClientPassports", 22, 123),
    )

    assert result == [111, 222]


async def test_raises_if_xmlrpc_error(xmlrpc_client):
    xmlrpc_client.request.coro.side_effect = xmlrpc.client.Fault(1, "error")
    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.list_client_passports(123)
    assert isinstance(exc.value.__cause__, xmlrpc.client.Fault)


async def test_raises_if_xmlrpcexception(xmlrpc_client):
    xmlrpc_client.request.coro.side_effect = xmlrpc.client.ProtocolError(
        "localhost:80/path", 500, "Error", {}
    )
    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.list_client_passports(123)
    assert isinstance(exc.value.__cause__, xmlrpc.client.ProtocolError)


async def test_raises_on_http_timeout(xmlrpc_client):
    xmlrpc_client.request.coro.side_effect = asyncio.TimeoutError()
    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.list_client_passports(123)
    assert isinstance(exc.value.__cause__, asyncio.TimeoutError)
