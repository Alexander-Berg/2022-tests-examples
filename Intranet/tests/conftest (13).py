import pytest
import sys

from unittest.mock import Mock
from uuid import uuid4

from celery.result import AsyncResult
from waffle.models import Switch

from tests.utils.clients import APIClient


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
def waffle(db):
    Switch.objects.create(name='enable_stage_status_current', active=True)


@pytest.fixture(autouse=True)
def get_issues_mock(monkeypatch):
    monkeypatch.setattr(
        'ok.tracker.base.client.issues.get',
        lambda *args, **kwargs: Mock()
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
