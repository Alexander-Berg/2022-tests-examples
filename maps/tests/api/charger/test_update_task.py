import pytest

from maps_adv.stat_controller.server.lib.domains import ChargerStatus
from maps_adv.stat_controller.server.tests.tools import dt

pytestmark = [pytest.mark.asyncio]


_status_sequences = [
    (ChargerStatus.accepted, ChargerStatus.context_received),
    (ChargerStatus.context_received, ChargerStatus.calculation_completed),
    (ChargerStatus.calculation_completed, ChargerStatus.billing_notified),
    (ChargerStatus.billing_notified, ChargerStatus.charged_data_sent),
    (ChargerStatus.charged_data_sent, ChargerStatus.completed),
]


def url(task_id: int) -> str:
    return f"/tasks/charger/{task_id}/"


@pytest.fixture
async def task_id(factory):
    return await factory.charger("executor0", dt(100), dt(160))


async def test_returns_200(api, task_id):
    response = await api.put(
        url(task_id),
        json={
            "status": "context_received",
            "executor_id": "executor0",
            "execution_state": "some_state",
        },
    )

    assert response.status == 200


async def test_errored_for_invalid_task_id(api):
    response = await api.put(
        url("kek"),
        json={
            "status": "context_received",
            "executor_id": "executor0",
            "execution_state": "some_state",
        },
    )
    json = await response.json()

    assert response.status == 400
    assert json == {"task_id": ["Not a valid integer."]}


@pytest.mark.parametrize("current, target", [s for s in _status_sequences])
async def test_will_update_task_to_passed_status(current, target, factory, api):
    task_id = await factory.charger("executor0", dt(100), dt(160), current)

    await api.put(
        url(task_id),
        json={
            "status": target.value,
            "executor_id": "executor0",
            "execution_state": "some_state",
        },
    )

    details = await factory.charger.details(task_id)
    assert details["status"] == target


async def test_will_update_execution_state(factory, api):
    task_id = await factory.charger(
        "executor0", dt(100), dt(160), ChargerStatus.accepted, "current_state"
    )
    await api.put(
        url(task_id),
        json={
            "status": "context_received",
            "executor_id": "executor0",
            "execution_state": "target_state",
        },
    )

    details = await factory.charger.details(task_id)
    assert details["execution_state"] == "target_state"


@pytest.mark.parametrize(
    "payload, error",
    (
        [
            {
                "status": "context_received",
                "executor_id": "",
                "execution_state": "some_state",
            },
            {"executor_id": ["Value should not be empty."]},
        ],
        [
            {"status": "kek", "executor_id": "sample", "execution_state": "some_state"},
            {"status": ["Invalid enum member kek"]},
        ],
        [
            {"executor_id": "sample", "execution_state": "some_state"},
            {"status": ["Missing data for required field."]},
        ],
        [
            {"status": "context_received", "execution_state": "some_state"},
            {"executor_id": ["Missing data for required field."]},
        ],
        [
            {"status": "context_received", "executor_id": "sample"},
            {"execution_state": ["Missing data for required field."]},
        ],
    ),
)
async def test_errored_for_invalid_payload(task_id, payload, error, api):
    response = await api.put(url(task_id), json=payload)
    json = await response.json()

    assert response.status == 400
    assert json == error


@pytest.mark.parametrize(
    "current, target",
    [
        (current, target)
        for current in ChargerStatus
        for target in ChargerStatus
        if (current, target) not in _status_sequences
    ],
)
async def test_errored_for_status_sequence_violation(current, target, factory, api):
    task_id = await factory.charger("executor0", dt(100), dt(160), current)

    response = await api.put(
        url(task_id),
        json={
            "status": target.value,
            "executor_id": "executor0",
            "execution_state": "some_state",
        },
    )
    json = await response.json()

    assert response.status == 400
    assert json == {
        "status": [
            f"task_id = {task_id}, executor_id = executor0, "
            f"status = {current.value} -> {target.value}"
        ]
    }


async def test_errored_if_in_progress_by_another_executor(task_id, api):
    response = await api.put(
        url(task_id),
        json={
            "status": "context_received",
            "executor_id": "executor1",
            "execution_state": "some_state",
        },
    )
    json = await response.json()

    assert response.status == 400
    assert json == {
        "executor_id": [
            f"task_id = {task_id}, status = context_received, "
            f"executor_id = executor0 -> executor1"
        ]
    }


async def test_returns_task_data(task_id, api):
    response = await api.put(
        url(task_id),
        json={
            "status": "context_received",
            "executor_id": "executor0",
            "execution_state": "some_state",
        },
    )
    json = await response.json()

    assert json == {
        "id": task_id,
        "timing_from": "1970-01-01T00:01:40+00:00",
        "timing_to": "1970-01-01T00:02:40+00:00",
    }
