from datetime import date
from unittest import mock
import pytest

from maps_adv.billing_proxy.lib.core.balance_client import BalanceApiError
from maps_adv.billing_proxy.lib.db.enums import CurrencyType, PaymentType

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture(autouse=True)
def common_balance_client_mocks(balance_client):
    balance_client.find_clients.coro.return_value = [
        {
            "id": 22,
            "name": "Имя 122",
            "email": "122@ya.ru",
            "phone": "222-222",
            "is_agency": False,
        },
        {
            "id": 33,
            "name": "Имя 133",
            "email": "133@ya.ru",
            "phone": "333-333",
            "is_agency": True,
        },
    ]
    balance_client.list_client_contracts.coro.return_value = [
        {
            "id": 111,
            "external_id": "1111/12",
            "currency": CurrencyType.RUB,
            "is_active": True,
            "date_start": date(2012, 2, 2),
            "date_end": date(2012, 3, 3),
            "payment_type": PaymentType.POST,
        },
        {
            "id": 222,
            "external_id": "2222/12",
            "currency": CurrencyType.USD,
            "is_active": False,
            "date_start": date(2012, 4, 4),
            "date_end": None,
            "payment_type": PaymentType.PRE,
        },
    ]
    balance_client.list_client_passports.coro.return_value = [123, 321]


async def test_uses_balance_client(clients_domain, clients_dm, balance_client):
    clients_dm.list_client_ids.coro.return_value = [22, 33]
    balance_client.find_clients.coro.return_value = [
        {
            "id": 22,
            "name": "Имя 122",
            "email": "122@ya.ru",
            "phone": "222-222",
            "is_agency": False,
        },
        {
            "id": 33,
            "name": "Имя 133",
            "email": "133@ya.ru",
            "phone": "333-333",
            "is_agency": True,
        },
    ]

    await clients_domain.sync_clients_data_and_contracts_with_balance()

    balance_client.find_clients.assert_called_with([22, 33])
    assert sorted(balance_client.list_client_contracts.call_args_list) == [
        mock.call(22),
        mock.call(33),
    ]
    assert sorted(balance_client.list_client_passports.call_args_list) == [
        mock.call(22),
        mock.call(33),
    ]
    assert clients_dm.upsert_client.mock_calls == [
        mock.call(
            id=22,
            name="Имя 122",
            email="122@ya.ru",
            phone="222-222",
            is_agency=False,
            con=None,
        ),
        mock.call(
            id=33,
            name="Имя 133",
            email="133@ya.ru",
            phone="333-333",
            is_agency=True,
            con=None,
        ),
    ]
    assert clients_dm.sync_client_contracts.mock_calls == [
        mock.call(
            22,
            [
                {
                    "id": 111,
                    "external_id": "1111/12",
                    "currency": CurrencyType.RUB,
                    "is_active": True,
                    "date_start": date(2012, 2, 2),
                    "date_end": date(2012, 3, 3),
                    "payment_type": PaymentType.POST,
                },
                {
                    "id": 222,
                    "external_id": "2222/12",
                    "currency": CurrencyType.USD,
                    "is_active": False,
                    "date_start": date(2012, 4, 4),
                    "date_end": None,
                    "payment_type": PaymentType.PRE,
                },
            ],
        ),
        mock.call(
            33,
            [
                {
                    "id": 111,
                    "external_id": "1111/12",
                    "currency": CurrencyType.RUB,
                    "is_active": True,
                    "date_start": date(2012, 2, 2),
                    "date_end": date(2012, 3, 3),
                    "payment_type": PaymentType.POST,
                },
                {
                    "id": 222,
                    "external_id": "2222/12",
                    "currency": CurrencyType.USD,
                    "is_active": False,
                    "date_start": date(2012, 4, 4),
                    "date_end": None,
                    "payment_type": PaymentType.PRE,
                },
            ],
        ),
    ]


async def test_calls_contracts_update_if_client_data_update_fails(
    clients_domain, clients_dm, balance_client
):
    clients_dm.list_client_ids.coro.return_value = [22]
    balance_client.find_clients.coro.side_effect = BalanceApiError()

    await clients_domain.sync_clients_data_and_contracts_with_balance()

    clients_dm.sync_client_contracts.assert_called_with(
        22,
        [
            {
                "id": 111,
                "external_id": "1111/12",
                "currency": CurrencyType.RUB,
                "is_active": True,
                "date_start": date(2012, 2, 2),
                "date_end": date(2012, 3, 3),
                "payment_type": PaymentType.POST,
            },
            {
                "id": 222,
                "external_id": "2222/12",
                "currency": CurrencyType.USD,
                "is_active": False,
                "date_start": date(2012, 4, 4),
                "date_end": None,
                "payment_type": PaymentType.PRE,
            },
        ],
    )


async def test_continues_on_balance_api_error(
    clients_domain, clients_dm, balance_client
):
    clients_dm.list_client_ids.coro.return_value = [22, 33, 44]

    def _list_client_contracts(client_id):
        if client_id == 22:
            return [
                {
                    "id": 111,
                    "external_id": "1111/12",
                    "currency": CurrencyType.RUB,
                    "is_active": True,
                    "date_start": date(2012, 2, 2),
                    "date_end": date(2012, 3, 3),
                    "payment_type": PaymentType.POST,
                }
            ]
        elif client_id == 33:
            raise BalanceApiError()
        elif client_id == 44:
            return [
                {
                    "id": 222,
                    "external_id": "2222/12",
                    "currency": CurrencyType.RUB,
                    "is_active": True,
                    "date_start": date(2012, 2, 2),
                    "date_end": date(2012, 3, 3),
                    "payment_type": PaymentType.POST,
                },
                {
                    "id": 333,
                    "external_id": "3333/12",
                    "currency": CurrencyType.RUB,
                    "is_active": True,
                    "date_start": date(2012, 2, 2),
                    "date_end": date(2012, 3, 3),
                    "payment_type": PaymentType.POST,
                },
            ]

    balance_client.list_client_contracts.coro.side_effect = _list_client_contracts

    await clients_domain.sync_clients_data_and_contracts_with_balance()

    assert balance_client.list_client_contracts.call_count == 3
    assert clients_dm.sync_client_contracts.mock_calls == [
        mock.call(
            22,
            [
                {
                    "id": 111,
                    "external_id": "1111/12",
                    "currency": CurrencyType.RUB,
                    "is_active": True,
                    "date_start": date(2012, 2, 2),
                    "date_end": date(2012, 3, 3),
                    "payment_type": PaymentType.POST,
                }
            ],
        ),
        mock.call(
            44,
            [
                {
                    "id": 222,
                    "external_id": "2222/12",
                    "currency": CurrencyType.RUB,
                    "is_active": True,
                    "date_start": date(2012, 2, 2),
                    "date_end": date(2012, 3, 3),
                    "payment_type": PaymentType.POST,
                },
                {
                    "id": 333,
                    "external_id": "3333/12",
                    "currency": CurrencyType.RUB,
                    "is_active": True,
                    "date_start": date(2012, 2, 2),
                    "date_end": date(2012, 3, 3),
                    "payment_type": PaymentType.POST,
                },
            ],
        ),
    ]
