import json
import mock
import pytest

from django.conf import settings
from django.utils.encoding import force_text

from intranet.crt.core.models import Certificate, CertificateType
from intranet.crt.constants import AFFILIATION, CERT_TYPE, CERT_EXTENSION, CA_NAME
from intranet.crt.csr import FullSubjectCsrConfig, TpmSmartcard1CSubjectCsrConfig, TempPcCsrConfig
from intranet.crt.tags.models import CertificateTag, TagFilter
from __tests__.utils.common import attrdict, create_permission, create_group
from intranet.crt.utils.ssl import PemCertificate, get_x509_custom_extensions


@pytest.fixture()
def noc_tags():
    tags = [
        ('Office.VPN', 0),
        ('Office.VPN.NoSplit', 0),
        ('Office.WiFi.PDAS', 0),
        ('Office.WiFi.Yandex', 0),
        ('wired.longreauth', 0),
        ('Office.8021X.Zombie', 10),
        ('Office.8021X.Admins', 20),
        ('Office.8021X.Helpdesk', 30),
        ('Office.8021X.Fin', 50),
        ('Office.8021X.Sales', 50),
        ('Office.8021X.Probki', 60),
        ('Office.8021X.Developers', 90),
        ('TmpAuth', 100),
        ('Office.8021X.Staff', 1000),
    ]
    CertificateTag.objects.bulk_create([
        CertificateTag(
            name=name,
            priority=priority,
            type='noc_use',
            is_active=True,
        ) for name, priority in tags
    ])
    return {noc_tag.name: noc_tag for noc_tag in CertificateTag.objects.all()}


@pytest.fixture()
def tag_filters():
    tag_filters = [
        ('all', 'staff_filter', 'd69121d68e950ce59a6bf92e74101650a817e85a'),
        ('admins', 'staff_filter', '467970ac16e38986e01de8a46acd10ef43a2a016'),
        ('helpdesk', 'staff_filter', 'f04033ab284f2816b5f4382feabb8a2724268546'),
        ('zombie', 'staff_filter', '4f01bf60db50e241477f22d93950b6f7522f0dc0'),
        ('YaStaff', 'staff_filter', 'e07ad89e9cf4db46bcc8cf296f25da47777482d4'),
        ('market', 'staff_filter', '39df39cab468c35006318a9a2fe2ecddda39b2e3'),
        ('taxi', 'staff_filter', 'fcdf92ed51b4ca6448b2b2b2d72eed27225ff69d'),
        ('fin', 'staff_filter', '75ce90e8ec65f792997c61d5e310cc090dcdfc89'),
        ('exp', 'staff_filter', 'a9d9fca97191c56330eb9c0a792520ae34509378'),
        ('noc', 'staff_filter', 'd5eedda1746387bf3fe13db4616c1d21953c5104'),
        ('outstaff', 'staff_filter', '35b3285080159d602b1968ab864b93e41db3c208'),
        ('Ext', 'staff_filter', '56d7582b52e4fa6af613d726e8ea8a6cd3a55631'),
        ('normal_user', 'staff_api', 'login=normal_user'),
        ('another_user', 'staff_api', 'login=another_user'),
    ]
    TagFilter.objects.bulk_create([
        TagFilter(name=name, type=type, filter=filter, is_active=True) for name, type, filter in tag_filters
    ])
    return attrdict({filter.name: filter for filter in TagFilter.objects.all()})


@pytest.fixture()
def filters_with_tags(noc_tags, tag_filters):
    tag_filters.all.tags.add(noc_tags['Office.WiFi.Yandex'])
    tag_filters.all.tags.add(noc_tags['Office.VPN'])
    tag_filters.noc.tags.add(noc_tags['wired.longreauth'])
    tag_filters.zombie.tags.add(noc_tags['Office.8021X.Zombie'])
    tag_filters.admins.tags.add(noc_tags['Office.8021X.Admins'])
    tag_filters.helpdesk.tags.add(noc_tags['Office.8021X.Helpdesk'])
    tag_filters.fin.tags.add(noc_tags['Office.8021X.Fin'])
    tag_filters.taxi.tags.add(noc_tags['Office.8021X.Probki'])
    tag_filters.exp.tags.add(noc_tags['Office.8021X.Developers'])
    tag_filters.YaStaff.tags.add(noc_tags['Office.8021X.Staff'])

    CertificateTag.filter_cert_types.through.objects.bulk_create([
        CertificateTag.filter_cert_types.through(
            certificatetag=cert_tag,
            certificatetype=cert_type,
        ) for cert_tag in CertificateTag.objects.all() for cert_type in CertificateType.objects.all()])

    return attrdict({filter.name: filter for filter in TagFilter.objects.all()})


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('username', ['normal_user', 'another_user', 'noc_user'])
def test_pem_contains_tag_oids(crt_client, users, filters_with_tags, username, path):
    normal_user = users['normal_user']
    another_user = users['another_user']
    noc_user = users['noc_user']
    filters_with_tags.all.users.add(normal_user, another_user, noc_user)
    filters_with_tags.YaStaff.users.add(normal_user, another_user, noc_user)
    filters_with_tags.noc.users.add(noc_user)
    filters_with_tags.exp.users.add(another_user)

    requester = users[username]
    crt_client.login(requester)
    request_data = {
        'common_name': '{}@ld.yandex.ru'.format(requester.username),
        'type': CERT_TYPE.LINUX_PC,
        'ca_name': CA_NAME.TEST_CA,
        'pc_os': 'some_os',
        'pc_mac': 'some_mac',
        'pc_hostname': 'some_hostname',
    }
    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == 201

    cert = Certificate.objects.last()
    custom_extensions = get_x509_custom_extensions(PemCertificate(cert.certificate).x509_object)
    custom_extensions = {key: value.value for key, value in list(custom_extensions.items())}

    expected_tags = {
        'normal_user': {'vpn_tags': b'Default', 'wired_tags': b'Staff', 'wireless_tags': b'Yandex'},
        'another_user': {'vpn_tags': b'Default', 'wired_tags': b'Developers,Staff', 'wireless_tags': b'Yandex'},
        'noc_user': {'vpn_tags': b'Default', 'wired_tags': b'longreauth,Staff', 'wireless_tags': b'Yandex'}
    }

    assert custom_extensions == expected_tags[username]

    expected_builtin_tags = {
        'normal_user': 'vpn_tags: Default; wired_tags: Staff; wireless_tags: Yandex',
        'another_user': 'vpn_tags: Default; wired_tags: Developers,Staff; wireless_tags: Yandex',
        'noc_user': 'vpn_tags: Default; wired_tags: longreauth,Staff; wireless_tags: Yandex',
    }

    for url in ['/api/certificate/{}/', '/api/v2/certificate/{}/']:
        response = crt_client.json.get(url.format(cert.pk), data=request_data).json()
        assert response['builtin_tags'] == expected_builtin_tags[username]

    for value in None, '', 'no_pem_data':
        Certificate.objects.filter(pk=cert.pk).update(certificate=value)
        response = crt_client.json.get(url.format(cert.pk), data=request_data).json()
        assert response['builtin_tags'] == ''


@pytest.mark.parametrize('oid_permission', [False, True])
@pytest.mark.parametrize('requester_username', ['normal_user', 'helpdesk_user'])
@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('cert_type', CERT_TYPE.CSR_REQUESTABLE_TYPES)
@mock.patch('intranet.crt.users.models.old_get_inums_and_models')
def test_tag_oids_issue_by_csr_permission_required(mocked_bot, mocked_ldap, crt_client, users, path,
                                                   cert_type, requester_username, oid_permission, abc_services,
                                                   normal_user_ld_cert, helpdesk_user_ld_cert):
    helpdesk_user = users['helpdesk_user']

    permissions = [
        'can_issue_device_certificates',
        'can_issue_rc_server_certificates',
        'can_issue_yc_server_certificates',
        'can_issue_zomb_pc_certificates',
        'can_issue_client_server_certificates',
        'can_issue_bank_client_server_certificates',
        'can_issue_hypercube_certificates',
        'can_issue_sdc_certificates',
        'can_issue_imdm_certificates',
        'can_issue_tpm_smartcard_1c_certificates',
        'can_issue_temp_pc_certificates',
        'can_issue_bank_pc_certificates',
        'can_issue_vpn_token_certificates_for_self',
    ]
    if oid_permission:
        permissions.append('can_issue_certificate_with_tag_oids')
    issuer_group = create_group('issuer_group', permissions=[
        create_permission(permission, 'core', 'certificate') for permission in permissions
    ])
    helpdesk_user.groups.add(issuer_group)

    requester = users[requester_username]
    crt_client.login(requester)
    content = {
        'country': 'RU',
        'city': 'Moscow',
        'unit': 'Infra',
        'email': '{}@yandex-team.ru'.format(requester.username),
    }
    if cert_type in (CERT_TYPE.CLIENT_SERVER, CERT_TYPE.HOST):
        content['common_name'] = 'xxx.yndx.net'
    else:
        content['common_name'] = '{}@ld.yandex.ru'.format(requester.username)

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
    csr_config.update_extensions_with_context({'wired_tags': ['Fin', 'Yandex']})
    request_data = {
        'type': cert_type,
        'ca_name': CA_NAME.TEST_CA,
        'request': str(csr_config.get_csr(), encoding='utf-8'),
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
        csr_config = TpmSmartcard1CSubjectCsrConfig(
            cn_1='Normal User',
            cn_2='Users',
            dc_1='ld',
            dc_2='yandex',
            dc_3='ru',
        )
        csr_config.update_extensions_with_context({'wired_tags': ['Fin', 'Yandex']})

        request_data = {
            'type': CERT_TYPE.TPM_SMARTCARD_1C,
            'ca_name': CA_NAME.TEST_CA,
            'request': force_text(csr_config.get_csr()),
        }
    if cert_type == CERT_TYPE.TEMP_PC:
        csr_config = TempPcCsrConfig(common_name='helpdesk_user@pda-ld.yandex.ru')
        csr_config.update_extensions_with_context({'wired_tags': ['Fin', 'Yandex']})
        csr = force_text(csr_config.get_csr())

        request_data = {
            'type': CERT_TYPE.TEMP_PC,
            'ca_name': CA_NAME.TEST_CA,
            'request': csr,
        }
    if cert_type == CERT_TYPE.VPN_TOKEN:
        helpdesk_user.affiliation = AFFILIATION.EXTERNAL
        helpdesk_user.save()
        mocked_bot.return_value = []

    response = crt_client.json.post(path, data=request_data)

    if requester_username != 'helpdesk_user':
        assert response.status_code == 403
        if cert_type in CERT_TYPE.PUBLIC_REQUEST_TYPES:
            assert response.content == b'{"detail":"Issuing a certificate with tag OIDs requires special permission"}'
        else:
            assert response.content == b'{"detail":"You do not have permission to perform this action."}'
    else:
        if cert_type in CERT_EXTENSION.PERMITTED_IN_PUBLIC_CSR and not oid_permission:
            assert response.status_code == 403
            assert response.content == b'{"detail":"CSR extensions contains restricted values"}'
        elif oid_permission:
            assert response.status_code == 201
        else:
            assert response.status_code == 403
            assert response.content == b'{"detail":"Issuing a certificate with tag OIDs requires special permission"}'


@pytest.mark.parametrize('oid_permission', [False, True])
def test_tag_oids_reissue_by_csr_permission_not_required(crt_client, users, oid_permission):
    helpdesk_user = users['helpdesk_user']

    issuer_group = create_group('issuer_group', permissions=[
        create_permission('can_issue_certificate_with_tag_oids', 'core', 'certificate'),
    ])

    csr_context = {
        'country': 'RU',
        'city': 'Moscow',
        'unit': 'Infra',
        'email': '{}@yandex-team.ru'.format(helpdesk_user.username),
        'common_name': '{}@ld.yandex.ru'.format(helpdesk_user.username),
    }
    csr_config = FullSubjectCsrConfig(**csr_context)
    csr_config.update_extensions_with_context({'wired_tags': ['Fin', 'Yandex']})
    csr = force_text(csr_config.get_csr())

    request_data = {
        'type': CERT_TYPE.PC,
        'ca_name': CA_NAME.TEST_CA,
        'request': csr,
        'pc_os': 'some_os',
        'pc_mac': 'some_mac',
        'pc_hostname': 'some_hostname',
    }

    crt_client.login(helpdesk_user.username)

    helpdesk_user.groups.add(issuer_group)
    crt_client.json.post('/api/certificate/', data=request_data)
    if not oid_permission:
        helpdesk_user.groups.remove(issuer_group)

    old_cert = Certificate.objects.get()
    csr_subject = (
        '/C=RU/ST=Moscow/L=Moscow/O=Yandex/OU=Infra/CN={0}@ld.yandex.ru'
        '/emailAddress={0}@yandex-team.ru'.format(helpdesk_user.username)
    )
    reissue_params = {
        'HTTP_X_SSL_CLIENT_SERIAL': old_cert.serial_number,
        'HTTP_X_SSL_CLIENT_VERIFY': '0',
        'HTTP_X_SSL_CLIENT_SUBJECT': csr_subject,
        'data': {
            'request': csr,
        }
    }
    response = crt_client.json.post('/api/reissue/', **reissue_params)

    assert response.status_code == 201
    pem_bytes = response.json()['certificate']
    custom_extensions = get_x509_custom_extensions(PemCertificate(pem_bytes).x509_object)
    assert custom_extensions['wired_tags'].value == b'Fin,Yandex'


@pytest.mark.parametrize('cert_type', [CERT_TYPE.NINJA, CERT_TYPE.LINUX_PC, CERT_TYPE.HOST])
def test_filter_without_filtercerttype_affects_all_taggable(crt_client, users, noc_tags, tag_filters,
                                                            cert_type, abc_services):
    tag_probki = noc_tags['Office.8021X.Probki']
    tag_pdas = noc_tags['Office.WiFi.PDAS']

    normal_user = users['normal_user']
    tag_filters.normal_user.users.add(normal_user)
    TagFilter.objects.exclude(pk=tag_filters.normal_user.pk).delete()

    # PDAS попадет только в ninja
    tag_pdas.filters.add(tag_filters.normal_user)
    tag_pdas.filter_cert_types.add(CertificateType.objects.get(name=CERT_TYPE.NINJA))

    # Probki попадет в любые CERT_TYPE.TAGGABLE_TYPES
    tag_probki.filters.add(tag_filters.normal_user)

    requester = normal_user
    crt_client.login(requester)
    request_data = {
        'type': cert_type,
        'ca_name': CA_NAME.TEST_CA,
    }
    if cert_type == CERT_TYPE.LINUX_PC:
        request_data.update({
            'common_name': '{}@ld.yandex.ru'.format(requester.username),
            'pc_os': 'dummy',
            'pc_mac': 'dummy',
            'pc_hostname': 'dummy',
        })
    if cert_type == CERT_TYPE.NINJA:
        request_data.update({'common_name': '{}@pda-ld.yandex.ru'.format(requester.username)})

    if cert_type == CERT_TYPE.HOST:
        request_data.update({
            'common_name': 'hostname.yandex.ru',
            'hosts': 'hostname.yandex.ru',
            'abc_service': 1,
        })

    response = crt_client.json.post('/api/v2/certificates/', data=request_data)
    assert response.status_code == 201

    cert = Certificate.objects.get()
    custom_extensions = get_x509_custom_extensions(PemCertificate(cert.certificate).x509_object)
    custom_extensions = {key: value.value for key, value in custom_extensions.items()}

    expected_tags = {
        CERT_TYPE.NINJA: {'wired_tags': b'Probki', 'wireless_tags': b'PDAS'},
        CERT_TYPE.LINUX_PC: {'wired_tags': b'Probki'},
        CERT_TYPE.HOST: {},
    }
    assert custom_extensions == expected_tags[cert_type]

    response = crt_client.json.get('/api/v2/certificate/{}/'.format(cert.pk), data=request_data)
    result_tags = json.loads(response.content)['tags']
    result_tag_names = {tag_dict['name'] for tag_dict in result_tags}

    expected_tag_names = {
        CERT_TYPE.NINJA: {'Office.8021X.Probki', 'Office.WiFi.PDAS'},
        CERT_TYPE.LINUX_PC: {'Office.8021X.Probki'},
        CERT_TYPE.HOST: set(),
    }
    assert expected_tag_names[cert_type] == result_tag_names

    def staff_person_getiter(lookup):
        if 'normal_user' in lookup.values():
            return [{'login': 'normal_user'}]
        return []

    mock_getiter_path = 'ids.services.staff.repositories.person.StaffPersonRepository.getiter'
    with mock.patch(mock_getiter_path, side_effect=staff_person_getiter):
        from intranet.crt.tags.tasks.sync_filters_tags import SyncFilterTagsTask
        SyncFilterTagsTask.locked_stamped_run()

    response = crt_client.json.get('/api/v2/certificate/{}/'.format(cert.pk), data=request_data)
    result_tags = json.loads(response.content)['tags']
    result_tag_names = {tag_dict['name'] for tag_dict in result_tags}

    expected_tag_names = {
        CERT_TYPE.NINJA: {'Office.8021X.Probki', 'Office.WiFi.PDAS'},
        CERT_TYPE.LINUX_PC: {'Office.8021X.Probki'},
        CERT_TYPE.HOST: set(),
    }
    assert expected_tag_names[cert_type] == result_tag_names
