import asyncio
import logging
import pytest
from contextlib import contextmanager

from mock import patch, mock
from sqlalchemy import create_engine, event
from sqlalchemy.engine import Engine
from sqlalchemy.orm import scoped_session
from fastapi.testclient import TestClient as SyncTestClient
from helpers import TestClient
from watcher.config import settings

logging.getLogger("factory").setLevel(logging.CRITICAL)

pytest_plugins = [
    'fixtures.factories.abc_migration',
    'fixtures.factories.composition',
    'fixtures.factories.member',
    'fixtures.factories.from_transfer',
    'fixtures.factories.staff',
    'fixtures.factories.service',
    'fixtures.factories.interval',
    'fixtures.factories.slot',
    'fixtures.factories.shift',
    'fixtures.factories.schedule',
    'fixtures.factories.base',
    'fixtures.factories.role',
    'fixtures.factories.scope',
    'fixtures.factories.rating',
    'fixtures.factories.gap',
    'fixtures.schedule_data',
    'fixtures.shift_data',
    'fixtures.factories.problem',
    'fixtures.factories.event',
    'fixtures.factories.rating',
    'fixtures.factories.notification',
    'fixtures.factories.bot_user',
    'fixtures.factories.gap_settings',
    'fixtures.factories.unistat',
]


@pytest.fixture
def config(monkeypatch):
    monkeypatch.setenv('TVM2_ASYNC', 'true')
    monkeypatch.setenv('QLOUD_TVM_TOKEN', 'test_token')
    monkeypatch.setenv('TVM2_USE_QLOUD', 'true')
    from watcher.config import settings
    return settings


@pytest.fixture
def app(monkeypatch):
    monkeypatch.setenv('TVM2_ASYNC', 'true')
    monkeypatch.setenv('QLOUD_TVM_TOKEN', 'test_token')
    monkeypatch.setenv('TVM2_USE_QLOUD', 'true')
    from watcher.app import get_app
    return get_app()


@pytest.fixture(scope='session')
def session_scope_session(db_session):
    return scoped_session(db_session.cached_sessionmaker)


@pytest.fixture
def scope_session(db_session):
    return scoped_session(db_session.cached_sessionmaker)


@pytest.fixture
def sqlalchemy_session(config, scope_session):
    engine = create_engine(config.database_url)
    scope_session.configure(bind=engine)
    session = scope_session()
    yield session
    session.rollback()
    scope_session.remove()


@pytest.fixture(scope='function')
def db_session():
    from watcher.db.base import _get_fastapi_sessionmaker
    return _get_fastapi_sessionmaker()


@pytest.fixture(scope='function', autouse=True)
def create_test_database(db_session):
    from watcher.db import BaseModel

    engine = db_session.cached_engine
    BaseModel.metadata.create_all(engine)
    yield
    db_session.cached_sessionmaker.close_all()
    BaseModel.metadata.drop_all(engine)


@pytest.fixture(scope='module')
def event_loop():
    yield asyncio.get_event_loop()


@pytest.fixture(scope='function', autouse=True)
def test_request_user(staff_factory, permission_factory):
    # uid из config.test_user_data['uid']
    settings.test_user_data['uid'] = 123
    staff = staff_factory(uid=123, login='superuser_staff')
    permission_factory(
        user_id=staff.user_id,
        permission_id=settings.FULL_ACCESS_ID,
    )
    permission_factory(
        user_id=staff.user_id,
        permission_id=settings.SERVICES_VIEWER_ID,
    )
    return staff


@pytest.fixture
async def async_client(app):
    async with TestClient(app=app) as client:
        yield client


@pytest.fixture
def client(app):
    with SyncTestClient(app=app) as client:
        yield client


@pytest.fixture
def watcher_robot(staff_factory):
    return staff_factory(login=settings.ROBOT_LOGIN)


@pytest.fixture
def assert_json_keys_value_equal():
    def _check_json_keys_value(data, expected):
        for key, value in expected.items():
            assert data[key] == value

    return _check_json_keys_value


@pytest.fixture
def assert_equal_stable_list(assert_json_keys_value_equal):
    # Сравнивает контент вне зависимости от перестановок элементов

    def _assert_equal_stable_list(data, expected, sort_key='id'):
        for obj, obj_exp in zip(
            sorted(data, key=lambda x: x[sort_key]),
            sorted(expected, key=lambda x: x[sort_key])
        ):
            assert_json_keys_value_equal(obj, obj_exp)

    return _assert_equal_stable_list


@pytest.fixture
def start_people_allocation_mock() -> mock.Mock:
    with mock.patch('watcher.tasks.people_allocation.start_people_allocation.delay') as _mock:
        yield _mock


@pytest.fixture(scope="session", autouse=True)
def default_session_fixture(request):
    patchers = [
        ('watcher.logic.clients.base.get_tvm2_ticket', {'return_value': 'some_ticket'}),
        ('watcher.logic.permissions.is_superuser', {'new': lambda staff: staff.login == 'superuser_staff'}),
    ]

    patched_list = []
    for patcher, patch_data in patchers:
        patched = patch(
            patcher,
            **patch_data
        )
        patched.start()
        patched_list.append(patched)

    def unpatch():
        for patched_el in patched_list:
            patched_el.stop()

    request.addfinalizer(unpatch)


@pytest.fixture
def set_testing():
    initial = settings.ENV_TYPE
    settings.ENV_TYPE = settings.ENV_TYPE_TESTING
    yield
    settings.ENV_TYPE = initial


@pytest.fixture
def set_production():
    initial = settings.ENV_TYPE
    settings.ENV_TYPE = settings.ENV_TYPE_PRODUCTION
    yield
    settings.ENV_TYPE = initial


@pytest.fixture
def set_production_for_unit_tests():
    initial = settings.ENV_TYPE
    settings.ENV_TYPE = 'production_for_unit_tests'
    yield
    settings.ENV_TYPE = initial


@pytest.fixture
def assert_count_queries():
    @contextmanager
    def _check_count_queries(number):
        queries = []

        @event.listens_for(Engine, "before_cursor_execute")
        def before_cursor_execute(conn, cursor, statement, parameters, context, executemany):
            if isinstance(parameters, dict):
                statement = statement % parameters
            queries.append(statement)
        try:
            yield
        finally:
            event.remove(Engine, "before_cursor_execute", before_cursor_execute)
            assert len(queries) == number, 'SQL executions for a query:\n{}'.format('\n\n'.join(queries))

    return _check_count_queries


@pytest.fixture(scope='function')
def user_with_permissions(staff_factory, permission_factory):
    settings.test_user_data['uid'] = 456
    staff = staff_factory(uid=456, login='test_user_full_access')
    permission_factory(
        user_id=staff.user_id,
        permission_id=settings.FULL_ACCESS_ID,
    )
    permission_factory(
        user_id=staff.user_id,
        permission_id=settings.SERVICES_VIEWER_ID,
    )
    return staff
