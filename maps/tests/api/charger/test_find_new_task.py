import asyncio

import pytest

from maps_adv.stat_controller.server.lib.domains import (
    ChargerStatus,
    NormalizerStatus,
    SystemOp,
)
from maps_adv.stat_controller.server.tests.tools import dt

url = "/tasks/charger/"

pytestmark = [pytest.mark.asyncio]

_not_important_statuses = [
    ChargerStatus.accepted,
    ChargerStatus.context_received,
    ChargerStatus.calculation_completed,
]

_important_statuses = [ChargerStatus.billing_notified, ChargerStatus.charged_data_sent]


@pytest.fixture
def system_op(system_op_dm, config):
    return SystemOp(system_op_dm, config["TASK_LIFETIME"])


@pytest.fixture
async def normalized(factory):
    return await factory.normalizer(
        "executor0", dt(0), dt(60), NormalizerStatus.completed
    )


@pytest.mark.usefixtures("normalized")
async def test_returns_201(api):
    response = await api.post(url, json={"executor_id": "executor0"})

    assert response.status == 201


async def test_returns_200_if_there_is_no_tasks(api):
    response = await api.post(url, json={"executor_id": "executor0"})
    json = await response.json()

    assert response.status == 200
    assert json == {}


async def test_does_not_use_charged_tasks(api, factory):
    await factory.charger("executor0", dt(100), dt(160), ChargerStatus.completed)

    response = await api.post(url, json={"executor_id": "executor1"})
    json = await response.json()

    assert response.status == 200
    assert json == {}


async def test_returns_task_updated_from_normalized(normalized, api):
    response = await api.post(url, json={"executor_id": "executor1"})
    json = await response.json()

    assert json["id"] == normalized


async def test_always_uses_oldest_not_charged(normalized, factory, api):
    await factory.normalizer("executor1", dt(100), dt(160), NormalizerStatus.completed)

    response = await api.post(url, json={"executor_id": "executor0"})
    json = await response.json()

    assert json["id"] == normalized


@pytest.mark.parametrize(
    "status", [el for el in ChargerStatus if el != ChargerStatus.completed]
)
async def test_does_not_return_new_while_in_progress(status, factory, api):
    await factory.charger("executor0", dt(0), dt(60), status)
    await factory.normalizer("executor1", dt(100), dt(160), NormalizerStatus.completed)

    response = await api.post(url, json={"executor_id": "executor3"})
    json = await response.json()

    assert response.status == 200
    assert json == {}


async def test_returns_new_if_no_in_progress(factory, api):
    await factory.charger("executor0", dt(0), dt(60), ChargerStatus.completed)
    expected_id = await factory.normalizer(
        "executor1", dt(100), dt(160), NormalizerStatus.completed
    )

    response = await api.post(url, json={"executor_id": "executor3"})
    json = await response.json()

    assert json["id"] == expected_id


@pytest.mark.parametrize(
    "status", [el for el in ChargerStatus if el != ChargerStatus.completed]
)
async def returns_older_failed_instead_of_new(status, factory, api):
    expected_id = await factory.charger("executor0", dt(0), dt(60), status, failed=True)
    await factory.normalizer("executor1", dt(100), dt(160), NormalizerStatus.completed)

    response = await api.post(url, json={"executor_id": "executor0"})
    json = await response.json()

    assert json["id"] == expected_id


async def test_updates_task_status(normalized, factory, api):
    await api.post(url, json={"executor_id": "executor1"})

    current_log_id = await factory.cur_log_id(normalized)
    details = await factory.charger.details(normalized)

    assert details["status"] == ChargerStatus.accepted
    assert details["current_log_id"] == current_log_id


@pytest.mark.parametrize("status", _important_statuses)
async def test_does_not_update_reanimated_task_status_for_important_statuses(
    status, system_op, factory, config, api
):
    task_id = await factory.charger("executor0", dt(100), dt(160), status)
    await factory.expire(task_id, config["TASK_LIFETIME"])

    await system_op.mark_expired_tasks_as_failed()
    await api.post(url, json={"executor_id": "executor0"})

    current_log_id = await factory.cur_log_id(task_id)
    details = await factory.charger.details(task_id)

    assert details["status"] == status
    assert details["current_log_id"] == current_log_id


@pytest.mark.parametrize("status", _not_important_statuses)
async def test_does_updates_reanimated_task_status_for_not_important_statuses(
    status, system_op, factory, config, api
):
    task_id = await factory.charger("executor0", dt(100), dt(160), status)
    await factory.expire(task_id, config["TASK_LIFETIME"])

    await system_op.mark_expired_tasks_as_failed()
    await api.post(url, json={"executor_id": "executor0"})

    current_log_id = await factory.cur_log_id(task_id)
    details = await factory.charger.details(task_id)

    assert details["status"] == ChargerStatus.accepted
    assert details["current_log_id"] == current_log_id


async def test_returns_task_data(normalized, api):
    response = await api.post(url, json={"executor_id": "executor1"})
    json = await response.json()

    assert json == {
        "id": normalized,
        "timing_from": "1970-01-01T00:00:00+00:00",
        "timing_to": "1970-01-01T00:01:00+00:00",
        "status": "accepted",
    }


@pytest.mark.parametrize("status", _important_statuses)
async def test_returns_reanimated_task_data_for_important_statuses(
    status, system_op, factory, config, api
):
    task_id = await factory.charger(
        "executor1",
        dt(100),
        dt(160),
        status,
        state=["a", {"int": 10, "decimal-like": "100.35"}],
    )
    await factory.expire(task_id, config["TASK_LIFETIME"])

    await system_op.mark_expired_tasks_as_failed()
    response = await api.post(url, json={"executor_id": "executor2"})
    json = await response.json()

    assert json == {
        "id": task_id,
        "timing_from": "1970-01-01T00:01:40+00:00",
        "timing_to": "1970-01-01T00:02:40+00:00",
        "status": status.value,
        "execution_state": ["a", {"int": 10, "decimal-like": "100.35"}],
    }


@pytest.mark.parametrize("status", _not_important_statuses)
async def test_returns_reanimated_task_data_for_not_important_statuses(
    status, system_op, factory, config, api
):
    task_id = await factory.charger(
        "executor1",
        dt(100),
        dt(160),
        status,
        state=["a", {"int": 10, "decimal-like": "100.35"}],
    )
    await factory.expire(task_id, config["TASK_LIFETIME"])

    await system_op.mark_expired_tasks_as_failed()
    response = await api.post(url, json={"executor_id": "executor2"})
    json = await response.json()

    assert json == {
        "id": task_id,
        "timing_from": "1970-01-01T00:01:40+00:00",
        "timing_to": "1970-01-01T00:02:40+00:00",
        "status": "accepted",
    }


async def test_errored_for_empty_executor_id(api):
    response = await api.post(url, json={"executor_id": ""})
    json = await response.json()

    assert response.status == 400
    assert json == {"executor_id": ["Value should not be empty."]}


async def test_errored_for_empty_data(api):
    response = await api.post(url, json={})
    json = await response.json()

    assert response.status == 400
    assert json == {"executor_id": ["Missing data for required field."]}


@pytest.mark.real_db
@pytest.mark.usefixtures("normalized")
async def test_cant_request_two_tasks_in_one_moment(factory, api):
    await factory.normalizer("executor1", dt(100), dt(160), NormalizerStatus.completed)

    async def _request(executor_id, sleep):
        await asyncio.sleep(sleep)
        return await api.post(url, json={"executor_id": executor_id})

    responses = await asyncio.gather(
        _request("executor2", 0), _request("executor3", 0.001)
    )

    assert responses[0].status == 201
    assert responses[1].status == 409
