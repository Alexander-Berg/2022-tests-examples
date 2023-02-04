from datetime import date, datetime, timezone

import pytest

from maps_adv.billing_proxy.lib.db.enums import CurrencyType, PaymentType

pytestmark = [pytest.mark.asyncio]

NOW_IN_UTC = datetime.now(tz=timezone.utc)


@pytest.fixture(autouse=True)
def common_balance_client_mocks(balance_client):
    balance_client.find_client.coro.return_value = {
        "id": 600,
        "name": "Имя клиента",
        "email": "email@example.com",
        "phone": "8(499)123-45-67",
        "is_agency": False,
    }

    balance_client.list_client_contracts.coro.return_value = []


async def test_returns_client_data_from_balance(factory, clients_dm):
    result = await clients_dm.upsert_client(
        id=600,
        name="Имя клиента",
        email="email@example.com",
        phone="8(499)123-45-67",
        is_agency=False,
        has_accepted_offer=False,
        created_at=NOW_IN_UTC,
    )
    await clients_dm.sync_client_contracts(
        client_id=600,
        contracts=[
            {
                "id": 11,
                "external_id": "999/11",
                "currency": CurrencyType.RUB,
                "is_active": False,
                "date_start": date(2012, 2, 2),
                "date_end": date(2012, 3, 3),
                "payment_type": PaymentType.POST,
            },
            {
                "id": 22,
                "external_id": "999/22",
                "currency": CurrencyType.RUB,
                "is_active": True,
                "date_start": date(2012, 4, 4),
                "date_end": None,
                "payment_type": PaymentType.PRE,
            },
        ],
    )

    assert result == {
        "id": 600,
        "name": "Имя клиента",
        "email": "email@example.com",
        "phone": "8(499)123-45-67",
        "is_agency": False,
        "created_from_cabinet": False,
        "account_manager_id": None,
        "domain": "",
        "partner_agency_id": None,
        "has_accepted_offer": False,
        "created_at": NOW_IN_UTC,
        "representatives": [],
    }

    db_client_data = await factory.get_client(600)
    assert db_client_data == {
        "id": 600,
        "name": "Имя клиента",
        "email": "email@example.com",
        "phone": "8(499)123-45-67",
        "is_agency": False,
        "created_from_cabinet": False,
        "account_manager_id": None,
        "domain": "",
        "partner_agency_id": None,
        "has_accepted_offer": False,
        "created_at": NOW_IN_UTC,
        "representatives": [],
    }

    contracts_data = await factory.get_client_contracts(600)
    assert contracts_data == [
        {
            "id": 11,
            "client_id": 600,
            "external_id": "999/11",
            "currency": CurrencyType.RUB.name,
            "is_active": False,
            "date_start": date(2012, 2, 2),
            "date_end": date(2012, 3, 3),
            "payment_type": PaymentType.POST.name,
            "preferred": False,
        },
        {
            "id": 22,
            "client_id": 600,
            "external_id": "999/22",
            "currency": CurrencyType.RUB.name,
            "is_active": True,
            "date_start": date(2012, 4, 4),
            "date_end": None,
            "payment_type": PaymentType.PRE.name,
            "preferred": False,
        },
    ]


async def test_updates_client_data_if_found_locally(
    factory, clients_dm, balance_client
):
    client = await factory.create_client(
        id=55,
        name="Старое имя",
        email="old@example.com",
        phone="(095)123-45-67",
        is_agency=False,
        account_manager_id=200400,
        domain="someDomain",
        partner_agency_id=123,
    )

    result = await clients_dm.upsert_client(
        id=client["id"],
        name="Новое имя",
        email="new@example.com",
        phone="(495)321-54-76",
        is_agency=False,
        partner_agency_id=123,
    )

    assert await factory.get_client(client["id"]) == {
        "id": client["id"],
        "name": "Новое имя",
        "email": "new@example.com",
        "phone": "(495)321-54-76",
        "is_agency": False,
        "created_from_cabinet": False,
        "account_manager_id": 200400,
        "domain": "someDomain",
        "partner_agency_id": 123,
        "has_accepted_offer": False,
        "created_at": None,
        "representatives": [],
    }

    assert result == {
        "id": client["id"],
        "name": "Новое имя",
        "email": "new@example.com",
        "phone": "(495)321-54-76",
        "is_agency": False,
        "created_from_cabinet": False,
        "account_manager_id": 200400,
        "domain": "someDomain",
        "partner_agency_id": 123,
        "has_accepted_offer": False,
        "created_at": None,
        "representatives": [],
    }
