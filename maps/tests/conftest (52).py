import aiohttp.pytest_plugin
import pytest

from maps_adv.points.client.lib import Client

pytest_plugins = ["aiohttp.pytest_plugin"]
del aiohttp.pytest_plugin.loop  # DEVTOOLS-5496


@pytest.fixture
async def rmock(aresponses):
    return lambda *a: aresponses.add("points.server", *a)


@pytest.fixture
@pytest.mark.usefixtures("aresponses")
def make_client(config):
    return lambda **kw: Client("http://points.server")


@pytest.fixture
def mock_points(rmock):
    return lambda h: rmock("/api/v1/points/billboard/1/by-polygons/", "POST", h)
