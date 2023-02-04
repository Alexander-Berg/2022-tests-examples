import pytest
import asyncio
import torch

from ads_pytorch.tools.multiprocessing import get_multiprocessing_context
from ads_pytorch.tools.socketpair_transport import socketpair_transport
from ads_pytorch.tools.tensor_pipe import TensorPipe
from ads_pytorch.model_calcer.deep.gpu.update_server import (
    UpdateServer,
    WorkerSideTransport,
    make_updater_transports
)


PROCESS_START_TIMEOUT = 90
pytestmark = pytest.mark.requires_cuda


##############################################################
#                   UpdateServer Unit tests                  #
##############################################################


class CopyGradOptim(torch.optim.Optimizer):
    def __init__(self, params):
        defaults = dict()
        super(CopyGradOptim, self).__init__(params, defaults)

    @torch.no_grad()
    def step(self, closure=None):
        for group in self.param_groups:
            for p in group['params']:
                if p.grad is not None:
                    p.copy_(p.grad)


# IMPORTANT: CUDA send/recv CAN'T be done in same process
# (segfault), so we have to test everything via multiprocessing

# PLS don't make it a fixture - by somewhat reason cleaning up fixtures with multiprocessing
# results in many garbage in test-stderr
def _prepare_updater(optim_cls=torch.optim.SGD):
    gpu0 = torch.device("cuda", 0)
    model = torch.nn.Sequential(
        torch.nn.Linear(1000, 1000),
        torch.nn.ReLU(inplace=True),
        torch.nn.LayerNorm(1000),
        torch.nn.Linear(1000, 1000),
        torch.nn.ReLU(inplace=True),
        torch.nn.LayerNorm(1000),
        torch.nn.Linear(1000, 1000),
        torch.nn.ReLU(inplace=True),
        torch.nn.LayerNorm(1000),
        torch.nn.Linear(1000, 1),
    ).to(gpu0)
    updated_parameters = dict(model.named_parameters())
    optimizers = [
        optim_cls(
            list(updated_parameters.values()),
            lr=1
        )
    ]

    mp_ctx = get_multiprocessing_context()
    worker_transport, updater_transport = make_updater_transports(ctx=mp_ctx)
    sync_reader, sync_writer = TensorPipe(threaded_reader=False, threaded_writer=False)
    host_sock, subproc_sock = socketpair_transport()

    command_queue = mp_ctx.Queue()
    update_server = UpdateServer(
        optimizers=optimizers,
        updated_parameters=updated_parameters,
        transports=[updater_transport],
        command_queue=command_queue,
        synchronize_reader=sync_reader,
        synchronize_writer=sync_writer,
        host_synchronize_socket=host_sock,
        subproc_synchronize_socket=subproc_sock
    )

    proc = mp_ctx.Process(
        target=update_server.run,
        daemon=True
    )
    proc.start()
    got_result = command_queue.get(timeout=PROCESS_START_TIMEOUT)
    if got_result is not True:
        raise got_result

    return worker_transport, model, updated_parameters, optimizers, command_queue, update_server


@pytest.mark.parametrize("device_id", [0, 1])
@pytest.mark.asyncio
async def test_get_replica(device_id):
    worker_transport, model, updated_parameters, optimizers, _, server = _prepare_updater()
    worker_transport: WorkerSideTransport

    # device_id
    replica_parameters = await worker_transport.request_parameters_replica(device_id=device_id)

    assert set(updated_parameters.keys()) == set(replica_parameters.keys())
    for k in updated_parameters:
        assert replica_parameters[k].device.index == device_id
        assert replica_parameters[k].storage().data_ptr() != updated_parameters[k].storage().data_ptr()
        assert torch.allclose(updated_parameters[k].cpu(), replica_parameters[k].cpu())


@pytest.mark.parametrize("sync_times", [1, 5])
@pytest.mark.parametrize("device_id", [0, 1])
@pytest.mark.asyncio
async def test_update(device_id, sync_times):
    worker_transport, model, updated_parameters, optimizers, _, server = _prepare_updater()
    worker_transport: WorkerSideTransport

    def _check():
        for k in updated_parameters:
            assert replica_parameters[k].device.index == device_id
            assert replica_parameters[k].storage().data_ptr() != updated_parameters[k].storage().data_ptr()
            assert torch.allclose(updated_parameters[k].cpu(), replica_parameters[k].cpu())

    replica_parameters = await worker_transport.request_parameters_replica(device_id=device_id)

    assert set(updated_parameters.keys()) == set(replica_parameters.keys())
    _check()

    for i in range(1, sync_times + 1):
        prev_params = {key: param.cpu().clone() for key, param in (await server.host_synchronize()).items()}

        # Wait until we update parameters
        await worker_transport.send_gradients({
            key: torch.full_like(param, fill_value=i + 0.5)
            for key, param in updated_parameters.items()
        })
        await worker_transport.wait_previous_update()

        for k in replica_parameters.keys():
            assert torch.allclose(replica_parameters[k].cpu(), prev_params[k].cpu())

        # Ask for a new replica
        replica_parameters = await worker_transport.request_parameters_replica(device_id=device_id)

        # Check we've updated parameters
        for k in replica_parameters.keys():
            assert torch.allclose(replica_parameters[k].cpu(), prev_params[k].cpu() - (i + 0.5))


@pytest.mark.parametrize("sync_times", [1, 5])
@pytest.mark.parametrize("device_id", [0, 1])
@pytest.mark.asyncio
async def test_update_several_nosync(device_id, sync_times):
    worker_transport, model, updated_parameters, optimizers, _, server = _prepare_updater()
    worker_transport: WorkerSideTransport

    start_parameters = {key: param.cpu().clone() for key, param in updated_parameters.items()}

    for i in range(1, sync_times + 1):
        await worker_transport.send_gradients({
            key: torch.full_like(param, fill_value=i + 0.5)
            for key, param in updated_parameters.items()
        })

    new_parameters = await server.host_synchronize()
    replica_parameters = await worker_transport.request_parameters_replica(device_id=device_id)

    fill_value = sum([i + 0.5 for i in range(1, sync_times + 1)])
    for _newp in [new_parameters, replica_parameters]:
        for k in start_parameters.keys():
            assert torch.allclose(_newp[k].cpu(), start_parameters[k].cpu() - fill_value)


class ToCatch(Exception):
    pass


class FailOptimizer(torch.optim.SGD):
    def step(self, *args, **kwargs):
        raise ToCatch


@pytest.mark.parametrize("device_id", [0, 1])
@pytest.mark.asyncio
async def test_update_fails(device_id):
    worker_transport, model, updated_parameters, optimizers, cmd_queue, server = _prepare_updater(
        optim_cls=FailOptimizer
    )

    await worker_transport.request_parameters_replica(device_id=device_id)

    # Blocking send to ensure we have sended when go to queue
    await worker_transport.send_gradients({
        key: torch.full_like(param, fill_value=0.5)
        for key, param in updated_parameters.items()
    })

    # This must end with exception in queue. In training mode, queue is processed
    # by worker pool itself. It will rethrow it on any error and kill whole training
    with pytest.raises(ToCatch):
        ex = cmd_queue.get()
        assert isinstance(ex, ToCatch)
        raise ex


@pytest.mark.parametrize("device_id", [0, 1])
@pytest.mark.asyncio
async def test_update_with_different_nones(device_id):
    sync_times = 5
    worker_transport, model, updated_parameters, optimizers, _, server = _prepare_updater()
    worker_transport: WorkerSideTransport

    for i in range(1, sync_times + 1):
        prev_params = {key: param.cpu().clone() for key, param in (await server.host_synchronize()).items()}

        await worker_transport.send_gradients({
            key: (None if j == i else torch.full_like(param, fill_value=i + 0.5))
            for j, (key, param) in enumerate(updated_parameters.items())
        })
        await worker_transport.wait_previous_update()
        replica_parameters = await worker_transport.request_parameters_replica(device_id=device_id)

        for j, k in enumerate(replica_parameters.keys()):
            if j != i:
                assert torch.allclose(replica_parameters[k].cpu(), prev_params[k].cpu() - (i + 0.5))
            else:
                # none grad
                assert torch.allclose(replica_parameters[k].cpu(), prev_params[k].cpu())


# Threaded check
# It's REALLY hard to check whether we don't have update race conditions here
# However, we can test behavior of our updater in parallel with different settings
# (however, we can sync in parallel with update...)
# pass


@pytest.mark.parametrize("device_id", [0, 1], ids=["SameGPU", "OtherGPU"])
@pytest.mark.asyncio
async def test_multiple_threads(device_id):
    workers_count = 20

    gpu0 = torch.device("cuda", 0)
    model = torch.nn.Sequential(
        torch.nn.Linear(1000, 1000),
        torch.nn.ReLU(inplace=True),
        torch.nn.LayerNorm(1000),
        torch.nn.Linear(1000, 1000),
        torch.nn.ReLU(inplace=True),
        torch.nn.LayerNorm(1000),
        torch.nn.Linear(1000, 1000),
        torch.nn.ReLU(inplace=True),
        torch.nn.LayerNorm(1000),
        torch.nn.Linear(1000, 1),
    ).to(gpu0)
    updated_parameters = dict(model.named_parameters())
    # Pay attention, it's important for final comparison
    for v in updated_parameters.values():
        torch.nn.init.zeros_(v)
    optimizers = [
        torch.optim.SGD(
            list(updated_parameters.values()),
            lr=1
        )
    ]

    mp_ctx = get_multiprocessing_context()

    transports = [
        make_updater_transports(ctx=mp_ctx)
        for _ in range(workers_count)
    ]

    command_queue = mp_ctx.Queue()
    sync_reader, sync_writer = TensorPipe(threaded_reader=False, threaded_writer=False)
    host_sock, subproc_sock = socketpair_transport()

    update_server = UpdateServer(
        optimizers=optimizers,
        updated_parameters=updated_parameters,
        transports=[x[1] for x in transports],
        command_queue=command_queue,
        synchronize_reader=sync_reader,
        synchronize_writer=sync_writer,
        host_synchronize_socket=host_sock,
        subproc_synchronize_socket=subproc_sock
    )

    proc = mp_ctx.Process(
        target=update_server.run,
        daemon=True
    )
    proc.start()
    got_result = command_queue.get(timeout=PROCESS_START_TIMEOUT)
    if got_result is not True:
        raise got_result

    # Okay, lets have fun - launch 20 parallel coroutines spamming gradient-sync and
    # finally we have to check that we've correctly added all updates

    done_counter = workers_count

    async def _worker(worker_id: int):
        transport: WorkerSideTransport = transports[worker_id][0]

        for i in range(10):
            await transport.send_gradients({
                key: torch.full_like(param, fill_value=i + 0.5)
                for j, (key, param) in enumerate(updated_parameters.items())
            })
            await transport.wait_previous_update()

        nonlocal done_counter
        done_counter -= 1
        if done_counter == 0:
            total_updates = sum([i + 0.5 for i in range(10)]) * workers_count
            await asyncio.sleep(1)
            replica_parameters = await transport.request_parameters_replica(device_id=device_id)

            for v in replica_parameters.values():
                assert torch.allclose(v, torch.full_like(v, fill_value=-total_updates))

    await asyncio.gather(*[_worker(worker_id=i) for i in range(workers_count)])

    assert done_counter == 0
