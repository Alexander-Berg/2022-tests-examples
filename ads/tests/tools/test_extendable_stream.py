"""
In this file we test ONLY base implementation, not the code for calling YT http requests
"""

import asyncio
import pytest
from queue import Queue
from ads_pytorch.tools.extendable_stream import BaseAsyncExtendableStream, StoppedExtendableStream, FinishedExtendableStream


class AsyncSortedExtendableStream(BaseAsyncExtendableStream):
    def __init__(self):
        super(AsyncSortedExtendableStream, self).__init__()
        self._futures = Queue()

    def _async_iterator_put_impl(self, future):
        self._futures.put(future)

    async def _async_iterator_next_impl(self):
        future = self._futures.get()
        return await future

    def _empty_impl(self):
        return self._futures.empty()


def create_future(x):
    loop = asyncio.get_event_loop()
    future = loop.create_future()
    future.set_result(x)
    return future


def create_exception_future(ex):
    loop = asyncio.get_event_loop()
    future = loop.create_future()
    future.set_exception(ex)
    return future


def create_wait_future(sleep_time, x):
    async def foo():
        await asyncio.sleep(sleep_time)
        return x
    return asyncio.ensure_future(foo())


################################################
#             TEST NORMAL ITERATION            #
################################################


@pytest.mark.asyncio
async def test_empty_iterator():
    it = AsyncSortedExtendableStream()
    it.stop()
    counter = 0
    async for _ in it:
        counter += 1
    assert counter == 0

    assert it.done()
    assert not it.terminated()
    assert it.finished()


@pytest.mark.asyncio
async def test_non_empty_iterator():
    it = AsyncSortedExtendableStream()
    for i in range(10):
        it.put(create_future(i))
    it.stop()

    lst = []
    async for x in it:
        lst.append(x)

    assert list(lst) == list(range(10))

    assert it.done()
    assert not it.terminated()
    assert it.finished()


@pytest.mark.asyncio
async def test_modify_while_iterating():
    it = AsyncSortedExtendableStream()
    counter = 10
    for i in range(counter):
        it.put(create_future(i))

    lst = []
    stopped = False
    async for x in it:
        if not stopped:
            it.put(create_future(counter))
        counter += 1
        lst.append(x)
        if counter == 100:
            stopped = True
            it.stop()

    assert list(lst) == list(range(100))
    assert it.done()
    assert not it.terminated()
    assert it.finished()


@pytest.mark.asyncio
async def test_double_iteration():
    it = AsyncSortedExtendableStream()
    counter = 10
    for i in range(counter):
        it.put(create_future(i))
    it.stop()

    lst = []
    async for x in it:
        lst.append(x)

    lst1 = []
    async for x in it:
        lst1.append(x)

    assert lst == list(range(10))
    assert lst1 == []


@pytest.mark.asyncio
async def test_we_can_throw_after_stop():
    it = AsyncSortedExtendableStream()
    for i in range(10):
        it.put(create_future(i))
    it.stop()
    for i in range(5):
        await it.__anext__()
    exc_type = ChildProcessError
    it.throw(exc_type())

    with pytest.raises(exc_type):
        async for _ in it:
            pass
    with pytest.raises(exc_type):
        it.cancel()
    with pytest.raises(exc_type):
        it.stop()
    with pytest.raises(exc_type):
        it.put(1)
    with pytest.raises(exc_type):
        it.throw(ValueError())
    with pytest.raises(exc_type):
        it.cancel()

    assert it.done()
    assert it.terminated()
    assert not it.finished()


@pytest.mark.asyncio
async def test_we_can_cancel_after_stop():
    it = AsyncSortedExtendableStream()
    for i in range(10):
        it.put(create_future(i))
    it.stop()
    for i in range(5):
        await it.__anext__()
    exc_type = asyncio.CancelledError
    it.cancel()

    with pytest.raises(exc_type):
        async for _ in it:
            pass
    with pytest.raises(exc_type):
        it.cancel()
    with pytest.raises(exc_type):
        it.stop()
    with pytest.raises(exc_type):
        it.put(1)
    with pytest.raises(exc_type):
        it.throw(ValueError())
    with pytest.raises(exc_type):
        it.cancel()

    assert it.done()
    assert it.terminated()
    assert not it.finished()


@pytest.mark.asyncio
async def test_methods_throw_after_throw():
    it = AsyncSortedExtendableStream()
    for i in range(10):
        it.put(create_future(i))
    for i in range(7):
        await it.__anext__()
    it.put(create_future(10050))
    exc_type = ChildProcessError
    it.throw(exc_type())

    with pytest.raises(exc_type):
        async for _ in it:
            pass
    with pytest.raises(exc_type):
        it.cancel()
    with pytest.raises(exc_type):
        it.stop()
    with pytest.raises(exc_type):
        it.put(1)
    with pytest.raises(exc_type):
        it.throw(RuntimeError())
    with pytest.raises(exc_type):
        it.cancel()

    assert it.done()
    assert not it.finished()
    assert it.terminated()


@pytest.mark.asyncio
async def test_methods_throw_after_cancel():
    it = AsyncSortedExtendableStream()
    for i in range(10):
        it.put(create_future(i))
    for i in range(7):
        await it.__anext__()
    it.put(create_future(10050))
    exc_type = asyncio.CancelledError
    it.cancel()

    with pytest.raises(exc_type):
        async for _ in it:
            pass
    with pytest.raises(exc_type):
        it.cancel()
    with pytest.raises(exc_type):
        it.stop()
    with pytest.raises(exc_type):
        it.put(1)
    with pytest.raises(exc_type):
        it.throw(RuntimeError())
    with pytest.raises(exc_type):
        it.cancel()

    assert it.done()
    assert not it.finished()
    assert it.terminated()


################################################
#                TEST EXCEPTIONS               #
################################################


class MyException(Exception):
    pass


@pytest.mark.asyncio
async def test_bad_future_kills_stream():
    it = AsyncSortedExtendableStream()
    for i in range(10):
        it.put(create_future(i))
    it.put(create_exception_future(KeyError("Oops...")))
    lst = []
    with pytest.raises(KeyError):
        async for x in it:
            lst.append(x)
    assert set(lst) == set(range(10))
    assert it.terminated()
    assert it.done()
    assert not it.finished()
    with pytest.raises(KeyError):
        async for x in it:
            pass


class PutExceptionStream(AsyncSortedExtendableStream):
    def __init__(self):
        super(PutExceptionStream, self).__init__()
        self.fail = False

    def _async_iterator_put_impl(self, future):
        if self.fail:
            raise MyException("OOPS")
        self._futures.put(future)


@pytest.mark.asyncio
async def test_put_exception():
    it = PutExceptionStream()
    for i in range(10):
        it.put(create_future(i))

    lst = []
    counter = 15
    with pytest.raises(MyException):
        async for x in it:
            lst.append(x)
            it.put(create_future(100))
            counter -= 1
            if counter == 0:
                it.fail = True

    assert lst == list(range(10)) + [100] * 6
    assert it.done()
    assert it.terminated()
    assert not it.finished()
    assert isinstance(it.get_exception(), MyException)


class EmptyExceptionStream(AsyncSortedExtendableStream):
    def __init__(self):
        super(EmptyExceptionStream, self).__init__()
        self.fail = False

    def _empty_impl(self):
        if self.fail:
            raise MyException("OOPS")
        return super(EmptyExceptionStream, self)._empty_impl()


@pytest.mark.asyncio
async def test_empty_exception_put():
    it = EmptyExceptionStream()
    for i in range(10):
        it.put(create_future(i))
    it.fail = True
    it.put(create_future(100))
    assert not it.done()
    assert not it.terminated()
    assert not it.finished()


@pytest.mark.asyncio
async def test_empty_exception_anext():
    it = EmptyExceptionStream()
    for i in range(10):
        it.put(create_future(i))
    await it.__anext__()
    it.fail = True
    with pytest.raises(MyException):
        await it.__anext__()
    with pytest.raises(MyException):
        async for _ in it:
            pass
    assert it.done()
    assert it.terminated()
    assert not it.finished()


class AnextImplExceptionStream(AsyncSortedExtendableStream):
    def __init__(self):
        super(AnextImplExceptionStream, self).__init__()
        self.fail = False

    async def _async_iterator_next_impl(self):
        if self.fail:
            raise MyException("OOPS")
        return await super(AnextImplExceptionStream, self)._async_iterator_next_impl()


@pytest.mark.asyncio
async def test_async_impl_exception_put():
    it = AnextImplExceptionStream()
    for i in range(10):
        it.put(create_future(i))
    it.fail = True
    it.put(create_future(100))
    assert not it.done()
    assert not it.terminated()
    assert not it.finished()


@pytest.mark.asyncio
async def test_async_impl_exception_anext():
    it = AnextImplExceptionStream()
    for i in range(10):
        it.put(create_future(i))
    await it.__anext__()
    it.fail = True
    with pytest.raises(MyException):
        await it.__anext__()
    with pytest.raises(MyException):
        async for _ in it:
            pass
    assert it.done()
    assert it.terminated()
    assert not it.finished()


@pytest.mark.asyncio
async def test_async_impl_exception_anext():
    it = AnextImplExceptionStream()
    for i in range(10):
        it.put(create_future(i))
    counter = 5
    with pytest.raises(MyException):
        async for x in it:
            counter -= 1
            if counter == 0:
                it.fail = True


####################################################
# TEST ASYNCHRONOUS ITERATION AND STOP/TERMINATION #
####################################################


@pytest.mark.asyncio
async def test_throw_will_terminate_anext_waiting_empty_stream():
    async def foo(stream):
        async for x in stream:
            pass
    it = AsyncSortedExtendableStream()
    future = asyncio.ensure_future(foo(it))
    await asyncio.sleep(0.01)
    it.throw(MyException("OOPS"))
    with pytest.raises(MyException):
        await future


@pytest.mark.asyncio
async def test_async_stop_will_interrupt_empty_stream():
    async def foo(stream):
        async for x in stream:
            pass
    it = AsyncSortedExtendableStream()
    future = asyncio.ensure_future(foo(it))
    await asyncio.sleep(0.01)
    it.stop()
    await future


@pytest.mark.parametrize('add_previous', [True, False], ids=['AddPrevious', 'Usual'])
@pytest.mark.parametrize('action', ['Cancel', 'Throw'])
@pytest.mark.parametrize('stop', [True, False], ids=['Stop', 'NoStop'])
@pytest.mark.asyncio
async def test_throw_will_terminate_anext_waiting(stop, action, add_previous):
    lst = []
    async def foo(stream):
        async for x in stream:
            lst.append(x)

    it = AsyncSortedExtendableStream()
    if add_previous:
        for i in range(10):
            it.put(create_future(i))
    for i in range(3):
        it.put(asyncio.sleep(100500))
    if stop:
        it.stop()
    future = asyncio.ensure_future(foo(it))
    await asyncio.sleep(0.01)
    if action == 'Throw':
        it.throw(MyException("OOPS"))
    else:
        it.cancel()
    exc_type = MyException if action == 'Throw' else asyncio.CancelledError
    with pytest.raises(exc_type):
        await future
    if add_previous:
        assert lst == list(range(10))
    else:
        assert lst == []
