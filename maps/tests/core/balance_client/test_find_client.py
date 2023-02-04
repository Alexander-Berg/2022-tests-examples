import asyncio
import xmlrpc.client

import pytest

from maps_adv.billing_proxy.lib.core.balance_client import (
    BalanceApiError,
    BalanceClient,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def xmlrpc_client_mock(xmlrpc_client):
    xmlrpc_client.request.coro.return_value = [
        0,
        [
            {
                "IS_AGENCY": 0,
                "SINGLE_ACCOUNT_NUMBER": "",
                "CLIENT_ID": 22,
                "PHONE": "322-223",
                "URL": "www.example.com",
                "NAME": "Имя клиента",
                "EMAIL": "email@example.com",
                "CLIENT_TYPE_ID": 2,
                "CITY": "",
                "REGION_ID": 225,
                "SERVICE_DATA": {},
                "FAX": "",
                "AGENCY_ID": 0,
            }
        ],
    ]


async def test_return_data(xmlrpc_client):
    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    result = await balance_client.find_client(22)

    assert result == {
        "id": 22,
        "name": "Имя клиента",
        "email": "email@example.com",
        "phone": "322-223",
        "is_agency": False,
        "partner_agency_id": 0,
    }


async def test_xmlrpc_api_called(xmlrpc_client):
    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    await balance_client.find_client(55)

    xmlrpc_client.request.assert_called_with("Balance.GetClientByIdBatch", ["55"])


async def test_returns_none_if_client_not_found(xmlrpc_client):
    xmlrpc_client.request.coro.return_value = [0, []]

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    result = await balance_client.find_client(55)

    assert result is None


async def test_raises_if_xmlrpc_error(xmlrpc_client):
    xmlrpc_client.request.coro.side_effect = xmlrpc.client.Fault(1, "error")

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.find_client(55)

    assert isinstance(exc.value.__cause__, xmlrpc.client.Fault)


async def test_raises_if_bad_response_code(xmlrpc_client):
    xmlrpc_client.request.coro.return_value[0] = 1

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.find_client(55)

    assert (
        str(exc.value) == "Balance.GetClientByIdBatch returned nonzero status code: 1"
    )


async def test_raises_if_xmlrpcexception(xmlrpc_client):
    xmlrpc_client.request.coro.side_effect = xmlrpc.client.ProtocolError(
        "localhost:80/path", 500, "Error", {}
    )

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.find_client(55)

    assert isinstance(exc.value.__cause__, xmlrpc.client.ProtocolError)


async def test_raises_if_multiple_clients_returned(xmlrpc_client):
    xmlrpc_client.request.coro.return_value = [
        0,
        [
            {
                "IS_AGENCY": 0,
                "SINGLE_ACCOUNT_NUMBER": "",
                "CLIENT_ID": 22,
                "PHONE": "322-223",
                "URL": "www.example.com",
                "NAME": "Имя клиента",
                "EMAIL": "email@example.com",
                "CLIENT_TYPE_ID": 2,
                "CITY": "",
                "REGION_ID": 225,
                "SERVICE_DATA": {},
                "FAX": "",
                "AGENCY_ID": 0,
            },
            {
                "IS_AGENCY": 0,
                "SINGLE_ACCOUNT_NUMBER": "",
                "CLIENT_ID": 23,
                "PHONE": "322-223",
                "URL": "www.example.com",
                "NAME": "Имя клиента",
                "EMAIL": "email@example.com",
                "CLIENT_TYPE_ID": 2,
                "CITY": "",
                "REGION_ID": 225,
                "SERVICE_DATA": {},
                "FAX": "",
                "AGENCY_ID": 0,
            },
        ],
    ]

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.find_client(55)

    assert str(exc.value) == "Balance.GetClientByIdBatch returned multiple clients (2)"


async def test_raises_on_http_timeout(xmlrpc_client):
    xmlrpc_client.request.coro.side_effect = asyncio.TimeoutError()

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.find_client(55)

    assert isinstance(exc.value.__cause__, asyncio.TimeoutError)
