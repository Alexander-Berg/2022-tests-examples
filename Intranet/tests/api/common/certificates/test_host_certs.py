import json

import pytest

from constance.test import override_config
from django.db.models import Max
from django_abc_data.models import AbcService
from rest_framework import status

from intranet.crt.constants import CERT_TYPE, CERT_STATUS, CA_NAME, CERT_TEMPLATE
from intranet.crt.core.models import Certificate
from __tests__.utils.common import create_certificate
from intranet.crt.core.controllers.certificates.host import HostCertificateController

pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def create_abc_services(abc_services):
    pass


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_host_certificate_without_abc_service(crt_client, users, pc_csrs, path):
    requester = 'helpdesk_user'

    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.TEST_CA,
        'hosts': 'sad-host-without-owner.yandex.ru',
    }

    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    response = crt_client.json.post(path, data=request_data)

    if path == '/api/certificate/':
        assert response.status_code == status.HTTP_201_CREATED
        data = response.json()
        cert = Certificate.objects.get(serial_number=data['serial_number'])
        assert cert.requested_by_csr is False
        assert cert.is_reissue is False
    else:
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        assert response.json() == {'abc_service': ['This field is required.']}


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_host_certificate_cn_not_in_sans(crt_client, users, path):
    requester = 'yc_server_user'

    request_data = {
        'type': CERT_TYPE.YC_SERVER,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': 'host.cloud.yandex.net',
        'hosts': 'other-host.cloud.yandex.net',
        'abc_service': 1,
    }

    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == 400
    assert response.json()['non_field_errors'] == ['Common Name must be included in Certificate Subject Alternative Names (hosts)']


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_host_certificate_with_duplicating_hosts(crt_client, users, pc_csrs, path):
    requester = 'helpdesk_user'

    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.TEST_CA,
        'hosts': 'very-popular-host.ru,very-popular-host.ru,some-unique-host.com',
        'abc_service': 1,
    }

    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_201_CREATED
    assert sorted(response.json()['hosts']) == sorted(['very-popular-host.ru', 'some-unique-host.com'])


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('json_content_type', [True, False])
@pytest.mark.parametrize('yav_blacklist', [['helpdesk_user'], []])
def test_request_host_certificate(crt_client, users, path, user_datas, json_content_type, yav_blacklist):
    requester = 'helpdesk_user'

    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.TEST_CA,
        'hosts': 'very-very-very-very-very-long-64-symbols-hostname.yandex-team.ru',
        'abc_service': 1,
    }

    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    with override_config(YAV_BLACKLIST=yav_blacklist):
        if json_content_type:
            response = crt_client.json.post(path, data=request_data)
        else:
            response = crt_client.post(path, data=request_data)

    assert response.status_code == status.HTTP_201_CREATED

    if json_content_type:
        response_data = response.json()
    else:
        response_data = json.loads(response.content)
    assert response_data['type'] == CERT_TYPE.HOST
    assert response_data['status'] == CERT_STATUS.ISSUED
    assert response_data['hosts'] == ['very-very-very-very-very-long-64-symbols-hostname.yandex-team.ru']
    if path == '/api/certificate/':
        assert response_data['username'] == requester
        assert response_data['requester'] == requester
        assert response_data['abc_service'] == 1
    else:
        assert response_data['user'] == user_datas[requester]
        assert response_data['requester'] == user_datas[requester]
        assert response_data['abc_service']['id'] == 1

    cert = Certificate.objects.get()
    host = cert.hosts.get()
    assert host.hostname == 'very-very-very-very-very-long-64-symbols-hostname.yandex-team.ru'
    if yav_blacklist:
        assert cert.uploaded_to_yav is None
    else:
        assert cert.uploaded_to_yav is False


@pytest.mark.parametrize('cert_type', [CERT_TYPE.HOST, CERT_TYPE.YC_SERVER, CERT_TYPE.MDB])
@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_host_certificate_for_two_hosts(crt_client, users, user_datas, cert_type, path):

    params = {
        CERT_TYPE.MDB: {
            'requester': 'mdb_user',
            'requested_hosts': 'host-1.db.yandex.net,host-2.db.yandex.net',
            'expected_hosts': ['host-1.db.yandex.net', 'host-2.db.yandex.net'],
        },
        'default': {
            'requester': 'yc_server_user',
            'requested_hosts': 'host-1.yandexcloud.net,host-2.yandexcloud.net',
            'expected_hosts': ['host-1.yandexcloud.net', 'host-2.yandexcloud.net']
        }
    }
    params = params.get(cert_type) or params['default']

    requester = params['requester']
    requested_hosts = params['requested_hosts']
    expected_hosts = params['expected_hosts']

    request_data = {
        'type': cert_type,
        'ca_name': CA_NAME.TEST_CA,
        'hosts': requested_hosts,
        'abc_service': 1,
    }

    user = users[requester]
    crt_client.login(user.username)

    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_201_CREATED

    response_data = response.json()
    assert response_data['type'] == cert_type
    assert response_data['status'] == CERT_STATUS.ISSUED
    assert set(response_data['hosts']) == set(expected_hosts)
    assert response_data['common_name'] == expected_hosts[0]
    if path == '/api/certificate/':
        assert response_data['username'] == requester
        assert response_data['requester'] == requester
        if cert_type == CERT_TYPE.HOST:
            assert response_data['abc_service'] == 1
        else:
            assert 'abc_service' not in response_data
    else:
        assert response_data['user'] == user_datas[requester]
        assert response_data['requester'] == user_datas[requester]
        if cert_type == CERT_TYPE.HOST:
            assert response_data['abc_service']['id'] == 1
        else:
            assert 'abc_service' not in response_data

    hosts = Certificate.objects.get().hosts.values_list('hostname', flat=True)
    assert set(hosts) == set(expected_hosts)


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_host_certificate_with_abc_service(crt_client, users, user_datas, pc_csrs, path):
    requester = 'helpdesk_user'

    abc_service = AbcService.objects.get(external_id=1)

    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.TEST_CA,
        'hosts': 'boring-generic-name.yandex-team.ru',
        'abc_service': abc_service.external_id,
    }

    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_201_CREATED

    response_data = response.json()
    assert response_data['type'] == CERT_TYPE.HOST
    assert response_data['status'] == CERT_STATUS.ISSUED
    if path == '/api/certificate/':
        assert response_data['username'] == requester
        assert response_data['requester'] == requester
        assert response_data['abc_service'] == abc_service.external_id
    else:
        assert response_data['user'] == user_datas[requester]
        assert response_data['requester'] == user_datas[requester]
        assert response_data['abc_service']['id'] == abc_service.external_id

    cert = Certificate.objects.get()
    assert cert.abc_service.pk == abc_service.pk


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_host_certificate_with_invalid_abc_service(crt_client, users, pc_csrs, path):
    requester = 'helpdesk_user'

    max_service_id = list(AbcService.objects.aggregate(max_id=Max('external_id')).values())[0]

    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.TEST_CA,
        'hosts': 'boring-generic-name.yandex-team.ru',
        'abc_service': max_service_id + 1,
    }

    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_400_BAD_REQUEST
    assert Certificate.objects.count() == 0


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_filter_host_certificates_by_abc_service(crt_client, users, pc_csrs, path):
    requester = 'helpdesk_user'
    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    for i in range(1, 4):
        request_data = {
            'type': CERT_TYPE.HOST,
            'ca_name': CA_NAME.TEST_CA,
            'hosts': 'host-%d.yandex-team.ru' % i,
            'abc_service': i,
        }
        response = crt_client.json.post(path, data=request_data)
        assert response.status_code == status.HTTP_201_CREATED

    # Without filters
    response = crt_client.json.get(path, dict(_fields='hosts,abc_service'))
    assert response.status_code == status.HTTP_200_OK
    response_data = response.json()

    if path == '/api/certificate/':
        services = {cert['abc_service'] for cert in response_data['results']}
    else:
        services = {(cert['abc_service']['id'] if cert['abc_service'] else None)
                    for cert in response_data['results']}
    assert services == {1, 2, 3}

    # With filters
    for i in range(1, 4):
        response = crt_client.json.get(path, dict(abc_service=i, _fields='hosts,abc_service'))
        assert response.status_code == status.HTTP_200_OK
        response_data = response.json()

        if path == '/api/certificate/':
            services_ids = {cert['abc_service'] for cert in response_data['results']}
        else:
            services_ids = {(cert['abc_service']['id'] if cert['abc_service'] else None)
                            for cert in response_data['results']}
        assert services_ids == {i}


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_filter_host_certificates_with_abc_service(crt_client, users, pc_csrs, certificate_types, path):
    requester = 'helpdesk_user'
    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    for i in range(1, 7):
        kwargs = {'hosts': ['host-%d.yandex.ru' % i]}
        if i <= 3:
            kwargs['abc_service'] = AbcService.objects.get(external_id=i)
        create_certificate(helpdesk_user, certificate_types[CERT_TYPE.HOST], **kwargs)

    # Without filters
    response = crt_client.json.get(path, dict(_fields='hosts,abc_service'))
    assert response.status_code == status.HTTP_200_OK
    response_data = response.json()['results']

    if path == '/api/certificate/':
        services = [cert['abc_service'] for cert in response_data]
    else:
        services = [(cert['abc_service']['id'] if cert['abc_service'] else None)
                    for cert in response_data]
    assert services == [None, None, None, 3, 2, 1]

    # Without abc_service
    response = crt_client.json.get(path, dict(abc_service__isnull=True, _fields='hosts,abc_service'))
    assert response.status_code == status.HTTP_200_OK
    response_data = response.json()['results']

    if path == '/api/certificate/':
        services_ids = [cert['abc_service'] for cert in response_data]
    else:
        services_ids = [(cert['abc_service']['id'] if cert['abc_service'] else None)
                        for cert in response_data]
    hosts = [cert['hosts'][0] for cert in response_data]
    assert services_ids == [None, None, None]
    assert hosts == ['host-6.yandex.ru', 'host-5.yandex.ru', 'host-4.yandex.ru']

    # With abc_service
    response = crt_client.json.get(path, dict(abc_service__isnull=False, _fields='hosts,abc_service'))
    assert response.status_code == status.HTTP_200_OK
    response_data = response.json()['results']

    if path == '/api/certificate/':
        services_ids = [cert['abc_service'] for cert in response_data]
    else:
        services_ids = [(cert['abc_service']['id'] if cert['abc_service'] else None)
                        for cert in response_data]
    hosts = [cert['hosts'][0] for cert in response_data]
    assert services_ids == [3, 2, 1]
    assert hosts == ['host-3.yandex.ru', 'host-2.yandex.ru', 'host-1.yandex.ru']


@pytest.mark.parametrize('path', ['/api/certificate/{}/', '/api/v2/certificate/{}/'])
def test_update_abc_service(crt_client, users, certificate_types, path):
    requester = 'helpdesk_user'
    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    abc_service = AbcService.objects.get(external_id=1)

    cert = create_certificate(
        helpdesk_user,
        certificate_types[CERT_TYPE.HOST],
        hosts=['boring-generic-name.yandex-team.ru'],
    )

    assert cert.abc_service is None

    if path == '/api/certificate/{}/':
        request_data = {
            'action': 'update',
            'abc_service': abc_service.external_id
        }
        response = crt_client.json.post(path.format(cert.pk), request_data)
    else:
        request_data = {
            'abc_service': abc_service.external_id
        }
        response = crt_client.json.patch(path.format(cert.pk), request_data)
    assert response.status_code == status.HTTP_200_OK

    cert = Certificate.objects.get()
    assert cert.abc_service.pk == abc_service.pk


@pytest.mark.parametrize('path', ['/api/certificate/{}/', '/api/v2/certificate/{}/'])
def test_update_unknown_abc_service(crt_client, users, certificate_types, path):
    requester = 'helpdesk_user'
    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    cert = create_certificate(
        helpdesk_user,
        certificate_types[CERT_TYPE.HOST],
        hosts=['boring-generic-name.yandex-team.ru'],
    )

    assert cert.abc_service is None

    if path == '/api/certificate/{}/':
        request_data = {
            'action': 'update',
            'abc_service': 9999999999
        }
        response = crt_client.json.post(path.format(cert.pk), request_data)
    else:
        request_data = {
            'abc_service': 9999999999
        }
        response = crt_client.json.patch(path.format(cert.pk), request_data)
    assert response.status_code == status.HTTP_400_BAD_REQUEST

    cert = Certificate.objects.get()
    assert cert.abc_service is None


def test_request_host_certificate_with_ecc(crt_client, users, pc_csrs):
    path = '/api/certificate/'
    requester = 'helpdesk_user'

    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.TEST_CA,
        'hosts': 'sad-host-without-owner.yandex.ru',
        'is_ecc': True
    }

    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_201_CREATED
    data = response.json()
    cert = Certificate.objects.get(serial_number=data['serial_number'])
    assert cert.is_ecc
    assert HostCertificateController(cert).get_internal_ca_template() == CERT_TEMPLATE.WEB_SERVER_ECC


@pytest.mark.parametrize(
    'desired_ttl_days, expected_ttl_days',
    [
        (100500, 365),
        (364, 364),
    ]
)
@pytest.mark.parametrize(
    'path',
    [
        '/api/certificate/',
        '/api/v2/certificates/',
        '/api/frontend/certificates/'
    ]
)
def test_host_certificate_ttl_limiting(crt_client, users, path, desired_ttl_days, expected_ttl_days):
    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.TEST_CA,
        'hosts': 'sad-host-without-owner.yandex.ru',
        'desired_ttl_days': desired_ttl_days,
        'abc_service': AbcService.objects.first().external_id,
    }

    crt_client.login(users['normal_user'])
    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_201_CREATED, response.content
    data = response.json()
    cert = Certificate.objects.get(serial_number=data['serial_number'])
    assert cert.desired_ttl_days == expected_ttl_days


@pytest.mark.parametrize(
    'common_name, hosts, expected_common_name',
    [
        ['a.yandex.ru', 'a.yandex.ru,b.yandex.ru', 'a.yandex.ru'],
        ['a.yandex.ru', 'b.yandex.ru,a.yandex.ru', 'a.yandex.ru'],
        [None, 'a.yandex.ru,b.yandex.ru', 'a.yandex.ru'],
        [None, 'a.yandex.ru,*.yandex.ru', '*.yandex.ru'],
        ['', 'a.yandex.ru,b.yandex.ru', 'a.yandex.ru'],
        ['', 'a.yandex.ru,*.yandex.ru', '*.yandex.ru'],
    ]
)
@pytest.mark.parametrize(
    'path',
    [
        '/api/certificate/',
        '/api/v2/certificates/',
        '/api/frontend/certificates/'
    ]
)
def test_host_certificate_common_name(crt_client, users, path, common_name, hosts, expected_common_name):
    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.TEST_CA,
        'abc_service': AbcService.objects.first().external_id,
        'hosts': hosts,
        'common_name': common_name,
    }

    crt_client.login(users['normal_user'])
    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_201_CREATED, response.content
    data = response.json()
    cert = Certificate.objects.get(serial_number=data['serial_number'])
    assert cert.common_name == expected_common_name
    cert_hosts = list(cert.hosts.order_by('hostname').values_list('hostname', flat=True))
    assert cert_hosts == sorted(hosts.split(','))


@pytest.mark.parametrize(
    'path',
    [
        '/api/certificate/',
        '/api/v2/certificates/',
        '/api/frontend/certificates/'
    ]
)
def test_unsupported_type_on_ca(crt_client, users, path):
    ca_name = CA_NAME.CERTUM_TEST_CA
    cert_type = CERT_TYPE.PC
    crt_client.login(users['helpdesk_user'])

    request_data = {
        'type': cert_type,
        'ca_name': ca_name,
        'hosts': 'qwe.yandex.ru',
    }
    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_400_BAD_REQUEST, response.content
    assert response.json() == {
        'non_field_errors': [f'Invalid {cert_type} cert for {ca_name}']
    }
