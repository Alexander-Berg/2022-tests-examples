import pytest

from maps_adv.stat_controller.server.lib.domains import (
    Collector,
    CollectorStatus,
    InProgressByAnotherExecutor,
    StatusSequenceViolation,
)
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def domain(collector_dm):
    return Collector(collector_dm)


@pytest.fixture
async def accepted(factory):
    return await factory.collector("executor0", dt(100), dt(160))


async def test_will_update_task_to_passed_status(accepted, domain, factory):
    await domain.update("executor0", accepted, CollectorStatus.completed)

    details = await factory.collector.details(accepted)
    assert details["status"] == CollectorStatus.completed


async def test_raises_for_status_sequence_violation(domain, factory):
    task_id = await factory.collector(
        "executor0", dt(100), dt(160), CollectorStatus.completed
    )

    with pytest.raises(
        StatusSequenceViolation,
        match=(
            f"task_id = {task_id}, executor_id = executor0, "
            f"status = completed -> accepted"
        ),
    ):
        await domain.update("executor0", task_id, CollectorStatus.accepted)


async def test_raises_if_task_in_progress_by_another_executor(accepted, domain):
    with pytest.raises(
        InProgressByAnotherExecutor,
        match=(
            f"task_id = {accepted}, status = completed, "
            f"executor_id = executor0 -> executor1"
        ),
    ):
        await domain.update("executor1", accepted, CollectorStatus.completed)


async def test_returns_task_data(accepted, domain):
    got = await domain.update("executor0", accepted, CollectorStatus.completed)

    assert got == {"id": accepted, "timing_from": dt(100), "timing_to": dt(160)}
