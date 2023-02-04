# coding: utf-8


import pytest

from idm.users.models import Group
from idm.core.models import Role
from idm.utils import reverse
from idm.services.models import Service

from idm.tests.utils import set_workflow

pytestmark = [pytest.mark.django_db]


@pytest.fixture
def services():
    top_service = Service.objects.create(
        slug='',
        name='',
        external_id=0,
        membership_inheritance=True,
        parent=None,
    )
    service1 = Service.objects.create(
        slug='1',
        external_id=1,
        name='1',
        membership_inheritance=True,
        parent=top_service,
    )
    service2 = Service.objects.create(
        slug='2',
        external_id=2,
        name='2',
        membership_inheritance=True,
        parent=service1,
    )
    service3 = Service.objects.create(
        slug='3',
        name='3',
        external_id=3,
        membership_inheritance=True,
        parent=service2,
    )
    service4 = Service.objects.create(
        slug='4',
        external_id=4,
        name='4',
        membership_inheritance=True,
        parent=service3,
    )
    service5 = Service.objects.create(
        slug='5',
        external_id=5,
        membership_inheritance=False,
        name='5',
        parent=service4,
    )
    service6 = Service.objects.create(
        slug='6',
        external_id=6,
        name='6',
        membership_inheritance=False,
        parent=service1,
    )
    service7 = Service.objects.create(
        slug='7',
        name='7',
        external_id=7,
        membership_inheritance=True,
        parent=service6,
    )
    return [service1, service2, service3, service4, service5, service6, service7]


@pytest.fixture
def groups(services):
    group0 = Group.objects.create(
        type='wiki',
        slug='',
        name='',
        external_id=1000,
        parent=None
    )
    groups = [group0]
    for service in services:
        group_slug = 'svc_' + service.slug
        groups.append(Group.objects.create(
            type='service',
            slug=group_slug,
            name=service.name,
            external_id=1000 + service.external_id,
            parent=group0
        ))
    group8 = Group.objects.create(
        type='department',
        slug='department',
        name='department',
        external_id=2000,
        parent=group0
    )
    groups.append(group8)
    return groups


@pytest.fixture
def roles(groups, flat_arda_users, simple_system):
    a = 'admin'
    b = 'manager'
    c = 'poweruser'
    d = 'superuser'
    simple_system.request_policy = 'anyone'
    simple_system.save(update_fields=['request_policy'])
    set_workflow(simple_system, 'approvers=[]', 'approvers=[]')
    frodo = flat_arda_users.frodo
    c1 = Role.objects.request_role(frodo, groups[1], simple_system, '', {'role': c})
    a2 = Role.objects.request_role(frodo, groups[2], simple_system, '', {'role': a})
    b3 = Role.objects.request_role(frodo, groups[3], simple_system, '', {'role': b})
    b4 = Role.objects.request_role(None, groups[4], simple_system, '', {'role': b}, parent=b3)
    b5 = Role.objects.request_role(frodo, groups[5], simple_system, '', {'role': b})
    a4 = Role.objects.request_role(frodo, groups[4], simple_system, '', {'role': a})
    e6 = Role.objects.request_role(frodo, groups[6], simple_system, '', {'role': b}, {'login': 'frodo'})
    b7 = Role.objects.request_role(frodo, groups[7], simple_system, '', {'role': b})
    d7 = Role.objects.request_role(frodo, groups[7], simple_system, '', {'role': d})
    return groups, [c1, a2, b3, a4, e6, b7, d7, b4, b5]


def test_get_changed_roles_same_inheritance(client, roles, flat_arda_users):
    """
    GET /frontend/group_changed_roles/
    """
    groups, roles = roles
    client.login(flat_arda_users.frodo)
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='group_changed_roles',)

    response = client.json.get(url, {'group': groups[5].external_id, 'membership_inheritance': False})
    assert response.status_code == 400
    assert response.json()['message'] == 'Nothing changes'


def test_get_changed_roles_same_parent(client, roles, flat_arda_users):
    """
    GET /frontend/group_changed_roles/
    """
    groups, roles = roles
    client.login(flat_arda_users.frodo)
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='group_changed_roles',)
    response = client.json.get(url, {'group': groups[5].external_id, 'parent': groups[4].external_id})
    assert response.status_code == 400
    assert response.json()['message'] == 'Nothing changes'


def test_get_changed_roles_move_down(client, roles, flat_arda_users):
    """
    GET /frontend/group_changed_roles/
    """
    groups, roles = roles
    client.login(flat_arda_users.frodo)
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='group_changed_roles',)
    response = client.json.get(url, {'group': groups[3].external_id, 'parent': groups[4].external_id})
    assert response.status_code == 400
    assert response.json()['message'] == 'Descendant cannot be a new parent'


def test_get_changed_roles_wrong_type_of_group(client, roles, flat_arda_users):
    """
    GET /frontend/group_changed_roles/
    """
    groups, roles = roles
    client.login(flat_arda_users.frodo)
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='group_changed_roles',)
    response = client.json.get(url, {'group': groups[0].external_id, 'parent': groups[4].external_id})
    assert response.status_code == 400
    assert response.json()['message'] == 'Invalid data sent'
    assert response.json()['errors'] == {'group': ['works only with services and departments']}


def test_get_changed_roles_inheritance_with_deparment(client, roles, flat_arda_users):
    """
    GET /frontend/group_changed_roles/
    """
    groups, roles = roles
    client.login(flat_arda_users.frodo)
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='group_changed_roles',)
    response = client.json.get(url, {'group': groups[8].external_id, 'membership_inheritance': False})
    assert response.status_code == 400
    assert response.json()['message'] == 'Invalid data sent'
    assert response.json()['errors'] == {'group': ['only services has membership_inheritance']}


def test_get_changed_roles_inheritance_without_group(client, roles, flat_arda_users):
    """
    GET /frontend/group_changed_roles/
    """
    client.login(flat_arda_users.frodo)
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='group_changed_roles',)
    response = client.json.get(url, {'membership_inheritance': False})
    assert response.status_code == 400
    assert response.json()['message'] == 'Invalid data sent'
    assert response.json()['errors'] == {'group': ['Обязательное поле.']}


def test_get_changed_roles_parent_of_other_type(client, roles, flat_arda_users):
    """
    GET /frontend/group_changed_roles/
    """
    groups, roles = roles
    client.login(flat_arda_users.frodo)
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='group_changed_roles',)
    response = client.json.get(url, {'group': groups[7].external_id, 'parent': groups[8].external_id})
    assert response.status_code == 400
    assert response.json()['message'] == 'Group and parent should have the same type'


def test_get_changed_roles_no_parent_or_inheritance(client, roles, flat_arda_users):
    """
    GET /frontend/group_changed_roles/
    """
    groups, roles = roles
    client.login(flat_arda_users.frodo)
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='group_changed_roles',)
    response = client.json.get(url, {'group': groups[8].external_id})
    assert response.status_code == 400
    assert response.json()['message'] == 'exactly one of fields parent or membership_inheritance should be defined'


def test_get_changed_roles_both_parent_and_inheritance(client, roles, flat_arda_users):
    """
    GET /frontend/group_changed_roles/
    """
    groups, roles = roles
    client.login(flat_arda_users.frodo)
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='group_changed_roles',)
    response = client.json.get(url, {
        'group': groups[8].external_id,
        'parent': groups[4].external_id,
        'membership_inheritance': True,
    })
    assert response.status_code == 400
    assert response.json()['message'] == 'exactly one of fields parent or membership_inheritance should be defined'


def test_get_changed_roles_new_parent(client, roles, flat_arda_users):
    """
    GET /frontend/group_changed_roles/
    """
    groups, roles = roles
    c1, a2, b3, a4, e6, b7, d7, b4, b5 = roles
    client.login(flat_arda_users.frodo)
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='group_changed_roles',)
    response = client.json.get(url, {
        'group': groups[3].external_id,
        'parent': groups[7].external_id,
    })
    assert response.status_code == 200
    result = response.json()
    clean_result(result)
    assert_contains(result, [
        {
            'group': groups[3],
            'lost': {c1, a2},
            'obtained': {d7, e6},
        },
        {
            'group': groups[4],
            'lost': {c1},
            'obtained': {d7, e6},
        },
    ])

def test_get_changed_roles_new_parent_with_departments(client, roles, flat_arda_users):
    """
    GET /frontend/group_changed_roles/
    """
    groups, roles = roles
    for group in groups[1:-1]:
        parent = Group.objects.get(external_id=group.get_service().parent.external_id + 1000)
        group.type='department'
        group.parent = parent
        group.save()
    c1, a2, b3, a4, e6, b7, d7, b4, b5 = roles
    client.login(flat_arda_users.frodo)
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='group_changed_roles', )
    response = client.json.get(url, {
        'group': groups[3].external_id,
        'parent': groups[7].external_id,
    })
    assert response.status_code == 200
    result = response.json()
    clean_result(result)
    assert_contains(result, [
        {
            'group': groups[3],
            'lost': {a2},
            'obtained': {d7, e6},
        },
        {
            'group': groups[4],
            'lost': set(),
            'obtained': {d7, e6},
        },
        {
            'group': groups[5],
            'lost': set(),
            'obtained': {d7, e6},
        },
    ])


def test_get_changed_roles_remove_inheritance(client, roles, flat_arda_users):
    """
    GET /frontend/group_changed_roles/
    """
    groups, roles = roles
    c1, a2, b3, a4, e6, b7, d7, b4, b5 = roles
    client.login(flat_arda_users.frodo)
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='group_changed_roles',)
    response = client.json.get(url, {
        'group': groups[3].external_id,
        'membership_inheritance': False,
    })
    assert response.status_code == 200
    result = response.json()
    clean_result(result)
    assert_contains(result, [
        {
            'group': groups[3],
            'lost': {c1, a2},
            'obtained': set(),
        },
        {
            'group': groups[4],
            'lost': {c1},
            'obtained': set(),
        },
    ])


def test_get_changed_roles_add_inheritance(client, roles, flat_arda_users):
    """
    GET /frontend/group_changed_roles/
    """
    groups, roles = roles
    c1, a2, b3, a4, e6, b7, d7, b4, b5 = roles
    client.login(flat_arda_users.frodo)
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='group_changed_roles',)
    response = client.json.get(url, {
        'group': groups[6].external_id,
        'membership_inheritance': True,
    })
    assert response.status_code == 200
    result = response.json()
    clean_result(result)
    assert_contains(result, [
        {
            'group': groups[6],
            'lost': set(),
            'obtained': {c1},
        },
        {
            'group': groups[7],
            'lost': set(),
            'obtained': {c1},
        },
    ])


def test_get_changed_roles_add_inheritance_for_role_with_parent(client, roles, flat_arda_users):
    """
    GET /frontend/group_changed_roles/
    """
    groups, roles = roles
    c1, a2, b3, a4, e6, b7, d7, b4, b5 = roles
    client.login(flat_arda_users.frodo)
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='group_changed_roles',)
    Service.objects.filter(external_id=4).update(membership_inheritance=False)
    response = client.json.get(url, {
        'group': groups[5].external_id,
        'membership_inheritance': True,
    })
    assert response.status_code == 200
    result = response.json()
    role_with_parent = result[0]['obtained'][0 if result[0]['obtained'][1]['parent'] is None else 1]
    assert role_with_parent['parent']['id'] == b3.id
    clean_result(result)
    assert result == [
        {
            'group': groups[5],
            'lost': set(),
            'obtained': {b4, a4},
        },
    ]


def clean_result(result):
    for table in result:
        table['group'] = Group.objects.get(external_id=table['group']['id'])
        table['lost'] = {Role.objects.get(id=role['id']) for role in table['lost']}
        table['obtained'] = {Role.objects.get(id=role['id']) for role in table['obtained']}

def assert_contains(container, checklist):
    for obj in checklist:
        assert obj in container
