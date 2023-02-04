import pytest
import mock
from rest_framework import status

from intranet.crt.constants import CERT_TYPE, CERT_STATUS, CA_NAME

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('is_active', [True, False])
@pytest.mark.parametrize('in_hiring', [True, False])
def test_pc_certificates_for_inhiring_users(crt_client, users, pc_csrs, is_active, in_hiring):
    requester = 'helpdesk_user'
    user = 'normal_user'
    normal_user = users[user]
    normal_user.is_active = is_active
    normal_user.in_hiring = in_hiring
    normal_user.save()

    request_data = {
        'type': CERT_TYPE.PC,
        'ca_name': CA_NAME.TEST_CA,
        'pc_os': 'Mac OS X 10.12',
        'pc_hostname': 'mac_user02',
        'pc_serial_number': '1111',
        'pc_mac': '111',
        'request': pc_csrs[user],
    }

    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    with mock.patch('intranet.crt.api.base.serializer_mixins.personal.get_inum_by_sn') as get_inum_by_sn:
        get_inum_by_sn.return_value = 'inum'
        response = crt_client.json.post('/api/certificate/', data=request_data)

    if is_active or in_hiring:
        assert response.status_code == status.HTTP_201_CREATED
        response_data = response.json()
        assert response_data['type'] == CERT_TYPE.PC
        assert response_data['username'] == user
        assert response_data['requester'] == requester
        assert response_data['status'] == CERT_STATUS.ISSUED
    else:
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        response_data = response.json()
        assert response_data == {
            'request': ['User "normal_user" is inactive']
        }
