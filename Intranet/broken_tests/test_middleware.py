import mock
import pytest

from django.test.utils import override_settings


pytestmark = pytest.mark.django_db
bots_url = '/internal/uhura/bots/'
registration_url = '/internal/uhura/registration/'
ping_url = '/ping/'
HEADERS = {
    'HTTP_HOST': 'localhost',
    'content_type': 'application/json'
}


def test_ping(client):
    with override_settings(MIDDLEWARE=['tasha.middleware.TVMMiddleware']):
        response = client.get(ping_url, **HEADERS)
        assert response.status_code == 200
        assert response.content == b'pong'


def test_no_header(client):
    with override_settings(MIDDLEWARE=['tasha.middleware.TVMMiddleware']):
        response = client.get(bots_url, **HEADERS)
        assert response.status_code == 403


def test_with_header_fail(client):
    with override_settings(MIDDLEWARE=['tasha.middleware.TVMMiddleware']):
        with mock.patch('tasha.middleware.TVMMiddleware.check_service_ticket') as patch:
            patch.return_value = False
            response = client.get(bots_url, HTTP_X_YA_SERVICE_TICKET='1', **HEADERS)
        assert response.status_code == 403


def test_with_header_success(client):
    with override_settings(MIDDLEWARE=['tasha.middleware.TVMMiddleware']):
        with mock.patch('tasha.middleware.TVMMiddleware.check_service_ticket') as patch:
            patch.return_value = True
            response = client.get(bots_url, HTTP_X_YA_SERVICE_TICKET='1', **HEADERS)
        assert response.status_code == 200
