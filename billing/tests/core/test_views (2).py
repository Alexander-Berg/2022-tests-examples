import pytest


def test_index(client):
    response = client.get('')
    assert response.status_code == 200
    assert 'Инфратрек' in response.content.decode()


def test_check_error(client, init_user):

    response = client.get('/errcheck', follow=True)
    assert b'Not Found' in response.content

    # Авторизован.
    user = init_user()

    with pytest.raises(RuntimeError) as e:
        client.force_login(user)
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

    monkeypatch.setattr('ift.core.views.connection', DummyConnection())

    assert b'failure' in client.get('/ping').content

    ok = True
