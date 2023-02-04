import json
import pretend
import pytest

from django.urls import reverse

from django_yauth.authentication_mechanisms import tvm
from django_yauth.authentication_mechanisms.tvm.request import (
    TvmImpersonatedRequest,
    TvmServiceRequest,
)
from django_yauth.middleware import YandexAuthMiddleware
from django_yauth.user import AnonymousYandexUser, YandexTestUserDescriptor
from tvm2 import TVM2
from tvm2.ticket import ServiceTicket

from plan import settings
from plan.common.middleware import UserMiddleware
from common import factories


class FakeRequest(object):
    def __init__(self, meta, method='GET'):
        self.META = meta
        self.method = method
        self.GET = {}
        self.COOKIES = {}


@pytest.fixture
def patch_tvm(monkeypatch):
    def fake_apply(self, service_ticket, user_ip, user_ticket=None):
        if not (service_ticket or user_ticket):
            return self.anonymous()
        if user_ticket:
            return self.get_person_user(
                user_ticket=pretend.stub(default_uid=user_ticket),
                service_ticket=service_ticket,
                raw_user_ticket=user_ticket,
                raw_service_ticket=service_ticket,
                user_ip=user_ip,
            )
        else:
            return self.get_service_user(
                service_ticket=service_ticket,
                raw_service_ticket=service_ticket,
                user_ip=user_ip,
            )

    monkeypatch.setattr(TVM2, '_init_context', lambda *args, **kwargs: None)
    monkeypatch.setattr(tvm.Mechanism, 'apply', fake_apply)


class TvmUserPatch(object):
    def __init__(self):
        self.uid = None
        self.service_ticket = None


@pytest.fixture
def set_tvm_auth_result(monkeypatch, patch_tvm):
    user_patch = TvmUserPatch()

    def fake_get_user(*args, **kwargs):
        if not (user_patch.uid or user_patch.service_ticket):
            return AnonymousYandexUser(mechanism=tvm.Mechanism())
        elif user_patch.uid:
            return TvmImpersonatedRequest(
                user_ticket=user_patch.uid,
                service_ticket=user_patch.service_ticket,
                uid=user_patch.uid,
                mechanism=tvm.Mechanism(),
            )
        else:
            return TvmServiceRequest(
                service_ticket=user_patch.service_ticket,
                uid=None,
                mechanism=tvm.Mechanism(),
            )

    monkeypatch.setattr(YandexTestUserDescriptor, '_get_yandex_user', fake_get_user)
    return user_patch


def test_tvm_authenticate_user(client, patch_tvm):
    staff = factories.StaffFactory(login='bebe')
    meta = {'HTTP_X_YA_SERVICE_TICKET': 'xxx', 'HTTP_X_YA_USER_TICKET': staff.uid}
    request = FakeRequest(meta)
    auth_middleware = YandexAuthMiddleware()
    auth_middleware.process_request(request)
    user_middleware = UserMiddleware()
    user_middleware.process_request(request)
    assert request.user.is_authenticated()
    assert request.user == staff.user
    assert request.person.staff == staff


def test_tvm_authenticate_service(client, patch_tvm):
    meta = {'HTTP_X_YA_SERVICE_TICKET': 'xxx'}
    request = FakeRequest(meta)
    auth_middleware = YandexAuthMiddleware()
    auth_middleware.process_request(request)
    user_middleware = UserMiddleware()
    user_middleware.process_request(request)
    assert not request.user.is_authenticated()
    assert not hasattr(request, 'person')


@pytest.mark.parametrize('tvm_mode', [None, 'service', 'user'])
@pytest.mark.parametrize('tvm_id', ['222', '333'])
def test_view_without_mixin(client, set_tvm_auth_result, tvm_mode, tvm_id):
    staff = factories.StaffFactory()
    if tvm_mode:
        set_tvm_auth_result.service_ticket = ServiceTicket({
            'dst': 'xxx',
            'debug_string': 'yyy',
            'logging_string': 'zzz',
            'scopes': '123',
            'src': tvm_id,
        })
        if tvm_mode == 'user':
            set_tvm_auth_result.uid = staff.uid
    client.login(staff.login)
    response = client.json.get(reverse('monitorings:failed-tasks'))
    if tvm_mode is None:
        assert response.status_code == 200
    elif tvm_mode == 'user':
        assert response.status_code == 200
    else:  # service
        assert response.status_code == 403


@pytest.mark.parametrize('tvm_mode', ['service', 'user'])
@pytest.mark.parametrize('tvm_id', ['222', '333'])
@pytest.mark.parametrize('endpoint', [
    'api-v4:service-list', 'api-v3:service-list',
    'api-v3:service-member-list', 'api-v4:service-member-list', 'api-v4:service-department-list',
    'api-v3:service-contact-list', 'api-v4:service-contact-list',
    'api-v3:service-responsible-list', 'api-v4:service-responsible-list',
    'api-v4:service-gradient-list', 'api-v4:moves-list',
    'api-v3:role-list', 'api-v4:role-list', 'api-v3:role-scope-list', 'api-v4:role-scope-list',
    'api-v3:duty-shift-list', 'api-v4:duty-shift-list', 'api-v4:duty-on-duty-list',
    'api-v4:duty-schedule-list', 'api-v4:allowforduty-list',
    'resources-api:serviceresources-list', 'api-v3:resource-consumer-list', 'api-v4:resource-consumer-list',
    'api-v4:resource-type-category-list', 'api-v4:resource-tag-category-list',
    'api-v4:resource-tag-list',  'api-v4:resource-list',
])
def test_view_with_mixin(client, set_tvm_auth_result, tvm_mode, tvm_id, endpoint):
    staff = factories.StaffFactory()
    staff.user.is_superuser = True
    staff.user.save()
    if tvm_mode:
        set_tvm_auth_result.service_ticket = ServiceTicket({
            'dst': 'xxx',
            'debug_string': 'yyy',
            'logging_string': 'zzz',
            'scopes': '123',
            'src': tvm_id,
        })
        if tvm_mode == 'user':
            set_tvm_auth_result.uid = staff.uid
    client.login(staff.login)
    data = {}
    if endpoint in ('api-v3:duty-shift-list', 'api-v4:duty-shift-list'):
        data = {
            'date_from': '2020-02-01',
            'date_to': '2020-02-02',
        }
    elif endpoint in ['api-v4:allowforduty-list', ]:
        service = factories.ServiceFactory()
        data = {
            'service': service.id
        }
    response = client.json.get(reverse(endpoint), data)
    assert response.status_code == 200


@pytest.mark.parametrize('tvm_id', ['222', '333'])
def test_with_uid_in_header(client, set_tvm_auth_result, tvm_id, staff_factory):
    set_tvm_auth_result.service_ticket = ServiceTicket({
        'dst': 'xxx',
        'debug_string': 'yyy',
        'logging_string': 'zzz',
        'scopes': '123',
        'src': tvm_id,
    })
    headers = {'HTTP_X_UID': staff_factory().uid}
    response = client.json.get(
        reverse('api-frontend:permission-list'),
        **headers
    )
    if tvm_id == '222':
        assert response.status_code == 200
    else:
        assert response.status_code == 403
        assert response.content == b'You must be authenticated as user'


@pytest.mark.parametrize('tvm_id', [settings.IDM_TVM_ID, '123'])
@pytest.mark.parametrize('endpoint,method', [
    ('idm-api:info', 'GET'),
    ('idm-ext-api:info', 'GET'),
    ('idm-api:add-role', 'POST'),
    ('idm-ext-api:add-role', 'POST'),
])
def test_idm_tvm(client, set_tvm_auth_result, tvm_id, endpoint, method):
    set_tvm_auth_result.service_ticket = ServiceTicket({
        'dst': 'xxx',
        'debug_string': 'yyy',
        'logging_string': 'zzz',
        'scopes': '123',
        'src': tvm_id,
    })
    staff = factories.StaffFactory()
    if method == 'GET':
        response = client.json.get(reverse(endpoint))
    else:
        response = client.json.post(
            reverse(endpoint),
            data={
                'login': staff.login,
                'role': json.dumps({
                    'type': 'internal',
                    'internal_key': 'superuser',
                }),
            }
        )
    if tvm_id == settings.IDM_TVM_ID:
        assert response.status_code == 200
    else:
        assert response.status_code == 403
