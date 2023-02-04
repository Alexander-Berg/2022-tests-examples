import pytest

from maps_adv.common.helpers import dt
from maps_adv.warden.server.lib.domains.tasks import (
    Domain,
    ExecutorIdAlreadyUsed,
    TaskTypeAlreadyAssigned,
    TooEarlyForNewTask,
)

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.mock_dm,
    pytest.mark.freeze_time(dt("2019-12-01 06:03:00")),
]


@pytest.fixture
def domain(dm, config):
    return Domain(dm, config["EXTRA_EXECUTION_TIME"])


@pytest.mark.parametrize(
    "created, last_scheduled, new_scheduled",
    [
        # scheduled
        (
            dt("2019-12-01 05:30:00"),
            dt("2019-12-01 05:49:00"),
            dt("2019-12-01 05:56:00"),
        ),
        # schedule_time for new task based on scheduled_time of old one
        (
            dt("2019-12-01 16:00:00"),
            dt("2019-12-01 05:49:00"),
            dt("2019-12-01 05:56:00"),
        ),
    ],
)
@pytest.mark.parametrize("metadata", [None, {"some": "json"}])
async def test_creates_task(
    metadata, created, last_scheduled, new_scheduled, dm, domain
):
    dm.retrieve_task_type_details.coro.return_value = dict(
        id=100, time_limit=300, schedule="*/7 * * * *", restorable=True
    )
    dm.retrieve_active_task_details.coro.return_value = None
    dm.retrieve_last_failed_task_details.coro.return_value = None
    dm.find_last_task_of_type.coro.return_value = dict(
        created=created, id=10, scheduled_time=last_scheduled
    )
    dm.create_task.coro.return_value = dict(task_id=200, status="accepted")

    got = await domain.create_task("executor0", "task_type", metadata)

    assert got == dict(task_id=200, status="accepted", time_limit=300)
    dm.create_task.assert_called_with(
        "executor0", 100, new_scheduled, metadata, "like a connection"
    )


async def test_acquires_lock(dm, domain):
    dm.retrieve_task_type_details.coro.return_value = dict(
        id=100, time_limit=300, schedule="*/7 * * * *", restorable=True
    )
    dm.retrieve_active_task_details.coro.return_value = None
    dm.retrieve_last_failed_task_details.coro.return_value = None
    dm.find_last_task_of_type.coro.return_value = dict(
        created=dt("2019-12-01 05:54:00"),
        id=10,
        scheduled_time=dt("2019-12-01 05:49:00"),
    )
    dm.create_task.coro.return_value = dict(task_id=200, status="accepted")

    await domain.create_task("executor0", "task_type")

    dm.lock.assert_called_with("task_type")


@pytest.mark.freeze_time(dt("2019-12-01 06:01:00"))
async def test_raises_if_there_is_task_in_progress(dm, domain):
    dm.retrieve_task_type_details.coro.return_value = dict(
        id=100, time_limit=300, schedule="*/7 * * * *"
    )
    dm.retrieve_active_task_details.coro.return_value = {
        "id": 200,
        "intake_time": dt("2019-12-01 06:00:00"),
        "status": "accepted",
        "metadata": None,
    }

    with pytest.raises(TaskTypeAlreadyAssigned, match="task_type"):
        await domain.create_task("executor0", "task_type")

    assert dm.create_task.called is False


async def test_raises_if_too_early_for_start(dm, domain):
    dm.retrieve_task_type_details.coro.return_value = dict(
        id=100, time_limit=300, schedule="*/7 * * * *", restorable=True
    )
    dm.retrieve_active_task_details.coro.return_value = None
    dm.retrieve_last_failed_task_details.coro.return_value = None
    dm.find_last_task_of_type.coro.return_value = dict(
        created=dt("2019-12-01 06:00:00"),
        scheduled_time=dt("2019-12-01 06:00:00"),
        id=10,
    )

    with pytest.raises(TooEarlyForNewTask) as exc:
        await domain.create_task("executor0", "task_type")

    assert exc.value.scheduled_time == dt("2019-12-01 06:07:00")
    assert dm.create_task.called is False


@pytest.mark.parametrize("failed_metadata", [None, {"some": "json"}])
async def test_restores_last_failed_task_if_resporable(failed_metadata, dm, domain):
    dm.retrieve_task_type_details.coro.return_value = dict(
        id=100, time_limit=300, schedule="*/7 * * * *", restorable=True
    )
    dm.retrieve_active_task_details.coro.return_value = None
    dm.retrieve_last_failed_task_details.coro.return_value = dict(
        id=300, status="some_status", metadata=failed_metadata
    )

    got = await domain.create_task("executor0", "task_type", {"meta": "data"})

    assert dm.create_task.called is False
    dm.restore_task.assert_called_with(
        executor_id="executor0",
        task_id=300,
        status="some_status",
        metadata=failed_metadata,
        con="like a connection",
    )

    assert got == dict(
        task_id=300, status="some_status", time_limit=300, metadata=failed_metadata
    )


async def test_restores_last_failed_despite_schedule_if_restorable(dm, domain):
    dm.retrieve_task_type_details.coro.return_value = dict(
        id=100, time_limit=300, schedule="* * * * *", restorable=True
    )
    dm.retrieve_active_task_details.coro.return_value = None
    dm.retrieve_last_failed_task_details.coro.return_value = dict(
        id=300, status="some_status", metadata={"meta": "data"}
    )

    got = await domain.create_task("executor0", "task_type")

    assert dm.create_task.called is False
    assert dm.find_last_task_of_type.called is False
    dm.restore_task.assert_called_with(
        executor_id="executor0",
        task_id=300,
        status="some_status",
        metadata={"meta": "data"},
        con="like a connection",
    )

    assert got == dict(
        task_id=300, status="some_status", time_limit=300, metadata={"meta": "data"}
    )


async def test_returns_new_task_if_there_is_one_and_not_restorable(dm, domain):
    dm.retrieve_task_type_details.coro.return_value = dict(
        id=100, time_limit=300, schedule="*/5 * * * *", restorable=False
    )
    dm.retrieve_active_task_details.coro.return_value = None
    dm.find_last_task_of_type.coro.return_value = dict(
        created=dt("2019-12-01 05:40:00"),
        id=10,
        scheduled_time=dt("2019-12-01 05:40:00"),
    )
    dm.create_task.coro.return_value = dict(task_id=200, status="accepted")

    got = await domain.create_task("executor0", "task_type", {"meta": "data"})

    dm.create_task.assert_called_with(
        "executor0",
        100,
        dt("2019-12-01 05:45:00"),
        {"meta": "data"},
        "like a connection",
    )

    assert dm.retrieve_last_failed_task_details.called is False

    assert got == dict(task_id=200, status="accepted", time_limit=300)


@pytest.mark.freeze_time("2019-12-01 06:06:00")
async def test_marks_task_in_progress_as_failed_if_it_is_too_old(dm, domain):
    dm.retrieve_task_type_details.coro.return_value = dict(
        id=100, time_limit=300, schedule="*/7 * * * *", restorable=True
    )
    dm.retrieve_active_task_details.coro.return_value = {
        "id": 200,
        "intake_time": dt("2019-12-01 06:00:00"),
        "status": "accepted",
        "metadata": None,
    }
    dm.retrieve_last_failed_task_details.coro.return_value = None

    await domain.create_task("executor0", "task_type", {"meta": "data"})

    dm.mark_task_as_failed.assert_called_with(200, "like a connection")


@pytest.mark.freeze_time("2019-12-01 06:06:00")
async def test_restores_too_old_task_if_restorable(dm, domain):
    dm.retrieve_task_type_details.coro.return_value = dict(
        id=100, time_limit=300, schedule="*/7 * * * *", restorable=True
    )
    dm.retrieve_active_task_details.coro.return_value = {
        "id": 200,
        "intake_time": dt("2019-12-01 06:00:00"),
        "status": "some_status",
        "metadata": {"old": "meta"},
    }
    dm.retrieve_last_failed_task_details.coro.return_value = None

    got = await domain.create_task("executor0", "task_type", {"new": "meta"})

    dm.create_task.assert_not_called()
    dm.retrieve_last_failed_task_details.assert_not_called()
    dm.restore_task.assert_called_with(
        executor_id="executor0",
        task_id=200,
        status="some_status",
        metadata={"old": "meta"},
        con="like a connection",
    )
    assert got == dict(
        task_id=200, status="some_status", time_limit=300, metadata={"old": "meta"}
    )


@pytest.mark.freeze_time("2019-12-01 06:06:00")
async def test_returns_new_task_if_there_is_too_old_one_and_not_restorable(dm, domain):
    dm.retrieve_task_type_details.coro.return_value = dict(
        id=100, time_limit=300, schedule="*/7 * * * *", restorable=False
    )
    dm.retrieve_active_task_details.coro.return_value = {
        "id": 200,
        "intake_time": dt("2019-12-01 06:00:00"),
        "status": "some_status",
        "metadata": {"old": "meta"},
    }
    dm.retrieve_last_failed_task_details.coro.return_value = None
    dm.find_last_task_of_type.coro.return_value = dict(
        created=dt("2019-12-01 05:54:00"),
        id=10,
        scheduled_time=dt("2019-12-01 05:49:00"),
    )
    dm.create_task.coro.return_value = dict(task_id=200, status="accepted")

    got = await domain.create_task("executor0", "task_type", {"new": "meta"})

    dm.create_task.assert_called_with(
        "executor0",
        100,
        dt("2019-12-01 05:56:00"),
        {"new": "meta"},
        "like a connection",
    )
    assert dm.retrieve_last_failed_task_details.called is False
    assert got == dict(task_id=200, status="accepted", time_limit=300)


async def test_raises_if_executor_id_already_exists(dm, domain):
    dm.is_executor_id_exists.coro.return_value = True

    with pytest.raises(ExecutorIdAlreadyUsed):
        await domain.create_task("executor0", "task_type")
