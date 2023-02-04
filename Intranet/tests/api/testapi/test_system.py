# coding: utf-8
"""
Тесты API открытого для тестировщиков на тестинге (triple-combo!)
"""


import pytest

from idm.core.models import System, Role, Workflow
from idm.tests.utils import set_workflow, add_perms_by_role, create_user
from idm.utils import reverse

# разрешаем использование базы в тестах
pytestmark = pytest.mark.django_db


def test_get_systems(client, simple_system, pt1_system, users_for_test):
    """
    GET /testapi/systems/
    """
    client.login('art')
    data = client.json.get(reverse('api_dispatch_list', api_name='testapi', resource_name='systems')).json()
    assert data['meta']['total_count'] == 2
    assert [system_data['slug'] for system_data in data['objects']] == ['simple', 'test1']
    assert all([system_data['is_active'] for system_data in data['objects']]) is True
    assert 'workflow' not in data['objects'][0]

    # check if only active systems in response
    pt1_system.is_active = False
    pt1_system.save()
    data2 = client.json.get(reverse('api_dispatch_list', api_name='testapi', resource_name='systems')).json()
    assert data2['meta']['total_count'] == 1
    assert data2['objects'][0]['slug'] == 'simple'


def test_get_system(client, simple_system, users_for_test):
    """
    GET /testapi/systems/simple/
    """
    client.login('art')
    data = client.json.get(reverse('api_dispatch_detail', api_name='testapi', resource_name='systems', slug='simple')
                           ).json()
    assert data['slug'] == 'simple'
    assert data['is_active'] is True
    assert 'workflow' not in data


def test_create_system(client, users_for_test):
    """
    POST /testapi/systems/
    """
    (art, fantom, terran, admin) = users_for_test

    client.login('art')
    data = {
        'slug': 'testsystem',
        'workflow': 'approvers = []',
        'url': 'http://example.com',
    }

    client.json.post(reverse('api_dispatch_list', api_name='testapi', resource_name='systems'), data=data)

    assert System.objects.count() == 1

    system = System.objects.get()
    assert system.slug == 'testsystem'
    assert system.base_url == 'http://example.com'

    system.fetch_actual_workflow()
    workflow = system.actual_workflow
    assert 'approvers = []' == workflow.workflow

    response = client.delete(
        reverse('api_dispatch_detail', api_name='testapi', resource_name='systems', slug=system.slug),
        content_type='application/json'
    )

    assert response.status_code == 204
    assert System.objects.count() == 0
    assert Workflow.objects.count() == 0
