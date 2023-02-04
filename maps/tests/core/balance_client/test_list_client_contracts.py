import asyncio
import datetime
import xmlrpc.client

import pytest

from maps_adv.billing_proxy.lib.core.balance_client import (
    BalanceApiError,
    BalanceClient,
)
from maps_adv.billing_proxy.lib.db.enums import CurrencyType, PaymentType

pytestmark = [pytest.mark.asyncio]


def get_balance_contract_data(**kwargs):
    common_data = {
        "ID": 12345,
        "EXTERNAL_ID": "123321/56",
        "CURRENCY": "RUR",
        "SERVICES": [11],
        "IS_ACTIVE": 1,
        "DT": datetime.datetime(2000, 3, 1, 0, 0),
        "FINISH_DT": datetime.datetime(2001, 4, 2, 0, 0),
        "PAYMENT_TYPE": 2,
        "IS_DEACTIVATED": 0,
        "IS_CANCELLED": 0,
        "MANAGER_CODE": 20309,
        "IS_FAXED": 1,
        "IS_SIGNED": 1,
        "CONTRACT_TYPE": 61,
        "PERSON_ID": 5383346,
        "IS_SUSPENDED": 0,
    }
    common_data.update(kwargs)

    return common_data


async def test_return_data(xmlrpc_client):
    xmlrpc_client.request.coro.return_value = [[get_balance_contract_data()]]

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    result = await balance_client.list_client_contracts(22)

    assert result == [
        {
            "id": 12345,
            "external_id": "123321/56",
            "is_active": True,
            "date_start": datetime.date(2000, 3, 1),
            "date_end": datetime.date(2001, 4, 2),
            "payment_type": PaymentType.PRE,
            "currency": CurrencyType.RUB,
        }
    ]
    assert isinstance(result[0]["is_active"], bool)


async def test_xmlrpc_api_called(xmlrpc_client):
    xmlrpc_client.request.coro.return_value = [[get_balance_contract_data()]]

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    await balance_client.list_client_contracts(55)

    assert xmlrpc_client.request.call_args == (
        ("Balance.GetClientContracts", {"ClientID": 55}),
        {},
    )


async def test_not_returns_contract_not_for_this_service(xmlrpc_client):
    xmlrpc_client.request.coro.return_value = [
        [get_balance_contract_data(SERVICES=[88])]
    ]

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    result = await balance_client.list_client_contracts(55)

    assert result == []


async def test_not_returns_only_contracts_for_this_service(xmlrpc_client):
    contract_expected1 = get_balance_contract_data(ID=111, SERVICES=[11])
    contract_expected2 = get_balance_contract_data(ID=111888, SERVICES=[11, 88])
    contract_not_expected = get_balance_contract_data(ID=888, SERVICES=[88])
    xmlrpc_client.request.coro.return_value = [
        [contract_expected1, contract_expected2, contract_not_expected]
    ]

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    result = await balance_client.list_client_contracts(55)

    returned_contract_ids = list(c["id"] for c in result)
    assert returned_contract_ids == [111, 111888]


async def test_not_returns_contracts_with_no_services(xmlrpc_client):
    contract_data = get_balance_contract_data(ID=222)
    del contract_data["SERVICES"]
    xmlrpc_client.request.coro.return_value = [[contract_data]]

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    result = await balance_client.list_client_contracts(55)

    assert result == []


@pytest.mark.parametrize(
    ("balance_currency", "expected_currency"),
    [
        ("RUR", CurrencyType.RUB),
        ("TRY", CurrencyType.TRY),
        ("KZT", CurrencyType.KZT),
        ("BYN", CurrencyType.BYN),
        ("EUR", CurrencyType.EUR),
        ("USD", CurrencyType.USD),
    ],
)
async def test_currency_translation(xmlrpc_client, balance_currency, expected_currency):
    contract_data = get_balance_contract_data(CURRENCY=balance_currency)
    xmlrpc_client.request.coro.return_value = [[contract_data]]

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    result = await balance_client.list_client_contracts(22)

    assert result[0]["currency"] == expected_currency


async def test_not_returns_contract_with_unknown_currency(xmlrpc_client):
    contract_data = get_balance_contract_data(CURRENCY="GREEN_PAPER")
    xmlrpc_client.request.coro.return_value = [[contract_data]]

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    result = await balance_client.list_client_contracts(22)

    assert result == []


@pytest.mark.parametrize(
    ("balance_payment_type_code", "payment_type"),
    [(2, PaymentType.PRE), (3, PaymentType.POST)],
)
async def test_payment_type_translation(
    xmlrpc_client, balance_payment_type_code, payment_type
):
    contract_data = get_balance_contract_data(PAYMENT_TYPE=balance_payment_type_code)
    xmlrpc_client.request.coro.return_value = [[contract_data]]

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    result = await balance_client.list_client_contracts(22)

    assert result[0]["payment_type"] == payment_type


async def test_raises_if_xmlrpc_error(xmlrpc_client):
    xmlrpc_client.request.coro.side_effect = xmlrpc.client.Fault(1, "error")

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.list_client_contracts(55)

    assert isinstance(exc.value.__cause__, xmlrpc.client.Fault)


async def test_raises_if_xmlrpcexception(xmlrpc_client):
    xmlrpc_client.request.coro.side_effect = xmlrpc.client.ProtocolError(
        "localhost:80/path", 500, "Error", {}
    )

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.list_client_contracts(55)

    assert isinstance(exc.value.__cause__, xmlrpc.client.ProtocolError)


async def test_raises_on_http_timeout(xmlrpc_client):
    xmlrpc_client.request.coro.side_effect = asyncio.TimeoutError()

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.list_client_contracts(55)

    assert isinstance(exc.value.__cause__, asyncio.TimeoutError)
