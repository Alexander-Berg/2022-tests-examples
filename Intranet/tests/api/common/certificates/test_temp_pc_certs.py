import pytest
from constance.test import override_config
from rest_framework import status

from django.contrib.auth.models import Group
from django.utils.encoding import force_text

from intranet.crt.constants import CERT_TYPE, CA_NAME, CERT_STATUS, CERT_TEMPLATE
from intranet.crt.core.models import Certificate
from intranet.crt.csr.config import TempPcCsrConfig
from intranet.crt.utils.ssl import get_x509_custom_extensions
from __tests__.utils.common import create_certificate
from __tests__.utils.ssl import build_temp_pc_csr, PemCertificate


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_temp_pc_csr_required(crt_client, users, path):
    temp_pc_user = users['temp_pc_user']
    crt_client.login(temp_pc_user.username)

    request_data = {
        'type': CERT_TYPE.TEMP_PC,
        'ca_name': CA_NAME.TEST_CA,
    }

    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json() == {'request': ['This field is required.']}


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_temp_pc_permission_required(crt_client, users, user_datas,
                                             path, normal_user_ld_cert):
    normal_user = users['normal_user']
    crt_client.login(normal_user.username)

    common_name = f'{normal_user.username}@pda-ld.yandex.ru'

    request_data = {
        'type': CERT_TYPE.TEMP_PC,
        'ca_name': CA_NAME.TEST_CA,
        'request': build_temp_pc_csr(common_name),
    }

    response = crt_client.json.post(path, data=request_data)
    response_data = response.json()

    assert response.status_code == status.HTTP_403_FORBIDDEN
    assert response_data == {'detail': 'You do not have permission to perform this action.'}

    Group.objects.get(name='temp_pc_issue').user_set.add(normal_user)

    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_201_CREATED


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_temp_pc(crt_client, users, user_datas, path, temp_pc_user_ld_cert):
    temp_pc_user = users['temp_pc_user']
    temp_pc_user.email = 'temp_pc_user@pytest'
    temp_pc_user.save()

    crt_client.login(temp_pc_user.username)

    common_name = f'{temp_pc_user.username}@pda-ld.yandex.ru'

    request_data = {
        'type': CERT_TYPE.TEMP_PC,
        'ca_name': CA_NAME.TEST_CA,
        'request': build_temp_pc_csr(common_name),
    }

    response = crt_client.json.post(path, data=request_data)
    response_data = response.json()

    assert response.status_code == status.HTTP_201_CREATED
    assert response_data['common_name'] == common_name
    assert response_data['type'] == CERT_TYPE.TEMP_PC
    assert response_data['status'] == CERT_STATUS.ISSUED
    if path == '/api/certificate/':
        assert response_data['requester'] == temp_pc_user.username
        assert response_data['username'] == temp_pc_user.username
    else:
        assert response_data['requester'] == user_datas[temp_pc_user.username]
        assert response_data['user'] == user_datas[temp_pc_user.username]

    cert = Certificate.objects.get(type__name=CERT_TYPE.TEMP_PC)

    assert cert.status == 'issued'
    assert cert.common_name == common_name
    assert cert.user == temp_pc_user
    assert cert.requester == temp_pc_user
    assert cert.email == 'temp_pc_user@pytest'
    assert cert.used_template == CERT_TEMPLATE.USER_PDAS_1D
    assert cert.requested_by_csr is True


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_temp_pc_common_name_ignored(crt_client, users, user_datas,
                                             path, temp_pc_user_ld_cert):
    temp_pc_user = users['temp_pc_user']

    crt_client.login(temp_pc_user.username)

    common_name = f'{temp_pc_user.username}@pda-ld.yandex.ru'

    request_data = {
        'type': CERT_TYPE.TEMP_PC,
        'ca_name': CA_NAME.TEST_CA,
        'request': build_temp_pc_csr(common_name),
        'common_name': 'another_user@pda-ld.yandex.ru'
    }

    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_201_CREATED

    cert = Certificate.objects.get(type__name=CERT_TYPE.TEMP_PC)

    assert cert.common_name == common_name
    assert cert.user == temp_pc_user
    assert cert.requester == temp_pc_user


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_temp_pc_for_another_user(crt_client, users, path, temp_pc_user_ld_cert):
    helpdesk_user = users['helpdesk_user']
    temp_pc_user = users['temp_pc_user']

    Group.objects.get(name='temp_pc_issue').user_set.add(helpdesk_user)

    crt_client.login(helpdesk_user.username)

    common_name = f'{temp_pc_user.username}@pda-ld.yandex.ru'

    request_data = {
        'type': CERT_TYPE.TEMP_PC,
        'ca_name': CA_NAME.TEST_CA,
        'request': build_temp_pc_csr(common_name),
    }

    response = crt_client.json.post(path, data=request_data)
    response_data = response.json()

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response_data == {'request': ['CSR Common Name does not match authorized user']}


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_temp_pc_with_invalid_common_name(crt_client, users, path, temp_pc_user_ld_cert):
    temp_pc_user = users['temp_pc_user']
    crt_client.login(temp_pc_user.username)

    common_name = f'{temp_pc_user.username}@ld.yandex.ru'

    request_data = {
        'type': CERT_TYPE.TEMP_PC,
        'ca_name': CA_NAME.TEST_CA,
        'request': build_temp_pc_csr(common_name),
    }

    response = crt_client.json.post(path, data=request_data)
    response_data = response.json()

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response_data == {'request': ['Invalid common name']}


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_temp_pc_with_extensions(crt_client, users, path, temp_pc_user_ld_cert):
    temp_pc_user = users['temp_pc_user']
    crt_client.login(temp_pc_user.username)

    common_name = f'{temp_pc_user.username}@pda-ld.yandex.ru'

    csr_config = TempPcCsrConfig(common_name=common_name)
    csr_config.update_extensions_with_context({
        'wired_tags': ['Other'],
        'wireless_tags': ['PDAS'],
    })
    csr = force_text(csr_config.get_csr())

    request_data = {
        'type': CERT_TYPE.TEMP_PC,
        'ca_name': CA_NAME.TEST_CA,
        'request': csr,
    }
    crt_client.json.post(path, data=request_data)

    cert = Certificate.objects.get(type__name=CERT_TYPE.TEMP_PC)

    custom_extensions = get_x509_custom_extensions(PemCertificate(cert.certificate).x509_object)

    assert custom_extensions['wired_tags'].value == b'Other'
    assert custom_extensions['wireless_tags'].value == b'PDAS'


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_temp_pc_with_forbidden_extensions(crt_client, users, path, temp_pc_user_ld_cert):
    temp_pc_user = users['temp_pc_user']
    crt_client.login(temp_pc_user.username)

    common_name = f'{temp_pc_user.username}@pda-ld.yandex.ru'

    csr_config = TempPcCsrConfig(common_name=common_name)
    csr_config.update_extensions_with_context({
        'wired_tags': ['Other'],
        'wireless_tags': ['Yandex'],
        'vpn_tags': ['Default'],
    })
    csr = force_text(csr_config.get_csr())

    request_data = {
        'type': CERT_TYPE.TEMP_PC,
        'ca_name': CA_NAME.TEST_CA,
        'request': csr,
    }
    response = crt_client.json.post(path, data=request_data)
    response_data = response.json()

    assert response.status_code == status.HTTP_403_FORBIDDEN
    assert response_data == {'detail': 'CSR contains restricted extensions'}

    csr_config = TempPcCsrConfig(common_name=common_name)
    csr_config.update_extensions_with_context({
        'wired_tags': ['Yandex'],
        'wireless_tags': ['Yandex', 'PDAS'],
    })
    csr = force_text(csr_config.get_csr())
    request_data.update({'request': csr})

    response = crt_client.json.post(path, data=request_data)
    response_data = response.json()

    assert response.status_code == status.HTTP_403_FORBIDDEN
    assert response_data == {'detail': 'CSR extensions contains restricted values'}

    csr_config = TempPcCsrConfig(common_name=common_name)
    csr_config.update_extensions_with_context({
        'wired_tags': ['Other'],
        'wireless_tags': ['PDAS'],
    })
    csr = force_text(csr_config.get_csr())
    request_data.update({'request': csr})

    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_201_CREATED

    cert = Certificate.objects.get(type__name=CERT_TYPE.TEMP_PC)
    custom_extensions = get_x509_custom_extensions(PemCertificate(cert.certificate).x509_object)

    assert custom_extensions['wired_tags'].value == b'Other'
    assert custom_extensions['wireless_tags'].value == b'PDAS'


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_temp_pc_revokes_existing_certs(crt_client, users, user_datas,
                                                path, temp_pc_user_ld_cert):
    temp_pc_user = users['temp_pc_user']
    temp_pc_user.email = 'temp_pc_user@pytest'
    temp_pc_user.save()

    crt_client.login(temp_pc_user.username)

    common_name = f'{temp_pc_user.username}@pda-ld.yandex.ru'

    request_data = {
        'type': CERT_TYPE.TEMP_PC,
        'ca_name': CA_NAME.TEST_CA,
        'request': build_temp_pc_csr(common_name),
    }

    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_201_CREATED

    cert = Certificate.objects.get(type__name=CERT_TYPE.TEMP_PC)

    request_data = {
        'type': CERT_TYPE.TEMP_PC,
        'ca_name': CA_NAME.TEST_CA,
        'request': build_temp_pc_csr(common_name),
    }

    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_201_CREATED

    cert.refresh_from_db()

    assert cert.status == CERT_STATUS.REVOKED
    assert Certificate.objects.filter(status=CERT_STATUS.REVOKED).count() == 1
    assert Certificate.objects.filter(
        status=CERT_STATUS.ISSUED,
        type__name=CERT_TYPE.TEMP_PC
    ).count() == 1


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_temp_pc_issue_requires_active_ld_cert(crt_client, users, user_datas,
                                               path, certificate_types):
    temp_pc_user = users['temp_pc_user']
    temp_pc_user.email = 'temp_pc_user@pytest'
    temp_pc_user.save()

    crt_client.login(temp_pc_user.username)

    common_name = f'{temp_pc_user.username}@pda-ld.yandex.ru'
    request_data = {
        'type': CERT_TYPE.TEMP_PC,
        'ca_name': CA_NAME.TEST_CA,
        'request': build_temp_pc_csr(common_name),
    }
    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json() == {
        'request': [
            'User temp_pc_user has no active certificates '
            'to access Yandex network. Issuing prohibited.'
        ]
    }

    create_certificate(
        temp_pc_user, certificate_types['pc'], common_name=f'{temp_pc_user.username}@ld.yandex.ru'
    )
    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_201_CREATED

    # Проверим PDAS_WHITELIST
    temp_pc_user.certificates.all().delete()
    with override_config(PDAS_WHITELIST='temp_pc_user'):
        response = crt_client.json.post(path, data=request_data)
        assert response.status_code == status.HTTP_201_CREATED
