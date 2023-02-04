import asyncio
from unittest.mock import Mock

import aiohttp
import pytest

from maps_adv.common.helpers import Any
from maps_adv.common.lasagna import Lasagna
from maps_adv.warden.client.lib import TaskContext

pytestmark = [pytest.mark.asyncio]


class SampleApp(Lasagna):
    async def _setup_layers(self, db):
        return aiohttp.web.Application()


async def test_not_calls_task_if_warden_url_is_not_set(run):
    task = Mock()

    class App(SampleApp):
        TASKS = {"task": Mock()}

    app = App({"WARDEN_TASKS": ["task"]})
    await run(app)
    await asyncio.sleep(0.1)

    assert task.called is False


async def test_calls_task_as_expected_if_warden_url_set(run, mock_warden):
    mock_1 = Mock()
    mock_2 = Mock()

    class App(SampleApp):
        TASKS = {"task_1": mock_1, "task_2": mock_2}
        TASKS_KWARGS_KEYS = ["config", "db"]

    app = App(
        {"WARDEN_URL": "http://localhost:100500", "WARDEN_TASKS": ["task_1", "task_2"]}
    )
    await run(app)
    await asyncio.sleep(0.1)

    mock_1.assert_called_with(Any(TaskContext), config=app.config, db=app.db)
    mock_2.assert_called_with(Any(TaskContext), config=app.config, db=app.db)


async def test_calls_only_specified_tasks(run, mock_warden):
    mock_1 = Mock()
    mock_2 = Mock()

    class App(SampleApp):
        TASKS = {"task_1": mock_1, "task_2": mock_2}
        TASKS_KWARGS_KEYS = ["config", "db"]

    app = App({"WARDEN_URL": "http://localhost:100500", "WARDEN_TASKS": ["task_1"]})
    await run(app)
    await asyncio.sleep(0.1)

    assert mock_1.called is True
    assert mock_2.called is False


async def test_no_tasks_called_if_no_tasks_specified(run, mock_warden):
    mock_1 = Mock()
    mock_2 = Mock()

    class App(SampleApp):
        TASKS = {"task_1": mock_1, "task_2": mock_2}
        TASKS_KWARGS_KEYS = ["config", "db"]

    app = App({"WARDEN_URL": "http://localhost:100500", "WARDEN_TASKS": []})
    await run(app)
    await asyncio.sleep(0.1)

    assert mock_1.called is False
    assert mock_2.called is False


async def test_no_tasks_called_if_tasks_config_value_missing(run, mock_warden):
    mock_1 = Mock()
    mock_2 = Mock()

    class App(SampleApp):
        TASKS = {"task_1": mock_1, "task_2": mock_2}
        TASKS_KWARGS_KEYS = ["config", "db"]

    app = App({"WARDEN_URL": "http://localhost:100500"})
    await run(app)
    await asyncio.sleep(0.1)

    assert mock_1.called is False
    assert mock_2.called is False
