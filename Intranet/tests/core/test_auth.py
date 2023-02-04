import pytest
from django.http import QueryDict
from django.utils import timezone
from django_yauth.exceptions import TwoCookiesRequired

from intranet.crt.core.auth import AuthenticationWhitelistedMiddleware
from intranet.crt.core.models import AliveCheckpoint

pytestmark = pytest.mark.django_db


class FakeRequest(object):
    def __init__(self, path):
        self.method = 'GET'
        self.path = path
        self.GET = QueryDict()
        self.COOKIES = {}
        self.META = {}

    @property
    def user(self):
        raise TwoCookiesRequired

    def get_host(self):
        return 'crt-api.yandex-team.ru'

    def is_secure(self):
        return True


def test_TwoCookiesRequired(monkeypatch):
    request = FakeRequest('/idm/')
    assert AuthenticationWhitelistedMiddleware().process_request(request).status_code == 401


def test_normal_redirect(monkeypatch):
    """Для некоторых путей при отсутствии кук идёт редирект на Паспорт"""
    request = FakeRequest('/ninja/')
    assert AuthenticationWhitelistedMiddleware().process_request(request).status_code == 302


def test_qloud_secret_header_middleware(crt_client, users, settings):
    # когда header передается
    user = users['normal_user']
    crt_client.login(user.username)
    response = crt_client.get('/api/certificate/')
    assert response.status_code == 200

    AliveCheckpoint.objects.create(modified_at=timezone.now())

    assert crt_client.get('/ping').status_code == 200

    # когда header не передается
    settings.CRT_BALANCER_SECRET_HEADER = "p@ssw0rd"
    response = crt_client.get('/api/certificate/')
    assert response.status_code == 401 and response.content == b'invalid HTTP_X_CRT_BALANCER_SECRET_HEADER'

    assert crt_client.get('/ping/').status_code == 200
