import pytest

import os
import shutil
import torch
import tempfile
import struct
import contextlib
import asyncio
from typing import IO
from deploy.processors.serialize_processor import SerializeProcessor
from deploy.processors.base_model_processor import HashEmbeddingProcessorInput
from ads_pytorch.core.model_serializer import ModelSaverProgressTracker
from ads_pytorch.hash_embedding.hash_embedding import HashEmbedding, create_hash_table, create_item
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


@pytest.mark.parametrize('chunk_size', [30, 10000])
@pytest.mark.parametrize('dim', [1, 100])
@pytest.mark.parametrize('size', [10, 10000])
@pytest.mark.parametrize('table_type', ["adam"])
@pytest.mark.asyncio
async def test_validation_info(chunk_size, dim, size, table_type):
    hash_embedding = HashEmbedding(create_hash_table(table_type, dim))
    for i in range(1, size):
        hash_embedding.insert_item(i, create_item(table_type, dim))

    item = create_item(table_type, dim)
    item.w = torch.FloatTensor([42.0] * dim)
    hash_embedding.insert_item(0, item)

    save_pool = DiskSavePool(workers_count=5)
    file_system_adapter = CypressAdapterMock()
    processor = SerializeProcessor(
        file_system_adapter=file_system_adapter,
        save_pool=save_pool,
        chunk_size=chunk_size,
        progress_tracker=ModelSaverProgressTracker()
    )
    module_name = "hash_embedding"
    with tempfile.TemporaryDirectory() as temp_dir:
        async with enter_context(fs_adapter=file_system_adapter, save_pool=save_pool) as tx:
            await processor.process(
                [HashEmbeddingProcessorInput(name=module_name, embedding=hash_embedding, save_path=temp_dir)],
                tx=tx
            )
        validation_info = await file_system_adapter.get(os.path.join(temp_dir, ".".join([module_name, processor.serialized_table_name]), "@validation_info"))
        read_size = validation_info[processor.size_file_name]
        assert read_size == size

        read_dim = validation_info[processor.dim_file_name]
        assert read_dim == dim

        read_embed = validation_info[processor.zero_embedding_file_name]
        assert torch.allclose(torch.FloatTensor(read_embed), item.w)


def read_serialized_item(file: IO, size: int, true_dim: int):
    key = int.from_bytes(file.read(8), byteorder='little', signed=True)
    assert key in set(range(size))
    dim = int.from_bytes(file.read(2), byteorder='little', signed=False)
    assert dim == true_dim
    for _ in range(dim):
        data = struct.unpack('<f', file.read(4))[0]
        assert data == key + 10.0

    return key


@pytest.mark.parametrize('chunk_size', [30, 10000])
@pytest.mark.parametrize('dim', [1, 100])
@pytest.mark.parametrize('size', [10, 10000])
@pytest.mark.parametrize('table_type', ["adam"])
@pytest.mark.asyncio
async def test_deserialization_table(chunk_size, dim, size, table_type):
    hash_embedding = HashEmbedding(create_hash_table(table_type, dim))
    for i in range(size):
        item = create_item(table_type, dim)
        item.w = torch.FloatTensor([i + 10.0] * dim)
        hash_embedding.insert_item(i, item)

    save_pool = DiskSavePool(workers_count=5)
    file_system_adapter = CypressAdapterMock()
    processor = SerializeProcessor(
        file_system_adapter=file_system_adapter,
        save_pool=save_pool,
        chunk_size=chunk_size,
        progress_tracker=ModelSaverProgressTracker()
    )
    module_name = "hash_embedding"
    with tempfile.TemporaryDirectory() as temp_dir:
        async with enter_context(fs_adapter=file_system_adapter, save_pool=save_pool) as tx:
            await processor.process(
                [HashEmbeddingProcessorInput(name=module_name, embedding=hash_embedding, save_path=temp_dir)],
                tx=tx
            )
        with open(os.path.join(temp_dir, ".".join([module_name, processor.serialized_table_name])), 'rb') as f:
            read_keys = {read_serialized_item(f, size, dim) for _ in range(size)}
            assert read_keys == set(range(size))
