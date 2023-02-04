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
                "CLIENT_ID": 111,
                "CLIENT_TYPE_ID": 0,
                "NAME": "Клиент 111",
                "EMAIL": "client111@yandex.ru",
                "PHONE": "111-111-111",
                "FAX": "0111-0111",
                "URL": "clients.ru/111",
                "IS_AGENCY": 0,
                "AGENCY_ID": 0,
            },
            {
                "CLIENT_ID": 222,
                "CLIENT_TYPE_ID": 0,
                "NAME": "Клиент 222",
                "EMAIL": "client222@yandex.ru",
                "PHONE": "222-222-222",
                "FAX": "0222-0222",
                "URL": "clients.ru/222",
                "IS_AGENCY": 1,
                "AGENCY_ID": 0,
            },
        ],
    ]


async def test_return_data(xmlrpc_client):
    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    result = await balance_client.find_clients([111, 222])

    assert result == [
        {
            "id": 111,
            "name": "Клиент 111",
            "email": "client111@yandex.ru",
            "phone": "111-111-111",
            "is_agency": False,
        },
        {
            "id": 222,
            "name": "Клиент 222",
            "email": "client222@yandex.ru",
            "phone": "222-222-222",
            "is_agency": True,
        },
    ]


async def test_xmlrpc_api_called(xmlrpc_client):
    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    await balance_client.find_clients([111, 222])

    xmlrpc_client.request.assert_called_with(
        "Balance.GetClientByIdBatch", ["111", "222"]
    )


async def test_returns_only_clients_found(xmlrpc_client):
    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    result = await balance_client.find_clients([111, 222])

    assert result == [
        {
            "id": 111,
            "name": "Клиент 111",
            "email": "client111@yandex.ru",
            "phone": "111-111-111",
            "is_agency": False,
        },
        {
            "id": 222,
            "name": "Клиент 222",
            "email": "client222@yandex.ru",
            "phone": "222-222-222",
            "is_agency": True,
        },
    ]


async def test_returns_empty_list_if_no_clients_found(xmlrpc_client):
    xmlrpc_client.request.coro.return_value = [0, []]

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    result = await balance_client.find_clients([111, 222])

    assert result == []


async def test_fetches_clients_in_chunks(xmlrpc_client):
    def _xmlrpc_call_mock_callable(_, cids):
        return [
            0,
            list(
                {
                    "CLIENT_ID": str(cid),
                    "CLIENT_TYPE_ID": 0,
                    "NAME": "Клиент {cid}".format(cid=cid),
                    "EMAIL": "client{cid}@yandex.ru".format(cid=cid),
                    "PHONE": "{cid}-{cid}-{cid}".format(cid=cid),
                    "FAX": "0{cid}-0{cid}".format(cid=cid),
                    "URL": "clients.ru/{cid}".format(cid=cid),
                    "IS_AGENCY": 0,
                    "AGENCY_ID": 0,
                }
                for cid in cids
            ),
        ]

    xmlrpc_client.request.coro.side_effect = _xmlrpc_call_mock_callable

    balance_client = BalanceClient(
        11, {11: "token"}, 22, xmlrpc_client, clients_fetch_chunk_size=2
    )
    result = await balance_client.find_clients([111, 222, 333, 444, 555])

    expected_result = []
    for i in range(1, 6):
        cl_id = str(i) * 3
        expected_result.append(
            {
                "id": int(cl_id),
                "name": f"Клиент {cl_id}",
                "email": f"client{cl_id}@yandex.ru",
                "phone": f"{cl_id}-{cl_id}-{cl_id}",
                "is_agency": False,
            }
        )
    assert result == expected_result
    assert xmlrpc_client.request.coro.call_args_list == [
        (("Balance.GetClientByIdBatch", ["111", "222"]),),
        (("Balance.GetClientByIdBatch", ["333", "444"]),),
        (("Balance.GetClientByIdBatch", ["555"]),),
    ]


async def test_raises_if_xmlrpc_error(xmlrpc_client):
    xmlrpc_client.request.coro.side_effect = xmlrpc.client.Fault(1, "error")

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.find_clients([55])

    assert isinstance(exc.value.__cause__, xmlrpc.client.Fault)


async def test_raises_if_bad_response_code(xmlrpc_client):
    xmlrpc_client.request.coro.return_value[0] = 1

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.find_clients([55])

    assert (
        str(exc.value) == "Balance.GetClientByIdBatch returned nonzero status code: 1"
    )


async def test_raises_if_xmlrpcexception(xmlrpc_client):
    xmlrpc_client.request.coro.side_effect = xmlrpc.client.ProtocolError(
        "localhost:80/path", 500, "Error", {}
    )

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.find_clients([55])

    assert isinstance(exc.value.__cause__, xmlrpc.client.ProtocolError)


async def test_raises_on_http_timeout(xmlrpc_client):
    xmlrpc_client.request.coro.side_effect = asyncio.TimeoutError()

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.find_clients([55])

    assert isinstance(exc.value.__cause__, asyncio.TimeoutError)
