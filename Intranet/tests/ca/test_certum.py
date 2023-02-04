import pytest
from django.core import mail
from django.core.management import call_command
from django.utils import timezone
from django.utils.encoding import force_text
from mock import mock
from rest_framework import status

from intranet.crt.constants import CERT_TYPE, CA_NAME, CERT_STATUS, HOST_VALIDATION_CODE_STATUS
from intranet.crt.core.ca.certum import CertumZeepClient
from intranet.crt.core.ca.exceptions import CaError
from intranet.crt.core.models import HostToApprove, Certificate
from __tests__.utils.common import create_certificate, MockYavClient, attrdict, approve_cert, RecursiveCallableAttrDict
from intranet.crt.utils.test_ca import create_pem_certificate

pytestmark = pytest.skip()


@pytest.fixture()
def fake_client_init(monkeypatch):
    def fake_init(self):
        self.client = RecursiveCallableAttrDict()
    monkeypatch.setattr('intranet.crt.core.ca.certum.CertumZeepClient.__init__', fake_init)


class FakeClient(CertumZeepClient):
    days = 0

    @classmethod
    def create(cls, wsdl_url, username, password):
        return FakeClient(wsdl_url, username, password)

    def call(self, *args, **kwargs):
        raise NotImplementedError

    def order(self, *args, **kwargs):
        return attrdict({
            'orderID': '0xBAD1DEA',
            'SANVerification': attrdict({
                'approverMethod': 'DNS',
                'code': '0xDEADBEEF',
                'approverEmail': f'admin@{self.domain}',
                'FQDNs': attrdict({
                    'FQDN': [
                        self.domain,
                    ]
                }),
            }),
        })

    def get_order(self, request_id):
        assert request_id == '0xBAD1DEA'
        cert = Certificate.objects.get(request_id=request_id)
        pem_cert = create_pem_certificate(cert.request, not_before=timezone.now()+timezone.timedelta(days=self.days))
        FakeClient.pem_cert = pem_cert

        return attrdict({
            'orderStatus': attrdict({
                'orderStatus': 'OK',
            }),
            'certificateDetails': attrdict({
                'X509Cert': force_text(pem_cert),
            })
        })


def FakeRevokeClient(error_code=None):
    if error_code is not None:
        errors = [attrdict({'errorCode': error_code})]
    else:
        errors = []

    class SpecifiedFakeRevokeClient(CertumZeepClient):
        default_error_code = error_code

        def __init__(self):
            pass

        @classmethod
        def create(cls, wsdl_url, username, password):
            return FakeRevokeClient(cls.default_error_code)()

        def revoke_certificate(self, serial_number):
            self.revoke_call('revokeCertificate')

        def _call(self, *args, **kwargs):
            return attrdict({
                'responseHeader': attrdict({
                    'successCode': 1 if error_code is not None else 0,
                    'errors': attrdict({'Error': errors, 'sometrash': 'sometrash'}),
                }),
            })

    return SpecifiedFakeRevokeClient


@pytest.mark.parametrize('domain', ['example.com', 'example.co.il', 'example.yandex.ru'])
@pytest.mark.parametrize('should_approve', [True, False])
def test_certum_request(crt_client, users, crt_robot, domain, should_approve):
    requester = 'helpdesk_user'

    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.CERTUM_TEST_CA,
        'hosts': domain,
    }

    HostToApprove.objects.all().delete()
    # чтобы обойти кастомный save
    HostToApprove.objects.bulk_create([
        HostToApprove(
            auto_managed=True,
            managed_dns=True,
            host=domain
        )
    ])

    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    with mock.patch('intranet.crt.api.base.serializer_mixins.need_approve.create_st_issue_for_cert_approval') as mocked:
        mocked.return_value = attrdict({'key': 'SECTASK-666'})
        response = crt_client.json.post('/api/certificate/', data=request_data)

    assert response.status_code == status.HTTP_201_CREATED

    cert = Certificate.objects.get()
    assert cert.status == CERT_STATUS.NEED_APPROVE

    # подтвердим запрос
    approve_cert(crt_client, should_approve)
    cert.refresh_from_db()
    if should_approve is False:
        assert cert.status == CERT_STATUS.REJECTED
        return
    assert cert.status == CERT_STATUS.REQUESTED

    with mock.patch('intranet.crt.core.ca.certum.CertumZeepClient', FakeClient) as mocked:
        if domain == 'example.yandex.ru':
            mocked.domain = 'yandex.ru'
        else:
            mocked.domain = domain
        with mock.patch('intranet.crt.core.models.get_name_servers') as get_name_servers:
            get_name_servers.return_value = {'ns1.yandex.ru'}
            with mock.patch('intranet.crt.core.models.name_servers_are_managed') as name_servers_are_managed:
                name_servers_are_managed.return_value = True
                call_command('issue_certificates')
    if domain == 'example.yandex.ru':
        assert HostToApprove.objects.count() == 2
        hta = HostToApprove.objects.get(host='yandex.ru')
    else:
        hta = HostToApprove.objects.get()
    assert hta.certificates.count() == 1
    assert hta.certificates.get() == cert
    validation_code = hta.validation_codes.get(status=HOST_VALIDATION_CODE_STATUS.validation)
    assert validation_code.code == '0xDEADBEEF'

    cert.refresh_from_db()
    assert cert.request_id == '0xBAD1DEA'
    assert cert.status == CERT_STATUS.VALIDATION
    assert len(mail.outbox) == 0

    # Сертификат все равно еще на валидации, т.к. not_before больше текущей даты
    with mock.patch('intranet.crt.core.ca.certum.CertumZeepClient', FakeClient) as mocked:
        mocked.days = 1
        call_command('issue_certificates')
    cert.refresh_from_db()
    assert cert.request_id == '0xBAD1DEA'
    assert cert.status == CERT_STATUS.VALIDATION
    assert len(mail.outbox) == 0

    with mock.patch('intranet.crt.core.ca.certum.CertumZeepClient', FakeClient) as mocked:
        mocked.days = 0
        call_command('issue_certificates')
    cert.refresh_from_db()
    assert cert.status == CERT_STATUS.ISSUED
    assert cert.certificate == force_text(FakeClient.pem_cert)

    # Заказано не по csr, письмо не отправляем
    assert len(mail.outbox) == 0

    with mock.patch('intranet.crt.core.models.get_yav_client') as get_client:
        get_client.return_value = MockYavClient()
        call_command('save_private_keys_to_yav')

    # Письмо отправим после записи приватного ключа в секретницу
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.subject == 'Сертификат готов (helpdesk_user@yandex-team.ru)'


@pytest.mark.parametrize('error_code', [1185, 1033, 1151, 1145])
def test_certum_revoke_request(users, certificate_types, error_code):
    normal_user = users['normal_user']
    cert = create_certificate(
        normal_user,
        certificate_types['host'],
        hosts=['yandex.ru'],
    )
    cert.ca_name = CA_NAME.CERTUM_TEST_CA
    cert.request_id = '1234'
    cert.serial_number = '12345'
    cert.save()
    assert cert.status == CERT_STATUS.ISSUED

    with mock.patch('intranet.crt.core.ca.certum.CertumZeepClient', FakeRevokeClient(error_code)):
        cert.controller.revoke(normal_user)

    cert.refresh_from_db()
    assert cert.status == CERT_STATUS.REVOKED
    assert cert.revoked_by == normal_user


def test_certum_cancel_order(users, certificate_types):
    normal_user = users['normal_user']
    cert = create_certificate(
        normal_user,
        certificate_types['host'],
        hosts=['yandex.ru'],
    )
    cert.ca_name = CA_NAME.CERTUM_TEST_CA
    cert.request_id = 'somereq-id-1'
    cert.serial_number = None
    cert.status = CERT_STATUS.VALIDATION
    cert.save()

    with mock.patch('intranet.crt.core.ca.certum.CertumZeepClient', mock.Mock) as mocked:
        mocked.create = mock.PropertyMock()
        mocked.cancel_order = mock.PropertyMock()
        mocked.revoke_certificate = mock.PropertyMock()

        cert.controller.revoke(normal_user)

        mocked.cancel_order.assert_called_with(cert.request_id)
        mocked.revoke_certificate.assert_not_called()

    cert.refresh_from_db()
    assert cert.status == CERT_STATUS.REVOKED
    assert cert.revoked_by == normal_user


def test_certum_revoke_bad_request(users, certificate_types):
    normal_user = users['normal_user']
    cert = create_certificate(
        normal_user,
        certificate_types['host'],
        hosts=['yandex.ru'],
    )
    cert.ca_name = CA_NAME.CERTUM_TEST_CA
    cert.request_id = '1234'
    cert.serial_number = '12345'
    cert.save()
    assert cert.status == CERT_STATUS.ISSUED

    with mock.patch('intranet.crt.core.ca.certum.CertumZeepClient', FakeRevokeClient(100500)):
        with pytest.raises(CaError):
            cert.controller.revoke(normal_user)

    cert.refresh_from_db()
    assert cert.status == CERT_STATUS.ISSUED


def test_order_ecc_certificate(users, certificate_types, fake_client_init):
    non_ecc_reqeust = '''-----BEGIN CERTIFICATE REQUEST-----
MIIDJTCCAg0CAQAwgawxJDAiBgNVBAMTG2NlcnRpZmljYXRvci55YW5kZXgtdGVh
bS5ydTEmMCQGCSqGSIb3DQEJARYXc2VjdXJpdHlAeWFuZGV4LXRlYW0ucnUxCzAJ
BgNVBAYTAlJVMRswGQYDVQQIExJSdXNzaWFuIEZlZGVyYXRpb24xDzANBgNVBAcT
Bk1vc2NvdzETMBEGA1UEChMKWWFuZGV4IExMQzEMMAoGA1UECxMDSVRPMIIBIjAN
BgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAw51fp5hQR1aPGueQWug732l1TL3A
tFuIUdRhasrajqMQpaNqKjd0+e/ILX+G/UHbsDTg3jDcRetFR7BfkEGqadH2hRYg
tPU5zYIj3Asy90w+zFqI2YFI2NljIKgo7duQCXpbtexuA2CpM8udIYj/zeWhbyUD
rr4y6Sd5/Gor73ErJ2/fkr9g7KCoCAHIJ6NwyxkVn7sXchZDOzxfAoX2XcQZsstI
LRtINKhrHMz6B5X3qziBaoSgH0dWNqT1Vppmh9ugJWXYKwnKQDREDXOPzE4o+DWd
M8RKKZb7hBJQWZnuyE48W9MYC9C7+P64Qqi6j1VQaDe6T7q9qnI38p17GwIDAQAB
oDMwMQYJKoZIhvcNAQkOMSQwIjALBgNVHQ8EBAMCBaAwEwYDVR0lBAwwCgYIKwYB
BQUHAwEwDQYJKoZIhvcNAQEFBQADggEBACM+qBKXf4UoXE7FUFV/TMPfOhF63OGO
ndLi/O0EGkqxdyqtRV0Z8Sx3ZI2EwTgaoAhfwFeuWgE2rXOD/WGlK0/BqkAA5S0a
xYqEC939OSd3m69aYZxXfbduRH8qMDe3vqEpO2XLfxLVOgQD2LYAkm2cMSCDzwg6
I+9nHJKbzNcL3h/SnMh3NI891g7X5U+RLyE6auYr99dSq7wXl3/J4pchz6YP0nov
c30t57OkoHfZyVo27iR+Q9JQfIXnR7Wq4ovXzdNd4757kNmYyqrUmYYpot+f9Uel
m2zkG357YAF4nIkVjyc4FaTPz335l1888PKRG14sapSRlsFAGNGtQTA=
-----END CERTIFICATE REQUEST-----'''
    ecc_request = '''-----BEGIN CERTIFICATE REQUEST-----
MIIB9zCCAZ0CAQAwgZwxCzAJBgNVBAYTAlJVMQ8wDQYDVQQIEwZSdXNzaWExDzAN
BgNVBAcTBk1vc2NvdzETMBEGA1UEChMKWWFuZGV4IExMQzEMMAoGA1UECxMDSVRP
MSUwIwYDVQQDExxyZXBvcnQuYXBwbWV0cmljYS55YW5kZXgubmV0MSEwHwYJKoZI
hvcNAQkBFhJwa2lAeWFuZGV4LXRlYW0ucnUwWTATBgcqhkjOPQIBBggqhkjOPQMB
BwNCAATiEgVYByHVIZU8D14L29BAjx6A3i90TV2hH1coZidQdO/hnQ6qNztFbmWF
10XUanMB4GwxS+gbiM8hc1JEV2bhoIGdMIGaBgkqhkiG9w0BCQ4xgYwwgYkwCQYD
VR0TBAIwADALBgNVHQ8EBAMCBeAwbwYDVR0RBGgwZoIccmVwb3J0LmFwcG1ldHJp
Y2EueWFuZGV4Lm5ldIIlcmVwb3J0LXBhcnRuZXJzLmFwcG1ldHJpY2EueWFuZGV4
Lm5ldIIfcm9zZW5iZXJnLmFwcG1ldHJpY2EueWFuZGV4Lm5ldDAKBggqhkjOPQQD
AgNIADBFAiEAjNanDKUKfXdScKCEAzSuUC4wHrKy+1lXAcdMA3fIwhcCICkyLmsD
UYLbLJmibPrMa5pkYii9hYHu77F+8Cszxpwl
-----END CERTIFICATE REQUEST-----'''

    assert CertumZeepClient().create_order_parameters(non_ecc_reqeust, None, None).get('hashAlgorithm') is None
    assert CertumZeepClient().create_order_parameters(ecc_request, None, None).get('hashAlgorithm') == 'ECC-SHA256'


def test_tld_verification_skipped(crt_client, users):
    HostToApprove.objects.bulk_create([
        HostToApprove(
            auto_managed=True,
            managed_dns=True,
            host='yandex.com.ru'
        )
    ])
    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.CERTUM_TEST_CA,
        'hosts': 'yandex.com.ru, *.yandex.com.ru',
    }
    crt_client.login('helpdesk_user')

    with mock.patch(
        'intranet.crt.api.base.serializer_mixins.need_approve.create_st_issue_for_cert_approval'
    ) as mocked_st:
        mocked_st.return_value = attrdict({'key': 'SECTASK-123'})
        crt_client.json.post('/api/certificate/', data=request_data)

    cert = Certificate.objects.get()
    approve_cert(crt_client, True)

    with mock.patch('intranet.crt.core.ca.certum.CertumZeepClient', FakeClient) as mocked_client:
        def fake_order(*args, **kwargs):
            return attrdict({
                'orderID': '0xBAD1DEA',
                'SANVerification': attrdict({
                    'approverMethod': 'DNS',
                    'code': '0xDEADBEEF',
                    'approverEmail': 'admin@com.ru',
                    'FQDNs': attrdict({
                        'FQDN': [
                            'yandex.com.ru',
                            'com.ru'
                        ]
                    }),
                }),
            })

        mocked_client.order = fake_order
        cert.controller.ca._send_request(cert)

    assert HostToApprove.objects.filter(host='yandex.com.ru').exists()
    assert not HostToApprove.objects.filter(host='com.ru').exists()
