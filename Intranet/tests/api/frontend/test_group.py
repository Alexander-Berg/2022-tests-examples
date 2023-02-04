# coding: utf-8

import datetime

import pytest
from django.utils.timezone import make_aware

from idm.users.models import Group
from idm.utils import reverse

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('api_name', ['frontend', 'v1'])
def test_get_group(client, arda_users, department_structure, api_name):
    """
    GET /frontend/groups/{id}/
    """
    client.login('frodo')
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    fellowship.description = 'Fellowship of the ring'
    fellowship.save()
    fellowship_url = reverse('api_dispatch_detail', api_name=api_name, resource_name='groups',
                             external_id=fellowship.external_id)
    response = client.json.get(fellowship_url)
    assert response.status_code == 200
    data = response.json()
    expected = {
        'id', 'slug', 'name', 'url', 'description', 'state',
        'created_at', 'type', 'updated_at', 'deprived_at',
    }
    if api_name == 'frontend':
        expected |= {'ancestors', 'responsibles'}
    assert set(data.keys()) == expected
    if api_name == 'frontend':
        assert data['name'] == 'Братство кольца'
    else:
        assert data['name'] == {
            'ru': 'Братство кольца',
            'en': 'Fellowship of the Ring',
        }
    assert data['url'] == 'https://staff.test.yandex-team.ru/departments/fellowship-of-the-ring/'
    assert data['id'] == fellowship.external_id
    assert data['state'] == 'active'
    assert data['description'] == 'Fellowship of the ring'
    if api_name == 'frontend':
        assert data['ancestors'] == [
            {'id': 101, 'name': 'Средиземье', 'url': 'middle-earth'},
            {'id': 103, 'name': 'Объединения', 'url': 'associations'},
        ]
        assert data['responsibles'] == [
            {'username': 'frodo', 'full_name': 'Фродо Бэггинс'},
            {'username': 'galadriel', 'full_name': 'galadriel'},
            {'username': 'gandalf', 'full_name': 'gandalf'},
            {'username': 'sam', 'full_name': 'sam'},
        ]

    # несуществующие группы выдают 404
    nonexistent_url = reverse('api_dispatch_detail', api_name='frontend', resource_name='groups',
                              external_id='foobar')
    response = client.json.get(nonexistent_url)
    assert response.status_code == 404


@pytest.mark.parametrize('api_name', ['frontend', 'v1'])
def test_get_groups_service_scopes(client, arda_users, api_name, django_assert_num_queries):
    client.login('frodo')
    groups_url = reverse('api_dispatch_list', api_name=api_name, resource_name='groups')
    root = Group.objects.create(type='service', level=0)
    service = Group.objects.create(type='service', parent=root)
    for _ in range(50):
        Group.objects.create(type='service', parent=service)

    with django_assert_num_queries(5):
        response = client.json.get(groups_url, {'type': 'service'})

    assert response.status_code == 200
    assert len(response.json()['objects']) == 51


@pytest.mark.parametrize('api_name', ['frontend', 'v1'])
def test_get_groups(client, arda_users, department_structure, api_name):
    """
    GET /frontend/groups/
    """
    client.login('frodo')
    fellowship = department_structure.fellowship
    groups_url = reverse('api_dispatch_list', api_name=api_name, resource_name='groups')
    response = client.json.get(groups_url)
    assert response.status_code == 200
    data = response.json()
    assert data['meta']['total_count'] == Group.objects.filter(level__gt=0).count()
    fellowship = next(item for item in data['objects'] if item['id'] == fellowship.external_id)
    assert fellowship.keys() == {
        'name', 'url', 'created_at', 'updated_at', 'slug', 'state', 'type', 'id',
    }
    if api_name == 'frontend':
        assert fellowship['name'] == 'Братство кольца'
    else:
        assert fellowship['name'] == {
            'ru': 'Братство кольца',
            'en': 'Fellowship of the Ring',
        }

    # type='department'
    department_structure.associations.type = 'service'
    department_structure.associations.save()
    data = client.json.get(groups_url, {'type': 'department'}).json()

    assert data['meta']['total_count'] == 6
    assert all(obj['type'] == 'department' for obj in data['objects'])

    department_structure.fellowship.mark_depriving()

    # is_active=True
    data = client.json.get(groups_url, {'is_active': True}).json()

    assert data['meta']['total_count'] == 6
    assert all(obj['state'] == 'active' for obj in data['objects'])

    # is_active=False
    data = client.json.get(groups_url, {'is_active': False}).json()

    assert data['meta']['total_count'] == 1
    assert all(obj['state'] in ('deprived', 'depriving') for obj in data['objects'])


@pytest.mark.parametrize('api_name', ['frontend', 'v1'])
def test_get_groups_updated(client, api_name, arda_users, department_structure):
    client.login('frodo')
    groups_url = reverse('api_dispatch_list', api_name=api_name, resource_name='groups')

    for i, group in enumerate(department_structure.values()):
        dt = make_aware(datetime.datetime.fromtimestamp(i*3600))
        Group.objects.filter(id=group.id).update(updated_at=dt)

    response = client.json.get(groups_url, {
        'updated__since': datetime.datetime.fromtimestamp(2*3600),
        'updated__until': datetime.datetime.fromtimestamp(4*3600),
    })

    assert response.status_code == 200
    assert len(response.json()['objects']) == 3


def test_get_root(client, arda_users, department_structure):
    """
    GET /frontend/groups/{id}/
    """
    client.login('frodo')

    root = Group.objects.get(slug='department')
    response = client.json.get(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='groups', external_id=root.external_id)
    )
    assert response.status_code == 404
