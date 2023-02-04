import mock
import pytest

from django.contrib.auth.models import Permission
from django.utils.encoding import force_text
from rest_framework import status

from intranet.crt.constants import CERT_TYPE, CA_NAME
from intranet.crt.csr import FullSubjectCsrConfig


pytestmark = pytest.mark.django_db


def make_csr():
    csr_config = FullSubjectCsrConfig(
        common_name='normal_user@ld.yandex.ru',
        email='normal_user@yandex-team.ru',
        country='RU',
        city='Moscow',
        unit='Infra',
    )
    return force_text(csr_config.get_csr())


@pytest.fixture(autouse=True)
def mock_bot():
    with mock.patch('intranet.crt.api.base.serializer_mixins.personal.get_inum_by_sn') as get_inum_by_sn:
        get_inum_by_sn.return_value = 'inum'
        yield


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_issue_bank_pc_cert_permission(crt_client, users, path):
    request_data = {
        'type': CERT_TYPE.BANK_CLIENT_SERVER,
        'ca_name': CA_NAME.TEST_CA,
        'request': make_csr(),
        'pc_os': 'os',
        'pc_hostname': 'hostname',
        'pc_serial_number': 'serial',
        'pc_mac': 'mac',
    }

    another_user = users['another_user']
    crt_client.login(another_user.username)

    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_403_FORBIDDEN

    permission = Permission.objects.get(codename='can_issue_bank_client_server_certificates')
    another_user.user_permissions.add(permission)

    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_201_CREATED


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_issue_bank_pc_request_field_is_required(crt_client, users, path):
    request_data = {
        'type': CERT_TYPE.BANK_CLIENT_SERVER,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': 'normal_user@ld.yandex.ru',
        'pc_os': 'os',
        'pc_hostname': 'hostname',
        'pc_serial_number': 'serial',
        'pc_mac': 'mac',
    }
    another_user = users['another_user']
    permission = Permission.objects.get(codename='can_issue_bank_client_server_certificates')
    another_user.user_permissions.add(permission)

    crt_client.login(another_user.username)

    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json() == {'request': ['This field is required.']}
