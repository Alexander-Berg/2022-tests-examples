import pytest

from django.conf import settings
from django.core.urlresolvers import reverse
from django.http.response import HttpResponse
from sys import platform


MAC_OS = platform == 'darwin'


@pytest.mark.skipif(condition=not MAC_OS,
                    reason='TVM not working with Docker for Mac due to IPv6')
@pytest.mark.usefixtures('enable_tvm_middleware')
def test_ping(client):
    """
    Тест вьюхи с пингом
    """
    response = client.get('/ping/')
    assert response.status_code == 200
    assert response.content.decode() == 'pong'


@pytest.mark.django_db
def test_auth_ping(jclient):
    """
    Тест пинга с авторизацией, проверка самого `jclient`
    """
    auth_ping_url = reverse('auth_ping')

    # делаем запрос без авторизации
    response = jclient.get(auth_ping_url)
    assert response.status_code == 401

    # авторизовываемся и делаем запрос
    jclient.login()
    response = jclient.get(auth_ping_url)
    assert response.status_code == 200
    assert response.content.decode() == 'pong'

    # разлогиниваемся
    jclient.logout()
    response = jclient.get(auth_ping_url)
    assert response.status_code == 401
