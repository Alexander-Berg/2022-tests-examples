import pytest

from maps_adv.common.ugcdb_client import UgcdbClient

pytest_plugins = ["smb.common.aiotvm.pytest.plugin"]


@pytest.fixture
async def client(aiotvm):
    async with UgcdbClient(
        url="http://ugcdb.test", tvm=aiotvm, tvm_destination="ugcdb"
    ) as _client:
        yield _client
