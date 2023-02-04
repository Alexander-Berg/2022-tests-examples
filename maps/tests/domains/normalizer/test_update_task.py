import pytest

from maps_adv.stat_controller.server.lib.domains import (
    InProgressByAnotherExecutor,
    Normalizer,
    NormalizerStatus,
    StatusSequenceViolation,
)
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def domain(normalizer_dm, config):
    return Normalizer(
        normalizer_dm,
        config["TIME_LAG"],
        config["MIN_TIME_RANGE"],
        config["MAX_TIME_RANGE"],
        config["MAX_TIME_RANGE_TO_SKIP"],
    )


async def test_will_update_task_to_passed_status(factory, domain):
    task_id = await factory.normalizer("executor0", dt(100), dt(160))

    got = await domain.update("executor0", task_id, NormalizerStatus.completed)

    assert got == {"id": task_id, "timing_from": dt(100), "timing_to": dt(160)}


@pytest.mark.parametrize(
    "current, target",
    (
        [NormalizerStatus.accepted, NormalizerStatus.accepted],
        [NormalizerStatus.completed, NormalizerStatus.accepted],
        [NormalizerStatus.completed, NormalizerStatus.completed],
    ),
)
async def test_raises_for_status_sequence_violation(current, target, domain, factory):
    task_id = await factory.normalizer("executor0", dt(100), dt(160), current)

    with pytest.raises(
        StatusSequenceViolation,
        match=(
            f"task_id = {task_id}, executor_id = executor0, "
            f"status = {current.value} -> {target.value}"
        ),
    ):
        await domain.update("executor0", task_id, target)


async def test_raises_if_task_in_progress_by_another_executor(factory, domain):
    task_id = await factory.normalizer("executor0", dt(100), dt(160))

    with pytest.raises(
        InProgressByAnotherExecutor,
        match=(
            f"task_id = {task_id}, status = completed, "
            f"executor_id = executor0 -> executor1"
        ),
    ):
        await domain.update("executor1", task_id, NormalizerStatus.completed)
