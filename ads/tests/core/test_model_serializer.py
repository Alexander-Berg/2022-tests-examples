# FIXME: add tests on serialization with empty items

import random
import shutil
import tempfile
from typing import List, Optional
import pytest
import contextlib
from uuid import uuid4
import torch
import os
import dataclasses
from ads_pytorch.cpp_lib import libcpp_lib
from ads_pytorch.tools.async_worker_pool import BaseAsyncWorkerPool, BaseAsyncWorker
from ads_pytorch.tools.buffered_parallel_reader import BufferedParallelReader
from ads_pytorch.core.model_serializer import ModelLoader, ModelSaver, ModelInfo
from ads_pytorch.core.file_system_adapter import BaseFileSystemAdapter
from ads_pytorch.core.psmodel import BaseParameterServerModule
from ads_pytorch.hash_embedding.hash_embedding import HashEmbedding, create_hash_table, create_item
from ads_pytorch.core.optim import ParameterServerOptimizer
from ads_pytorch.hash_embedding.optim import EmbeddingAdamOptimizer, create_optimizer


def make_hash_table(dim):
    return create_hash_table("adam", dim)


def make_item(dim):
    return create_item("adam", dim)


def make_optimizer(*args, **kwargs):
    return EmbeddingAdamOptimizer(*args, **kwargs)


class BaseEmbeddingModel(BaseParameterServerModule):
    def __init__(self):
        super(BaseEmbeddingModel, self).__init__()

    def async_forward(self, inputs, indices=None):
        return [self.embeddings[i](inputs[2 * i], inputs[2 * i + 1]) for i in range(len(self.embeddings))]

    def sync_forward(self, input_tensors):
        raise NotImplementedError()


class CrossNetworkEmbedding(BaseEmbeddingModel):
    """
    CrossNetwork has the following recurrent update equations
    x_{i+1} = x_0(x_{i}.dot(w_{i}) + 1) + b_{i}

    For the DesiredOnlineProperty, we have to obtain zero prediction from zero initial initialization and - more
    important - keep model trainable

    Usually, we do not use the CrossNetworkEmbedding inside some other equations.
    Usually, we just put it with dot product
    """
    def __init__(self, embedding_dims: List[int], depth: int):
        super(CrossNetworkEmbedding, self).__init__()
        self.embedding_dims = embedding_dims
        self.depth = depth
        self.embeddings = torch.nn.ModuleList([HashEmbedding(make_hash_table(d)) for d in self.embedding_dims])
        total_len = sum(self.embedding_dims)
        self.biases = torch.nn.ParameterList(
            [torch.nn.Parameter(torch.FloatTensor(total_len, 1)) for _ in range(depth)])
        self.weights = torch.nn.ParameterList(
            [torch.nn.Parameter(torch.FloatTensor(total_len, 1)) for _ in range(depth)])

        # orthogonal initialization of biases with respect to weights
        # This will make sure that final output of the network in case of all-zeroes embedding is
        # just sum of biases - "mean" vector
        tmp = torch.zeros(total_len, 2 * depth)
        tmp.requires_grad = False
        torch.nn.init.orthogonal_(tmp)
        for i, p in enumerate(self.weights):
            p.data = tmp[:, i].unsqueeze(1)

        for i, p in enumerate(self.biases):
            p.data = tmp[:, depth + i]

    def sync_forward(self, input_tensors):
        x_0 = torch.cat(input_tensors, dim=1)
        cur_x = x_0
        for i in range(self.depth):
            matmul_with_weights = cur_x.matmul(self.weights[i])
            cur_x = x_0 * matmul_with_weights + self.biases[i] + cur_x
        return cur_x


class CrossNetwork(CrossNetworkEmbedding):
    def __init__(self, embedding_dims: List[int], depth: int):
        super(CrossNetwork, self).__init__(embedding_dims, depth)
        total_dim = sum(self.embedding_dims)
        self.final_W = torch.nn.Linear(total_dim, 1, bias=True)
        torch.nn.init.orthogonal_(self.final_W.weight)
        self.final_W.bias.data.fill_(random.random())

    def sync_forward(self, input_tensors):
        cur_x = super(CrossNetwork, self).sync_forward(input_tensors)
        return self.final_W(cur_x)


class NestedCrossNetworkModel(BaseParameterServerModule):
    def __init__(self):
        super(NestedCrossNetworkModel, self).__init__()
        self.cn1 = CrossNetwork([30] * 10, 6)
        self.cn2 = CrossNetwork([30] * 10, 6)
        self.cn3 = CrossNetwork([30] * 10, 6)
        self.bias = torch.nn.Parameter(torch.FloatTensor([1]))
        self.bias.data.fill_(random.random())

    def async_forward(self, inputs):
        return self.cn1.async_forward(inputs) + self.cn2.async_forward(inputs) + self.cn3.async_forward(inputs)

    def sync_forward(self, async_outputs):
        split = int(len(async_outputs) / 3)
        return self.cn1.sync_forward(async_outputs[:split]) + \
               self.cn2.sync_forward(async_outputs[split: 2*split]) + \
               self.cn3.sync_forward(async_outputs[-split:]) + self.bias


@pytest.fixture
def folder():
    d = tempfile.mkdtemp()
    try:
        yield d
    finally:
        shutil.rmtree(d)


class DiskFileSystemAdapter(BaseFileSystemAdapter[str, str]):
    def __init__(self):
        super(DiskFileSystemAdapter, self).__init__()
        # This will ensure that ALL operations with the model are performed
        # under single transaction
        self._tx = None

    def _validate_tx(self, tx):
        assert tx is not None
        if self._tx is None:
            self._tx = tx
        assert self._tx == tx

    def path_join(self, *paths: str) -> str:
        return os.path.join(*paths)

    async def rmtree(self, folder: str, tx: Optional[str] = None) -> None:
        self._validate_tx(tx)
        shutil.rmtree(folder)

    async def concatenate(self, source_paths: List[str], destination_path: str, tx: Optional[str] = None) -> None:
        self._validate_tx(tx)
        with open(destination_path, "wb") as fdst:
            for src in source_paths:
                with open(src, "rb") as fsrc:
                    shutil.copyfileobj(fsrc=fsrc, fdst=fdst)

    async def create_directory(self, path: str, tx: Optional[str] = None):
        self._validate_tx(tx)

    async def create_file(self, path: str, tx: Optional[str] = None):
        self._validate_tx(tx)

    async def _generate_temp_directory_name(self):
        return tempfile.mkdtemp()

    @contextlib.asynccontextmanager
    async def transaction_ctx(self, parent_tx: Optional[str] = None) -> str:
        yield str(uuid4())


# Testing savers: save to folder on disk
class DiskSaveWorker(BaseAsyncWorker):
    async def _do_job(self, data_stream, path, tx):
        assert tx is not None
        if not os.path.exists(os.path.dirname(path)):
            os.makedirs(os.path.dirname(path))
        with open(path, 'wb') as f:
            if isinstance(data_stream, (str, bytes, libcpp_lib.StringHandle, memoryview)):
                f.write(bytes(data_stream))
            else:
                async for data in data_stream:
                    f.write(data)


class DiskSavePool(BaseAsyncWorkerPool):
    def create_new_worker(self, new_rank: int):
        return DiskSaveWorker(new_rank)


class DiskLoadWorker(BaseAsyncWorker):
    async def _do_job(self, path):
        with open(path, 'rb') as f:
            return f.read()


class DiskLoadPool(BaseAsyncWorkerPool):
    def create_new_worker(self, new_rank: int):
        return DiskLoadWorker(new_rank)


##########################################################
#                 CHUNKED DESERIALIZATION                #
##########################################################


@dataclasses.dataclass(frozen=True)
class DiskFilePath:
    path: str
    offset: int
    size: int

    def __post_init__(self):
        file_size = os.path.getsize(self.path)
        if self.offset + self.size > file_size:
            raise ValueError("offset + size exceeds file size {} for {}".format(file_size, self))


class ChunkedDiskLoadWorker(BaseAsyncWorker):
    async def _do_job(self, path: DiskFilePath):
        with open(path.path, 'rb') as f:
            f.seek(path.offset)
            return f.read(path.size)


class ChunkedDiskLoadPool(BaseAsyncWorkerPool):
    def create_new_worker(self, new_rank: int):
        return ChunkedDiskLoadWorker(new_rank)


@dataclasses.dataclass
class ChunkFileSplitter:
    chunk_size: int

    async def __call__(self, path: str):
        file_size = os.path.getsize(path)
        # First, slice table to chunks
        cur_offset = 0

        while cur_offset < file_size:
            _cur_size = min(self.chunk_size, file_size - cur_offset)
            yield DiskFilePath(path, offset=cur_offset, size=_cur_size)
            cur_offset += _cur_size


###############################################
#                    TESTS                    #
###############################################


@pytest.mark.parametrize('save_chunk', [1 << 5, 1 << 30], ids=['SaveChunked', 'SaveSimple'])
@pytest.mark.parametrize('chunked', [True, False], ids=['LoadChunked', 'LoadSimple'])
@pytest.mark.asyncio
async def test_save_load_model(folder, chunked, save_chunk):
    model = NestedCrossNetworkModel()
    for embed in model.hash_embedding_parameters():
        for item_id in range(50):
            item = make_item(30)
            item.first_moment = torch.FloatTensor([item_id] * 30)
            item.w = torch.FloatTensor([item_id] * 30)
            item.second_moment = torch.FloatTensor([item_id] * 30)
            embed.hash_table.insert_item(item_id, item)

    saver = ModelSaver(
        folder=folder,
        chunk_size=save_chunk,
        file_system_adapter=DiskFileSystemAdapter(),
        save_pool=DiskSavePool()
    )
    await saver.save_model(model)

    if chunked:
        loader = ModelLoader(
            folder=folder,
            file_system_adapter=DiskFileSystemAdapter(),
            buffered_reader=BufferedParallelReader(ChunkedDiskLoadPool()),
            download_splitter=ChunkFileSplitter(chunk_size=100)
        )
    else:
        loader = ModelLoader(
            folder=folder,
            file_system_adapter=DiskFileSystemAdapter(),
            buffered_reader=BufferedParallelReader(DiskLoadPool())
        )
    model2 = NestedCrossNetworkModel()

    # Sanity check if we refactor tests: we need to check that initialization is pure random
    for p, reference_p in zip(model2.deep_parameters(), model.deep_parameters()):
        assert not torch.all(torch.eq(p.data, reference_p.data))

    await loader.load_model(model2)

    # Now, we load from dict and check we loaded all properly
    for p, reference_p in zip(model2.deep_parameters(), model.deep_parameters()):
        assert torch.all(torch.eq(p.data, reference_p.data))

    for p, reference_p in zip(model2.hash_embedding_parameters(), model.hash_embedding_parameters()):
        assert p.hash_table.size() == reference_p.hash_table.size()
        for item_id in range(50):
            item = p.hash_table.lookup_item(item_id)
            reference_item = reference_p.hash_table.lookup_item(item_id)
            assert torch.all(torch.eq(item.first_moment, reference_item.first_moment))
            assert torch.all(torch.eq(item.w, reference_item.w))
            assert torch.all(torch.eq(item.second_moment, reference_item.second_moment))


@pytest.mark.parametrize('joint_save', [True, False])
@pytest.mark.parametrize('joint_load', [True, False])
@pytest.mark.asyncio
async def test_optimizer_consistency_with_model(folder, joint_save, joint_load):
    torch.manual_seed(123456)
    """
    In this extended test, we check not only proper serialize/deserialize, but also consistency of model and optimizer:
    doing forward/backward pass + optimizer step will yield same results
    """
    model = NestedCrossNetworkModel()
    for embed in model.hash_embedding_parameters():
        for i in range(1, 7):
            item = make_item(30)
            item.w = torch.rand(30)
            embed.hash_table.insert_item(i, item)

    optimizer = ParameterServerOptimizer(
        torch.optim.Adam(model.deep_parameters()),
        make_optimizer(model.hash_embedding_parameters())
    )

    saver = ModelSaver(folder=folder, file_system_adapter=DiskFileSystemAdapter(), save_pool=DiskSavePool())
    if joint_save:
        await saver.save_model_and_optimizer(model=model, optimizer=optimizer)
    else:
        await saver.save_model(model)
        await saver.save_optimizer(optimizer)

    loader = ModelLoader(folder=folder, file_system_adapter=DiskFileSystemAdapter(), buffered_reader=BufferedParallelReader(DiskLoadPool()))
    model2 = NestedCrossNetworkModel()
    optimizer2 = ParameterServerOptimizer(
        torch.optim.Adam(model2.deep_parameters()),
        make_optimizer(model2.hash_embedding_parameters())
    )

    # Sanity check if we refactor tests: we need to check that initialization is pure random
    for p, reference_p in zip(model2.deep_parameters(), model.deep_parameters()):
        assert not torch.all(torch.eq(p.data, reference_p.data))

    if joint_load:
        await loader.load_model_and_optimizer(model=model2, optimizer=optimizer2)
    else:
        await loader.load_model(model2)
        await loader.load_optimizer(optimizer2)

    # Now, we load from dict and check we loaded all properly
    for p, reference_p in zip(model2.deep_parameters(), model.deep_parameters()):
        assert torch.all(torch.eq(p.data, reference_p.data))

    for (p, reference_p) in zip(model2.hash_embedding_parameters(), model.hash_embedding_parameters()):
        for i in [1, 2, 3, 4, 5, 6]:
            assert p.hash_table.size() == reference_p.hash_table.size()
            item = p.hash_table.lookup_item(i)
            reference_item = reference_p.hash_table.lookup_item(i)
            assert torch.all(torch.eq(item.first_moment, reference_item.first_moment))
            assert torch.all(torch.eq(item.w, reference_item.w))
            assert torch.all(torch.eq(item.second_moment, reference_item.second_moment))

    # Finally, we check that doing forward/backward passes yield similar results for both models
    data = torch.LongTensor([1, 2, 3, 2, 4, 5, 6, 1, 5, 2, 3])
    data_len = torch.IntTensor([4, 3, 4])
    inputs = [data, data_len] * 10
    y = torch.FloatTensor([1, 2, 3])
    loss = torch.nn.MSELoss()

    for i in range(2):
        model.zero_grad()
        prediction = model(inputs)
        loss(prediction, y).backward()
        optimizer.step()

    # This is the sanity check that our model has trained and parameters have changed
    for p, reference_p in zip(model2.deep_parameters(), model.deep_parameters()):
        assert not torch.all(torch.eq(p.data, reference_p.data))

    for p, reference_p in zip(model2.hash_embedding_parameters(), model.hash_embedding_parameters()):
        for i in [1, 2, 3, 4, 5, 6]:
            item = p.hash_table.lookup_item(i)
            reference_item = reference_p.hash_table.lookup_item(i)
            assert not torch.all(torch.eq(item.first_moment, reference_item.first_moment))
            assert not torch.all(torch.eq(item.w, reference_item.w))
            assert not torch.all(torch.eq(item.second_moment, reference_item.second_moment))

    for i in range(2):
        model2.zero_grad()
        prediction2 = model2(inputs)
        loss(prediction2, y).backward()
        optimizer2.step()

    # Compare parameters after loading
    for p, reference_p in zip(model2.deep_parameters(), model.deep_parameters()):
        assert torch.allclose(p.data, reference_p.data, atol=1e-02, rtol=1e-02)

    for p, reference_p in zip(model2.hash_embedding_parameters(), model.hash_embedding_parameters()):
        assert p.hash_table.size() == reference_p.hash_table.size()
        for i in [1, 2, 3, 4, 5, 6]:
            item = p.hash_table.lookup_item(i)
            reference_item = reference_p.hash_table.lookup_item(i)
            assert torch.allclose(item.first_moment, reference_item.first_moment, atol=1e-02, rtol=1e-01)
            assert torch.allclose(item.w, reference_item.w, atol=1e-03, rtol=1e-03)
            assert torch.allclose(item.second_moment, reference_item.second_moment, atol=1e-02, rtol=1e-02)


############################################################
#                 SAVE POOL TEST EXCEPTIONS                #
############################################################


class ExceptionWorker(BaseAsyncWorker):
    async def _do_job(self, data_stream, path, tx):
        assert tx is not None
        raise RuntimeError


class ExceptionSavePool(BaseAsyncWorkerPool):
    def create_new_worker(self, new_rank: int):
        return ExceptionWorker(new_rank)


@pytest.mark.parametrize('joint_save', [True, False], ids=['Joint', 'Separate'])
@pytest.mark.asyncio
async def test_fail_save(folder, joint_save):
    model = NestedCrossNetworkModel()
    for i, embed in enumerate(model.hash_embedding_parameters()):
        item = make_item(30)
        item.first_moment = torch.FloatTensor([i] * 30)
        item.w = torch.FloatTensor([i] * 30)
        item.second_moment = torch.FloatTensor([i] * 30)
        embed.hash_table.insert_item(i, item)

    optimizer = ParameterServerOptimizer(
        torch.optim.Adam(model.deep_parameters()),
        make_optimizer(model.hash_embedding_parameters())
    )

    saver = ModelSaver(folder=folder, save_pool=ExceptionSavePool(), file_system_adapter=DiskFileSystemAdapter())
    if joint_save:
        with pytest.raises(RuntimeError):
            await saver.save_model_and_optimizer(model=model, optimizer=optimizer)
    else:
        with pytest.raises(RuntimeError):
            await saver.save_model(model)
        with pytest.raises(RuntimeError):
            await saver.save_optimizer(optimizer)


class CallCounterExceptionWorker(DiskSaveWorker):
    def __init__(self, *args, **kwargs):
        self.path_call_counters = {}
        super(CallCounterExceptionWorker, self).__init__(*args, **kwargs)

    async def _do_job(self, data_stream, path, tx):
        assert tx is not None
        for i in range(3):
            res = await super(CallCounterExceptionWorker, self)._do_job(data_stream, path, tx)
        return res


class CallCounterExceptionSavePool(BaseAsyncWorkerPool):
    def create_new_worker(self, new_rank: int):
        return CallCounterExceptionWorker(new_rank)


@pytest.mark.parametrize('joint_save', [True, False], ids=['Joint', 'Separate'])
@pytest.mark.asyncio
async def test_retry_save(folder, joint_save):
    model = NestedCrossNetworkModel()
    for i, embed in enumerate(model.hash_embedding_parameters()):
        item = make_item(30)
        item.first_moment = torch.FloatTensor([i] * 30)
        item.w = torch.FloatTensor([i] * 30)
        item.second_moment = torch.FloatTensor([i] * 30)
        embed.hash_table.insert_item(i, item)

    optimizer = ParameterServerOptimizer(
        torch.optim.Adam(model.deep_parameters()),
        make_optimizer(model.hash_embedding_parameters())
    )

    save_pool = CallCounterExceptionSavePool()
    saver = ModelSaver(folder=folder, chunk_size=10 ** 10, save_pool=save_pool, file_system_adapter=DiskFileSystemAdapter())
    if joint_save:
        await saver.save_model_and_optimizer(model=model, optimizer=optimizer)
    else:
        await saver.save_model(model)
        await saver.save_optimizer(optimizer)


############################################################
#                 LOAD POOL TEST EXCEPTIONS                #
############################################################


class ExceptionLoadWorker(BaseAsyncWorker):
    async def _do_job(self, path):
        raise RuntimeError


class ExceptionLoadPool(BaseAsyncWorkerPool):
    def create_new_worker(self, new_rank: int):
        return ExceptionLoadWorker(new_rank)


@pytest.mark.parametrize('joint_save', [True, False])
@pytest.mark.parametrize('joint_load', [True, False])
@pytest.mark.asyncio
async def test_save_load_exception_pool(folder, joint_save, joint_load):
    """
    In this extended test, we check not only proper serialize/deserialize, but also consistency of model and optimizer:
    doing forward/backward pass + optimizer step will yield same results
    """
    model = NestedCrossNetworkModel()
    for i, embed in enumerate(model.hash_embedding_parameters()):
        item = make_item(30)
        item.first_moment = torch.FloatTensor([i] * 30)
        item.w = torch.FloatTensor([i] * 30)
        item.second_moment = torch.FloatTensor([i] * 30)
        embed.hash_table.insert_item(i, item)

    optimizer = ParameterServerOptimizer(
        torch.optim.Adam(model.deep_parameters()),
        make_optimizer(model.hash_embedding_parameters())
    )

    saver = ModelSaver(folder=folder, file_system_adapter=DiskFileSystemAdapter(), save_pool=DiskSavePool())
    if joint_save:
        await saver.save_model_and_optimizer(model=model, optimizer=optimizer)
    else:
        await saver.save_model(model)
        await saver.save_optimizer(optimizer)

    loader = ModelLoader(folder=folder, file_system_adapter=DiskFileSystemAdapter(), buffered_reader=BufferedParallelReader(ExceptionLoadPool()))
    model2 = NestedCrossNetworkModel()
    optimizer2 = ParameterServerOptimizer(
        torch.optim.Adam(model2.deep_parameters()),
        make_optimizer(model2.hash_embedding_parameters())
    )

    # Sanity check if we refactor tests: we need to check that initialization is pure random
    for p, reference_p in zip(model2.deep_parameters(), model.deep_parameters()):
        assert not torch.all(torch.eq(p.data, reference_p.data))

    if joint_load:
        with pytest.raises(RuntimeError):
            await loader.load_model_and_optimizer(model=model2, optimizer=optimizer2)
    else:
        with pytest.raises(RuntimeError):
            await loader.load_model(model2)
        with pytest.raises(RuntimeError):
            await loader.load_optimizer(optimizer2)


@pytest.mark.asyncio
async def test_chunked_inconsistency(folder):
    # This checks if our chunker has a bug and we did not read whole hash embedding
    # table

    async def bad_file_splitter(path):
        yield DiskFilePath(path, offset=0, size=1)

    model = NestedCrossNetworkModel()
    for embed in model.hash_embedding_parameters():
        for item_id in range(50):
            item = make_item(30)
            item.first_moment = torch.FloatTensor([item_id] * 30)
            item.w = torch.FloatTensor([item_id] * 30)
            item.second_moment = torch.FloatTensor([item_id] * 30)
            embed.hash_table.insert_item(item_id, item)

    saver = ModelSaver(
        folder=folder,
        chunk_size=1 << 30,
        file_system_adapter=DiskFileSystemAdapter(),
        save_pool=DiskSavePool()
    )
    await saver.save_model(model)

    loader = ModelLoader(
        folder=folder,
        file_system_adapter=DiskFileSystemAdapter(),
        buffered_reader=BufferedParallelReader(ChunkedDiskLoadPool()),
        download_splitter=bad_file_splitter
    )

    model2 = NestedCrossNetworkModel()

    with pytest.raises(Exception):
        await loader.load_model(model2)


@pytest.mark.asyncio
async def test_save_prediction_mode(folder):
    model = NestedCrossNetworkModel()
    for embed in model.hash_embedding_parameters():
        for item_id in range(50):
            item = make_item(30)
            item.first_moment = torch.FloatTensor([item_id + 1] * 30)
            item.w = torch.FloatTensor([item_id + 1] * 30)
            item.second_moment = torch.FloatTensor([item_id + 1] * 30)
            embed.hash_table.insert_item(item_id, item)

        assert embed.hash_table.size() == 50

    saver = ModelSaver(
        folder=folder,
        chunk_size=1 << 30,
        file_system_adapter=DiskFileSystemAdapter(),
        save_pool=DiskSavePool(),
        save_mode="predict"
    )
    await saver.save_model(model)

    with open(os.path.join(folder, "model_info"), "rb") as f:
        import pickle
        model_info: ModelInfo = pickle.load(f)
        assert all(x == 50 for x in model_info.hash_embedding_sizes.values())

    loader = ModelLoader(
        folder=folder,
        file_system_adapter=DiskFileSystemAdapter(),
        buffered_reader=BufferedParallelReader(DiskLoadPool()),
    )

    model2 = NestedCrossNetworkModel()
    for m in model2.hash_embedding_parameters():
        m.hash_table = create_hash_table("prediction", m.hash_table.dim())

    await loader.load_model(model2)

    for m in model2.modules():
        if isinstance(m, HashEmbedding):
            assert m.size() == 50  # the first item should not be saved because it's all-zeros
            # and it should be considered as empty
            for item_id in range(50):
                item = m.lookup_item(item_id)
                assert torch.allclose(item.w, torch.FloatTensor([item_id + 1] * 30))


###############################################
#             CORRUPTED FILES TESTS           #
###############################################


@pytest.mark.parametrize("save_mode", ["train", "predict"])
@pytest.mark.asyncio
async def test_files_inconsistency(folder, save_mode):
    # Check for cor
    model = NestedCrossNetworkModel()
    for embed in model.hash_embedding_parameters():
        for item_id in range(50):
            item = make_item(30)
            item.first_moment = torch.FloatTensor([item_id] * 30)
            item.w = torch.FloatTensor([item_id] * 30)
            item.second_moment = torch.FloatTensor([item_id] * 30)
            embed.hash_table.insert_item(item_id, item)

    saver = ModelSaver(
        folder=folder,
        chunk_size=1 << 30,
        file_system_adapter=DiskFileSystemAdapter(),
        save_pool=DiskSavePool(),
        save_mode=save_mode
    )

    await saver.save_model(model)
    for path in os.listdir(folder):
        with tempfile.TemporaryDirectory() as work_folder:
            shutil.copytree(folder, os.path.join(work_folder, "_tmp"))
            abs_path = os.path.join(work_folder, "_tmp", path)
            size = os.path.getsize(abs_path)
            with open(abs_path, "rb") as f:
                readed = f.read(size // 2)
            with open(abs_path, "wb") as f:
                f.write(readed)

            loader = ModelLoader(
                folder=folder,
                file_system_adapter=DiskFileSystemAdapter(),
                buffered_reader=BufferedParallelReader(ChunkedDiskLoadPool()),
            )

            model2 = NestedCrossNetworkModel()
            if save_mode == "predict":
                for m in model2.hash_embedding_parameters():
                    m.hash_table = create_hash_table("prediction", m.hash_table.dim())

            with pytest.raises(Exception):
                await loader.load_model(model2)
