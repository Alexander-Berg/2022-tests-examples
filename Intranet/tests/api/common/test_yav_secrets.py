import mock
import pytest

from rest_framework import status

from intranet.crt.constants import CERT_TYPE, CA_NAME
from __tests__.utils.common import MockYavClient

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('secret_uuid', [None, 'valid_secret_uuid', 'access_denied_secret_uuid', 'another_secret_uuid'])
def test_request_without_csr(crt_client, users, path, secret_uuid, abc_services, settings):
    requester = 'helpdesk_user'

    # проверим, что в запросах без csr валидируется secret_id
    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.TEST_CA,
        'hosts': 'test.yandex-team.ru',
        'abc_service': 1,
    }
    if secret_uuid:
        request_data['yav_secret_id'] = secret_uuid

    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)
    with mock.patch('intranet.crt.api.v1.certificates.serializers.base.get_yav_client') as get_client:
        get_client.return_value = MockYavClient()
        response = crt_client.json.post(path, data=request_data)

    data = response.json()
    if not secret_uuid or secret_uuid == 'valid_secret_uuid':
        assert response.status_code == status.HTTP_201_CREATED
        assert data.get('yav_secret_id') == secret_uuid
        assert data['requested_by_csr'] is False
    else:
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        assert data == {
            'yav_secret_id': [
                'Секрет с id {0} не существует или у пользователя {1} отсутствует к нему доступ'
                .format(secret_uuid, settings.CRT_ROBOT)
            ]
        }


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_with_csr(crt_client, users, path, abc_services, settings, pc_csrs):
    requester = 'helpdesk_user'

    # проверим, что при запросе с csr secret_id не валидируется
    # (при запросе по csr в секретницу ничего не пишем)
    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.TEST_CA,
        'hosts': 'test.yandex-team.ru',
        'abc_service': 1,
        'request': pc_csrs['normal_user'],
        'yav_secret_id': 'another_secret_uuid',
    }

    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)
    with mock.patch('intranet.crt.api.v1.certificates.serializers.base.get_yav_client') as get_client:
        get_client.return_value = MockYavClient()
        response = crt_client.json.post(path, data=request_data)

    data = response.json()
    assert response.status_code == status.HTTP_201_CREATED
    assert data['requested_by_csr']
