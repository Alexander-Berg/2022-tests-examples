import pytest

from freezegun import freeze_time
from rest_framework import status

from django.utils import timezone

from intranet.crt.constants import CERT_TYPE
from intranet.crt.core.models import Certificate
from __tests__.utils.common import create_certificate

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_id(crt_client, certificate_types, users, path):
    user = users['normal_user']
    crt_client.login(user.username)
    certs_number = 10
    for i in range(certs_number):
        create_certificate(user, certificate_types[CERT_TYPE.ASSESSOR])
    cert = Certificate.objects.all()[5]

    # Проверим фильтрацию по id
    response = crt_client.json.get(path, {'_fields': 'id', 'order_by': 'id', 'id': cert.id})
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data['count'] == 1
    assert data['results'][0]['id'] == cert.id

    # Проверим фильтрацию по id__gt
    response = crt_client.json.get(path, {'_fields': 'id', 'order_by': 'id', 'id__gt': cert.id})
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    expected_data = set(Certificate.objects.filter(id__gt=cert.id).values_list('id', flat=True))
    assert data['count'] == len(expected_data)
    recieved_data = {certificate['id'] for certificate in data['results']}
    assert recieved_data == expected_data

    # Проверим фильтрацию по id__lt
    response = crt_client.json.get(path, {'_fields': 'id', 'order_by': 'id', 'id__lt': cert.id})
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    expected_data = set(Certificate.objects.filter(id__lt=cert.id).values_list('id', flat=True))
    assert data['count'] == len(expected_data)
    recieved_data = {certificate['id'] for certificate in data['results']}
    assert recieved_data == expected_data


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_serial_searching_uppercase(crt_client, certificate_types, users, path):
    user = users['normal_user']
    crt_client.login(user.username)
    create_certificate(user, certificate_types[CERT_TYPE.ASSESSOR], serial_number='DEAADBEEF')

    cert = Certificate.objects.get()

    data = crt_client.json.get(path, {'_fields': 'id', 'serial_number': cert.serial_number}).json()

    assert data['count'] == 1
    assert data['results'][0]['id'] == cert.id

    data = crt_client.json.get(path, {'_fields': 'id', 'serial_number': cert.serial_number.lower()}).json()

    assert data['count'] == 1
    assert data['results'][0]['id'] == cert.id


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_updated__gt_filter(crt_client, users, certificate_types, path):
    user = users['normal_user']
    crt_client.login(user.username)

    for timestamp in (
        '1999-12-31T00:00:00+03:00',
        '2000-01-01T00:00:01+03:00',
        '2000-01-01T00:00:10+03:00',
        '2000-02-01T00:00:01+03:00',
    ):
        with freeze_time(timezone.datetime.fromisoformat(timestamp)):
            create_certificate(user, certificate_types[CERT_TYPE.ASSESSOR])

    response = crt_client.json.get(path, {'_fields': 'id', 'updated__gt': '2000-01-01T00:00:00'}).json()
    assert response['count'] == 3
    response = crt_client.json.get(path, {'_fields': 'id', 'updated__gt': '2000-01-01T00:00:01'}).json()
    assert response['count'] == 2
    response = crt_client.json.get(path, {'_fields': 'id', 'updated__gt': '2000-02-01T00:00:00'}).json()
    assert response['count'] == 1
    response = crt_client.json.get(path, {'_fields': 'id', 'updated__gt': '2000-02-01T00:01:00'}).json()
    assert response['count'] == 0


def test_username(crt_client, certificate_types, users):
    normal_user_cert = create_certificate(users['normal_user'], certificate_types[CERT_TYPE.PC])
    another_user_cert = create_certificate(users['another_user'], certificate_types[CERT_TYPE.PC])
    helpdesk_user_cert = create_certificate(users['helpdesk_user'], certificate_types[CERT_TYPE.PC])

    helpdesk_user = users['helpdesk_user']
    crt_client.login(helpdesk_user)

    response = crt_client.json.get('/api/certificate/', {'_fields': 'id', 'username': 'normal_user'})
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data['count'] == 1
    assert data['results'][0]['id'] == normal_user_cert.id

    response = crt_client.json.get('/api/certificate/', {'_fields': 'id', 'username': 'another_user'})
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data['count'] == 1
    assert data['results'][0]['id'] == another_user_cert.id

    #  В v1, если username не передан, юзер видит свои сертификаты
    response = crt_client.json.get('/api/certificate/', {'_fields': 'id'})
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data['count'] == 1
    assert data['results'][0]['id'] == helpdesk_user_cert.id

    response = crt_client.json.get('/api/certificate/', {'_fields': 'id', 'username': 'unexistent'})
    assert response.status_code == status.HTTP_200_OK
    data = response.json()
    assert data['count'] == 0
    assert data['results'] == []

    for all_value in ('', '__any__'):
        response = crt_client.json.get('/api/certificate/', {'_fields': 'id', 'username': all_value})
        assert response.status_code == status.HTTP_200_OK
        data = response.json()
        assert data['count'] == 3
        assert {c['id'] for c in data['results']} == {
            normal_user_cert.pk,
            another_user_cert.pk,
            helpdesk_user_cert.pk,
        }
