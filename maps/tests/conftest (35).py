import pytest

from maps_adv.geosmb.clients.notify_me import NotifyMeClient


@pytest.fixture
async def client():
    async with NotifyMeClient(url="http://notify_me_test.server") as client:
        yield client
