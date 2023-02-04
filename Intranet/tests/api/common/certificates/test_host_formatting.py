import pytest
from rest_framework import status

from intranet.crt.constants import CERT_TYPE, CERT_STATUS, CA_NAME
from intranet.crt.core.models import Certificate

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('cert_type', [CERT_TYPE.HOST, CERT_TYPE.YC_SERVER])
@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('asterisk_count', list(range(6)))
def test_request_wildcard_certificate(crt_client, users, user_datas, asterisk_count, path, abc_services, cert_type):
    """Можно выписывать wildcard-сертификаты, но только с одной звездой"""
    requester = 'yc_server_user'

    hostname = '{}cloud.yandex.net'.format('*.' * asterisk_count)
    request_data = {
        'type': cert_type,
        'ca_name': CA_NAME.TEST_CA,
        'hosts': hostname,
        'abc_service': 1,
    }

    yc_server_user = users[requester]
    crt_client.login(yc_server_user.username)

    response = crt_client.json.post(path, data=request_data)
    if asterisk_count < 2:
        assert response.status_code == status.HTTP_201_CREATED

        response_data = response.json()
        assert response_data['type'] == cert_type
        assert response_data['status'] == CERT_STATUS.ISSUED
        if path == '/api/certificate/':
            assert response_data['username'] == requester
            assert response_data['requester'] == requester
        else:
            assert response_data['user'] == user_datas[requester]
            assert response_data['requester'] == user_datas[requester]

        certificate = Certificate.objects.get()
        assert certificate.status == CERT_STATUS.ISSUED
    else:
        assert response.status_code == status.HTTP_400_BAD_REQUEST
        data = response.json()
        expected = (
                       'Host \'%s\' contains invalid formatting, e.g. '
                       'contains two or more asterisks, '
                       'non-leading asterisks, whitespace characters, etc.'
                    ) % hostname
        assert data['hosts'] == [expected]


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('hostname', ['team.-yandex.ru', '.yandex.ru', 'team-.yandex.ru',
                                      'yandex.', '{}.ru'.format('a'*70), 'team..ru', '.ru',
                                      '*.ru', 'yandex.*', 'https://yandex.ru',
                                      'yandex_team.ru', 'hodor.'*42 + 'auto']
                         )
def test_request_certificate_for_invalid_host(crt_client, users, hostname, path, abc_services):
    requester = 'helpdesk_user'

    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.TEST_CA,
        'hosts': hostname,
        'abc_service': 1,
    }

    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_400_BAD_REQUEST
    data = response.json()
    expected = (
        'Host \'{}\' contains invalid formatting, e.g. '
        'contains two or more asterisks, '
        'non-leading asterisks, whitespace characters, etc.'
    ).format(hostname)
    assert data['hosts'] == [expected]


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_certificate_for_hosts(crt_client, users, path, abc_services):
    requester = 'helpdesk_user'

    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.TEST_CA,
        'hosts': '   ya.ru,  ya.com, ',
        'abc_service': 1,
    }

    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    response = crt_client.json.post(path, data=request_data)
    assert response.status_code == status.HTTP_201_CREATED
    assert sorted(response.json()['hosts']) == ['ya.com', 'ya.ru']


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('divider', [' ', '   ', '\n', '\t', '\n\t\t   \n'])
def test_request_certificate_for_whitespaced_hosts(crt_client, users, path, divider, abc_services):
    requester = 'helpdesk_user'

    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.TEST_CA,
        'hosts': '   ya.ru ya.com, ',
        'abc_service': 1,
    }

    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    response = crt_client.json.post(path, data=request_data)
    if path == '/api/certificate/':
        assert response.status_code == status.HTTP_201_CREATED
        assert set(response.json()['hosts']) == {'ya.ru', 'ya.com'}
    else:
        assert response.status_code == status.HTTP_400_BAD_REQUEST


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize('hostname', ['yandex-team.ru', 'a.b.c.ru', '*.yandex.ru', 'a-b.c-d.ru',
                                      'яндекс.рф', 'я.ру', '*.яндекс-команда.рф',
                                      'hodor.' * 42 + 'com']
                         )
def test_request_certificate_for_valid_host(crt_client, users, user_datas, hostname, path, abc_services):
    requester = 'helpdesk_user'

    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.TEST_CA,
        'hosts': hostname,
        'abc_service': 1,
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
    else:
        assert response_data['user'] == user_datas[requester]
        assert response_data['requester'] == user_datas[requester]

    certificate = Certificate.objects.get()
    assert certificate.status == CERT_STATUS.ISSUED


@pytest.mark.parametrize('path', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_certificate_for_idna_encoded_host(crt_client, users, path, abc_services):
    requester = 'helpdesk_user'

    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.TEST_CA,
        'common_name': 'xn--80aqf2ac.xn--d1acpjx3f.xn--p1ai',
        'hosts': 'xn--80aqf2ac.xn--d1acpjx3f.xn--p1ai,такси.яндекс.рф,someother.yandex.ru',
        'abc_service': 1,
    }

    helpdesk_user = users[requester]
    crt_client.login(helpdesk_user.username)

    response = crt_client.json.post(path, data=request_data)

    assert response.status_code == status.HTTP_201_CREATED
    assert response.json()['common_name'] == 'такси.яндекс.рф'
    assert set(response.json()['hosts']) == {'такси.яндекс.рф', 'someother.yandex.ru'}
