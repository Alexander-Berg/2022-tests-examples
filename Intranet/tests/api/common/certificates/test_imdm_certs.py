import pytest
from django.utils.encoding import force_text
from rest_framework import status

from intranet.crt.constants import CERT_TYPE, CA_NAME, CERT_STATUS
from intranet.crt.csr import FullSubjectCsrConfig
from __tests__.utils.common import create_permission, create_group, create_user

pytestmark = pytest.mark.django_db


imdm_user_data = {
    'username': 'imdm_user',
    'first_name': {'en': 'imdm_user', 'ru': 'imdm_user'},
    'last_name': {'en': 'imdm_user', 'ru': 'imdm_user'},
    'is_active': True,
    'in_hiring': False,
}


@pytest.fixture
def imdm_user():
    can_issue_imdm_certificates = create_permission('can_issue_imdm_certificates', 'core', 'certificate')
    imdm_group = create_group('imdm_group', permissions=[can_issue_imdm_certificates])
    return create_user('imdm_user', groups=[imdm_group])


@pytest.fixture
def csr_content(users):
    return {
        'country': 'RU',
        'city': 'Moscow',
        'email': 'normal_user@yandex-team.ru',
        'common_name': '{}@ld.yandex.ru'.format('normal_user'),
        'unit': 'MOBILE',
    }


@pytest.fixture
def request_data():
    return {
        'type': CERT_TYPE.IMDM,
        'ca_name': CA_NAME.TEST_CA,
        'pc_serial_number': 'FK2VTQEAJCL',
    }


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_imdm(crt_client, imdm_user, csr_content, request_data, path, user_datas):
    csr_config = FullSubjectCsrConfig(**csr_content)

    request_data['request'] = force_text(csr_config.get_csr())

    crt_client.login(imdm_user.username)

    response = crt_client.json.post(path, data=request_data)
    response_data = response.json()
    assert response.status_code == status.HTTP_201_CREATED
    assert response_data['common_name'] == csr_content['common_name']
    assert response_data['type'] == CERT_TYPE.IMDM
    assert response_data['status'] == CERT_STATUS.ISSUED
    if path == '/api/certificate/':
        assert response_data['requester'] == imdm_user.username
        assert response_data['username'] == 'normal_user'
    else:
        assert response_data['requester'] == imdm_user_data
        assert response_data['user'] == user_datas['normal_user']


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_imdm_without_perm(crt_client, users, request_data, path):
    requester = 'helpdesk_user'

    crt_client.login(requester)
    response = crt_client.json.post(path, data=request_data)

    response_data = response.json()
    assert response.status_code == status.HTTP_403_FORBIDDEN
    assert response_data['detail'] == 'You do not have permission to perform this action.'


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_imdm_without_csr(crt_client, imdm_user, request_data, path):
    crt_client.login(imdm_user.username)
    del request_data['pc_serial_number']
    response = crt_client.json.post(path, data=request_data)

    response_data = response.json()
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response_data['request'] == ['This field is required.']
    assert response_data['pc_serial_number'] == ['This field is required.']


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_imdm_for_mobile_user(crt_client, imdm_user, csr_content, request_data, path):
    # pda-ld должно быть валидным вариантом
    csr_content['common_name'] = '{}@pda-ld.yandex.ru'.format(imdm_user.username)
    csr_config = FullSubjectCsrConfig(**csr_content)

    request_data['request'] = force_text(csr_config.get_csr())
    request_data['pc_serial_number'] = 'A'*12

    crt_client.login(imdm_user.username)
    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_201_CREATED


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_imdm_with_invalid_common_name(crt_client, imdm_user, csr_content, request_data, path):
    csr_content['common_name'] = 'noway@example.com'
    csr_config = FullSubjectCsrConfig(**csr_content)

    request_data['request'] = force_text(csr_config.get_csr())
    request_data['pc_serial_number'] = 'd/_-='

    crt_client.login(imdm_user.username)
    response = crt_client.json.post(path, data=request_data)

    response_data = response.json()
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response_data['request'] == ['Invalid common name']
    assert response_data['pc_serial_number'] == ['invalid field format']


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_imdm_with_invalid_user(crt_client, imdm_user, csr_content, request_data, path):
    csr_content['common_name'] = 'noway@ld.yandex.ru'
    csr_config = FullSubjectCsrConfig(**csr_content)

    request_data['request'] = force_text(csr_config.get_csr())

    crt_client.login(imdm_user.username)
    response = crt_client.json.post(path, data=request_data)

    response_data = response.json()
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response_data['request'] == ['User "noway" does not exist']


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_imdm_with_invalid_unit(crt_client, imdm_user, csr_content, request_data, path):
    csr_content['unit'] = 'invalid'
    csr_config = FullSubjectCsrConfig(**csr_content)

    request_data['request'] = force_text(csr_config.get_csr())

    crt_client.login(imdm_user.username)
    response = crt_client.json.post(path, data=request_data)

    response_data = response.json()
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response_data['request'] == ['Invalid organizational unit name']
