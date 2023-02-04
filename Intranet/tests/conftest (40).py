import pytest
from mock import patch, Mock
from sqlalchemy import create_engine
from fastapi import Request

from intranet.trip.src.config import settings
from intranet.trip.src.db.tables import metadata
from intranet.trip.src.cache import Cache
from intranet.trip.src.unit_of_work import UnitOfWork
from .helpers import TestClient
from .factories import Factory
from .mocks import MockRedisLock


class RollbackTestQueries(Exception):
    pass


class MockedRedis:
    async def enqueue_job(self, *args, **kwargs):
        pass

    def close(self, *args, **kwargs):
        pass

    async def wait_closed(self, *args, **kwargs):
        pass

    async def set(self, *args, **kwargs):
        pass

    async def get(self, *args, **kwargs):
        pass

    async def expire(self, *args, **kwargs):
        pass


async def mocked_create_redis_pool():
    return MockedRedis()


async def override_get_unit_of_work(request: Request):
    if hasattr(request.app.state, '_global_uow') and request.app.state._global_uow:
        yield request.app.state._global_uow
    else:
        async with request.app.state.db.acquire() as conn:
            yield UnitOfWork(conn=conn, redis=request.app.state.redis)


class MockedUnitOfWork(UnitOfWork):
    """
    Нужен для проверки какие именно вызываются отложенные таски (хранятся в self._jobs)
    """
    async def _enqueue_jobs(self):
        pass


class MockedCache(Cache):
    CACHE = {}

    async def get(self, key):
        return self.CACHE.get(key)

    async def set(self, key, data, expire_after=None):
        self.CACHE[key] = data


@pytest.fixture
def app():
    from intranet.trip.src.main import app
    from intranet.trip.src.api.depends import _get_unit_of_work

    app.dependency_overrides[_get_unit_of_work] = override_get_unit_of_work
    return app


@pytest.fixture(scope='session', autouse=True)
def create_test_database():
    from intranet.trip.src.config import settings

    engine = create_engine(settings.database_url)
    metadata.create_all(engine)
    yield
    metadata.drop_all(engine)


@pytest.fixture(autouse=True)
async def mock_http_requests():
    async def mocked_make_request(*args, **kwargs):
        raise Exception('HTTP request in test. Need to patch it!')

    with patch('async_clients.clients.base.BaseClient._make_request', mocked_make_request):
        yield


@pytest.fixture
async def client(app):
    with patch('intranet.trip.src.main.create_redis_pool', mocked_create_redis_pool):
        async with TestClient(app=app) as client:
            yield client


@pytest.fixture
async def db_engine():
    from aiopg.sa import create_engine

    async with create_engine(**settings.db_settings) as engine:
        yield engine


@pytest.fixture
async def redis():
    redis = await mocked_create_redis_pool()
    yield redis


@pytest.fixture
async def cache():
    redis = await mocked_create_redis_pool()
    cache = MockedCache(redis=redis)
    yield cache


@pytest.fixture
async def uow(client):
    """
     - Если нам не нужен клиент (мы его не тестим):
    Клиент будет все равно создан, но нам типа пофиг?

     - Если клиент нам-таки нужен:
    то он будет создан и будет использовать тот же самый коннекшн
    НО он будет использовать другой unit_of_work
    """
    redis = await mocked_create_redis_pool()

    async with client.app.state.db.acquire() as conn:
        uow = MockedUnitOfWork(conn=conn, redis=redis)

        client.app.state._global_uow = uow

        try:
            async with uow:
                yield uow
                raise RollbackTestQueries
        except RollbackTestQueries:
            pass
        finally:
            client.app.state._global_uow = None


@pytest.fixture
async def f(uow):
    yield Factory(conn=uow._conn)


@pytest.fixture
async def mock_redis_lock():
    yield MockRedisLock(Mock(), '')
