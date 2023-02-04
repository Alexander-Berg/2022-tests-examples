import os
from contextlib import contextmanager
from typing import Union, Type

import django
import luigi
import pytest
from django.conf import settings
from freezegun import freeze_time

from dwh.core.luigi_compat import ContextCapturer
from dwh.core.models import User, Work
from dwh.core.resources import ResourceMap
from dwh.core.schemas.work import ContextSchema
from dwh.core.toolbox.tasks import get_registered_task

try:
    import library.python  # noqa: F401
    import pkgutil  # noqa: F401
    django.setup()  # Аркадийный pytest-django на этом моменте ещё не сконфигурировал Django.

    ARCADIA_RUN = True

except ImportError:
    ARCADIA_RUN = False

try:
    from envbox import get_environment
    environ = get_environment()

except ImportError:
    environ = os.environ


@pytest.fixture(autouse=True)
def db_access_on(django_db_reset_sequences,):
    """Используем бд в тестах без поимённого маркирования."""


@pytest.fixture
def init_user():
    """Создаёт объект пользователя."""

    def init_user_(username: str = None, *, robot: bool = False, support: bool = False, **kwargs) -> User:

        username = username or settings.TEST_USERNAME

        if robot:
            username = settings.ROBOT_NAME

        user = User(
            username=username,
            **kwargs
        )
        pwd = 'testpassword'
        user.set_password(pwd)
        user.testpassword = pwd

        if support:
            user.roles = {'support': {}}

        user.save()

        return user

    return init_user_


@pytest.fixture
def init_work():
    """Создаёт объект работы."""

    def init_work_(data: dict = None):

        if data is None:
            data = {'meta': {'task_name': 'echo'}, 'params': {'a': 2}}

        work = Work.initialize(
            remote='tests',
            request_id='143456',
            input_data=data,
        )
        return work

    return init_work_


@pytest.fixture
def run_task():
    """Позволяет запустить экземпляр задания напрямую (без машинерии веб-сервиса).
    Удобно для локальной отладки заданий.

    Перед запуском тестов, использующих данную фикстуру,
    полезно выставить переменные окружения, например::

        LD_LIBRARY_PATH=/home/idlesign/arc/arcadia/billing/dwh/src/dwh/instantclient
        YENV_TYPE=testing

        DWH_ROOT=/home/idlesign/arc/arcadia/billing/dwh/src/dwh/conf/remote/usr/bin/dwh
        DWH_SHELVE_PATH=/home/idlesign/arc/arcadia/billing/dwh/src/dwh/conf/state/pub
        DWH_200_ROOT=/home/idlesign/arc/arcadia/billing/dwh/src/dwh/conf/remote/usr/bin/dwh/dwh-200/

    Пример::

        from dwh.grocery.dwh_200 import DWH200, ReportPhase

        result = run_task(DWH200(
            phase=ReportPhase.ForecastPhase,
            no_excel=True,
            calc_yan=False,
            mnclose='no-mnclose',
        ))

        print(result)

        # или, если требуется приведение параметров
        result = run_task(DWH200, dict(
            phase=0,
            no_excel='true',
            calc_yan='false',
            mnclose='no-mnclose',
        ))

    """
    def run_task_(task: Union[luigi.Task, Type[luigi.Task]], params: dict = None):

        if params:
            task = task(**Work.parse_task_params(task_cls=task, params=params))

        result = luigi.build(
            [task],
            detailed_summary=True,
            local_scheduler=True,
            logging_conf_file='',
        )

        return result

    return run_task_


@pytest.fixture
def check_work_stop(monkeypatch):
    """Производит проверку остановки указанной работы

    :param monkeypatch:

    """
    def check_work_stop_(work: Work):
        harakiri = None

        def set_harakiri(timer):
            nonlocal harakiri
            harakiri = timer

        # эмуляция выполнения
        work._in_progress = True
        work._context = ContextSchema()
        work._context_capture = ContextCapturer()
        work._resource_map = ResourceMap()

        monkeypatch.setattr('uwsgiconf.uwsgi.set_user_harakiri', set_harakiri)

        work.stop()
        assert harakiri == 1

    return check_work_stop_


@pytest.fixture
def run_bg_task():
    """Запускает фоновый процесс по его имени."""

    def run_bg_task_(name: str):
        # Пробуем запустить зарегистированное фоновое задание.
        return get_registered_task(name).func()

    return run_bg_task_


@pytest.fixture
def mock_solomon(response_mock):
    """Обращения к соломону при использовании этой фикстуры
    будут проходить успешно.

    """
    with response_mock(
        'POST https://solomon-prestable.yandex.net/api/v2/push?project=dwh&cluster=default&service=push -> 200:ok'
    ):
        yield


@pytest.fixture
def time_freeze():
    """Менеджер контекста. Позволяет симитировать указанное время."""

    @contextmanager
    def time_freeze_(dt: str):
        with freeze_time(dt):
            yield

    return time_freeze_
