from datetime import datetime, timezone

import pytest

from maps_adv.billing_proxy.lib.domain.exceptions import (
    AgencyDoesNotExist,
    ClientContractMismatch,
    ClientDoesNotExist,
    ClientIsAgency,
    ClientIsNotAgency,
    ClientNotInAgency,
    ContractDoesNotExist,
    ProductAgencyMismatch,
    ProductClientMismatch,
    ProductDoesNotExist,
    ProductIsInactive,
    ProductServiceMismatch,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture(autouse=True)
def common_dm_mocks(freezer, orders_dm, clients_dm, products_dm):
    freezer.move_to(datetime(2000, 2, 2, tzinfo=timezone.utc))

    clients_dm.find_client_locally.coro.side_effect = [
        {"id": 11, "is_agency": False},
        {"id": 3333, "is_agency": True},
    ]
    clients_dm.client_is_in_agency.coro_return_value = True
    clients_dm.find_contract.coro.return_value = {
        "id": 4444,
        "external_id": "4444/44",
        "client_id": 3333,
    }
    products_dm.find_product.coro.return_value = {
        "id": 123,
        "service_id": 110,
        "active_from": datetime(2000, 1, 1, tzinfo=timezone.utc),
        "active_to": datetime(2000, 3, 3, tzinfo=timezone.utc),
        "act_text": "Текст акта из продукта",
        "dedicated_client_ids": None,
        "available_for_agencies": True,
        "available_for_internal": True,
    }
    orders_dm.create_order.coro.return_value = {"id": 1, "title": "Заказ"}


async def test_uses_dm(orders_domain, orders_dm, products_dm):
    result = await orders_domain.create_order(
        title="Заказ",
        text="Текст",
        comment="Коммент",
        client_id=11,
        agency_id=3333,
        contract_id=4444,
        product_id=123,
        service_id=110,
    )

    orders_dm.create_order.assert_called_with(
        service_id=110,
        title="Заказ",
        text="Текст",
        comment="Коммент",
        client_id=11,
        agency_id=3333,
        contract_id=4444,
        product_id=123,
    )
    assert result == {"id": 1, "title": "Заказ"}


async def test_raises_for_inexisting_client(orders_domain, clients_dm):
    clients_dm.find_client_locally.coro.side_effect = [
        None,
        {"id": 3333, "is_agency": True},
    ]

    with pytest.raises(ClientDoesNotExist) as exc:
        await orders_domain.create_order(
            title="Заказ",
            text="Текст",
            comment="Коммент",
            client_id=11,
            agency_id=3333,
            contract_id=4444,
            product_id=123,
            service_id=110,
        )

    assert exc.value.client_id == 11


async def test_raises_for_inexisting_agency(orders_domain, clients_dm):
    clients_dm.find_client_locally.coro.side_effect = [
        {"id": 11, "is_agency": False},
        None,
    ]

    with pytest.raises(AgencyDoesNotExist) as exc:
        await orders_domain.create_order(
            title="Заказ",
            text="Текст",
            comment="Коммент",
            client_id=11,
            agency_id=3333,
            contract_id=4444,
            product_id=123,
            service_id=110,
        )

    assert exc.value.agency_id == 3333


async def test_raises_if_client_is_agency(orders_domain, clients_dm):
    clients_dm.find_client_locally.coro.side_effect = [
        {"id": 11, "is_agency": True},
        {"id": 3333, "is_agency": True},
    ]

    with pytest.raises(ClientIsAgency) as exc:
        await orders_domain.create_order(
            title="Заказ",
            text="Текст",
            comment="Коммент",
            client_id=11,
            agency_id=3333,
            contract_id=4444,
            product_id=123,
            service_id=110,
        )

    assert exc.value.client_id == 11


async def test_raises_if_agency_is_not_agency(orders_domain, clients_dm):
    clients_dm.find_client_locally.coro.side_effect = [
        {"id": 11, "is_agency": False},
        {"id": 3333, "is_agency": False},
    ]

    with pytest.raises(ClientIsNotAgency) as exc:
        await orders_domain.create_order(
            title="Заказ",
            text="Текст",
            comment="Коммент",
            client_id=11,
            agency_id=3333,
            contract_id=4444,
            product_id=123,
            service_id=110,
        )

    assert exc.value.client_id == 3333


async def test_raises_if_client_not_in_agency(orders_domain, clients_dm):
    clients_dm.client_is_in_agency.coro.return_value = False

    with pytest.raises(ClientNotInAgency) as exc:
        await orders_domain.create_order(
            title="Заказ",
            text="Текст",
            comment="Коммент",
            client_id=11,
            agency_id=3333,
            contract_id=4444,
            product_id=123,
            service_id=110,
        )

    assert exc.value.client_id == 11
    assert exc.value.agency_id == 3333


async def test_raises_for_inexistent_contract(orders_domain, clients_dm):
    clients_dm.find_contract.coro.return_value = None

    with pytest.raises(ContractDoesNotExist) as exc:
        await orders_domain.create_order(
            title="Заказ",
            text="Текст",
            comment="Коммент",
            client_id=11,
            agency_id=3333,
            contract_id=4444,
            product_id=123,
            service_id=110,
        )

    assert exc.value.contract_id == 4444


@pytest.mark.parametrize(
    "contract_client_id", [5555, 11]  # Some other client  # client, but not agency
)
async def test_raises_for_agency_contract_mismatch(
    orders_domain, clients_dm, contract_client_id
):
    clients_dm.find_contract.coro.return_value = {
        "id": 4444,
        "external_id": "4444/44",
        "client_id": contract_client_id,
    }

    with pytest.raises(ClientContractMismatch) as exc:
        await orders_domain.create_order(
            title="Заказ",
            text="Текст",
            comment="Коммент",
            client_id=11,
            agency_id=3333,
            contract_id=4444,
            product_id=123,
            service_id=110,
        )

    assert exc.value.client_id == 3333
    assert exc.value.contract_id == 4444


async def test_raises_for_client_contract_mismatch(orders_domain, clients_dm):
    clients_dm.find_contract.coro.return_value = {
        "id": 4444,
        "external_id": "4444/44",
        "client_id": 5555,
    }

    with pytest.raises(ClientContractMismatch) as exc:
        await orders_domain.create_order(
            title="Заказ",
            text="Текст",
            comment="Коммент",
            client_id=11,
            agency_id=None,
            contract_id=4444,
            product_id=123,
            service_id=110,
        )

    assert exc.value.client_id == 11
    assert exc.value.contract_id == 4444


async def test_raises_for_inexistent_product(orders_domain, products_dm):
    products_dm.find_product.coro.return_value = None

    with pytest.raises(ProductDoesNotExist) as exc:
        await orders_domain.create_order(
            title="Заказ",
            text="Текст",
            comment="Коммент",
            client_id=11,
            agency_id=3333,
            contract_id=4444,
            product_id=123,
            service_id=110,
        )

    assert exc.value.product_id == 123


@pytest.mark.parametrize(
    ("active_from", "active_to"),
    [
        (
            datetime(2000, 1, 1, tzinfo=timezone.utc),
            datetime(2000, 1, 31, tzinfo=timezone.utc),
        ),
        (
            datetime(2000, 3, 1, tzinfo=timezone.utc),
            datetime(2000, 3, 31, tzinfo=timezone.utc),
        ),
    ],
)
async def test_raises_for_inactive_product(
    orders_domain, products_dm, active_from, active_to
):
    products_dm.find_product.coro.return_value = {
        "id": 123,
        "service_id": 110,
        "active_from": active_from,
        "active_to": active_to,
        "act_text": "Текст акта из продукта",
        "dedicated_client_ids": None,
        "available_for_agencies": True,
        "available_for_internal": True,
    }

    with pytest.raises(ProductIsInactive) as exc:
        await orders_domain.create_order(
            title="Заказ",
            text="Текст",
            comment="Коммент",
            client_id=11,
            agency_id=3333,
            contract_id=4444,
            product_id=123,
            service_id=110,
        )

    assert exc.value.product_id == 123


async def test_raises_for_product_not_for_agency(orders_domain, products_dm):
    products_dm.find_product.coro.return_value = {
        "id": 123,
        "service_id": 110,
        "active_from": datetime(2000, 1, 1, tzinfo=timezone.utc),
        "active_to": datetime(2000, 3, 3, tzinfo=timezone.utc),
        "act_text": "Текст акта из продукта",
        "dedicated_client_ids": None,
        "available_for_agencies": False,
        "available_for_internal": True,
    }

    with pytest.raises(ProductAgencyMismatch) as exc:
        await orders_domain.create_order(
            title="Заказ",
            text="Текст",
            comment="Коммент",
            client_id=11,
            agency_id=3333,
            contract_id=4444,
            product_id=123,
            service_id=110,
        )

    assert exc.value.product_id == 123
    assert exc.value.agency_id == 3333


async def test_raises_for_product_not_for_internal(
    orders_domain, products_dm, clients_dm
):
    clients_dm.find_contract.coro.return_value = {
        "id": 4444,
        "external_id": "4444/44",
        "client_id": 11,
    }
    products_dm.find_product.coro.return_value = {
        "id": 123,
        "service_id": 110,
        "active_from": datetime(2000, 1, 1, tzinfo=timezone.utc),
        "active_to": datetime(2000, 3, 3, tzinfo=timezone.utc),
        "act_text": "Текст акта из продукта",
        "dedicated_client_ids": None,
        "available_for_agencies": True,
        "available_for_internal": False,
    }

    with pytest.raises(ProductAgencyMismatch) as exc:
        await orders_domain.create_order(
            title="Заказ",
            text="Текст",
            comment="Коммент",
            client_id=11,
            agency_id=None,
            contract_id=4444,
            product_id=123,
            service_id=110,
        )

    assert exc.value.product_id == 123
    assert exc.value.agency_id is None


async def test_creates_order_for_dedictated_product(
    orders_domain, orders_dm, products_dm
):
    products_dm.find_product.coro.return_value = {
        "id": 123,
        "service_id": 110,
        "active_from": datetime(2000, 1, 1, tzinfo=timezone.utc),
        "active_to": datetime(2000, 3, 3, tzinfo=timezone.utc),
        "act_text": "Текст акта из продукта",
        "dedicated_client_ids": [11],
        "available_for_agencies": True,
        "available_for_internal": True,
    }

    await orders_domain.create_order(
        title="Заказ",
        text="Текст",
        comment="Коммент",
        client_id=11,
        agency_id=3333,
        contract_id=4444,
        product_id=123,
        service_id=110,
    )

    assert orders_dm.create_order.called


async def test_raises_for_dedictated_product_not_for_client(orders_domain, products_dm):
    products_dm.find_product.coro.return_value = {
        "id": 123,
        "service_id": 110,
        "active_from": datetime(2000, 1, 1, tzinfo=timezone.utc),
        "active_to": datetime(2000, 3, 3, tzinfo=timezone.utc),
        "act_text": "Текст акта из продукта",
        "dedicated_client_ids": [333],
        "available_for_agencies": False,
        "available_for_internal": True,
    }

    with pytest.raises(ProductClientMismatch) as exc:
        await orders_domain.create_order(
            title="Заказ",
            text="Текст",
            comment="Коммент",
            client_id=11,
            agency_id=3333,
            contract_id=4444,
            product_id=123,
            service_id=110,
        )

    assert exc.value.product_id == 123
    assert exc.value.client_ids == [11]


async def test_raises_for_service_id_mismatch(orders_domain, products_dm):
    products_dm.find_product.coro.return_value = {
        "id": 123,
        "service_id": 110,
        "active_from": datetime(2000, 1, 1, tzinfo=timezone.utc),
        "active_to": datetime(2000, 3, 3, tzinfo=timezone.utc),
        "act_text": "Текст акта из продукта",
        "dedicated_client_ids": None,
        "available_for_agencies": True,
        "available_for_internal": True,
    }

    with pytest.raises(ProductServiceMismatch) as exc:
        await orders_domain.create_order(
            title="Заказ",
            text="Текст",
            comment="Коммент",
            client_id=11,
            agency_id=3333,
            contract_id=None,
            product_id=123,
            service_id=37,
        )

    assert exc.value.product_id == 123
    assert exc.value.product_service_id == 110
    assert exc.value.service_id == 37
