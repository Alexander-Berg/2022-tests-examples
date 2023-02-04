import builtins
from io import StringIO

import pytest
from django.test import Client


def get(url: str, *, client: Client = None) -> str:
    client = client or Client()

    response = client.get(url, follow=True)

    return response.content.decode()


def test_version():
    assert '1.' in get('/version')


@pytest.mark.django_db
def test_check_error(admin_client):

    with pytest.raises(RuntimeError) as e:
        get('/errcheck', client=admin_client)

    assert str(e.value) == 'errboo test'


@pytest.mark.django_db
def test_runtask(admin_client, monkeypatch):
    monkeypatch.setattr(builtins, 'open', lambda *args, **kargs: StringIO())
    assert 'success' in get('/api/core/tasks/check_task_work/run/', client=admin_client)
    assert 'task not found' in get('/api/core/tasks/unknowntask/run/', client=admin_client)


def test_ping(monkeypatch):

    ok = False

    class DummyConnection:

        def connect(self):
            return

        def is_usable(self):
            return ok

    monkeypatch.setattr('refs.core.views.connection', DummyConnection())

    assert 'failure' in get('/ping')

    ok = True
    assert 'success' in get('/ping')
