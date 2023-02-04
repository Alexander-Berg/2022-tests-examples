import os
import json
from typing import List
from unittest import mock
from contextlib import contextmanager

import pytest
import django.utils.timezone
from django.conf import settings
from django.test import override_settings
from django.contrib.auth.models import User, Group
from rest_framework.test import APIClient, APIRequestFactory

from django_yauth.user import YandexUser, AnonymousYandexUser
from django_yauth.authentication_mechanisms.tvm.request import TvmServiceRequest

from billing.dcsaap.backend.core import enum
from billing.dcsaap.backend.core.utils import tracker
from billing.dcsaap.backend.project.const import AUDIT_GROUP_NAME, SERVICEMAN_GROUP_NAME, APP_PREFIX
from billing.dcsaap.backend.core.models import CheckPrepareRun
from tvmauth.mock import TvmClientPatcher, MockedTvmClient

from billing.dcsaap.backend.tests.utils.models import create_check, create_run, create_diff
from billing.dcsaap.backend.tests.utils.tvm import TVMAPIClient, get_fake_ticket
from billing.dcsaap.backend.tests.utils import const


@pytest.fixture(autouse=True)
def enable_db_for_all_tests(db):
    """
    Специальная фикстура, которая позволяет не помечать каждый тест, использующий БД,
      с помощью `pytest.mark.django_db`, а активирует использование БД для каждого теста.

    Активация производится использованием этой фикстурой фикструы `db` в совокупности с `autouse=True`.
    """
    pass


@pytest.fixture
@contextmanager
def non_yolo():
    e = os.environ.pop("YOLO", None)
    try:
        yield
    finally:
        if e:
            os.environ['YOLO'] = e


@pytest.fixture(scope="session", autouse=True)
def tvm2_get_service_tickets():
    """
    Подменяем получение сервисных тикетов библиотекой `tvm2`
    """

    def get_service_tickets(self, *destinations, **kwargs):
        return {
            destination: get_fake_ticket(self.client_id, destination)
            for destination in destinations
            if str(destination) != str(const.INVALID_SERVICE_TICKET)
        }

    with mock.patch('tvm2.TVM2.get_service_tickets', new=get_service_tickets):
        yield


@pytest.fixture(scope='session', autouse=True)
def tvmauth_mock():
    with TvmClientPatcher(MockedTvmClient(self_tvm_id=int(settings.YAUTH_TVM2_CLIENT_ID))) as _fixture:
        yield _fixture


@pytest.fixture
def yt_client_mock():
    """
    Подменяет `yt.wrapper.YTClient` для возможности переопределения ответов методов по типу `read_table`/`write_table`.
    """
    with mock.patch('yt.wrapper.YtClient') as m:
        yield m


@pytest.fixture
def nirvana_api_mock():
    """
    Подменяет NirvanaApi
    """
    with mock.patch(f'{APP_PREFIX}.api.nirvana.NirvanaApi') as m:
        nirvana_api = m.return_value
        yield nirvana_api


@pytest.fixture(autouse=True)
def workflow_instance_lifecycle_mock(nirvana_api_mock):
    """
    Метод NirvanaApi.get_workflow_meta_data всегда возвращает словарь с {'lifecycleStyle': 'approved', ...}
    """
    return_value = {
        'lifecycleStatus': 'approved',
        'guid': 'xxx',
        'instanceId': 'yyy',
        'nsId': 123,
        'owner': 'zzz',
        'instanceCreator': 'user',
        'name': 'name',
        'cloneOfInstance': 'www',
        'created': '2022-07-15T15:11:36+0300',
        'updated': '2022-07-15T15:12:25+0300',
        'quotaProjectId': 'dcsaap',
        'rejected': False,
        'archived': False,
        'peerApprovalRequired': False,
        'tags': [],
    }
    nirvana_api_mock.get_workflow_meta_data.return_value = return_value


@pytest.fixture
def api_client():
    """
    Клиент для выполнения запросов к REST API проекта.
    Аналог `self.client` у класса `rest_framework.test.APITestCase`.
    """
    return APIClient()


@pytest.fixture
def api_rf():
    """
    "Фабрика запросов"
    `APIRequestFactory`: https://www.django-rest-framework.org/api-guide/testing/#apirequestfactory
    Аналог Django `Request Factory`:
      https://docs.djangoproject.com/en/2.2/topics/testing/advanced/#django.test.client.RequestFactory

    Позволяет сфабриковать `request` и использовать его в тестах,
      как будто мы обрабатываем запрос на одно из наших представлений (view).
    """
    return APIRequestFactory()


@pytest.fixture
def tvm_api_client():
    """
    Клиент для выполнения запросов к REST API проекта
      с использованием авторизации через сервисные TVM-тикеты.
    Аналог `self.client` у класса `rest_framework.test.APITestCase`
      с выполненым `self.credentials('HTTP_X_YA_SERVICE_TICKET': '3:serv:xxx:xxx')`
    """
    return TVMAPIClient()


@pytest.fixture
def ylock_mock():
    """
    Подменяет менеджер блокировок ylock
    Блокировки всегда проходят успешно
    """

    with mock.patch('ylock.create_manager') as m:
        yield m


@pytest.fixture
def freeze_now():
    """
    "Замораживает" значение `django.utils.timezone.now` на момент запуска теста.
    Используется для остановки автоматического обновления полей с `auto_now = True`.
    """
    value = django.utils.timezone.now()
    with mock.patch('django.utils.timezone.now', return_value=value) as m:
        yield m


@pytest.fixture
def yt_client_read_table_mock(yt_client_mock):
    """
    Фикстура, которая подменяет список возвращаемых значений методом `YtClient.read_table`.
    В данный момент предполагается что read_table вызывается с параметром `raw=False`.
    Результат выполнения фикстуры - список с двумя дополнительными методами:
      - `mock`: `property`, содержит мок функции `YtClient.read_table`
      - `set(List)`: метод, устанавливает данные возвращаемые подменяемым методом

    Пример использования:
    ```
    def test_function(yt_client_read_table_mock):
        yt_client_read_table_mock.set([1, 2, 3])
        yt_client_read_table_mock.append(4)

        result = yt.YtClient().read_table('/some/table')
        assert result == [1, 2, 3, 4]
    ```
    """

    class ReadTableData(list):
        @property
        def mock(self):
            return yt_client_mock.read_table

        def set(self, data: List):
            self[:] = data

    rv = yt_client_mock.read_table.return_value = ReadTableData()
    return rv


@pytest.fixture
def some_check(db):
    """
    Некоторая сверка "по-умолчанию"
    """
    return create_check('check', enum.HAHN, '/t1', '/t2', 'k1 k2', 'v1 v2', '/res')


@pytest.fixture
def some_run(db, some_check):
    """
    Некоторый запуск "по-умолчанию"
    """
    return create_run(some_check)


@pytest.fixture
def some_diffs(db, some_run, yt_client_read_table_mock):
    """
    Некоторые расхождения "по-умолчанию"
    """
    return [
        create_diff(some_run, 'k1', 'k1_value', 'column', '1', '2'),
        create_diff(some_run, 'k1', 'k1_value', 'column', '3', '4'),
    ]


@pytest.fixture
def some_prepare(db, some_check) -> CheckPrepareRun:
    """
    Некоторый запуски подготовки данных для некоторой сверки
    """
    return CheckPrepareRun.objects.create(check_model=some_check)


@pytest.fixture
def tracker_mock(requests_mock):
    """
    Включает использование трекера и подменяет его ответы.

    В данный умеет работать только с созданием тикета.
    В данный момент ничего не возвращает.
    """
    tracker_url = f'{settings.TRACKER_API_HOST}/{tracker.VERSION}'

    def format_issue(request, context):
        data = json.loads(request.body)
        queue = data["queue"]

        summary = data['summary']
        assert summary, 'Summary should be filled'

        return {
            'self': f'{tracker_url}/issues/f{queue}',
            'key': f'{queue}-1',
            'summary': summary,
            'description': data['description'],
        }

    # Перехватываем создание тикета
    requests_mock.register_uri('POST', f'{tracker_url}/issues/', status_code=201, json=format_issue)
    # Трекер первом обращении к коллекции скачивает информацию о всех существующих полях
    # Если ему не ответить - уходит в бесконечную рекурсию и умирает
    requests_mock.register_uri('GET', f'{tracker_url}/fields/', status_code=200, json=[])

    with override_settings(TRACKER_ENABLED=True):
        yield


@pytest.fixture
def super_yauser():
    """
    Создаёт суперпользователя и возвращает django-yauth.user.YandexUser для него
    """
    login = 'anakin-skywalker'
    User.objects.create_superuser(login, 'xx@yy.com', None)
    return YandexUser(uid=2, fields=dict(login=login))


@pytest.fixture
def anon_yauser():
    """
    Возвращает django-yauth.user.AnonymousYandexUser
    """
    return AnonymousYandexUser()


@pytest.fixture
def yauser():
    """
    Создаёт обычного пользователя без ролей и возвращает django-yauth.user.YandexUser для него
    """
    login = 'luke-skywalker'
    User.objects.create_user(username=login)
    return YandexUser(uid=1, fields=dict(login=login))


@pytest.fixture
def int_audit_yauser():
    """
    Создаёт пользователя c ролью Внут.Аудит и возвращает django-yauth.user.YandexUser для него
    """
    login = 'emperor-palpatine'
    user = User.objects.create_user(username=login)
    user.groups.add(Group.objects.get(name=AUDIT_GROUP_NAME))
    return YandexUser(uid=3, fields=dict(login=login))


@pytest.fixture
def serviceman_yauser():
    """
    Создаёт пользователя c ролью Представитель сервиса и возвращает django-yauth.user.YandexUser для него
    """
    login = 'qui-gon'
    user = User.objects.create_user(username=login)
    user.groups.add(Group.objects.get(name=SERVICEMAN_GROUP_NAME))
    return YandexUser(uid=4, fields=dict(login=login))


@pytest.fixture
def tvm_service() -> TvmServiceRequest:
    """
    Создаёт TvmServiceRequest (когда к нам приходят с TVM Service тикетом)
    """
    return TvmServiceRequest(
        service_ticket="Fake",
        uid=None,
        mechanism="tvm",
        raw_service_ticket="Fake",
        user_ip="127.0.0.1",
    )
