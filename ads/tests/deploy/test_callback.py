import pytest
import torch
import os
from typing import Dict, Optional, Any, List
import tempfile
import datetime
import yt.wrapper as yt
from ads_pytorch.yt.table_path import TablePath

from ads_pytorch.yt.file_system_adapter_mock import CypressAdapterMock

from ads_pytorch.online_learning.production.artifact.folder import (
    BranchDirectoryArtifactStorage
)

from ads_pytorch.tools.async_worker_pool import BaseAsyncWorkerPool
from ads_pytorch.model_calcer.minibatch_record import MinibatchRecord
from ads_pytorch.deploy.callbacks import ProductionDeployCallback, PeriodicDeployCallback
from ads_pytorch.deploy.base_cpp_deploy import CppDeployer
from ads_pytorch.tools.stream_reader import BaseStreamReader
from ads_pytorch.core.disk_adapter import DiskSavePool
from ads_pytorch.deploy.deployable_model_serialization import (
    register_deep_serializer,
    unregister_deep_serializer
)
from ads_pytorch.deploy.deployable_model import (
    IDeployableModel,
    TorchModuleDeployDescriptor
)
from ads_pytorch.nn.module.base_embedding_model import BaseEmbeddingModel, EmbeddingDescriptor
from ads_pytorch.online_learning.production.uri import ProdURI
from ads_pytorch.online_learning.production.dataset import DatetimeURI


class TestStreamReader(BaseStreamReader):
    def __init__(self, inputs: Dict[str, Any], reader_pool: BaseAsyncWorkerPool, max_memory: Optional[int] = 5 * 1 << 20, name=None):
        super().__init__(reader_pool, max_memory, name)
        self.inputs = inputs

    async def schedule(self, data):
        return self.iter()

    async def iter(self):
        yield MinibatchRecord(inputs=self.inputs, targets=None, keys=None)


class MyModel(torch.nn.Module):
    def __init__(self, outputs):
        super(MyModel, self).__init__()
        self._outputs = outputs

    def forward(self, *input):
        return self._outputs


class MyModuleDeployable(IDeployableModel):
    def __init__(self, embedding_model, outputs):
        super(MyModuleDeployable, self).__init__(embedding_model)
        self._deep = MyModel(outputs)

    def get_serializable_models(self):
        return {
            "model123": TorchModuleDeployDescriptor(
                model=self._deep,
                serialize_mode="__pytest_model__"
            )
        }

    def deployable_model_forward(self, embedded_inputs: List[torch.Tensor]):
        return self._deep(*embedded_inputs)


@pytest.fixture
def mymodule_serializer():
    @register_deep_serializer("__pytest_model__")
    async def serialize_mymodule(model, fs_adapter, save_pool, path, tx):
        with open(os.path.join(path, "ahaha"), "wt") as f:
            f.write("123")

    yield

    unregister_deep_serializer("__pytest_model__")


@pytest.mark.asyncio
async def test_production_callback(mymodule_serializer):
    inputs = {
        "cat_inp1": (torch.LongTensor([1, 2, 3]), torch.IntTensor([3])),
        "rv_1": torch.Tensor([[1, 2, 3], [4, 5, 6]])
    }
    outputs = {
        "prediction1": torch.Tensor([1, 1, 1]), "prediction2": torch.Tensor([2, 2, 2, 2])
    }
    download_pool = DiskSavePool()
    stream_reader = TestStreamReader(inputs, download_pool)
    fs_adapter = CypressAdapterMock()
    deployer = CppDeployer(
        file_system_adapter=fs_adapter,
        upload_pool=DiskSavePool(),
        download_pool=download_pool,
        stream_reader=stream_reader,
        uri_to_single_batch_sample_converter=lambda x: x
    )
    with tempfile.TemporaryDirectory() as tmp:
        callback = ProductionDeployCallback(
            deployer=deployer,
            artifact_storage=BranchDirectoryArtifactStorage(fs_adapter, tmp, "branch"),
            artifact_name="test_artifact",
        )
        model = MyModuleDeployable(
            embedding_model=BaseEmbeddingModel([EmbeddingDescriptor(name="cat", features=['cat_inp1'], dim=5)], external_factors=['rv_1']),
            outputs=outputs
        )
        await callback(model, None, None, ProdURI(DatetimeURI("//home/ahahhahaha", datetime.datetime.now()), False))
        value_path = os.path.join(tmp, "branches", "branch", "test_artifact", "0")
        assert os.path.exists(value_path)
        model_path = os.path.join(value_path, "model123")
        assert os.path.exists(model_path)


@pytest.mark.asyncio
async def test_periodic_callback(mymodule_serializer):
    inputs = {
        "cat_inp1": (torch.LongTensor([1, 2, 3]), torch.IntTensor([3])),
        "rv_1": torch.Tensor([[1, 2, 3], [4, 5, 6]])
    }
    outputs = {
        "prediction1": torch.Tensor([1, 1, 1]), "prediction2": torch.Tensor([2, 2, 2, 2])
    }
    download_pool = DiskSavePool()
    stream_reader = TestStreamReader(inputs, download_pool)
    fs_adapter = CypressAdapterMock()
    deployer = CppDeployer(
        file_system_adapter=fs_adapter,
        upload_pool=DiskSavePool(),
        download_pool=download_pool,
        stream_reader=stream_reader,
        uri_to_single_batch_sample_converter=lambda x: x
    )
    uri = TablePath("//home/ahahhahaha")
    with tempfile.TemporaryDirectory() as tmp:
        callback = PeriodicDeployCallback(
            deployer=deployer,
            file_system_adapter=fs_adapter,
            save_path=tmp,
            uris=frozenset({uri}),
        )
        model = MyModuleDeployable(
            embedding_model=BaseEmbeddingModel([EmbeddingDescriptor(name="cat", features=['cat_inp1'], dim=5)], external_factors=['rv_1']),
            outputs=outputs
        )
        await callback(model, None, None, uri)
        value_path = tmp
        assert os.path.exists(value_path)
        model_path = os.path.join(value_path, "model123")
        assert os.path.exists(model_path)
