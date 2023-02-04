import pytest

from maps_adv.common.aioyav import YavClient


@pytest.fixture
def rmock(aresponses):
    return lambda *a: aresponses.add("vault-api.passport.yandex.net", *a)


@pytest.fixture
async def make_yav():
    clients = []

    async def make(token):
        client = await YavClient(token)
        clients.append(client)
        return client

    yield make
    for client in clients:
        await client.close()


@pytest.fixture
async def yav(make_yav):
    return await make_yav("oauth_token")
