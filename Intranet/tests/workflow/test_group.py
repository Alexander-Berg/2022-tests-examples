# coding: utf-8


import pytest

from idm.core.workflow.exceptions import GroupHasNoParentError, GroupDoesNotExist, NoSuchPriorityPolicyError, NoGroupError
from idm.core.workflow.plain.approver import AnyApprover, Approver
from idm.core.workflow.plain.group import groupify, GroupWrapper, PRIORITY_POLICIES
from idm.core.workflow.plain.user import userify
from idm.users import ranks as ranks_const
from idm.tests.utils import assert_num_queries


pytestmark = pytest.mark.django_db


def test_group_get_responsibles(department_structure):
    fellowship = department_structure.fellowship
    fellowship = groupify(fellowship)
    responsible = fellowship.get_all_responsibles()
    assert list(responsible) == [userify(user) for user in ('frodo', 'galadriel', 'gandalf', 'sam')]
    heads = list(fellowship.get_heads())
    assert heads == [userify('frodo')]


def test_get_parent(department_structure):
    fellowship = groupify(department_structure.fellowship)
    assert fellowship.get_parent().group.slug == 'associations'


def test_no_parent_exception(department_structure):
    fellowship = groupify(department_structure.fellowship)
    middle_earth = fellowship.get_parent().get_parent()
    assert middle_earth.group.slug == 'middle-earth'
    with pytest.raises(GroupHasNoParentError):
        middle_earth.get_parent()
    root = groupify(middle_earth.group.parent)
    with pytest.raises(GroupHasNoParentError):
        root.get_parent()


def test_group_type(department_structure):
    fellowship = groupify(department_structure.fellowship)
    assert fellowship.type == 'department'
    assert fellowship.external_id == 105
    assert fellowship.slug == 'fellowship-of-the-ring'


def test_group_parent(department_structure):
    fellowship = groupify(department_structure.fellowship)
    parent = fellowship.parent
    assert isinstance(parent, GroupWrapper)
    assert parent.external_id == 103
    assert parent.slug == 'associations'


def test_is_root(department_structure):
    fellowship = groupify(department_structure.fellowship)
    assert fellowship.is_root() is False
    assert fellowship.parent.is_root() is False
    assert fellowship.parent.parent.is_root() is True


def test_get_root(department_structure):
    fellowship = groupify(department_structure.fellowship)
    root = fellowship.get_root()
    assert isinstance(root, GroupWrapper)
    assert root.slug == 'middle-earth'


def test_get_ancestor(department_structure):
    fellowship = groupify(department_structure.fellowship)
    ancestor1 = fellowship.get_ancestor(1)
    ancestor2 = fellowship.get_ancestor(2)
    ancestor3 = fellowship.get_ancestor(3)
    assert ancestor3 == fellowship
    assert isinstance(ancestor3, GroupWrapper)
    with pytest.raises(ValueError):
        fellowship.get_ancestor(4)
    with pytest.raises(ValueError):
        fellowship.get_ancestor(0)
    ancestor_minus_1 = fellowship.get_ancestor(-1)
    ancestor_minus_2 = fellowship.get_ancestor(-2)
    with pytest.raises(ValueError):
        fellowship.get_ancestor(-3)
    assert ancestor1 == ancestor_minus_2
    assert ancestor2 == ancestor_minus_1


def test_context_passing(department_structure):
    """Проверим, что get_parent() и get_ancestor() сохраняют контекст объекта"""

    context = {'hello': 'world'}
    fellowship = GroupWrapper(department_structure.fellowship, context)
    assert fellowship.context == context
    parent = fellowship.get_parent()
    ancestor = fellowship.get_ancestor(1)
    assert parent.context == context
    assert ancestor.context == context


def test_get_ancestors(department_structure):
    """Проверим метод get_ancestors()"""

    fellowship = groupify(department_structure.fellowship)
    ancestors = fellowship.get_ancestors()
    assert len(ancestors) == 3
    assert [item.slug for item in ancestors] == ['middle-earth', 'associations', 'fellowship-of-the-ring']


def test_is_descendant_of(department_structure):
    """Проверим метод is_descendant_of"""

    earth = groupify(department_structure.earth)
    fellowship = groupify(department_structure.fellowship)
    valinor = groupify(department_structure.valinor)
    assert fellowship.is_descendant_of(earth) is True
    assert fellowship.is_descendant_of(valinor) is False
    assert earth.is_descendant_of(fellowship) is False
    assert fellowship.is_descendant_of(fellowship) is True


def test_is_subgroup_of(department_structure):
    """Проверим метод is_subgroup_of"""

    fellowship = groupify(department_structure.fellowship)
    associations = groupify(department_structure.associations)
    assert fellowship.is_subgroup_of('middle-earth')
    assert associations.is_subgroup_of('middle-earth')
    assert fellowship.is_subgroup_of('valinor') is False
    with pytest.raises(GroupDoesNotExist):
        fellowship.is_subgroup_of('ext')


def test_staff_id_property(department_structure):
    fellowship = groupify(department_structure.fellowship)
    assert fellowship.staff_id == 105


def test_get_children(department_structure):
    fellowship = groupify(department_structure.fellowship)
    earth = groupify(department_structure.earth)
    assert fellowship.get_children() == []
    assert set(earth.get_children()) == {groupify(department_structure.associations), groupify(department_structure.lands)}


def test_get_responsibles(arda_users, department_structure):
    fellowship = groupify(department_structure.fellowship)
    responsibles = fellowship.get_responsibles()
    expected = [[arda_users.frodo, arda_users.galadriel, arda_users.gandalf, arda_users.sam], [arda_users.varda]]
    assert responsibles == expected
    assert responsibles[0].level == 3
    assert responsibles[1].level == 2
    assert responsibles[0][0].rank == ranks_const.HEAD
    assert responsibles[0][1].rank == ranks_const.DEPUTY



    # chain=False приводит к единому списку
    responsibles = fellowship.get_responsibles(chain=False, ranks=[ranks_const.DEPUTY])
    # поскольку мы берём только заместителей, то frodo в конце, как заместитель в associations
    assert responsibles == [arda_users.galadriel, arda_users.gandalf, arda_users.sam, arda_users.frodo]

    # up_to_level позволяет отсечь слишком высоуровневые группы
    # 0 - только текущий уровень, положительное N – все группы ниже уровня N включительно
    # отрицательное -N – все группы ниже, чем текущий-N включительно
    responsibles = fellowship.get_responsibles(chain=False, ranks=[ranks_const.DEPUTY], up_to_level=0)
    assert responsibles == [arda_users.galadriel, arda_users.gandalf, arda_users.sam, ]

    # если нет подходящих под критерий пользователей, то отдаём пустой список
    responsibles = fellowship.get_responsibles(chain=False, ranks=[ranks_const.BUDGET_HOLDER])
    assert responsibles == []

    # для chain=True тоже
    responsibles = fellowship.get_responsibles(chain=True, ranks=[ranks_const.BUDGET_HOLDER])
    assert responsibles == []


def test_get_responsibles_priorities(arda_users, department_structure):
    # для руководителей и замов одинаковые приоритеты на одном уровне
    fellowship = groupify(department_structure.fellowship)
    responsibles = fellowship.get_responsibles(priority_policy=PRIORITY_POLICIES.EQUAL)

    # индекс это уровень подразделения снизу
    expected_priorities = [
        {
            'frodo': 1,
            'sam': 1,
            'gandalf': 1,
            'galadriel': 1,
        },
        {
            'varda': 2,
            'frodo': 2,
        }
    ]

    for level, user_list in enumerate(responsibles):
        for user in user_list:
            assert user.priority == expected_priorities[level][user.username]

    # для руководителей приоритет выше чем у замов на одном уровне
    responsibles = fellowship.get_responsibles(priority_policy=PRIORITY_POLICIES.DEPUTY_LESS)

    # индекс это уровень подразделения снизу
    expected_priorities = [
        {
            'frodo': 2,
            'sam': 1,
            'gandalf': 1,
            'galadriel': 1,
        },
        {
            'varda': 4,
        }
    ]

    for level, user_list in enumerate(responsibles):
        for user in user_list:
            assert user.priority == expected_priorities[level][user.username]

    # для руководителей приоритет ниже чем у замов на одном уровне
    responsibles = fellowship.get_responsibles(priority_policy=PRIORITY_POLICIES.HEAD_LESS)

    # индекс это уровень подразделения снизу
    expected_priorities = [
        {
            'frodo': 1,
            'sam': 2,
            'gandalf': 2,
            'galadriel': 2,
        },
        {
            'varda': 3,
        }
    ]

    for level, user_list in enumerate(responsibles):
        for user in user_list:
            assert user.priority == expected_priorities[level][user.username]

    # не возвращать замов
    responsibles = fellowship.get_responsibles(priority_policy=PRIORITY_POLICIES.NO_DEPUTIES)

    # индекс это уровень подразделения снизу
    expected_priorities = [
        {
            'frodo': 1,
        },
        {
            'varda': 2,
        }
    ]

    for level, user_list in enumerate(responsibles):
        for user in user_list:
            assert user.priority == expected_priorities[level][user.username]


def test_get_responsible_priorities_exception(arda_users, department_structure):
    fellowship = groupify(department_structure.fellowship)

    with pytest.raises(NoSuchPriorityPolicyError) as exc:
        fellowship.get_responsibles(priority_policy="error_policy")
    assert str(exc.value) == ("Priority error_policy does not exist. Available priorities are:head_less, "
                                 "deputy_less, equal, no_deputies")


def test_get_chain_of_heads(arda_users, department_structure):
    """
        Проверяет метод get_chain_of_heads для группы
    """
    fellowship = groupify(department_structure.fellowship)
    associations = groupify(department_structure.associations)
    valinor = groupify(department_structure.valinor)

    expected_result = {
        'frodo': 1,
        'sam': 2,
        'galadriel': 2,
        'gandalf': 2,
        'varda': 3,
    }

    heads_and_deputies = fellowship.get_chain_of_heads()

    assert len(heads_and_deputies) == len(expected_result)

    for approver in heads_and_deputies:
        assert approver.priority == expected_result[approver.user.username]

    # priority_policy = EQUAL
    expected_result = {
        'frodo': 1,
        'varda': 1,
    }

    heads_and_deputies = associations.get_chain_of_heads(priority_policy=PRIORITY_POLICIES.EQUAL)

    assert len(heads_and_deputies) == len(expected_result)

    for approver in heads_and_deputies:
        assert approver.priority == expected_result[approver.user.username]

    # priority_policy = DEPUTY_LESS
    expected_result = {
        'manve': 1,
        'varda': 2,
    }

    heads_and_deputies = valinor.get_chain_of_heads(priority_policy=PRIORITY_POLICIES.DEPUTY_LESS)

    assert len(heads_and_deputies) == len(expected_result)

    for approver in heads_and_deputies:
        assert approver.priority == expected_result[approver.user.username]

    # ranks = head_only
    expected_result = {
        'frodo': 1,
        'varda': 2,
    }

    heads_and_deputies = fellowship.get_chain_of_heads(priority_policy=PRIORITY_POLICIES.NO_DEPUTIES)

    assert len(heads_and_deputies) == len(expected_result)

    for approver in heads_and_deputies:
        assert approver.priority == expected_result[approver.user.username]


def test_get_chain_of_heads_for_svc(arda_users, department_structure):
    from idm.users.models import Group, User, GroupService, GroupMembership, GroupResponsibility
    from idm.users import ranks
    schema = [
        ('root', None, ['gandalf']),
        ('parent', 'root', ['aragorn', 'legolas', 'gimli']),
        ('self', 'parent', ['frodo']),
        ('child', 'self', ['sam']),
    ]
    supergroup = Group.objects.get(parent=None, type='service')
    for index, (service_slug, parent_slug, usernames) in enumerate(schema):
        parent_id = None
        if parent_slug:
            parent_id = GroupService.objects.get(slug=parent_slug).id
        GroupService.objects.create(slug=service_slug, external_id=index, parent_id=parent_id)
        group = Group.objects.create(parent=supergroup, slug=f'svc_{service_slug}', type='service')
        for username in usernames:
            GroupMembership.objects.create(group=group, user=arda_users[username], is_direct=True, state='active')
            GroupResponsibility.objects.create(group=group, user=arda_users[username], is_active=True, rank=ranks.HEAD)
    User.objects.filter(id=arda_users['gimli'].id).update(is_active=False)
    target_group = GroupWrapper(Group.objects.get(slug='svc_self'))
    approvers = AnyApprover([
        Approver(arda_users['frodo'], priority=1),
        Approver(arda_users['aragorn'], priority=2),
        Approver(arda_users['legolas'], priority=2),
        Approver(arda_users['gandalf'], priority=3)
    ])
    with assert_num_queries(4):
        # - получаем сервис
        # - получаем родительские сервисы
        # - получаем группы этих сервисов
        # - получаем ответственных за сервисы
        assert target_group.get_chain_of_heads() == approvers


def test_groupify(department_structure):
    fellowship = groupify('fellowship-of-the-ring')
    assert fellowship.slug == 'fellowship-of-the-ring'
    with pytest.raises(NoGroupError) as err:
        fellowship = groupify('some_department_slug')
    assert str(err.value) == 'Подразделение some_department_slug не существует.'
