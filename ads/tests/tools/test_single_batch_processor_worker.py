import asyncio
from typing import List, Any

import pytest
from ads_pytorch.tools.single_batch_processor_worker import (
    SingleBatchProcessorWorker,
    ISingleBatchProcessorWorkerImpl,
    DEFAULT_SINGLE_BATCH_WORKER_RESULT
)


class SetResultProcessor(ISingleBatchProcessorWorkerImpl):
    async def __call__(self, callbacks: List[asyncio.Future], jobs: List[int]) -> None:
        result = sum(jobs)
        for c in callbacks:
            if not c.done():
                c.set_result(result)


class DontSetResultProcessor(ISingleBatchProcessorWorkerImpl):
    def __init__(self):
        self._res = 0

    async def __call__(self, callbacks: List[asyncio.Future], jobs: List[int]) -> None:
        self._res += sum(jobs)


@pytest.mark.asyncio
async def test_processor_set_result():
    processor = SingleBatchProcessorWorker(SetResultProcessor())
    results = await asyncio.gather(*[processor.run_job(i) for i in range(10)])
    assert all(x == sum(range(10)) for x in results)
    results = await asyncio.gather(*[processor.run_job(i) for i in range(7)])
    assert all(x == sum(range(7)) for x in results)


@pytest.mark.asyncio
async def test_processor_aggregate_result():
    impl = DontSetResultProcessor()
    processor = SingleBatchProcessorWorker(impl)
    await asyncio.gather(*[processor.run_job(i) for i in range(10)])
    assert impl._res == sum(range(10))


class ToCatch(Exception):
    pass


class ExceptionProcessor(ISingleBatchProcessorWorkerImpl):
    def __init__(self):
        self.call_counter = 0

    async def __call__(self, callbacks: List[asyncio.Future], jobs: List[int]) -> None:
        self.call_counter += 1
        raise ToCatch


@pytest.mark.asyncio
async def test_processor_exception():
    impl = ExceptionProcessor()
    processor = SingleBatchProcessorWorker(impl)
    for _ in range(4):
        futures, _ = await asyncio.wait([processor.run_job(i) for i in range(10)], return_when=asyncio.ALL_COMPLETED)
        for f in futures:
            with pytest.raises(ToCatch):
                await f

    assert impl.call_counter == 4
