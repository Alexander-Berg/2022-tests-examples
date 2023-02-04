import os
import subprocess
import pytest
import yatest
import asyncio
import vcr

from asynctest import patch
from gino.ext.starlette import Gino
from pyscopg2.asyncpg import PoolManager
from helpers import TestClient


async def _clear(self):
    self._balancer = None
    if self._refresh_role_tasks is not None:
        for refresh_role_task in self._refresh_role_tasks:
            refresh_role_task.cancel()
        self._refresh_role_tasks = None
    release_tasks = []
    for connection in self._unmanaged_connections:
        release_tasks.append(self.release(connection))

    await asyncio.gather(*release_tasks, return_exceptions=True)
    self._unmanaged_connections.clear()
    self._master_pool_set.clear()
    self._replica_pool_set.clear()


@pytest.fixture(scope="session", autouse=True)
def default_session_fixture(request):
    patchers = [
        'intranet.domenator.src.logic.tvm2.get_service_ticket',
        'intranet.domenator.src.logic.webmaster.get_service_ticket',
        'intranet.domenator.src.logic.gendarme.get_service_ticket',
        'intranet.domenator.src.logic.fouras.get_service_ticket',
        'intranet.domenator.src.logic.connect.get_service_ticket',
        'intranet.domenator.src.logic.blackbox.get_service_ticket',
        'intranet.domenator.src.logic.passport.get_service_ticket',
    ]

    patched_list = []
    for patcher in patchers:
        patched = patch(
            patcher,
            return_value='some_ticket'
        )
        patched.start()
        patched_list.append(patched)

    patched = patch.object(
        PoolManager, '_clear', _clear
    )
    patched.start()
    patched_list.append(patched)

    def unpatch():
        for patched_el in patched_list:
            patched_el.stop()

    request.addfinalizer(unpatch)


@pytest.fixture(scope='module')
def event_loop():
    yield asyncio.get_event_loop()


@pytest.fixture
async def db_bind():
    from intranet.domenator.src.db.engine import get_engine_config

    db = Gino()
    config = get_engine_config()
    return db.with_bind(bind=config['dsn'])


@pytest.fixture
def test_vcr():
    path = source_path('intranet/domenator/vcr_cassettes')
    return vcr.VCR(
        cassette_library_dir=path,
    )


def source_path(path):
    try:
        return yatest.common.source_path(path)
    except AttributeError:
        # only for local pycharm tests
        return os.path.join(os.environ["Y_PYTHON_SOURCE_ROOT"], path)


def binary_path(path):
    try:
        return yatest.common.binary_path(path)
    except AttributeError:
        # only for local pycharm tests
        return os.path.join(os.environ["Y_PYTHON_SOURCE_ROOT"], path)


# @pytest.fixture(scope="session", autouse=True)
def migrate_db(request):
    if os.environ.get('NO_MIGRATE_DB'):
        return

    path_alembic = 'intranet/domenator/migrations/domenator-db'
    command_path = binary_path(path_alembic)

    base_command = (
        '{command_path} -c migrations/alembic.ini upgrade head'
    )

    base_command = base_command.format(
        command_path=command_path,
    )
    if os.environ.get('PYCHARM_TEST_RUN'):
        subprocess.check_call(base_command, shell=True)
    else:
        yatest.common.execute(
            base_command,
            shell=True,
            check_exit_code=True
        )

    # os.environ['NO_MIGRATE_DB'] = '1'


def unmigrate_db(request):
    path_alembic = 'intranet/domenator/migrations/domenator-db'
    command_path = binary_path(path_alembic)

    base_command = (
        '{command_path} -c migrations/alembic.ini downgrade base'
    )

    base_command = base_command.format(
        command_path=command_path,
    )
    if os.environ.get('PYCHARM_TEST_RUN'):
        subprocess.check_call(base_command, shell=True)
    else:
        yatest.common.execute(
            base_command,
            shell=True,
            check_exit_code=True
        )


@pytest.fixture
def config(monkeypatch):
    monkeypatch.setenv('TVM2_ASYNC', 'true')
    monkeypatch.setenv('QLOUD_TVM_TOKEN', 'test_token')
    monkeypatch.setenv('TVM2_USE_QLOUD', 'true')
    from intranet.domenator.src.settings import config

    return config


@pytest.fixture
def app(monkeypatch):
    monkeypatch.setenv('TVM2_ASYNC', 'true')
    monkeypatch.setenv('QLOUD_TVM_TOKEN', 'test_token')
    monkeypatch.setenv('TVM2_USE_QLOUD', 'true')

    from intranet.domenator.src.app import get_app
    return get_app()


@pytest.fixture
async def client(app):
    # TODO: Эта схема миграций временная так как будет медленно работать
    # на большом количестве тестов, нужно поменять ее - оставить только migrate_DB
    # в начале сесии и стартовать транзакцию при начале теста и откатывать ее после теста

    migrate_db(None)
    async with TestClient(app=app) as client:
        yield client
    unmigrate_db(None)
