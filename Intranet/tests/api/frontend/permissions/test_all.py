# coding: utf-8


import pytest

from django.conf import settings

from idm.core import models
from idm.utils import reverse

from idm.tests.utils import add_perms_by_role, setify

pytestmark = pytest.mark.django_db


def test_permissions_all(client, simple_system, arda_users):
    """GET /permissions/all/"""

    frodo = arda_users.frodo
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='permissions/all')
    empty_response = {'data': [], 'meta': {'total_count': 0}}

    response = client.json.get(url)
    assert response.status_code == 200
    assert response.json() == empty_response

    response = client.json.get(url, {'system': simple_system.slug})
    assert response.status_code == 200
    assert response.json() == empty_response

    add_perms_by_role('developer', frodo)
    perms = set()
    for x in settings.IDM_COMMON_ROLES_PERMISSIONS['developer']:
        if not x.startswith('idm_'):
            perms.add(x)
        else:
            perms.add(x[4:])
    expected_data = {'data': perms, 'meta': {'total_count': len(perms)}}

    response = client.json.get(url)
    assert response.status_code == 200
    assert setify(response.json()) == expected_data

    assert setify(client.json.get(url, {'system': simple_system.slug}).json()) == expected_data

    add_perms_by_role('roles_manage', frodo, simple_system)

    response = client.json.get(url)
    assert response.status_code == 200
    assert setify(response.json()) == expected_data

    response = client.json.get(url, {'system': simple_system.slug})
    assert response.status_code == 200
    for x in settings.IDM_SYSTEM_ROLES_PERMISSIONS['roles_manage']:
        if not x.startswith('idm_'):
            perms.add(x)
        else:
            perms.add(x[4:])
    assert setify(response.json()) == {'data': perms, 'meta': {'total_count': len(perms)}}


def test_local_permissions_dont_get_into_api(client, simple_system, arda_users):
    """Проверим, что неглобальные пермишены не выгружаются в ручку"""

    frodo = arda_users.frodo
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='permissions/all')
    add_perms_by_role('roles_manage', frodo, simple_system, '/superuser/')
    empty_response = {'data': [], 'meta': {'total_count': 0}}

    response = client.json.get(url)
    assert response.status_code == 200
    assert response.json() == empty_response

    response = client.json.get(url, {'system': simple_system.slug})
    assert response.status_code == 200
    assert response.json() == empty_response

    add_perms_by_role('roles_manage', frodo, simple_system, '/')
    response = client.json.get(url)
    assert response.status_code == 200
    assert response.json() == empty_response

    response = client.json.get(url, {'system': simple_system.slug})
    assert response.status_code == 200
    perms = set()
    for x in settings.IDM_SYSTEM_ROLES_PERMISSIONS['roles_manage']:
        if not x.startswith('idm_'):
            perms.add(x)
        else:
            perms.add(x[4:])
    assert setify(response.json()) == {'data': set(perms), 'meta': {'total_count': len(perms)}}


def test_scopes(client, complex_system, arda_users):
    """Проверим, что проверка скоупов работае"""

    frodo = arda_users.frodo
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='permissions/all')
    add_perms_by_role('roles_manage', frodo, complex_system, '/subs/manager/')

    response = client.json.get(url, {'system': complex_system.slug, 'path': '/rules/'})
    assert response.status_code == 200
    assert response.json() == {'data': [], 'meta': {'total_count': 0}}

    response = client.json.get(url, {'system': complex_system.slug, 'path': '/subs/manager/'})
    assert response.status_code == 200
    perms = set()
    for x in settings.IDM_SYSTEM_ROLES_PERMISSIONS['roles_manage']:
        if not x.startswith('idm_'):
            perms.add(x)
        else:
            perms.add(x[4:])
    assert setify(response.json()) == {'data': perms, 'meta': {'total_count': len(perms)}}



def test_creator_and_responsible(client, simple_system, arda_users):
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='permissions/all')
    client.login('frodo')
    frodo = arda_users.frodo

    simple_system.creator = frodo
    simple_system.save()
    response = client.json.get(url, {'system': simple_system.slug, 'path': '/rules/'})
    creator_data = response.json()

    simple_system.creator = frodo
    simple_system.save()
    add_perms_by_role('responsible', frodo, simple_system)
    response = client.json.get(url, {'system': simple_system.slug, 'path': '/rules/'})
    responsible_data = response.json()

    assert creator_data == responsible_data