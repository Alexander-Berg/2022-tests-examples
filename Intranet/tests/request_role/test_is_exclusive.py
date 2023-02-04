import pytest
import waffle.testutils

from idm.core.constants.role import ROLE_STATE
from idm.core.models import UserPassportLogin, Role
from idm.tests.utils import raw_make_role, set_workflow, DEFAULT_WORKFLOW

pytestmark = [pytest.mark.django_db]


@pytest.mark.parametrize('same_login', (True, False))
@pytest.mark.parametrize('switch_is_active', (True, False))
def test_add_role_to_exclusive_node(simple_system, arda_users, same_login, switch_is_active):
    UserPassportLogin.objects.create(user=arda_users.legolas, login='legolas', is_fully_registered=True)
    other_role = raw_make_role(
        arda_users.frodo,
        simple_system,
        data={'role': 'manager'},
        fields_data={'login': 'abc', 'passport-login': 'frodo'}
    )

    role1 = raw_make_role(
        arda_users.frodo,
        simple_system,
        data={'role': 'admin'},
        fields_data={'login': 'abc', 'passport-login': 'frodo'}
    )
    role1.node.is_exclusive = True
    role1.node.save()

    with waffle.testutils.override_switch('deprive_other_roles_if_exclusive', active=switch_is_active):
        role2 = Role.objects.request_role(
            arda_users.legolas,
            arda_users.legolas,
            simple_system,
            '',
            data={'role': 'admin'},
            fields_data={'passport-login': 'legolas', 'login': 'abc' + ('' if same_login else 'xxx')},
        )

    role2.refresh_from_db()
    assert role2.state == ROLE_STATE.GRANTED

    role1.refresh_from_db()
    if same_login and switch_is_active:
        assert role1.state == ROLE_STATE.DEPRIVED
    else:
        assert role1.state == ROLE_STATE.GRANTED

    other_role.refresh_from_db()
    assert other_role.state == ROLE_STATE.GRANTED


def test_add_role_to_not_exclusive(simple_system, arda_users):
    role1 = raw_make_role(
        arda_users.frodo,
        simple_system,
        data={'role': 'admin'},
    )
    assert not role1.node.is_exclusive

    role2 = Role.objects.request_role(
        arda_users.legolas,
        arda_users.legolas,
        simple_system,
        '',
        data={'role': 'admin'},
    )

    role2.refresh_from_db()
    assert role2.state == ROLE_STATE.GRANTED
    role1.refresh_from_db()
    assert role1.state == ROLE_STATE.GRANTED


@pytest.mark.parametrize('add_group_to_group', ((True, False), (True, True), (False, True)))
def test_add_role_to_group_role_in_exclusive(simple_system, arda_users, department_structure, add_group_to_group):
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    shire = department_structure.shire

    role1 = Role.objects.request_role(
        frodo,
        fellowship if add_group_to_group[1] else frodo,
        simple_system,
        '',
        data={'role': 'admin'}
    )
    assert not role1.node.is_exclusive

    role1.node.is_exclusive = True
    role1.node.save()

    role2 = Role.objects.request_role(
        frodo,
        shire if add_group_to_group[0] else frodo,
        simple_system,
        '',
        data={'role': 'admin'}
    )

    role1.refresh_from_db()
    role2.refresh_from_db()
    roles_count = 2
    if add_group_to_group[0]:
        roles_count += shire.members.count()
    if add_group_to_group[1]:
        roles_count += fellowship.members.count()
    assert Role.objects.filter(state=ROLE_STATE.GRANTED).count() == roles_count
    assert Role.objects.count() == roles_count

