import pytest

from idm.core.constants.groupmembership_system_relation import MEMBERSHIP_SYSTEM_RELATION_STATE
from idm.core.constants.role import ROLE_STATE
from idm.core.constants.system import SYSTEM_GROUP_POLICY, SYSTEM_REQUEST_POLICY
from idm.core.models import GroupMembershipSystemRelation, Role
from idm.core.tasks.group_memberships import SyncAndPushMemberships

pytestmark = [pytest.mark.django_db]


@pytest.mark.parametrize('group_policy', SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS)
@pytest.mark.usefixtures('default_workflow')
def test_sync_and_push(simple_system, arda_users, department_structure, group_policy):
    simple_system.group_policy = group_policy
    simple_system.request_policy = SYSTEM_REQUEST_POLICY.ANYONE
    simple_system.save()

    fellowship = department_structure.fellowship
    valinor = department_structure.valinor

    assert GroupMembershipSystemRelation.objects.count() == 0

    for group in (valinor, fellowship):
        role = Role.objects.request_role(arda_users.frodo, group, simple_system, 'comment', {'role': 'manager'})
        if group_policy == SYSTEM_GROUP_POLICY.AWARE_OF_MEMBERSHIPS_WITHOUT_LOGINS:
            Role.objects.poke_awaiting_roles()

        role.refresh_from_db()
        assert role.state == ROLE_STATE.GRANTED

    assert GroupMembershipSystemRelation.objects.filter(membership__group=valinor).count() > 0
    assert GroupMembershipSystemRelation.objects.filter(membership__group=fellowship).count() > 0

    GroupMembershipSystemRelation.objects.all().update(state=MEMBERSHIP_SYSTEM_RELATION_STATE.ACTIVATING)
    SyncAndPushMemberships.apply_async(kwargs={'system_id': simple_system.id, 'group_id': fellowship.id})

    not_activating_valinor_memberships = (
        GroupMembershipSystemRelation.objects
        .filter(membership__group=valinor)
        .exclude(state=MEMBERSHIP_SYSTEM_RELATION_STATE.ACTIVATING)
    )
    assert not_activating_valinor_memberships.count() == 0

    inactive_fellowship_memberships = (
        GroupMembershipSystemRelation.objects
        .filter(membership__group=fellowship)
        .exclude(state=MEMBERSHIP_SYSTEM_RELATION_STATE.ACTIVATED)
    )
    assert inactive_fellowship_memberships.count() == 0
