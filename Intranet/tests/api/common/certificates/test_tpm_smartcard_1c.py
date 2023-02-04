import pytest

from intranet.crt.constants import CERT_TYPE, CA_NAME
from intranet.crt.core.models import Certificate
from __tests__.utils.ssl import get_modulus_from_pem, build_tpm_smartcard_1c_csr

pytestmark = pytest.mark.django_db


def test_issue_tpm_smartcard_1c_certificate(mocked_ldap, crt_client, users):
    normal_user = users['normal_user']
    tpm_smartcard_1c_user = users['tpm_smartcard_1c_user']
    csr_string = build_tpm_smartcard_1c_csr('Normal User')

    request_data = {
        'type': CERT_TYPE.TPM_SMARTCARD_1C,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': 'normal_user@ld.yandex.ru',
        'request': csr_string,
    }

    crt_client.login(normal_user)
    response = crt_client.json.post('/api/certificate/', data=request_data)
    assert response.status_code == 403

    crt_client.logout()
    crt_client.login(tpm_smartcard_1c_user)

    response = crt_client.json.post('/api/certificate/', data=request_data)

    assert response.status_code == 201

    cert = Certificate.objects.get()

    assert cert.user == normal_user
    assert cert.requester == tpm_smartcard_1c_user
    assert cert.common_name == 'normal_user@smartcard'
    assert csr_string.strip() == cert.request

    csr_req_modulus = get_modulus_from_pem(csr_string)
    cert_modulus = get_modulus_from_pem(cert.certificate)

    assert csr_req_modulus == cert_modulus


def test_issue_tpm_smartcard_1c_certificate_invalid_cn(mocked_ldap, crt_client, users):
    user = users['tpm_smartcard_1c_user']

    request_data = {
        'type': CERT_TYPE.TPM_SMARTCARD_1C,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': 'normal_user@ld.yandex.ru',
        'request': build_tpm_smartcard_1c_csr('NoLDAP User'),
    }
    crt_client.login(user)
    response = crt_client.json.post('/api/certificate/', data=request_data)

    assert response.status_code == 400
    assert ('CSR Subject \'CN=NoLDAP User,CN=Users,DC=ld,DC=yandex,DC=ru\''
            ' does not match any LDAP distinguishedName' in str(response.json()))


def test_issue_tpm_smartcard_1c_certificate_foreign_user(mocked_ldap, crt_client, users):
    tpm_smartcard_1c_user = users['tpm_smartcard_1c_user']
    csr_string = build_tpm_smartcard_1c_csr('Another User', foreign=True)

    request_data = {
        'type': CERT_TYPE.TPM_SMARTCARD_1C,
        'ca_name': CA_NAME.TEST_CA,
        'request': csr_string,
    }

    crt_client.login(tpm_smartcard_1c_user)

    response = crt_client.json.post('/api/certificate/', data=request_data)

    assert response.status_code == 201
    cert = Certificate.objects.get()

    assert cert.user.username == 'another_user'
    assert cert.requester == tpm_smartcard_1c_user
