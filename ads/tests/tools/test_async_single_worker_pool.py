import pytest
import asyncio
from ads_pytorch.tools.async_single_worker_pool import BaseAsyncSingleWorkerPool


class DummyStatefulWorker(BaseAsyncSingleWorkerPool):
    def __init__(self, *args):
        super(DummyStatefulWorker, self).__init__(*args)
        self.call_impl_called = 0
        self.lazy_init_called = 0

    async def _call_impl(self, *args, **kwargs) -> None:
        self.call_impl_called += 1

    async def _lazy_init_impl(self) -> None:
        self.lazy_init_called += 1


@pytest.mark.asyncio
async def test_lazy_init():
    worker = DummyStatefulWorker()
    assert worker.lazy_init_called == 0
    assert worker.call_impl_called == 0

    for i in range(3):
        await worker()
        assert worker.lazy_init_called == 1
        assert worker.call_impl_called == i + 1


class MyException(BaseException):
    pass


class LazyInitDead(DummyStatefulWorker):
    async def _lazy_init_impl(self):
        raise MyException()


class CallImplDead(DummyStatefulWorker):
    async def _call_impl(self, *args, **kwargs):
        raise MyException()


@pytest.mark.parametrize('worker', [LazyInitDead(), CallImplDead()], ids=['init', 'call'])
@pytest.mark.asyncio
async def test_autodie(worker):
    with pytest.raises(MyException) as exc:
        await worker()

    exception_value = exc.value
    assert worker.is_dead()
    with pytest.raises(MyException) as exc:
        await worker()
        # Check that we have reraised previous exception
        assert exc.value is exception_value


@pytest.mark.parametrize('worker', [LazyInitDead(MyException), CallImplDead(MyException)], ids=['init', 'call'])
@pytest.mark.asyncio
async def test_ignore_exc(worker):
    with pytest.raises(MyException) as exc:
        await worker()

    exception_value = exc.value
    assert not worker.is_dead()
    with pytest.raises(MyException) as exc:
        await worker()
        # Check that we have reraised previous exception
        assert exc.value is exception_value


@pytest.mark.asyncio
async def test_cancellation():
    class FirstInfiniteCallWorkerPool(BaseAsyncSingleWorkerPool):
        def __init__(self):
            super(FirstInfiniteCallWorkerPool, self).__init__(asyncio.CancelledError)
            self.call_impl_called = 0

        async def _call_impl(self, x) -> None:
            self.call_impl_called += 1
            if self.call_impl_called == 1:
                await asyncio.sleep(100500)
            return x

    pool = FirstInfiniteCallWorkerPool()

    async def _infinite_task():
        await pool(105)

    task = asyncio.create_task(_infinite_task())
    await asyncio.sleep(0.2)
    assert not task.done()

    task.cancel()
    with pytest.raises(asyncio.CancelledError):
        await task

    assert not pool.is_dead()

    for i in range(3):
        assert await pool(i) == i


@pytest.mark.asyncio
async def test_consequent_execution():
    class PoolWithCounter(BaseAsyncSingleWorkerPool):
        EXECUTING_COUNT = 0
        MAX_EXECUTING_COUNT = 0

        async def _call_impl(self, *args, **kwargs):
            try:
                type(self).EXECUTING_COUNT += 1
                assert type(self).EXECUTING_COUNT == 1
                type(self).MAX_EXECUTING_COUNT = max(
                    type(self).MAX_EXECUTING_COUNT,
                    type(self).EXECUTING_COUNT
                )
                await asyncio.sleep(0.01)
            finally:
                type(self).EXECUTING_COUNT -= 1
                assert type(self).EXECUTING_COUNT == 0

    pool = PoolWithCounter()

    async def _task_dispatcher():
        for _ in range(5):
            await pool()

    await asyncio.gather(*[_task_dispatcher() for _ in range(10)])
    assert PoolWithCounter.MAX_EXECUTING_COUNT == 1
