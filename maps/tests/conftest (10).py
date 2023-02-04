import pytest

from maps_adv.common.avatars import AvatarsClient, AvatarsInstallation

pytest_plugins = ["smb.common.aiotvm.pytest.plugin"]


@pytest.fixture
async def client(mocker, aiotvm):
    mocker.patch.dict(
        "maps_adv.common.avatars.avatars_installations",
        {
            "debug": AvatarsInstallation(
                outer_read_url="http://avatars-outer-read.server",
                inner_read_url="http://avatars-inner-read.server",
                write_url="http://avatars-write.server",
            )
        },
    )

    async with AvatarsClient(
        installation="debug",  # noqa
        namespace="avatars-namespace",
        tvm_client=aiotvm,
        tvm_destination="avatars",
    ) as client:
        yield client
