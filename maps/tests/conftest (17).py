import pytest

from maps_adv.common.mds import MDSClient, MDSInstallation

pytest_plugins = ["smb.common.aiotvm.pytest.plugin"]


@pytest.fixture
async def client(mocker, aiotvm):
    mocker.patch.dict(
        "maps_adv.common.mds.mds_installations",
        {
            "debug": MDSInstallation(
                outer_read_url="http://mds-outer-read.server",
                inner_read_url="http://mds-inner-read.server",
                write_url="http://mds-write.server",
            )
        },
    )

    async with MDSClient(
        installation="debug",  # noqa
        namespace="mds-namespace",
        tvm_client=aiotvm,
        tvm_destination="mds",
    ) as client:
        yield client
