import pytest
import os
from typing import Dict, Tuple
import tempfile
import datetime
import struct

import torch.nn

from ads_pytorch.yt.file_system_adapter_mock import CypressAdapterMock

from ads_pytorch.online_learning.production.artifact.folder import (
    artifact_values_path,
    BranchDirectoryArtifactStorage
)

from ads_pytorch.hash_embedding.hash_embedding import HashEmbedding, create_hash_table, create_item
from deploy.callbacks.cpp_apply_processor import CppApplyProcessorCallback
from ads_pytorch.online_learning.production.uri import ProdURI
from ads_pytorch.online_learning.production.dataset import DatetimeURI
from deploy.production_model import (
    DeployableModel,
    ApplicableModel,
    EmbeddingDescriptor,
    ValidationDescriptor,
    EmbeddingFeature
)
from ads_pytorch.tools.async_worker_pool import BaseAsyncWorkerPool, BaseAsyncWorker


class DiskSaveWorker(BaseAsyncWorker):
    async def _do_job(self, data_stream, path, tx):
        assert tx is not None
        if not os.path.exists(os.path.dirname(path)):
            os.makedirs(os.path.dirname(path))
        with open(path, 'wb') as f:
            if isinstance(data_stream, (str, bytes)):
                f.write(data_stream)
            else:
                async for data in data_stream:
                    f.write(data)


class DiskSavePool(BaseAsyncWorkerPool):
    def create_new_worker(self, new_rank: int):
        return DiskSaveWorker(new_rank)


class ConstantModule(torch.nn.Module):
    def __init__(self, output_size: Tuple[int], output_value: float):
        super(ConstantModule, self).__init__()
        self._output_size = output_size
        self._output_value = output_value

    def forward(self, *input):
        return torch.ones(self._output_size) * self._output_value


class EmptyModel(ApplicableModel):
    def __init__(
            self,
            hash_embeddings_num: int,
            hash_embeddings_size: int,
            inputs_num: int,
            output_size: Tuple[int] = (1,),
            output_value: float = 0,
            table_type: str = "adam"
    ):
        super(EmptyModel, self).__init__()
        self.embeddings = torch.nn.ModuleDict(
            {
                "hash_emebdding_{}".format(i): HashEmbedding(create_hash_table(table_type, i + 10))
                for i in range(hash_embeddings_num)
            }
        )
        self._table_type = table_type
        self._init_embeddings(hash_embeddings_size)
        self._inputs_num = inputs_num
        self._output_size = output_size
        self._output_value = output_value

    def _init_embeddings(self, size: int):
        for table in self.embeddings.values():
            for i in range(1, size):
                item = create_item(self._table_type, table.dim())
                item.w = torch.FloatTensor([i] * table.dim())
                table.insert_item(i, item)
            if size > 0:
                item = create_item(self._table_type, table.dim())
                item.w = torch.FloatTensor([42.0] * table.dim())
                table.insert_item(0, item)

    def get_hash_embedding_mapping(self) -> Dict[str, EmbeddingDescriptor]:
        return {
            name: EmbeddingDescriptor(
                embedding=embed,
                features=[EmbeddingFeature(name=name, compute_mode="sum")]
            )
            for name, embed in self.embeddings.items()
        }

    def get_deep_part(self) -> torch.nn.Module:
        return ConstantModule(output_size=self._output_size, output_value=self._output_value)

    def process_parameter_name(self, name: str) -> str:
        return name

    def get_validation_inputs(self) -> ValidationDescriptor:
        return ValidationDescriptor(inputs={}, result=self.get_deep_part()({}))

    @property
    def model_config(self) -> Dict:
        return {
            "output_dim": 50
        }


class TestModel(DeployableModel):
    def __init__(
        self,
        hash_embeddings_num: int,
        hash_embeddings_size: int,
        inputs_num: int,
        output_size: Tuple[int] = (1,),
        output_value: float = 0,
        table_type: str = "adam"
    ):
        super(TestModel, self).__init__()
        self.model = EmptyModel(
            hash_embeddings_num,
            hash_embeddings_size,
            inputs_num,
            output_size,
            output_value,
            table_type
        )

    def get_applicable_models(self) -> Dict[str, ApplicableModel]:
        return {"EmptyModel": self.model}


async def process_model(model: DeployableModel, chunk_size: int, model_dir: str, branch: str, artifact: str):
    upload_pool = DiskSavePool(5)

    storage = BranchDirectoryArtifactStorage(
        fs_adapter=CypressAdapterMock(),
        model_yt_dir=model_dir,
        branch_name=branch
    )

    callback = CppApplyProcessorCallback(
        file_system_adapter=CypressAdapterMock(),
        upload_pool=upload_pool,
        artifact_storage=storage,
        artifact_name=artifact,
        chunk_size=chunk_size
    )

    await callback(
        model,
        None,
        None,
        ProdURI(DatetimeURI("//home/TEST_URI", date=datetime.datetime.now()), False)
    )


async def check_embedding_validation(table_path: str, dim: int, size: int):
    fs_adapter = CypressAdapterMock()

    validation_info = await fs_adapter.get(os.path.join(table_path, "@validation_info"))

    read_size = validation_info["validation_data_size"]
    assert read_size == size

    read_dim = validation_info["validation_data_dim"]
    assert read_dim == dim

    read_embed = validation_info["validation_data_zero_embedding"]
    gt = torch.FloatTensor([42.0] * dim) if size > 0 else torch.FloatTensor([0.0] * dim)
    assert torch.allclose(torch.FloatTensor(read_embed), gt)


async def check_embeddings(table_path: str, dim: int, size: int):
    keys = set()
    with open(table_path, "rb") as f:
        for _ in range(size):
            key = int.from_bytes(f.read(8), byteorder='little', signed=True)
            keys.add(key)
            read_dim = int.from_bytes(f.read(2), byteorder='little', signed=False)
            assert read_dim == dim
            for __ in range(dim):
                data = struct.unpack('<f', f.read(4))[0]
                assert data == key if key > 0 else 42
    assert keys == set(range(size))


@pytest.mark.parametrize('hash_embeddings_num', [0, 1, 10])
@pytest.mark.parametrize('hash_embedding_size', [0, 1000, 10**4])
@pytest.mark.parametrize('table_type', ["adam"])
@pytest.mark.parametrize('chunk_size', [1024, 10**6])
@pytest.mark.asyncio
async def test_hash_embedding_processing(hash_embeddings_num, hash_embedding_size, table_type, chunk_size):
    model = TestModel(
        hash_embeddings_num=hash_embeddings_num,
        hash_embeddings_size=hash_embedding_size,
        inputs_num=0,
        table_type=table_type
    )

    branch_name = "branch1"
    artifact = "art1"
    fs_adapter = CypressAdapterMock()
    with tempfile.TemporaryDirectory() as tmp_dir:
        await process_model(model, chunk_size=chunk_size, model_dir=tmp_dir, branch=branch_name, artifact=artifact)
        artifact_path = artifact_values_path(fs_adapter, tmp_dir, branch_name, artifact)
        value_path = fs_adapter.path_join(artifact_path, "0")
        model_path = fs_adapter.path_join(value_path, "EmptyModel")
        assert await fs_adapter.exists(model_path)
        hash_embeddings_path = fs_adapter.path_join(model_path, "hash_embeddings")
        if hash_embeddings_num == 0:
            assert await fs_adapter.exists(hash_embeddings_path)
            assert await fs_adapter.listdir(hash_embeddings_path) == ["shared_embedding_info"]
            return
        for i in range(hash_embeddings_num):
            name = "hash_emebdding_{}".format(i)
            table_path = os.path.join(hash_embeddings_path, ".".join([name, "serialized_table"]))
            await check_embedding_validation(table_path=table_path, dim=i + 10, size=hash_embedding_size)
            await check_embeddings(table_path=table_path, dim=i + 10, size=hash_embedding_size)
