# coding: utf-8


import pytest

from idm.core.constants.rolefield import FIELD_TYPE
from idm.core.models import RoleNode, RoleField
from idm.users.models import Group
from idm.utils import reverse
# разрешаем использование базы в тестах
pytestmark = pytest.mark.django_db


def test_rolerequest_fields(client, complex_system_w_deps, arda_users):
    """POST /api/frontend/rolerequests/fields"""

    url = reverse('api_dispatch_list', api_name='frontend', resource_name='rolerequestfields')
    client.login('frodo')

    response = client.json.post(url, {
        'system': complex_system_w_deps.slug,
        'path': '/subs/developer/',
        'fields_data': None
    })
    assert response.status_code == 400

    response = client.json.post(url, {
        'system': complex_system_w_deps.slug,
        'path': '/subs/developer/',
        'fields_data': None,
        'user': 'frodo',
    })
    assert response.status_code == 200
    data = response.json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['slug'] == 'ring_type'
    assert data['objects'][0]['name'] == 'Тип кольца'
    assert data['objects'][0]['type'] == 'choicefield'

    response = client.json.post(url, {
        'system': complex_system_w_deps.slug,
        'path': '/subs/developer/',
        'fields_data': {},
        'user': 'frodo',
    })
    assert response.status_code == 200
    data = response.json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['slug'] == 'ring_type'

    response = client.json.post(url, {
        'system': complex_system_w_deps.slug,
        'path': '/subs/developer/',
        'fields_data': {
            'ring_type': 'mortalmen'
        },
        'user': 'frodo',
    })

    data = response.json()
    assert len(data['objects']) == 2
    assert data['objects'][0]['slug'] == 'ring_type'
    assert data['objects'][1]['slug'] == 'qty'

    response = client.json.post(url, {
        'system': complex_system_w_deps.slug,
        'path': '/subs/developer/',
        'fields_data': {
            'ring_type': 'mortalmen',
            'qty': 4
        },
        'user': 'frodo',
    })
    data = response.json()
    assert len(data['objects']) == 2
    assert data['objects'][0]['slug'] == 'ring_type'
    assert data['objects'][1]['slug'] == 'qty'

    response = client.json.post(url, {
        'system': complex_system_w_deps.slug,
        'path': '/subs/developer/',
        'fields_data': {
            'ring_type': 'mortalmen',
            'qty': 9
        },
        'user': 'frodo',
    })
    data = response.json()
    assert len(data['objects']) == 3
    assert data['objects'][0]['slug'] == 'ring_type'
    assert data['objects'][1]['slug'] == 'qty'
    assert data['objects'][2]['slug'] == 'omnipotence'


def test_fields_serialization(client, complex_system_w_deps, arda_users):
    """Проверка отдачи полей с выбором для фронтэнда"""
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='rolerequestfields')
    response = client.json.post(url, {
        'system': complex_system_w_deps.slug,
        'path': '/subs/developer/',
        'fields_data': {
            'ring_type': 'mortalmen',
            'qty': 9
        },
        'user': 'frodo',
    })

    expected = {
        'meta': {'offset': 0, 'limit': 20, 'next': None, 'previous': None, 'total_count': 3},
        'objects': [
            {
                'slug': 'ring_type',
                'name': 'Тип кольца',
                'type': 'choicefield',
                'required': False,
                'is_shareable': True,
                'options': {
                    'widget': 'radio',
                    'custom': False,
                    'choices': [
                        {
                            'value': 'elvenkings',
                            'name': 'Для королей эльфов',
                        },
                        {
                            'value': 'dwarflords',
                            'name': 'Для королей гномов',
                        },
                        {
                            'value': 'mortalmen',
                            'name': 'Для людей',
                        },
                        {
                            'value': 'darklord',
                            'name': 'Для Саурона',
                        }
                    ]
                }
            }, {
                'slug': 'qty',
                'name': 'Количество',
                'type': 'integerfield',
                'required': True,
                'is_shareable': True,
                'options': {
                    'placeholder': 'Число же!',
                    'default': 1,
                }
            }, {
                'slug': 'omnipotence',
                'name': 'Требуется всемогущество',
                'type': 'booleanfield',
                'required': False,
                'is_shareable': True,
            },
        ]
    }
    assert response.json() == expected


def test_suggest_fields(client, complex_system, arda_users, department_structure):
    """Для групп паспортные логины необязательны"""
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='rolerequestfields')
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/fields/macros/all')

    developer = RoleNode.objects.get(slug='developer')
    RoleField.objects.all().delete()
    developer.fields.create(
        type=FIELD_TYPE.SUGGEST,
        is_required=True,
        slug='network_macro',
        options={'suggest': 'macros'},
    )

    response = client.json.post(url, {
        'system': complex_system.slug,
        'path': '/subs/developer/',
        'user': 'frodo',
    })
    assert response.status_code == 200
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20, 'next': None, 'previous': None, 'total_count': 1},
        'objects': [
            {
                'slug': 'network_macro',
                'name': '',
                'type': 'suggestfield',
                'required': True,
                'is_shareable': True,
                'options': {'suggest': suggest_url},
            },
        ]
    }


def test_optional_logins_for_groups(client, complex_system, arda_users, department_structure):
    """Для групп паспортные логины необязательны"""
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='rolerequestfields')

    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    developer = RoleNode.objects.get(slug='developer')
    RoleField.objects.all().delete()
    for slug in ['passport-login', 'totallyrandomname']:
        developer.fields.create(
            type=FIELD_TYPE.PASSPORT_LOGIN,
            is_required=True,
            slug=slug,
        )

    response = client.json.post(url, {
        'system': complex_system.slug,
        'path': '/subs/developer/',
    })
    assert response.status_code == 400

    response = client.json.post(url, {
        'system': complex_system.slug,
        'path': '/subs/developer/',
        'user': 'frodo',
    })
    assert response.status_code == 200
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20, 'next': None, 'previous': None, 'total_count': 2},
        'objects': [
            {
                'slug': 'passport-login',
                'name': '',
                'type': 'passportlogin',
                'required': True,
                'is_shareable': False,
            },
            {
                'slug': 'totallyrandomname',
                'name': '',
                'type': 'passportlogin',
                'required': True,
                'is_shareable': False,
            },
        ]
    }

    response = client.json.post(url, {
        'system': complex_system.slug,
        'path': '/subs/developer/',
        'group': fellowship.external_id,
    })
    assert response.status_code == 200
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20, 'next': None, 'previous': None, 'total_count': 0},
        'objects': []
    }
