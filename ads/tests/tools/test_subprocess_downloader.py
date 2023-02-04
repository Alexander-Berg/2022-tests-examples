import pytest
import contextlib
import asyncio
import dataclasses
import random
from typing import Tuple, List, Dict
from ads_pytorch.tools.subprocess_downloader import SubprocessDownloadWorkerPool
from ads_pytorch.tools.async_worker_pool import BaseAsyncWorkerPool, BaseAsyncWorker
from ads_pytorch.tools.multiprocessing import get_multiprocessing_context


class ToCatchExc(Exception):
    pass


class DownloadWorker(BaseAsyncWorker):
    def __init__(self, data: Dict[int, List[bytes]], rank: int):
        super(DownloadWorker, self).__init__(rank=rank)
        self.data = data

    async def _do_job(self, path: int):
        if path == 100500:
            raise ToCatchExc
        return self.data[path]


class DownloadWorkerPool(BaseAsyncWorkerPool):
    def __init__(self, workers_count: int, data: Dict[int, List[bytes]]):
        super(DownloadWorkerPool, self).__init__(workers_count=workers_count, )
        self.data = data

    def create_new_worker(self, new_rank: int):
        return DownloadWorker(
            rank=new_rank,
            data=self.data
        )


@dataclasses.dataclass
class _PoolFactory:
    workers_count: int
    data: Dict[int, List[bytes]]

    async def __aenter__(self):
        self._stack = contextlib.AsyncExitStack()
        pool = await self._stack.enter_async_context(DownloadWorkerPool(workers_count=self.workers_count, data=self.data))
        return pool

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        await self._stack.__aexit__(exc_type, exc_val, exc_tb)


@pytest.mark.parametrize("workers_count", [1, 2, 100])
@pytest.mark.asyncio
async def test_download_small(workers_count):
    data_sequences = {
        i: [str(i).encode('utf-8') * 2 ** 10] * 10000
        for i in range(workers_count)
    }

    async with SubprocessDownloadWorkerPool(
        worker_count=workers_count,

        download_pool_factory=_PoolFactory(workers_count=workers_count, data=data_sequences),
        mp_ctx=get_multiprocessing_context()
    ) as pool:
        loop = asyncio.get_running_loop()
        futures = {
            i: loop.create_task(pool.assign_job(i))
            for i in range(workers_count)
        }
        await asyncio.gather(*list(futures.values()))

        for i, future in futures.items():
            data = await (await future)
            assert bytes(data) == b''.join(data_sequences[i])


@pytest.mark.asyncio
async def test_cant_start_process():

    @contextlib.asynccontextmanager
    async def _factory():
        async with DownloadWorkerPool(workers_count=1, data={0: [b'1']}) as pool:
            yield pool

    mp_ctx = get_multiprocessing_context()
    with pytest.raises(Exception):
        async with SubprocessDownloadWorkerPool(
            worker_count=1,

            download_pool_factory=_factory,
            mp_ctx=mp_ctx
        ):
            pass


@pytest.mark.parametrize("workers_count", [10])
@pytest.mark.asyncio
async def test_fail_during_download(workers_count):
    data_sequences = {
        i: [str(i).encode('utf-8') * 2 ** 10] * 10000
        for i in range(workers_count)
    }

    with pytest.raises((Exception, asyncio.CancelledError)):
        async with SubprocessDownloadWorkerPool(
            worker_count=workers_count,

            download_pool_factory=_PoolFactory(workers_count=workers_count, data=data_sequences),
            mp_ctx=get_multiprocessing_context(),
            poll_frequency=0.1
        ) as pool:
            loop = asyncio.get_running_loop()
            futures = {
                i: loop.create_task(pool.assign_job(i))
                for i in range(workers_count)
            }
            # No such key in data - will raise KeyError
            futures[100500] = loop.create_task(pool.assign_job(100500))
            await asyncio.gather(*list(futures.values()))

            for i, future in futures.items():
                await (await future)


@pytest.mark.parametrize("workers_count", [1, 3])
@pytest.mark.asyncio
async def test_download_large(workers_count):

    def _size_generator(max_size: int) -> int:
        if random.random() < 0.1:
            return 0
        return random.randint(1, max_size)

    def _string_generator(max_size: int) -> bytes:
        seq_len = _size_generator(max_size=max_size)
        return bytes(bytearray(
            random.getrandbits(8)
            for _ in range(seq_len)
        ))

    data_sequences = {
        i: [_string_generator(2 ** 10) for _ in range(1000)]
        for i in range(workers_count)
    }

    results = []

    async with SubprocessDownloadWorkerPool(
        worker_count=workers_count,

        download_pool_factory=_PoolFactory(workers_count=workers_count, data=data_sequences),
        mp_ctx=get_multiprocessing_context()
    ) as pool:
        loop = asyncio.get_running_loop()
        futures = {
            i: loop.create_task(pool.assign_job(i))
            for i in range(workers_count)
        }
        await asyncio.gather(*list(futures.values()))

        for i, future in futures.items():
            data = await (await future)
            assert bytes(data) == b''.join(data_sequences[i])
