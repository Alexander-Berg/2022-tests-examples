import asyncio
import random
import sys
import dataclasses

import pytest
from ads_pytorch.tools.stream_reader import BaseStreamReader
from ads_pytorch.tools.async_worker_pool import BaseAsyncWorkerPool, BaseAsyncWorker


class InstantDownloadWorker(BaseAsyncWorker):
    async def _do_job(self, data):
        return data


class RandomDownloadWorker(BaseAsyncWorker):
    async def _do_job(self, data):
        await asyncio.sleep(random.random() * 0.1)
        return data


class MyDownloadPool(BaseAsyncWorkerPool):
    def __init__(self, downloader_count=1, cls=InstantDownloadWorker, ):
        self._cls = cls
        super(MyDownloadPool, self).__init__(workers_count=downloader_count)

    def create_new_worker(self, new_rank: int):
        return self._cls(new_rank)


class MyChunkedStreamReader(BaseStreamReader):
    async def _split_to_chunks(self, job):
        for x in job.split('\n'):
            yield x, 0


##########################################################
#                    NORMAL WORK TESTS                   #
##########################################################


@pytest.mark.asyncio
async def test_start_stop():
    pool = MyDownloadPool(2)
    async with pool as pool:
        pass


@pytest.mark.asyncio
async def test_empty_pool():
    pool = MyDownloadPool(2)
    async with pool as pool:
        reader = MyChunkedStreamReader(pool)
        for _ in range(3):
            async with reader:
                pass


@pytest.mark.asyncio
async def test_read_one():
    pool = MyDownloadPool(2)
    async with pool as pool:
        async with MyChunkedStreamReader(pool) as reader:
            iterator1 = await reader.schedule("\n".join([str(i) for i in range(5)]))
            lst = []
            async for x in iterator1:
                lst.append(x)
            assert lst == list(str(x) for x in range(5))


@pytest.mark.asyncio
async def test_multiple_time_enter_and_read_one():
    pool = MyDownloadPool(2)
    async with pool as pool:
        for _ in range(5):
            async with MyChunkedStreamReader(pool) as reader:
                iterator1 = await reader.schedule("\n".join([str(i) for i in range(5)]))
                lst = []
                async for x in iterator1:
                    lst.append(x)
                assert lst == list(str(x) for x in range(5))


@pytest.mark.asyncio
async def test_multiple_concurrent_readers_from_one_pool():
    pool = MyDownloadPool(2)

    async def read_with_pool(pool):
        async with MyChunkedStreamReader(pool) as reader:
            iterator1 = await reader.schedule("\n".join([str(i) for i in range(10)]))
            lst = []
            async for x in iterator1:
                lst.append(x)
            assert lst == list(str(x) for x in range(10))

    async with pool as pool:
        futures = [asyncio.ensure_future(read_with_pool(pool)) for _ in range(5)]
        await asyncio.wait(futures, return_when=asyncio.ALL_COMPLETED)

    for f in futures:
        await f


@pytest.mark.asyncio
@pytest.mark.parametrize('num_concurrent_readers', [1, 5, 10])
async def test_schedule_multiple_but_read_one(num_concurrent_readers):
    pool = MyDownloadPool(2)

    async def read_with_pool(pool):
        async with MyChunkedStreamReader(pool) as reader:
            iterator1 = await reader.schedule("\n".join([str(i) for i in range(5)]))
            iterator2 = await reader.schedule("\n".join([str(i) for i in range(50, 100, 2)]))
            iterator3 = await reader.schedule("My monolith string")
            lst = []
            async for x in iterator1:
                lst.append(x)
            assert lst == list(str(x) for x in range(5))

        for iterator in (iterator2, iterator3):
            with pytest.raises(TypeError):
                for it in iterator:
                    pass

    async with pool as pool:
        futures = [asyncio.ensure_future(read_with_pool(pool)) for _ in range(num_concurrent_readers)]
        await asyncio.wait(futures, return_when=asyncio.ALL_COMPLETED)

    for f in futures:
        await f


@pytest.mark.asyncio
async def test_schedule_and_read_multiple():
    pool = MyDownloadPool(2)
    async with pool as pool:
        async with MyChunkedStreamReader(pool) as reader:
            iterator1 = await reader.schedule("\n".join([str(i) for i in range(5)]))
            iterator2 = await reader.schedule("\n".join([str(i) for i in range(50, 100, 2)]))
            iterator3 = await reader.schedule("My monolith string")
            converter = list
            res1 = list()

            async for x in iterator1:
                res1.append(x)
            assert converter(res1) == converter(str(x) for x in range(5))

            res1 = list()
            async for x in iterator3:
                res1.append(x)
            assert res1 == ["My monolith string"]

            res1 = list()
            async for x in iterator2:
                res1.append(x)
            assert converter(res1) == converter(str(x) for x in range(50, 100, 2))


@pytest.mark.asyncio
async def test_schedule_and_read_multiple_random_order():
    pool = MyDownloadPool(2)
    async with pool as pool:
        async with MyChunkedStreamReader(pool) as reader:
            iterator1 = await reader.schedule("\n".join([str(i) for i in range(5)]))
            iterator2 = await reader.schedule("\n".join([str(i) for i in range(50, 100, 2)]))
            iterator3 = await reader.schedule("My monolith string")

            iter1 = iterator1.__aiter__()
            iter2 = iterator2.__aiter__()
            iter3 = iterator3.__aiter__()

            assert "50" == await iter2.__anext__()
            assert "52" == await iter2.__anext__()
            assert "0" == await iter1.__anext__()
            assert "54" == await iter2.__anext__()
            assert "56" == await iter2.__anext__()
            assert "58" == await iter2.__anext__()
            assert "1" == await iter1.__anext__()
            assert "My monolith string" == await iter3.__anext__()
            assert "2" == await iter1.__anext__()
            with pytest.raises(StopAsyncIteration):
                await iter3.__anext__()
            for i in range(60, 98, 2):
                assert str(i) == await iter2.__anext__()
            assert "3" == await iter1.__anext__()
            assert "98" == await iter2.__anext__()
            with pytest.raises(StopAsyncIteration):
                await iter2.__anext__()
            assert "4" == await iter1.__anext__()
            with pytest.raises(StopAsyncIteration):
                await iter1.__anext__()


@pytest.mark.asyncio
async def test_schedule_multiple_but_read_one_out_of_context():
    pool = MyDownloadPool(2)
    async with pool as pool:
        async with MyChunkedStreamReader(pool) as reader:
            iterator1 = await reader.schedule("\n".join([str(i) for i in range(5)]))
            iterator2 = await reader.schedule("\n".join([str(i) for i in range(50, 100, 2)]))
            iterator3 = await reader.schedule("My monolith string")
            lst = []
            async for x in iterator1:
                lst.append(x)
            assert lst == list(str(x) for x in range(5))

    for iterator in (iterator2, iterator3):
        with pytest.raises(TypeError):
            for it in iterator:
                pass


@pytest.mark.asyncio
@pytest.mark.parametrize('num_concurrent_readers', [1, 5])
async def test_sorted_read_from_random_sleep_iterator(num_concurrent_readers):
    pool = MyDownloadPool(2, cls=RandomDownloadWorker)
    count = 20

    async def read_with_pool(pool):
        async with MyChunkedStreamReader(pool) as reader:
            iterator1 = await reader.schedule("\n".join([str(i) for i in range(count)]))

            lst = []
            async for x in iterator1:
                lst.append(x)
            assert lst == list(str(x) for x in range(count))

    async with pool as pool:
        futures = [asyncio.ensure_future(read_with_pool(pool)) for _ in range(num_concurrent_readers)]
        await asyncio.wait(futures, return_when=asyncio.ALL_COMPLETED)

    for f in futures:
        await f


class MyDownloadPoolAssignCounter(BaseAsyncWorkerPool):
    def __init__(self, downloader_count=1, cls=InstantDownloadWorker, ):
        self._cls = cls
        self.assign_counter = 0
        super(MyDownloadPoolAssignCounter, self).__init__(workers_count=downloader_count)

    def create_new_worker(self, new_rank: int):
        return self._cls(new_rank)

    async def assign_job(self, *args, **kwargs):
        self.assign_counter += 1
        return await super(MyDownloadPoolAssignCounter, self).assign_job(*args, **kwargs)


@pytest.mark.asyncio
async def test_manual_cancel_iterator():
    pool = MyDownloadPool(2)
    async with pool as pool:
        async with MyChunkedStreamReader(pool) as reader:
            iterator1 = await reader.schedule("\n".join([str(i) for i in range(10)]))
            iterator2 = await reader.schedule("\n".join([str(i) for i in range(10)]))
            iterator3 = await reader.schedule("\n".join([str(i) for i in range(10)]))
            counter = 0
            async for x in iterator1:
                if counter >= 5:
                    iterator1.cancel()
                    break
                else:
                    counter += 1
            with pytest.raises(asyncio.CancelledError):
                async for _ in iterator1:
                    pass

            async for _ in iterator2:
                pass

            iterator3.cancel()

        assert not iterator1.finished()
        assert iterator1.terminated()
        assert iterator1.done()

        assert iterator2.done()
        assert iterator2.finished()
        assert iterator2.stopped()
        assert not iterator2.terminated()

        assert isinstance(iterator3.get_exception(), asyncio.CancelledError)
        assert iterator3.done()
        assert iterator3.terminated()


##########################################################
#                    RAM BARRIER TESTS                   #
##########################################################


class MyChunkedStreamReaderChunkDoneOverrided(BaseStreamReader):
    def __init__(self, *args, **kwargs):
        self.CHUNKS_PROCESSED = 0
        super(MyChunkedStreamReaderChunkDoneOverrided, self).__init__(*args, **kwargs)

    async def _split_to_chunks(self, job):
        for x in job.split('\n'):
            yield x, 0

    def _on_chunk_done(self, chunk):
        self.CHUNKS_PROCESSED += 1
        return super(MyChunkedStreamReaderChunkDoneOverrided, self)._on_chunk_done(chunk)


@pytest.mark.asyncio
async def test_ram_barrier_stop_downloading():
    pool = MyDownloadPool(2)
    # 10 bytes max RAM
    async with pool:
        async with MyChunkedStreamReaderChunkDoneOverrided(pool, max_memory=10) as reader:
            stream = await reader.schedule("\n".join([str(i) * 20 for i in range(10)]))  # each chunk will run out of memory
            await asyncio.sleep(0.02)
            reference = reader.CHUNKS_PROCESSED
            for _ in range(5):
                await asyncio.sleep(0.02)
                # We do not control exact memory limit. Instead, we just block when we know about the limit
                assert reader.CHUNKS_PROCESSED == reference

            await stream.__anext__()
            await asyncio.sleep(0.02)
            reference = reader.CHUNKS_PROCESSED
            for _ in range(5):
                await asyncio.sleep(0.02)
                # We do not control exact memory limit. Instead, we just block when we know about the limit
                assert reader.CHUNKS_PROCESSED == reference


@pytest.mark.asyncio
async def test_ram_counter():
    pool = MyDownloadPool(2)
    # 10 bytes max RAM
    async with pool:
        async with MyChunkedStreamReaderChunkDoneOverrided(pool) as reader:
            stream1 = await reader.schedule("\n".join([str(i) * 20 for i in range(10)]))
            await asyncio.sleep(0.01)
            current_memory = reader.current_ready_memory  # Ensure that future has finished
            assert current_memory == sum(sys.getsizeof(x) for x in [str(i) * 20 for i in range(10)])
            async for x in stream1:
                current_memory -= sys.getsizeof(x)
                assert reader.current_ready_memory == current_memory
            assert reader.current_ready_memory == 0
        assert reader.current_ready_memory == 0
    assert reader.current_ready_memory == 0


@pytest.mark.asyncio
async def test_ram_barrier_correct_process_multiple_subscribers():
    # This test is unified for both sorted and unsorted streams, but it's hard to check it on unsorted. We just rely
    # on the fact that testing code is the same
    pool = MyDownloadPool(2)
    # 10 bytes max RAM
    async with pool:
        async with MyChunkedStreamReaderChunkDoneOverrided(pool) as reader:
            stream1 = await reader.schedule("\n".join([str(i) * 20 for i in range(10)]))
            stream2 = await reader.schedule("\n".join([str(i) * 20 for i in range(10)]))
            stream3 = await reader.schedule("\n".join([str(i) * 20 for i in range(10)]))
            await asyncio.sleep(0.3)
            for i in range(10):
                await stream1.__anext__()
                await stream2.__anext__()
                await stream3.__anext__()
            assert reader.current_ready_memory == 0
        assert reader.current_ready_memory == 0
    assert reader.current_ready_memory == 0


##########################################################
#                     EXCEPTION TESTS                    #
##########################################################


@dataclasses.dataclass
class Counter:
    x: int = 0


class MyException(Exception):
    pass


class ExceptionDownloadWorker(BaseAsyncWorker):
    def __init__(self, rank: int, counter: Counter):
        super(ExceptionDownloadWorker, self).__init__(rank)
        self.counter = counter

    async def _do_job(self, data):
        self.counter.x += 1
        if self.counter.x > 3:
            raise MyException("OOPS")

        return data


class ExceptionDownloadPool(BaseAsyncWorkerPool):
    def __init__(self, downloader_count=1, ):
        self.counter = Counter()
        super(ExceptionDownloadPool, self).__init__(workers_count=downloader_count)

    def create_new_worker(self, new_rank: int):
        return ExceptionDownloadWorker(new_rank, self.counter)


@pytest.mark.asyncio
async def test_exception_download_pool_single_stream():
    pool = ExceptionDownloadPool(2)
    with pytest.raises(MyException):
        async with pool as pool:
            async with MyChunkedStreamReader(pool) as reader:
                iterator1 = await reader.schedule("\n".join([str(i) for i in range(50)]))
                async for _ in iterator1:
                    pass
    assert pool.is_dead()
    assert not pool.active_workers_count


@pytest.mark.asyncio
async def test_exception_download_pool_multiple_stream():
    pool = ExceptionDownloadPool(2)
    with pytest.raises(MyException):
        async with pool as pool:
            async with MyChunkedStreamReader(pool) as reader:
                iterators = [
                    await reader.schedule("\n".join([str(i) for i in range(50)]))
                    for _ in range(3)
                ]
                started = [it.__aiter__() for it in iterators]
                for i in range(50):
                    for it in started:
                        s = await it.__anext__()
                        assert isinstance(s, str)
    assert pool.is_dead()
    assert pool.active_workers_count <= 0


class ExceptionChunkedStreamReader(BaseStreamReader):
    def __init__(
            self,
            reader_pool,
            call_count
    ):
        self.call_count = call_count
        super(ExceptionChunkedStreamReader, self).__init__(reader_pool=reader_pool)

    async def _split_to_chunks(self, job):
        for i, x in enumerate(job.split('\n')):
            if i == self.call_count:
                raise MyException("OOPS")
            yield x, 0


@pytest.mark.asyncio
@pytest.mark.parametrize('call_count', [0, 3])
async def test_exception_chunking_single_stream(call_count):
    pool = MyDownloadPool(2, )
    with pytest.raises(MyException):
        async with pool as pool:
            async with ExceptionChunkedStreamReader(pool, call_count=call_count) as reader:
                await reader.schedule("\n".join([str(i) for i in range(50)]))
    assert not pool.active_workers_count


# test priority


class PriorityChunkedStreamReader(BaseStreamReader):
    async def _split_to_chunks(self, job):
        for i, x in enumerate(job.split('\n')):
            yield x, -i


@pytest.mark.asyncio
async def test_bad_priorities():
    # set max memory to zero to allow exactly one currently downloaded chunk
    async with MyDownloadPool(downloader_count=1) as pool:
        async with PriorityChunkedStreamReader(reader_pool=pool, max_memory=1) as stream_reader:
            with pytest.raises(Exception):
                await stream_reader.schedule("1\n2\n3\n4\n5")
