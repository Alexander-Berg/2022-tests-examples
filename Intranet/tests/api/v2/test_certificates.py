import json

import pytest

from django.contrib.auth.models import Permission
from django.utils import timezone
from django_abc_data.models import AbcService

from intranet.crt.api.v2.certificates.views import CertificateList
from intranet.crt.constants import CERT_STATUS, ABC_CERTIFICATE_MANAGER_SCOPE, ABC_ADMINISTRATOR_SCOPE, CERT_TYPE, CA_NAME
from intranet.crt.core.models import Certificate, CertificateType
from intranet.crt.users.models import CrtGroup
from __tests__.tags.test_sync_tags import certificates
from __tests__.utils.common import create_permission, create_group, create_certificate

from rest_framework import status
from rest_framework.test import APIRequestFactory, force_authenticate

pytestmark = pytest.mark.django_db

normal_user_data = {
    'username': 'normal_user',
    'first_name': {'en': 'normal_user', 'ru': 'normal_user'},
    'last_name': {'en': 'normal_user', 'ru': 'normal_user'},
    'is_active': True,
    'in_hiring': False,
}
tag_user_data = {
    'username': 'tag_user',
    'first_name': {'en': 'tag_user', 'ru': 'tag_user'},
    'last_name': {'en': 'tag_user', 'ru': 'tag_user'},
    'is_active': True,
    'in_hiring': False,
}


def test_default_filter(crt_client, users, certificates):
    tag_user = users['tag_user']
    crt_client.login(tag_user.username)

    # Смотрим на сертификаты с соответствующим скоупом
    response = crt_client.json.get('/api/v2/certificates/', {'_fields': 'corn_field'})
    assert response.status_code == 200
    data = response.json()['results']
    for cert in Certificate.objects.filter(id__in=[cert['id'] for cert in data]):
        assert tag_user.staff_groups.filter(
            abc_service=cert.abc_service,
            role_scope__in={ABC_CERTIFICATE_MANAGER_SCOPE, ABC_ADMINISTRATOR_SCOPE}
        ).exists() or cert.user.username == tag_user_data['username']

    for cert in tag_user.staff_groups.filter(role_scope__in={ABC_CERTIFICATE_MANAGER_SCOPE, ABC_ADMINISTRATOR_SCOPE}):
        assert Certificate.objects.filter(abc_service=cert.abc_service, id__in=[cert['id'] for cert in data]).exists()


def test_default_filter_same_certificate(crt_client, users, certificates):
    tag_user = users['tag_user']
    crt_client.login(tag_user.username)

    # Смотрим на сертификаты с соответствующим скоупом
    response = crt_client.json.get('/api/v2/certificates/', {'_fields': 'corn_field'})
    assert response.status_code == 200
    data = response.json()['results']
    first_data_length = len(data)
    for cert in Certificate.objects.filter(id__in=[cert['id'] for cert in data]):
        assert tag_user.staff_groups.filter(
            abc_service=cert.abc_service,
            role_scope__in={ABC_CERTIFICATE_MANAGER_SCOPE, ABC_ADMINISTRATOR_SCOPE}
        ).exists() or cert.user.username == tag_user_data['username']

    for cert in tag_user.staff_groups.filter(role_scope__in={ABC_CERTIFICATE_MANAGER_SCOPE, ABC_ADMINISTRATOR_SCOPE}):
        assert Certificate.objects.filter(abc_service=cert.abc_service, id__in=[cert['id'] for cert in data]).exists()

    service = AbcService.objects.create(external_id=77, created_at=timezone.now(), modified_at=timezone.now())
    create_certificate(
        abc_service=service,
        requester=users['normal_user'],
        user=users['normal_user'],
        type=CertificateType.objects.get(name=CERT_TYPE.ASSESSOR)
    )
    tag_user.staff_groups.add(CrtGroup.objects.create(
        abc_service=service,
        role_scope=ABC_CERTIFICATE_MANAGER_SCOPE,
        external_id=777,
        is_deleted=False
    ))
    tag_user.staff_groups.add(CrtGroup.objects.create(
        abc_service=service,
        role_scope=ABC_ADMINISTRATOR_SCOPE,
        external_id=7777,
        is_deleted=False
    ))
    response = crt_client.json.get('/api/v2/certificates/', {'_fields': 'corn_field'})
    assert response.status_code == 200
    data = response.json()['results']
    assert len(data) == first_data_length + 1


def test_filter_by_user(crt_client, users, certificates):
    tag_user = users['tag_user']
    crt_client.login(tag_user.username)

    # Смотрим на свои сертификаты
    response = crt_client.json.get('/api/v2/certificates/', {'_fields': 'user'})
    assert response.status_code == 200
    data = response.json()['results']
    assert len(data) == len([cert for cert in list(certificates.values()) if cert.user == tag_user])
    for cert in data:
        assert cert['user'] == tag_user_data

    # Если мы применим фильтр по владельцу и укажем не себя, то получим пустой ответ
    response = crt_client.json.get('/api/v2/certificates/', {'_fields': 'request,user', 'user': 'normal_user'})
    assert response.status_code == 200
    assert len(response.json()['results']) == 0

    # Если указан не существующий user, не находим сертификатов
    response = crt_client.json.get('/api/v2/certificates/', {'_fields': 'user', 'user': 'unexistent_user'})
    assert response.status_code == 200
    data = response.json()
    assert data['count'] == 0
    assert data['results'] == []

    # Если пользователь присутствует в списке фильтра по владельцу, то увидит только свои сертификаты
    response = crt_client.json.get('/api/v2/certificates/', {'_fields': 'user', 'user': 'tag_user,normal_user'})
    assert response.status_code == 200
    data = response.json()['results']
    assert len(data) == len([cert for cert in list(certificates.values()) if cert.user == tag_user])
    for cert in data:
        assert cert['user'] == tag_user_data

    # Теперь мы можем смотреть на все сертификаты
    viewer_perm = create_permission('can_view_any_certificate', 'core', 'certificate')
    viewer_group = create_group('viewers', permissions=[viewer_perm])
    tag_user.groups.add(viewer_group)

    response = crt_client.json.get('/api/v2/certificates/', {'_fields': 'user'})
    assert response.status_code == 200
    data = response.json()['results']
    assert len(data) > len([cert for cert in list(certificates.values())if cert.user == tag_user])
    assert len(data) == len(certificates)

    # Ограничимся своими
    response = crt_client.json.get('/api/v2/certificates/', {'_fields': 'user', 'user': 'tag_user'})
    assert response.status_code == 200
    data = response.json()['results']
    assert len(data) == len([cert for cert in list(certificates.values())if cert.user == tag_user])
    for cert in data:
        assert cert['user'] == tag_user_data


def test_fields(crt_client, users, certificates):
    tag_user = users['tag_user']
    crt_client.login(tag_user.username)

    response = crt_client.json.get('/api/v2/certificates/')
    assert response.status_code == 400
    assert response.json() == {'detail': '_fields parameter is required'}

    for field in ['id', 'id,id', 'corn_field', 'first_strange_field,second_strange_field']:
        response = crt_client.json.get('/api/v2/certificates/', {'_fields': field})
        assert response.status_code == 200
        data = response.json()['results']
        for cert in data:
            assert list(cert.keys()) == ['id']

    for field in ['user', 'id,user', 'user,id']:
        response = crt_client.json.get('/api/v2/certificates/', {'_fields': field})
        assert response.status_code == 200
        data = response.json()['results']
        for cert in data:
            assert set(cert.keys()) == {'id', 'user'}
            assert cert['user'] == tag_user_data


def test_humanized_fields(crt_client, users, certificate_types, abc_services):
    normal_user = users['normal_user']
    crt_client.login(normal_user.username)

    service = AbcService.objects.get(external_id=3)
    certificate = create_certificate(normal_user, certificate_types['host'], abc_service=service)

    response = crt_client.json.get(
        '/api/v2/certificates/',
        {'_fields': 'user,requester,status,status_human,type,type_human,username'}
    )
    assert response.status_code == 200
    assert len(response.json()['results']) == 1

    cert = response.json()['results'][0]
    assert cert == {
        'id': certificate.pk,
        'user': normal_user_data,
        'requester': normal_user_data,
        'status': 'issued',
        'status_human': {'en': 'Issued', 'ru': 'Выдан'},
        'type': 'host',
        'type_human': {'en': 'For web servers', 'ru': 'Для web-сервера'},
    }


def test_available_actions_format(crt_client, users, certificate_types):
    helpdesk_user = users['helpdesk_user']
    normal_user = users['normal_user']
    crt_client.login(normal_user.username)

    # Хотим смотреть и изменять все сертификаты
    viewer_perm = Permission.objects.get(codename='can_view_any_certificate')
    update_perm = Permission.objects.get(codename='can_change_abc_service_for_any_certificate')
    normal_user.user_permissions.add(viewer_perm)
    normal_user.user_permissions.add(update_perm)

    cert = create_certificate(helpdesk_user, certificate_types['assessor'])

    response = crt_client.json.get('/api/v2/certificate/{}/'.format(cert.pk))
    assert response.status_code == 200
    available_actions = response.json()['available_actions']
    assert available_actions == [{
        'id': 'update',
        'name': {
            'en': 'Update',
            'ru': 'Обновить',
        }
    }]


def test_available_actions_content(crt_client, users, certificate_types):
    helpdesk_user = users['helpdesk_user']

    normal_user = users['normal_user']
    # Хотим смотреть на все сертификаты
    viewer_perm = Permission.objects.get(codename='can_view_any_certificate')
    normal_user.user_permissions.add(viewer_perm)

    crt_client.login(normal_user.username)

    cert_normal = create_certificate(normal_user, certificate_types['assessor'])
    cert_helpdesk = create_certificate(helpdesk_user, certificate_types['assessor'])

    # В списке информацию о действиях не отдаём
    response = crt_client.json.get('/api/v2/certificates/', {'_fields': 'available_actions'})
    assert response.status_code == 200
    for cert in response.json()['results']:
        assert 'available_actions' not in cert

    # Наш сертификат
    response = crt_client.json.get('/api/v2/certificate/{}/'.format(cert_normal.id))
    assert response.status_code == 200
    actions = {action['id'] for action in response.json()['available_actions']}
    assert actions == {'download', 'update', 'hold', 'revoke'}

    # Чужой сертификат
    response = crt_client.json.get('/api/v2/certificate/{}/'.format(cert_helpdesk.id))
    assert response.status_code == 200
    assert response.json()['available_actions'] == []

    # Добавим чутка прав
    perm = Permission.objects.get(codename='can_change_abc_service_for_any_certificate')
    normal_user.user_permissions.add(perm)
    response = crt_client.json.get('/api/v2/certificate/{}/'.format(cert_helpdesk.id))
    assert response.status_code == 200
    actions = {action['id'] for action in response.json()['available_actions']}
    assert actions == {'update'}

    # Теперь все права есть
    normal_user.is_superuser = True
    normal_user.save()
    response = crt_client.json.get('/api/v2/certificate/{}/'.format(cert_helpdesk.id))
    assert response.status_code == 200
    actions = {action['id'] for action in response.json()['available_actions']}
    assert actions == {'download', 'update', 'hold', 'revoke'}


def test_available_actions_for_specific_statuses(crt_client, users, certificate_types):
    normal_user = users['normal_user']
    cert = create_certificate(normal_user, certificate_types['assessor'])
    crt_client.login(normal_user.username)

    assert cert.status == CERT_STATUS.ISSUED
    response = crt_client.json.get('/api/v2/certificate/{}/'.format(cert.pk))
    assert response.status_code == 200
    actions = {action['id'] for action in response.json()['available_actions']}
    assert actions == {'download', 'update', 'hold', 'revoke'}

    cert.status = CERT_STATUS.HOLD
    cert.save()
    response = crt_client.json.get('/api/v2/certificate/{}/'.format(cert.pk))
    assert response.status_code == 200
    actions = {action['id'] for action in response.json()['available_actions']}
    assert actions == {'download', 'update', 'unhold', 'revoke'}

    cert.status = CERT_STATUS.ERROR
    cert.save()
    response = crt_client.json.get('/api/v2/certificate/{}/'.format(cert.pk))
    assert response.status_code == 200
    actions = {action['id'] for action in response.json()['available_actions']}
    assert actions == set()


def test_download(crt_client, users, certificate_types):
    normal_user = users['normal_user']
    crt_client.login(normal_user.username)

    cert = create_certificate(normal_user, certificate_types['imdm'])

    response = crt_client.json.get('/api/v2/certificate/{}/download/'.format(cert.id))
    assert response.status_code == 400
    assert response.content == b'\'format\' parameter should be specified'

    response = crt_client.json.get('/api/v2/certificate/{}/download/'.format(cert.id), {'format': 'pem'})
    assert response.status_code == 200
    assert response['Content-Type'] == 'application/x-pem-file; charset=utf-8'

    response = crt_client.json.get('/api/v2/certificate/{}/download/'.format(cert.id), {'format': 'pfx'})
    assert response.status_code == 404
    assert response['Content-Type'] == 'text/html; charset=utf-8'
    assert (response.content.decode('utf-8') ==
            'Невозможно создать PFX файл для сертификата {}, приватный ключ не найден'.format(cert.id))

    response = crt_client.json.get('/api/v2/certificate/{}/download/'.format(cert.id), {'format': 'iphoneexchange'})
    assert response.status_code == 404
    assert response['Content-Type'] == 'text/html; charset=utf-8'
    assert (response.content.decode('utf-8') ==
            'Невозможно создать IPHONEEXCHANGE файл для сертификата {}, приватный ключ не найден'.format(cert.id))


def test_blank_strings(crt_client, users, certificate_types):
    # Не все поля, но поля всех возможных строковых типов
    fields_to_check = {'serial_number', 'request', 'issued', 'revoked', 'st_issue_key'}

    normal_user = users['normal_user']
    crt_client.login(normal_user.username)

    cert = create_certificate(normal_user, certificate_types['host'])
    Certificate.objects.filter(pk=cert.pk).update(request=None)
    assert cert.serial_number is None

    for path in ['/api/certificate/{}/', '/api/v2/certificate/{}/']:
        response = crt_client.json.get(path.format(cert.pk))
        assert response.status_code == 200
        for field in fields_to_check:
            assert response.json()[field] is None

    response = crt_client.json.get('/api/frontend/certificate/{}/'.format(cert.pk))
    assert response.status_code == 200
    for field in fields_to_check:
        assert response.json()[field] == ''


def test_default_notify_on_expiration(crt_client, users, certificate_types):
    # Не все поля, но поля всех возможных строковых типов
    # fields_to_check = {'serial_number', 'request', 'issued', 'revoked', 'st_issue_key'}

    normal_user = users['normal_user']
    crt_client.login(normal_user.username)

    cert = create_certificate(normal_user, certificate_types['host'])
    assert cert.notify_on_expiration is True


@pytest.mark.parametrize('url', ['/api/certificate/', '/api/v2/certificates/'])
def test_request_cert_over_api(crt_client, users, abc_services, url):
    requester = users['normal_user']

    crt_client.login(requester)

    non_csr_common_name = 'normal_user@ld.yandex.ru'
    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.TEST_CA,
        'pc_os': 'dummy',
        'pc_mac': 'dummy',
        'pc_hostname': 'dummy',
        'common_name': non_csr_common_name,
        'abc_service': 1,
    }

    create_response = crt_client.json.post(url, data=request_data)
    assert create_response.status_code == status.HTTP_201_CREATED, create_response.json()
    response_data = create_response.json()
    assert 'notify_on_expiration' in response_data
    assert response_data['notify_on_expiration'] is True


@pytest.mark.parametrize('url', ['/api/certificate/', '/api/v2/certificates/'])
@pytest.mark.parametrize("notify_param_value, resulting_value",  [
    (True, True),
    ("true", True),
    (False, False),
    ("false", False)
])
def test_notify_on_expiration_api_handle(crt_robot, users, abc_services, notify_param_value, resulting_value, url):
    user = users['normal_user']
    factory = APIRequestFactory()
    non_csr_common_name = 'normal_user@ld.yandex.ru'

    request_data = {
        'type': CERT_TYPE.HOST,
        'ca_name': CA_NAME.TEST_CA,
        'pc_os': 'dummy',
        'pc_mac': 'dummy',
        'pc_hostname': 'dummy',
        'common_name': non_csr_common_name,
        'abc_service': 1,
        'notify_on_expiration': notify_param_value,
    }
    data = json.dumps(request_data)
    request = factory.post(url, data,  content_type='application/json')
    force_authenticate(request, user)
    view = CertificateList.as_view()
    response = view(request)
    response.render()

    assert resulting_value == response.data['notify_on_expiration']
    assert 201 == response.status_code
