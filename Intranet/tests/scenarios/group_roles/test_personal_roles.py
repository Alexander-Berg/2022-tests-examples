import pytest

from idm.core.constants.role import ROLE_STATE
from idm.core.models import Role, RoleNode
from idm.tests.utils import add_members, set_workflow, remove_members

pytestmark = [pytest.mark.django_db]


@pytest.mark.parametrize('without_hold,target_state', ((True, ROLE_STATE.DEPRIVED), (False, ROLE_STATE.ONHOLD)))
def test_create_personal_role_on_group_join(arda_users, fellowship, simple_system, without_hold, target_state):
    """
    TestpalmID: 3456788-218
    """
    set_workflow(simple_system, 'approvers = []', 'approvers = []')
    group_role = Role.objects.request_role(
        arda_users.gandalf, fellowship, simple_system, '', {'role': 'manager'}, without_hold=without_hold,
    )
    assert Role.objects.filter(parent=group_role).count() == fellowship.members.count() != 0
    assert not arda_users.sauron.roles.exists()

    add_members(fellowship, [arda_users.sauron])

    role = arda_users.sauron.roles.get()
    assert role.state == ROLE_STATE.GRANTED
    assert role.parent_id == group_role.id

    remove_members(fellowship, [arda_users.sauron])

    role.refresh_from_db()
    assert role.state == target_state


@pytest.mark.parametrize('is_frozen', (True, False))
@pytest.mark.parametrize('without_hold', (True, False))
def test_should_be_put_on_hold(arda_users, simple_system, is_frozen, without_hold):
    arda_users.frodo.is_frozen = is_frozen
    arda_users.frodo.save()

    role_node = RoleNode.objects.get_node_by_data(simple_system, {'role': 'manager'})
    role = Role.objects.create_role(
        subject=arda_users.frodo, system=simple_system,
        node=role_node, fields_data=None,
        save=True, without_hold=without_hold
    )
    role.set_raw_state('granted')
    expected = True
    if is_frozen or without_hold:
        expected = False
    assert role.should_be_put_on_hold() is expected
