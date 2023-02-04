import pytest
import os
import tempfile
import yt.wrapper as yt
from ads_pytorch.yt.table_path import TablePath
from ads_pytorch.yt.file_system_adapter_mock import CypressAdapterMock
from ads_pytorch.online_learning.production.artifact.folder import *


##################################################
#                  NEW VALUE TESTS               #
##################################################


@pytest.fixture
def branch():
    return "branch1"


@pytest.fixture
def model_yt_dir(branch):
    with tempfile.TemporaryDirectory() as tmp:
        os.makedirs(os.path.join(tmp, "branches"))
        os.makedirs(os.path.join(tmp, "branches", branch))
        os.makedirs(state_path(fs=CypressAdapterMock(), model_yt_dir=tmp, branch_name=branch))

        yield tmp


@pytest.mark.asyncio
async def test_did_not_create_new_value(model_yt_dir, branch):
    fs = CypressAdapterMock()
    with pytest.raises(NewValueNotCreatedError):
        async with new_artifact_value(
            fs=fs,
            model_yt_dir=model_yt_dir,
            branch_name=branch,
            artifact_name="some_art",
            uri="//home/123"
        ) as value:
            # Check that we do everything under tx
            assert value.tx is not None
            assert not await fs.exists(value.path, tx=value.tx)
    assert not await fs.exists(value.path)


@pytest.mark.parametrize(
    'uri',
    ['//home/123', TablePath("//home/123", lower_key="1", upper_key="5")],
    ids=['str', 'TablePath']
)
@pytest.mark.parametrize('method', ['create_file', 'create_table', 'create_directory'])
@pytest.mark.asyncio
async def test_new_value_with_new_dir(model_yt_dir, branch, method, uri):
    artifact_name = "some_art"
    fs = CypressAdapterMock()

    folder = artifact_values_path(
        fs=fs,
        model_yt_dir=model_yt_dir,
        branch_name=branch,
        artifact_name=artifact_name
    )
    assert not await fs.exists(folder)

    storage = BranchDirectoryArtifactStorage(
        model_yt_dir=model_yt_dir,
        branch_name=branch,
        fs_adapter=fs
    )

    async with storage.new_artifact_path(
        artifact_name=artifact_name,
        uri=uri
    ) as value:
        # Check that we do everything under tx
        assert await fs.get(os.path.join(folder, "@" + PYTORCH_URI_ATTRIBUTE)) == {}
        assert value.tx is not None
        assert not await fs.exists(value.path, tx=value.tx)
        await getattr(fs, method)(value.path)
    assert await fs.exists(value.path)
    assert await fs.get(os.path.join(value.path, "@" + PYTORCH_URI_ATTRIBUTE)) == TablePath(uri).to_yson_string()
    assert await fs.get(os.path.join(folder, "@" + PYTORCH_URI_ATTRIBUTE)) == {
        '0': TablePath(uri).to_yson_string()
    }


@pytest.mark.parametrize('method', ['create_file', 'create_table', 'create_directory'])
@pytest.mark.asyncio
async def test_new_value_existing_dir(model_yt_dir, branch, method):
    artifact_name = "some_art"
    uri = "//home/123"
    fs = CypressAdapterMock()

    folder = artifact_values_path(fs=fs, model_yt_dir=model_yt_dir, branch_name=branch, artifact_name=artifact_name)

    storage = BranchDirectoryArtifactStorage(
        fs_adapter=fs,
        model_yt_dir=model_yt_dir,
        branch_name=branch
    )

    for i in range(4):
        async with storage.new_artifact_path(
            artifact_name=artifact_name,
            uri=os.path.join(uri, str(i))
        ) as value:
            await getattr(fs, method)(value.path)

    assert await fs.get(os.path.join(folder, "@" + PYTORCH_URI_ATTRIBUTE)) == {
        str(i): os.path.join(uri, str(i))
        for i in range(4)
    }
    for i in range(4):
        assert await fs.get(os.path.join(folder, str(i), "@" + PYTORCH_URI_ATTRIBUTE)) == os.path.join(uri, str(i))
