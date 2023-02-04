import pytest

import os
import torch
import numpy as np
import yaml
import tempfile
import contextlib
from typing import Tuple, Dict
from ads_pytorch.hash_embedding import HashEmbedding

from deploy.production_model import ApplicableModel, EmbeddingDescriptor, ValidationDescriptor
from deploy.processors.eigenlib_processor import EigenLibProcessor, Matrix
from ads_pytorch.tools.async_worker_pool import BaseAsyncWorkerPool, BaseAsyncWorker
from ads_pytorch.yt.file_system_adapter_mock import CypressAdapterMock


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


@contextlib.asynccontextmanager
async def enter_context(fs_adapter: CypressAdapterMock, save_pool: DiskSavePool):
    async with contextlib.AsyncExitStack() as stack:
        await stack.enter_async_context(fs_adapter)
        await stack.enter_async_context(save_pool)
        tx = await stack.enter_async_context(fs_adapter.transaction_ctx())
        yield tx


class ConstantModule(torch.nn.Module):
    def __init__(self, output_size: Tuple[int], output_value: float):
        super(ConstantModule, self).__init__()
        self._output_size = output_size
        self._output_value = output_value

    def forward(self, *input):
        return torch.ones(self._output_size) * self._output_value


class EmptyModel(ApplicableModel):
    def __init__(self, inputs_num: int = 0, inputs_size: int = 1, output_size: Tuple[int] = (1,), output_value: float = 0):
        super(EmptyModel, self).__init__()
        self._inputs_num = inputs_num
        self._inputs_size = inputs_size
        self._output_size = output_size
        self._output_value = output_value

    def get_hash_embedding_mapping(self) -> Dict[str, EmbeddingDescriptor]:
        return {}

    def get_deep_part(self) -> torch.nn.Module:
        return ConstantModule(output_size=self._output_size, output_value=self._output_value)

    def process_parameter_name(self, name: str) -> str:
        return name

    def get_validation_inputs(self) -> ValidationDescriptor:
        inputs = {str(i): torch.zeros((10, self._inputs_size + i)) for i in range(self._inputs_num)}
        result = self.get_deep_part()(inputs)
        return ValidationDescriptor(inputs=inputs, result=result)

    @property
    def model_config(self) -> Dict:
        return {
            "output_dim": 50
        }


class DummyModule(torch.nn.Module):
    def __init__(self):
        super(DummyModule, self).__init__()
        self.linear = torch.nn.Linear(7, 13)
        self.linear2 = torch.nn.Linear(10, 1, False)
        self.drop = torch.nn.Linear(100, 100, bias=False)

        self.param = torch.nn.Parameter(torch.ones(15))
        self.param_dict = torch.nn.ParameterDict({'named_param': torch.nn.Parameter(torch.ones(11) * 11)})

        self.model_list = torch.nn.ModuleList([torch.nn.LayerNorm(5)])

    def forward(self, *input):
        return torch.zeros(1)


class DummyModel(ApplicableModel):
    def __init__(self):
        super(DummyModel, self).__init__()
        self.deep_part = DummyModule()

    def get_hash_embedding_mapping(self) -> Dict[str, HashEmbedding]:
        return {}

    def get_deep_part(self) -> torch.nn.Module:
        return self.deep_part

    def get_validation_inputs(self) -> ValidationDescriptor:
        return ValidationDescriptor(inputs={}, result=self.get_deep_part()({}))

    def process_parameter_name(self, name: str) -> str:
        if name == "drop.weight":
            return ""
        return name

    @property
    def model_config(self) -> Dict:
        return {
            "output_dim": 50
        }


def create_async_mocks():
    return DiskSavePool(workers_count=5), CypressAdapterMock()


def load_model_state(dir: str, model_state_name: str):
    with open(os.path.join(dir, model_state_name), "r") as f:
        model_state = yaml.load(f.read())
    return model_state


def save_all_from_model_state(model_state: Dict[str, Dict[str, bytes]], dir: str):
    model_state_parts = ["state_dict", "validation_info", "model_config"]
    for part in model_state_parts:
        for file_name, data in model_state[part].items():
            with open(os.path.join(dir, file_name), "wb") as f:
                f.write(data)


@pytest.mark.parametrize('inputs_num', [1, 10], ids=['inputs_num={}'.format(i) for i in [1, 10]])
@pytest.mark.parametrize('inputs_size', [1, 100], ids=['inputs_size={}'.format(i) for i in [1, 100]])
@pytest.mark.asyncio
async def test_validation_input(inputs_num, inputs_size):
    model = EmptyModel(inputs_num=inputs_num, inputs_size=inputs_size)
    save_pool, fs_adapter = create_async_mocks()
    processor = EigenLibProcessor(fs_adapter, save_pool, model)
    with tempfile.TemporaryDirectory() as temp_dir:
        async with enter_context(fs_adapter, save_pool) as tx:
            await processor.process(model.get_deep_part(), model.get_validation_inputs(), temp_dir, tx=tx)
        model_state = load_model_state(temp_dir, processor.model_state_name)
        save_all_from_model_state(model_state, temp_dir)
        for i in range(inputs_num):
            input_name = processor.get_validation_input_name(str(i))
            matrix = Matrix.from_dump_with_meta(os.path.join(temp_dir, input_name))
            assert matrix.shape[0] == 10
            assert matrix.shape[1] == inputs_size + i


@pytest.mark.parametrize('output_size', [(10, 10), (5, 1), (1, 100)], ids=['output_size={}'.format(i) for i in [(10, 10), (5, 1), (1, 100)]])
@pytest.mark.parametrize('output_value', [42.0, 0.0, -1.0], ids=['output_value={}'.format(v) for v in [42.0, 0.0, -1.0]])
@pytest.mark.asyncio
async def test_validation_output(output_size, output_value):
    model = EmptyModel(output_size=output_size, output_value=output_value)
    save_pool, fs_adapter = create_async_mocks()
    processor = EigenLibProcessor(fs_adapter, save_pool, model)
    with tempfile.TemporaryDirectory() as temp_dir:
        async with enter_context(fs_adapter, save_pool) as tx:
            await processor.process(model.get_deep_part(), model.get_validation_inputs(), temp_dir, tx=tx)
        model_state = load_model_state(temp_dir, processor.model_state_name)
        save_all_from_model_state(model_state, temp_dir)
        output_name = processor.validation_output_name
        matrix = Matrix.from_dump_with_meta(os.path.join(temp_dir, output_name))
        assert np.allclose(matrix.np_array, np.ones(output_size, dtype=np.float32) * output_value)


@pytest.mark.asyncio
async def test_parameters_dump():
    model = DummyModel()
    save_pool, fs_adapter = create_async_mocks()
    processor = EigenLibProcessor(fs_adapter, save_pool, model)
    with tempfile.TemporaryDirectory() as temp_dir:
        async with enter_context(fs_adapter, save_pool) as tx:
            await processor.process(model.get_deep_part(), model.get_validation_inputs(), temp_dir, tx=tx)
        model_state = load_model_state(temp_dir, processor.model_state_name)
        save_all_from_model_state(model_state, temp_dir)
        assert os.path.exists(os.path.join(temp_dir, "linear.weight"))
        assert os.path.exists(os.path.join(temp_dir, "linear.bias"))
        assert os.path.exists(os.path.join(temp_dir, "linear2.weight"))
        assert not os.path.exists(os.path.join(temp_dir, "drop.weight"))

        matrix = Matrix.from_dump_with_meta(os.path.join(temp_dir, "param"))
        assert np.allclose(matrix.np_array, np.ones(15))
        matrix = Matrix.from_dump_with_meta(os.path.join(temp_dir, "param_dict.named_param"))
        assert np.allclose(matrix.np_array, np.ones(11) * 11)
        matrix = Matrix.from_dump_with_meta(os.path.join(temp_dir, "model_list.0.weight"))
        assert np.allclose(matrix.np_array, np.ones(5))
        matrix = Matrix.from_dump_with_meta(os.path.join(temp_dir, "model_list.0.bias"))
        assert np.allclose(matrix.np_array, np.zeros(5))


class ModelWithConfig(ApplicableModel):
    def __init__(self, model_config: Dict):
        super(ModelWithConfig, self).__init__()
        self._model_config = model_config

    def get_hash_embedding_mapping(self) -> Dict[str, HashEmbedding]:
        return {}

    def get_deep_part(self) -> torch.nn.Module:
        return ConstantModule(output_size=(1,), output_value=0)

    def get_validation_inputs(self) -> ValidationDescriptor:
        return ValidationDescriptor(inputs={}, result=self.get_deep_part()({}))

    def process_parameter_name(self, name: str) -> str:
        return name

    @property
    def model_config(self) -> Dict:
        return self._model_config


@pytest.mark.asyncio
async def test_model_config():
    true_config = {"int": 10, "float": 5.0, "bool": True, "list": [1, 4.0, True], "dict": {"key": "value"}, "output_dim": 50}
    model = ModelWithConfig(true_config)
    save_pool, fs_adapter = create_async_mocks()
    processor = EigenLibProcessor(fs_adapter, save_pool, model)
    with tempfile.TemporaryDirectory() as temp_dir:
        async with enter_context(fs_adapter, save_pool) as tx:
            await processor.process(model.get_deep_part(), model.get_validation_inputs(), temp_dir, tx=tx)
        model_state = load_model_state(temp_dir, processor.model_state_name)
        save_all_from_model_state(model_state, temp_dir)
        with open(os.path.join(temp_dir, processor.model_config_name)) as f:
            config = yaml.load(f)
            assert config == true_config
