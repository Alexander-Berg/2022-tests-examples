import pytest
import sys

from unittest.mock import Mock, MagicMock
from uuid import uuid4

from constance import config
from celery.result import AsyncResult
from django.conf import settings
from django.contrib.auth import get_user_model
from waffle.models import Switch  # noqa

from tests.utils.clients import APIClient


User = get_user_model()


@pytest.fixture(autouse=True)
def no_requests(monkeypatch):
    """
    Везде нужно отдельно мокать походы в разные api.
    Фикстура нужна, чтобы гарантировано упасть при попытке сходить куда-либо.
    """
    monkeypatch.delattr('requests.sessions.Session.request')


@pytest.fixture(autouse=True)
def no_celery_tasks(monkeypatch):
    monkeypatch.setattr('celery.Celery.send_task', lambda *x, **y: AsyncResult(uuid4().hex))


@pytest.fixture(autouse=True)
def no_get_user_tickets(monkeypatch):
    monkeypatch.setattr(
        'ok.utils.context.get_user_ticket',
        lambda *args, **kwargs: 'user_ticket',
    )


@pytest.fixture(autouse=True)
def no_get_service_tickets(monkeypatch):
    monkeypatch.setattr(
        'ok.utils.tvm.get_service_ticket',
        lambda *args, **kwargs: 'service_ticket',
    )
    monkeypatch.setattr(
        'ok.utils.tvm.get_service_tickets',
        lambda *args, **kwargs: {'key': 'service_ticket'},
    )


@pytest.fixture(autouse=True)
def waffle(db):
    """
    Здесь создаются дефолтные waffle-свитчи и флаги
    """
    Switch.objects.create(name='enable_groups_multiplesuggest_for_approvements', active=True)
    Switch.objects.create(name='enable_issue_access_checking', active=True)


@pytest.fixture(autouse=True)
def constance(db):
    """
    Триггерим запись дефолтных значений в конфиг
    """
    config.APPROVEMENT_AUTHORS_WHITELIST_BY_TRACKER_QUEUE


@pytest.fixture(autouse=True)
def yauth_test_user(request, db):
    """
    Создаём дефолтного пользователя, от имени которого делаем запросы
    """
    if 'without_yauth_test_user' not in request.keywords:
        User.objects.create(username=settings.YAUTH_TEST_USER['login'])


@pytest.fixture(autouse=True)
def get_issues_mock(monkeypatch):
    monkeypatch.setattr(
        'ok.tracker.base.client.issues.get',
        lambda *args, **kwargs: Mock()
    )
    monkeypatch.setattr(
        'ok.tracker.base.get_tracker_client',
        lambda *args, **kwargs: MagicMock(),
    )


@pytest.fixture(autouse=True)
def wiki_format_markup_mock(monkeypatch):
    monkeypatch.setattr(
        'ok.utils.wiki.format_markup',
        lambda x, *args: x
    )


@pytest.fixture
def client():
    return APIClient()


# Полностью мокаем весь YT
sys.modules['yt'] = Mock()
