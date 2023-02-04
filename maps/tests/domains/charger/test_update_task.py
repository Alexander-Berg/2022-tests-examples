import pytest

from maps_adv.stat_controller.server.lib.domains import (
    Charger,
    ChargerStatus,
    InProgressByAnotherExecutor,
    StatusSequenceViolation,
)
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]

_status_sequences = [
    (ChargerStatus.accepted, ChargerStatus.context_received),
    (ChargerStatus.context_received, ChargerStatus.calculation_completed),
    (ChargerStatus.calculation_completed, ChargerStatus.billing_notified),
    (ChargerStatus.billing_notified, ChargerStatus.charged_data_sent),
    (ChargerStatus.charged_data_sent, ChargerStatus.completed),
]


@pytest.fixture
def domain(charger_dm):
    return Charger(charger_dm)


@pytest.fixture
async def accepted(factory):
    return await factory.charger("executor0", dt(100), dt(160))


@pytest.mark.parametrize("current, target", [s for s in _status_sequences])
async def test_will_update_task_to_passed_status(current, target, domain, factory):
    task_id = await factory.charger("executor0", dt(100), dt(160), current)

    await domain.update("executor0", task_id, target, "some_state")

    details = await factory.charger.details(task_id)
    assert details["status"] == target


async def test_will_update_execution_state(factory, domain):
    task_id = await factory.charger(
        "executor0", dt(100), dt(160), ChargerStatus.accepted, "current_state"
    )

    await domain.update(
        "executor0", task_id, ChargerStatus.context_received, "target_state"
    )

    details = await factory.charger.details(task_id)
    assert details["execution_state"] == "target_state"


@pytest.mark.parametrize(
    "current, target",
    [
        (current, target)
        for current in ChargerStatus
        for target in ChargerStatus
        if (current, target) not in _status_sequences
    ],
)
async def test_raises_for_status_sequence_violation(current, target, domain, factory):
    task_id = await factory.charger("executor0", dt(100), dt(160), current)

    with pytest.raises(
        StatusSequenceViolation,
        match=(
            f"task_id = {task_id}, executor_id = executor0, "
            f"status = {current.value} -> {target.value}"
        ),
    ):
        await domain.update("executor0", task_id, target, "some_state")


async def test_raises_if_task_in_progress_by_another_executor(accepted, domain):
    with pytest.raises(
        InProgressByAnotherExecutor,
        match=(
            f"task_id = {accepted}, status = context_received, "
            f"executor_id = executor0 -> executor1"
        ),
    ):
        await domain.update(
            "executor1", accepted, ChargerStatus.context_received, "some_state"
        )


async def test_returns_task_data(accepted, domain):
    got = await domain.update(
        "executor0", accepted, ChargerStatus.context_received, "some_state"
    )

    assert got == {"id": accepted, "timing_from": dt(100), "timing_to": dt(160)}
