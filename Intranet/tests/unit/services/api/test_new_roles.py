# coding: utf-8
from __future__ import unicode_literals

import pytest
from tvm2.ticket import ServiceTicket
from django.contrib.auth.models import AnonymousUser
from django.core.urlresolvers import reverse
from django.core.urlresolvers import resolve
from django.test.client import RequestFactory


pytestmark = pytest.mark.django_db


APIS = ('api-v3', 'api-v4')


def tvm_ticket():
    return ServiceTicket({
        'dst': 'xxx',
        'debug_string': 'yyy',
        'logging_string': 'zzz',
        'scopes': '123',
        'src': 42,
    })


@pytest.fixture(name='tvm_ticket')
def tvm_ticket_fixture():
    return tvm_ticket()


@pytest.mark.parametrize('api', APIS)
def test_permission_middleware_active_with_can_edit(api, client, staff_factory):
    """
    Use staff instance with 'can_edit' permission

    """
    staff_with_can_edit = staff_factory('full_access')
    client.login(staff_with_can_edit.login)
    url = reverse('%s:service-member-list' % api)

    response = client.json.get(url, data={})
    assert response.status_code == 200

    response = client.json.post(url, data={})
    assert response.status_code == 400


@pytest.mark.parametrize('api', APIS)
def test_permission_middleware_without_can_edit(api, client, staff_factory):
    """
    Use staff instance without 'can_edit' permission

    """
    staff_only_view = staff_factory('own_only_viewer')
    client.login(staff_only_view.login)
    url = reverse('%s:service-member-list' % api)

    response = client.json.get(url, data={})
    assert response.status_code == 200

    response = client.json.post(url, data={})
    assert response.status_code == 403


AUTHORIZED_RESULTS = [
    (tvm_ticket(), AnonymousUser(), 200),
    (tvm_ticket(), None, 200),
    (None, AnonymousUser(), 403),
]


@pytest.mark.parametrize('api', APIS)
@pytest.mark.parametrize('tvm_ticket, user, result_code', AUTHORIZED_RESULTS)
def test_auth_anon_notvm(api, tvm_ticket, user, result_code):
    url = reverse('%s:service-member-list' % api)
    request = RequestFactory().get(url)
    request.user = user
    request.tvm_service_ticket = tvm_ticket
    view_func = resolve(url).func
    assert view_func(request).status_code == result_code
