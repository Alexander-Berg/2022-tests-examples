import pytest
import mock
from waffle.testutils import override_switch

from django.core.management import call_command
from django.http import HttpResponseNotFound
from rest_framework import status

from intranet.crt.constants import AFFILIATION, CERT_TYPE, CA_NAME, CERT_STATUS
from intranet.crt.core.ca.exceptions import CaError, ValidationCaError
from intranet.crt.core.models import Certificate
from __tests__.utils.ssl import get_modulus_from_pem, build_tpm_smartcard_1c_csr
from __tests__.utils.common import attrdict, approve_cert, create_certificate


pytestmark = pytest.mark.django_db


RC_FIELDLIST = {
    'serial_number', 'type', 'status', 'id', 'held_by', 'unheld_by', 'builtin_tags',
    'issued', 'revoked', 'revoked_by', 'updated', 'added', 'end_date', 'desired_ttl_days',
    'username', 'requester',
    'common_name', 'download', 'download2',
    'device_platform', 'tags', 'manual_tags',
    'priv_key_deleted_at', 'error_message', 'url', 'ca_name', 'request_id',
    'certificate', 'used_template', 'is_ecc',
    'yav_secret_id', 'is_reissue', 'uploaded_to_yav', 'yav_secret_version', 'requested_by_csr',
}

PC_FIELDLIST = RC_FIELDLIST | {
    'pc_inum', 'pc_os', 'pc_hostname', 'request', 'pc_serial_number', 'pc_mac', 'helpdesk_ticket'
}


@pytest.fixture
def rc_cert(crt_client, users, settings):
    settings.CRT_REISSUE_RC_THRESHOLD_DAYS = 500
    requester = 'rc_server_user'

    crt_client.login(requester)

    request_data = {
        'type': CERT_TYPE.RC_SERVER,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': 'xxx.search.yandex.net',
    }

    response = crt_client.json.post('/api/certificate/', data=request_data)
    assert response.status_code == 201
    cert = Certificate.objects.get()
    assert cert.type.name == CERT_TYPE.RC_SERVER
    return cert


def test_reissue_rc_server(crt_client, users, settings, rc_cert):
    response = crt_client.json.post(
        '/api/reissue/',
        HTTP_X_SSL_CLIENT_SERIAL=rc_cert.serial_number,
        HTTP_X_SSL_CLIENT_VERIFY='0',
        HTTP_X_SSL_CLIENT_SUBJECT='xxx.search.yandex.net',
    )
    assert response.status_code == 201
    data = response.json()
    assert set(data.keys()) == RC_FIELDLIST

    serial_number = data['serial_number']
    common_name = data['common_name']
    assert common_name == 'xxx.search.yandex.net'

    new_cert = Certificate.objects.get(serial_number=serial_number)
    assert new_cert.pk != rc_cert.pk
    assert new_cert.status == 'issued'
    assert new_cert.is_reissue is True
    assert new_cert.requested_by_csr is False

    rc_cert.refresh_from_db()
    # Old certificate is not revoked, we wait until expiration
    assert rc_cert.status == 'issued'


@pytest.mark.parametrize('cert_type', (CERT_TYPE.PC, CERT_TYPE.BANK_PC))
def test_reissue_pc_cert(crt_client, users, pc_csrs, settings, cert_type):
    settings.CRT_REISSUE_PC_THRESHOLD_DAYS = 500
    requester = 'helpdesk_user'
    requestee = 'normal_user'
    crt_client.login(requester)

    request_data = {
        'type': cert_type,
        'ca_name': CA_NAME.TEST_CA,
        'pc_os': 'Mac OS X 10.12',
        'pc_hostname': 'mac_user02',
        'pc_serial_number': '1111',
        'pc_mac': '111',
        'request': pc_csrs[requestee],
        'pc_inum': '111',
    }

    response = crt_client.json.post('/api/certificate/', data=request_data)
    assert response.status_code == 201
    cert_data = response.json()
    serial_number = cert_data['serial_number']

    crt_client.logout()
    cert = Certificate.objects.get()
    assert cert.type.name == cert_type

    csr_subject = (
        '/C=RU/ST=Moscow/L=Moscow/O=Yandex/OU=Infra/CN=normal_user@ld.yandex.ru'
        '/emailAddress=normal_user@yandex-team.ru'
    )

    kwargs = {
        'HTTP_X_SSL_CLIENT_SERIAL': serial_number,
        'HTTP_X_SSL_CLIENT_VERIFY': '0',
        'HTTP_X_SSL_CLIENT_SUBJECT': csr_subject,
        'data': {
            'request': pc_csrs[requestee],
        }
    }

    response = crt_client.json.post('/api/reissue/', **kwargs)
    assert response.status_code == 201
    data = response.json()
    assert set(data.keys()) == PC_FIELDLIST

    serial_number = data['serial_number']
    common_name = data['common_name']
    assert common_name == '%s@ld.yandex.ru' % requestee

    new_cert = Certificate.objects.get(serial_number=serial_number)
    assert new_cert.pk != cert.pk
    assert new_cert.status == 'issued'
    assert new_cert.is_reissue is True
    assert new_cert.requested_by_csr is True
    assert new_cert.user.username == requestee
    assert new_cert.requester.username == requestee

    cert.refresh_from_db()
    # Old certificate is not revoked, we wait until expiration
    assert cert.status == 'issued'


@pytest.mark.parametrize('cert_type', (CERT_TYPE.PC, CERT_TYPE.BANK_PC))
@mock.patch('intranet.crt.api.base.serializer_mixins.reissue.get_inums_and_models')
@pytest.mark.parametrize('updated_field', (
    'pc_os', 'pc_hostname', 'pc_serial_number', 'pc_mac', 'pc_inum', 'helpdesk_ticket',
))
def test_reissue_pc_cert_with_new_pc_fields(
    mocked_bot, crt_client, users, pc_csrs, settings, updated_field, cert_type,
):
    settings.CRT_REISSUE_PC_THRESHOLD_DAYS = 500
    requester = 'helpdesk_user'
    requestee = 'normal_user'
    crt_client.login(requester)
    mocked_bot.return_value = [('updated', 'Notebook')]

    request_data = {
        'pc_os': 'orig',
        'pc_hostname': 'orig',
        'pc_serial_number': 'orig',
        'pc_mac': 'orig',
        'pc_inum': 'orig',
        'helpdesk_ticket': 'orig',
        'type': cert_type,
        'ca_name': CA_NAME.TEST_CA,
        'request': pc_csrs[requestee],
    }

    response = crt_client.json.post('/api/certificate/', data=request_data)
    assert response.status_code == 201
    cert_data = response.json()
    serial_number = cert_data['serial_number']
    csr_subject = (
        '/C=RU/ST=Moscow/L=Moscow/O=Yandex/OU=Infra/CN=normal_user@ld.yandex.ru'
        '/emailAddress=normal_user@yandex-team.ru'
    )

    kwargs = {
        'HTTP_X_SSL_CLIENT_SERIAL': serial_number,
        'HTTP_X_SSL_CLIENT_VERIFY': '0',
        'HTTP_X_SSL_CLIENT_SUBJECT': csr_subject,
        'data': {
            'request': pc_csrs[requestee],
            updated_field: 'updated',
        }
    }

    response = crt_client.json.post('/api/reissue/', **kwargs)
    assert response.status_code == 201
    expected = {
        'pc_os': 'orig',
        'pc_hostname': 'orig',
        'pc_serial_number': 'orig',
        'pc_mac': 'orig',
        'pc_inum': 'orig',
        'helpdesk_ticket': 'orig',
    }
    expected.update({updated_field: 'updated'})

    cert = Certificate.objects.last()

    assert cert.pc_os == expected['pc_os']
    assert cert.pc_hostname == expected['pc_hostname']
    assert cert.pc_serial_number == expected['pc_serial_number']
    assert cert.pc_mac == expected['pc_mac']
    assert cert.pc_inum == expected['pc_inum']
    assert cert.helpdesk_ticket == expected['helpdesk_ticket']


def test_reissue_unknown_certificate(crt_client, users, settings):
    settings.CRT_REISSUE_RC_THRESHOLD_DAYS = 500
    crt_client.login('helpdesk_user')
    response = crt_client.json.post(
        '/api/reissue/',
        HTTP_X_SSL_CLIENT_SERIAL=1000000000,
        HTTP_X_SSL_CLIENT_VERIFY='0',
        HTTP_X_SSL_CLIENT_SUBJECT='xxx.search.yandex.net',
    )
    assert response.status_code == 400
    assert response.json() == {
        'detail': 'Client certificate with given serial number not found (1000000000)',
    }


def test_reissue_fail(crt_client, users, pc_csrs, settings, monkeypatch):
    settings.CRT_REISSUE_PC_THRESHOLD_DAYS = 500
    requester = 'helpdesk_user'
    requestee = 'normal_user'
    crt_client.login(requester)

    request_data = {
        'type': CERT_TYPE.PC,
        'ca_name': CA_NAME.TEST_CA,
        'pc_os': 'Mac OS X 10.12',
        'pc_hostname': 'mac_user02',
        'pc_serial_number': '1111',
        'pc_mac': '111',
        'request': pc_csrs[requestee],
        'pc_inum': '111',
    }

    response = crt_client.json.post('/api/certificate/', data=request_data)
    assert response.status_code == 201
    cert_data = response.json()
    serial_number = cert_data['serial_number']

    crt_client.logout()
    cert = Certificate.objects.get()
    assert cert.type.name == CERT_TYPE.PC

    csr_subject = (
        '/C=RU/ST=Moscow/L=Moscow/O=Yandex/OU=Infra/CN=normal_user@ld.yandex.ru'
        '/emailAddress=normal_user@yandex-team.ru'
    )

    kwargs = {
        'HTTP_X_SSL_CLIENT_SERIAL': serial_number,
        'HTTP_X_SSL_CLIENT_VERIFY': '0',
        'HTTP_X_SSL_CLIENT_SUBJECT': csr_subject,
        'data': {
            'request': pc_csrs[requestee],
        }
    }

    with mock.patch('intranet.crt.core.ca.test.TestCA._issue') as patched_issue:
        patched_issue.side_effect = CaError()
        response = crt_client.json.post('/api/reissue/', **kwargs)
        assert response.status_code == 500


@pytest.mark.parametrize('use_force', [True, False])
def test_force_reissue_rc_certificate(crt_client, users, settings, use_force):
    settings.CRT_REISSUE_RC_THRESHOLD_DAYS = 0
    requester = 'rc_server_user'

    crt_client.login(requester)

    request_data = {
        'type': CERT_TYPE.RC_SERVER,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': 'xxx.search.yandex.net',
    }

    response = crt_client.json.post('/api/certificate/', data=request_data)
    assert response.status_code == status.HTTP_201_CREATED
    cert_data = response.json()
    serial_number = cert_data['serial_number']

    crt_client.logout()
    cert = Certificate.objects.get()
    assert cert.type.name == CERT_TYPE.RC_SERVER

    reissue_data = {
        'force': use_force,
    }

    response = crt_client.json.post(
        '/api/reissue/',
        data=reissue_data,
        HTTP_X_SSL_CLIENT_SERIAL=serial_number,
        HTTP_X_SSL_CLIENT_VERIFY='0',
        HTTP_X_SSL_CLIENT_SUBJECT='xxx.search.yandex.net',
    )

    expected_status_code = status.HTTP_201_CREATED if use_force else status.HTTP_400_BAD_REQUEST
    assert response.status_code == expected_status_code

    if not use_force:
        assert 'Current certificate is not expiring' in response.json().get('detail', '')
        return

    data = response.json()
    assert set(data.keys()) == RC_FIELDLIST

    serial_number = data['serial_number']
    common_name = data['common_name']
    assert common_name == 'xxx.search.yandex.net'

    new_cert = Certificate.objects.get(serial_number=serial_number)
    assert new_cert.pk != cert.pk
    assert new_cert.status == 'issued'

    assert new_cert.is_reissue is True
    assert new_cert.requested_by_csr is False

    cert.refresh_from_db()
    # Old certificate is not revoked, we wait until expiration
    assert cert.status == 'issued'


@pytest.mark.parametrize('user_has_hw', (True, False))
@pytest.mark.parametrize('affiliation', AFFILIATION.CHOICES)
@mock.patch('intranet.crt.users.models.old_get_inums_and_models')
def test_reissue_vpn_token_certificate(mocked_bot, crt_client, users, settings, pc_csrs, affiliation, user_has_hw):
    helpdesk_user = users['helpdesk_user']
    external_user = users['external_user']
    csr_string = pc_csrs[external_user.username]

    mocked_bot.return_value = []
    crt_client.login(helpdesk_user)

    request_data = {
        'type': CERT_TYPE.VPN_TOKEN,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': 'external_user@ld.yandex.ru',
        'request': csr_string,
    }
    response = crt_client.json.post('/api/certificate/', data=request_data)

    assert response.status_code == 201
    crt_client.logout()

    mocked_bot.return_value = [('618', 'Notebook')] if user_has_hw else []
    external_user.affiliation = affiliation[0]
    external_user.save()

    old_cert = Certificate.objects.get()
    csr_subject = (
        '/C=RU/ST=Moscow/L=Moscow/O=Yandex/OU=Infra/CN=external_user@ld.yandex.ru'
        '/emailAddress=normal_user@yandex-team.ru'
    )
    request_data = {
        'HTTP_X_SSL_CLIENT_SERIAL': old_cert.serial_number,
        'HTTP_X_SSL_CLIENT_VERIFY': '0',
        'HTTP_X_SSL_CLIENT_SUBJECT': csr_subject,
        'data': {
            'request': csr_string,
        }
    }
    response = crt_client.json.post('/api/reissue/', **request_data)

    assert response.status_code == 400
    assert response.json()['detail'] == 'vpn-token certificates can only be reissued with old CSR reusing'

    request_data['data'] = {
        'reuse_csr': True,
    }
    response = crt_client.json.post('/api/reissue/', **request_data)

    assert response.status_code == 400
    assert response.json()['detail'] == ('Current certificate is not expiring. Threshold is {} days'
                                         .format(settings.CRT_REISSUE_VPN_THRESHOLD_DAYS))
    request_data['data'] = {
        'reuse_csr': True,
        'force': True,
    }
    response = crt_client.json.post('/api/reissue/', **request_data)

    if external_user.affiliation == AFFILIATION.YANDEX:
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        assert response.json() == ['vpn-token certificates is only for external users.']
        return

    if user_has_hw:
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        assert response.json() == ['vpn-token certificates is only for users without BOT hardware.']
        return

    assert response.status_code == 201

    cert1, cert2 = Certificate.objects.order_by('pk')
    assert csr_string.strip() == cert1.request == cert2.request
    assert cert2.is_reissue is True
    assert cert2.requested_by_csr is True

    req_modulus = get_modulus_from_pem(csr_string)
    cert1_modulus = get_modulus_from_pem(cert1.certificate)
    cert2_modulus = get_modulus_from_pem(cert2.certificate)

    assert req_modulus == cert1_modulus == cert2_modulus

    assert cert1.revoke_at is not None
    assert cert2.revoke_at is None


@pytest.mark.parametrize('server_client_approving', [True, False])
def test_reissue_client_server_certificate(crt_client, users, settings, client_server_csrs, server_client_approving):
    noc_user = users['noc_user']
    csr_string, ya_csr_string = client_server_csrs
    request_data = {
        'type': CERT_TYPE.CLIENT_SERVER,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': 'client-server.yndx.net',
        'request': csr_string,
    }
    with override_switch('client_server_should_be_approved', active=server_client_approving):
        with mock.patch('intranet.crt.api.base.serializer_mixins.need_approve.create_st_issue_for_cert_approval') as mocked:
            crt_client.login(noc_user)
            mocked.return_value = attrdict({'key': 'SECTASK-666'})
            response = crt_client.json.post('/api/certificate/', data=request_data)
        assert response.status_code == 201
        old_cert = Certificate.objects.get()
        if server_client_approving:
            assert old_cert.status == CERT_STATUS.NEED_APPROVE
            approve_cert(crt_client)
            old_cert.refresh_from_db()
            crt_client.login(noc_user)
            response = crt_client.json.get('/api/frontend/certificate/{}/'.format(old_cert.pk))
            assert response.json()['approve_request'] == old_cert.approve_request.id
            assert old_cert.status == CERT_STATUS.REQUESTED
        call_command('issue_certificates')
        old_cert.refresh_from_db()
    assert old_cert.status == CERT_STATUS.ISSUED

    csr_subject = (
        '/C=RU/ST=Moscow/L=Moscow/O=Yandex/OU=Infra/CN=client-server.yndx.net'
        '/emailAddress=normal_user@yandex-team.ru'
    )
    request_data = {
        'HTTP_X_SSL_CLIENT_SERIAL': old_cert.serial_number,
        'HTTP_X_SSL_CLIENT_VERIFY': '0',
        'HTTP_X_SSL_CLIENT_SUBJECT': csr_subject,
        'data': {
            'reuse_csr': True,
        }
    }
    response = crt_client.json.post('/api/reissue/', **request_data)

    assert response.status_code == 400
    assert response.json()['detail'] == ('Current certificate is not expiring. Threshold is {} days'
                                         .format(settings.CRT_REISSUE_CLIENT_SERVER_THRESHOLD_DAYS))

    request_data['data'] = {
        'reuse_csr': True,
        'force': True,
    }
    # cert2
    response = crt_client.json.post('/api/reissue/', **request_data)

    assert response.status_code == 201

    request_data['data'] = {
        'force': True,
        'reuse_csr': True,         # реюзаем, если указано,
        'request': ya_csr_string,  # 'request' может присутствовать, но не смотрим
    }
    # cert3
    response = crt_client.json.post('/api/reissue/', **request_data)

    assert response.status_code == 201

    request_data['data'] = {
        'force': True,
        'request': ya_csr_string,
    }
    # cert4
    response = crt_client.json.post('/api/reissue/', **request_data)

    assert response.status_code == 201

    cert1, cert2, cert3, cert4 = Certificate.objects.order_by('pk')
    assert csr_string.strip() == cert1.request == cert2.request == cert3.request != cert4.request

    req_modulus = get_modulus_from_pem(csr_string)

    cert1_modulus = get_modulus_from_pem(cert1.certificate)
    cert2_modulus = get_modulus_from_pem(cert2.certificate)
    cert3_modulus = get_modulus_from_pem(cert3.certificate)
    cert4_modulus = get_modulus_from_pem(cert4.certificate)

    assert req_modulus == cert1_modulus == cert2_modulus == cert3_modulus != cert4_modulus
    assert cert2.is_reissue and cert3.is_reissue and cert4.is_reissue
    assert cert2.requested_by_csr and cert3.requested_by_csr and cert4.requested_by_csr


@pytest.mark.parametrize('server_client_approving', [True, False])
def test_reissue_bank_client_server_certificate(crt_client, users, settings, bank_client_server_csrs,
                                                server_client_approving):
    helpdesk_user = users['helpdesk_user']
    csr_string, ya_csr_string = bank_client_server_csrs
    request_data = {
        'type': CERT_TYPE.BANK_CLIENT_SERVER,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': 'xxx.yandex-bank.net',
        'request': csr_string,
    }
    with override_switch('bank_client_server_should_be_approved', active=server_client_approving):
        with mock.patch('intranet.crt.api.base.serializer_mixins.need_approve.create_st_issue_for_cert_approval') as mocked:
            crt_client.login(helpdesk_user)
            mocked.return_value = attrdict({'key': 'SECTASK-666'})
            response = crt_client.json.post('/api/certificate/', data=request_data)
        assert response.status_code == 201
        old_cert = Certificate.objects.get()
        if server_client_approving:
            assert old_cert.status == CERT_STATUS.NEED_APPROVE
            approve_cert(crt_client)
            old_cert.refresh_from_db()
            crt_client.login(helpdesk_user)
            response = crt_client.json.get('/api/frontend/certificate/{}/'.format(old_cert.pk))
            assert response.json()['approve_request'] == old_cert.approve_request.id
            assert old_cert.status == CERT_STATUS.REQUESTED
        call_command('issue_certificates')
        old_cert.refresh_from_db()
    assert old_cert.status == CERT_STATUS.ISSUED

    csr_subject = (
        '/C=RU/ST=Moscow/L=Moscow/O=Yandex/OU=Infra/CN=xxx.yandex-bank.net'
        '/emailAddress=normal_user@yandex-team.ru'
    )
    request_data = {
        'HTTP_X_SSL_CLIENT_SERIAL': old_cert.serial_number,
        'HTTP_X_SSL_CLIENT_VERIFY': '0',
        'HTTP_X_SSL_CLIENT_SUBJECT': csr_subject,
        'data': {
            'reuse_csr': True,
        }
    }
    response = crt_client.json.post('/api/reissue/', **request_data)

    assert response.status_code == 400
    assert response.json()['detail'] == ('Current certificate is not expiring. Threshold is {} days'
                                         .format(settings.CRT_REISSUE_BANK_CLIENT_SERVER_THRESHOLD_DAYS))

    request_data['data'] = {
        'reuse_csr': True,
        'force': True,
    }
    # cert2
    response = crt_client.json.post('/api/reissue/', **request_data)

    assert response.status_code == 201

    request_data['data'] = {
        'force': True,
        'reuse_csr': True,         # реюзаем, если указано,
        'request': ya_csr_string,  # 'request' может присутствовать, но не смотрим
    }
    # cert3
    response = crt_client.json.post('/api/reissue/', **request_data)

    assert response.status_code == 201

    request_data['data'] = {
        'force': True,
        'request': ya_csr_string,
    }
    # cert4
    response = crt_client.json.post('/api/reissue/', **request_data)

    assert response.status_code == 201

    cert1, cert2, cert3, cert4 = Certificate.objects.order_by('pk')
    assert csr_string.strip() == cert1.request == cert2.request == cert3.request != cert4.request

    req_modulus = get_modulus_from_pem(csr_string)

    cert1_modulus = get_modulus_from_pem(cert1.certificate)
    cert2_modulus = get_modulus_from_pem(cert2.certificate)
    cert3_modulus = get_modulus_from_pem(cert3.certificate)
    cert4_modulus = get_modulus_from_pem(cert4.certificate)

    assert req_modulus == cert1_modulus == cert2_modulus == cert3_modulus != cert4_modulus
    assert cert2.is_reissue and cert3.is_reissue and cert4.is_reissue
    assert cert2.requested_by_csr and cert3.requested_by_csr and cert4.requested_by_csr


def test_desired_ttl_days(crt_client, rc_cert):
    response = crt_client.json.post(
        '/api/reissue/',
        {
            'desired_ttl_days': 18,
        },
        HTTP_X_SSL_CLIENT_SERIAL=rc_cert.serial_number,
        HTTP_X_SSL_CLIENT_VERIFY='0',
        HTTP_X_SSL_CLIENT_SUBJECT='xxx.search.yandex.net',
    )
    assert response.status_code == 201
    new_cert = Certificate.objects.get(serial_number=response.json()['serial_number'])
    assert new_cert.desired_ttl_days == 18


def test_desired_ttl_days_from_prev_cert(crt_client, rc_cert):
    rc_cert.desired_ttl_days = 1234
    rc_cert.save()
    response = crt_client.json.post(
        '/api/reissue/',
        HTTP_X_SSL_CLIENT_SERIAL=rc_cert.serial_number,
        HTTP_X_SSL_CLIENT_VERIFY='0',
        HTTP_X_SSL_CLIENT_SUBJECT='xxx.search.yandex.net',
    )
    assert response.status_code == 201
    new_cert = Certificate.objects.get(serial_number=response.json()['serial_number'])
    assert new_cert.desired_ttl_days == 1234


def test_desired_ttl_days_wrong_data(crt_client, rc_cert):
    rc_cert.desired_ttl_days = 1234
    rc_cert.save()
    response = crt_client.json.post(
        '/api/reissue/',
        {
            'desired_ttl_days': 'a',
        },
        HTTP_X_SSL_CLIENT_SERIAL=rc_cert.serial_number,
        HTTP_X_SSL_CLIENT_VERIFY='0',
        HTTP_X_SSL_CLIENT_SUBJECT='xxx.search.yandex.net',
    )
    assert response.status_code == 400
    assert response.json() == {'desired_ttl_days': ['A valid integer is required.']}


def test_issue_raises_CaError(crt_client, rc_cert):
    with mock.patch('intranet.crt.core.ca.test.TestCA._issue') as _issue:
        _issue.side_effect = ValidationCaError('message_from_exc')
        response = crt_client.json.post(
            '/api/reissue/',
            {
                'desired_ttl_days': 18,
            },
            HTTP_X_SSL_CLIENT_SERIAL=rc_cert.serial_number,
            HTTP_X_SSL_CLIENT_VERIFY='0',
            HTTP_X_SSL_CLIENT_SUBJECT='xxx.search.yandex.net',
        )
    assert response.status_code == 400
    assert response.json() == {'detail': 'message_from_exc'}


@pytest.mark.parametrize('reuse_csr', [True, False])
def test_reissue_tpm_smartcard_1c_certificate(mocked_ldap, crt_client, users, settings, reuse_csr):
    tpm_smartcard_1c_user = users['tpm_smartcard_1c_user']
    csr_string_1 = build_tpm_smartcard_1c_csr('Normal User')
    csr_string_2 = build_tpm_smartcard_1c_csr('Normal User')

    crt_client.login(tpm_smartcard_1c_user)

    request_data = {
        'type': CERT_TYPE.TPM_SMARTCARD_1C,
        'ca_name': CA_NAME.TEST_CA,
        'request': csr_string_1,
    }
    response = crt_client.json.post('/api/certificate/', data=request_data)

    assert response.status_code == 201

    crt_client.logout()

    old_cert = Certificate.objects.get()
    csr_subject = '/DC=ru/DC=yandex/DC=ld/CN=Users/CN=Normal User'
    request_data = {
        'HTTP_X_SSL_CLIENT_SERIAL': old_cert.serial_number,
        'HTTP_X_SSL_CLIENT_VERIFY': '0',
        'HTTP_X_SSL_CLIENT_SUBJECT': csr_subject,
        'data': {
            'request': csr_string_2,
            'reuse_csr': reuse_csr,
        }
    }
    response = crt_client.json.post('/api/reissue/', **request_data)

    assert response.status_code == 400
    assert response.json()['detail'] == ('Current certificate is not expiring. Threshold is {} days'
                                         .format(settings.CRT_REISSUE_TPM_SMARTCARD_1C_THRESHOLD_DAYS))

    request_data['data'].update({'force': True})
    response = crt_client.json.post('/api/reissue/', **request_data)

    assert response.status_code == 201

    cert1, cert2 = Certificate.objects.order_by('pk')

    assert csr_string_1.strip() == cert1.request
    assert cert2.is_reissue is True
    assert cert2.requested_by_csr is True

    assert (cert1.request == cert2.request) == reuse_csr

    csr1_req_modulus = get_modulus_from_pem(csr_string_1)
    csr2_req_modulus = get_modulus_from_pem(csr_string_2)
    cert1_modulus = get_modulus_from_pem(cert1.certificate)
    cert2_modulus = get_modulus_from_pem(cert2.certificate)

    assert csr1_req_modulus == cert1_modulus
    assert (cert1_modulus == cert2_modulus) == reuse_csr
    assert (csr2_req_modulus == cert2_modulus) != reuse_csr


def test_reissue_holds_certificates_with_same_pc_enum(crt_client, users, pc_csrs, settings, certificate_types):
    settings.CRT_REISSUE_PC_THRESHOLD_DAYS = 500
    normal_user = users['normal_user']
    another_user = users['another_user']

    for sn in range(3):
        create_certificate(
            normal_user,
            certificate_types['pc'],
            serial_number=str(sn),
            common_name='normal_user@ld.yandex.ru',
            pc_inum='same',
        )
    old_cert = Certificate.objects.last()
    create_certificate(another_user, certificate_types['linux-pc'], pc_inum='same')
    create_certificate(normal_user, certificate_types['pc'], pc_inum='another')

    for cert in Certificate.objects.all():
        assert cert.revoke_at is None

    csr_subject = (
        '/C=RU/ST=Moscow/L=Moscow/O=Yandex/OU=Infra/CN=normal_user@ld.yandex.ru'
        '/emailAddress=normal_user@yandex-team.ru'
    )
    kwargs = {
        'HTTP_X_SSL_CLIENT_SERIAL': old_cert.serial_number,
        'HTTP_X_SSL_CLIENT_VERIFY': '0',
        'HTTP_X_SSL_CLIENT_SUBJECT': csr_subject,
        'data': {
            'request': pc_csrs[normal_user.username],
        }
    }

    response = crt_client.json.post('/api/reissue/', **kwargs)
    assert response.status_code == 201

    reissued_cert_pk = response.json()['id']

    for cert in Certificate.objects.filter(pc_inum='same').exclude(pk=reissued_cert_pk):
        assert cert.revoke_at is not None

        action = cert.actions.get()
        assert action.type == 'cert_add_to_hold_queue'
        assert (
            action.description == 'reissued'
            if cert == old_cert
            else f'reissued with same pc_inum by {reissued_cert_pk}'
        )

    for cert in Certificate.objects.exclude(pc_inum='same'):
        assert cert.revoke_at is None
        assert not cert.actions.exists()


def test_reissue_skips_certificates_with_blank_pc_enum(crt_client, users, pc_csrs, settings, certificate_types):
    settings.CRT_REISSUE_PC_THRESHOLD_DAYS = 500
    normal_user = users['normal_user']
    another_user = users['another_user']

    for sn in range(3):
        create_certificate(
            normal_user,
            certificate_types['pc'],
            serial_number=str(sn),
            common_name='normal_user@ld.yandex.ru',
            pc_inum='',
        )
    old_cert = Certificate.objects.last()
    create_certificate(another_user, certificate_types['linux-pc'], pc_inum='')
    create_certificate(normal_user, certificate_types['pc'], pc_inum='')

    for cert in Certificate.objects.all():
        assert cert.revoke_at is None

    csr_subject = (
        '/C=RU/ST=Moscow/L=Moscow/O=Yandex/OU=Infra/CN=normal_user@ld.yandex.ru'
        '/emailAddress=normal_user@yandex-team.ru'
    )
    kwargs = {
        'HTTP_X_SSL_CLIENT_SERIAL': old_cert.serial_number,
        'HTTP_X_SSL_CLIENT_VERIFY': '0',
        'HTTP_X_SSL_CLIENT_SUBJECT': csr_subject,
        'data': {
            'request': pc_csrs[normal_user.username],
        }
    }
    response = crt_client.json.post('/api/reissue/', **kwargs)
    assert response.status_code == 201

    for cert in Certificate.objects.all():
        if cert == old_cert:
            assert cert.revoke_at is not None
            action = cert.actions.get()
            assert action.type == 'cert_add_to_hold_queue'
            assert action.description == 'reissued'
        else:
            assert cert.revoke_at is None
            assert not cert.actions.exists()


@pytest.mark.parametrize('cert_type', (CERT_TYPE.PC, CERT_TYPE.BANK_PC))
@pytest.mark.parametrize('pc_inum', ('correct', 'incorrect'))
@mock.patch('intranet.crt.api.base.serializer_mixins.reissue.get_inums_and_models')
def test_reissue_pc_inum_validation(mocked_bot, crt_client, users, pc_csrs, settings, pc_inum, cert_type):
    settings.CRT_REISSUE_PC_THRESHOLD_DAYS = 500
    requester = 'helpdesk_user'
    requestee = 'normal_user'
    crt_client.login(requester)
    mocked_bot.return_value = [('correct', 'Notebook')]

    request_data = {
        'pc_os': 'orig',
        'pc_hostname': 'orig',
        'pc_serial_number': 'orig',
        'pc_mac': 'orig',
        'pc_inum': 'orig',
        'type': cert_type,
        'ca_name': CA_NAME.TEST_CA,
        'request': pc_csrs[requestee],
    }

    response = crt_client.json.post('/api/certificate/', data=request_data)
    assert response.status_code == 201
    cert_data = response.json()
    serial_number = cert_data['serial_number']
    csr_subject = (
        '/C=RU/ST=Moscow/L=Moscow/O=Yandex/OU=Infra/CN=normal_user@ld.yandex.ru'
        '/emailAddress=normal_user@yandex-team.ru'
    )

    kwargs = {
        'HTTP_X_SSL_CLIENT_SERIAL': serial_number,
        'HTTP_X_SSL_CLIENT_VERIFY': '0',
        'HTTP_X_SSL_CLIENT_SUBJECT': csr_subject,
        'data': {
            'request': pc_csrs[requestee],
            'pc_inum': pc_inum,
        }
    }

    response = crt_client.json.post('/api/reissue/', **kwargs)
    assert response.status_code == 201 if pc_inum == 'correct' else 400

    if pc_inum == 'correct':
        cert = Certificate.objects.last()
        assert cert.pc_inum == pc_inum
    else:
        assert response.json() == {
            'detail': 'User normal_user has no device with pc_inum \'incorrect\''
        }


@pytest.mark.parametrize('cert_type', (CERT_TYPE.PC, CERT_TYPE.BANK_PC))
@mock.patch('intranet.crt.core.utils.CrtSession.get')
def test_reissue_pc_inum_validation_404(crt_session, crt_client, users, pc_csrs, settings, cert_type):
    settings.CRT_REISSUE_PC_THRESHOLD_DAYS = 500
    requester = 'helpdesk_user'
    requestee = 'normal_user'
    crt_client.login(requester)
    crt_session.return_value = HttpResponseNotFound('{"error": "User not found"}')

    request_data = {
        'pc_os': 'orig',
        'pc_hostname': 'orig',
        'pc_serial_number': 'orig',
        'pc_mac': 'orig',
        'pc_inum': 'orig',
        'type': cert_type,
        'ca_name': CA_NAME.TEST_CA,
        'request': pc_csrs[requestee],
    }
    response = crt_client.json.post('/api/certificate/', data=request_data)
    assert response.status_code == 201
    cert_data = response.json()
    serial_number = cert_data['serial_number']
    csr_subject = (
        '/C=RU/ST=Moscow/L=Moscow/O=Yandex/OU=Infra/CN=normal_user@ld.yandex.ru'
        '/emailAddress=normal_user@yandex-team.ru'
    )
    kwargs = {
        'HTTP_X_SSL_CLIENT_SERIAL': serial_number,
        'HTTP_X_SSL_CLIENT_VERIFY': '0',
        'HTTP_X_SSL_CLIENT_SUBJECT': csr_subject,
        'data': {
            'request': pc_csrs[requestee],
            'pc_inum': 'somestr',
        }
    }
    response = crt_client.json.post('/api/reissue/', **kwargs)
    assert response.status_code == 201
