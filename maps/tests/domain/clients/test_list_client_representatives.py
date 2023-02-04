import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]
from maps_adv.billing_proxy.lib.domain.exceptions import (
    ClientDoesNotExist,
    AgencyDoesNotExist,
)


async def test_calls_balance(clients_domain, clients_dm):
    clients_dm.find_client_locally.coro.return_value = {
        "is_agency": False,
        "representatives": [111, 222, 333],
    }

    result = await clients_domain.list_client_representatives(123, is_agency=False)
    assert result == [111, 222, 333]

    clients_dm.find_client_locally.coro.return_value = {
        "is_agency": True,
        "representatives": [],
    }
    result = await clients_domain.list_client_representatives(123, is_agency=True)
    assert result == []


async def test_raises_if_no_client(clients_domain, clients_dm):
    clients_dm.find_client_locally.coro.return_value = None

    with pytest.raises(ClientDoesNotExist) as exc:
        await clients_domain.list_client_representatives(123, False)
    assert exc.value.client_id == 123

    with pytest.raises(AgencyDoesNotExist) as exc:
        await clients_domain.list_client_representatives(123, True)
    assert exc.value.agency_id == 123

    clients_dm.find_client_locally.coro.return_value = {"is_agency": True}

    with pytest.raises(ClientDoesNotExist) as exc:
        await clients_domain.list_client_representatives(123, False)
    assert exc.value.client_id == 123

    clients_dm.find_client_locally.coro.return_value = {"is_agency": False}

    with pytest.raises(AgencyDoesNotExist) as exc:
        await clients_domain.list_client_representatives(123, True)
    assert exc.value.agency_id == 123
