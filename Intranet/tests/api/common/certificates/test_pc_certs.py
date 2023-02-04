import mock
import pytest
from constance.test import override_config
from django.contrib.auth.models import Permission
from django.utils import timezone
from django.utils.encoding import force_text
from django_abc_data.models import AbcService
from rest_framework import status

from intranet.crt.constants import CERT_TYPE, CERT_STATUS, CA_NAME, AFFILIATION
from intranet.crt.core.models import Certificate
from intranet.crt.csr import FullSubjectCsrConfig
from __tests__.utils.common import create_certificate
from __tests__.utils.ssl import CsrBuilder, build_tpm_smartcard_1c_csr, build_temp_pc_csr

pytestmark = pytest.mark.django_db


def make_request_data(common_name='normal_user@ld.yandex.ru', cert_type=CERT_TYPE.ZOMB_PC, st_id='FAT-123'):
    return {
        'type': cert_type,
        'ca_name': CA_NAME.TEST_CA,
        'pc_os': 'Mac OS X 10.12',
        'pc_hostname': 'mac_user02',
        'pc_mac': '111',
        'request': force_text(csr_from_common_name(common_name)),
        'hardware_request_st_id': st_id,
    }


def csr_from_common_name(common_name):
    csr_config = FullSubjectCsrConfig(
        common_name=common_name,
        email='normal_user@yandex-team.ru',
        country='RU',
        city='Moscow',
        unit='Infra',
    )
    return csr_config.get_csr()


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_get_new_user_certificates(crt_client, users, path):
    normal_user = users['normal_user']
    crt_client.login(normal_user.username)

    response = crt_client.json.get(path)

    assert response.status_code == status.HTTP_200_OK

    response_data = response.json()
    assert response_data['count'] == 0
    assert response_data['results'] == []


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_pc_cert_without_permission(crt_client, users, path):
    request_data = {
        'type': CERT_TYPE.PC,
        'ca_name': CA_NAME.TEST_CA,
        'request': 'csr',
        'pc_os': 'os',
        'pc_hostname': 'hostname',
        'pc_serial_number': 'serial',
        'pc_mac': 'mac',
    }

    normal_user = users['normal_user']
    crt_client.login(normal_user.username)

    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_403_FORBIDDEN


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('cert_type', CERT_TYPE.USERS_TYPES & CERT_TYPE.CSR_REQUESTABLE_TYPES)
@pytest.mark.parametrize('helpdesk_ticket', ['TEST-1', None])
@mock.patch('intranet.crt.users.models.old_get_inums_and_models')
def test_request_certificate_accept_hd_ticket_field(mocked_bot, crt_client, users, pc_csrs, cert_type, mocked_ldap,
                                                    settings, path, helpdesk_ticket, super_user_ld_cert):
    path = '/api/v2/certificates/'

    super_user = users['super_user']

    if cert_type == CERT_TYPE.TPM_SMARTCARD_1C:
        request = build_tpm_smartcard_1c_csr('Normal User')
    elif cert_type == CERT_TYPE.BOTIK:
        request = None
    elif cert_type == CERT_TYPE.TEMP_PC:
        request = force_text(build_temp_pc_csr(common_name='super_user@pda-ld.yandex.ru'))
    else:
        content = {
            'country': 'RU',
            'city': 'Moscow',
            'email': 'super_user@yandex-team.ru',
            'unit': 'Infra',
            'common_name': 'super_user@ld.yandex.ru',
        }

        if cert_type == CERT_TYPE.IMDM:
            content.update(unit='MOBILE')
        elif cert_type == CERT_TYPE.ZOMBIE:
            content.update(common_name='zomb-user@ld.yandex.ru')
            content.update(unit='zomb')
        elif cert_type == CERT_TYPE.ZOMB_PC:
            content.update(common_name='zomb.wst.yandex.net')

        request = force_text(FullSubjectCsrConfig(**content).get_csr())

    request_data = {
        'type': cert_type,
        'ca_name': CA_NAME.TEST_CA,
        'helpdesk_ticket': helpdesk_ticket,

    }

    if helpdesk_ticket is None:
        request_data.pop('helpdesk_ticket')

    if request:
        request_data.update(request=request)

    if cert_type == CERT_TYPE.MOBVPN:
        request_data.update(secret=settings.MOBVPN_SECRET_TOKEN)
    elif cert_type == CERT_TYPE.IMDM:
        request_data.update(pc_serial_number='FK2VTQEAJCLA')
    elif cert_type == CERT_TYPE.BOTIK:
        request_data.update(common_name='tunnel144.88')
    elif cert_type in {CERT_TYPE.ZOMB_PC, CERT_TYPE.PC, CERT_TYPE.BANK_PC, CERT_TYPE.LINUX_PC, CERT_TYPE.COURTECY_VPN}:
        request_data.update(pc_mac='mac')
        request_data.update(pc_hostname='hostname')
        request_data.update(hardware_request_st_id='abc-1')
        request_data.update(pc_os='os')
    if cert_type == CERT_TYPE.VPN_TOKEN:
        mocked_bot.return_value = []
        super_user.affiliation = AFFILIATION.EXTERNAL
        super_user.save()

    crt_client.login(super_user.username)

    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_201_CREATED
    data = response.json()
    cert = Certificate.objects.get(serial_number=data['serial_number'])
    assert cert.helpdesk_ticket == helpdesk_ticket
    assert data['helpdesk_ticket'] == helpdesk_ticket


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_helpdesk_requests_pc_certificate(crt_client, users, user_datas, pc_csrs, path):
    requester = 'helpdesk_user'
    user = 'normal_user'

    request_data = {
        'common_name': '',
        'type': CERT_TYPE.PC,
        'ca_name': CA_NAME.TEST_CA,
        'pc_os': 'Mac OS X 10.12',
        'pc_hostname': 'mac_user02',
        'pc_serial_number': '1111',
        'pc_mac': '111',
        'request': pc_csrs[user]
    }

    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)
    with mock.patch('intranet.crt.api.base.serializer_mixins.personal.get_inum_by_sn') as get_inum_by_sn:
        get_inum_by_sn.return_value = 'inum'
        response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_201_CREATED

    response_data = response.json()
    assert response_data['type'] == CERT_TYPE.PC
    assert response_data['status'] == CERT_STATUS.ISSUED
    if path == '/api/certificate/':
        assert response_data['username'] == user
        assert response_data['requester'] == requester
    else:
        assert response_data['user'] == user_datas[user]
        assert response_data['requester'] == user_datas[requester]


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_invalid_type(crt_client, users, path):
    requester = 'helpdesk_user'

    request_data = {
        'type': 'invalid_type',
        'ca_name': CA_NAME.TEST_CA,
        'pc_os': 'Mac OS X 10.12',
        'pc_hostname': 'mac_user02',
        'pc_serial_number': '1111',
        'pc_mac': '111',
        'common_name': 'something',
    }

    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    with mock.patch('intranet.crt.api.base.serializer_mixins.personal.get_inum_by_sn') as get_inum_by_sn:
        get_inum_by_sn.return_value = 'inum'
        response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_400_BAD_REQUEST


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_inactive_type(crt_client, users, path):
    requester = 'helpdesk_user'

    request_data = {
        'type': 'inactive_type',
        'ca_name': CA_NAME.TEST_CA,
        'request': 'csr',
        'pc_os': 'Mac OS X 10.12',
        'pc_hostname': 'mac_user02',
        'pc_serial_number': '1111',
        'pc_mac': '111',
    }

    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    with mock.patch('intranet.crt.api.base.serializer_mixins.personal.get_inum_by_sn') as get_inum_by_sn:
        get_inum_by_sn.return_value = 'inum'
        response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_400_BAD_REQUEST


@pytest.mark.parametrize('post_path,get_path',
                         [('/api/certificate/', '/api/certificate/{id}/download'),
                          ('/api/v2/certificates/', '/api/v2/certificate/{id}/download/'),
                          ])
def test_download2_error_message(crt_client, users, post_path, get_path):
    requester = 'helpdesk_user'

    csr_content = {
        'country': 'RU',
        'city': 'Moscow',
        'email': 'imdm_user@yandex-team.ru',
        'unit': 'Infra',
        'common_name': 'helpdesk_user@ld.yandex.ru',
    }
    csr = FullSubjectCsrConfig(**csr_content).get_csr()

    request_data = {
        'type': CERT_TYPE.LINUX_TOKEN,
        'ca_name': CA_NAME.TEST_CA,
        'request': force_text(csr),
    }

    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    response = crt_client.json.post(post_path, data=request_data)
    assert response.status_code == status.HTTP_201_CREATED

    certificate = Certificate.objects.get()

    crt_client.login('normal_user')
    get_path = get_path.format(id=certificate.pk)

    if get_path.startswith('/api/certificate/'):
        response = crt_client.json.get(get_path)
    else:
        response = crt_client.json.get(get_path, {'format': 'pem'})

    assert response.status_code == status.HTTP_403_FORBIDDEN
    assert response.content == b'Access denied'


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_assessor_cert(crt_client, users, user_datas, path):
    requester = 'helpdesk_user'
    assessor = 'normal_user'

    request_data = {
        'type': CERT_TYPE.ASSESSOR,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': assessor,
    }

    crt_client.login(requester)
    response = crt_client.json.post(path, data=request_data)

    response_data = response.json()
    assert response.status_code == status.HTTP_201_CREATED
    assert response_data['type'] == CERT_TYPE.ASSESSOR
    assert response_data['status'] == CERT_STATUS.ISSUED
    if path == '/api/certificate/':
        assert response_data['username'] == assessor
        assert response_data['requester'] == requester
    else:
        assert response_data['user'] == user_datas[assessor]
        assert response_data['requester'] == user_datas[requester]


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_rc_server_cert(crt_client, users, user_datas, path):
    requester = 'rc_server_user'
    server_name = 'sas-1.search.yandex.net'

    request_data = {
        'type': CERT_TYPE.RC_SERVER,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': server_name,
    }

    crt_client.login(requester)
    response = crt_client.json.post(path, data=request_data)

    response_data = response.json()
    assert response.status_code == status.HTTP_201_CREATED
    assert response_data['type'] == CERT_TYPE.RC_SERVER
    assert response_data['status'] == CERT_STATUS.ISSUED
    assert response_data['common_name'] == server_name
    if path == '/api/certificate/':
        assert response_data['username'] == requester
        assert response_data['requester'] == requester
    else:
        assert response_data['user'] == user_datas[requester]
        assert response_data['requester'] == user_datas[requester]


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_hypercube_requests_hypercube_certificate(crt_client, users, user_datas,
                                                  pc_csrs, path, certificate_types):
    normal_user = 'normal_user'
    hypercube_user = 'hypercube_user'

    data = {
        'type': CERT_TYPE.HYPERCUBE,
        'ca_name': CA_NAME.TEST_CA,
    }

    # У обычных пользователей нет прав
    crt_client.login(normal_user)
    response = crt_client.json.post(path, data=data)
    assert response.status_code == status.HTTP_403_FORBIDDEN

    crt_client.login(hypercube_user)
    # common name должен иметь такой же паттерн, что и ninja-сертификаты
    data = {
        'type': CERT_TYPE.HYPERCUBE,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': '{}@pda.yandex.ru'.format(normal_user),
    }
    response = crt_client.json.post(path, data=data)
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json() == {'common_name': ['Invalid common name']}

    # desired_ttl_days – обязательный параметр
    data = {
        'type': CERT_TYPE.HYPERCUBE,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': '{}@pda-ld.yandex.ru'.format(normal_user),
    }
    response = crt_client.json.post(path, data=data)
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json() == {'desired_ttl_days': ['This field is required.']}

    # Корректный запрос, но normal_user необходим активный ld-сертификат
    data = {
        'type': CERT_TYPE.HYPERCUBE,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': '{}@pda-ld.yandex.ru'.format(normal_user),
        'desired_ttl_days': '10',
    }
    response = crt_client.json.post(path, data=data)
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert response.json() == {
        'non_field_errors': [
            'User normal_user has no active certificates '
            'to access Yandex network. Issuing prohibited.'
        ]
    }
    # Проверим PDAS_WHITELIST
    with override_config(PDAS_WHITELIST='normal_user'):
        response = crt_client.json.post(path, data=data)
        assert response.status_code == status.HTTP_201_CREATED

    # Создадим сертификат. Успешный запрос без PDAS_WHITELIST
    create_certificate(
        users['normal_user'], certificate_types['pc'], common_name='normal_user@ld.yandex.ru'
    )
    response = crt_client.json.post(path, data=data)
    assert response.status_code == status.HTTP_201_CREATED
    response_data = response.json()
    assert response_data['type'] == CERT_TYPE.HYPERCUBE
    assert response_data['status'] == CERT_STATUS.ISSUED
    if path == '/api/certificate/':
        assert response_data['username'] == normal_user  # юзером становится тот, для кого запрошен серт
        assert response_data['requester'] == hypercube_user
    else:
        assert response_data['user'] == user_datas[normal_user]
        assert response_data['requester'] == user_datas[hypercube_user]


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_invalid_ca(crt_client, users, path):
    requester = 'helpdesk_user'
    crt_client.login(requester)

    request_data = {
        'type': CERT_TYPE.ASSESSOR,
        'ca_name': 'InvalidCA',
        'common_name': requester,
    }

    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_400_BAD_REQUEST


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_anavailable_ca(crt_client, users, path):
    requester = 'helpdesk_user'
    crt_client.login(requester)

    request_data = {
        'type': CERT_TYPE.ASSESSOR,
        'ca_name': 'InternalCA',
        'common_name': requester,
    }

    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_400_BAD_REQUEST


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('cert_type', CERT_TYPE.CSR_REQUESTABLE_TYPES)
@mock.patch('intranet.crt.users.models.old_get_inums_and_models')
def test_csr_is_allowed_for_a_subset_of_types(mocked_bot, mocked_ldap, crt_client, users, cert_type,
                                              settings, path, abc_services, normal_user_ld_cert):
    """Проверим, что запрос через CSR можно делать только для нескольких типов сертификатов
    """

    requester = 'normal_user'
    user = users[requester]
    user.is_superuser = True
    user.save()

    crt_client.login(requester)
    content = {
        'country': 'RU',
        'city': 'Moscow',
        'unit': 'Infra',
        'email': 'normal_user@yandex-team.ru',
    }
    if cert_type in (CERT_TYPE.CLIENT_SERVER, CERT_TYPE.HOST):
        content['common_name'] = 'xxx.yndx.net'
    else:
        content['common_name'] = 'normal_user@ld.yandex.ru'

    if cert_type == CERT_TYPE.ZOMBIE:
        content['unit'] = 'zomb'
        content['common_name'] = 'zomb-user@ld.yandex.ru'

    if cert_type == CERT_TYPE.ZOMB_PC:
        content['common_name'] = 'zomb.wst.yandex.net'

    if cert_type == CERT_TYPE.IMDM:
        content['unit'] = 'MOBILE'

    if cert_type == CERT_TYPE.BANK_CLIENT_SERVER:
        content['common_name'] = 'xxx.yandex-bank.net'

    csr_config = FullSubjectCsrConfig(**content)

    request_data = {
        'type': cert_type,
        'ca_name': CA_NAME.TEST_CA,
        'request': force_text(csr_config.get_csr()),
    }

    if cert_type == CERT_TYPE.MOBVPN:
        request_data.update({
            'secret': settings.MOBVPN_SECRET_TOKEN,
        })
    elif cert_type in (
        CERT_TYPE.PC, CERT_TYPE.BANK_PC, CERT_TYPE.COURTECY_VPN, CERT_TYPE.LINUX_PC, CERT_TYPE.ZOMB_PC
    ):
        request_data.update({
            'pc_os': 'dummy',
            'pc_mac': 'dummy',
            'pc_hostname': 'dummy',
        })
    elif cert_type == CERT_TYPE.HOST:
        request_data.update({
            'hosts': 'xxx.yndx.net',
            'abc_service': 1,
        })
    elif cert_type == CERT_TYPE.IMDM:
        request_data.update({
            'pc_serial_number': 'FK2VTQEAJCLA',
        })
    if cert_type == CERT_TYPE.ZOMB_PC:
        request_data.update({
            'hardware_request_st_id': 'abc-1',
        })
    if cert_type == CERT_TYPE.TPM_SMARTCARD_1C:
        request_data = {
            'type': CERT_TYPE.TPM_SMARTCARD_1C,
            'ca_name': CA_NAME.TEST_CA,
            'request': build_tpm_smartcard_1c_csr('Normal User'),
        }
    if cert_type == CERT_TYPE.TEMP_PC:
        content['common_name'] = 'normal_user@pda-ld.yandex.ru'
        request_data.update({
            'type': CERT_TYPE.TEMP_PC,
            'ca_name': CA_NAME.TEST_CA,
            'request': build_temp_pc_csr(content['common_name']),
        })
    if cert_type == CERT_TYPE.VPN_TOKEN:
        mocked_bot.return_value = []
        user.affiliation = AFFILIATION.EXTERNAL
        user.save()

    response = crt_client.json.post(path, data=request_data)
    data = response.json()
    assert response.status_code == 201
    if cert_type not in CERT_TYPE.CUSTOM_COMMON_NAME_TYPES:
        assert data['common_name'] == content['common_name']
    assert data['requested_by_csr'] is True


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('common_name_result', [
    ('cn.yandex.ru', False),
    ('cn.yandex.net', True),
    ('cn.yandex.com', False),
    ('cn.example.com', False),
    ('crt.yandex-team.ru', True),
    ('Yandex Office IPSEC CN', True),
    ('Ab ra ca dab ra', False),
])
def test_client_server_pattern_matching(crt_client, users, common_name_result, settings, path):
    """ Проверим, что запрос client_server через CSR валидирует CN по шаблонам из настроек """
    requester = 'normal_user'
    user = users[requester]
    user.is_superuser = True
    user.save()

    common_name, result = common_name_result

    content = {
        'country': 'RU',
        'city': 'Moscow',
        'unit': 'Infra',
        'email': 'normal_user@yandex-team.ru',
        'common_name': common_name,
    }

    csr_config = FullSubjectCsrConfig(**content)
    request_data = {
        'type': CERT_TYPE.CLIENT_SERVER,
        'ca_name': CA_NAME.TEST_CA,
        'request': force_text(csr_config.get_csr()),
    }

    crt_client.login(requester)
    response = crt_client.json.post(path, data=request_data)

    status_code = status.HTTP_201_CREATED if result else status.HTTP_400_BAD_REQUEST
    assert response.status_code == status_code


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_client_server_abc_service(crt_client, users, settings, path):
    requester = 'normal_user'
    user = users[requester]
    user.is_superuser = True
    user.save()

    content = {
        'country': 'RU',
        'city': 'Moscow',
        'unit': 'Infra',
        'email': 'normal_user@yandex-team.ru',
        'common_name': 'cn.yandex.net',
    }
    service = AbcService.objects.create(external_id=123, created_at=timezone.now(), modified_at=timezone.now())
    csr_config = FullSubjectCsrConfig(**content)
    request_data = {
        'type': CERT_TYPE.CLIENT_SERVER,
        'ca_name': CA_NAME.TEST_CA,
        'request': force_text(csr_config.get_csr()),
        'abc_service': 123,
    }

    crt_client.login(requester)
    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_201_CREATED
    cert = Certificate.objects.get()
    assert cert.abc_service == service


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('cert_type', CERT_TYPE.NOCSR_REQUESTABLE_TYPES)
def test_csr_is_forbidden_for_a_subset_of_types(crt_client, users, user_datas, cert_type, path,
                                                abc_services, normal_user_ld_cert):
    requester = 'normal_user'
    user = users[requester]
    user.is_superuser = True
    user.save()

    crt_client.login(requester)
    content = {
        'country': 'RU',
        'city': 'Moscow',
        'unit': 'Infra',
        'email': 'normal_user@yandex-team.ru',
    }
    if cert_type in (CERT_TYPE.RC_SERVER,):
        content['common_name'] = 'sas-1.search.yandex.net'
    else:
        content['common_name'] = 'fakeuser@ld.yandex.ru'

    csr_config = FullSubjectCsrConfig(**content)

    request_data = {
        'type': cert_type,
        'ca_name': CA_NAME.TEST_CA,
    }
    if cert_type not in CERT_TYPE.CSR_REQUESTABLE_TYPES:
        request_data['request'] = force_text(csr_config.get_csr())

    non_csr_common_name = None
    if cert_type == CERT_TYPE.HOST:
        non_csr_common_name = 'xxx.search.yandex.net'
        request_data['hosts'] = non_csr_common_name
        request_data['abc_service'] = 1
    elif cert_type in (CERT_TYPE.LINUX_PC, CERT_TYPE.COURTECY_VPN, CERT_TYPE.PC):
        non_csr_common_name = 'normal_user@ld.yandex.ru'
        request_data.update({
            'request': '',
            'yav_secret_id': '',
            'pc_os': 'dummy',
            'pc_mac': 'dummy',
            'pc_hostname': 'dummy',
            'common_name': non_csr_common_name,
        })
    elif cert_type == CERT_TYPE.BOTIK:
        non_csr_common_name = 'tunnel144.88'
        request_data.update({
            'common_name': non_csr_common_name,
        })
    elif cert_type == CERT_TYPE.RC_SERVER:
        non_csr_common_name = 'xxx.search.yandex.net'
        request_data['common_name'] = non_csr_common_name
    elif cert_type == CERT_TYPE.YC_SERVER:
        non_csr_common_name = 'xxx.yandexcloud.net'
        request_data['common_name'] = non_csr_common_name
    elif cert_type == CERT_TYPE.MDB:
        non_csr_common_name = 'xxx.db.yandex.net'
        request_data['common_name'] = non_csr_common_name
    elif cert_type == CERT_TYPE.CLIENT_SERVER:
        non_csr_common_name = 'xxx.yndx.net'
        request_data['common_name'] = non_csr_common_name
    elif cert_type == CERT_TYPE.BANK_CLIENT_SERVER:
        non_csr_common_name = 'xxx.yandex-bank.net'
        request_data['common_name'] = non_csr_common_name
    elif cert_type == CERT_TYPE.POSTAMATE:
        non_csr_common_name = 'postamate@market.yandex'
        request_data['common_name'] = non_csr_common_name
    elif cert_type == CERT_TYPE.SDC:
        non_csr_common_name = 'vehicle-15647.rover.sdc.yandex.net'
        request_data['common_name'] = non_csr_common_name
    elif cert_type == CERT_TYPE.ASSESSOR:
        non_csr_common_name = 'normal_user'
        request_data['common_name'] = non_csr_common_name
    elif cert_type == CERT_TYPE.NINJA:
        non_csr_common_name = 'normal_user@pda-ld.yandex.ru'
        request_data['common_name'] = non_csr_common_name
    elif cert_type == CERT_TYPE.NINJA_EXCHANGE:
        non_csr_common_name = 'normal_user@ld.yandex.ru'
        request_data['common_name'] = non_csr_common_name
    elif cert_type == CERT_TYPE.HYPERCUBE:
        non_csr_common_name = 'normal_user@pda-ld.yandex.ru'
        request_data['common_name'] = non_csr_common_name
        request_data['desired_ttl_days'] = 3

    response = crt_client.json.post(path, data=request_data)
    data = response.json()
    assert response.status_code == 201
    # Everything is fine, request field was ignored
    assert data['requested_by_csr'] is False
    if cert_type in (CERT_TYPE.NINJA, CERT_TYPE.NINJA_EXCHANGE, CERT_TYPE.HYPERCUBE):
        if path == '/api/certificate/':
            assert data['username'] == 'normal_user'
        else:
            assert data['user'] == user_datas['normal_user']
    elif cert_type == CERT_TYPE.BOTIK:
        assert data['common_name'] == 'tunnel144.88@botik.yandex.ru'
    elif cert_type == CERT_TYPE.ASSESSOR:
        assert data['common_name'] == 'normal_user@assessors.yandex-team.ru'
    else:
        assert data['common_name'] == non_csr_common_name


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize(
    'cert_type',
    CERT_TYPE.CSR_REQUESTABLE_TYPES.intersection(CERT_TYPE.NOCSR_REQUESTABLE_TYPES),
)
def test_both_csr_nocsr_requestable_specialcases(crt_client, users, cert_type, path, abc_services):
    requester = 'normal_user'
    user = users[requester]
    user.is_superuser = True
    user.save()

    crt_client.login(requester)
    content = {
        'country': 'RU',
        'city': 'Moscow',
        'unit': 'Infra',
        'email': 'normal_user@yandex-team.ru',
    }
    common_name = 'normal_user@ld.yandex.ru'
    content['common_name'] = common_name
    csr_config = FullSubjectCsrConfig(**content)
    request_data = {
        'type': cert_type,
        'ca_name': CA_NAME.TEST_CA,
        'pc_os': 'dummy',
        'pc_mac': 'dummy',
        'pc_hostname': 'dummy',
        'common_name': common_name,
        'request': force_text(csr_config.get_csr()),
    }
    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == 400
    assert response.json() == {'non_field_errors': ['Both common_name and request given.']}


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('cert_type', CERT_TYPE.CSR_REQUESTABLE_TYPES)
@pytest.mark.parametrize('spoofing_method', ['prefix', 'postfix'])
def test_common_name_spoofing_csr(mocked_ldap, crt_client, users, cert_type, settings, path,
                                  abc_services, spoofing_method):
    """CERTOR-650"""

    requester = 'normal_user'
    user = users[requester]
    user.is_superuser = True
    user.save()

    crt_client.login(requester)
    content = {
        'country': 'RU',
        'city': 'Moscow',
        'unit': 'Infra',
        'email': 'normal_user@yandex-team.ru',
    }
    if cert_type in (CERT_TYPE.CLIENT_SERVER, CERT_TYPE.HOST):
        if spoofing_method == 'prefix':
            content['common_name'] = 'CN=evil@ld.yandex.ru;.ipmi.yandex.net'
        else:
            content['common_name'] = 'xxx.yndx.net/CN=evil@ld.yandex.ru'
    else:
        if spoofing_method == 'prefix':
            content['common_name'] = 'CN=evil@ld.yandex.ru;normal_user@ld.yandex.ru'
        else:
            content['common_name'] = 'normal_user@ld.yandex.ru/CN=evil@ld.yandex.ru'

    if cert_type == CERT_TYPE.IMDM:
        content['unit'] = 'MOBILE'

    csr_config = FullSubjectCsrConfig(**content)

    request_data = {
        'type': cert_type,
        'ca_name': CA_NAME.TEST_CA,
        'request': force_text(csr_config.get_csr()),
    }

    if cert_type == CERT_TYPE.MOBVPN:
        request_data.update({
            'secret': settings.MOBVPN_SECRET_TOKEN,
        })
    elif cert_type == CERT_TYPE.PC:
        request_data.update({
            'pc_os': 'dummy',
            'pc_mac': 'dummy',
            'pc_hostname': 'dummy',
        })
    elif cert_type == CERT_TYPE.HOST:
        request_data.update({
            'hosts': 'xxx.yndx.net',
            'abc_service': 1,
        })
    elif cert_type == CERT_TYPE.TPM_SMARTCARD_1C:
        if spoofing_method == 'prefix':
            cn = 'CN=AnotherUser;Valid User'
        else:
            cn = 'Valid User/CN=Another User'
        request_data = {
            'type': CERT_TYPE.TPM_SMARTCARD_1C,
            'ca_name': CA_NAME.TEST_CA,
            'request': build_tpm_smartcard_1c_csr(cn),
        }

    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == 400
    data = response.json()

    error_field = 'request'
    if cert_type == CERT_TYPE.VPN_TOKEN:
        error_field = 'non_field_errors'

    error_message = 'Invalid common name'
    if cert_type == CERT_TYPE.TPM_SMARTCARD_1C:
        if spoofing_method == 'prefix':
            error_message = (
                'CSR Subject \'CN=CN=AnotherUser\\;Valid User,CN=Users,DC=ld,DC=yandex,DC=ru\''
                ' does not match any LDAP distinguishedName. Please contact https://st.yandex-team.ru//CERTOR'
            )
        else:
            error_message = (
                'CSR Subject \'CN=Valid User/CN=Another User,CN=Users,DC=ld,DC=yandex,DC=ru\''
                ' does not match any LDAP distinguishedName. Please contact https://st.yandex-team.ru//CERTOR'
            )
    assert data[error_field] == [error_message]


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('cert_type', set(CERT_TYPE.active_types()))
@pytest.mark.parametrize('spoofing_method', ['prefix', 'postfix'])
def test_common_name_spoofing(crt_client, users, cert_type, settings, path, abc_services, spoofing_method):
    """CERTOR-650"""

    requester = 'normal_user'
    user = users[requester]
    user.is_superuser = True
    user.save()
    crt_client.login(requester)

    request_data = {
        'type': cert_type,
        'ca_name': CA_NAME.TEST_CA,
    }

    common_name = None
    if cert_type == CERT_TYPE.HOST:
        if spoofing_method == 'prefix':
            common_name = 'CN=evil@ld.yandex.ru;xxx.search.yandex.net'
        else:
            common_name = 'xxx.search.yandex.net/CN=evil@ld.yandex.ru'
        request_data['hosts'] = common_name
        request_data['abc_service'] = 1
    elif cert_type in (CERT_TYPE.LINUX_PC, CERT_TYPE.PC, CERT_TYPE.ZOMBIE):
        if spoofing_method == 'prefix':
            common_name = 'CN=evil@ld.yandex.ru;normal_user@ld.yandex.ru'
        else:
            common_name = 'normal_user@ld.yandex.ru/CN=evil@ld.yandex.ru'

        request_data.update({
            'pc_os': 'dummy',
            'pc_mac': 'dummy',
            'pc_hostname': 'dummy',
            'common_name': common_name,
        })
    elif cert_type == CERT_TYPE.BOTIK:
        if spoofing_method == 'prefix':
            common_name = 'CN=evil@ld.yandex.ru;tunnel144.88'
        else:
            common_name = 'tunnel144.88/CN=evil@ld.yandex.ru'
    elif cert_type == CERT_TYPE.RC_SERVER:
        if spoofing_method == 'prefix':
            common_name = 'CN=evil@ld.yandex.ru;xxx.search.yandex.net'
        else:
            common_name = 'xxx.search.yandex.net/CN=evil@ld.yandex.ru'
    elif cert_type == CERT_TYPE.CLIENT_SERVER:
        if spoofing_method == 'prefix':
            common_name = 'CN=evil@ld.yandex.ru;xxx.yndx.net'
        else:
            common_name = 'xxx.yndx.net/CN=evil@ld.yandex.ru'
    elif cert_type == CERT_TYPE.BANK_CLIENT_SERVER:
        if spoofing_method == 'prefix':
            common_name = 'CN=evil@ld.yandex.ru;xxx.yandex-bank.net'
        else:
            common_name = 'xxx.yandex-bank.net/CN=evil@ld.yandex.ru'
    elif cert_type == CERT_TYPE.SDC:
        if spoofing_method == 'prefix':
            common_name = 'CN=evil@ld.yandex.ru;vehicle-15647.rover.sdc.yandex.net'
        else:
            common_name = 'vehicle-15647.rover.sdc.yandex.net/CN=evil@ld.yandex.ru'
    elif cert_type == CERT_TYPE.ASSESSOR:
        if spoofing_method == 'prefix':
            common_name = 'CN=evil@ld.yandex.ru;normal_user'
        else:
            common_name = 'normal_user/CN=evil@ld.yandex.ru'
    elif cert_type in (CERT_TYPE.NINJA, CERT_TYPE.NINJA_EXCHANGE, CERT_TYPE.HYPERCUBE):
        if spoofing_method == 'prefix':
            common_name = 'CN=evil@ld.yandex.ru;normal_user@pda-ld.yandex.ru'
        else:
            common_name = 'normal_user@pda-ld.yandex.ru/CN=evil@ld.yandex.ru'
    elif cert_type in CERT_TYPE.MOBVPN:
        request_data['secret'] = settings.MOBVPN_SECRET_TOKEN
        if spoofing_method == 'prefix':
            common_name = 'CN=evil@ld.yandex.ru;normal_user@ld.yandex.ru'
        else:
            common_name = 'normal_user@ld.yandex.ru/CN=evil@ld.yandex.ru'
    request_data['common_name'] = common_name
    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == 400
    data = response.json()
    if cert_type in CERT_TYPE.CSR_REQUESTABLE_TYPES - {CERT_TYPE.HOST}:
        if cert_type in (
            CERT_TYPE.VPN_TOKEN, CERT_TYPE.CLIENT_SERVER,
            CERT_TYPE.MOBVPN, CERT_TYPE.TPM_SMARTCARD_1C,
            CERT_TYPE.BANK_CLIENT_SERVER,
        ):
            assert data['request'] == ['This field is required.']
    elif cert_type == CERT_TYPE.HOST:
        if spoofing_method == 'prefix':
            error_message = (
                'Host \'CN=evil@ld.yandex.ru;xxx.search.yandex.net\' contains invalid formatting, e.g. '
                'contains two or more asterisks, '
                'non-leading asterisks, whitespace characters, etc.'
            )
        else:
            error_message = (
                'Host \'xxx.search.yandex.net/CN=evil@ld.yandex.ru\' contains invalid formatting, e.g. '
                'contains two or more asterisks, '
                'non-leading asterisks, whitespace characters, etc.'
            )
        assert data == {
            'hosts': [error_message]
        }
    elif cert_type == CERT_TYPE.ASSESSOR:
        assert data == {
            'common_name': ['Пользователь не существует либо уволен'],
        }
    elif cert_type == CERT_TYPE.BOTIK:
        assert set(data.keys()) == {'common_name'}
        assert len(data['common_name']) == 1
        assert data['common_name'][0].startswith('Invalid common name: it does not match regex')
    elif cert_type == CERT_TYPE.SDC:
        assert set(data.keys()) == {'common_name'}
        assert len(data['common_name']) == 1
        assert data['common_name'][0] == 'Invalid common name'


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('cert_type', CERT_TYPE.PUBLIC_REQUEST_TYPES - CERT_TYPE.HARDWARE_TYPES - {CERT_TYPE.BOTIK})
def test_selfrequest_is_validated(crt_client, users, cert_type, path):
    requester = 'normal_user'

    crt_client.login(requester)
    request_data = {
        'type': cert_type,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': 'helpdesk_user@ld.yandex.ru',
    }

    if cert_type == CERT_TYPE.LINUX_PC:
        request_data.update({
            'pc_os': 'dummy',
            'pc_mac': 'dummy',
            'pc_hostname': 'dummy',
        })
    elif cert_type == CERT_TYPE.NINJA:
        request_data['common_name'] = 'helpdesk_user@pda-ld.yandex.ru'
    elif cert_type == CERT_TYPE.NINJA_EXCHANGE:
        request_data['common_name'] = 'helpdesk_user@ld.yandex.ru'
    elif cert_type == CERT_TYPE.MOBVPN:
        content = {
            'country': 'RU',
            'city': 'Moscow',
            'unit': 'Infra',
            'email': 'normal_user@yandex-team.ru',
            'common_name': 'helpdesk_user@ld.yandex.ru',
        }
        csr_config = FullSubjectCsrConfig(**content)
        del request_data['common_name']
        request_data['request'] = force_text(csr_config.get_csr())

    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    # Everything is fine, request field was ignored
    data = response.json()
    if cert_type == CERT_TYPE.MOBVPN:
        assert data['request'] == ['Invalid user in common name']
    else:
        assert data['common_name'] == ['Invalid user in common name']


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('email', [', tamirok@mail.com', 'tamir=ok@mail.ru', 'tamirok/@mail.com', 'tamirok@mail'])
def test_mobvpn_empty_email(crt_client, users, settings, email, path):
    requester = 'normal_user'
    crt_client.login(requester)
    request_data = {
        'type': CERT_TYPE.MOBVPN,
        'ca_name': CA_NAME.TEST_CA,
        'secret': settings.MOBVPN_SECRET_TOKEN,
    }
    content = {
        'country': 'RU',
        'city': 'Moscow',
        'unit': 'Infra',
        'email': email,
        'common_name': 'normal_user@ld.yandex.ru',
    }
    csr_config = FullSubjectCsrConfig(**content)
    request_data['request'] = force_text(csr_config.get_csr())
    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    data = response.json()
    assert data['request'] == ['Invalid email address']


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('email', [', invalid@mail.com', 'in=valid@mail.ru', 'invalid/@mail.com', 'invalid@mail'])
def test_linux_pc_invalid_email(crt_client, users, settings, email, path):
    requester = 'normal_user'
    crt_client.login(requester)
    request_data = {
        'type': CERT_TYPE.LINUX_PC,
        'ca_name': CA_NAME.TEST_CA,
        'pc_os': 'some_os',
        'pc_mac': 'some_mac',
        'pc_hostname': 'some_hostname',
    }
    content = {
        'country': 'RU',
        'city': 'Moscow',
        'unit': 'Infra',
        'email': email,
        'common_name': 'normal_user@ld.yandex.ru',
    }
    csr_config = FullSubjectCsrConfig(**content)
    request_data['request'] = force_text(csr_config.get_csr())
    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    data = response.json()
    assert data['request'] == ['Invalid email address']


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('oid', ['common_name', 'email_address'])
def test_linux_pc_multiply_oid(crt_client, users, path, oid):
    requester = 'normal_user'
    user = users[requester]
    crt_client.login(user)
    csr_builder = (
        CsrBuilder()
        .add_common_name('normal_user@ld.yandex.ru')
        .add_email_address('test@ld.yandex.ru')
        .add_unit_name('Infra')
    )
    if oid == 'common_name':
        csr_builder.add_common_name('normal_user_2@ld.yandex.ru')
    elif oid == 'email_address':
        csr_builder.add_email_address('another@ld.yandex.ru')

    request_data = {
        'type': CERT_TYPE.LINUX_PC,
        'ca_name': CA_NAME.TEST_CA,
        'pc_os': 'some_os',
        'pc_mac': 'some_mac',
        'pc_hostname': 'some_hostname',
        'request': force_text(csr_builder.get_pem_csr()),
    }
    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    data = response.json()
    if oid == 'common_name':
        assert data['request'] == ['Multiply Common Name oid']
    elif oid == 'email_address':
        assert data['request'] == ['Multiply email oid']


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_zomb_cert(crt_client, users, path):
    for common_name in ('a-b.c.wst.yandex.net', 'AZ09.zombie.yandex.net'):
        request_data = make_request_data(common_name=common_name)
        zomb_pc_user = users['zomb_pc_user']
        crt_client.login(zomb_pc_user.username)
        response = crt_client.json.post(path, data=request_data)

        assert response.status_code == status.HTTP_201_CREATED
        response_data = response.json()
        assert response_data['type'] == CERT_TYPE.ZOMB_PC
        assert Certificate.objects.last().user == zomb_pc_user


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_zomb_cert_wrong_name(crt_client, users, path):
    for common_name in ('a-b.cwst.yandex.net', 'А.zombie.yandex.net', 'a.zombie.google.net'):
        request_data = make_request_data(common_name=common_name)
        zomb_pc_user = users['zomb_pc_user']
        crt_client.login(zomb_pc_user.username)
        response = crt_client.json.post(path, data=request_data)
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        response_data = response.json()
        assert response_data['request'] == ['Invalid common name']


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_zomb_cert_wrong_st_id(crt_client, users, path):
    request_data = make_request_data(common_name='a-b.c.wst.yandex.net', st_id='1-3')
    zomb_pc_user = users['zomb_pc_user']
    crt_client.login(zomb_pc_user.username)
    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    response_data = response.json()
    assert response_data['hardware_request_st_id'] == ['st-id should be a valid tracker key']


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_zomb_cert_normal_user(crt_client, users, path):
    request_data = make_request_data(common_name='a-b.c.wst.yandex.net', st_id='a-3')
    user = users['normal_user']
    crt_client.login(user.username)
    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_403_FORBIDDEN


@pytest.mark.parametrize('has_perm,user_is_external,success_expected', [
    (False, False, False),
    (True, False, False),
    (True, True, True),
])
@pytest.mark.parametrize('by_csr', [True, False])
@pytest.mark.parametrize('cert_type', CERT_TYPE.DEVICE_TYPES)
@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@mock.patch('intranet.crt.users.models.old_get_inums_and_models')
def test_ext_helpdesk_requests_pc_certificate_for_external(mocked_bot, crt_client, users, user_datas,
                                                           by_csr, pc_csrs, path, cert_type, has_perm,
                                                           user_is_external, success_expected):
    # Не могут быть запрошены по CSR
    if by_csr and cert_type in (CERT_TYPE.ASSESSOR,):
        return
    # Запрашиваются только по CSR
    if not by_csr and cert_type in (CERT_TYPE.LINUX_TOKEN, CERT_TYPE.PC, CERT_TYPE.VPN_TOKEN):
        return

    requester = users['another_user']
    if has_perm:
        perm = Permission.objects.get(codename='can_issue_device_certificates_for_external')
        requester.user_permissions.add(perm)

    user = users['normal_user']
    if user_is_external:
        user.affiliation = AFFILIATION.EXTERNAL
        user.save()

    common_names = {
        CERT_TYPE.ZOMBIE: 'zomb-user@ld.yandex.ru',
        CERT_TYPE.ASSESSOR: 'normal_user',
    }
    common_name = common_names.get(cert_type, 'normal_user@ld.yandex.ru')

    content = {
        'country': 'RU',
        'city': 'Moscow',
        'unit': 'Infra',
        'email': 'normal_user@yandex-team.ru',
        'common_name': common_name,
    }

    if cert_type == CERT_TYPE.ZOMBIE:
        success_expected = False
        content.update({'unit': 'zomb'})

    if cert_type == CERT_TYPE.VPN_TOKEN:
        mocked_bot.return_value = []

    csr = force_text(FullSubjectCsrConfig(**content).get_csr())

    request_data = {
        'type': cert_type,
        'ca_name': CA_NAME.TEST_CA,
        'pc_os': 'Mac OS X 10.12',
        'pc_hostname': 'mac_user02',
        'pc_serial_number': '1111',
        'pc_mac': '111',
    }
    if by_csr:
        request_data['request'] = csr
    else:
        request_data['common_name'] = common_name

    crt_client.login(requester.username)
    with mock.patch('intranet.crt.api.base.serializer_mixins.personal.get_inum_by_sn') as get_inum_by_sn:
        get_inum_by_sn.return_value = 'inum'
        response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_201_CREATED if success_expected else status.HTTP_403_FORBIDDEN

    response_data = response.json()
    if success_expected:
        assert response_data['type'] == cert_type
        assert response_data['status'] == CERT_STATUS.ISSUED
        if path == '/api/certificate/':
            assert response_data['username'] == user.username
            assert response_data['requester'] == requester.username
        else:
            assert response_data['user'] == user_datas[user.username]
            assert response_data['requester'] == user_datas[requester.username]
    else:
        assert response.json() == {'detail': 'You do not have permission to perform this action.'}
