import pytest
import dataclasses
import asyncio


from ads_pytorch.core.data_loader import DataLoader, DataLoaderNotStarted, DataLoaderIterator
from ads_pytorch.tools.stream_reader import BaseStreamReader
from ads_pytorch.tools.async_worker_pool import BaseAsyncWorker, BaseAsyncWorkerPool


# Fake download worker: just add a suffix


class FakeDownloadWorker(BaseAsyncWorker):
    def __init__(self, rank):
        super(FakeDownloadWorker, self).__init__(rank)

    async def _do_job(self, chunk: str):
        return chunk + "_down"


class FakeDownloadPool(BaseAsyncWorkerPool):
    def create_new_worker(self, new_rank: int):
        return FakeDownloadWorker(new_rank)


# StreamReader


class FakeStreamReader(BaseStreamReader):
    def __init__(self, reader_pool, uri_data):
        super(FakeStreamReader, self).__init__(reader_pool=reader_pool)
        self.uri_data = uri_data

    async def _split_to_chunks(self, job: str):
        data = self.uri_data[job]
        for x in data.split():
            yield x, 0


async def _ait(lst):
    for x in lst:
        yield x


def fake_data_loader():
    uri_data = {
        "uri1": "1 2 3",
        "uri2": "a b c"
    }
    downloader = FakeDownloadPool(5)
    stream_reader = FakeStreamReader(reader_pool=downloader, uri_data=uri_data)
    return DataLoader(downloader_pool=downloader, stream_reader=stream_reader, uri_iterable=_ait(["uri1", "uri2"]))


###############################################
#               NORMAL WORK TESTS             #
###############################################


@pytest.mark.asyncio
async def test_data_loader_not_started():
    uri_data = {
        "uri1": "1 2 3",
        "uri2": "a b c"
    }
    downloader = FakeDownloadPool(5)
    stream_reader = FakeStreamReader(reader_pool=downloader, uri_data=uri_data)
    data_loader = DataLoader(downloader_pool=downloader, stream_reader=stream_reader, uri_iterable=_ait(["uri1", "uri2"]))
    with pytest.raises(DataLoaderNotStarted):
        async for _ in data_loader:
            pass


@pytest.mark.asyncio
async def test_data_loader_iterate_over_data():
    uri_data = {
        "uri1": "1 2 3",
        "uri2": "a b c"
    }
    downloader = FakeDownloadPool(5)
    stream_reader = FakeStreamReader(reader_pool=downloader, uri_data=uri_data)
    data_loader = DataLoader(downloader_pool=downloader, stream_reader=stream_reader, uri_iterable=_ait(["uri2", "uri1"]))
    results = []
    async with data_loader:
        async for uri, stream in data_loader:
            cur_res = []
            async for r in stream:
                cur_res.append(r)
            results.append((uri, tuple(cur_res)))

    assert results == [
        ("uri2", ("a_down", "b_down", "c_down")),
        ("uri1", ("1_down", "2_down", "3_down"))
    ]


###############################################
#                EXCEPTION TESTS              #
###############################################


class MyException(Exception):
    pass


@dataclasses.dataclass
class CallCounter:
    x: int = 0


class ExceptionDownloadWorker(BaseAsyncWorker):
    def __init__(self, rank, counter):
        super(ExceptionDownloadWorker, self).__init__(rank)
        self.counter = counter

    async def _do_job(self, chunk: str):
        self.counter.x += 1
        if self.counter.x < 50:
            return chunk + "_down"
        if self.counter.x == 50:
            raise MyException("OOPS")
        elif self.counter.x > 50:
            await asyncio.sleep(100500)


class ExceptionDownloadPool(BaseAsyncWorkerPool):
    def __init__(self, *args, **kwargs):
        super(ExceptionDownloadPool, self).__init__(*args, **kwargs)
        self.counter = CallCounter()

    def create_new_worker(self, new_rank: int):
        return ExceptionDownloadWorker(new_rank, self.counter)


@pytest.mark.parametrize('wait', [True, False])
@pytest.mark.asyncio
async def test_data_loader_download_pool_exception(wait):
    uri_data = {"x{}".format(i): str(i) for i in range(100)}
    uris = ["x{}".format(i) for i in range(100)]

    downloader = ExceptionDownloadPool(100)
    stream_reader = FakeStreamReader(reader_pool=downloader, uri_data=uri_data)
    data_loader = DataLoader(downloader_pool=downloader, stream_reader=stream_reader, uri_iterable=_ait(uris))

    results = []
    with pytest.raises(MyException):
        async with data_loader:
            async for uri, stream in data_loader:
                if wait:
                    await asyncio.sleep(0.2)
                    wait = False
                cur_res = []
                async for r in stream:
                    cur_res.append(r)
                results.append((uri, tuple(cur_res)))

    assert results == [("x{}".format(i), ("{}_down".format(i),)) for i in range(49)]


class ExceptionStreamReader(BaseStreamReader):
    def __init__(self, reader_pool, uri_data, count):
        super(ExceptionStreamReader, self).__init__(reader_pool=reader_pool)
        self.uri_data = uri_data
        self.counter = CallCounter(0)
        self._max_count = count

    async def _split_to_chunks(self, job: str):
        self.counter.x += 1
        if self.counter.x > self._max_count:
            raise MyException("OOPS")
        data = self.uri_data[job]
        for x in data.split():
            yield x, 0


@pytest.mark.parametrize('count', [0, 50])
@pytest.mark.parametrize('wait', [True, False])
@pytest.mark.asyncio
async def test_data_loader_stream_reader_exception(wait, count):
    uri_data = {"x{}".format(i): str(i) for i in range(100)}
    uris = ["x{}".format(i) for i in range(100)]

    downloader = FakeDownloadPool(100, )
    stream_reader = ExceptionStreamReader(reader_pool=downloader, uri_data=uri_data, count=count)
    data_loader = DataLoader(downloader_pool=downloader, stream_reader=stream_reader, uri_iterable=_ait(uris))

    results = []
    with pytest.raises(MyException):
        async with data_loader:
            async for uri, stream in data_loader:
                if wait:
                    await asyncio.sleep(0.2)
                    wait = False
                cur_res = []
                async for r in stream:
                    cur_res.append(r)
                results.append((uri, tuple(cur_res)))

    if wait:
        assert results == []
    else:
        assert set(results).issubset({("x{}".format(i), ("{}_down".format(i),)) for i in range(49)})
