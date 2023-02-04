import urllib.parse
import pytest
from rest_framework import status

from intranet.crt.constants import CERT_TYPE, CA_NAME
from intranet.crt.core.models import Certificate
from intranet.crt.utils.ssl import parse_pfx

from __tests__.utils.common import mock_startrek

pytestmark = pytest.mark.django_db


def test_get_pfx(crt_client, users):
    requester = 'normal_user'

    crt_client.login(requester)

    non_csr_common_name = 'normal_user@ld.yandex.ru'
    request_data = {
        'type': CERT_TYPE.LINUX_PC,
        'ca_name': CA_NAME.TEST_CA,
        'pc_os': 'dummy',
        'pc_mac': 'dummy',
        'pc_hostname': 'dummy',
        'common_name': non_csr_common_name,
    }

    create_response = crt_client.json.post('/api/certificate/', data=request_data)
    assert create_response.status_code == status.HTTP_201_CREATED

    create_data = create_response.json()
    passphrase = 'qweasd'
    pfx_url = urllib.parse.urlsplit(create_data['download']).path

    get_pfx_data = {'password': passphrase}
    pfx_response = crt_client.json.get(pfx_url, data=get_pfx_data)
    assert pfx_response.status_code == status.HTTP_200_OK

    pfx_certificate, pfx_private_key = parse_pfx(pfx_response.content, passphrase)

    db_certificate = Certificate.objects.get(serial_number=create_data['serial_number'])
    assert db_certificate.certificate == pfx_certificate
    assert db_certificate.priv_key == pfx_private_key


def test_get_download2(crt_client, users):
    requester = 'normal_user'

    crt_client.login(requester)

    non_csr_common_name = 'normal_user@ld.yandex.ru'
    request_data = {
        'type': CERT_TYPE.LINUX_PC,
        'ca_name': CA_NAME.TEST_CA,
        'pc_os': 'dummy',
        'pc_mac': 'dummy',
        'pc_hostname': 'dummy',
        'common_name': non_csr_common_name,
    }

    create_response = crt_client.json.post('/api/certificate/', data=request_data)
    assert create_response.status_code == status.HTTP_201_CREATED

    create_data = create_response.json()
    download_url = urllib.parse.urlsplit(create_data['download2']).path
    certificate = Certificate.objects.get()
    assert download_url == '/api/certificate/%d/download' % certificate.pk

    pfx_response = crt_client.json.get(download_url)
    assert pfx_response.status_code == status.HTTP_200_OK
    assert b'-----BEGIN PRIVATE KEY-----' in pfx_response.content


def test_pfx_does_not_render_if_certificate_is_not_issued_yet(crt_client, users, settings):
    requester = 'normal_user'

    crt_client.login(requester)

    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.APPROVABLE_TEST_CA,
        'hosts': 'xxx.yandex.xxx',
    }

    with mock_startrek():
        response = crt_client.json.post('/api/certificate/', data=request_data)

    assert response.status_code == status.HTTP_201_CREATED
    data = response.json()
    assert data['status'] == 'need_approve'
    certificate = Certificate.objects.get()
    download_url = '/api/certificate/%d/download' % certificate.pk
    pfx_response = crt_client.json.get(download_url)
    assert pfx_response.status_code == 404
    assert pfx_response.content == b'Object is not updated on replica yet. Expected available in 5 seconds.'
    assert pfx_response['Retry-After'] == str(settings.CRT_EXPECTED_REPLICATION_LAG)
