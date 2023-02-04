from datetime import date

import pytest

from maps_adv.billing_proxy.lib.db.enums import CurrencyType, PaymentType

pytestmark = [pytest.mark.asyncio]


async def test_adds_missing_contracts(factory, clients_dm):
    await factory.create_client(
        id=22, name="Имя 22", email="22@ya.ru", phone="222", is_agency=False
    )
    await factory.create_contract(id=222, client_id=22)

    await clients_dm.sync_client_contracts(
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

    contracts_by_id = {
        contract.pop("id"): contract
        for contract in await factory.get_client_contracts(22)
    }
    assert len(contracts_by_id) == 2
    assert contracts_by_id[111] == {
        "client_id": 22,
        "external_id": "1111/12",
        "currency": CurrencyType.RUB.name,
        "is_active": True,
        "date_start": date(2012, 2, 2),
        "date_end": date(2012, 3, 3),
        "payment_type": PaymentType.POST.name,
        "preferred": False,
    }


async def test_updates_existing_contracts(factory, clients_dm):
    await factory.create_client(
        id=22, name="Имя 22", email="22@ya.ru", phone="222", is_agency=False
    )
    await factory.create_contract(
        id=222,
        client_id=22,
        external_id="0000/12",
        currency=CurrencyType.EUR,
        is_active=True,
        date_start=date(2011, 2, 3),
        date_end=date(2011, 3, 4),
        payment_type=PaymentType.POST,
        preferred=False,
    )

    await clients_dm.sync_client_contracts(
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

    contracts_by_id = {
        contract.pop("id"): contract
        for contract in await factory.get_client_contracts(22)
    }
    assert len(contracts_by_id) == 2
    assert contracts_by_id[222] == {
        "client_id": 22,
        "external_id": "2222/12",
        "currency": CurrencyType.USD.name,
        "is_active": False,
        "date_start": date(2012, 4, 4),
        "date_end": None,
        "payment_type": PaymentType.PRE.name,
        "preferred": False,
    }


async def test_marks_existing_contracts_not_provided_inactive(factory, clients_dm):
    await factory.create_client(
        id=22, name="Имя 22", email="22@ya.ru", phone="222", is_agency=False
    )
    await factory.create_contract(
        id=333,
        client_id=22,
        external_id="3333/12",
        currency=CurrencyType.RUB,
        is_active=True,
        date_start=date(2011, 2, 3),
        date_end=date(2011, 3, 4),
        payment_type=PaymentType.POST,
        preferred=False,
    )

    await clients_dm.sync_client_contracts(
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

    contracts_by_id = {
        contract.pop("id"): contract
        for contract in await factory.get_client_contracts(22)
    }
    assert len(contracts_by_id) == 3
    assert contracts_by_id[333] == {
        "client_id": 22,
        "external_id": "3333/12",
        "currency": CurrencyType.RUB.name,
        "is_active": False,
        "date_start": date(2011, 2, 3),
        "date_end": date(2011, 3, 4),
        "payment_type": PaymentType.POST.name,
        "preferred": False,
    }


async def test_combined_case(factory, clients_dm):
    await factory.create_client(
        id=22, name="Имя 22", email="22@ya.ru", phone="222", is_agency=False
    )
    await factory.create_contract(
        id=222,
        client_id=22,
        external_id="0000/12",
        currency=CurrencyType.EUR,
        is_active=True,
        date_start=date(2011, 2, 3),
        date_end=date(2011, 3, 4),
        payment_type=PaymentType.POST,
        preferred=False,
    )
    await factory.create_contract(
        id=333,
        client_id=22,
        external_id="3333/12",
        currency=CurrencyType.RUB,
        is_active=True,
        date_start=date(2011, 2, 3),
        date_end=date(2011, 3, 4),
        payment_type=PaymentType.POST,
        preferred=False,
    )

    await clients_dm.sync_client_contracts(
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

    contracts = await factory.get_client_contracts(22)
    assert contracts == [
        {
            "id": 111,
            "client_id": 22,
            "external_id": "1111/12",
            "currency": CurrencyType.RUB.name,
            "is_active": True,
            "date_start": date(2012, 2, 2),
            "date_end": date(2012, 3, 3),
            "payment_type": PaymentType.POST.name,
            "preferred": False,
        },
        {
            "id": 222,
            "client_id": 22,
            "external_id": "2222/12",
            "currency": CurrencyType.USD.name,
            "is_active": False,
            "date_start": date(2012, 4, 4),
            "date_end": None,
            "payment_type": PaymentType.PRE.name,
            "preferred": False,
        },
        {
            "id": 333,
            "client_id": 22,
            "external_id": "3333/12",
            "currency": CurrencyType.RUB.name,
            "is_active": False,
            "date_start": date(2011, 2, 3),
            "date_end": date(2011, 3, 4),
            "payment_type": PaymentType.POST.name,
            "preferred": False,
        },
    ]
