import asyncio

import pytest

from maps_adv.stat_controller.server.lib.domains import (
    ChargerStatus,
    CollectorStatus,
    NormalizerStatus,
    SystemOp,
)
from maps_adv.stat_controller.server.tests.tools import dt

url = "/tasks/collector/"

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def system_op(system_op_dm, config):
    return SystemOp(system_op_dm, config["TASK_LIFETIME"])


@pytest.fixture
async def charged(factory):
    return await factory.charger("executor0", dt(100), dt(160), ChargerStatus.completed)


@pytest.mark.usefixtures("charged")
async def test_returns_201(api):
    response = await api.post(url, json={"executor_id": "executor1"})

    assert response.status == 201


async def test_returns_200_if_there_is_no_tasks(api):
    response = await api.post(url, json={"executor_id": "executor1"})
    json = await response.json()

    assert response.status == 200
    assert json == {}


@pytest.mark.parametrize(
    "factory_name, status",
    (
        *[["normalizer", st] for st in list(NormalizerStatus)],
        *[["charger", st] for st in ChargerStatus.in_progress()],
        *[["collector", st] for st in list(CollectorStatus)],
    ),
)
async def test_does_not_use_not_charged_tasks(factory_name, status, api, factory):
    await factory[factory_name]("executor0", dt(100), dt(160), status)

    response = await api.post(url, json={"executor_id": "executor1"})
    json = await response.json()

    assert response.status == 200
    assert json == {}


async def test_returns_task_updated_from_charged(charged, api):
    response = await api.post(url, json={"executor_id": "executor1"})
    json = await response.json()

    assert json["id"] == charged


async def test_always_uses_last_charged(charged, factory, api):
    await factory.charger("executor1", dt(0), dt(60), ChargerStatus.completed)

    response = await api.post(url, json={"executor_id": "executor2"})
    json = await response.json()

    assert json["id"] == charged


async def test_returns_new_while_in_progress(charged, factory, api):
    await factory.collector("executor1", dt(0), dt(60))

    response = await api.post(url, json={"executor_id": "executor2"})
    json = await response.json()

    assert json["id"] == charged


async def returns_new_instead_of_failed_if_available(charged, factory, api):
    await factory.collector("executor1", dt(0), dt(60), failed=True)

    response = await api.post(url, json={"executor_id": "executor2"})
    json = await response.json()

    assert json["id"] == charged


async def test_returns_failed_if_no_other_available(factory, api):
    expected_id = await factory.collector("executor0", dt(0), dt(60), failed=True)
    await factory.charger("executor1", dt(100), dt(160), ChargerStatus.accepted)

    response = await api.post(url, json={"executor_id": "executor2"})
    json = await response.json()

    assert json["id"] == expected_id


async def test_updates_task_status(charged, factory, api):
    await api.post(url, json={"executor_id": "executor1"})

    current_log_id = await factory.cur_log_id(charged)
    details = await factory.collector.details(charged)
    assert details["status"] == CollectorStatus.accepted
    assert details["current_log_id"] == current_log_id


async def test_returns_task_data(charged, api):
    response = await api.post(url, json={"executor_id": "executor1"})
    json = await response.json()

    assert json == {
        "id": charged,
        "timing_from": "1970-01-01T00:01:40+00:00",
        "timing_to": "1970-01-01T00:02:40+00:00",
    }


async def test_returns_reanimated_task_data(system_op, factory, config, api):
    task_id = await factory.collector("executor1", dt(100), dt(160))
    await factory.expire(task_id, config["TASK_LIFETIME"])

    await system_op.mark_expired_tasks_as_failed()
    response = await api.post(url, json={"executor_id": "executor2"})
    json = await response.json()

    assert json == {
        "id": task_id,
        "timing_from": "1970-01-01T00:01:40+00:00",
        "timing_to": "1970-01-01T00:02:40+00:00",
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
@pytest.mark.usefixtures("charged")
async def test_cant_request_two_tasks_in_one_moment(factory, api):
    await factory.charger("executor1", dt(0), dt(60), ChargerStatus.completed)

    async def _request(executor_id, sleep):
        await asyncio.sleep(sleep)
        return await api.post(url, json={"executor_id": executor_id})

    responses = await asyncio.gather(
        _request("executor2", 0), _request("executor3", 0.001)
    )

    assert responses[0].status == 201
    assert responses[1].status == 409
