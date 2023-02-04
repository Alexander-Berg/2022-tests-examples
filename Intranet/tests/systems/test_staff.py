# coding: utf-8


import pytest
import re
import six

from idm.core.workflow.exceptions import AccessDenied
from idm.core.models import Role
from idm.tests.utils import (set_workflow, create_system, refresh, create_group_structure,
                             DEFAULT_WORKFLOW, attrdict)
from idm.users.constants.group import GROUP_TYPES
from idm.users.models import Group

from idm.core.workflow.exceptions import RoleNodeDoesNotExist


pytestmark = [pytest.mark.django_db]


base_group_workflow = """
if role.get('role') == 'member':
    group_id = int(role['group'])
    requested_group = groupify(group_id)
    if group.external_id == group_id:
        raise AccessDenied(u'Нельзя запросить роль участника группы для участников самой этой группы')
    data = {'type': 'groups', 'role': 'member', 'group': str(group.external_id)}
    groups = [group_ for group_ in system.all_groups_with_role(data, among_states='returnable')
                if group_.type == 'wiki']
    if groups:
        raise AccessDenied(u'Нельзя запросить роль на вики-группу, уже собираемую из другой вики-группы: %s' %
            ', '.join(group_.name for group_ in groups)
        )
"""

group_workflow_approverless = base_group_workflow + "approvers = []"
group_workflow_with_approvers = base_group_workflow + "approvers = ['legolas']"


@pytest.fixture
def wiki_groups(group_roots):
    root_wiki = group_roots[2]
    structure = [
        {
            'slug': 'wiki_a',
            'external_id': 200,
            'name': {
                'ru': 'Вики-группа A',
                'en': 'Wiki-group A',
            },
        },
        {
            'slug': 'wiki_b',
            'external_id': 201,
            'name': {
                'ru': 'Вики-группа Б',
                'en': 'Wiki-group B',
            },
        },
        {
            'slug': 'wiki_c',
            'external_id': 202,
            'name': {
                'ru': 'Вики-группа C',
                'en': 'Wiki-group C',
            },
        },
    ]
    create_group_structure(structure, root_wiki, GROUP_TYPES.WIKI)
    return attrdict({slug: Group.objects.get(slug=slug) for slug in ('wiki_a', 'wiki_b', 'wiki_c')})


@pytest.fixture
def staff_system(wiki_groups):
    groupinfo = {
        'name': 'Group name',
        'roles': {
            'values': {
                'member': 'Участник',
                'responsible': 'Ответственный'
            },
            'slug': 'role',
            'name': 'Роль'
        }
    }

    data = {
        'code': 0,
        'roles': {
            'slug': 'type',
            'name': 'Тип',
            'values': {
                'groups': {
                    'name': 'Управление группами',
                    'roles': {
                        'slug': 'group',
                        'values': {
                            six.text_type(group.external_id): groupinfo for group in Group.objects.filter(type='wiki')
                        }
                    }
                }
            }
        }
    }

    system = create_system('staff', name='Staff', public=True, role_tree=data)
    set_workflow(system, DEFAULT_WORKFLOW, group_code=group_workflow_approverless)
    return system


@pytest.fixture
def staff_with_approvers(staff_system):
    set_workflow(staff_system, DEFAULT_WORKFLOW, group_code=group_workflow_with_approvers)
    return staff_system


def test_deparment_group_request_is_ok(staff_system, arda_users, wiki_groups, department_structure):
    """Запрос на не-вики группу всегда разрешён"""

    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    wiki_a = wiki_groups.wiki_a
    wiki_a_data = {'type': 'groups', 'group': str(wiki_a.external_id), 'role': 'member'}
    with pytest.raises(RoleNodeDoesNotExist) as err:
        Role.objects.request_role(frodo, frodo, staff_system, '', {'role': 'admin'}, None)
    assert str(err.value) == 'Указан несуществующий узел: {role: admin}'


def test_selfrequest_is_forbidden(staff_system, arda_users, wiki_groups):
    """Проверим, что нельзя запросить роль на саму себя"""

    frodo = arda_users.frodo
    wiki_a = wiki_groups.wiki_a
    wiki_a_data = {'type': 'groups', 'group': str(wiki_a.external_id), 'role': 'member'}
    with pytest.raises(AccessDenied) as excinfo:
        Role.objects.request_role(frodo, wiki_a, staff_system, '', wiki_a_data, None)
    assert str(excinfo.value) == 'Нельзя запросить роль участника группы для участников самой этой группы'


def test_cyclic_request_is_forbidden(staff_system, arda_users, wiki_groups):
    """Проверим, что нельзя зациклить групповые роли"""

    frodo = arda_users.frodo
    wiki_a = wiki_groups.wiki_a
    wiki_b = wiki_groups.wiki_b
    wiki_c = wiki_groups.wiki_c
    wiki_b_data = {'type': 'groups', 'group': str(wiki_b.external_id), 'role': 'member'}
    wiki_c_data = {'type': 'groups', 'group': str(wiki_c.external_id), 'role': 'member'}

    role = Role.objects.request_role(frodo, wiki_a, staff_system, '', wiki_c_data, None)
    role = refresh(role)
    assert role.state == 'granted'
    with pytest.raises(AccessDenied) as excinfo:
        Role.objects.request_role(frodo, wiki_c, staff_system, '', wiki_b_data, None)
    expected = 'Нельзя запросить роль на вики-группу, уже собираемую из другой вики-группы: Вики-группа A'
    assert str(excinfo.value) == expected
    role2 = Role.objects.request_role(frodo, wiki_b, staff_system, '', wiki_c_data, None)
    role2 = refresh(role2)
    assert role2.state == 'granted'
    with pytest.raises(AccessDenied) as excinfo:
        Role.objects.request_role(frodo, wiki_c, staff_system, '', wiki_b_data, None)

    expected = '''Нельзя запросить роль на вики-группу, уже собираемую из другой вики-группы:
                  Вики-группа A, Вики-группа Б'''
    assert set(re.split('[:,\s]', str(excinfo.value))) == set(re.split('[:,\s]', expected))


def test_requested_roles_are_taken_into_account(staff_with_approvers, arda_users, wiki_groups):
    """Проверим, что при проверке ролей учитываются запрошенные"""

    frodo = arda_users.frodo
    wiki_a = wiki_groups.wiki_a
    wiki_b = wiki_groups.wiki_b
    wiki_c = wiki_groups.wiki_c
    wiki_b_data = {'type': 'groups', 'group': str(wiki_b.external_id), 'role': 'member'}
    wiki_c_data = {'type': 'groups', 'group': str(wiki_c.external_id), 'role': 'member'}

    # попытаемся сначала запросить роль B на A, а потом роль A на B
    role = Role.objects.request_role(frodo, wiki_a, staff_with_approvers, '', wiki_b_data, None)
    role = refresh(role)
    assert role.state == 'requested'
    with pytest.raises(AccessDenied) as excinfo:
        Role.objects.request_role(frodo, wiki_b, staff_with_approvers, '', wiki_c_data, None)
    expected = 'Нельзя запросить роль на вики-группу, уже собираемую из другой вики-группы: Вики-группа A'
    assert str(excinfo.value) == expected
