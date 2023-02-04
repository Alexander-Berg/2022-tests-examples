import pytest

from maps_adv.billing_proxy.lib.db.enums import CurrencyType, PaymentType

pytestmark = [pytest.mark.asyncio]


async def test_returns_contract(contract, clients_dm):
    result = await clients_dm.find_contract(contract_id=contract["id"])

    assert result == {
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


async def test_returns_none_if_no_contract_exists(contract, clients_dm):
    result = await clients_dm.find_contract(contract_id=contract["id"] + 1)

    assert result is None
