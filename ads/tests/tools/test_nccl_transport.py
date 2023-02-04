import os
import time
import pickle
from typing import Any, Union, Optional
import pytest
import torch
import threading
import socket
import dataclasses

from ads_pytorch.tools.tensor_pipe import (
    TensorPipeReader,
    TensorPipeWriter,
    TensorPipeClient,
    TensorPipeServer,
    IServerCallback
)

from ads_pytorch.tools.tensor_pipe.nccl import NCCLCopyBuilder

from ads_pytorch.tools import (
    ConnectAddress,
    Connector,
)

import asyncio
import uvloop
import tempfile

from ads_pytorch.cpp_lib import libcpp_lib
from ads_pytorch.tools.cpu_count import get_cpu_count

import multiprocessing as mp
import functools

uvloop.install()


def _asyncio_run(task):
    loop = asyncio.get_event_loop_policy().new_event_loop()
    loop.run_until_complete(task)


@pytest.fixture
def socket_dir():
    with tempfile.TemporaryDirectory(dir=".") as tmp:
        yield tmp


class ExceptionCallback(IServerCallback):
    async def __call__(
        self,
        action: bytes,
        server_identity: str,
        local_instance: Union[TensorPipeReader, TensorPipeWriter],
        payload: Any,
    ) -> None:
        raise RuntimeError("OOPS")


#################################################################
#                  NCCL transport tests                  #
#################################################################


def _run_nccl(rank: int, temp_name: str, return_dict, world_size: int):
    if rank == 0:
        nccl_id = libcpp_lib.NCCLUniqueID.request_ncclid()
        with open(temp_name, "wb") as f:
            pickle.dump(nccl_id, f)
    else:
        while not os.path.exists(temp_name):
            time.sleep(1)
        nccl_id = pickle.load(open(temp_name, "rb"))

    nccl_comm = libcpp_lib.NCCLComm.create(2, rank, nccl_id)

    torch.manual_seed(13)

    N = 100

    error = False

    if rank == 0:
        ncclContext = libcpp_lib.NCCLTransportContext(0, 0, 1, 2, nccl_comm)
        for i in range(world_size):
            send_tensor = torch.randn((N,)).to(torch.device(rank))
            ncclContext.send(send_tensor, 1)
    else:
        recv_tensor = torch.zeros((N,)).to(torch.device(rank))
        ncclContext = libcpp_lib.NCCLTransportContext(1, 1, 0, 2, nccl_comm)

        for i in range(world_size):
            randn_tensor = torch.randn((N,)).to(torch.device(rank))
            ncclContext.recv(recv_tensor, 0)
            if not torch.allclose(recv_tensor, randn_tensor):
                error = True
                break
    return_dict[rank] = error


@dataclasses.dataclass
class GPUCalcerImplResults:
    cpu_tensor: Optional[torch.Tensor] = dataclasses.field(default=None)
    gpu_tensor: Optional[torch.Tensor] = dataclasses.field(default=None)


async def __process_minibatch_loop(
    forward_reader: TensorPipeReader, backward_writer: TensorPipeWriter, rank: int
):
    device = torch.device(rank)
    send_tensor_cpu: torch.Tensor = torch.zeros((10)) + rank
    send_tensor_gpu: torch.Tensor = torch.zeros((10)).to(device) + rank

    await backward_writer.coro_send(
        GPUCalcerImplResults(send_tensor_cpu, send_tensor_gpu)
    )


async def _schedule_process_minibatch_loop(
    forward_reader: TensorPipeReader, backward_writer: TensorPipeWriter, rank: int
):
    loop = asyncio.get_running_loop()
    loop.create_task(
        __process_minibatch_loop(
            forward_reader=forward_reader, backward_writer=backward_writer, rank=rank
        )
    )


@dataclasses.dataclass
class DullReadWriteCallback(IServerCallback):
    rank: int
    reader: TensorPipeReader = None
    writer: TensorPipeWriter = None

    async def __call__(
        self,
        action: bytes,
        server_identity: str,
        local_instance: Union[TensorPipeReader, TensorPipeWriter],
        payload: Any,
    ) -> None:
        if isinstance(local_instance, TensorPipeReader):
            self.reader = local_instance
        else:
            self.writer = local_instance

        if self.reader and self.writer:
            await _schedule_process_minibatch_loop(
                forward_reader=self.reader, backward_writer=self.writer, rank=self.rank
            )


def _run_nccl_server_process(i, addr, n):
    pickle_lock = threading.Lock()
    server = TensorPipeServer(
        address=addr,
        copy_buffer_builder=NCCLCopyBuilder(i + 1, n + 1),
        accept_callback=DullReadWriteCallback(i + 1),
        pickle_thread_lock=pickle_lock,
    )

    async def _run_coro():
        await server.start_serving()
        await asyncio.sleep(10)

    loop = asyncio.get_event_loop()
    loop.run_until_complete(_run_coro())


# TODO: discuss with alxmoro3ov why loop is closed with len(world_size) > 1
@pytest.skip("Temporarily disabled")
@pytest.mark.parametrize("world_size", [1])
@pytest.mark.asyncio
async def test_connection_read_write(socket_dir, world_size):
    server_name_host = "worker_0"
    server_names = [f"worker_{i + 1}" for i in range(world_size)]

    addrs = [
        ConnectAddress(family=socket.AF_UNIX, path=os.path.join(".", str(i)))
        for i in range(world_size)
    ]

    connectors = Connector(
        addresses={sn: addr for sn, addr in zip(server_names, addrs)}
    )

    pickle_lock = threading.Lock()

    mp_ctx = mp.get_context("spawn")
    servers = [
        mp_ctx.Process(
            target=functools.partial(_run_nccl_server_process, i, addr, world_size),
            daemon=True,
        )
        for i, addr in enumerate(addrs)
    ]
    for item in servers:
        item.start()

    helper_thread_pool = libcpp_lib.ThreadPoolHandle(get_cpu_count())

    client = TensorPipeClient(
        connector=connectors,
        copy_buffer_builder=NCCLCopyBuilder(0, world_size + 1),
        pickle_thread_lock=pickle_lock,
        cpp_thread_pool=helper_thread_pool,
        verbose_death=False,
    )
    reader, writer = {}, {}

    async def get_res():
        async def _d(server_name):
            nonlocal reader, writer
            reader[server_name] = await client.connect_read(
                server_identity=server_name,
            )
            writer[server_name] = await client.connect_write(
                server_identity=server_name,
            )

        await asyncio.gather(*(_d(sn) for sn in server_names))

    await asyncio.gather(get_res())

    eps = 1e-6
    result = {}
    for i, server_name in enumerate(server_names):
        result[server_name]: GPUCalcerImplResults = await reader[
            server_name
        ].coro_recv()
        assert result[server_name].gpu_tensor.mean().item() - i - 1 < eps
        assert result[server_name].cpu_tensor.mean().item() - i - 1 < eps

    for item in servers:
        item.join()


@pytest.skip("Temporarily disabled")
@pytest.mark.parametrize("world_size", [3])
@pytest.mark.asyncio
async def test_simple_nccl_pipe_file_id(world_size: int):
    manager = mp.Manager()
    return_dict = manager.dict()

    temp_name = os.path.join(
        tempfile._get_default_tempdir(), next(tempfile._get_candidate_names())
    )

    ctx = mp.get_context("spawn")
    pool = [
        ctx.Process(target=_run_nccl, args=(i, temp_name, return_dict, world_size))
        for i in range(2)
    ]
    for p in pool:
        p.start()
    for p in pool:
        p.join()
    assert sum(return_dict.values()) == 0

