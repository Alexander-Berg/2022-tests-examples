import asyncio
from unittest.mock import Mock

import pytest

from maps_adv.common.helpers import dt
from maps_adv.warden.client.lib import PeriodicalTask
from maps_adv.warden.client.lib.exceptions import (
    Conflict,
    TaskTypeAlreadyAssigned,
    TooEarlyForNewTask,
)

pytestmark = [pytest.mark.asyncio]


async def test_works_correctly_without_sensors(mocker, client_factory):
    mocker.patch.object(PeriodicalTask, "_relaunch_interval_after_exception", new=0)

    task = PeriodicalTask("no_sensors", Mock([asyncio.sleep(0.1)]))

    try:
        await task(client_factory)
    except Exception:
        pytest.fail("Should not raise")


async def test_initiates_metric_group_empty(periodical_task, metric_group):
    periodical_task(Mock())

    assert metric_group.serialize("azaza") == []


@pytest.mark.parametrize(
    "side_effect, task_status",
    [
        ([asyncio.sleep(0.1)], "requested"),
        ([asyncio.sleep(0.1)], "accepted"),
        ([asyncio.sleep(0.1)], "completed"),
        (asyncio.TimeoutError(), "failed (timeout)"),
        (Conflict(), "failed (conflict)"),
        (TaskTypeAlreadyAssigned(), "failed (conflict)"),
        (
            TooEarlyForNewTask(
                next_try_proto_dt=dt("2020-10-01 18:00:00", as_proto=True)
            ),
            "failed (conflict)",
        ),
        (Exception, "failed"),
    ],
)
async def test_composes_sensors_with_correct_data(
    side_effect, task_status, client_factory, periodical_task, metric_group
):
    task = periodical_task(Mock(side_effect=side_effect))

    await task(client_factory)

    assert {
        "labels": {
            "metric_group": "azaza",
            "task_name": "task_1",
            "task_status": task_status,
        },
        "type": "RATE",
        "value": 1,
    } in metric_group.serialize("azaza")


async def test_increments_counters_correctly_each_task_launch(
    client_factory, periodical_task, metric_group
):
    task = periodical_task(Mock(side_effect=[asyncio.TimeoutError, asyncio.sleep(0.1)]))

    await task(client_factory)
    await task(client_factory)

    assert {
        s["labels"]["task_status"]: s["value"] for s in metric_group.serialize("azaza")
    } == {"requested": 2, "accepted": 2, "failed (timeout)": 1, "completed": 1}


async def test_counts_every_task_separately(
    client_factory, periodical_task, metric_group
):
    task1 = periodical_task(Mock(side_effect=[asyncio.sleep(0.1)]), name="task1")
    task2 = periodical_task(Mock(side_effect=Conflict()), name="task2")

    await task1(client_factory)
    await asyncio.sleep(0.1)
    await task2(client_factory)

    assert [
        dict(
            task_name=s["labels"]["task_name"],
            task_status=s["labels"]["task_status"],
            value=s["value"],
        )
        for s in metric_group.serialize("azaza")
    ] == [
        {"task_name": "task1", "task_status": "requested", "value": 1},
        {"task_name": "task1", "task_status": "accepted", "value": 1},
        {"task_name": "task1", "task_status": "completed", "value": 1},
        {"task_name": "task2", "task_status": "requested", "value": 1},
        {"task_name": "task2", "task_status": "accepted", "value": 1},
        {"task_name": "task2", "task_status": "failed (conflict)", "value": 1},
    ]
