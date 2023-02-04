import mock
import pytest
from rest_framework import status

from intranet.crt.constants import CERT_TYPE, CA_NAME
from intranet.crt.core.models import Certificate

pytestmark = pytest.mark.django_db

fake_certificate = '''-----BEGIN CERTIFICATE-----
MIIEmzCCA4OgAwIBAgIKH3S2iAAAAAAARDANBgkqhkiG9w0BAQsFADBbMRIwEAYK
CZImiZPyLGQBGRYCcnUxFjAUBgoJkiaJk/IsZAEZFgZ5YW5kZXgxEjAQBgoJkiaJ
k/IsZAEZFgJsZDEZMBcGA1UEAxMQWWFuZGV4SW50ZXJuYWxDQTAeFw0xMzA5Mjcx
NTIwNTdaFw0xNDA5MjcxNTIwNTdaMIGkMQswCQYDVQQGEwJydTEPMA0GA1UECBMG
UnVzc2lhMQ8wDQYDVQQHEwZNb3Njb3cxDzANBgNVBAoTBllhbmRleDEgMB4GA1UE
CwwXeWFuZGV4X21udF9pbmZyYV9vZmZpY2UxGzAZBgNVBAMMEnNsYXNoQGxkLnlh
bmRleC5ydTEjMCEGCSqGSIb3DQEJARYUc2xhc2hAeWFuZGV4LXRlYW0ucnUwgZ8w
DQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAK2DHX5bifWF1WALTEXFMvhO3OJMTwi8
6xR+OUzjNOTERxwrxA3R2cBVhN7uFXQhHo4e8w8Azo2/Jh/ECmt3zFzXHsyKvBzC
OzTCEAa1yF34gfUyNngKJwbP4UGp6MQIbgDGPrWkBnRmmZLRDHLfxelFnO0Fkf+j
MT0qjPHq1rpRAgMBAAGjggGZMIIBlTALBgNVHQ8EBAMCBaAwKQYDVR0lBCIwIAYK
KwYBBAGCNwoDBAYIKwYBBQUHAwQGCCsGAQUFBwMCMB0GA1UdDgQWBBRY8zzES8z8
pCiCxow/5uaDn0gofzAfBgNVHSMEGDAWgBSP3TKDCRNT3ZEaZumz1DzFtPJnSDBM
BgNVHR8ERTBDMEGgP6A9hjtodHRwOi8vY3Jscy55YW5kZXgucnUvWWFuZGV4SW50
ZXJuYWxDQS9ZYW5kZXhJbnRlcm5hbENBLmNybDBXBggrBgEFBQcBAQRLMEkwRwYI
KwYBBQUHMAKGO2h0dHA6Ly9jcmxzLnlhbmRleC5ydS9ZYW5kZXhJbnRlcm5hbENB
L1lhbmRleEludGVybmFsQ0EucDdiMD0GCSsGAQQBgjcVBwQwMC4GJisGAQQBgjcV
CIbcoSaFk9VVh/2BLYP55wKDldIEHIffj3eEj7s3AgFkAgEPMDUGCSsGAQQBgjcV
CgQoMCYwDAYKKwYBBAGCNwoDBDAKBggrBgEFBQcDBDAKBggrBgEFBQcDAjANBgkq
hkiG9w0BAQsFAAOCAQEApfBII2nLhdedpeJ6RW2InJopLPWJ8QDgZy0em9nME0EB
8eeysnWOI7VSgKX6Wc/PuaYHdPOYGMTnohJNIFLI1/1DjADCjiqAki8RXnP1WjW4
EEYrr6wJXqoNbZeSqFJBz5cbkfoK/Qczlyuib2xxptOozwpDG9DfySObjXdwlxkO
Sl6OsiyJrUYHXenvw3hvwsIFBu4utaIMFSx48I8nrJLAm2LHRp8xUXpN5BZZ37OF
JPpal+Ed7jWgnhBd8YiutUumXCxlhvedny7EAtHqB3rPKUsv1AIl4YTqVLTzR4BB
2nLWVWs7sRzdzj7LOchi8HWPA2pha3kY/m09brC7YA==
-----END CERTIFICATE-----
'''


def request_test_certificate(crt_client, username, cert_type, common_name, desired_ttl_days):
    request_data = {
        'type': cert_type,
        'ca_name': CA_NAME.INTERNAL_TEST_CA,
        'common_name': common_name,
    }

    if desired_ttl_days is not None:
        request_data['desired_ttl_days'] = desired_ttl_days

    crt_client.login(username)
    with mock.patch('intranet.crt.core.ca.registry.InternalCA.cert_request') as cert_request:
        with mock.patch('intranet.crt.core.ca.registry.InternalCA.get_certificate') as get_certificate:
            cert_request.return_value = 'url'
            get_certificate.return_value = fake_certificate
            response = crt_client.json.post('/api/certificate/', data=request_data)

    return response


@pytest.mark.parametrize('desired_ttl_days_templates', [
    (None, 'yaRC-Server'),
    (14, 'yaRC-Server_2w'),
    (30, 'yaRC-Server_1m'),
    (90, 'yaRC-Server_3m'),
    (180, 'yaRC-Server_6m'),
    (365, 'yaRC-Server_1y'),
])
def test_issue_rc_server_certificates(crt_client, users, desired_ttl_days_templates):
    desired_ttl_days, template = desired_ttl_days_templates

    response = request_test_certificate(
        crt_client,
        'rc_server_user',
        CERT_TYPE.RC_SERVER,
        'sas-1.search.yandex.net',
        desired_ttl_days,
    )

    assert response.status_code == status.HTTP_201_CREATED
    assert response.json()['used_template'] == template
    serial_number = response.json()['serial_number']
    certificate = Certificate.objects.get(serial_number=serial_number)
    assert certificate.desired_ttl_days == desired_ttl_days


def test_issue_rc_server_certificate_with_invalid_desired_ttl_days(crt_client, users):
    desired_ttl_days = 123

    response = request_test_certificate(
        crt_client,
        'rc_server_user',
        CERT_TYPE.RC_SERVER,
        'sas-1.search.yandex.net',
        desired_ttl_days,
    )

    assert response.status_code == status.HTTP_400_BAD_REQUEST


@pytest.mark.parametrize('desired_ttl_days_templates', [
    (3, 'YaUser-PDAS3'),
    (5, 'YaUser-PDAS5'),
    (10, 'YaUser-PDAS10'),
    (30, 'YaUser-PDAS30'),
])
def test_issue_hypercube_certificates(crt_client, users, desired_ttl_days_templates, finn_ld_cert):
    desired_ttl_days, template = desired_ttl_days_templates

    response = request_test_certificate(
        crt_client,
        'hypercube_user',
        CERT_TYPE.HYPERCUBE,
        'finn@pda-ld.yandex.ru',
        desired_ttl_days,
    )

    assert response.status_code == status.HTTP_201_CREATED
    serial_number = response.json()['serial_number']
    certificate = Certificate.objects.get(serial_number=serial_number)
    assert certificate.desired_ttl_days == desired_ttl_days


@pytest.mark.parametrize('desired_ttl_days', [None, 11])
def test_issue_hypercube_certificate_with_invalid_desired_ttl_days(crt_client, users, desired_ttl_days):
    response = request_test_certificate(
        crt_client,
        'hypercube_user',
        CERT_TYPE.HYPERCUBE,
        'finn@pda-ld.yandex.ru',
        desired_ttl_days,
    )
    assert response.status_code == status.HTTP_400_BAD_REQUEST
