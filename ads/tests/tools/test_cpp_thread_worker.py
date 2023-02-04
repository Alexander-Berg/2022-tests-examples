import pytest
import asyncio
from concurrent.futures import ProcessPoolExecutor
from concurrent.futures.process import BrokenProcessPool
import torch
from ads_pytorch.tools.cpp_async_threaded import (
    cpp_threaded_worker,
    AlreadyExecutingError,
)
from ads_pytorch.cpp_lib import libcpp_lib as real_libcpp_lib


class _AttrMock:
    def __getattr__(self, item: str):
        return getattr(real_libcpp_lib, "_cpp_thread_test_" + item)


libcpp_lib = _AttrMock()


@pytest.fixture
def threaded_worker():
    return cpp_threaded_worker(name="tested_cpp_worker")


@pytest.mark.asyncio
async def test_threaded_worker_execute_once(threaded_worker):
    tensor = torch.zeros(1)
    await threaded_worker(libcpp_lib.OkFunctor(tensor))
    assert torch.allclose(tensor, torch.FloatTensor([20]))


@pytest.mark.asyncio
async def test_threaded_worker_fail_with_message(threaded_worker):
    functor = libcpp_lib.ExceptionFunctor()
    with pytest.raises(Exception) as exc:
        await threaded_worker(functor)
    assert str(exc.value) == "Exception with message"


@pytest.mark.asyncio
async def test_very_big_exception_message(threaded_worker):
    functor = libcpp_lib.ExceptionVeryBigMessageFunctor()
    with pytest.raises(Exception) as exc:
        await threaded_worker(functor)
    assert str(exc.value) == '1' * (1 << 16)


@pytest.mark.asyncio
async def test_threaded_worker_fail_no_message(threaded_worker):
    functor = libcpp_lib.UnknownExceptionFunctor()
    with pytest.raises(Exception) as exc:
        await threaded_worker(functor)
    assert str(exc.value) == "NO AVAILABLE ex->what()"


@pytest.mark.asyncio
async def test_double_execution_with_infinite_function(threaded_worker):
    loop = asyncio.get_event_loop()

    holder = libcpp_lib.IntHolder()
    infinite_foo = libcpp_lib.SpinlockWaitFunctor(holder)

    task = loop.create_task(threaded_worker(infinite_foo))
    await asyncio.sleep(0.01)
    assert threaded_worker.is_executing

    with pytest.raises(AlreadyExecutingError):
        await threaded_worker(infinite_foo)

    await asyncio.sleep(0.1)
    with pytest.raises(AlreadyExecutingError):
        await threaded_worker(infinite_foo)

    await asyncio.sleep(0.1)
    holder.set(3)
    await task


@pytest.mark.asyncio
async def test_kill_thread_garbage_collection():
    async def some_foo():
        threaded_worker = cpp_threaded_worker()
        holder = libcpp_lib.IntHolder()
        loop = asyncio.get_event_loop()
        task = loop.create_task(threaded_worker(libcpp_lib.SpinlockWaitFunctor(holder)))
        await asyncio.sleep(0.01)
        holder.set(1)

    await some_foo()
    # Out wask is sleeping for 0.2 seconds
    await asyncio.sleep(0.3)


@pytest.mark.asyncio
async def test_execute_after_exception(threaded_worker):

    def foo():
        raise LookupError

    def bar():
        return 1

    with pytest.raises(Exception):
        await threaded_worker(libcpp_lib.ExceptionFunctor())

    tensor = torch.zeros(1)
    await threaded_worker(libcpp_lib.OkFunctor(tensor))
    assert tensor[0] == 20

    with pytest.raises(Exception):
        await threaded_worker(libcpp_lib.UnknownExceptionFunctor())

    tensor = torch.zeros(1)
    await threaded_worker(libcpp_lib.OkFunctor(tensor))
    assert tensor[0] == 20
