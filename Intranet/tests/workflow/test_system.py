# coding: utf-8


from textwrap import dedent

import pytest

from idm.core.workflow.exceptions import RoleNodeDoesNotExist
from idm.core.models import Role, ApproveRequest
from idm.core.workflow.plain.group import GroupWrapper
from idm.core.workflow.plain.system import SystemWrapper
from idm.core.workflow.plain.user import UserWrapper
from idm.tests.utils import set_workflow, raw_make_role, DEFAULT_WORKFLOW, refresh, remove_members
from idm.users.models import User

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('state', ['requested', 'approved', 'review_request', 'deprived'])
@pytest.mark.parametrize('among_states', [None, 'almost_active', 'active', 'returnable'])
def test_all_users_with_role(simple_system, arda_users, state, among_states):
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    raw_make_role(frodo, simple_system, {'role': 'superuser'}, state=state)

    workflow = 'approvers = any_from(system.all_users_with_role({"role": "superuser"}%s))'
    if among_states is None:
        workflow = workflow % ''
    else:
        workflow = workflow % ', among_states="{}"'.format(among_states)
    set_workflow(simple_system, workflow)

    role = Role.objects.request_role(legolas, legolas, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)
    if (
            (among_states in (None, 'almost_active') and state in ['approved', 'review_request']) or
            (among_states == 'active' and state in ['review_request']) or
            (among_states == 'returnable' and state in ['requested', 'review_request', 'approved'])
    ):
        assert role.state == 'requested'
        approvers = ApproveRequest.objects.filter(approve__role_request__role=role).values_list('approver', flat=True)
        assert list(approvers) == [frodo.pk]
    else:
        assert role.state == 'granted'


def test_all_users_with_unknown_role(simple_system, arda_users):
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    raw_make_role(frodo, simple_system, {'role': 'superuser'})

    set_workflow(simple_system, "approvers = any_from(system.all_users_with_role({'role': 'unknown'}))")

    with pytest.raises(RoleNodeDoesNotExist) as err:
        Role.objects.request_role(legolas, legolas, simple_system, '', {'role': 'admin'}, None)
    assert str(err.value) == 'Указан несуществующий узел: {role: unknown}'


def test_all_users_with_role_group_aware_system(aware_simple_system, arda_users, department_structure):
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    fellowship = department_structure.fellowship
    set_workflow(aware_simple_system, group_code=DEFAULT_WORKFLOW)

    Role.objects.request_role(frodo, fellowship, aware_simple_system, None, {'role': 'superuser'}, None)

    set_workflow(aware_simple_system, "approvers = any_from(system.all_users_with_role({'role': 'superuser'}))")

    role = Role.objects.request_role(legolas, legolas, aware_simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)
    assert role.state == 'granted'
    approves = (
        ApproveRequest.objects
        .filter(approve__role_request__role=role)
        .select_related('approver')
        .order_by('pk')
    )
    assert list(approves) == sorted(approves, key=lambda request_: request_.approver.username)
    approvers = approves.values_list('approver', flat=True)
    assert set(approvers) == set(fellowship.members.values_list('pk', flat=True))


def test_inactive_members_are_not_listed_in_all_users_with_role(aware_simple_system, arda_users, department_structure):
    frodo = arda_users.frodo
    legolas = arda_users.legolas
    fellowship = department_structure.fellowship
    set_workflow(aware_simple_system, group_code=DEFAULT_WORKFLOW)
    members_count = fellowship.members.count()
    remove_members(fellowship, [legolas])

    assert fellowship.members.count() == members_count - 1

    Role.objects.request_role(frodo, fellowship, aware_simple_system, None, {'role': 'superuser'}, None)

    set_workflow(aware_simple_system, "approvers = any_from(system.all_users_with_role({'role': 'superuser'}))")

    role = Role.objects.request_role(legolas, legolas, aware_simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)
    assert role.state == 'requested'
    approvers = ApproveRequest.objects.filter(approve__role_request__role=role).values_list('approver', flat=True)
    assert set(approvers) == set(fellowship.members.values_list('pk', flat=True))


def test_inactive_roles(simple_system, arda_users):
    frodo = arda_users.frodo
    raw_make_role(frodo, simple_system, {'role': 'superuser'}, state='deprived')

    wrapper = SystemWrapper(simple_system)
    assert wrapper.all_users_with_role({'role': 'superuser'}) == []

    raw_make_role(frodo, simple_system, {'role': 'admin'}, state='granted')
    assert wrapper.all_users_with_role({'role': 'superuser'}) == []
    assert wrapper.all_users_with_role({'role': 'admin'}) == [UserWrapper(frodo)]


def test_distinct(simple_system, arda_users, department_structure):
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    set_workflow(simple_system, DEFAULT_WORKFLOW, DEFAULT_WORKFLOW)
    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)
    wrapper = SystemWrapper(simple_system)
    owners = wrapper.all_users_with_role({'role': 'admin'})
    expected = sorted(fellowship.members.all(), key=lambda user: user.username)
    assert sorted(owners, key=lambda wrapper: wrapper.username) == expected


def test_all_users_with_role_limit(simple_system, arda_users, department_structure):
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    set_workflow(simple_system, DEFAULT_WORKFLOW, DEFAULT_WORKFLOW)
    Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)
    wrapper = SystemWrapper(simple_system)
    owners = wrapper.all_users_with_role({'role': 'admin'}, limit=5)
    # тут 4 так как фродо два раза в группу входит
    assert [obj.username for obj in owners] == ['frodo', 'meriadoc', 'peregrin', 'sam']


def test_all_groups_with_role(simple_system, arda_users, department_structure):
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    raw_make_role(frodo, simple_system, {'role': 'superuser'}, state='deprived')

    wrapper = SystemWrapper(simple_system)
    assert wrapper.all_groups_with_role({'role': 'superuser'}) == []

    raw_make_role(fellowship, simple_system, {'role': 'admin'}, state='granted')
    assert wrapper.all_groups_with_role({'role': 'superuser'}) == []
    assert wrapper.all_groups_with_role({'role': 'admin'}) == [GroupWrapper(fellowship)]


def test_systemify(simple_system, generic_system, arda_users, department_structure):
    """Проверим работу systemify()"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    fellowship = department_structure.fellowship
    raw_make_role(legolas, simple_system, {'role': 'admin'})

    wf = dedent('''
    with_role = systemify('simple').all_users_with_role({'role': 'admin'})
    approvers = any_from(with_role)
    ''')

    set_workflow(generic_system, wf, wf)

    role = Role.objects.request_role(frodo, frodo, generic_system, None, {'role': 'superuser'}, None)
    role = refresh(role)
    assert role.state == 'requested'
    approvers = (
        ApproveRequest.objects.filter(approve__role_request__role=role).
            values_list('approver__username', flat=True)
    )
    assert set(approvers) == {'legolas'}

    role2 = Role.objects.request_role(frodo, fellowship, generic_system, None, {'role': 'superuser'}, None)
    role2 = refresh(role2)
    assert role2.state == 'requested'
    approvers = (
        ApproveRequest.objects.filter(approve__role_request__role=role).
            values_list('approver__username', flat=True)
    )
    assert set(approvers) == {'legolas'}


def test_get_aliases_nodes(cauth):
    """Проверим работу функции system.get_aliased_nodes(name, lang='ru', type=None)"""

    wrapper = SystemWrapper(cauth)
    nodes = wrapper.get_aliased_nodes('vs-admin@yandex-team.ru')
    assert len(nodes) == 1
    node = nodes[0]
    assert node.slug == 'server1'

    nodes = wrapper.get_aliased_nodes('decline-too@yandex-team.ru')
    assert {node.slug for node in nodes} == {'server3', 'server5'}


def test_system_is_global(simple_system, arda_users):
    frodo = arda_users.frodo
    approver = User.objects.create(username=simple_system.slug)

    wf = dedent('''
    def f():
       return system.slug
       
    def g():
       return f()
       
    approvers = [g()]
    ''')

    set_workflow(simple_system, wf)

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    approve_request = ApproveRequest.objects.get(approve__role_request__role=role)
    assert approve_request.approver_id == approver.id
