from typing import Any, Union, List
import pytest
import torch
import threading
from ads_pytorch.tools.tensor_pipe import (
    TensorPipeReader, TensorPipeWriter,
    TensorPipeClient,
    TensorPipeServer,
    IServerCallback,
    LocalhostCopyBuilder,
)
from ads_pytorch.tools import (
    ConnectAddress,
    Connector,
)
from ads_pytorch.tools.socketpair_transport import IObjectPipe
import dataclasses
import socket
import asyncio
import uvloop
import os
import tempfile
uvloop.install()


def _asyncio_run(task):
    loop = asyncio.get_event_loop_policy().new_event_loop()
    loop.run_until_complete(task)


@pytest.fixture
def socket_dir():
    with tempfile.TemporaryDirectory(dir=".") as tmp:
        yield tmp


async def _consequent_jobs_coro(local_instance, done_event: asyncio.Event, count: int, payload: int):
    for i in range(1, count + 1):
        if isinstance(local_instance, TensorPipeWriter):
            done_event.clear()
            await local_instance.coro_send(torch.tensor([i + payload] * i))
            await done_event.wait()
        else:
            tensor = await local_instance.coro_recv()
            reference = torch.tensor([i + payload] * i)
            assert torch.allclose(tensor, reference)
            done_event.set()


class EmptyCallback(IServerCallback):
    async def __call__(self, action: bytes, server_identity: str, local_instance: Union[TensorPipeReader, TensorPipeWriter], payload: Any) -> None:
        pass


class ExceptionCallback(IServerCallback):
    async def __call__(self, action: bytes, server_identity: str,
                       local_instance: Union[TensorPipeReader, TensorPipeWriter], payload: Any) -> None:
        raise RuntimeError("OOPS")


@dataclasses.dataclass
class ConsequentialCallback(IServerCallback):
    value: int
    count: int
    done_event: asyncio.Event
    launched_task: asyncio.Task = dataclasses.field(init=False, default=None)

    async def __call__(
        self,
        action: bytes,
        server_identity: str,
        local_instance: Union[IObjectPipe, TensorPipeReader, TensorPipeWriter],
        payload: int
    ) -> None:
        loop = asyncio.get_running_loop()
        if isinstance(local_instance, (TensorPipeReader, TensorPipeWriter)):
            self.launched_task = loop.create_task(_consequent_jobs_coro(
                local_instance=local_instance,
                done_event=self.done_event,
                count=self.count,
                payload=payload
            ))
        else:
            assert payload == 123
            await local_instance.send(125)


@pytest.mark.parametrize("mode", ["read", "write"])
@pytest.mark.asyncio
async def test_single_connection(socket_dir, mode):
    server_name = "server"
    addr = ConnectAddress(family=socket.AF_UNIX, path=os.path.join(socket_dir, "0"))
    connector = Connector(addresses={server_name: addr})

    value = 25
    count = 10
    payload = 5
    done_event = asyncio.Event()

    pickle_lock = threading.Lock()

    server_callback = ConsequentialCallback(
        value=value,
        count=count,
        done_event=done_event
    )

    server = TensorPipeServer(
        address=addr,
        copy_buffer_builder=LocalhostCopyBuilder(),
        accept_callback=server_callback,
        pickle_thread_lock=pickle_lock
    )
    await server.start_serving()

    client = TensorPipeClient(
        connector=connector,
        copy_buffer_builder=LocalhostCopyBuilder(),
        pickle_thread_lock=pickle_lock,
        cpp_thread_pool=None,
        verbose_death=True
    )
    if mode == "read":
        local_instance = await client.connect_read(server_identity=server_name, payload=payload)
        assert isinstance(local_instance, TensorPipeReader)
    else:
        local_instance = await client.connect_write(server_identity=server_name, payload=payload)
        assert isinstance(local_instance, TensorPipeWriter)
    assert server_callback.launched_task is not None

    await asyncio.gather(
        server_callback.launched_task,
        _consequent_jobs_coro(
            local_instance=local_instance,
            done_event=done_event,
            count=count,
            payload=payload
        )
    )

    pipe = await client.connect_object_pipe(server_identity=server_name, payload=123)
    assert await pipe.recv() == 125


@pytest.mark.parametrize("mode", ["read", "write"])
@pytest.mark.asyncio
async def test_single_connection_exception(socket_dir, mode):
    server_name = "server"
    addr = ConnectAddress(family=socket.AF_UNIX, path=os.path.join(socket_dir, "0"))
    connector = Connector(addresses={server_name: addr})

    pickle_lock = threading.Lock()

    server = TensorPipeServer(
        address=addr,
        copy_buffer_builder=LocalhostCopyBuilder(),
        accept_callback=ExceptionCallback(),
        pickle_thread_lock=pickle_lock
    )
    await server.start_serving()

    client = TensorPipeClient(
        connector=connector,
        copy_buffer_builder=LocalhostCopyBuilder(),
        pickle_thread_lock=pickle_lock,
        cpp_thread_pool=None,
        verbose_death=True
    )
    with pytest.raises(Exception):
        if mode == "read":
            await client.connect_read(server_identity=server_name, payload=None)
        else:
            await client.connect_write(server_identity=server_name, payload=None)
