from typing import Any, List, Set

from ads_pytorch.model_calcer.minibatch_worker import (
    MinibatchWorkerPool,
    AbstractCalcer,
    CalcerResults,
    AbstractCalcerResultsHandler
)
from ads_pytorch.model_calcer.minibatch_record import MinibatchRecord
import torch
import pytest
import dataclasses


@dataclasses.dataclass
class FakeCalcer(AbstractCalcer):
    commands: List[Any] = dataclasses.field(init=False, default_factory=list)

    async def __call__(self, inputs, targets, data_identity: int = 0) -> CalcerResults:
        return CalcerResults(
            metrics={"1": data_identity},
            input_gradients={"2": torch.FloatTensor([2])},
            predictions={"1": torch.FloatTensor([1])},
            losses={"loss1": 2.5}
        )

    async def send_command(self, name: str, value: Any):
        if name == "raise_exc":
            raise ChildProcessError
        self.commands.append((name, value))

    def get_device(self) -> torch.device:
        return torch.device("cpu")


@pytest.mark.asyncio
async def test_calc():
    pool = MinibatchWorkerPool(
        calcer_factory=lambda idx: FakeCalcer(),
        calcer_results_handlers=[],
        num_workers=1
    )

    async with pool:
        future = await pool.assign_job(MinibatchRecord(inputs=None, targets=None))

    res = await future
    assert res == CalcerResults(
        metrics={"1": 0},
        input_gradients={"2": torch.FloatTensor([2])},
        predictions={"1": torch.FloatTensor([1])},
        losses={"loss1": 2.5}
    )


@pytest.mark.asyncio
async def test_fail_command():
    pool = MinibatchWorkerPool(
        calcer_factory=lambda idx: FakeCalcer(),
        calcer_results_handlers=[],
        num_workers=2
    )
    async with pool:
        with pytest.raises(ChildProcessError):
            await pool.send_command("raise_exc", 145)


@pytest.mark.asyncio
async def test_ok_command():
    calcers = [
        FakeCalcer(),
        FakeCalcer()
    ]
    pool = MinibatchWorkerPool(
        calcer_factory=lambda idx: calcers[idx],
        calcer_results_handlers=[],
        num_workers=2
    )
    async with pool:
        await pool.send_command("exec_cmd", 145)

    assert [calcer.commands == [("exec_cmd", 145)] for calcer in calcers]


@dataclasses.dataclass
class FakeMetricsHandler(AbstractCalcerResultsHandler):
    seen_identities: Set[int] = dataclasses.field(init=False, default_factory=set)
    call_counter: int = 0

    async def __call__(
            self,
            worker_identity: int,
            calcer_res: CalcerResults,
            batch: MinibatchRecord
    ):
        self.call_counter += 1
        self.seen_identities.add(worker_identity)
        assert calcer_res == CalcerResults(
            metrics={"1": worker_identity},
            input_gradients={"2": torch.FloatTensor([2])},
            predictions={"1": torch.FloatTensor([1])},
            losses={"loss1": 2.5}
        )
        if batch.keys is not None:
            raise ChildProcessError
        assert batch == MinibatchRecord(inputs=135, targets=149)


@pytest.mark.asyncio
async def test_metrics_handler():
    handler = FakeMetricsHandler()
    async with MinibatchWorkerPool(
        calcer_factory=lambda idx: FakeCalcer(),
        calcer_results_handlers=[],
        num_workers=10
    ) as pool:
        pool.add_result_handler(handler)
        for _ in range(20):
            await pool.assign_job(MinibatchRecord(inputs=135, targets=149))

    assert handler.seen_identities == set(range(10))
    assert handler.call_counter == 20


@pytest.mark.asyncio
async def test_metrics_handler_exception():
    handler = FakeMetricsHandler()
    with pytest.raises(ChildProcessError):
        async with MinibatchWorkerPool(
            calcer_factory=lambda idx: FakeCalcer(),
            calcer_results_handlers=[],
            num_workers=10
        ) as pool:
            pool.add_result_handler(handler)
            future = await pool.assign_job(MinibatchRecord(inputs=135, targets=149, keys=3))
            await future


@pytest.mark.asyncio
async def test_remove_handler():
    handler = FakeMetricsHandler()
    pool = MinibatchWorkerPool(
        calcer_factory=lambda idx: FakeCalcer(),
        calcer_results_handlers=[],
        num_workers=10
    )
    pool.add_result_handler(handler)
    async with pool:
        for _ in range(10):
            await pool.assign_job(MinibatchRecord(inputs=135, targets=149))
    pool.remove_result_handler(handler)
    async with pool:
        for _ in range(10):
            await pool.assign_job(MinibatchRecord(inputs=135, targets=149))

    assert handler.call_counter == 10


@pytest.mark.asyncio
async def test_add_weakref():
    pool = MinibatchWorkerPool(
        calcer_factory=lambda idx: FakeCalcer(),
        calcer_results_handlers=[],
        num_workers=10
    )

    async def _exec_with_handler(pool):
        handler = FakeMetricsHandler()
        pool.add_result_handler(handler)
        async with pool:
            for _ in range(10):
                await pool.assign_job(MinibatchRecord(inputs=135, targets=149))

    await _exec_with_handler(pool)
    assert len(pool._calcer_result_handlers) == 0


# test predefined set of commands


@pytest.mark.asyncio
async def test_predefined_commands():
    calcers = [
        FakeCalcer(),
        FakeCalcer(),
        FakeCalcer()
    ]
    pool = MinibatchWorkerPool(
        calcer_factory=lambda idx: calcers[idx],
        calcer_results_handlers=[],
        num_workers=3
    )

    async with pool:
        await pool.set_train_mode(False)
        await pool.set_get_predictions(True)

    assert all(calcer.commands == [
        ("set_train_mode", False),
        ("set_get_predictions", True)
    ] for calcer in calcers)
