from ads_pytorch.tools.broadcasted_context import (
    SlaveSingleConnectionBarrier,
    MasterSingleConnectionBarrier
)
from ads_pytorch.tools.wait_subprocess_future import BaseRemoteFutureWaiter, DummyRemoteFutureWaiter
from ads_pytorch.tools.socketpair_transport import ConnectAddress
import pytest
import os
import tempfile
import socket
import asyncio
import random
import dataclasses
from typing import Dict


##################################################################
#                        Barrier testing                         #
##################################################################


@pytest.fixture
def socket_folder():
    with tempfile.TemporaryDirectory() as tmp:
        yield tmp


@dataclasses.dataclass
class AddressBuilder:
    folder: str
    _counter: int = dataclasses.field(init=False, default=0)

    def __call__(self) -> ConnectAddress:
        path = os.path.join(self.folder, str(self._counter))
        self._counter += 1
        return ConnectAddress(family=socket.AF_UNIX, path=path)


@pytest.mark.asyncio
async def test_barrier_no_slaves():
    master_barrier = MasterSingleConnectionBarrier(
        slave_addresses=[],
        future_waiters=[]
    )

    for _ in range(3):
        async with master_barrier.context():
            pass


@pytest.mark.parametrize("job_count", [1, 10000], ids=["SingleTask", "ManyTasks"])
@pytest.mark.asyncio
async def test_barrier(socket_folder, job_count):
    addr_builder = AddressBuilder(folder=socket_folder)
    slaves = [
        SlaveSingleConnectionBarrier(
            address=addr_builder(),
            future_waiter=DummyRemoteFutureWaiter()
        )
        for _ in range(job_count)
    ]
    master_barrier = MasterSingleConnectionBarrier(
        slave_addresses=[x.address for x in slaves],
        future_waiters=[DummyRemoteFutureWaiter() for _ in slaves]
    )

    counter = 0

    async def _slave_coroutine(barrier: SlaveSingleConnectionBarrier, sleep_time: float):
        nonlocal counter
        await asyncio.sleep(sleep_time)
        counter += 1
        async with barrier.context():
            assert counter == job_count
        assert counter == job_count

    # at Nirvana, this will be done via waiting until our slave listens the port
    await asyncio.gather(*[slave.init() for slave in slaves])

    loop = asyncio.get_running_loop()
    tasks = [
        loop.create_task(_slave_coroutine(barrier=slave, sleep_time=random.random() / 10))
        for slave in slaves
    ]

    async with master_barrier.context():
        assert counter == job_count

    # gather slave tasks and raise on assertion
    await asyncio.gather(*tasks)


class ToCatch(Exception):
    pass


@dataclasses.dataclass
class CustomDeathWaiter(BaseRemoteFutureWaiter):
    all_tasks_reference: Dict[int, bool]  # this is common dict reference
    id: int
    poll_timeout: float = 0.1

    async def is_alive(self) -> bool:
        return self.all_tasks_reference[self.id]

    async def get_exception(self) -> BaseException:
        return ToCatch(f"dead {self.id}")


# We do not separately test slave's death after master because this test already checks it
@pytest.mark.asyncio
async def test_future_waiter_all_die(socket_folder):
    job_count = 1
    # [0; job_count) are slaves and job_count is master
    all_jobs_reference = {i: True for i in range(job_count + 1)}

    addr_builder = AddressBuilder(folder=socket_folder)
    slaves = [
        SlaveSingleConnectionBarrier(
            address=addr_builder(),
            future_waiter=CustomDeathWaiter(
                all_tasks_reference=all_jobs_reference,
                id=job_count  # master id!
            )
        )
        for _ in range(job_count)
    ]
    master_barrier = MasterSingleConnectionBarrier(
        slave_addresses=[x.address for x in slaves],
        future_waiters=[CustomDeathWaiter(all_tasks_reference=all_jobs_reference, id=i) for i in range(len(slaves))]
    )

    async def _slave_coroutine(
        barrier: SlaveSingleConnectionBarrier,
        slave_id: int
    ):
        try:
            while True:
                await asyncio.sleep(random.random() / 10)
                async with barrier.context():
                    pass
        except BaseException:
            nonlocal all_jobs_reference
            all_jobs_reference[slave_id] = False
            raise

    await asyncio.gather(*[slave.init() for slave in slaves])

    loop = asyncio.get_running_loop()
    tasks = [
        loop.create_task(_slave_coroutine(barrier=slave, slave_id=i))
        for i, slave in enumerate(slaves)
    ]

    # run for a while to ensure everything has started
    for i in range(2):
        async with master_barrier.context():
            pass

    all_jobs_reference[job_count] = False

    # check all slaves die after master's death
    for task in tasks:
        with pytest.raises(ToCatch):
            await task

    # check master die after slave death
    with pytest.raises(ToCatch):
        while True:
            async with master_barrier.context():
                pass
