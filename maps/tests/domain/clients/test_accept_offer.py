import pytest

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(clients_domain, clients_dm):
    clients_dm.set_client_has_accepted_offer.coro.return_value = None

    await clients_domain.set_client_has_accepted_offer(42, is_agency=False)

    clients_dm.set_client_has_accepted_offer.assert_called_with(42, False)
