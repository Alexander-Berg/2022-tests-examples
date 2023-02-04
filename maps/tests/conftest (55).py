import time
from datetime import datetime, timedelta, timezone

import aiohttp.pytest_plugin
import alembic.command
import alembic.config
import psycopg2
import pytest
from asyncpg import Connection

from maps_adv.stat_controller.server.lib import Application
from maps_adv.stat_controller.server.lib.data_managers import (
    Charger,
    ChargerStatus,
    Collector,
    CollectorStatus,
    Normalizer,
    NormalizerStatus,
    SystemOp,
)
from maps_adv.stat_controller.server.lib.db import DB

pytest_plugins = ["aiohttp.pytest_plugin"]
del aiohttp.pytest_plugin.loop

_config = {
    "DATABASE_URL": "postgresql://stat:stat@localhost:5433/stat",
    "DATABASE_URL_RO": None,
    "MIN_TIME_RANGE": 600,
    "MAX_TIME_RANGE": 1200,
    "MAX_TIME_RANGE_TO_SKIP": 60,
    "TIME_LAG": 30,
    "TASK_LIFETIME": 400,
    "TIME_PERIOD_AUTO_MARK_EXPIRED_TASKS": None,
}


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture(scope="session")
def config():
    return _config


@pytest.fixture(scope="session")
def wait_for_db(config):
    waited = 0

    while waited < 100:
        try:
            con = psycopg2.connect(config["DATABASE_URL"])
        except psycopg2.OperationalError:
            time.sleep(1)
            waited += 1
        else:
            con.close()
            break
    else:
        raise ConnectionRefusedError()


@pytest.fixture(scope="session")
def migrate_db(config, wait_for_db):
    cfg = alembic.config.Config("alembic.ini")
    cfg.set_main_option("sqlalchemy.url", config["DATABASE_URL"])

    alembic.command.upgrade(cfg, "head")
    yield
    alembic.command.downgrade(cfg, "base")


@pytest.fixture
def db(request, migrate_db):
    if request.node.get_closest_marker("real_db"):
        return request.getfixturevalue("_real_db")
    return request.getfixturevalue("_transaction_db")


@pytest.fixture
async def _real_db(config):
    cfg = alembic.config.Config("alembic.ini")
    cfg.set_main_option("sqlalchemy.url", config["DATABASE_URL"])
    _db = await DB.create(config["DATABASE_URL"], config["DATABASE_URL_RO"])

    yield _db

    await _db.close()

    alembic.command.downgrade(cfg, "base")
    alembic.command.upgrade(cfg, "head")


@pytest.fixture
async def _transaction_db(config):
    _db = await DB.create(config["DATABASE_URL"], use_single_connection=True)

    con = await _db.acquire()
    tr = con.transaction()
    await tr.start()

    yield _db

    await tr.rollback()
    await _db.release(con, force=True)

    await _db.close()


@pytest.fixture
async def con(db):
    async with db.acquire() as con:
        yield con


@pytest.fixture
async def api(config, db, aiohttp_client):
    app = Application(config)
    api = app.setup(db)
    return await aiohttp_client(api)


@pytest.fixture
def normalizer_dm(db):
    return Normalizer(db)


@pytest.fixture
def charger_dm(db):
    return Charger(db)


@pytest.fixture
def collector_dm(db):
    return Collector(db)


@pytest.fixture
def system_op_dm(db):
    return SystemOp(db)


@pytest.fixture
def factory(con, normalizer_dm, charger_dm, collector_dm):
    return Factory(con, normalizer_dm, charger_dm, collector_dm)


class Factory:
    def __init__(
        self,
        con: Connection,
        normalizer_dm: Normalizer,
        charger_dm: Charger,
        collector_dm: Collector,
    ):
        self._con = con

        self.normalizer = NormalizerFactory(self, normalizer_dm)
        self.charger = ChargerFactory(self, charger_dm)
        self.collector = CollectorFactory(self, collector_dm)

    def __getitem__(self, item: str):
        return getattr(self, item)

    async def fail(self, task_id: int):
        sql = "UPDATE tasks SET status = NULL WHERE id = $1"

        await self._con.execute(sql, task_id)

    async def cur_log_id(self, task_id: int) -> int:
        sql = "SELECT current_log_id FROM tasks WHERE id = $1"

        return await self._con.fetchval(sql, task_id)

    async def expire(self, task_id: int, delta: int):
        sql = (
            "UPDATE tasks_log SET created = $1 FROM tasks "
            "WHERE tasks.current_log_id = tasks_log.id AND tasks.id = $2"
        )
        created = datetime.now(tz=timezone.utc) - timedelta(seconds=delta)

        await self._con.execute(sql, created, task_id)


class NormalizerFactory:
    def __init__(self, factory: Factory, dm: Normalizer):
        self._root = factory
        self._dm = dm

    async def create(
        self,
        executor_id: str,
        timing_from: datetime,
        timing_to: datetime,
        status: NormalizerStatus = NormalizerStatus.accepted,
        failed: bool = False,
    ) -> int:
        task_id = await self._dm.create(executor_id, timing_from, timing_to)

        if status != NormalizerStatus.accepted:
            await self.update(executor_id, task_id, status)

        if failed:
            await self._root.fail(task_id)

        return task_id

    async def update(self, executor_id: str, task_id: int, status: NormalizerStatus):
        await self._dm.update(executor_id, task_id, status)

    async def details(self, task_id: int) -> dict:
        return await self._dm.retrieve_details(task_id)

    async def __call__(self, *args, **kwargs):
        return await self.create(*args, **kwargs)


class ChargerFactory:
    def __init__(self, factory: Factory, dm: Charger):
        self._root = factory
        self._dm = dm

    async def create(
        self,
        executor_id: str,
        timing_from: datetime,
        timing_to: datetime,
        status: ChargerStatus = ChargerStatus.accepted,
        failed: bool = False,
        state=None,
    ) -> int:
        task_id = await self._root.normalizer(executor_id, timing_from, timing_to)

        await self.update(executor_id, task_id, status, state)

        if failed:
            await self._root.fail(task_id)

        return task_id

    async def update(
        self, executor_id: str, task_id: int, status: ChargerStatus, state=None
    ):
        await self._dm.update(executor_id, task_id, status, state)

    async def details(self, task_id: int) -> dict:
        return await self._dm.retrieve_details(task_id)

    async def __call__(self, *args, **kwargs):
        return await self.create(*args, **kwargs)


class CollectorFactory:
    def __init__(self, factory: Factory, dm: Collector):
        self._root = factory
        self._dm = dm

    async def create(
        self,
        executor_id: str,
        timing_from: datetime,
        timing_to: datetime,
        status: CollectorStatus = CollectorStatus.accepted,
        failed: bool = False,
    ) -> int:
        task_id = await self._root.charger(executor_id, timing_from, timing_to)

        await self.update(executor_id, task_id, status)

        if failed:
            await self._root.fail(task_id)

        return task_id

    async def update(self, executor_id: str, task_id: int, status: CollectorStatus):
        await self._dm.update(executor_id, task_id, status)

    async def details(self, task_id: int) -> dict:
        return await self._dm.retrieve_details(task_id)

    async def __call__(self, *args, **kwargs):
        return await self.create(*args, **kwargs)
