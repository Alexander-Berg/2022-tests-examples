import pytest

from maps_adv.common.geoproduct import GeoproductClient

pytest_plugins = ["smb.common.aiotvm.pytest.plugin"]


@pytest.fixture
async def client(aiotvm):
    async with GeoproductClient(
        url="http://geoproduct.api",
        default_uid=1010,
        tvm_client=aiotvm,
        tvm_destination="geoproduct",
    ) as client:
        yield client
