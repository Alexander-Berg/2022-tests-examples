from datetime import datetime, timezone
from typing import List, Optional
from unittest.mock import Mock

import pytest
from asyncpg import Connection

from maps_adv.common.helpers import coro_mock, dt
from maps_adv.warden.server.lib import Application
from maps_adv.warden.server.lib.data_managers.tasks import (
    AbstractDataManager as AbstractTasksDataManager,
    DataManager as TasksDataManager,
)
from maps_adv.warden.server.lib.db import DB

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "smb.common.pgswim.pytest.plugin",
    "maps_adv.common.lasagna.pytest.plugin",
]


_config = dict(
    DATABASE_URL="postgresql://warden:warden@localhost:5433/warden",
    EXTRA_EXECUTION_TIME=1,
    WARDEN_URL=None,
    WARDEN_TASKS=[],
)


def pytest_configure(config):
    config.addinivalue_line(
        "addopts", "--database-url=postgresql://warden:warden@localhost:5433/warden"
    )
    config.addinivalue_line("markers", "mock_dm")


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture(scope="session")
def config():
    return _config.copy()


@pytest.fixture(scope="session", autouse=True)
def pgswim_engine_cls():
    return DB


@pytest.fixture
def dm(request):
    if request.node.get_closest_marker("mock_dm"):
        return request.getfixturevalue("_mock_dm")
    return request.getfixturevalue("_dm")


@pytest.fixture
def _dm(db):
    return TasksDataManager(db)


@pytest.fixture
def _mock_dm():
    class MockLock:
        async def __aenter__(self):
            return "like a connection"

        async def __aexit__(self, exc_type, *args, **kwargs):
            pass

    class MockDm(AbstractTasksDataManager):
        create_task = coro_mock()
        update_task = coro_mock()
        restore_task = coro_mock()
        retrieve_task_details = coro_mock()
        retrieve_task_type_details = coro_mock()
        is_there_task_in_progress = coro_mock()
        find_last_task_of_type = coro_mock()
        retrieve_active_task_details = coro_mock()
        mark_task_as_failed = coro_mock()
        mark_tasks_as_failed = coro_mock()
        retrieve_task_schedule_time = coro_mock()
        retrieve_last_failed_task_details = coro_mock()
        is_executor_id_exists = coro_mock()
        lock = Mock(side_effect=lambda *a: MockLock())

    mock_dm = MockDm()
    mock_dm.is_executor_id_exists.coro.return_value = False

    return mock_dm


@pytest.fixture
def app(config):
    return Application(config)


class Factory:
    def __init__(self, con: Connection):
        self.con = con

    async def create_task_type(
        self,
        name: str,
        time_limit: Optional[int] = 300,
        schedule: Optional[str] = "* * * * *",
        restorable: Optional[bool] = False,
    ) -> int:
        sql = """
        INSERT INTO task_types (name, time_limit, schedule, restorable)
        VALUES ($1, $2, $3, $4)
        RETURNING id
        """
        return await self.con.fetchval(sql, name, time_limit, schedule, restorable)

    async def create_task(
        self,
        executor_id: str,
        type_id: int,
        status: Optional[str] = None,
        scheduled_time: Optional[datetime] = None,
        metadata: Optional[str] = None,
        created: Optional[datetime] = None,
    ) -> dict:
        if created is None:
            created = datetime.now(tz=timezone.utc)
        if scheduled_time is None:
            scheduled_time = dt("2019-12-01 06:00:00")

        create_task_sql = """
            INSERT INTO tasks (type_id, status, created, intake_time, scheduled_time)
            VALUES ($1, 'accepted', $2, $2, $3)
            RETURNING id
        """

        create_log_sql = """
            WITH new_log AS (
                INSERT INTO tasks_log (task_id, status, executor_id, metadata)
                SELECT $1, 'accepted', $2, $3
                RETURNING id
            )
            UPDATE tasks
            SET current_log_id = new_log.id
            FROM new_log
            WHERE tasks.id = $1
        """

        async with self.con.transaction():
            task_id = await self.con.fetchval(
                create_task_sql, type_id, created, scheduled_time
            )
            await self.con.execute(create_log_sql, task_id, executor_id, metadata)

        if status not in (None, "accepted"):
            await self.update_task(executor_id, task_id, status)

        return dict(task_id=task_id, status=status)

    async def update_task(
        self,
        executor_id: str,
        task_id: int,
        status: str,
        metadata: Optional[str] = None,
        intake_time: Optional[datetime] = None,
    ) -> None:
        sql = """
            WITH new_log AS (
                INSERT INTO tasks_log (task_id, status, executor_id, metadata)
                SELECT $1, $2, $3, $4
                RETURNING id
            )
            UPDATE tasks
            SET status = $2,
                current_log_id = new_log.id,
                intake_time = COALESCE($5::timestamp with time zone, intake_time)
            FROM new_log
            WHERE tasks.id = $1
        """

        await self.con.execute(sql, task_id, status, executor_id, metadata, intake_time)

    async def task_details(self, task_id: int) -> dict:
        sql = """
            SELECT
                tasks.status,
                tasks_log.executor_id,
                tasks_log.metadata,
                tasks.scheduled_time
            FROM tasks
            JOIN tasks_log ON tasks.current_log_id = tasks_log.id
            WHERE tasks.id = $1
        """

        row = await self.con.fetchrow(sql, task_id)
        return dict(row)

    async def task_logs(self, task_id: int) -> List[dict]:
        sql = """
            SELECT *
            FROM tasks_log
            WHERE task_id = $1
            ORDER BY created, id
        """

        row = await self.con.fetch(sql, task_id)
        return list(map(dict, row))


@pytest.fixture
async def factory(con):
    return Factory(con)


@pytest.fixture
async def another_type_id(factory):
    return await factory.create_task_type("another_task_type", time_limit=600)


@pytest.fixture
async def type_id(another_type_id, factory):
    return await factory.create_task_type(
        "task_type", time_limit=300, schedule="*/5 * * * *"
    )


@pytest.fixture
async def restorable_type_id(another_type_id, factory):
    return await factory.create_task_type(
        "restorable", time_limit=300, schedule="*/5 * * * *", restorable=True
    )


@pytest.fixture
async def task_id(factory, type_id):
    return (await factory.create_task("executor0", type_id))["task_id"]
