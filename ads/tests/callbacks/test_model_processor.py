import pytest
import os
from typing import List, Dict, Type, Any
import tempfile
import datetime

import torch.nn

from ads_pytorch.yt.file_system_adapter_mock import CypressAdapterMock

from ads_pytorch.online_learning.production.artifact.folder import (
    artifact_values_path
)
from ads_pytorch.online_learning.production.artifact.folder import (
    BranchDirectoryArtifactStorage
)

from ads_pytorch.hash_embedding.hash_embedding import HashEmbedding, create_hash_table
from deploy.callbacks.model_processor import ModelProcessorCallback
from deploy.processors.base_model_processor import HashEmbeddingProcessorInput
from ads_pytorch.online_learning.production.uri import ProdURI
from ads_pytorch.online_learning.production.dataset import DatetimeURI
from deploy.production_model import DeployableModel, ApplicableModel, EmbeddingDescriptor, ValidationDescriptor, EmbeddingFeature
from ads_pytorch.tools.async_worker_pool import BaseAsyncWorkerPool, BaseAsyncWorker
from ads_pytorch.yt.file_system_adapter import CypressFileSystemAdapter
from deploy.processors.base_model_processor import ProcessModelDescriptor, BaseDeepPartProcessor, BaseHashEmbeddingProcessor, ApplicableModelProcessor
from ads_pytorch import wrap_model_with_concat_wrapper


class DummyDeepProcessor(BaseDeepPartProcessor):
    def __init__(self, save_pool: BaseAsyncWorkerPool):
        self._save_pool = save_pool
        self._file_system_adapter = CypressAdapterMock()

    async def process(self, deep_part: torch.nn.Module, validation_inputs: ValidationDescriptor, path: str, tx: Any):
        data = ' '.join([name for name in validation_inputs.inputs.keys()]).encode('utf-8')
        dest = self._file_system_adapter.path_join(path, "inputs")
        future = await self._save_pool.assign_job(data, dest, tx=tx)
        await future
        dim_data = ' '.join([str(tuple(t.size())[-1]) for t in validation_inputs.inputs.values()]).encode('utf-8')
        dim_path = self._file_system_adapter.path_join(path, "dims")
        future = await self._save_pool.assign_job(dim_data, dim_path, tx=tx)
        await future


class StateDictDeepProcesspr(BaseDeepPartProcessor):
    def __init__(self, save_pool: BaseAsyncWorkerPool):
        self._save_pool = save_pool
        self._file_system_adapter = CypressAdapterMock()

    async def process(self, deep_part: torch.nn.Module, validation_inputs: ValidationDescriptor, path: str, tx: Any):
        # just dump state dict
        dest = self._file_system_adapter.path_join(path, "state_dict")
        with open(dest, "wb") as f:
            torch.save(deep_part.state_dict(), f)


class DummyHashProcessor(BaseHashEmbeddingProcessor):
    def __init__(self, save_pool: BaseAsyncWorkerPool):
        self._save_pool = save_pool
        self._file_system_adapter = CypressAdapterMock()

    async def process(self, descriptors: List[HashEmbeddingProcessorInput], tx: Any):
        for desc in descriptors:
            path = self._file_system_adapter.path_join(desc.save_path, ".".join([desc.name, "dim_file"]))
            future = await self._save_pool.assign_job(str(desc.embedding.dim()).encode('utf-8'), path, tx=tx)
            await future


# fixme join two classes


class StateDictProcessorCallback(ModelProcessorCallback):
    async def _create_model_processor_descriptors(self, model: DeployableModel) -> List[ProcessModelDescriptor]:
        inner_processor = ApplicableModelProcessor(
            deep_part_processor=StateDictDeepProcesspr(save_pool=self._upload_pool),
            hash_embedding_processor=DummyHashProcessor(save_pool=self._upload_pool),
            save_pool=self._upload_pool,
            file_system_adapter=CypressAdapterMock()
        )
        return [
            ProcessModelDescriptor(
                model_name=name,
                processor=inner_processor,
                model=inner_model,
            )
            for name, inner_model in model.get_applicable_models().items()
        ]


class DummyProcessorCallback(ModelProcessorCallback):
    async def _create_model_processor_descriptors(self, model: DeployableModel):
        inner_processor = ApplicableModelProcessor(
            deep_part_processor=DummyDeepProcessor(save_pool=self._upload_pool),
            hash_embedding_processor=DummyHashProcessor(save_pool=self._upload_pool),
            save_pool=self._upload_pool,
            file_system_adapter=CypressAdapterMock()
        )

        return [
            ProcessModelDescriptor(
                model_name=name,
                processor=inner_processor,
                model=inner_model,
            )
            for name, inner_model in model.get_applicable_models().items()
        ]


class DummyModel(ApplicableModel):
    def __init__(self, num_embeddings: int, num_inputs: int):
        super(DummyModel, self).__init__()
        self.num_embeddings = num_embeddings
        self.num_inputs = num_inputs

    def get_hash_embedding_mapping(self) -> Dict[str, EmbeddingDescriptor]:
        return {
            "emb{}".format(i): EmbeddingDescriptor(
                HashEmbedding(create_hash_table("adam", i + 100)),
                [EmbeddingFeature(name="emb{}".format(i), compute_mode="sum")]
            )
            for i in range(self.num_embeddings)
        }

    def get_deep_part(self) -> torch.nn.Module:
        return torch.nn.Module()

    def get_validation_inputs(self) -> ValidationDescriptor:
        inputs = {"f{}".format(i): torch.zeros(i + 10) for i in range(self.num_inputs)}
        return ValidationDescriptor(
            inputs=inputs,
            result=torch.zeros(100)  # not checked in test_artifact_value test. Checked in other tests
        )

    def process_parameter_name(self, name: str) -> str:
        return name

    @property
    def model_config(self) -> Dict:
        return {
            "output_dim": 50
        }


class TestModel(DeployableModel):
    def __init__(self, num_embeddings: int, num_inputs: int):
        super(TestModel, self).__init__()
        self.model = DummyModel(num_embeddings, num_inputs)

    def get_applicable_models(self) -> Dict[str, ApplicableModel]:
        return {"self": self.model}


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


async def process_model(model: DeployableModel, callback_type: Type[ModelProcessorCallback], model_dir: str, branch: str, artifact: str):
    upload_pool = DiskSavePool(5)
    storage = BranchDirectoryArtifactStorage(
        fs_adapter=CypressAdapterMock(),
        model_yt_dir=model_dir,
        branch_name=branch
    )

    callback = callback_type(
        file_system_adapter=CypressAdapterMock(),
        upload_pool=upload_pool,
        artifact_storage=storage,
        artifact_name=artifact
    )

    await callback(
        model,
        None,
        None,
        ProdURI(DatetimeURI("//home/TEST_URI", date=datetime.datetime.now()), False)
    )


@pytest.mark.parametrize('num_embeddings', [0, 1, 10])
@pytest.mark.parametrize('num_inputs', [0, 1, 10])
@pytest.mark.asyncio
async def test_artifact_value(num_embeddings, num_inputs):
    branch_name = "branch1"
    artifact_name = "processed_model"
    model = TestModel(num_embeddings=num_embeddings, num_inputs=num_inputs)
    with tempfile.TemporaryDirectory() as temp_dir:
        await process_model(
            model=model,
            callback_type=DummyProcessorCallback,
            model_dir=temp_dir,
            branch=branch_name,
            artifact=artifact_name
        )
        artifact_path = artifact_values_path(CypressAdapterMock(), temp_dir, branch_name, artifact_name)
        value_path = os.path.join(artifact_path, "0")
        assert os.path.exists(value_path)

        with open(os.path.join(value_path, "self", "deep_part", "inputs")) as f:
            inputs = f.read()
            tokens = inputs.strip().split(' ')
            if num_inputs == 0:
                assert tokens == ['']
            else:
                assert tokens == ["f{}".format(i) for i in range(num_inputs)]

        with open(os.path.join(value_path, "self", "deep_part", "dims")) as f:
            inputs = f.read()
            tokens = inputs.strip().split(' ')
            if num_inputs == 0:
                assert tokens == ['']
            else:
                assert tokens == [str(i + 10) for i in range(num_inputs)]

        for i in range(num_embeddings):
            with open(os.path.join(value_path, "self", "hash_embeddings", ".".join(["emb{}".format(i), "dim_file"]))) as f:
                dim = int(f.read())
                assert dim == i + 100

        if num_embeddings == 0:
            assert os.path.exists(os.path.join(value_path, "self", "hash_embeddings"))
            assert os.listdir(os.path.join(value_path, "self", "hash_embeddings")) == ["shared_embedding_info"]


class _LinearApplicableModel(ApplicableModel):
    def __init__(self):
        super(_LinearApplicableModel, self).__init__()
        self._net = torch.nn.Linear(10, 10)

    def get_hash_embedding_mapping(self) -> Dict[str, EmbeddingDescriptor]:
        return {}

    def get_deep_part(self) -> torch.nn.Module:
        return self._net

    def process_parameter_name(self, name: str) -> str:
        return name

    def get_validation_inputs(self) -> ValidationDescriptor:
        tensor = torch.rand(10, 10)
        return ValidationDescriptor(
            inputs={"tensor1": torch.rand(10, 10)},
            result=self._net(tensor)
        )

    @property
    def model_config(self) -> Dict:
        return {
            "output_dim": 10
        }

    def async_forward(self, inputs):
        return inputs

    def sync_forward(self, async_outputs):
        return self.model(async_outputs["tensor1"])


class ModelWithSomeParams(DeployableModel):
    def __init__(self):
        super(ModelWithSomeParams, self).__init__()
        self.model = _LinearApplicableModel()

    def get_applicable_models(self) -> Dict[str, ApplicableModel]:
        return {"self": self.model}


@pytest.mark.asyncio
async def test_concat_wrapper():
    branch_name = "branch1"
    artifact_name = "processed_model"
    model = ModelWithSomeParams()
    model2 = ModelWithSomeParams()
    model2.load_state_dict(model.state_dict())

    model2 = wrap_model_with_concat_wrapper(model=model2)
    model2_dict = model2.net.state_dict()
    for n, p in model.state_dict().items():
        assert torch.allclose(p, model2_dict[n])

    with tempfile.TemporaryDirectory() as temp_dir, tempfile.TemporaryDirectory() as temp_dir2:
        for cur_model, model_dir in [(model, temp_dir), (model2, temp_dir2)]:
            await process_model(
                model=cur_model,
                callback_type=StateDictProcessorCallback,
                model_dir=model_dir,
                branch=branch_name,
                artifact=artifact_name
            )
        deep_part_path = os.path.join(
            artifact_values_path(CypressAdapterMock(), temp_dir, branch_name, artifact_name),
            "0", "self", "deep_part", "state_dict"
        )
        deep_part_path2 = os.path.join(
            artifact_values_path(CypressAdapterMock(), temp_dir2, branch_name, artifact_name),
            "0", "self", "deep_part", "state_dict"
        )

        # sanity check we don't have bug in path taking - they are different => models differ
        assert deep_part_path != deep_part_path2

        with open(deep_part_path, "rb") as f:
            dict1 = torch.load(f)

        with open(deep_part_path2, "rb") as f:
            dict2 = torch.load(f)

        assert set(dict1.keys()) == set(dict2.keys())

        for k, p in dict1.items():
            assert torch.allclose(p, dict2[k])
