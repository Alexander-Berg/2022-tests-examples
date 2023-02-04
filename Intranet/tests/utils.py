import datetime
import importlib
import pytest
import pytz

from collections import namedtuple
from contextlib import contextmanager, ExitStack
from functools import wraps
from typing import Dict, Container
from unittest.mock import Mock, patch

from django.core.cache import cache
from django.db import transaction
from django.db.models.signals import post_save, post_delete, m2m_changed
from django.test import override_settings
from django.utils import timezone
from waffle.models import Switch

from intranet.femida.src.actionlog import handlers as actionlog_handlers
from intranet.femida.src.calendar.api import Event
from intranet.femida.src.core.signals import post_update, post_bulk_create
from intranet.femida.src.utils.datetime import shifted_now


LOC_MEM_CACHE = {
    'default': {
        'BACKEND': 'django.core.cache.backends.locmem.LocMemCache',
    },
}


class AnyFrom:
    """
    Мок для тестов, чтобы сравнивать какое-либо значение с любым значением из возможных.
    Примеры:
    >>> AnyFrom(values=[1, 2]) == 2  # True
    >>> AnyFrom(values=[1, 2]) == 3  # False
    >>> AnyFrom(exclude=[1, 2]) == 2 # False
    >>> AnyFrom(exclude=[1, 2]) == 3 # True
    """
    def __init__(self, *, values=None, exclude=None):
        assert bool(values) != bool(exclude), 'use either `values` or `exclude`'
        self.values = values or []
        self.exclude = exclude or []

    def __eq__(self, other):
        if self.values:
            return other in self.values
        else:
            return other not in self.exclude

    def __repr__(self):
        return f'AnyFrom(values={self.values}, exclude={self.exclude})'


class Contains:
    """
    Мок для тестов, чтобы сравнивать, что заданное значение есть в проверяемом.
    Примеры:
    >>> Contains('тест') == 'Это валидный тест'  # True
    >>> Contains('тест') == 'Просто какой-то текст'  # False
    """
    def __init__(self, value):
        self.value = value

    def __eq__(self, other):
        return self.value in other

    def __repr__(self):
        return f'Contains(value={self.value})'


class ContainsDict:
    """
    Мок для тестов, чтобы сравнивать, что заданный словарь входит в проверяемый.
    Примеры:
    >>> ContainsDict({1: 2}) == {1: 2, 3: 4}  # True
    >>> ContainsDict({1: 2}) == {3: 4}  # False
    """
    def __init__(self, value):
        assert isinstance(value, dict)
        self.value = value

    def __eq__(self, other):
        if not isinstance(other, dict):
            return False
        return all(key in other and other[key] == self.value[key] for key in self.value)

    def __repr__(self):
        return f'ContainsDict(value={self.value})'


class AnyOrderList:
    """
    Мок для тестов, чтобы сравнивать два списка на равенство.
    Примеры:
    >>> AnyOrderList([1, 2, 3]) == [2, 1, 3]  # True
    >>> AnyOrderList([1, 2, 3]) == [1, 2]  # False
    >>> AnyOrderList([1, 1, 2]) == [1, 2, 2]  # False
    """

    def __init__(self, value):
        assert isinstance(value, list)
        self.value = value

    def __eq__(self, other):
        if not isinstance(other, list):
            return False
        return sorted(self.value) == sorted(other)

    def __repr__(self):
        return f'AnyOrderList(value={self.value})'


def get_mocked_event(event_id=1, *args, **kwargs):
    start_time = timezone.now() + datetime.timedelta(days=30)
    return Event(
        id=event_id,
        name='Event',
        start_time=start_time,
        description='some text',
        end_time=start_time + datetime.timedelta(hours=1),
        rooms=[
            {
                'name': 'Room1',
            },
        ],
        attendees=[],
    )


def get_forms_constructor_data(n, candidate_data=None):
    data = {
        'params': {
            'form_id': n,
            'cand_name': 'Name{}'.format(n),
            'cand_surname': 'Surname{}'.format(n),
            'cand_phone': '+712345678{}'.format(n),
            'cand_cv': 'https://study.yandex-team.ru/files?path=file.png',
            'cand_info': 'something{}'.format(n),
            'form_url': 'https://forms.yandex-team.ru/surveys/{}/?iframe=1'.format(n),
            'publication_url': 'https://yandex.ru/jobs/vacancies/something/something/',
            'cand_email': 'email{}@email.ru'.format(n),
            'cand_questions': 'Question1:\nAnswer1\n\nQuestion2:\nAnswer2',
        },
        'jsonrpc': '2.0',
        'method': 'POST',
        'id': n,
    }
    if candidate_data is not None:
        data['params'].update(candidate_data)
    return data


def fake_create_aarev_issue(*args, **kwargs):
    FakeStartrekIssue = namedtuple('FakeStartrekIssue', ['key', 'assignee'])
    assignee = Mock()
    assignee.login = 'username'
    return FakeStartrekIssue(key='AAREV-1', assignee=assignee)


@contextmanager
def assert_not_raises(exception=None):
    exception = exception or Exception
    try:
        yield
    except exception as exc:
        pytest.fail('Unexpected exception: %s' % repr(exc))


def enable_job_issue_workflow(func):
    """
    Для всех тестов по умолчанию свитч `ignore_job_issue_workflow` включен.
    В таком случае, игнорируются любые переходы
    в JOB-тикетах через воркфлоу вакансий и офферов.
    Этот декоратор отключает свитч на время выполнения декорируемой функции
    """
    @wraps(func)
    def wrapper(*args, **kwargs):
        switches = Switch.objects.filter(name='ignore_job_issue_workflow')
        switches.update(active=False)
        try:
            result = func(*args, **kwargs)
        finally:
            switches.update(active=True)
        return result

    return wrapper


def freeze():
    return datetime.datetime(2020, 1, 1, tzinfo=pytz.utc)


def use_cache(func):
    @override_settings(CACHES=LOC_MEM_CACHE)
    @wraps(func)
    def inner(*args, **kwargs):
        cache.clear()
        try:
            result = func(*args, **kwargs)
        finally:
            cache.clear()
        return result
    return inner


def connect_actionlog_signals():
    post_save.connect(actionlog_handlers.actionlog_callback)
    post_delete.connect(actionlog_handlers.actionlog_callback)
    post_update.connect(actionlog_handlers.actionlog_update_callback)
    post_bulk_create.connect(actionlog_handlers.actionlog_bulk_create_callback)
    m2m_changed.connect(actionlog_handlers.actionlog_m2m_changed_callback)


def disconnect_actionlog_signals():
    post_save.disconnect(actionlog_handlers.actionlog_callback)
    post_delete.disconnect(actionlog_handlers.actionlog_callback)
    post_update.disconnect(actionlog_handlers.actionlog_update_callback)
    post_bulk_create.disconnect(actionlog_handlers.actionlog_bulk_create_callback)
    m2m_changed.disconnect(actionlog_handlers.actionlog_m2m_changed_callback)


def enable_actionlog(func):
    @wraps(func)
    def inner(*args, **kwargs):
        connect_actionlog_signals()
        try:
            result = func(*args, **kwargs)
        finally:
            disconnect_actionlog_signals()
        return result
    return inner


@contextmanager
def ctx_combine(*managers):
    with ExitStack() as stack:
        yield [stack.enter_context(m) for m in managers]


def eager_task(name):
    module, task = name.rsplit('.', 1)
    module = importlib.import_module(module)
    task = getattr(module, task)
    return patch(f'{name}.delay', wraps=task)


@contextmanager
def run_commit_hooks():
    """
    Используется для запуска функций, переданных в transaction.on_commit,
    которые не запускаются в нетранзакционных тестах,
    потому что каждый запуск такого теста оборачивается в транзакцию
    """
    yield
    connection = transaction.get_connection()
    with patch.object(connection, 'validate_no_atomic_block', return_value=False):
        connection.run_and_clear_commit_hooks()


def patch_service_permissions(permissions: Dict[int, Container[str]]):
    def has_service_permission(yauser, perm):
        tvm_id = int(yauser.service_ticket.src)
        return perm in permissions.get(tvm_id, [])
    return patch(
        target='intranet.femida.src.api.core.permissions.has_service_permission',
        new=has_service_permission,
    )


def dt_to_str(dt):
    """
    Конвертирует объект datetime в строку формата, который sform ожидает получить от фронта
    """
    return dt.strftime('%Y-%m-%d %H:%M:%S')


def shifted_now_str(**kwargs):
    dt = shifted_now(**kwargs)
    return dt_to_str(dt)


def assert_fields_existence(data, fields, exists):
    for field in fields:
        if exists:
            assert field in data
        else:
            assert field not in data
