from typing import Any, Union, List
import pytest
import torch
import threading
from ads_pytorch.tools.multiprocessing import get_multiprocessing_context
from ads_pytorch.tools.socketpair_transport import (
    SocketTransport,
    socketpair_transport,
    send_object_via_socket,
    receive_object_via_socket,
    SimpleObjectPipe,
    MessagePipeRouter
)
import dataclasses
import asyncio
import uvloop

uvloop.install()


def _asyncio_run(task):
    loop = asyncio.get_event_loop_policy().new_event_loop()
    loop.run_until_complete(task)


#################################################################
#                  Socket and socketpair tests                  #
#################################################################


async def _src_socket_transport(s1):
    await s1.coro_send(b'1' * 10)
    b = await s1.coro_recv(2, exact=True) + await s1.coro_recv(8, exact=True)
    assert b == b'2' * 10


async def _dst_socket_transport(s2):
    a = await s2.coro_recv(10, exact=True)
    assert a == b'1' * 10
    await s2.coro_send(b'2' * 3)
    await s2.coro_send(b'2' * 7)


@pytest.mark.asyncio
async def test_socket_transport():
    s1, s2 = socketpair_transport()
    await asyncio.gather(_src_socket_transport(s1), _dst_socket_transport(s2))


def _socket_transport_mp_foo(s2):
    _asyncio_run(_dst_socket_transport(s2))


@pytest.mark.asyncio
async def test_socket_transport_multiprocessing():
    s1, s2 = socketpair_transport()
    ctx = get_multiprocessing_context()
    proc = ctx.Process(target=_socket_transport_mp_foo, kwargs=dict(s2=s2))
    proc.start()

    await _src_socket_transport(s1)


#################################################################
#            Functional interface on sending objects            #
#################################################################


@pytest.mark.asyncio
async def test_send_bytes():
    s1, s2 = socketpair_transport()
    data = b'1' * 100400
    await send_object_via_socket(sock=s1, obj=data)
    res = await receive_object_via_socket(sock=s2)
    assert res == data


@dataclasses.dataclass(eq=True)
class SomeStructure:
    x: Any
    y: Any


def _cmp_some_structures(left: SomeStructure, right: SomeStructure):
    assert isinstance(left, SomeStructure)
    assert isinstance(right, SomeStructure)
    assert torch.allclose(left.x, right.x)
    assert torch.allclose(left.y, right.y)


@pytest.mark.asyncio
async def test_send_object_no_lock():
    s1, s2 = socketpair_transport()
    data = SomeStructure(
        x={"1": 2, 3: b'456'},
        y={"1", 2, b''}
    )
    await send_object_via_socket(sock=s1, obj=data)
    res = await receive_object_via_socket(sock=s2)
    assert res == data


@pytest.mark.asyncio
async def test_send_object_with_lock():
    lock = threading.Lock()
    s1, s2 = socketpair_transport()
    data = SomeStructure(
        x=torch.tensor([1, 2, 3]),
        y=torch.rand(10, 10)
    )
    await send_object_via_socket(sock=s1, obj=data, pickler_lock=lock)
    res = await receive_object_via_socket(sock=s2, pickler_lock=lock)
    _cmp_some_structures(res, data)


@pytest.mark.asyncio
async def test_failure_receiving_without_pickle_lock():
    lock = threading.Lock()
    s1, s2 = socketpair_transport()
    data = SomeStructure(
        x=torch.tensor([1, 2, 3]),
        y=torch.rand(10, 10)
    )
    await send_object_via_socket(sock=s1, obj=data, pickler_lock=lock)
    with pytest.raises(Exception):
        await receive_object_via_socket(sock=s2)


#################################################################
#                     Functional concurrency                    #
#################################################################


@pytest.mark.parametrize("consumer_count", [1, 5, 100])
@pytest.mark.asyncio
async def test_multiple_producers_consumers_single_lock(consumer_count):
    s1, s2 = socketpair_transport()

    async def _producer(sock, lst):
        for x in lst:
            await send_object_via_socket(sock=sock, obj=x)

    data = [
        list(range(i * 1000000, i * 1000000 + 1000))
        for i in range(10)
    ]
    assert len(sum(data, [])) == len(set(sum(data, [])))
    consumed_data = set()
    total_count = sum(len(x) for x in data)
    done_event = asyncio.Event()

    async def _consumer(sock):
        nonlocal consumed_data
        while True:
            obj = await receive_object_via_socket(sock=sock)
            assert obj not in consumed_data
            consumed_data.add(obj)
            if len(consumed_data) == total_count:
                done_event.set()

    # we spawn both producers and consumers for all sockets to test duplex
    tasks = [
                _producer(sock=s1, lst=lst)
                for lst in data[:len(data) // 2]
            ] + [
                _producer(sock=s2, lst=lst)
                for lst in data[len(data) // 2:]
            ] + [
                _consumer(sock=s1)
                for _ in range(consumer_count)
            ] + [
                _consumer(sock=s2)
                for _ in range(consumer_count)
            ]

    for t in tasks:
        loop = asyncio.get_running_loop()
        loop.create_task(t)

    await done_event.wait()

    assert consumed_data == set(sum(data, []))


#################################################################
#                     asyncio Queue interface                   #
#################################################################


@pytest.mark.asyncio
async def test_simple_pipe():
    s1, s2 = socketpair_transport()
    lock = threading.Lock()
    p1 = SimpleObjectPipe(sock=s1, pickler_lock=lock)
    p2 = SimpleObjectPipe(sock=s2, pickler_lock=lock)
    data = SomeStructure(
        x=torch.tensor([1, 2, 3]),
        y=torch.rand(10, 10)
    )

    await p1.send(data)
    res = await p2.recv()
    _cmp_some_structures(res, data)


@pytest.mark.asyncio
async def test_socket_server_single_connection():
    s1, s2 = socketpair_transport()

    # Note that we have distinct servers here - they may lay on different machines
    server1 = MessagePipeRouter(sock=s1)
    server2 = MessagePipeRouter(sock=s2)

    await asyncio.gather(server1.start_serving(), server2.start_serving())

    pipe1 = await server1.connect(connection_id=0)
    pipe2 = await server2.connect(connection_id=0)

    for i in range(4):
        await pipe1.send(i)
        assert await pipe2.recv() == i


#######################################################################
#                       Socket server exceptions                      #
#######################################################################


async def _host_socket_server_error(sock: SocketTransport, helper_err_sock: SocketTransport):
    server = MessagePipeRouter(sock=sock, pickler_lock=threading.Lock())
    await server.start_serving()
    pipe = await server.connect(connection_id=15)
    assert await pipe.recv() == 103
    with pytest.raises(Exception):
        await pipe.send(torch.tensor(103), pickle_block=True)
        assert await helper_err_sock.coro_recv(1) == b'1'
        await pipe.send(105)


async def _mp_socket_server_error(sock: SocketTransport, helper_err_sock: SocketTransport):
    server = MessagePipeRouter(sock=sock)  # no pickler lock here!!
    await server.start_serving()
    pipe = await server.connect(connection_id=15)
    await pipe.send(103, pickle_block=False)
    with pytest.raises(Exception):
        try:
            # this should throw - we cannot unpickle pickled with thread lock tensor
            await pipe.recv()
        except BaseException:
            await helper_err_sock.coro_send(b'1')
            raise
        finally:
            # This wont take effect if we previously sended '1'
            await helper_err_sock.coro_send(b'2')


@pytest.mark.asyncio
async def test_socket_server_error_samehost():
    s1, s2 = socketpair_transport()
    helper1, helper2 = socketpair_transport()
    await asyncio.gather(
        _host_socket_server_error(sock=s1, helper_err_sock=helper1),
        _mp_socket_server_error(sock=s2, helper_err_sock=helper2)
    )


def _mp_socket_server_foo(sock: SocketTransport, helper: SocketTransport):
    async def _main():
        await helper.coro_send(b'1')
        await _mp_socket_server_error(sock=sock, helper_err_sock=helper)
    _asyncio_run(_main())


@pytest.mark.asyncio
async def test_socket_server_error_multiprocessing():
    s1, s2 = socketpair_transport()
    helper1, helper2 = socketpair_transport()

    ctx = get_multiprocessing_context()
    proc = ctx.Process(target=_mp_socket_server_foo, kwargs=dict(sock=s2, helper=helper2))
    proc.start()
    assert await helper1.coro_recv(1) == b'1'

    await _host_socket_server_error(sock=s1, helper_err_sock=helper1)
