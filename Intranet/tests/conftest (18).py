import importlib
import logging
from functools import partial
from datetime import timedelta, datetime

import pytest
from django.conf import settings
from django.db import connection

from tvmauth.mock import TvmClientPatcher, MockedTvmClient

from common import factories
from common.fixtures import *  # noqa: F401, F403
import plan.holidays.models as holidays_models
from plan.common.utils import timezone as utils
from plan.common.utils.sql import generate_resources_index
from plan.holidays.models import Holiday
from plan.staff.constants import LANG
from utils import Client


logging.getLogger("factory").setLevel(logging.CRITICAL)
logging.getLogger("django_replicated").setLevel(logging.CRITICAL)
logging.getLogger("plan.internal_roles.utils").setLevel(logging.CRITICAL)


@pytest.fixture()
def base_client(request):
    client_ = Client(settings=settings)
    client_.post = partial(client_.post, HTTP_ACCEPT_LANGUAGE=LANG.EN)
    client_.get = partial(client_.get, HTTP_ACCEPT_LANGUAGE=LANG.EN)
    client_.patch = partial(client_.patch, HTTP_ACCEPT_LANGUAGE=LANG.EN)
    client_.login(factories.StaffFactory().login)
    return client_


@pytest.fixture()
def client(base_client, staff_factory):
    base_client.login(staff_factory('full_access').login)
    return base_client


@pytest.fixture()
def only_view_client(base_client, staff_factory):
    base_client.login(staff_factory('own_only_viewer').login)
    return base_client


@pytest.fixture()
def client_without_accept_language(request):
    client_ = Client(settings=settings)
    staff = factories.StaffFactory()
    client_.login(staff.login)
    return client_


@pytest.fixture(autouse=True)
def postgresql_only(request):
    marker = request.node.get_closest_marker('postgresql')
    if marker and not connection.vendor.endswith('postgresql'):
        pytest.skip('Cannot run this test on postgresql')


@pytest.fixture(autouse=True)
def locked_context(monkeypatch):
    class Mocked(object):

        def __call__(self, *args, **kwargs):
            return self

        def __enter__(self, *args, **kwargs):
            return True

        def __exit__(self, *args, **kwargs):
            pass

    monkeypatch.setattr('plan.common.utils.locks.locked_context', Mocked())


@pytest.fixture(scope='session', autouse=True)
def db_postsetup(request, django_db_setup, django_db_blocker):
    django_db_blocker.unblock()
    request.addfinalizer(django_db_blocker.restore)

    resources_index_forward, resources_index_backward = generate_resources_index()

    resources_index_forward()

    def finalizer():
        resources_index_backward()

    request.addfinalizer(finalizer)


@pytest.fixture(scope='session', autouse=True)
def tvmauth_mock():
    with TvmClientPatcher(MockedTvmClient(self_tvm_id=int(settings.YAUTH_TVM2_CLIENT_ID))) as _fixture:
        yield _fixture


@pytest.fixture(autouse=True)
def daydata(monkeypatch):
    def patched_get_remote_holidays(dtstart, dtend):
        return {
            date: False
            for date in (
                dtstart + timedelta(days=n)
                for n in range((dtend - dtstart).days + 1)
            )
            if date.weekday() in (5, 6)
        }

    monkeypatch.setattr(holidays_models, 'get_remote_holidays', patched_get_remote_holidays)
    # с самой раней замоконной даты, до сегодня + радиус
    today = utils.today()
    Holiday.sync(date_from=datetime(year=2018, month=1, day=1).date(), date_to=today+settings.HOLIDAY_SYNC_RADIUS)
    yield


@pytest.fixture(autouse=True)
def patch_replication(monkeypatch):
    def process_request(*args, **kwargs):
        pass

    def process_response(self, request, response):
        return response

    monkeypatch.setattr('django_replicated.middleware.ReplicationMiddleware.process_request', process_request)
    monkeypatch.setattr('django_replicated.middleware.ReplicationMiddleware.process_response', process_response)
    yield


@pytest.fixture()
def crowdtest_environment(request, settings):
    """
    Переопределяет настройку CROWDTEST и перезагружает переданные в request модули

    Используется через pytest.mark.parametrize
    @pytest.mark.parametrize('crowdtest_environment', [['module.path']], indirect=True)
    или
    @pytest.mark.parametrize('other_param, crowdtest_environment', [
        ('other_param_value', ['moduleA.path', 'moduleB.path']),
    ], indirect=['crowdtest_environment'])
    """
    original_crowdtest = settings.CROWDTEST
    settings.CROWDTEST = True
    for module_path in request.param:
        module = importlib.import_module(module_path)
        importlib.reload(module)

    yield

    settings.CROWDTEST = original_crowdtest
    for module_path in request.param:
        module = importlib.import_module(module_path)
        importlib.reload(module)
