import pytest

from maps_adv.common.email_sender import Client


@pytest.fixture
def mock_sender_api(aresponses):
    return lambda *a: aresponses.add(
        "sender.api", "/api/0/kek-account/automation/promoletter", "POST", *a
    )


@pytest.fixture
def mock_api_transaction_send(aresponses):
    return lambda slug, *a: aresponses.add(
        "sender.api", f"/api/0/kek-account/transactional/{slug}/send", "POST", *a
    )


@pytest.fixture
async def client():
    async with Client(
        url="http://sender.api",
        account_slug="kek-account",
        account_token="account-token",
    ) as _client:
        yield _client
