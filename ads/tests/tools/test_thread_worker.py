import pytest
import asyncio
import time
import threading
from ads_pytorch.tools.async_threaded import threaded_worker, AlreadyExecutingError, _worker
import ads_pytorch.tools.async_threaded


@pytest.mark.parametrize('safe', [True, False])
@pytest.mark.asyncio
async def test_threaded_worker_noargs(safe):
    worker = threaded_worker(safe)
    res = await worker(lambda: 1)
    assert res == 1


@pytest.mark.parametrize('safe', [True, False])
@pytest.mark.asyncio
async def test_threaded_worker_args(safe):
    def foo(*args):
        return sum(args)

    worker = threaded_worker(safe)
    res = await worker(foo, 1, 2, 3)
    assert res == 6


@pytest.mark.parametrize('safe', [True, False])
@pytest.mark.asyncio
async def test_threaded_worker_kwargs(safe):
    def foo(**kwargs):
        return ''.join(list(kwargs.keys())), sum(kwargs.values())

    worker = threaded_worker(safe)
    keys, vals = await worker(foo, x=1, y=2, z=3)
    assert vals == 6
    assert keys == "xyz"


@pytest.mark.parametrize('safe', [True, False])
@pytest.mark.asyncio
async def test_threaded_worker_execute_once(safe):
    class Counter:
        def __init__(self):
            self.x = 0

    counter = Counter()

    def foo():
        counter.x += 1

    worker = threaded_worker(safe)
    await worker(foo)
    await asyncio.sleep(0.01)
    assert counter.x == 1


@pytest.mark.parametrize('safe', [True, False])
@pytest.mark.asyncio
async def test_kill_thread_garbage_collection(monkeypatch, safe):
    thread_counter = 0

    def my_worker(*args, **kwargs):
        nonlocal thread_counter
        thread_counter += 1
        _worker(*args, **kwargs)
        # this will check that we exited from worker and stopped thread after garbage collection
        thread_counter -= 1

    monkeypatch.setattr(ads_pytorch.tools.async_threaded, '_worker', my_worker)

    async def some_foo():
        loop = asyncio.get_event_loop()
        worker = threaded_worker(safe=safe)
        task = loop.create_task(worker(time.sleep, 0.2))
        await asyncio.sleep(0.01)
        assert thread_counter == 1

    await some_foo()
    # Out wask is sleeping for 0.2 seconds
    assert thread_counter == 1
    await asyncio.sleep(0.3)
    assert thread_counter == 0


@pytest.mark.parametrize('safe', [True, False])
@pytest.mark.asyncio
async def test_double_execution(safe):
    loop = asyncio.get_event_loop()

    x = 1

    def infinite_foo():
        nonlocal x
        while x:
            pass

    worker = threaded_worker(safe)
    task = loop.create_task(worker(infinite_foo))
    await asyncio.sleep(0.01)
    assert worker.is_executing

    with pytest.raises(AlreadyExecutingError):
        await worker(infinite_foo)

    await asyncio.sleep(0.1)
    with pytest.raises(AlreadyExecutingError):
        await worker(infinite_foo)

    await asyncio.sleep(0.1)
    x = 0


@pytest.mark.parametrize('safe', [True, False])
@pytest.mark.asyncio
async def test_exception(safe):
    worker = threaded_worker(safe)

    def foo():
        raise LookupError

    with pytest.raises(LookupError):
        await worker(foo)


@pytest.mark.parametrize('safe', [True, False])
@pytest.mark.asyncio
async def test_execute_after_exception(safe):
    worker = threaded_worker(safe)

    def foo():
        raise LookupError

    def bar():
        return 1

    with pytest.raises(LookupError):
        await worker(foo)

    res = await worker(bar)
    assert res == 1


@pytest.mark.parametrize('safe', [True, False])
@pytest.mark.asyncio
async def test_execute_after_exceptions_kills_new_created_thread(safe, monkeypatch):
    thread_counter = 0

    def my_worker(*args, **kwargs):
        nonlocal thread_counter
        thread_counter += 1
        try:
            _worker(*args, **kwargs)
        finally:
            # this will check that we exited from worker and stopped thread after garbage collection
            thread_counter -= 1

    monkeypatch.setattr(ads_pytorch.tools.async_threaded, '_worker', my_worker)

    def foo():
        raise LookupError

    def bar():
        return 1

    async def execute_with_consequent_exceptions():
        worker = threaded_worker(safe)

        await worker(bar)

        assert thread_counter == 1

        with pytest.raises(LookupError):
            await worker(foo)

        assert thread_counter == 1

        res = await worker(bar)
        assert res == 1
        assert thread_counter == 1

    assert thread_counter == 0
    await execute_with_consequent_exceptions()
    await asyncio.sleep(0.01)
    assert thread_counter == 0
