import io
import pytest
from typing import Dict
import torch
import os
import contextlib
import yaml
import numpy as np

from ads_pytorch.yt.file_system_adapter_mock import CypressAdapterMock
from ads_pytorch.tools.async_worker_pool import BaseAsyncWorkerPool, BaseAsyncWorker

from deploy.production_model import TensorApplicableModel
from deploy.processors.tsar_tensor_processor import TsarTensorProcessor, AdvMachineTensorProcessor


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


def async_mocks():
    return DiskSavePool(workers_count=5), CypressAdapterMock()


class SomeTsarTensorApplicableModel(TensorApplicableModel):
    def __init__(self):
        pass

    @property
    def model_config(self) -> Dict:
        return dict()

    def get_final_tsar_tensor(self) -> torch.Tensor:
        return torch.arange(3 * 4 * 5, dtype=torch.float32).view(3, 4, 5)


class SomeAdvMachineTensorApplicableModel(TensorApplicableModel):
    def __init__(self):
        pass

    @property
    def model_config(self) -> Dict:
        return dict()

    def get_final_tsar_tensor(self) -> torch.Tensor:
        return torch.FloatTensor([1, 2])


@contextlib.asynccontextmanager
async def enter_context(fs_adapter: CypressAdapterMock, save_pool: DiskSavePool):
    async with contextlib.AsyncExitStack() as stack:
        await stack.enter_async_context(fs_adapter)
        await stack.enter_async_context(save_pool)
        tx = await stack.enter_async_context(fs_adapter.transaction_ctx())
        yield tx


@pytest.mark.asyncio
async def test_tsar_tesnor_applicable_model():
    save_pool, fs_adapter = async_mocks()
    tensor_applicable = SomeTsarTensorApplicableModel()
    processor = TsarTensorProcessor(
        file_system_adapter=fs_adapter,
        save_pool=save_pool
    )

    await processor.process_model(
        tensor_applicable,
        base_dir='.',
        name='test_tsar_tesnor_applicable_model',
        parent_tx=''
    )

    tensor = yaml.load(open('test_tsar_tesnor_applicable_model/deep_part/model_state', 'r'))

    buffer = io.BytesIO()
    np.save(buffer, np.arange(3 * 4 * 5, dtype=np.float32).reshape(3, 4, 5))

    assert tensor['state_dict']['tensor'] == buffer.getvalue()


@pytest.mark.asyncio
async def test_adv_machine_tesnor_applicable_model():
    save_pool, fs_adapter = async_mocks()
    tensor_applicable = SomeAdvMachineTensorApplicableModel()
    processor = AdvMachineTensorProcessor(
        file_system_adapter=fs_adapter,
        save_pool=save_pool
    )

    await processor.process_model(
        tensor_applicable,
        base_dir='.',
        name='test_adv_machine_tesnor_applicable_model',
        parent_tx=''
    )

    tensor = yaml.load(open('test_adv_machine_tesnor_applicable_model/deep_part/model_state', 'r'))
    assert tensor['state_dict']['tensor'] == '1.00000000 2.00000000'
