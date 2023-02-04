import pytest

from maps_adv.common.yasms import YasmsClient

pytest_plugins = ["smb.common.aiotvm.pytest.plugin"]


@pytest.fixture
def mock_yasms_api(aresponses):
    return lambda *a: aresponses.add("yasms.api", "/sendsms", "GET", *a)


@pytest.fixture
async def yasms_client(aiotvm):
    async with YasmsClient(
        url="http://yasms.api",
        tvm=aiotvm,
        tvm_destination="anywhere",
        sender="awesome_sender",
    ) as client:
        yield client
