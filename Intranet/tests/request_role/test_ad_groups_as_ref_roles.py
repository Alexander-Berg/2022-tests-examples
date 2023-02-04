import textwrap

import pytest
import waffle.testutils

from idm.core.constants.role import ROLE_STATE
from idm.core.models import Role
from idm.tests.utils import set_workflow, mock_tree, sync_role_nodes

pytestmark = [pytest.mark.django_db]


@waffle.testutils.override_switch('convert_ad_groups_to_ref_roles', active=True)
def test_request_role_with_ad_groups(simple_system, arda_users, department_structure, ad_system):
    """Добавим роль, для которой необходимо добавление пользователя в AD группу"""
    fellowship = department_structure.fellowship
    frodo = arda_users.frodo

    workflow = textwrap.dedent('''
    ad_groups = ['OU=group1', 'OU=group2']
    if role['role'] == 'admin':
        ref_roles = [{'system': 'simple', 'role_data': {'role': 'manager'}}]
    approvers = []
    ''')
    tree = ad_system.plugin.get_info()
    with mock_tree(ad_system, tree):
        sync_role_nodes(ad_system)
    set_workflow(simple_system, workflow, workflow)

    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)
    role.refresh_from_db()
    assert role.state == ROLE_STATE.GRANTED

    assert not role.ad_groups
    assert role.ref_roles == [
        {
            'system': 'simple',
            'role_data': {
                'role': 'manager',
            }
        },
        {
            'system': 'ad_system',
            'role_data': {
                'type': 'roles_in_groups',
                'ad_group': 'OU=group1',
                'group_roles': 'member',
            }
        },
        {
            'system': 'ad_system',
            'role_data': {
                'type': 'roles_in_groups',
                'ad_group': 'OU=group2',
                'group_roles': 'member',
            }
        },
    ]

    expected_roles = {
        '/role/admin/',
        '/role/manager/',
        '/type/roles_in_groups/ad_group/OU=group1/group_roles/member/',
        '/type/roles_in_groups/ad_group/OU=group2/group_roles/member/',
    }
    assert set(list(frodo.roles.values_list('node__slug_path', flat=True).order_by('node__slug_path'))) == expected_roles
    for role in frodo.roles.all():
        assert not role.ad_groups
        assert not role.ref_roles
