import os
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'intranet.crt.settings')


import django
django.setup()

import datetime
import collections
import ldap
import mock

import pytest
from django.conf import settings
from django.utils import timezone
from django.core.cache import cache
from django.utils.encoding import force_text

from intranet.crt.constants import AFFILIATION, CERT_TYPE, TAG_SOURCE, CERT_STATUS, TAG_FILTER_TYPE
from intranet.crt.core.models import HostToApprove
from intranet.crt.csr import FullSubjectCsrConfig
from __tests__.utils.common import create_certificate, create_tag_filter


@pytest.fixture(autouse=True)
def no_external_calls(monkeypatch):
    """Для тестов никуда по урлам не ходим, все локально"""
    def opener(patched):
        def wrapper(url, *args, **kwargs):
            raise Exception("You forget to mock %s locally, it tries to open %s" % (patched, url))
        return wrapper
    monkeypatch.setattr('requests.request', opener('requests.request'))

    def get_class(patched):
        def wrapper(*args, **kwargs):
            raise Exception("You forget to mock %s locally" % patched)
        return wrapper
    # monkeypatch.setattr('urllib.URLopener', get_class('urllib.URLopener'))
    # monkeypatch.setattr('httplib.HTTPConnection', get_class('httplib.HTTPConnection'))


@pytest.fixture()
def crt_client(request, settings):
    from __tests__.utils.common import CrtClient

    client = CrtClient()

    def login(username):
        settings.YAUTH_TEST_USER = {'login': username}

    def logout():
        settings.YAUTH_TEST_USER = False

    client.login = login
    client.logout = logout
    return client


@pytest.fixture(autouse=True)
def certificate_types(transactional_db):
    from __tests__.utils.common import create_certificate_type

    active_type_names = [type_attrs[0] for type_attrs in CERT_TYPE.choices()]
    types = {type_name: create_certificate_type(type_name) for type_name in active_type_names}

    types['inactive_type'] = create_certificate_type('inactive_type', is_active=False)

    return types


@pytest.fixture()
def users():
    from __tests__.utils.common import create_user, create_group, create_permission

    globalsign_issuer_perm = create_permission('can_issue_globalsign', 'core', 'certificate')
    pc_cert_issuer_perm = create_permission('can_issue_device_certificates', 'core', 'certificate')
    external_pc_cert_issuer_perm = create_permission('can_issue_device_certificates_for_external', 'core', 'certificate')
    zomb_pc_issuer_prem = create_permission('can_issue_zomb_pc_certificates', 'core', 'certificate')
    client_server_cert_issuer_perm = create_permission('can_issue_client_server_certificates', 'core', 'certificate')
    bank_client_server_cert_issuer_perm = create_permission('can_issue_bank_client_server_certificates', 'core',
                                                           'certificate')
    revoke_perm = create_permission('can_revoke_any_certificate', 'core', 'certificate')
    revoke_only_users_perm = create_permission('can_revoke_users_certificates', 'core', 'certificate')
    rc_server_issuer_perm = create_permission('can_issue_rc_server_certificates', 'core', 'certificate')
    yc_server_issuer_perm = create_permission('can_issue_yc_server_certificates', 'core', 'certificate')
    mdb_issuer_perm = create_permission('can_issue_mdb_certificates', 'core', 'certificate')
    hypercube_server_issuer_perm = create_permission('can_issue_hypercube_certificates', 'core', 'certificate')
    cb_internal_ca_issuer_perm = create_permission('can_issue_cb_internal_ca', 'core', 'certificate')
    tpm_smartcard_1c_issuer_perm = create_permission('can_issue_tpm_smartcard_1c_certificates',
                                                     'core', 'certificate')
    approve_perm = create_permission('change_approverequest', 'core', 'approverequest')
    temp_pc_certificate_perm = create_permission('can_issue_temp_pc_certificates', 'core', 'certificate')
    can_issue_imdm_certificates_perm = create_permission('can_issue_imdm_certificates', 'core', 'certificate')
    can_issue_market_zombie_certificates_perm = create_permission('can_issue_market_zombie_certificates', 'core', 'certificate')
    can_issue_vpn_1d_certificates_perm = create_permission('can_issue_vpn_1d_certificates', 'core', 'certificate')
    can_issue_vpn_token_certificates_for_self = create_permission('can_issue_vpn_token_certificates_for_self', 'core', 'certificate')
    bank_pc_cert_issuer_perm = create_permission('can_issue_bank_pc_certificates', 'core', 'certificate')
    sdc_cert_issuer_perm = create_permission('can_issue_sdc_certificates', 'core', 'certificate')

    helpdesk_group = create_group('helpdesk', permissions=[
        pc_cert_issuer_perm,
        bank_pc_cert_issuer_perm,
        bank_client_server_cert_issuer_perm,
        revoke_only_users_perm,
        temp_pc_certificate_perm,
    ])
    globalsign_group = create_group('globalsign', permissions=[globalsign_issuer_perm])
    external_helpdesk_group = create_group('external_helpdesk', permissions=[external_pc_cert_issuer_perm])
    revoke_all_group = create_group('revoke_all', permissions=[revoke_perm])
    zomb_pc_group = create_group('zomb_pc', permissions=[zomb_pc_issuer_prem])
    rc_server_group = create_group('rc_server', permissions=[rc_server_issuer_perm])
    yc_server_group = create_group('yc_server', permissions=[yc_server_issuer_perm])
    mdb_group = create_group('mdb', permissions=[mdb_issuer_perm])
    infosec_group = create_group('infosec', permissions=[approve_perm])
    noc_group = create_group('noc', permissions=[client_server_cert_issuer_perm])
    hypercube_group = create_group('hypercube', permissions=[hypercube_server_issuer_perm, cb_internal_ca_issuer_perm])
    tpm_smartcard_1c_group = create_group('tpm_smartcard_1c_issue', permissions=[tpm_smartcard_1c_issuer_perm])
    temp_pc_group = create_group('temp_pc_issue', permissions=[temp_pc_certificate_perm])
    imdm_group = create_group('imdm_group', permissions=[can_issue_imdm_certificates_perm])
    vpn_1d_group = create_group('vpn_1d_group', permissions=[can_issue_vpn_1d_certificates_perm])

    helpdesk_user = 'helpdesk_user'
    external_helpdesk_user = 'external_helpdesk_user'
    globalsign_user = 'globalsign_user'
    zomb_pc_user = 'zomb_pc_user'
    normal_user = 'normal_user'
    another_user = 'another_user'
    tag_user = 'tag_user'
    rc_server_user = 'rc_server_user'
    yc_server_user = 'yc_server_user'
    mdb_user = 'mdb_user'
    zomb_user = 'zomb-user'
    zomb_fake_user = 'zomb-user-fake'
    dismissed_user = 'dismissed-user'
    infosec_user = 'infosec-user'
    noc_user = 'noc_user'
    hypercube_user = 'hypercube_user'
    revoke_all_user = 'revoke_all_user'
    tpm_smartcard_1c_user = 'tpm_smartcard_1c_user'
    temp_pc_user = 'temp_pc_user'
    vpn_1d_user = 'vpn_1d_user'
    robot_user = 'robot-user'
    super_user = 'super_user'
    external_user = 'external_user'

    adventure_time_user_names = [
        'bubblegum',
        'flame',
        'lumpy_space',
        'ghost',
        'hot_dog',
        'finn',
        'jake',
        'bmo',
        'shelby',
        'lemongrab',
        'banana_guard',
        'cinnamon_bun',
    ]

    new_users = {
        tag_user: create_user(tag_user),
        normal_user: create_user(normal_user),
        another_user: create_user(another_user),
        external_user: create_user(external_user, affiliation=AFFILIATION.EXTERNAL),
        helpdesk_user: create_user(helpdesk_user, groups=[helpdesk_group]),
        globalsign_user: create_user(globalsign_user, groups=[globalsign_group]),
        external_helpdesk_user: create_user(external_helpdesk_user, groups=[external_helpdesk_group]),
        zomb_pc_user: create_user(zomb_pc_user, groups=[zomb_pc_group]),
        rc_server_user: create_user(rc_server_user, groups=[rc_server_group]),
        yc_server_user: create_user(yc_server_user, groups=[yc_server_group]),
        tpm_smartcard_1c_user: create_user(tpm_smartcard_1c_user, groups=[tpm_smartcard_1c_group]),
        mdb_user: create_user(mdb_user, groups=[mdb_group]),
        zomb_user: create_user(zomb_user, is_robot=True),
        zomb_fake_user: create_user(zomb_fake_user, is_robot=False),
        dismissed_user: create_user(dismissed_user, is_active=False),
        infosec_user: create_user(infosec_user, is_staff=True, groups=[infosec_group]),
        noc_user: create_user(noc_user, groups=[noc_group]),
        hypercube_user: create_user(hypercube_user, groups=[hypercube_group]),
        temp_pc_user: create_user(temp_pc_user, groups=[temp_pc_group]),
        vpn_1d_user: create_user(vpn_1d_user, groups=[vpn_1d_group]),
        revoke_all_user: create_user(revoke_all_user, groups=[revoke_all_group]),
        robot_user: create_user(robot_user, is_robot=True),
        super_user: create_user(super_user, is_superuser=True)
    }

    new_users.update({username: create_user(username) for username in adventure_time_user_names})

    return new_users


@pytest.fixture
def user_datas(users):
    return {
        username: {
            'username': username,
            'first_name': {'en': user.first_name, 'ru': user.first_name_ru},
            'last_name': {'en': user.last_name, 'ru': user.last_name_ru},
            'is_active': user.is_active,
            'in_hiring': user.in_hiring,
        }
        for (username, user) in users.items()
    }


@pytest.fixture
def helpdesk_user(users):
    return users['helpdesk_user']


@pytest.fixture()
def pc_csrs():
    def make_csr_config(login):
        return force_text(FullSubjectCsrConfig(
            common_name=f'{login}@ld.yandex.ru',
            email=f'{login}@yandex-team.ru',
            country='RU',
            city='Moscow',
            unit='Infra',
        ).get_csr())

    logins = ['normal_user', 'external_user']
    return {login: make_csr_config(login) for login in logins}


@pytest.fixture()
def client_server_csrs():
    # CN client-server сертификата
    return [force_text(FullSubjectCsrConfig(
        common_name='client-server.yndx.net',
        email='normal_user@yandex-team.ru',
        country='RU',
        city='Moscow',
        unit='Infra',
    ).get_csr()) for i in range(2)]


@pytest.fixture()
def bank_client_server_csrs():
    # CN bank-client-server сертификата
    return [force_text(FullSubjectCsrConfig(
        common_name='xxx.yandex-bank.net',
        email='normal_user@yandex-team.ru',
        country='RU',
        city='Moscow',
        unit='Infra',
    ).get_csr()) for i in range(2)]


@pytest.fixture
def tag_filters():
    from __tests__.utils.common import create_tag_filter

    filter_names = [
        'pc_filter',
        'mobile_filter',
        'tag1_filter',
        'tag2_filter',
        'tag3_filter',
    ]

    adventure_time_filter_names = [
        'princesses',
        'tree_fort',
        'candy_people',
        'main_characters',
    ]

    filter_names.extend(adventure_time_filter_names)

    return {filter_name: create_tag_filter(name=filter_name) for filter_name in filter_names}


@pytest.fixture()
def tags(certificate_types, tag_filters):
    from __tests__.utils.common import create_certificate_tag

    pc_type = certificate_types[CERT_TYPE.PC]
    mobile_type = certificate_types[CERT_TYPE.MOBVPN]

    adventure_time_tag_names = [
        ('candy', 10),
        ('all_access', 20),
        ('ice_king_protect', 40),
        ('evil_protect', 40),
        ('gold', 50),
        ('power', 60),
    ]

    new_tags = {
        'pc_tag': create_certificate_tag('pc_tag', filters=[tag_filters['pc_filter']], types=[pc_type], priority=20),
        'mobile_tag': create_certificate_tag('mobile_tag', filters=[tag_filters['mobile_filter']], types=[mobile_type]),
        'tag1': create_certificate_tag('tag1', filters=[tag_filters['tag1_filter']], types=[pc_type], priority=50),
        'tag2': create_certificate_tag('tag2', filters=[tag_filters['tag2_filter']], types=[pc_type], priority=40),
        'tag3': create_certificate_tag('tag3', filters=[tag_filters['tag3_filter']], types=[pc_type], priority=30),
    }

    for tag_name, priority in adventure_time_tag_names:
        new_tags[tag_name] = create_certificate_tag(tag_name, priority=priority)

    return new_tags


@pytest.fixture()
def new_noc_certificates(certificate_types, users, tags, source=TAG_SOURCE.MANUAL):
    from __tests__.utils.common import create_certificate

    normal_user = users['normal_user']
    pc_type = certificate_types[CERT_TYPE.PC]
    common_name = 'pc@cert'

    serial_number_tags = collections.OrderedDict((
        ('A1', [tags['tag1'], tags['tag2'], tags['tag3']]),
        ('A2', [tags['tag1'], tags['tag2'], tags['tag3']]),
        ('A3', [tags['tag1'], tags['tag3']]),
        ('A7', [tags['tag1'], tags['tag2'], tags['tag3']]),
    ))

    noc_certificates = []
    now = timezone.now()

    for i, (serial_number, tags) in enumerate(serial_number_tags.items()):
        added = now - datetime.timedelta(days=i)
        certificate = create_certificate(
            normal_user, pc_type,
            common_name=common_name,
            serial_number=serial_number,
            added=added,
        )
        for tag in tags:
            certificate.add_tag(tag, source)

            for tag_filter in tag.filters.all():
                tag_filter.users.add(normal_user)
        noc_certificates.append(certificate)
    return noc_certificates


@pytest.fixture()
def crt_robot():
    from __tests__.utils.common import create_user

    robot = create_user(settings.CRT_ROBOT, is_robot=True)
    robot.email = 'robot-crt@yandex-team.ru'
    robot.is_superuser = True
    robot.save()
    return robot


@pytest.fixture
def abc_services():
    services = [
        {
            'id': 2,
            'created_at': '2016-12-31T23:59:59+00:00',
            'modified_at': '2017-04-02T07:32:43+00:00',
            'slug': 'test_service',
            'name': {
                'ru': 'Тестовый сервис',
                'en': 'Test service'
            },
            'owner': {
                'id': '7',
                'login': 'testuser',
                'first_name': 'Джеймс',
                'last_name': 'Бонд',
                'uid': '1111111111111111'
            },
            'parent': {
                'id': 1,
                'slug': 'parent_service',
                'name': {
                    'ru': 'Родительский сервис',
                    'en': 'Parent service'
                },
                'parent': None
            },
            'path': '/parent_service/test_service/',
            'readonly_state': None,
            'state': 'develop',
        },
        {
            'id': 1,
            'created_at': '2015-12-31T23:59:59+00:00',
            'modified_at': '2017-04-02T07:32:43+00:00',
            'slug': 'parent_service',
            'name': {
                    'ru': 'Родительский сервис',
                    'en': 'Parent service'
                },
            'owner': {
                'id': '66',
                'login': 'anotheruser',
                'first_name': 'Иван',
                'last_name': 'Иванов',
                'uid': '1111111111111112'
            },
            'parent': None,
            'path': '/parent_service/',
            'readonly_state': None,
            'state': 'develop',
        },
        {
            'id': 3,
            'created_at': '2016-12-31T23:59:58+00:00',
            'modified_at': '2017-06-02T07:32:43+00:00',
            'slug': 'another_service',
            'name': {
                'ru': 'Другой сервис',
                'en': 'Another service'
            },
            'owner': {
                'id': '1',
                'login': 'neo',
                'first_name': 'Томас',
                'last_name': 'Андерсон',
                'uid': '1111111111111111'
            },
            'parent': {
                'id': 1,
                'slug': 'parent_service',
                'name': {
                    'ru': 'Родительский сервис',
                    'en': 'Parent service'
                },
                'parent': None
            },
            'path': '/parent_service/another_service/',
            'readonly_state': None,
            'state': 'deleted',
        },
    ]

    from django_abc_data.tests.utils import patch_ids_repo as patch_abc_ids_repo
    with patch_abc_ids_repo(services=services):
        from django_abc_data.core import sync_services
        sync_services()

    return services


@pytest.fixture(autouse=True)
def autodrop_cache():
    cache.clear()


@pytest.fixture
def certificates(users, crt_robot, certificate_types):
    type_imdm = certificate_types[CERT_TYPE.IMDM]

    create_certificate(crt_robot, type_imdm, pc_serial_number='s1', serial_number='1')
    create_certificate(users['normal_user'], type_imdm, pc_serial_number='s1', serial_number='2')
    create_certificate(crt_robot, type_imdm, pc_serial_number='s1', serial_number='3')
    create_certificate(crt_robot, type_imdm, pc_serial_number='s1', status=CERT_STATUS.REVOKED, serial_number='4')

    create_certificate(crt_robot, type_imdm, pc_serial_number='s2', serial_number='5')
    create_certificate(crt_robot, type_imdm, pc_serial_number='s2', status=CERT_STATUS.ERROR, serial_number='6')
    create_certificate(crt_robot, type_imdm, pc_serial_number='s2', serial_number='7')
    create_certificate(crt_robot, certificate_types[CERT_TYPE.PC], pc_serial_number='s2')

    create_certificate(crt_robot, type_imdm, pc_serial_number='s3')

    create_certificate(crt_robot, type_imdm, pc_serial_number='s4', status=CERT_STATUS.REVOKED)
    create_certificate(crt_robot, type_imdm, pc_serial_number='s5', status=CERT_STATUS.REVOKED)


def make_user_ld_cert(user, cert_type):
    return create_certificate(
        user,
        cert_type,
        common_name=f'{user.username}@ld.yandex.ru',
    )


@pytest.fixture()
def normal_user_ld_cert(users, certificate_types):
    return make_user_ld_cert(users['normal_user'], certificate_types[CERT_TYPE.PC])


@pytest.fixture()
def temp_pc_user_ld_cert(users, certificate_types):
    return make_user_ld_cert(users['temp_pc_user'], certificate_types[CERT_TYPE.PC])


@pytest.fixture()
def hypercube_user_ld_cert(users, certificate_types):
    return make_user_ld_cert(users['hypercube_user'], certificate_types[CERT_TYPE.PC])


@pytest.fixture()
def helpdesk_user_ld_cert(users, certificate_types):
    return make_user_ld_cert(users['helpdesk_user'], certificate_types[CERT_TYPE.PC])


@pytest.fixture()
def super_user_ld_cert(users, certificate_types):
    return make_user_ld_cert(users['super_user'], certificate_types[CERT_TYPE.PC])


@pytest.fixture()
def zomb_pc_user_ld_cert(users, certificate_types):
    return make_user_ld_cert(users['zomb_pc_user'], certificate_types[CERT_TYPE.PC])


@pytest.fixture()
def finn_ld_cert(users, certificate_types):
    return make_user_ld_cert(users['finn'], certificate_types[CERT_TYPE.PC])


@pytest.fixture
def idm_tag_filters():
    return {
        name: create_tag_filter(name, type=TAG_FILTER_TYPE.IDM_SYSTEM)
        for name in ('Office.VPN', 'Office.8021X.Staff', 'Office.WiFi.Yandex')
    }


@pytest.fixture(autouse=True)
def hosts_to_approve():
    domains = ['yandex-team.ru', 'yandex.ru', 'yandex.net', 'yndx.net', 'yandex-team.com']
    HostToApprove.objects.bulk_create(HostToApprove(host=host) for host in domains)


@pytest.fixture()
def mocked_ldap(monkeypatch):
    def mocked_search_s(*args, **kwargs):
        distinguished_names = {
            'CN=Normal User,CN=Users,DC=ld,DC=yandex,DC=ru': b'normal_user',
            'CN=Another User,OU=ForeignUsers,DC=ld,DC=yandex,DC=ru': b'another_user',
        }
        dn_str = kwargs['filterstr'].strip('()').split('=', 1)[-1]
        result = distinguished_names.get(dn_str, None)
        if not result:
            raise ldap.NO_RESULTS_RETURNED
        return [(dn_str, {'sAMAccountName': [result]})]

    mock_ldap_client = mock.Mock()
    mock_ldap_client.search_s = mocked_search_s

    mock_get_client = mock.Mock(return_value=mock_ldap_client)

    monkeypatch.setattr('intranet.crt.utils.ldap.ldap.initialize', mock_get_client)
