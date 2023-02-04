import pytest
import asyncio
import contextlib
import random
from pytorch_embedding_model.utils.heap_sorted_stream import BaseHeapSortedStream


class IntHeapStream(BaseHeapSortedStream):
    def _get_record_id(self, item):
        return item


@contextlib.contextmanager
def _die_ctx(stream: BaseHeapSortedStream):
    try:
        yield
    except BaseException as ex:
        stream.throw(ex)


@pytest.mark.asyncio
async def test_single_producer():
    heap_stream = IntHeapStream()

    async def _producer():
        with _die_ctx(heap_stream):
            for i in range(1000):
                heap_stream.put(i)
                await asyncio.sleep(0)
        heap_stream.stop()

    asyncio.create_task(_producer())

    records = [r async for r in heap_stream]
    assert records == list(range(1000))


@pytest.mark.asyncio
async def test_single_producer_reverse_order():
    heap_stream = IntHeapStream()

    async def _producer():
        with _die_ctx(heap_stream):
            for i in reversed(range(1000)):
                heap_stream.put(i)
                await asyncio.sleep(0)
        heap_stream.stop()

    asyncio.create_task(_producer())

    records = [r async for r in heap_stream]
    assert records == list(range(1000))


@pytest.mark.asyncio
async def test_multiple_producer():
    heap_stream = IntHeapStream()
    producer_count = 5

    async def _producer(producer_id):
        for i in range(producer_id, 1000, producer_count):
            heap_stream.put(i)
            await asyncio.sleep(0)

    async def _run_producers():
        await asyncio.gather(*[_producer(i) for i in range(producer_count)])
        heap_stream.stop()

    asyncio.create_task(_run_producers())

    records = [r async for r in heap_stream]
    assert records == list(range(1000))


@pytest.mark.asyncio
async def test_multiple_producer_random():
    heap_stream = IntHeapStream()
    producer_count = 5

    async def _producer(producer_id):
        records = list(range(producer_id, 1000, producer_count))
        random.shuffle(records)
        for i in records:
            heap_stream.put(i)
            await asyncio.sleep(random.random() / 100)

    async def _run_producers():
        await asyncio.gather(*[_producer(i) for i in range(producer_count)])
        heap_stream.stop()

    asyncio.create_task(_run_producers())

    records = [r async for r in heap_stream]
    assert records == list(range(1000))
