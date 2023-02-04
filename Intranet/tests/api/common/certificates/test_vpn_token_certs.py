import mock
import pytest
from constance.test import override_config
from freezegun import freeze_time
from rest_framework import status

from django.conf import settings
from django.contrib.auth.models import Permission
from django.utils import timezone
from django.utils.encoding import force_text

from intranet.crt.constants import AFFILIATION, CA_NAME, CERT_TYPE
from intranet.crt.core.ca.exceptions import CaError
from intranet.crt.csr import FullSubjectCsrConfig
from __tests__.utils.common import create_certificate
from __tests__.utils.ssl import build_temp_pc_csr


@pytest.mark.parametrize('path', ('/api/certificate/', '/api/v2/certificates/'))
@pytest.mark.parametrize('user_has_hw', (True, False))
@pytest.mark.parametrize('user_has_existing_hw_certs', (True, False))
@pytest.mark.parametrize('affiliation', AFFILIATION.CHOICES)
@pytest.mark.parametrize('has_perm_for_self', (True, False))
@pytest.mark.parametrize('request_for_self', (True, False))
@mock.patch('intranet.crt.users.models.old_get_inums_and_models')
def test_vpn_token_request_restrictions(
    mocked_bot, request_for_self, has_perm_for_self, affiliation, crt_client, users,
    user_has_existing_hw_certs, user_has_hw, path, certificate_types,
):
    helpdesk_user = users['helpdesk_user']
    normal_user = users['normal_user']
    if has_perm_for_self:
        permission = Permission.objects.get(codename='can_issue_vpn_token_certificates_for_self')
        helpdesk_user.user_permissions.add(permission)

    cert_user = helpdesk_user if request_for_self else normal_user
    cert_user.affiliation = affiliation[0]
    cert_user.save()

    mocked_bot.return_value = [('618', 'Notebook')] if user_has_hw else []
    if user_has_existing_hw_certs:
        create_certificate(cert_user, certificate_types[CERT_TYPE.PC])

    csr = FullSubjectCsrConfig(
        common_name=f'{cert_user.username}@ld.yandex.ru',
        email=f'{cert_user.username}@yandex-team.ru',
        country='RU',
        city='Moscow',
        unit='Infra',
    ).get_csr()

    request_data = {
        'type': CERT_TYPE.VPN_TOKEN,
        'ca_name': CA_NAME.TEST_CA,
        'request': force_text(csr),
    }
    crt_client.login(helpdesk_user.username)
    response = crt_client.json.post('/api/certificate/', data=request_data)

    if request_for_self and has_perm_for_self:
        assert response.status_code == status.HTTP_201_CREATED
        return

    if cert_user.affiliation == AFFILIATION.YANDEX:
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        assert response.json() == {'non_field_errors': ['vpn-token certificates is only for external users.']}
        return

    if user_has_hw:
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        assert response.json() == {
            'non_field_errors': ['vpn-token certificates is only for users without BOT hardware.']
        }
        return

    if user_has_existing_hw_certs:
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        assert response.json() == {
            'non_field_errors': ['vpn-token certificates is only for users without personal hardware.']
        }
        return

    assert response.status_code == status.HTTP_201_CREATED


@pytest.mark.parametrize('path', ('/api/certificate/', '/api/v2/certificates/'))
@pytest.mark.parametrize(
    'cert_type',
    {CERT_TYPE.VPN_TOKEN} | {CERT_TYPE.TEMP_PC} | CERT_TYPE.USER_HARDWARE_TYPES,
)
@mock.patch('intranet.crt.users.models.old_get_inums_and_models')
def test_existing_vpn_token_hold(
    mocked_bot, certificate_types, cert_type, crt_client, users, path,
):
    helpdesk_user = users['helpdesk_user']
    helpdesk_user.affiliation = AFFILIATION.EXTERNAL
    helpdesk_user.save()

    existing_cert = create_certificate(helpdesk_user, certificate_types[CERT_TYPE.VPN_TOKEN])

    mocked_bot.return_value = []
    assert CERT_TYPE.TEMP_PC not in CERT_TYPE.USER_HARDWARE_TYPES

    if cert_type == CERT_TYPE.TEMP_PC:
        csr = build_temp_pc_csr(f'{helpdesk_user.username}@pda-ld.yandex.ru')
        create_certificate(helpdesk_user, certificate_types[CERT_TYPE.PC])
    else:
        csr = FullSubjectCsrConfig(
            common_name=f'{helpdesk_user.username}@ld.yandex.ru',
            email=f'{helpdesk_user.username}@yandex-team.ru',
            country='RU',
            city='Moscow',
            unit='Infra',
        ).get_csr()

    request_data = {
        'type': cert_type,
        'ca_name': CA_NAME.TEST_CA,
        'request': force_text(csr),
        'pc_os': 'dummy',
        'pc_mac': 'dummy',
        'pc_hostname': 'dummy',
    }
    crt_client.login(helpdesk_user.username)
    request_time = timezone.now()
    with freeze_time(request_time), override_config(PDAS_WHITELIST='normal_user'):
        response = crt_client.json.post('/api/certificate/', data=request_data)

    assert response.status_code == status.HTTP_201_CREATED

    existing_cert.refresh_from_db()
    if cert_type == CERT_TYPE.TEMP_PC:
        assert existing_cert.revoke_at is None
        return
    assert existing_cert.revoke_at == request_time + timezone.timedelta(
        days=settings.CRT_VPN_TOKEN_HOLD_ON_REISSUE_AFTER_DAYS
    )


@pytest.mark.parametrize('ca_error', (True, False))
@pytest.mark.parametrize('path', ('/api/certificate/', '/api/v2/certificates/'))
@pytest.mark.parametrize('cert_type', {CERT_TYPE.VPN_TOKEN} | CERT_TYPE.USER_HARDWARE_TYPES)
@mock.patch('intranet.crt.users.models.old_get_inums_and_models')
def test_existing_vpn_token_skip_hold_with_ca_error(
    mocked_bot, certificate_types, cert_type, crt_client, users, path, ca_error,
):
    helpdesk_user = users['helpdesk_user']
    helpdesk_user.affiliation = AFFILIATION.EXTERNAL
    helpdesk_user.save()

    existing_cert = create_certificate(helpdesk_user, certificate_types[CERT_TYPE.VPN_TOKEN])

    mocked_bot.return_value = []
    csr = FullSubjectCsrConfig(
        common_name=f'{helpdesk_user.username}@ld.yandex.ru',
        email=f'{helpdesk_user.username}@yandex-team.ru',
        country='RU',
        city='Moscow',
        unit='Infra',
    ).get_csr()

    request_data = {
        'type': cert_type,
        'ca_name': CA_NAME.TEST_CA,
        'request': force_text(csr),
        'pc_os': 'dummy',
        'pc_mac': 'dummy',
        'pc_hostname': 'dummy',
    }
    crt_client.login(helpdesk_user.username)
    request_time = timezone.now()
    with freeze_time(request_time), override_config(PDAS_WHITELIST='normal_user'):
        if ca_error:
            with mock.patch('intranet.crt.core.ca.test.TestCA._issue') as patched_issue:
                patched_issue.side_effect = CaError()
                response = crt_client.json.post('/api/certificate/', data=request_data)
                assert response.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR
                existing_cert.refresh_from_db()
                assert existing_cert.revoke_at is None
                return
        else:
            response = crt_client.json.post('/api/certificate/', data=request_data)

    assert response.status_code == status.HTTP_201_CREATED
    existing_cert.refresh_from_db()
    assert existing_cert.revoke_at == request_time + timezone.timedelta(
        days=settings.CRT_VPN_TOKEN_HOLD_ON_REISSUE_AFTER_DAYS
    )
    if cert_type == CERT_TYPE.VPN_TOKEN:
        assert helpdesk_user.certificates.active(CERT_TYPE.VPN_TOKEN).count() == 1
