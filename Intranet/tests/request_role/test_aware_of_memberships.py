# coding: utf-8


import mock
import pytest

from idm.core.constants.role import ROLE_STATE
from idm.core.models import GroupMembershipSystemRelation, Role
from idm.core.constants.groupmembership_system_relation import MEMBERSHIP_SYSTEM_RELATION_STATE
from idm.core.constants.system import SYSTEM_GROUP_POLICY
from idm.tests.utils import set_workflow, refresh, DEFAULT_WORKFLOW, force_awaiting_memberships_role_grant

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
def test_group_aware_system_add_group_role(simple_system, arda_users, department_structure, group_policy):
    """
    Проверим, что aware_of_memberships-система отправляет пуш про группу,
    но не отправляет про каждого пользователя
    """
    all_memberships = GroupMembershipSystemRelation.objects.all()
    simple_system.group_policy = group_policy
    simple_system.save()
    frodo = arda_users.frodo

    assert all_memberships.count() == 0

    fellowship = department_structure.fellowship
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    with mock.patch.object(simple_system.plugin.__class__, 'add_role') as add_role:
        add_role.return_value = {
            'data': {}
        }
        role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'})

    role = refresh(role)

    assert all_memberships.count() != 0
    assert all_memberships.exclude(state=MEMBERSHIP_SYSTEM_RELATION_STATE.ACTIVATED).count() == 0

    if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITH_LOGINS:
        assert role.state == ROLE_STATE.GRANTED
    else:
        force_awaiting_memberships_role_grant(role)

    assert role.refs.count() == 0
    assert Role.objects.count() == 1
