import pytest

from maps_adv.billing_proxy.lib.domain.exceptions import (
    AgencyDoesNotExist,
    ClientDoesNotExist,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture(autouse=True)
async def common_dm_mocks(orders_dm, clients_dm):
    clients_dm.agency_exists.coro.return_value = True
    clients_dm.client_exists.coro.return_value = True
    orders_dm.list_agency_orders.coro.return_value = [
        {"id": 1, "title": "Заказ 1"},
        {"id": 2, "title": "Заказ 2"},
    ]


async def test_uses_dm(orders_domain, orders_dm):
    result = await orders_domain.list_agency_orders(agency_id=111, client_id=22)

    orders_dm.list_agency_orders.assert_called_with(111, 22)
    assert result == [{"id": 1, "title": "Заказ 1"}, {"id": 2, "title": "Заказ 2"}]


async def test_raises_for_inexistent_agency(orders_domain, clients_dm):
    clients_dm.agency_exists.coro.return_value = False

    with pytest.raises(AgencyDoesNotExist) as exc:
        await orders_domain.list_agency_orders(agency_id=111, client_id=22)

    assert exc.value.agency_id == 111


async def test_raises_for_inexistent_client(orders_domain, clients_dm):
    clients_dm.client_exists.coro.return_value = False

    with pytest.raises(ClientDoesNotExist) as exc:
        await orders_domain.list_agency_orders(agency_id=111, client_id=22)

    assert exc.value.client_id == 22


async def test_works_for_internal_orders(orders_domain, orders_dm, clients_dm):
    await orders_domain.list_agency_orders(agency_id=None, client_id=22)

    assert not clients_dm.agency_exists.called
    orders_dm.list_agency_orders.assert_called_with(None, 22)


async def test_works_for_no_client_filter(orders_domain, orders_dm, clients_dm):
    await orders_domain.list_agency_orders(agency_id=111)

    assert not clients_dm.client_exists.called
    orders_dm.list_agency_orders.assert_called_with(111, None)
