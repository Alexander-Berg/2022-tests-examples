import pytest


def test_index(client, init_user):
    response = client.get('/', follow=True)
    assert b'Not Found' in response.content

    # Авторизован.
    init_user()
    response = client.get('/', follow=True)
    assert response.status_code == 200


def test_check_error(client, init_user):

    response = client.get('/errcheck', follow=True)
    assert b'Not Found' in response.content

    # Авторизован.
    init_user()

    with pytest.raises(RuntimeError) as e:
        client.get('/errcheck', follow=True)

    assert str(e.value) == 'errboo test'


def test_ping(monkeypatch, client):

    ok = False

    class DummyConnection:

        def connect(self):
            return

        def is_usable(self):
            if not ok:
                raise RuntimeError('wrong')
            return ok

    monkeypatch.setattr('mdh.core.views.connection', DummyConnection())

    assert b'failure' in client.get('/ping').content

    ok = True
    assert b'success' in client.get('/ping').content
