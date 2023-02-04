from constance.test import override_config

import mock
import pytest
from django.contrib.auth.models import Permission
from django.utils.encoding import force_text
from rest_framework import status

from intranet.crt.constants import CERT_TYPE, CA_NAME, CERT_STATUS
from intranet.crt.core.models import Certificate
from intranet.crt.csr import ZombieCsrConfig, FullSubjectCsrConfig
from __tests__.utils.ssl import CsrBuilder

pytestmark = pytest.mark.django_db


@pytest.fixture
def csr_content(users):
    return {
        'country': 'RU',
        'city': 'Moscow',
        'email': 'imdm_user@yandex-team.ru',
        'common_name': 'zomb-user@ld.yandex.ru',
        'unit': 'zomb',
    }


@pytest.fixture
def request_data():
    return {
        'type': CERT_TYPE.ZOMBIE,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': 'zomb-user@ld.yandex.ru',
    }


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_zombie_by_common_name(crt_client, helpdesk_user, user_datas, request_data, path):
    crt_client.login(helpdesk_user.username)

    response = crt_client.json.post(path, data=request_data)
    response_data = response.json()
    assert response.status_code == status.HTTP_201_CREATED
    assert response_data['common_name'] == request_data['common_name']
    assert response_data['type'] == CERT_TYPE.ZOMBIE
    assert response_data['status'] == CERT_STATUS.ISSUED
    if path == '/api/certificate/':
        assert response_data['requester'] == helpdesk_user.username
        assert response_data['username'] == 'zomb-user'
    else:
        assert response_data['requester'] == user_datas[helpdesk_user.username]
        assert response_data['user'] == user_datas['zomb-user']


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_zombie_by_request(crt_client, csr_content, helpdesk_user, user_datas, request_data, path):
    csr_config = ZombieCsrConfig(**csr_content)
    request_data['request'] = force_text(csr_config.get_csr())
    del request_data['common_name']

    crt_client.login(helpdesk_user.username)

    response = crt_client.json.post(path, data=request_data)
    response_data = response.json()
    assert response.status_code == status.HTTP_201_CREATED
    assert response_data['common_name'] == csr_content['common_name']
    assert response_data['type'] == CERT_TYPE.ZOMBIE
    assert response_data['status'] == CERT_STATUS.ISSUED
    if path == '/api/certificate/':
        assert response_data['requester'] == helpdesk_user.username
        assert response_data['username'] == 'zomb-user'
    else:
        assert response_data['requester'] == user_datas[helpdesk_user.username]
        assert response_data['user'] == user_datas['zomb-user']

    csr_content['unit'] = 'invalid'
    csr_config = FullSubjectCsrConfig(**csr_content)
    request_data['request'] = force_text(csr_config.get_csr())

    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json()['request'] == ['Invalid organizational unit name']


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_zombie_without_permission(crt_client, users, request_data, path):
    crt_client.login('normal_user')

    response = crt_client.json.post(path, data=request_data)

    response_data = response.json()
    assert response.status_code == status.HTTP_403_FORBIDDEN
    assert response_data['detail'] == 'You do not have permission to perform this action.'


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('common_name', [
    'invalid',
    'normal_user@ld.yandex.ru',
    'user@ld.yandex.ru',
])
def test_request_zombie_with_invalid_common_name(crt_client, helpdesk_user, request_data, common_name, path, users):
    crt_client.login(helpdesk_user.username)

    request_data['common_name'] = common_name

    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json()['common_name'] == ['Invalid common name']

    request_data['common_name'] = 'zomb-user-fake@ld.yandex.ru'

    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json()['non_field_errors'] == ['User must be a robot']

    del request_data['common_name']

    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json()['non_field_errors'] == ['Cannot find common_name/request field']


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_zombie_by_request_multiply_oid(crt_client, helpdesk_user, request_data, path):
    crt_client.login(helpdesk_user.username)
    del request_data['common_name']
    csr = (
        CsrBuilder()
        .add_common_name('zomb-user@ld.yandex.ru')
        .add_email_address('imdm_user@yandex-team.ru')
        .add_unit_name('zomb')
        .add_unit_name('zomb_2')
        .get_pem_csr()
    )
    request_data['request'] = force_text(csr)
    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json()['request'] == ['Multiply Organizational Unit Name oid']


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('by_csr', [True, False])
def test_request_zombie_with_market_zombie_permission(crt_client, users, request_data, path, csr_content, by_csr):
    requester = users['normal_user']
    crt_client.login('normal_user')
    request_data = {
        'type': CERT_TYPE.ZOMBIE,
        'ca_name': CA_NAME.TEST_CA,
    }
    if by_csr:
        csr_config = FullSubjectCsrConfig(**csr_content)
        request_data.update({'request': force_text(csr_config.get_csr())})
    else:
        request_data.update({'common_name': 'zomb-user@ld.yandex.ru'})

    response = crt_client.json.post(path, data=request_data)

    response_data = response.json()
    assert response.status_code == status.HTTP_403_FORBIDDEN
    assert response_data['detail'] == 'You do not have permission to perform this action.'

    permission = Permission.objects.get(codename='can_issue_market_zombie_certificates')
    requester.user_permissions.add(permission)

    with override_config(MARKET_ZOMBIES='zomb-user, zomb-user2'):
        response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_201_CREATED


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('field_name', ('pc_inum', 'pc_serial_number', 'pc_hostname', 'pc_mac'))
@pytest.mark.parametrize('field_is_set', [True, False])
def test_request_zombie_optional_fields(
    crt_client, users, request_data, path, csr_content, field_name, field_is_set,
):
    crt_client.login('helpdesk_user')
    if field_is_set:
        request_data.update({field_name: '42'})

    with mock.patch('intranet.crt.api.base.serializer_mixins.personal.get_inum_by_sn') as get_inum_by_sn:
        get_inum_by_sn.return_value = None
        response_data = crt_client.json.post(path, data=request_data).json()

    assert response_data.get(field_name) == ('42' if field_is_set else None)

    cert = Certificate.objects.get()
    assert getattr(cert, field_name) == ('42' if field_is_set else None)
