# coding: utf-8


from textwrap import dedent
from unittest import mock

import pytest

from idm.core.workflow.exceptions import ApproverNotFoundError, InvalidPriorityError
from idm.core.models import Role
from idm.core.workflow.plain.user import UserWrapper, userify
from idm.core.workflow.plain.approver import approverify, AnyApprover, Approver
from idm.core.workflow.shortcuts import workflow
from idm.tests.utils import create_user, set_workflow, refresh

from idm.core.constants.workflow import DEFAULT_PRIORITY

pytestmark = pytest.mark.django_db


def test_ensure_approver():
    art = create_user('art')

    assert Approver('art') == approverify(Approver('art'))
    assert Approver('art') == approverify(art)
    assert Approver('art') == approverify(UserWrapper(art))
    assert Approver('art') == approverify('art')


def test_any_approver_uses_ensure_approver_internally(arda_users):
    assert AnyApprover([Approver('frodo'), Approver('legolas'), Approver('gandalf')]) == \
        AnyApprover([arda_users.frodo, UserWrapper(arda_users.legolas), 'gandalf'])

    assert AnyApprover([Approver('frodo'), Approver('legolas')]) == Approver('frodo') | 'legolas'

    assert AnyApprover([Approver('frodo'), Approver('legolas')]) == Approver('frodo') | arda_users.legolas

    assert AnyApprover([Approver('frodo'), Approver('legolas')]) == Approver('frodo') | UserWrapper(arda_users.legolas)

    # более сложный случай
    expected = Approver('frodo') | 'legolas' | 'gandalf' | UserWrapper(arda_users.varda)
    assert AnyApprover([Approver('frodo'), Approver('legolas'), Approver('gandalf'), Approver('varda')]) == expected


def test_approvers_can_be_compared(arda_users):
    assert Approver('frodo') == Approver('frodo')


def test_approvers_can_be_ored(arda_users):
    obj = Approver('frodo') | Approver('legolas') | Approver('gandalf')
    assert isinstance(obj, AnyApprover)
    assert obj.approvers == [Approver('frodo'), Approver('legolas'), Approver('gandalf')]


def test_unknown_user_triggers_exception():
    with pytest.raises(ApproverNotFoundError):
        _ = Approver('daenerys').user


def test_approver_combinations(arda_users):
    expected = {Approver(arda_users.frodo), Approver(arda_users.legolas), Approver(arda_users.gandalf)}
    assert set(Approver('frodo') | Approver(userify('legolas')) | Approver('gandalf')) == expected
    expected = [Approver(arda_users.frodo), Approver(arda_users.legolas), Approver(arda_users.gimli), Approver(arda_users.varda)]
    assert list(Approver('frodo') | Approver('legolas') | Approver('gimli') | Approver('varda')) == expected

    assert [Approver('frodo'), Approver('legolas'), Approver('gandalf'), Approver('varda')] == (
        (Approver('frodo') | Approver('legolas') | Approver('gandalf') | Approver('varda')).approvers
    )
    expected = Approver('frodo') | Approver(userify('legolas')) | Approver('varda')
    assert Approver('frodo') | Approver('legolas') | Approver('varda') == expected
    assert Approver('frodo') | Approver('legolas') | Approver('varda') == (
        Approver('frodo') | Approver('legolas') | Approver('varda')
    )


def test_if_workflow_allow_to_use_UserWrapper_as_approver(superuser_node):
    create_user('art')
    art = userify('art')
    assert isinstance(art, UserWrapper)
    result = workflow(
        "approvers = [user]", {},
        requester=art,
        subject=art,
        system=superuser_node.system,
        node=superuser_node,
    )
    assert result.get('approvers') == [Approver('art')]


def test_anyapprover_on_the_top_level_is_pushed_down(arda_users, superuser_node):
    """Если на верхнем уровне находится AnyApprover, то считаем, что это единственная OR-группа,
    случайно попавшая не туда, куда нужно"""
    context = workflow("approvers = any_from(['frodo', 'legolas'])", {}, requester=arda_users.gandalf,
                       subject=arda_users.gandalf, system=superuser_node.system, node=superuser_node)
    approvers = context['approvers']
    assert approvers == [AnyApprover(['frodo', 'legolas'])]


def test_empty_any_from(simple_system, users_for_test):
    """Проверка случая, когда в any_from() передаётся пустой список"""
    (art, fantom, terran, admin) = users_for_test

    workflow1 = dedent('''
    users_with_role = system.all_users_with_role({'role': 'superuser'})
    approvers = [any_from(users_with_role)]
    ''')
    workflow2 = dedent('''
    users_with_role = system.all_users_with_role({'role': 'superuser'})
    approvers = any_from(users_with_role)
    ''')
    for wf_code in (workflow1, workflow2):
        Role.objects.all().delete()
        set_workflow(simple_system, wf_code)
        role = Role.objects.request_role(art, art, simple_system, None, {'role': 'superuser'}, None)
        role = refresh(role)
        assert role.state == 'granted'
        request = role.requests.get()
        assert request.is_done
    assert request.approves.count() == 0


def test_any_from_list(simple_system, users_for_test):
    """Проверка случая, когда в any_from() передаётся просто список"""

    (art, fantom, terran, admin) = users_for_test

    workflow1 = dedent('''
    team = ['fantom', 'terran']
    approvers = [any_from(team)]
    ''')
    workflow2 = dedent('''
    team = ['fantom', 'terran']
    approvers = any_from(team)
    ''')
    for wf_code in (workflow1, workflow2):
        Role.objects.all().delete()
        set_workflow(simple_system, wf_code)
        role = Role.objects.request_role(art, art, simple_system, None, {'role': 'superuser'}, None)
        role = refresh(role)
        assert role.state == 'requested'
        request = role.get_open_request()
        assert not request.is_done
        assert request.approves.count() == 1
        assert request.approves.get().requests.count() == 2


def test_any_from_notify(arda_users):
    """Проверка случая, когда в any_from передаётся notify"""

    # True главней, чем False
    result = AnyApprover(['frodo', 'legolas'], True) | AnyApprover(['legolas', 'gimli'], False)
    assert len(result.approvers) == 3
    frodo, legolas, gimli = result.approvers
    assert frodo.notify is True
    assert legolas.notify is True
    assert gimli.notify is False

    # True главней, чем False, даже в другом порядке
    result = AnyApprover(['frodo', 'legolas'], False) | AnyApprover(['legolas', 'gimli'], True)
    assert len(result.approvers) == 3
    frodo, legolas, gimli = result.approvers
    assert frodo.notify is False
    assert legolas.notify is True
    assert gimli.notify is True

    # False главней, чем None
    result = AnyApprover(['frodo', 'legolas']) | AnyApprover(['legolas', 'gimli'], False)
    assert len(result.approvers) == 3
    frodo, legolas, gimli = result.approvers
    assert frodo.notify is None
    assert legolas.notify is False
    assert gimli.notify is False

    # сохраняем notify при присоединении Approver
    result = AnyApprover(['frodo', 'legolas']) | Approver('gimli', True)
    assert len(result.approvers) == 3
    frodo, legolas, gimli = result.approvers
    assert frodo.notify is None
    assert legolas.notify is None
    assert gimli.notify is True


def test_2163(simple_system, users_for_test):
    """Проверка случая из RULES-2163, когда пустой список объединяется через OR с непустым"""
    (art, fantom, terran, admin) = users_for_test

    wf_code1 = dedent('''
    users_with_role = system.all_users_with_role({'role': 'superuser'})
    team = ['fantom', 'terran']
    approvers = [any_from(users_with_role) | any_from(team)]
    ''')
    wf_code2 = dedent('''
    users_with_role = system.all_users_with_role({'role': 'superuser'})
    team = ['fantom', 'terran']
    approvers = [any_from(users_with_role) | any_from(team)]
    ''')
    for wf_code in (wf_code1, wf_code2):
        set_workflow(simple_system, wf_code)
        Role.objects.all().delete()
        role = Role.objects.request_role(art, art, simple_system, None, {'role': 'superuser'}, None)
        request = role.get_open_request()
        assert request.approves.count() == 1
        assert request.approves.get().requests.count() == 2
        approve_request = request.approves.get().requests.select_related_for_set_decided().select_related('approver').first()
        approve_request.set_approved(approve_request.approver)
        role = refresh(role)
        assert role.state == 'granted'
        role = Role.objects.request_role(admin, admin, simple_system, None, {'role': 'superuser'}, None)
        request = role.get_open_request()
        assert request.approves.count() == 1
        assert request.approves.get().requests.count() == 3


def test_approver_with_default_priority(arda_users):
    assert Approver('frodo').priority == DEFAULT_PRIORITY
    assert Approver(arda_users.frodo).priority == DEFAULT_PRIORITY
    assert Approver(UserWrapper(arda_users.legolas)).priority == DEFAULT_PRIORITY


def test_anyapprover_with_default_priority(arda_users):
    """
        По умолчанию у группы не выставлен приоритет
    """
    assert AnyApprover(['frodo', 'legolas']).priority == DEFAULT_PRIORITY
    assert (Approver('frodo') | UserWrapper(arda_users.legolas)).priority == DEFAULT_PRIORITY
    assert (Approver('frodo') | arda_users.legolas).priority == DEFAULT_PRIORITY
    assert (AnyApprover(['frodo', 'legolas']) |
            AnyApprover([Approver('gimli'), Approver('sauron')])).priority == DEFAULT_PRIORITY


def test_approvers_with_priority(arda_users):
    assert Approver('frodo', priority=4).priority == 4

    assert Approver(UserWrapper(arda_users.frodo, priority=5)).priority == 5

    # в случае конфликта приоритетов, приоритет берется извне.
    assert Approver(UserWrapper(arda_users.frodo, priority=4), priority=5).priority == 5

    # у кого не проставлен приоритет, у тех он равер DEFAULT_PRIORITY
    # личные приоритеты сотрудников сохраняются
    obj = Approver('frodo', priority=4) | 'gandalf' | Approver('legolas', priority=5) | UserWrapper(arda_users.sauron,
                                                                                                    priority=3)
    expected_priorities = {
        'frodo': 4,
        'gandalf': DEFAULT_PRIORITY,
        'legolas': 5,
        'sauron': 3,
    }

    for approver in obj.approvers:
        assert approver.priority == expected_priorities[approver.user.username]


def test_not_setting_priority_of_group_to_approver_without_priority(arda_users):
    """
        Проверка случая, когда сотруднику без приоритета присваивается DEFAULT_PRIORITY
        Чтобы у сотрудника появился приоритет, нужно указать его явно
    """
    # группа слева
    group = AnyApprover(['legolas', 'saruman', 'gandalf'], priority=3) | Approver('gimli') | Approver('galadriel')

    expected_priorities = {
        'legolas': 3,
        'saruman': 3,
        'gandalf': 3,
        'gimli': DEFAULT_PRIORITY,
        'galadriel': DEFAULT_PRIORITY,
    }

    assert len(group) == 5

    for approver in group.approvers:
        assert approver.priority == expected_priorities[approver.user.username]

    # сотрудник слева
    group = (Approver('gimli') | AnyApprover(['legolas', 'saruman', 'gandalf'], priority=3) |
             UserWrapper(arda_users.galadriel))

    assert len(group) == 5

    for approver in group.approvers:
        assert approver.priority == expected_priorities[approver.user.username]

    # всем сотрудникам без приоритета присваивается DEFAULT_PRIORITY
    group = (AnyApprover(['bilbo', 'sam', 'legolas']) | Approver('gandalf', priority=1) |
             AnyApprover(['saruman', 'gimli', 'galadriel'], priority=2) |
             AnyApprover(['aragorn', 'varda']))

    expected_priorities = {
        'bilbo': DEFAULT_PRIORITY,
        'sam': DEFAULT_PRIORITY,
        'legolas': DEFAULT_PRIORITY,
        'gandalf': 1,
        'saruman': 2,
        'gimli': 2,
        'galadriel': 2,
        'aragorn': DEFAULT_PRIORITY,
        'varda': DEFAULT_PRIORITY,
    }

    assert len(group) == 9

    for approver in group.approvers:
        assert approver.priority == expected_priorities[approver.user.username]

    group = (AnyApprover(['gimli', 'frodo', 'varda'], priority=1) |
             AnyApprover(['saruman', 'legolas', 'gandalf'], priority=2) |
             AnyApprover(['galadriel', 'sam']) |
             AnyApprover(['boromir', 'aragorn', 'peregrin'], priority=5) |
             'bilbo'
             )
    assert len(group) == 12

    # у galadriel, sam и bilbo нет явных приоритетов
    expected_priorities = {
        'gimli': 1,
        'frodo': 1,
        'varda': 1,
        'saruman': 2,
        'legolas': 2,
        'gandalf': 2,
        'galadriel': DEFAULT_PRIORITY,
        'sam': DEFAULT_PRIORITY,
        'boromir': 5,
        'aragorn': 5,
        'peregrin': 5,
        'bilbo': DEFAULT_PRIORITY,
    }

    for approver in group.approvers:
        assert approver.priority == expected_priorities[approver.user.username]


def test_not_setting_group_priority_to_approver_with_priority(arda_users):
    """
        Проверка случая когда сотруднику с приоритетом не выставляется приоритет группы
    """
    # у legolas и gimli свои приоритеты. Saruman и Varda присваиваются приоритет группы
    group = AnyApprover([Approver('legolas', priority=1), Approver('gimli', priority=2), 'saruman',
                         UserWrapper(arda_users.varda)], priority=4)

    assert len(group) == 4

    expected_priorities = {
        'legolas': 1,
        'gimli': 2,
        'saruman': 4,
        'varda': 4,
    }

    for approver in group.approvers:
        assert approver.priority == expected_priorities[approver.user.username]

    # группа слева
    group = AnyApprover(['legolas', 'saruman', 'gandalf'], priority=3) | Approver('gimli', priority=7)

    assert len(group) == 4

    for approver in group.approvers:
        if approver.user.username == 'gimli':
            assert approver.priority == 7
        else:
            assert approver.priority == 3

    # группа без приоритета слева
    group = AnyApprover(['legolas', 'saruman', 'gandalf']) | Approver('gimli', priority=7) | 'bilbo'

    assert len(group) == 5

    for approver in group.approvers:
        if approver.user.username == 'gimli':
            assert approver.priority == 7
        else:
            assert approver.priority == DEFAULT_PRIORITY

    # группа с приоритетом справа
    group = Approver('gimli', priority=7) | AnyApprover(['legolas', 'saruman', 'gandalf'], priority=3)

    assert len(group) == 4
    for approver in group.approvers:
        if approver.user.username == 'gimli':
            assert approver.priority == 7
        else:
            assert approver.priority == 3

    # группа без приоритета справа
    group = Approver('gimli', priority=7) | AnyApprover(['legolas', 'saruman', 'gandalf'])

    assert len(group) == 4
    for approver in group.approvers:
        if approver.user.username == 'gimli':
            assert approver.priority == 7
        else:
            assert approver.priority == DEFAULT_PRIORITY


def test_selecting_approver_with_lower_priority(arda_users):
    """
        Проверка случая когда повторяющиеся сотрудники объединяются в группу и у них сохраняется минимальный приоритет

    """
    group = (Approver('gimli', priority=10) |
             AnyApprover([Approver('gimli', priority=2), Approver('saruman', priority=4), 'gandalf'], priority=1) |
             Approver('saruman') | 'gandalf' | Approver(arda_users.gandalf, priority=6) | 'manve')

    expected_priorities = {
        'gimli': 2,
        'saruman': 4,
        'gandalf': 1,
        'manve': DEFAULT_PRIORITY,  # не указан явный приоритет
    }

    assert len(group) == 4

    for approver in group.approvers:
        assert approver.priority == expected_priorities[approver.user.username]


def test_approver_with_incorrect_priority(arda_users):
    """
        Проверяет случаи когда приоритету присваивается отрицательное число, строка, etc
    """
    with pytest.raises(InvalidPriorityError) as exc:
        Approver('gimli', priority=-1)

    assert str(exc.value) == "Приоритет должен быть целым числом от 0 до {}".format(DEFAULT_PRIORITY)

    with pytest.raises(InvalidPriorityError):
        Approver('gimli', priority=1.5)

    with pytest.raises(InvalidPriorityError):
        Approver('gimli', priority="Python - it fits your brain")

    with pytest.raises(InvalidPriorityError):
        Approver('gimli', priority=10**10)


def test_empty_anyapprover():
    assert [] == AnyApprover([])
    assert [] == AnyApprover([AnyApprover([])])


def test_approver_repr(arda_users):
    """
        Проверяет текстовое представление с приоритетом и флагом notify для Approver
    """
    str_template = "approver({}, priority={}, notify={})"
    str_template_without_notify = "approver({}, priority={})"

    approvers_with_notify = {
        Approver(arda_users.frodo, priority=2, notify=False): str_template,
        Approver(arda_users.sam, priority=DEFAULT_PRIORITY, notify=True): str_template
    }
    approver_without_notify = {
        Approver(arda_users.gimli, priority=3): str_template_without_notify,
        Approver(arda_users.varda): str_template_without_notify
    }

    for approver, template in approvers_with_notify.items():
        assert repr(approver) == template.format(approver.user.username, approver.priority, approver.notify)

    for approver, template in approver_without_notify.items():
        assert repr(approver) == template.format(approver.user.username, approver.priority)


def test_anyapprover_repr(arda_users):
    """
        Проверяет текстовое представление с приоритетом и флагом notify для каждого Approver в AnyApprover
    """
    anyapprover_repr_template = "any_from([{}])"

    any_approvers = [
        AnyApprover([
            Approver(arda_users.frodo, priority=2, notify=False),
            Approver(arda_users.sam, priority=DEFAULT_PRIORITY, notify=True),
            Approver(arda_users.gimli, priority=3),
            Approver(arda_users.varda)
        ]),
        AnyApprover([
            Approver(arda_users.frodo, priority=2, notify=False),
            Approver(arda_users.sam, priority=DEFAULT_PRIORITY, notify=True),
            Approver(arda_users.gimli, priority=3),
            Approver(arda_users.varda)
        ], notify=False, priority=1),
        (
            Approver(arda_users.sauron, notify=False) |
            Approver(arda_users.gandalf, priority=DEFAULT_PRIORITY) |
            Approver(arda_users.galadriel)
        ),
        AnyApprover([Approver(arda_users.varda)]),
    ]

    for any_approver in any_approvers:
        assert repr(any_approver) == anyapprover_repr_template.format(','.join(repr(approver)
                                                                      for approver in any_approver))


def test_load_users(simple_system, users_for_test):
    """Проверка того, что user в модели approver загружаются один раз одним запросом (bulk)"""

    (art, fantom, terran, admin) = users_for_test

    wf_code = dedent('''
    team = ['fantom', 'terran']
    approvers = [any_from(team)]
    ''')

    set_workflow(simple_system, wf_code)

    with mock.patch("idm.core.workflow.plain.approver.User.objects") as user:
        user_cursor = mock.MagicMock()
        user_cursor.__iter__.return_value = [
            fantom, terran
        ]
        user.users.return_value = user_cursor

        filter_user_cursor = mock.MagicMock()
        filter_user_cursor.__iter__.return_value = [
            fantom, terran
        ]
        user_cursor.filter.return_value = filter_user_cursor
        user_cursor.get.return_value = mock.MagicMock()

        filter_user_cursor.all.return_value = [
            fantom, terran
        ]

        Role.objects.request_role(art, art, simple_system, None, {'role': 'superuser'}, None)
        user_cursor.filter.assert_called_once()
        user_cursor.get.assert_not_called()


