import pytest
from rest_framework import status

from django.conf import settings

from intranet.crt.constants import CERT_TYPE
from intranet.crt.core.models import Certificate
from __tests__.utils.common import create_certificate

pytestmark = pytest.mark.django_db


def assert_meta(response, total_count, actual_count, page):
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert set(data.keys()) == {'count', 'page', 'next', 'previous', 'results'}
    assert data['count'] == total_count
    assert len(data['results']) == actual_count
    assert data['page'] == page


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_pages(crt_client, users, certificate_types, path):
    user = users['normal_user']
    crt_client.login(user.username)

    certs_number = 25

    for i in range(certs_number):
        create_certificate(user, certificate_types[CERT_TYPE.ASSESSOR])

    response = crt_client.json.get(path, {'_fields': 'id'})
    assert_meta(response, certs_number, 10, 1)

    response = crt_client.json.get(path, {'_fields': 'id', 'page': 1})
    assert_meta(response, certs_number, 10, 1)

    response = crt_client.json.get(path, {'_fields': 'id', 'page': 2})
    assert_meta(response, certs_number, 10, 2)

    response = crt_client.json.get(path, {'_fields': 'id', 'page': 3})
    assert_meta(response, certs_number, 5, 3)


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_no_pages(crt_client, users, certificate_types, path):
    user = users['normal_user']
    crt_client.login(user.username)

    for _ in range(20):
        create_certificate(user, certificate_types[CERT_TYPE.ASSESSOR])

    response = crt_client.json.get(path, {'_fields': 'id', 'no_meta': True})
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert isinstance(data, list)
    assert len(data) == settings.REST_FRAMEWORK['PAGE_SIZE']


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_order_by(crt_client, users, certificate_types, path):
    user = users['normal_user']
    crt_client.login(user.username)
    for _ in range(20):
        create_certificate(user, certificate_types[CERT_TYPE.ASSESSOR])

    response = crt_client.json.get(path, {'_fields': 'id', 'order_by': 'id'})
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data['results'][0]['id'] == Certificate.objects.order_by('id').first().id

    response = crt_client.json.get(path, {'_fields': 'id', 'order_by': '-id'})
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data['results'][0]['id'] == Certificate.objects.order_by('-id').first().id
