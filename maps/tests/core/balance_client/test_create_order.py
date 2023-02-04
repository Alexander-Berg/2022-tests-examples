import asyncio
import xmlrpc.client

import pytest

from maps_adv.billing_proxy.lib.core.balance_client import (
    BalanceApiError,
    BalanceClient,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def create_data():
    return {
        "order_id": 22,
        "client_id": 33,
        "agency_id": 44,
        "oracle_product_id": 55,
        "contract_id": 66,
        "act_text": "Текст акта",
        "text": "Текст заказа",
    }


async def test_return_data(xmlrpc_client, create_data):
    xmlrpc_client.request.coro.return_value = [[[0, "Success"]]]

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    assert await balance_client.create_order(*create_data) is None


async def test_calls_xmlrpc_api(xmlrpc_client, create_data):
    xmlrpc_client.request.coro.return_value = [[[0, "Success"]]]

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    await balance_client.create_order(**create_data)

    xmlrpc_client.request.assert_called_with(
        "Balance.CreateOrUpdateOrdersBatch",
        22,
        [
            {
                "ClientID": str(create_data["client_id"]),
                "ServiceID": 11,
                "ProductID": create_data["oracle_product_id"],
                "ServiceOrderID": str(create_data["order_id"]),
                "ActText": create_data["act_text"],
                "Text": create_data["text"],
                "AgencyID": str(create_data["agency_id"]),
                "ContractID": str(create_data["contract_id"]),
            }
        ],
        "token",
    )


async def test_not_includes_agency_when_it_is_none(xmlrpc_client, create_data):
    xmlrpc_client.request.coro.return_value = [[[0, "Success"]]]

    del create_data["agency_id"]

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    await balance_client.create_order(**create_data)

    xmlrpc_client.request.assert_called_with(
        "Balance.CreateOrUpdateOrdersBatch",
        22,
        [
            {
                "ClientID": str(create_data["client_id"]),
                "ServiceID": 11,
                "ProductID": create_data["oracle_product_id"],
                "ServiceOrderID": str(create_data["order_id"]),
                "ActText": create_data["act_text"],
                "Text": create_data["text"],
                "ContractID": str(create_data["contract_id"]),
            }
        ],
        "token",
    )


async def test_not_includes_contract_when_it_is_none(xmlrpc_client, create_data):
    xmlrpc_client.request.coro.return_value = [[[0, "Success"]]]

    create_data["contract_id"] = None

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    await balance_client.create_order(**create_data)

    xmlrpc_client.request.assert_called_with(
        "Balance.CreateOrUpdateOrdersBatch",
        22,
        [
            {
                "ClientID": str(create_data["client_id"]),
                "ServiceID": 11,
                "ProductID": create_data["oracle_product_id"],
                "ServiceOrderID": str(create_data["order_id"]),
                "ActText": create_data["act_text"],
                "Text": create_data["text"],
                "AgencyID": str(create_data["agency_id"]),
            }
        ],
        "token",
    )


async def test_not_includes_text_when_it_is_empty(xmlrpc_client, create_data):
    xmlrpc_client.request.coro.return_value = [[[0, "Success"]]]

    expected_order = {
        "ClientID": str(create_data["client_id"]),
        "ServiceID": 11,
        "ProductID": create_data["oracle_product_id"],
        "ServiceOrderID": str(create_data["order_id"]),
        "ActText": create_data["act_text"],
        "AgencyID": str(create_data["agency_id"]),
        "ContractID": str(create_data["contract_id"]),
    }

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)

    create_data["text"] = None
    await balance_client.create_order(**create_data)
    xmlrpc_client.request.assert_called_with(
        "Balance.CreateOrUpdateOrdersBatch",
        22,
        [expected_order],
        "token",
    )

    create_data["text"] = ""
    await balance_client.create_order(**create_data)
    xmlrpc_client.request.assert_called_with(
        "Balance.CreateOrUpdateOrdersBatch",
        22,
        [expected_order],
        "token",
    )


async def test_raises_if_xmlrpc_error(xmlrpc_client, create_data):
    xmlrpc_client.request.coro.side_effect = xmlrpc.client.Fault(1, "error")

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.create_order(**create_data)

    assert isinstance(exc.value.__cause__, xmlrpc.client.Fault)


async def test_raises_if_bad_response_code(xmlrpc_client, create_data):
    xmlrpc_client.request.coro.return_value = [[[1, "Some error"]]]

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.create_order(**create_data)

    assert str(exc.value) == (
        "Balance.CreateOrUpdateOrdersBatch "
        "returned nonzero status code: 1 (Some error)"
    )


async def test_raises_if_xmlrpcexception(xmlrpc_client, create_data):
    xmlrpc_client.request.coro.side_effect = xmlrpc.client.ProtocolError(
        "localhost:80/path", 500, "Error", {}
    )

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.create_order(**create_data)

    assert isinstance(exc.value.__cause__, xmlrpc.client.ProtocolError)


async def test_raises_on_http_timeout(xmlrpc_client, create_data):
    xmlrpc_client.request.coro.side_effect = asyncio.TimeoutError()

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.create_order(**create_data)

    assert isinstance(exc.value.__cause__, asyncio.TimeoutError)
