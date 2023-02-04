from unittest import mock

import pytest

from django.utils import timezone
from django.utils.encoding import force_text
from rest_framework import status

from intranet.crt.core.ca.internal import InternalCA
from intranet.crt.core.ca.registry import CA_REGISTRY
from intranet.crt.core.models import Certificate
from intranet.crt.constants import CERT_TYPE, CA_NAME, CERT_TEMPLATE, CERT_STATUS
from intranet.crt.csr import HostCsrConfig
from __tests__.utils.common import capture_raw_http, create_certificate, ResponseMock, assert_contains
from intranet.crt.utils.test_ca import create_pem_certificate

pytestmark = pytest.mark.django_db

sans = ['san1.yandex-team.ru', 'san2.yandex-team.ru']
csr_content = {
    'common_name': 'san1.yandex-team.ru',
    'email': 'helpdesk_user@ld.yandex.ru',
    'sans': sans,
}

csr = force_text(HostCsrConfig(**csr_content).get_csr())


def dated_content(method, url, **kwargs):
    if url.endswith('/certsrv/certfnsh.asp'):
        assert method == 'POST'
        assert 'data' in kwargs
        assert kwargs['data'].get('Mode') == 'newreq'
        assert kwargs['data'].get('CertAttrib') == 'CertificateTemplate:{}'.format(CERT_TEMPLATE.WEB_SERVER)
        assert kwargs['data'].get('CertRequest') == csr

        return ResponseMock('FILLER certnew.cer?ReqID=123&amp;Enc=b64 FILLER', url=url)

    if url.endswith('certnew.cer?ReqID=123&Enc=b64'):
        assert method == 'GET'

        certificate = create_pem_certificate(csr)
        return ResponseMock(certificate)

    raise AssertionError('Invalid url {}'.format(url))


def test_request_host_cert(crt_client, users):
    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.INTERNAL_TEST_CA,
        'request': csr,
        'hosts': ','.join(sans),
    }

    crt_client.login('normal_user')
    with capture_raw_http(side_effect=dated_content):
        response = crt_client.json.post('/api/certificate/', data=request_data)

    assert response.status_code == status.HTTP_201_CREATED
    data = response.json()
    cert = Certificate.objects.get(serial_number=data['serial_number'])
    assert cert.requested_by_csr is True
    assert data['common_name'] == 'san1.yandex-team.ru'
    assert data['hosts'] == ['san1.yandex-team.ru', 'san2.yandex-team.ru']


def test_request_host_cert_wrong_csr(crt_client, users):
    bad_csr = HostCsrConfig(
        common_name='host.yandex-team.ru',
        email='mail@yandex-team.ru',
        sans=['other_host.yandex-team.ru']
    ).get_csr()

    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.INTERNAL_TEST_CA,
        'request': force_text(bad_csr),
    }
    response = crt_client.json.post('/api/certificate/', data=request_data)
    assert response.status_code == 400
    assert response.json()['non_field_errors'] == ['Common Name must be included in Certificate Subject Alternative Names (hosts)']


def test_revoke_imported_cert(crt_client, users, crt_robot, certificate_types):
    internal_ca = InternalCA(**CA_REGISTRY[CA_NAME.INTERNAL_CA]['kwargs'])

    cert = create_certificate(
        crt_robot, certificate_types[CERT_TYPE.PC], status=CERT_STATUS.ISSUED,
        end_date=timezone.now() + timezone.timedelta(days=1), serial_number='1', is_imported=True
    )
    with mock.patch('intranet.crt.core.ca.internal.InternalCA.revoke_action') as m:
        m.return_value = None
        internal_ca.revoke(cert)
        assert m.call_args_list == [mock.call(cert, 'CRL_REASON_CESSATION_OF_OPERATION')]

    with mock.patch('intranet.crt.core.ca.internal.InternalCA.revoke_action') as m:
        m.return_value = None
        cert.certificate = ''
        cert.is_imported = True
        internal_ca.revoke(cert)
        assert m.call_args_list == [mock.call(cert, 'CRL_REASON_CESSATION_OF_OPERATION')]

    with mock.patch('intranet.crt.core.ca.internal.InternalCA.revoke_action') as m:
        m.return_value = None
        cert.is_imported = False
        internal_ca.revoke(cert)
        assert m.call_count == 0


def test_request_host_cert_external_domain(crt_client, users):
    ext_hosts = ['*.yndx.net', 'san1.yandex-team.ru', 'san1.extdomain.ru', 'tyandex.net']
    csr_content = {
        'common_name': 'san1.yandex-team.ru',
        'email': 'helpdesk_user@ld.yandex.ru',
        'sans': ext_hosts,
    }

    csr = HostCsrConfig(**csr_content).get_csr()

    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.INTERNAL_TEST_CA,
        'request': force_text(csr),
        'hosts': ','.join(ext_hosts),
    }

    crt_client.login('normal_user')
    response = crt_client.json.post('/api/certificate/', data=request_data)

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    response = response.json()
    assert_contains(response['non_field_errors'][0],
                    ['Хосты: san1.extdomain.ru, tyandex.net являются внешними и не могут быть выпущены этим УЦ ',
                     'автоматически. Обратитесь за дополнительной информацией через ',
                     'форму https://wiki.yandex-team.ru/security/ssl/'])
