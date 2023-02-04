import pytest

from maps_adv.common.helpers import dt
from maps_adv.warden.server.lib.domains.tasks import (
    Domain,
    StatusSequenceViolation,
    TaskInProgressByAnotherExecutor,
    UpdateToInitialStatus,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture
def domain(dm, config):
    return Domain(dm, config["EXTRA_EXECUTION_TIME"])


@pytest.mark.parametrize(
    "current, target",
    (
        ["accepted", "completed"],
        ["accepted", "some_status"],
        ["accepted", "failed"],
        ["some_status", "another_status"],
        ["some_status", "completed"],
        ["some_status", "failed"],
    ),
)
@pytest.mark.parametrize("metadata", [None, '{"some": "json"}'])
async def test_updates_task(metadata, current, target, dm, domain):
    dm.retrieve_task_schedule_time.coro.return_value = dt("2019-12-01 06:00:00")
    dm.retrieve_task_type_details.coro.return_value = dict(
        id=100, time_limit=300, schedule="* * * * *"
    )
    dm.retrieve_task_details.coro.return_value = dict(
        status=current,
        executor_id="executor0",
        metadata=metadata,
        scheduled_time=dt("2019-12-01 06:00:00"),
    )

    await domain.update_task("executor0", "task_type", 200, target, metadata)

    dm.update_task.assert_called_with(
        "executor0", 200, target, metadata, "like a connection"
    )


@pytest.mark.freeze_time(dt("2019-12-01 06:00:00"))
async def test_returns_schedule_time_when_updated_to_completed(dm, domain):
    dm.retrieve_task_schedule_time.coro.return_value = dt("2019-12-01 06:00:00")
    dm.retrieve_task_type_details.coro.return_value = dict(
        id=100, time_limit=300, schedule="* * * * *"
    )
    dm.retrieve_task_details.coro.return_value = dict(
        status="current",
        executor_id="executor0",
        metadata=None,
        scheduled_time=dt("2019-12-01 06:00:00"),
    )

    got = await domain.update_task("executor0", "task_type", 200, "completed")

    assert got == dt("2019-12-01 06:01:00")


async def test_returns_nothing_when_updated_to_not_completed(dm, domain):
    dm.retrieve_task_type_details.coro.return_value = dict(
        id=100, time_limit=300, schedule="* * * * *"
    )
    dm.retrieve_task_details.coro.return_value = dict(
        status="current",
        executor_id="executor0",
        metadata=None,
        scheduled_time=dt("2019-12-01 06:00:00"),
    )

    got = await domain.update_task("executor0", "task_type", 200, "any_other_status")

    assert got is None


async def test_raises_for_unexpected_executor(dm, domain):
    dm.retrieve_task_type_details.coro.return_value = dict(
        id=100, time_limit=300, schedule="* * * * *"
    )
    dm.retrieve_task_details.coro.return_value = dict(
        status="accepted",
        executor_id="executor0",
        metadata=None,
        scheduled_time=dt("2019-12-01 06:00:00"),
    )

    with pytest.raises(TaskInProgressByAnotherExecutor):
        await domain.update_task("executor1", "task_type", 200, "completed")

    assert dm.update_task.called is False


@pytest.mark.parametrize(
    "current, target",
    (
        ["some_status", "some_status"],
        ["completed", "some_status"],
        ["completed", "failed"],
        ["failed", "some_status"],
        ["failed", "completed"],
    ),
)
async def test_raises_for_status_sequence_violation(current, target, dm, domain):
    dm.retrieve_task_type_details.coro.return_value = dict(
        id=100, time_limit=300, schedule="* * * * *"
    )
    dm.retrieve_task_details.coro.return_value = dict(
        status=current,
        executor_id="executor0",
        metadata=None,
        scheduled_time=dt("2019-12-01 06:00:00"),
    )

    with pytest.raises(StatusSequenceViolation) as exc_info:
        await domain.update_task("executor0", "task_type", 200, target)

    assert exc_info.value.from_ == current
    assert exc_info.value.to == target

    assert dm.update_task.called is False


@pytest.mark.parametrize(
    "current_status", ("accepted", "completed", "failed", "some_status")
)
async def test_raises_for_initial_status(dm, domain, current_status):
    dm.retrieve_task_type_details.coro.return_value = dict(
        id=100, time_limit=300, schedule="* * * * *"
    )
    dm.retrieve_task_details.coro.return_value = dict(
        status=current_status,
        executor_id="executor0",
        metadata=None,
        scheduled_time=dt("2019-12-01 06:00:00"),
    )

    with pytest.raises(UpdateToInitialStatus):
        await domain.update_task("executor0", "task_type", 200, "accepted")
