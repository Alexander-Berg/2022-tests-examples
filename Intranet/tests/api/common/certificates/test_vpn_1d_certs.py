import pytest
from rest_framework import status

from django.contrib.auth.models import Group
from django.utils.encoding import force_text

from intranet.crt.constants import CERT_TYPE, CA_NAME, CERT_STATUS, CERT_TEMPLATE
from intranet.crt.core.models import Certificate
from intranet.crt.csr.config import Vpn1DCsrConfig
from intranet.crt.utils.ssl import PemCertificate, get_x509_custom_extensions
from __tests__.utils.common import create_certificate_tag


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_vpn_1d_common_name_required(crt_client, users, path):
    vpn_1d_user = users['vpn_1d_user']
    crt_client.login(vpn_1d_user.username)

    request_data = {
        'type': CERT_TYPE.VPN_1D,
        'ca_name': CA_NAME.TEST_CA,
    }
    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json() == {'common_name': ['This field is required.']}


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_vpn_1d_permission_required(crt_client, users, user_datas, path, normal_user_ld_cert):
    normal_user = users['normal_user']
    crt_client.login(normal_user.username)

    request_data = {
        'type': CERT_TYPE.VPN_1D,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': f'{normal_user.username}@ld.yandex.ru',
    }
    response = crt_client.json.post(path, data=request_data)
    response_data = response.json()

    assert response.status_code == status.HTTP_403_FORBIDDEN
    assert response_data == {'detail': 'You do not have permission to perform this action.'}

    Group.objects.get(name='vpn_1d_group').user_set.add(normal_user)

    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_201_CREATED


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_vpn_1d(crt_client, users, user_datas, path):
    vpn_1d_user = users['vpn_1d_user']
    vpn_1d_user.email = f'{vpn_1d_user.username}@pytest'
    vpn_1d_user.save()
    crt_client.login(vpn_1d_user.username)

    common_name = f'{vpn_1d_user.username}@ld.yandex.ru'
    request_data = {
        'type': CERT_TYPE.VPN_1D,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': common_name,
    }
    response = crt_client.json.post(path, data=request_data)
    response_data = response.json()

    assert response.status_code == status.HTTP_201_CREATED
    assert response_data['common_name'] == common_name
    assert response_data['type'] == CERT_TYPE.VPN_1D
    assert response_data['status'] == CERT_STATUS.ISSUED
    if path == '/api/certificate/':
        assert response_data['requester'] == vpn_1d_user.username
        assert response_data['username'] == vpn_1d_user.username
    else:
        assert response_data['requester'] == user_datas[vpn_1d_user.username]
        assert response_data['user'] == user_datas[vpn_1d_user.username]

    cert = Certificate.objects.get(type__name=CERT_TYPE.VPN_1D)

    assert cert.status == 'issued'
    assert cert.common_name == common_name
    assert cert.user == vpn_1d_user
    assert cert.email == vpn_1d_user.email
    assert cert.requester == vpn_1d_user
    assert cert.used_template == CERT_TEMPLATE.VPN_1D
    assert cert.requested_by_csr is False


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_vpn_1d_request_ignored(crt_client, users, user_datas, path):
    vpn_1d_user = users['vpn_1d_user']
    crt_client.login(vpn_1d_user.username)
    common_name = f'{vpn_1d_user.username}@ld.yandex.ru'
    csr_config = Vpn1DCsrConfig(**{
        'country': 'RU',
        'city': 'Moscow',
        'email': 'normal_user@yandex-team.ru',
        'common_name': 'normal_user@ld.yandex.ru',
        'unit': 'lpc',
    })
    csr = force_text(csr_config.get_csr())
    request_data = {
        'type': CERT_TYPE.VPN_1D,
        'ca_name': CA_NAME.TEST_CA,
        'request': csr,
        'common_name': common_name,
    }
    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_201_CREATED

    cert = Certificate.objects.get(type__name=CERT_TYPE.VPN_1D)
    assert cert.common_name == common_name
    assert cert.user == vpn_1d_user
    assert cert.requester == vpn_1d_user
    assert cert.requested_by_csr is False


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_vpn_1d_for_another_user(crt_client, users, path):
    vpn_1d_user = users['vpn_1d_user']
    normal_user = users['normal_user']

    crt_client.login(vpn_1d_user.username)
    common_name = f'{normal_user.username}@ld.yandex.ru'
    request_data = {
        'type': CERT_TYPE.VPN_1D,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': common_name,
    }
    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_201_CREATED

    cert = Certificate.objects.get(type__name=CERT_TYPE.VPN_1D)
    assert cert.common_name == common_name
    assert cert.user == normal_user
    assert cert.requester == vpn_1d_user


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_vpn_1d_with_invalid_common_name(crt_client, users, path):
    vpn_1d_user = users['vpn_1d_user']
    crt_client.login(vpn_1d_user.username)

    common_name = f'{vpn_1d_user.username}@pda-ld.yandex.ru'
    request_data = {
        'type': CERT_TYPE.VPN_1D,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': common_name,
    }
    response = crt_client.json.post(path, data=request_data)
    response_data = response.json()

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response_data == {'common_name': ['Invalid common name']}


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_vpn_1d_with_invalid_user_in_common_name(crt_client, users, path):
    vpn_1d_user = users['vpn_1d_user']
    crt_client.login(vpn_1d_user.username)

    request_data = {
        'type': CERT_TYPE.VPN_1D,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': 'invalid_user@ld.yandex.ru',
    }
    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json() == {'common_name': ['User "invalid_user" does not exist']}

    request_data.update({'common_name': 'dismissed-user@ld.yandex.ru'})
    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json() == {'common_name': ['User "dismissed-user" is inactive']}


@pytest.mark.parametrize('helpdesk_ticket', (None, 'TEST-1'))
@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_vpn_1d_helpdesk_ticket(crt_client, users, user_datas, path, helpdesk_ticket):
    vpn_1d_user = users['vpn_1d_user']
    crt_client.login(vpn_1d_user.username)

    common_name = f'{vpn_1d_user.username}@ld.yandex.ru'
    request_data = {
        'type': CERT_TYPE.VPN_1D,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': common_name,
    }
    if helpdesk_ticket:
        request_data.update({'helpdesk_ticket': helpdesk_ticket})

    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_201_CREATED

    cert = Certificate.objects.get(type__name=CERT_TYPE.VPN_1D)
    assert cert.helpdesk_ticket == helpdesk_ticket


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_vpn_1d_tag_oids(crt_client, users, user_datas, path, tag_filters, certificate_types):
    vpn_1d_user = users['vpn_1d_user']
    normal_user = users['normal_user']
    tagfilter = tag_filters['mobile_filter']
    tagfilter.add_user(normal_user)
    create_certificate_tag('Office.VPN', filters=[tagfilter], types=[certificate_types['vpn-1d']])

    crt_client.login(vpn_1d_user.username)
    request_data = {
        'type': CERT_TYPE.VPN_1D,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': f'{normal_user.username}@ld.yandex.ru',
    }
    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_201_CREATED

    cert = Certificate.objects.get(type__name=CERT_TYPE.VPN_1D)
    custom_extensions = get_x509_custom_extensions(PemCertificate(cert.certificate).x509_object)

    assert custom_extensions['vpn_tags'].value == b'Default'
