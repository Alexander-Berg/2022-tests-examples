import pytest

from maps_adv.geosmb.clients.cdp import CdpClient

pytest_plugins = ["aiohttp.pytest_plugin", "smb.common.aiotvm.pytest.plugin"]


@pytest.fixture
async def cdp_client(aiotvm):
    async with CdpClient(
        url="https://cdp.test", tvm=aiotvm, tvm_destination="cdp"
    ) as _client:
        yield _client
