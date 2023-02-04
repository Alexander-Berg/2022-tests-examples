import asyncio
import xmlrpc.client
from datetime import datetime, timezone
from decimal import Decimal

import pytest

from maps_adv.billing_proxy.lib.core.balance_client import (
    BalanceApiError,
    BalanceClient,
)

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.freeze_time(datetime(2000, 2, 3, 12, 34, 56)),
]


async def test_xmlrpc_api_called(xmlrpc_client):
    xmlrpc_client.request.coro.return_value = ({"11": {"1": 1, "12": 0, "33": 0}},)

    xmlrpc_client = xmlrpc_client
    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    await balance_client.update_orders(
        {
            1: {"consumed": Decimal("100"), "service_id": 37},
            12: {"consumed": Decimal("50.1234"), "service_id": 110},
            33: {"consumed": Decimal("333")},
        },
        datetime(2000, 2, 3, 18, 25, 36, tzinfo=timezone.utc),
    )

    xmlrpc_client.request.assert_called_with(
        "Balance.UpdateCampaigns",
        [
            {
                "ServiceID": 37,
                "ServiceOrderID": "1",
                "Bucks": "100",
                "dt": "20000203212536",
                "Money": 0,
                "stop": 0,
                "Days": -1,
            },
            {
                "ServiceID": 110,
                "ServiceOrderID": "12",
                "Bucks": "50.1234",
                "dt": "20000203212536",
                "Money": 0,
                "stop": 0,
                "Days": -1,
            },
            {
                "ServiceID": 11,
                "ServiceOrderID": "33",
                "Bucks": "333",
                "dt": "20000203212536",
                "Money": 0,
                "stop": 0,
                "Days": -1,
            },
        ],
    )


async def test_returns_values_got_from_balance(xmlrpc_client):
    xmlrpc_client.request.coro.return_value = [
        {"37": {"1": 1, "2": 0}},
        {"110": {"12": 0, "22": 1}},
    ]

    xmlrpc_client = xmlrpc_client
    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    result = await balance_client.update_orders(
        {
            1: {"consumed": Decimal("100"), "service_id": 37},
            2: {"consumed": Decimal("200"), "service_id": 37},
            12: {"consumed": Decimal("500"), "service_id": 110},
            22: {"consumed": Decimal("550"), "service_id": 110},
        },
        datetime(2000, 2, 3, 18, 25, 36, tzinfo=timezone.utc),
    )

    assert result == {37: {1: True, 2: False}, 110: {12: False, 22: True}}


async def test_returns_empty_dict_for_empty_query(xmlrpc_client):
    xmlrpc_client.request.coro.return_value = [{}]

    xmlrpc_client = xmlrpc_client
    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    result = await balance_client.update_orders(
        {}, datetime(2000, 2, 3, 18, 25, 36, tzinfo=timezone.utc)
    )

    assert result == {}


async def test_raises_if_xmlrpc_error(xmlrpc_client):
    xmlrpc_client.request.coro.side_effect = xmlrpc.client.Fault(1, "error")

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.update_orders(
            {
                1: {"consumed": Decimal("100"), "service_id": 37},
                12: {"consumed": Decimal("50.1234"), "service_id": 110},
            },
            datetime(2000, 2, 3, 18, 25, 36, tzinfo=timezone.utc),
        )

    assert isinstance(exc.value.__cause__, xmlrpc.client.Fault)


async def test_raises_if_xmlrpcexception(xmlrpc_client):
    xmlrpc_client.request.coro.side_effect = xmlrpc.client.ProtocolError(
        "localhost:80/path", 500, "Error", {}
    )

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.update_orders(
            {
                1: {"consumed": Decimal("100"), "service_id": 37},
                12: {"consumed": Decimal("50.1234"), "service_id": 110},
            },
            datetime(2000, 2, 3, 18, 25, 36, tzinfo=timezone.utc),
        )

    assert isinstance(exc.value.__cause__, xmlrpc.client.ProtocolError)


async def test_raises_on_http_timeout(xmlrpc_client):
    xmlrpc_client.request.coro.side_effect = asyncio.TimeoutError()

    balance_client = BalanceClient(11, {11: "token"}, 22, xmlrpc_client)
    with pytest.raises(BalanceApiError) as exc:
        await balance_client.update_orders(
            {
                1: {"consumed": Decimal("100"), "service_id": 37},
                12: {"consumed": Decimal("50.1234"), "service_id": 110},
            },
            datetime(2000, 2, 3, 18, 25, 36, tzinfo=timezone.utc),
        )

    assert isinstance(exc.value.__cause__, asyncio.TimeoutError)
