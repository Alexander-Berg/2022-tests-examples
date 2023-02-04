import json

import pytest

from sendr_aiopg.engine.lazy import PgPingerClient

from pay.sprint.sprint.conf import settings
from pay.sprint.sprint.tests.utils import dummy_async_context_manager, dummy_async_function
from pay.sprint.sprint.utils.db import create_configured_engine


@pytest.fixture
def mock_pg_pinger(mocker):
    db_host = '0.0.0.0'

    if settings.DB_USE_ARCADIA_RECIPE:
        with open('pg_recipe.json', 'r') as f:
            db_host = json.load(f)['host']

    async def dummy_hosts(self, preset):
        return {'hosts': [{'hostname': db_host}]}

    mocker.patch.object(PgPingerClient, 'hosts', dummy_hosts)


@pytest.fixture(autouse=True, scope='session')
def configure_db():
    if settings.DB_USE_ARCADIA_RECIPE:
        with open('pg_recipe.json', 'r') as f:
            pg_recipe = json.load(f)
            settings.DATABASE = {
                'NAME': pg_recipe['dbname'],
                'USER': pg_recipe['user'],
                'PASSWORD': '',
                'HOST': pg_recipe['host'],
                'PORT': pg_recipe['port'],
                'USE_SSL': None,
                'CONNECT_TIMEOUT': 2,
                'TIMEOUT': 6,
                'TARGET_SESSION_ATTRS': 'read-write',
            }

    yield


@pytest.fixture
def raw_db_engine(loop, mock_pg_pinger):
    engine = create_configured_engine()
    yield engine
    engine.close()
    loop.run_until_complete(engine.wait_closed())


@pytest.fixture
def mocked_db_engine(loop, monkeypatch, mocker, raw_db_engine):
    MOCK_TR = True
    conn = loop.run_until_complete(raw_db_engine.acquire().__aenter__())
    tr = loop.run_until_complete(conn.begin())

    def mock_acquire(preset=None):
        return dummy_async_context_manager(conn)

    def mock_tr():
        tr = mocker.MagicMock
        tr.close = tr.commit = tr.rollback = dummy_async_function()
        tr.is_active = True
        return dummy_async_context_manager(tr)

    if MOCK_TR:
        monkeypatch.setattr(raw_db_engine, 'acquire', mock_acquire)
        monkeypatch.setattr(conn, 'begin', mock_tr)

    yield raw_db_engine

    if not MOCK_TR:
        loop.run_until_complete(tr.commit())
    else:
        loop.run_until_complete(tr.rollback())
