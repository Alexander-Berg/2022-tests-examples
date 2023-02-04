import pytest

from maps_adv.stat_controller.server.lib.domains import CollectorStatus
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


def url(task_id: int) -> str:
    return f"/tasks/collector/{task_id}/"


@pytest.fixture
async def task_id(factory):
    return await factory.collector("executor0", dt(100), dt(160))


async def test_returns_200(api, task_id):
    response = await api.put(
        url(task_id), json={"status": "completed", "executor_id": "executor0"}
    )

    assert response.status == 200


async def test_errored_for_invalid_task_id(api):
    response = await api.put(
        url("kek"), json={"status": "completed", "executor_id": "executor0"}
    )
    json = await response.json()

    assert response.status == 400
    assert json == {"task_id": ["Not a valid integer."]}


async def test_will_update_task_to_passed_status(task_id, factory, api):
    await api.put(
        url(task_id), json={"status": "completed", "executor_id": "executor0"}
    )

    details = await factory.collector.details(task_id)
    assert details["status"] == CollectorStatus.completed


@pytest.mark.parametrize(
    "payload, error",
    (
        [
            {"status": "kek", "executor_id": "executor0"},
            {"status": ["Invalid enum member kek"]},
        ],
        [
            {"executor_id": "executor0"},
            {"status": ["Missing data for required field."]},
        ],
        [
            {"status": "completed"},
            {"executor_id": ["Missing data for required field."]},
        ],
    ),
)
async def test_errored_for_invalid_payload(payload, error, task_id, api):
    response = await api.put(url(task_id), json=payload)
    json = await response.json()

    assert response.status == 400
    assert json == error


async def test_errored_for_status_sequence_violation(factory, api):
    task_id = await factory.collector(
        "executor0", dt(100), dt(160), CollectorStatus.completed
    )

    response = await api.put(
        url(task_id), json={"status": "accepted", "executor_id": "executor0"}
    )
    json = await response.json()

    assert response.status == 400
    assert json == {
        "status": [
            f"task_id = {task_id}, executor_id = executor0, "
            "status = completed -> accepted"
        ]
    }


async def test_errored_if_in_progress_by_another_executor(task_id, api):
    response = await api.put(
        url(task_id), json={"status": "completed", "executor_id": "executor1"}
    )
    json = await response.json()

    assert response.status == 400
    assert json == {
        "executor_id": [
            f"task_id = {task_id}, status = completed, "
            f"executor_id = executor0 -> executor1"
        ]
    }


async def test_returns_task_data(task_id, api):
    response = await api.put(
        url(task_id), json={"status": "completed", "executor_id": "executor0"}
    )
    json = await response.json()

    assert json == {
        "id": task_id,
        "timing_from": "1970-01-01T00:01:40+00:00",
        "timing_to": "1970-01-01T00:02:40+00:00",
    }
