# coding: utf-8
import json

from mock import patch

from django.test.client import Client
from django.core.urlresolvers import reverse


api_resources = {
    'group', 'room', 'office', 'equipment', 'departmentstaff', 'person',
    'groupmembership', 'position', 'organization', 'geography', 'table', 'occupation',
}


def test_info():
    client = Client()
    response = client.get(reverse('idm-api:info'))
    assert response.status_code == 200
    info_data = json.loads(response.content)
    assert info_data['code'] == 0
    assert info_data['roles']['slug'] == 'role_type'

    assert info_data['roles']['values'].keys() == ['resource_access']
    resources = info_data['roles']['values']['resource_access']['roles']
    assert resources['slug'] == 'resource'
    assert set(resources['values'].keys()) == api_resources

    for resource_name in api_resources:
        roles = resources['values'][resource_name]['roles']
        assert roles['slug'] == 'access_type'
        assert 'full_access' in roles['values']
        assert 'partial_access' in roles['values']
        url_field = roles['values']['partial_access']['fields'][0]
        assert url_field['slug'] == 'access_url'
        assert url_field['required'] == True


def test_get_all_roles(test_idm_roles_collection):
    test_idm_roles_collection.insert_one(
        {
            'role_type': 'resource_access',
            'subject': 'user1',
            'subject_type': 'user',
            'resource': 'person',
            'role': 'full_access',
            'fields': {},
            'deleted': False,
        }
    )
    test_idm_roles_collection.insert_one(
        {
            'role_type': 'resource_access',
            'subject': 'user2',
            'subject_type': 'user',
            'resource': 'department',
            'role': 'partial_access',
            'fields': {
                'access_url': 'https://staff-api.test.yandex-team.ru/v3/offices'
                              '?_fields=code,name,city.country,city.id'
            },
            'deleted': False,
        }
    )

    client = Client()
    response = client.get(reverse('idm-api:get-all-roles'))
    assert response.status_code == 200
    all_roles_data = json.loads(response.content)
    assert all_roles_data['code'] == 0
    users = all_roles_data['users']
    assert len(users) == 2
    users = sorted(users, key=lambda x: x['login'])
    assert users == [
        {
            'login': 'user1',
            'subject_type': 'user',
            'roles': [
                {'resource': 'person', 'role_type': 'resource_access', 'access_type': 'full_access'}
            ]
        },
        {
            'login': 'user2',
            'subject_type': 'user',
            'roles': [
                [
                    {
                        'resource': 'department',
                        'role_type': 'resource_access',
                        'access_type': 'partial_access',
                    },
                    {
                        'access_url': 'https://staff-api.test.yandex-team.ru/v3/offices?_fields=code,name,city.country,city.id'
                    },
                ],
            ]
        },
    ]


def test_get_roles(test_idm_roles_collection):
    raw_roles = (
        {
            'role_type': 'resource_access',
            'subject': 'user1',
            'subject_type': 'user',
            'resource': 'person',
            'role': 'full_access',
            'fields': {},
            'deleted': False,
        },
        {
            'role_type': 'resource_access',
            'subject': 'user2',
            'subject_type': 'user',
            'resource': 'department',
            'role': 'partial_access',
            'fields': {
                'access_url': 'https://staff-api.test.yandex-team.ru/v3/offices'
                              '?_fields=code,name,city.country,city.id'
            },
            'deleted': False,
        },
        {
            'role_type': 'resource_access',
            'subject': 'user1',
            'subject_type': 'user',
            'resource': 'group',
            'role': 'full_access',
            'fields': {},
            'deleted': False,
        },
    )


    test_idm_roles_collection.insert_many(raw_roles, ordered=True)

    client = Client()

    # first hit
    response = client.get(reverse('idm-api:get-roles') + '?limit=2')
    assert response.status_code == 200
    roles_data = json.loads(response.content)
    assert roles_data['code'] == 0
    assert 'next-url' in roles_data
    next_url = roles_data['next-url']
    roles = roles_data['roles']
    assert len(roles) == 2
    assert roles[0] == {
        'path': '/{role_type}/{resource}/{role}/'.format(**raw_roles[0]),
        'login': raw_roles[0]['subject'],
    }
    assert roles[1] == {
        'path': '/{role_type}/{resource}/{role}/'.format(**raw_roles[1]),
        'login': raw_roles[1]['subject'],
        'fields': raw_roles[1]['fields'],
    }

    # second hit
    response = client.get(next_url)
    assert response.status_code == 200
    roles_data = json.loads(response.content)
    assert roles_data['code'] == 0
    assert 'next-url' not in roles_data
    roles = roles_data['roles']
    assert len(roles) == 1
    assert roles == [{
        'path': '/{role_type}/{resource}/{role}/'.format(**raw_roles[2]),
        'login': raw_roles[2]['subject'],
    }]

def test_add_person_role(test_idm_roles_collection):
    client = Client()
    url_field_value = (
        'https://staff-api.test.yandex-team.ru/v3/offices?_fields=code,name,city.country,city.id'
    )
    request_role_kwargs = {
        'login': 'user1',
        'subject_type': 'user',
        'fields': '{"access_url": "%s"}' % url_field_value,
        'role': '{"resource": "office", "role_type": "resource_access", '
                '"access_type": "partial_access"}',
        'path': '/role_type/resource_access/resource/office/access_type/partial_access/',
    }
    url = reverse('idm-api:add-role')
    with patch('staff_api.v3_0.idm.hooks.get_person_id_by_login', return_value=10693):
        with patch(
                'staff_api.v3_0.idm.resource_access_actions.get_person_login_by_id',
                return_value='user1'
        ):
            response = client.post(url, data=request_role_kwargs)
    assert response.status_code == 200
    response_data = json.loads(response.content)
    assert response_data == {
        'code': 0,
        'data': {'access_url': url_field_value},
    }

    existing_role = test_idm_roles_collection.find_one({}, {'_id': 0})
    assert existing_role == {
        'subject_type': 'user',
        'resource': 'office',
        'fields': {
            'access_url': url_field_value
        },
        'role': 'partial_access',
        'role_type': 'resource_access',
        'subject': 'user1',
        'handled': False,
        'deleted': False,
    }


def test_remove_person_role(test_idm_roles_collection):
    client = Client()
    test_idm_roles_collection.insert_one(
        {
            'role_type': 'resource_access',
            'subject': 'user1',
            'subject_type': 'user',
            'resource': 'person',
            'role': 'full_access',
            'fields': {},
            'handled': True,
            'deleted': False,
        }
    )

    remove_role_kwargs = {
        'login': 'user1',
        'subject_type': 'user',
        'fields': '{}',
        'role': '{"resource": "person", "role_type": "resource_access", '
                '"access_type": "full_access"}',
        'path': '/role_type/resource_access/resource/person/access_type/full_access/',
    }
    url = reverse('idm-api:remove-role')
    with patch('staff_api.v3_0.idm.hooks.get_person_id_by_login', return_value=10693):
        with patch(
                'staff_api.v3_0.idm.resource_access_actions.get_person_login_by_id',
                return_value='user1'
        ):
            response = client.post(url, data=remove_role_kwargs)

    assert response.status_code == 200
    assert response.content == '{"code": 0}'

    existing_role = test_idm_roles_collection.find_one({}, {'_id': 0})
    assert existing_role['deleted']
    assert not existing_role['handled']
