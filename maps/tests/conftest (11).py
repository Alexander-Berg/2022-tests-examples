import pytest

from maps_adv.common.blackbox import BlackboxClient

pytest_plugins = ["smb.common.aiotvm.pytest.plugin"]


@pytest.fixture
async def client(aiotvm):
    async with BlackboxClient(
        url="http://blackbox.api",
        session_host="session.host",
        tvm=aiotvm,
        tvm_destination="blackbox",
    ) as client:
        yield client
