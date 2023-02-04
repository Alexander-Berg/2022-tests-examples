import pytest

from maps_adv.geosmb.clients.bunker import BunkerClient


@pytest.fixture
async def client():
    async with BunkerClient(url="http://bunker.server") as client:
        yield client
