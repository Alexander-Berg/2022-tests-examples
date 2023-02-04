import random

import pytest
from unittest.mock import MagicMock, patch, AsyncMock, call

from src.change_registry.pull_from_registry import PullData
from src.pull_from_logbroker import PullLogBrokerData
from src.main_tasks import start_tasks, shutdown_tasks, periodic


@pytest.mark.asyncio
async def test_start_tasks():
    app = MagicMock()
    container = MagicMock()
    lock_provider = MagicMock()

    with patch('src.main_tasks.periodic') as periodic_patch:
        with patch('src.main_tasks.Container', return_value=container):
            with patch('src.main_tasks.PgAdvisoryLockProvider', return_value=lock_provider):
                await start_tasks(app)
                assert len(app.state.tasks) == 2
                registry_call = call(container, app, PullData, lock_provider, 1 * 60, 30 * 60)
                department_call = call(container, app, PullLogBrokerData, lock_provider, 1 * 60, 30 * 60)
                periodic_patch.assert_has_calls([registry_call, department_call])


@pytest.mark.asyncio
async def test_shutdown_tasks():
    app = MagicMock()
    app.state.tasks = [MagicMock() for _ in range(random.randint(3, 5))]

    await shutdown_tasks(app)

    for task in app.state.tasks:
        task.cancel.assert_called_once_with()


@pytest.mark.asyncio
async def test_periodic():
    async def _sleep(_):
        raise StopIteration('Intended from test')

    app = MagicMock()
    task = AsyncMock()
    task.run.return_value = True
    task_type = MagicMock(__module__=f'module{random.random()}', __qualname__=f'qualname{random.random()}')
    task_name = f'{task_type.__module__}.{task_type.__qualname__}'
    container = MagicMock()
    container.resolve.return_value = task

    lock_provider = MagicMock()
    lock = AsyncMock()
    lock_provider.acquire_lock.return_value = lock
    delay = random.random()
    timeout = random.randint(30, 50)

    with patch('src.main_tasks.asyncio.sleep', _sleep):
        try:
            await periodic(container, app, task_type, lock_provider, delay, timeout)
            raise Exception('StopIteration expected')
        except RuntimeError as e:
            assert e.args[0] == 'coroutine raised StopIteration'

        lock_provider.acquire_lock.assert_called_once_with(app, task_name, timeout)
        task.run.assert_called_once_with(app)
        container.resolve.assert_called_once_with(task_type)
        lock.__aenter__.assert_called_once()
        lock.__aexit__.assert_called_once()
