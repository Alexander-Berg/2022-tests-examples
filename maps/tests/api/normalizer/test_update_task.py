import pytest

from maps_adv.stat_controller.server.lib.domains import NormalizerStatus
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
async def task_id(factory):
    return await factory.normalizer("executor0", dt(100), dt(160))


async def test_returns_200(api, task_id):
    response = await api.put(
        f"/tasks/normalizer/{task_id}/",
        json={"status": "completed", "executor_id": "executor0"},
    )

    assert response.status == 200


async def test_errored_for_invalid_task_id(api):
    response = await api.put(
        "/tasks/normalizer/kek/", json={"status": "accepted", "executor_id": "20"}
    )
    json = await response.json()

    assert response.status == 400
    assert json == {"task_id": ["Not a valid integer."]}


@pytest.mark.parametrize(
    "payload, error",
    (
        [
            {"status": "accepted", "executor_id": ""},
            {"executor_id": ["Value should not be empty."]},
        ],
        [
            {"status": "kek", "executor_id": "20"},
            {"status": ["Invalid enum member kek"]},
        ],
        [{"executor_id": "20"}, {"status": ["Missing data for required field."]}],
        [{"status": "accepted"}, {"executor_id": ["Missing data for required field."]}],
    ),
)
async def test_errored_for_invalid_payload(payload, error, api):
    response = await api.put("/tasks/normalizer/10/", json=payload)
    json = await response.json()

    assert response.status == 400
    assert json == error


async def test_errored_if_in_progress_by_another_executor(api, task_id):
    response = await api.put(
        f"/tasks/normalizer/{task_id}/",
        json={"status": "completed", "executor_id": "executor1"},
    )
    json = await response.json()

    assert response.status == 400
    assert json == {
        "executor_id": [
            f"task_id = {task_id}, status = completed, "
            f"executor_id = executor0 -> executor1"
        ]
    }


async def test_update_task_to_completed(api, task_id, factory):
    await api.put(
        f"/tasks/normalizer/{task_id}/",
        json={"status": "completed", "executor_id": "executor0"},
    )

    details = await factory.normalizer.details(task_id)
    assert details["status"] == NormalizerStatus.completed


async def test_returns_task_data(api, task_id):
    response = await api.put(
        f"/tasks/normalizer/{task_id}/",
        json={"status": "completed", "executor_id": "executor0"},
    )
    json = await response.json()

    assert json == {
        "id": task_id,
        "timing_from": "1970-01-01T00:01:40+00:00",
        "timing_to": "1970-01-01T00:02:40+00:00",
    }
