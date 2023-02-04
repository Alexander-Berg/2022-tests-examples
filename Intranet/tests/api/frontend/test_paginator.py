# coding: utf-8


import pytest
from idm.utils import reverse

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('order_by', ['id', '-id', '-updated', 'system', None])
@pytest.mark.parametrize('api', ['frontend', 'v1'])
def test_roles_order_by_id(client, simple_system, order_by, arda_users_with_roles, api):
    limit = 5
    client.login('frodo')
    kwargs = {
        'system': 'simple',
        'limit': str(limit),
    }
    if order_by is not None:
        kwargs['order_by'] = order_by
        roles = simple_system.roles.order_by(order_by)
    else:
        roles = simple_system.roles
    roles_id = sorted([role.id for role in roles.iterator()])
    return_roles_id = []
    response = client.json.get(reverse('api_dispatch_list', api_name=api, resource_name='roles'), kwargs)
    for _ in range(roles.count()//limit + 1):
        assert response.status_code == 200
        data = response.json()
        if data['meta']['next']:
            if order_by in ('id', '-id'):
                last_key = roles[limit-1].id
                limit += 5
                assert 'last_key=%s' % last_key in data['meta']['next']
            else:
                assert 'last_key' not in data['meta']['next']
            return_roles_id += [role['id'] for role in data['objects']]
            response = client.json.get(data['meta']['next'])
    assert data['meta']['next'] is None
    return_roles_id += [role['id'] for role in data['objects']]
    if order_by != 'system':
        assert sorted(return_roles_id) == roles_id


@pytest.mark.parametrize('api', ['frontend', 'v1'])
def test_no_meta(client, simple_system, arda_users_with_roles, api):
    client.login('frodo')
    limit = 5
    offset = 0
    kwargs = {
        'system': 'simple',
        'limit': limit,
        'offset': offset,
    }
    response = client.json.get(reverse('api_dispatch_list', api_name=api, resource_name='roles'), kwargs)
    data = response.json()
    assert data['meta']['total_count'] == simple_system.roles.count()
    count = data['meta']['total_count']
    assert len(data['objects']) == limit
    kwargs.update({'no_meta': True})
    roles_recieved = 0
    roles_id = []
    while offset < count:
        response = client.json.get(reverse('api_dispatch_list', api_name=api, resource_name='roles'), kwargs)
        data = response.json()
        assert data.get('meta') is None
        offset += limit
        roles_recieved += len(data['objects'])
        roles_id += [role['id'] for role in data['objects']]
        kwargs.update({'offset': offset})
    assert roles_recieved == count
    assert sorted(roles_id) == sorted(simple_system.roles.values_list('id', flat=True))


@pytest.mark.parametrize('api', ['frontend', 'v1'])
def test_only_meta(client, simple_system, arda_users_with_roles, api):
    client.login('frodo')
    limit = 5
    offset = 0
    kwargs = {
        'system': 'simple',
        'limit': limit,
        'offset': offset,
        'only_meta': True,
    }
    response = client.json.get(reverse('api_dispatch_list', api_name=api, resource_name='roles'), kwargs)
    data = response.json()
    assert data['meta']['total_count'] == simple_system.roles.count()
    assert list(data.keys()) == ['meta']
