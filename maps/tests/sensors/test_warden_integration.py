import asyncio
from unittest.mock import Mock

import aiohttp
import pytest

from maps_adv.common.helpers import Any
from maps_adv.common.lasagna import Lasagna
from maps_adv.warden.client.lib.exceptions import Conflict

pytestmark = [pytest.mark.asyncio]

url = "/sensors/"


class SampleApp(Lasagna):
    async def _setup_layers(self, db):
        return aiohttp.web.Application()


async def test_sensors_are_empty_if_warden_url_is_not_set(run):
    class App(SampleApp):
        TASKS = {"task": Mock()}

    app = App({"WARDEN_TASKS": ["task"]})
    client = await run(app)

    await asyncio.sleep(0.1)

    resp = await client.get(url)
    assert await resp.json() == {"sensors": []}


async def test_sensors_are_empty_if_warden_tasks_are_empty(run):
    class App(SampleApp):
        TASKS = {"task": Mock()}

    app = App({"WARDEN_URL": "http://localhost:100500", "WARDEN_TASKS": []})

    client = await run(app)

    await asyncio.sleep(0.1)

    resp = await client.get(url)
    assert await resp.json() == {"sensors": []}


@pytest.mark.parametrize(
    "side_effect, task_status",
    [
        ([asyncio.sleep(0.1)], "requested"),
        ([asyncio.sleep(0.1)], "accepted"),
        ([asyncio.sleep(0.1)], "completed"),
        ([asyncio.TimeoutError], "failed (timeout)"),
        (Conflict(), "failed (conflict)"),
        (Exception(), "failed"),
    ],
)
async def test_labels_with_task_name_and_task_status(
    side_effect, task_status, run, mock_warden
):
    class App(SampleApp):
        TASKS = {"task_1": Mock(side_effect=side_effect)}
        TASKS_KWARGS_KEYS = ["config", "db"]

    app = App({"WARDEN_URL": "http://localhost:100500", "WARDEN_TASKS": ["task_1"]})

    client = await run(app)
    await asyncio.sleep(0.2)

    resp = await client.get(url)
    assert {
        "labels": {
            "metric_group": "warden_tasks_status",
            "task_name": "task_1",
            "task_status": task_status,
        },
        "type": "RATE",
        "value": Any(int),
    } in (await resp.json())["sensors"]
