from operator import itemgetter

import pytest

from maps_adv.billing_proxy.lib.db.enums import CurrencyType, PaymentType

pytestmark = [pytest.mark.asyncio]


async def test_list_clients_contracts(factory, client, clients_dm):
    contract1 = await factory.create_contract(client_id=client["id"])
    contract2 = await factory.create_contract(client_id=client["id"], date_end=None)

    result = await clients_dm.list_contacts_by_client(client["id"])
    expected = [
        {
            "id": contract1["id"],
            "external_id": contract1["external_id"],
            "client_id": contract1["client_id"],
            "currency": CurrencyType(contract1["currency"]),
            "is_active": contract1["is_active"],
            "date_start": contract1["date_start"],
            "date_end": contract1["date_end"],
            "payment_type": PaymentType(contract1["payment_type"]),
            "preferred": contract1["preferred"],
        },
        {
            "id": contract2["id"],
            "external_id": contract2["external_id"],
            "client_id": contract2["client_id"],
            "currency": CurrencyType(contract2["currency"]),
            "is_active": contract2["is_active"],
            "date_start": contract2["date_start"],
            "date_end": contract2["date_end"],
            "payment_type": PaymentType(contract2["payment_type"]),
            "preferred": contract2["preferred"],
        },
    ]

    assert sorted(result, key=itemgetter("id")) == sorted(
        expected, key=itemgetter("id")
    )


async def test_returns_empty_list_if_no_contracts(factory, client, clients_dm):
    assert await clients_dm.list_contacts_by_client(client["id"]) == []


async def test_not_returns_other_client_contracts(factory, client, clients_dm):
    await factory.create_contract()

    assert await clients_dm.list_contacts_by_client(client["id"]) == []


async def test_returns_only_this_client_contracts(factory, client, clients_dm):
    contract = await factory.create_contract(client_id=client["id"])
    await factory.create_contract()

    result = await clients_dm.list_contacts_by_client(client["id"])

    assert result == [
        {
            "id": contract["id"],
            "external_id": contract["external_id"],
            "client_id": contract["client_id"],
            "currency": CurrencyType(contract["currency"]),
            "is_active": contract["is_active"],
            "date_start": contract["date_start"],
            "date_end": contract["date_end"],
            "payment_type": PaymentType(contract["payment_type"]),
            "preferred": contract["preferred"],
        }
    ]
