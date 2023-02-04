# coding: utf-8

import pytest

from idm.core.constants.system import SYSTEM_GROUP_POLICY, SYSTEM_CHECKS
from idm.nodes.updatable import UpdatableNode
from idm.users.constants.group import GROUP_TYPES
from idm.users.models import Group
from idm.utils import reverse

pytestmark = pytest.mark.django_db


def test_rolerequestadditional_fields_with_user(client, complex_system_w_deps, arda_users):
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='rolerequestadditionalfields')
    client.login('frodo')

    response = client.json.get(url, {
        'system': complex_system_w_deps.slug,
        'user': 'frodo',
    })
    assert response.status_code == 200
    data = response.json()['objects']
    assert data == []


def test_rolerequestadditional_fields_empty(client, arda_users):
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='rolerequestadditionalfields')
    client.login('frodo')

    response = client.get(url)
    assert response.status_code == 400


@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.ALL_POLICIES)
@pytest.mark.parametrize('group_type', GROUP_TYPES.ALL_GROUPS)
def test_rolerequestadditional_fields_with_group_wrong_type(client, complex_system, arda_users,
                                                            department_structure, group_policy, group_type):
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='rolerequestadditionalfields')

    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    fellowship.type = group_type
    fellowship.save()

    complex_system.group_policy = group_policy
    complex_system.save()

    response = client.json.get(url, {
        'system': complex_system.slug,
        'group': fellowship.external_id,
    })
    if group_type == GROUP_TYPES.TVM_SERVICE:
        assert response.status_code == 400
        return
    assert response.status_code == 200
    data = response.json()['objects']

    if group_policy == SYSTEM_GROUP_POLICY.UNAWARE and group_type in GROUP_TYPES.USER_GROUPS:
        slugs = list(SYSTEM_CHECKS.SLUGS)
        if group_type != GROUP_TYPES.DEPARTMENT:
            slugs.remove(SYSTEM_CHECKS.WITH_INHERITANCE)
    else:
        slugs = []

    expected_data = [
        {
            'slug': slug,
            'name': SYSTEM_CHECKS.NAMES[slug],
            'type': 'bool',
        }
        for slug in slugs
    ]
    assert sorted(data, key=lambda x: x['slug']) == sorted(expected_data, key=lambda x: x['slug'])


def test_rolerequestadditional_fields_with_inactive_group(client, complex_system, arda_users,
                                                          department_structure):
    client.login('frodo')
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='rolerequestadditionalfields')

    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    fellowship.state = UpdatableNode.INACTIVE_STATES[0]
    fellowship.save()

    response = client.json.get(url, {
        'system': complex_system.slug,
        'group': fellowship.external_id,
    })
    assert response.status_code == 400
