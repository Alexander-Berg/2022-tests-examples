import json
from unittest import mock

import pytest
from ad_system.tests.conftest import LdapMock
from django.conf import settings
from django.test import client as TestClient


@pytest.fixture
def fake_newldap(settings, monkeypatch):
    client = mock.MagicMock()

    def ldap_initialize(uri, *args, **kwargs):
        client.initialize(uri)
        return client

    monkeypatch.setattr('ldap.initialize', ldap_initialize)

    settings.AD_LDAP_HOST = 'ldap://fake.ldap.net'
    settings.CA_BUNDLE = 'fake_ca_bundle'
    settings.AD_LDAP_USERNAME = 'fake_user'
    settings.AD_LDAP_PASSWD = 'fake_password'
    return client


@mock.patch('ad_system.hooks.NewLDAP', LdapMock)
def test_add_role(client: TestClient):
    role_data = {
        'type': 'roles_in_groups',
        'ad_group': 'OU=group1',
        'group_roles': 'member',
    }
    resp = client.post('/idm/add-role/', {'login': 'frodo', 'role': json.dumps(role_data)})
    response = json.loads(resp.content)
    assert response.get('code') == 0


@mock.patch('ad_system.hooks.NewLDAP', LdapMock)
def test_add_responsible_role(client: TestClient):
    role_data = {
        'type': 'roles_in_groups',
        'ad_group': 'OU=group1',
        'group_roles': 'responsible',
    }
    resp = client.post('/idm/add-role/', {'login': 'login', 'role': json.dumps(role_data)})
    response = json.loads(resp.content)
    assert response.get('code') == 0


@mock.patch('ad_system.hooks.NewLDAP', LdapMock)
def test_add_existent_responsible_role(client: TestClient):
    role_data = {
        'type': 'roles_in_groups',
        'ad_group': 'OU=group1',
        'group_roles': 'responsible',
    }
    resp = client.post('/idm/add-role/', {'login': 'frodo', 'role': json.dumps(role_data)})
    response = json.loads(resp.content)
    assert response.get('code') == 500


@pytest.mark.parametrize('endpoint', [
    '/idm/add-role/',
    '/idm/remove-role/',
])
def test_fake_role(client: TestClient, endpoint):
    role_data = {
        'group_roles': '-'
    }
    resp = client.post(endpoint, {'login': 'login', 'role': json.dumps(role_data)})
    response = json.loads(resp.content)
    assert response.get('code') == 500


@mock.patch('ad_system.hooks.NewLDAP', LdapMock)
def test_remove_role(client: TestClient):
    role_data = {
        'type': 'roles_in_groups',
        'ad_group': 'OU=group1',
        'group_roles': 'member',
    }
    resp = client.post('/idm/remove-role/', {'login': 'sam', 'role': json.dumps(role_data)})
    response = json.loads(resp.content)
    assert response.get('code') == 0


@mock.patch('ad_system.hooks.NewLDAP', LdapMock)
def test_remove_responsible_role(client: TestClient):
    role_data = {
        'type': 'roles_in_groups',
        'ad_group': 'OU=group1',
        'group_roles': 'responsible',
    }
    resp = client.post('/idm/remove-role/', {'login': 'frodo', 'role': json.dumps(role_data)})
    response = json.loads(resp.content)
    assert response.get('code') == 0


@mock.patch('ad_system.hooks.NewLDAP', LdapMock)
def test_remove_nonexistent_responsible_role(client: TestClient):
    role_data = {
        'type': 'roles_in_groups',
        'ad_group': 'OU=group2',
        'group_roles': 'responsible',
    }
    resp = client.post('/idm/remove-role/', {'login': 'frodo', 'role': json.dumps(role_data)})
    response = json.loads(resp.content)
    assert response.get('code') == 500


@mock.patch('ad_system.hooks.NewLDAP', LdapMock)
def test_add_relation(client: TestClient):
    role_data = {
        'type': 'global',
        'role': 'system_group_relation',
    }
    fields_data = {
        'system': "test_system",
        'group_dn': "OU=group3",
    }
    resp = client.post('/idm/add-role/', {
        'login': 'login',
        'role': json.dumps(role_data),
        'fields': json.dumps(fields_data)
    })
    response = json.loads(resp.content)
    assert response.get('code') == 0 and response.get('data') == {
        'system': 'test_system',
        'group_dn': f'OU=group3',
    }


@mock.patch('ad_system.hooks.NewLDAP', LdapMock)
def test_add_relation_with_creating_ad_group(client: TestClient):
    role_data = {
        'type': 'global',
        'role': 'system_group_relation',
    }
    fields_data = {
        'system': 'test_system',
        'group_dn': f'CN=new_system,{settings.AD_LDAP_IDM_OU}'
    }
    resp = client.post('/idm/add-role/', {
        'login': 'login',
        'role': json.dumps(role_data),
        'fields': json.dumps(fields_data)
    })
    response = json.loads(resp.content)
    assert response.get('code') == 0 and response.get('data') == {
        'system': 'test_system',
        'group_dn': f'CN=new_system,{settings.AD_LDAP_IDM_OU}'
    }


@mock.patch('ad_system.hooks.NewLDAP', LdapMock)
def test_add_relation_on_exist_relation(client: TestClient):
    role_data = {
        'type': 'global',
        'role': 'system_group_relation',
    }
    fields_data = {
        'system': 'test_system',
        'group_dn': 'OU=group2'
    }
    resp = client.post('/idm/add-role/', {
        'login': 'login',
        'role': json.dumps(role_data),
        'fields': json.dumps(fields_data)
    })
    response = json.loads(resp.content)
    assert response.get('code') == 500


@mock.patch('ad_system.hooks.NewLDAP', LdapMock)
def test_add_relation_with_creating_wrong_ad_group(client: TestClient):
    role_data = {
        'type': 'global',
        'role': 'system_group_relation',
    }
    fields_data = {
        'system': 'test_system',
        'group_dn': f'OU=group'
    }
    resp = client.post('/idm/add-role/', {
        'login': 'login',
        'role': json.dumps(role_data),
        'fields': json.dumps(fields_data)
    })
    response = json.loads(resp.content)
    assert response.get('code') == 500


@mock.patch('ad_system.hooks.NewLDAP', LdapMock)
def test_remove_relation(client: TestClient):
    role_data = {
        'type': 'global',
        'role': 'system_group_relation'
    }
    fields_data = {
        'system': "kek",
        'group_dn': "OU=group2",
    }
    resp = client.post('/idm/remove-role/', {
        'login': 'login',
        'role': json.dumps(role_data),
        'fields': json.dumps(fields_data)
    })
    response = json.loads(resp.content)
    assert response.get('code') == 0


@mock.patch('ad_system.hooks.NewLDAP', LdapMock)
def test_remove_empty_relation(client: TestClient):
    role_data = {
        'type': 'global',
        'role': 'system_group_relation'
    }
    fields_data = {
        'system': "kek",
        'group_dn': "OU=group1",
    }
    resp = client.post('/idm/remove-role/', {
        'login': 'login',
        'role': json.dumps(role_data),
        'fields': json.dumps(fields_data)
    })
    response = json.loads(resp.content)
    assert response.get('code') == 500


@mock.patch('ad_system.hooks.NewLDAP', LdapMock)
def test_get_all_roles(client):
    resp = client.get('/idm/get-all-roles/')
    response = json.loads(resp.content)
    assert response.get('code') == 0
    assert response == {
        'code': 0,
        'users': [
            {
                'login': 'frodo',
                'roles': [
                    {'type': 'roles_in_groups', 'ad_group': 'OU=group1', 'group_roles': 'responsible'},
                    {'type': 'roles_in_groups', 'ad_group': 'OU=group2', 'group_roles': 'member'}
                ]
            },
            {
                'login': 'sam',
                'roles': [
                    {'type': 'roles_in_groups', 'ad_group': 'OU=group1', 'group_roles': 'member'}
                ]
            },
            {
                'login': settings.AD_IDM_USERNAME,
                'roles': [
                    [
                        {'type': 'global', 'role': 'system_group_relation'},
                        {'system': 'kek', 'group_dn': 'OU=group2'},
                    ],
                ]
            }
        ]
    }
