import os
from unittest import mock
import pytest
from mockredis import MockRedis

from intranet.search.settings import setup_environ
from intranet.search.abovemeta.requester import Requester

from .client import Client

os.environ['DJANGO_SETTINGS_MODULE'] = 'intranet.search.tests.helpers.settings'


def pytest_configure():
    setup_environ()


@pytest.fixture
def bisearch_app():
    os.environ['APP_NAME'] = 'bisearch'
    setup_environ()
    try:
        yield
    finally:
        os.environ['APP_NAME'] = 'isearch'
        setup_environ()


@pytest.fixture(scope="function")
def requester():
    from intranet.search.tests.helpers.abovemeta_helpers import RequesterFetchMock
    with mock.patch.object(Requester, 'fetch', new_callable=RequesterFetchMock) as req:
        yield req


@pytest.fixture
def app():
    from intranet.search.abovemeta.app import application
    return application


@pytest.fixture(scope='session', autouse=True)
def global_celery():
    from intranet.search.core.celery import global_app, signals, db
    global_app.conf.CELERY_ALWAYS_EAGER = True
    signals.task_prerun.disconnect(db.close_old_connections)
    return global_app


@pytest.fixture
def api_client():
    return Client()


@pytest.fixture
def csrf_api_client():
    return Client(enforce_csrf_checks=True)


@pytest.fixture(scope='function', autouse=True)
def mock_redis(monkeypatch):
    client = MockRedis()
    monkeypatch.setattr('intranet.search.core.redis.get_client', lambda: client)
    return client
