# -*- coding: utf-8 -*-


import mock
import pytest
from django.conf import settings
from django.core import mail
from django.core.management import call_command
from django.test import override_settings
from django.utils.translation import override

from idm.core.constants.system import SYSTEM_INCONSISTENCY_POLICY
from idm.core.models import Role
from idm.core.plugins.errors import PluginError
from idm.inconsistencies.models import Inconsistency
from idm.tests.utils import (mock_all_roles, assert_inconsistency, raw_make_role, clear_mailbox, make_role,
                             assert_contains, refresh, mock_ids_repo, get_recievers, add_perms_by_role)

pytestmark = [
    pytest.mark.django_db,
    pytest.mark.robot,
]


def test_check_roles_mails_about_inconsistency_their_side(simple_system, generic_system, arda_users, settings):
    """ Тест отправки почты для случая заведения неконсистентности на стороне системы """
    frodo = arda_users.frodo

    roles = [{
        'login': 'frodo',
        'roles': [{'role': 'admin'}],
    }]
    with mock_all_roles(simple_system, roles), mock_all_roles(generic_system, []):
        call_command('idm_check_and_resolve', check_only=True)

    assert Inconsistency.objects.count() == 1
    inconsistency = Inconsistency.objects.get()
    assert_inconsistency(
        inconsistency,
        user=frodo,
        system=simple_system,
        path='/role/admin/',
        remote_fields=None,
    )

    assert not refresh(simple_system).is_broken
    assert len(mail.outbox) == 4
    message = mail.outbox[0]
    assert message.subject == 'Нарушение консистентности в системе Simple система'
    assert (get_recievers(mail.outbox) ==
            {'simple@yandex-team.ru', 'simplesystem@yandex-team.ru'} | set(settings.EMAILS_FOR_REPORTS))
    assert_contains([
        'В системе Simple система есть расхождения:',
        'Фродо Бэггинс (frodo) имеет в системе Simple система роли, о которых не знает IDM:',
        'Роль: Админ',
        # 'Список неконсистентностей:'
    ], message.body)
    # если в системе нет неконсистентностей, про неё нет и строчки в письме
    assert 'Generic' not in message.body


def test_check_roles_mails_about_inconsistency_our_side(simple_system, arda_users):
    """ Тест отправки почты для случая заведения неконсистентности на стороне IDM. """

    role = make_role(arda_users.frodo, simple_system, {'role': 'manager'})
    clear_mailbox()

    with mock_all_roles(simple_system, []):
        call_command('idm_check_and_resolve', check_only=True)

    assert Inconsistency.objects.count() == 1
    inconsistency = Inconsistency.objects.get()
    assert_inconsistency(
        inconsistency,
        user=arda_users.frodo,
        system=simple_system,
        path='/role/manager/',
        remote_fields=None,
        type=Inconsistency.TYPE_OUR,
        our_role=role,
    )

    assert not refresh(simple_system).is_broken
    assert len(mail.outbox) == 4
    message = mail.outbox[0]
    assert message.subject == 'Нарушение консистентности в системе Simple система'
    assert_contains([
        'В системе Simple система есть расхождения:',
        'Фродо Бэггинс (frodo) имеет в IDM роли, о которых не знает Simple система:',
        'Роль: Менеджер',
        # 'Список неконсистентностей:'
    ], message.body)


def test_check_roles_mails_about_login_inconsistency(simple_system):
    """ Тест отправки почты для случая заведения на стороне системы ролей на пользователя, о котором не знает IDM """

    all_roles = [{
        'login': 'weirdo',
        'roles': [{'role': 'admin'}]
    }]

    with mock_all_roles(simple_system, all_roles):
        call_command('idm_check_and_resolve', check_only=True)

    assert Inconsistency.objects.count() == 1
    inconsistency = Inconsistency.objects.get()
    assert_inconsistency(inconsistency,
                         user=None,
                         system=simple_system,
                         path='/role/admin/',
                         remote_username='weirdo',
                         state='active'
                         )

    assert not refresh(simple_system).is_broken
    assert len(mail.outbox) == 4
    message = mail.outbox[0]
    assert message.subject == 'Нарушение консистентности в системе Simple система'
    assert_contains([
        'В системе Simple система есть расхождения:',
        'В системе Simple система существуют сотрудники со следующими логинами, о которых не знает IDM:',
        'weirdo',
    ], message.body)


def test_check_roles_mails_about_group_inconsistency_our_side(aware_simple_system, arda_users, department_structure):
    """ Тест отправки почты для случая заведения неконсистентности на стороне IDM. """

    system = aware_simple_system
    fellowship = department_structure.fellowship
    role = make_role(fellowship, system, {'role': 'manager'})
    assert Role.objects.count() == 1
    clear_mailbox()

    with mock_all_roles(system, user_roles=[], group_roles=[]):
        call_command('idm_check_and_resolve', check_only=True)

    assert Inconsistency.objects.count() == 1
    inconsistency = Inconsistency.objects.get()
    assert_inconsistency(inconsistency, group=fellowship, system=system, our_role=role, path='/role/manager/')

    assert not refresh(system).is_broken
    assert len(mail.outbox) == 4
    message = mail.outbox[0]
    assert message.subject == 'Нарушение консистентности в системе Simple система'
    assert_contains([
        'В системе Simple система есть расхождения:',
        'Братство кольца (105) имеет в IDM роли, о которых не знает Simple система:',
        'Роль: Менеджер',
        # 'Список неконсистентностей:'
    ], message.body)


def test_check_roles_mails_about_unknown_node(simple_system, arda_users):
    """ Тест отправки почты для случая неконсистентности неизвестного узла.
    Если неизвестны одновременно пользователь/группа и узел, то такая неконсистентность
    считается неконсистентностью логина, а не неизвестного узла."""

    all_roles = [{
        'login': 'frodo',
        'roles': [
            {'role': 'salesman'},
            [{'role': 'animal'}, {'animal': 'lobster'}]
        ]
    }, {
        'login': 'johnshow',
        'roles': [
            {'role': 'aliveperson'},
        ]
    }]

    with mock_all_roles(simple_system, all_roles):
        call_command('idm_check_and_resolve', check_only=True)

    assert Inconsistency.objects.count() == 3
    john_inconsistency = Inconsistency.objects.get(remote_username='johnshow')
    assert john_inconsistency.type == Inconsistency.TYPE_UNKNOWN_USER

    inconsistencies = Inconsistency.objects.exclude(pk=john_inconsistency.pk)
    for inconsistency in inconsistencies:
        assert_inconsistency(inconsistency,
                             system=simple_system,
                             user=arda_users.frodo,
                             type=Inconsistency.TYPE_UNKNOWN_ROLE)

    assert not refresh(simple_system).is_broken
    assert len(mail.outbox) == 4
    message = mail.outbox[0]
    assert message.subject == 'Нарушение консистентности в системе Simple система'
    assert_contains([
        'В системе Simple система есть расхождения:',
        'В системе Simple система существуют сотрудники со следующими логинами, о которых не знает IDM:',
        'johnshow',
        'Фродо Бэггинс (frodo) имеет в системе Simple система роли, '
        'которые не соответствуют ни одному из узлов дерева ролей системы, известных IDM:',
        'node={"role": "salesman"}',
        'node={"role": "animal"}, fields={"animal": "lobster"}',
        # 'Список неконсистентностей:'
    ], message.body)


def test_responsible_users_fetch_mails_about_inconsistency(simple_system, arda_users):
    """Тест отправки почты ответственным за систему в случае неконсистентности в их системе"""

    system = simple_system
    system.emails = ''
    add_perms_by_role('responsible', arda_users.frodo, system)
    system.save()

    all_roles = [{
        'login': 'frodo',
        'roles': [
            {'role': 'salesman'}
        ]
    }]

    with mock_all_roles(system, all_roles):
        call_command('idm_check_and_resolve', check_only=True)

    assert not refresh(system).is_broken
    assert len(mail.outbox) == 3
    message = mail.outbox[0]
    assert message.subject == 'Нарушение консистентности в системе Simple система'
    assert message.to[0] == arda_users.frodo.email


def test_check_roles_mails_about_group_inconsistency(aware_simple_system):
    """Тест отправки почты для случая заведения на стороне системы ролей на группу, о которой не знает IDM"""

    system = aware_simple_system
    all_roles = [{
        'group': 777,
        'roles': [{'role': 'admin'}]
    }]

    with mock_all_roles(system, group_roles=all_roles):
        call_command('idm_check_and_resolve', check_only=True)

    assert Inconsistency.objects.count() == 1
    inconsistency = Inconsistency.objects.get()
    assert_inconsistency(inconsistency, user=None, system=system, path='/role/admin/', remote_group=777)

    assert not refresh(system).is_broken
    assert len(mail.outbox) == 4
    message = mail.outbox[0]
    assert message.subject == 'Нарушение консистентности в системе Simple система'
    assert_contains([
        'В системе Simple система есть расхождения:',
        'В системе Simple система существуют группы со следующими идентификаторами, о которых не знает IDM:',
        '777'
    ], message.body)


def test_system_breaks_when_there_are_too_many_inconsistencies(simple_system, arda_users, settings):
    """
    TestpalmID: 3456788-111
    """
    """Тестирует предохранитель системы, когда слишком много неконсистентностей"""

    raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'}, state='granted')
    raw_make_role(arda_users.legolas, simple_system, {'role': 'admin'}, state='granted')
    raw_make_role(arda_users.gandalf, simple_system, {'role': 'admin'}, state='granted')
    raw_make_role(arda_users.gandalf, simple_system, {'role': 'manager'}, state='granted')

    assert Role.objects.count() == 4
    assert simple_system.is_broken is False

    all_roles = [
        {
            'login': 'frodo',
            'roles': [
                [{'role': 'manager'}, {'passport-login': 'minor1'}]
            ]
        },
        {
            'login': 'gimli',
            'roles': [
                [{'role': 'manager'}, {'passport-login': 'minor2'}]
            ]
        }
    ]
    with mock_all_roles(simple_system, all_roles), override('ru'):
        Inconsistency.objects.check_roles()

    # система должна поломаться, при этом должны создаться новые неконсистентности.
    # При этом система не учитывается при вызове idm_check_and_resolve, так как она сломана
    simple_system = refresh(simple_system)
    assert simple_system.is_broken is True
    assert simple_system.actions.filter(action='system_marked_broken').count() == 1
    action = simple_system.actions.filter(action='system_marked_broken').get()
    assert action.comment == 'Слишком много расхождений: 6'
    assert simple_system.actions.filter(action='system_marked_racovered').exists() is False
    assert Inconsistency.objects.count() == 6
    assert Role.objects.count() == 4

    assert len(mail.outbox) == 4
    message = mail.outbox[0]
    assert (get_recievers(mail.outbox) ==
            {'simple@yandex-team.ru', 'simplesystem@yandex-team.ru'} | set(settings.EMAILS_FOR_REPORTS))
    assert message.subject == 'Поломка в системе Simple система.'
    assert_contains([
        'Система "Simple система" заблокирована, так как выявлено большое количество расхождений: 6',
        'Роль: Админ',
        'Роль: Менеджер',
        'legolas',
        'frodo',
        'gandalf',
        'gimli',
    ], message.body)

    # все расхождения остались и после попытки их автоматически разрешить, т.к. система сломана
    call_command('idm_check_and_resolve', resolve_only=True)
    assert Inconsistency.objects.active().count() == 6


def test_system_does_not_break_on_too_many_unknown_usernames_second_time(simple_system):
    """Протестируем, что система не ломается, если ей известно о большом количестве расхождений логина/группы"""

    all_roles = [{'login': 'ork_%d' % i, 'roles': [{'role': 'admin'}]} for i in range(6)]
    with mock_all_roles(simple_system, all_roles):
        Inconsistency.objects.check_roles()

    simple_system = refresh(simple_system)
    assert simple_system.is_broken
    assert Inconsistency.objects.count() == 6
    simple_system.recover()

    # повторная проверка не ломает систему, так как мы уже знаем об этих расхождениях
    with mock_all_roles(simple_system, all_roles):
        Inconsistency.objects.check_roles()

    simple_system = refresh(simple_system)
    assert not simple_system.is_broken


def test_system_still_breaks_after_many_wrong_logins(simple_system, arda_users):
    """Протестируем, что система ломается, если ей сначала было известно о большом количестве
    неконсистентностей логина/группы, а потом стало известно о большом количестве неконсистентностей других типов"""

    all_roles = [{'login': 'ork_%d' % i, 'roles': [{'role': 'admin'}]} for i in range(5)]
    with mock_all_roles(simple_system, all_roles):
        Inconsistency.objects.check_roles()

    simple_system = refresh(simple_system)
    assert not simple_system.is_broken
    assert Inconsistency.objects.count() == 5

    all_roles = [
        {
            'login': 'frodo',
            'roles': [
                [{'role': 'admin'}, {'login': 'frodo%s' % i}]
                for i in range(6)
            ]
        }
    ]

    # повторная проверка должна сломать систему
    with mock_all_roles(simple_system, all_roles):
        Inconsistency.objects.check_roles()

    simple_system = refresh(simple_system)
    assert simple_system.is_broken


def test_break_system_with_group_inconsistencies(aware_simple_system, arda_users, department_structure):
    """Сломаем систему групповыми и персональными ролями"""

    fellowship = department_structure.fellowship
    system = aware_simple_system
    raw_make_role(fellowship, system, {'role': 'admin'}, system_specific={'login': 'one'}, state='granted')
    raw_make_role(fellowship, system, {'role': 'manager'}, system_specific={'login': 'two'}, state='granted')

    user_roles = [
        {
            'login': 'frodo',
            'roles': [
                [{'role': 'manager'}, {'passport-login': 'minor1'}]
            ]
        },
        {
            'login': 'legolas',
            'roles': [
                [{'role': 'poweruser'}, {'passport-login': 'minor2'}],
            ]
        },
        {
            'login': 'kontrabas',
            'roles': [
                [{'role': 'manager'}]
            ]
        }
    ]
    group_roles = [
        {
            'group': fellowship.external_id,
            'roles': [
                [{'role': 'manager'}, {}],
                [{'role': 'admin'}, {}]
            ]
        },
        {
            'group': 666,
            'roles': [
                [{'role': 'manager'}, {}]
            ]
        }
    ]
    with mock_all_roles(system, user_roles, group_roles), override('ru'):
        Inconsistency.objects.check_roles()

    # система должна поломаться, при этом должны создаться новые расхождения.
    # При этом система не учитывается при вызове idm_check_and_resolve, так как она сломана
    assert Inconsistency.objects.count() == 8
    system = refresh(system)
    assert system.is_broken is True
    assert system.actions.filter(action='system_marked_broken').count() == 1
    action = system.actions.filter(action='system_marked_broken').get()
    assert action.comment == 'Слишком много расхождений: 8'
    assert system.actions.filter(action='system_marked_racovered').exists() is False
    assert Role.objects.count() == 2

    assert len(mail.outbox) == 4
    message = mail.outbox[0]
    assert message.subject == 'Поломка в системе Simple система.'
    assert_contains([
        'Система "Simple система" заблокирована, так как выявлено большое количество расхождений: 8',
        'В IDM есть, в системе отсутствуют роли:',
        'Братство кольца (105)',
        'Роль: Менеджер',
        'Роль: Админ',
        'В системе есть, в IDM отсутствуют роли:',
        'Роль: Могучий Пользователь',
        'Фродо Бэггинс (frodo)',
        'legolas (legolas)',
        'Неизвестные IDM пользователи:',
        'kontrabas',
        'Неизвестные IDM идентификаторы групп:',
        '666',
    ], message.body)

    # все неконсистентности остались и после попытки их автоматически разрешить, т.к. система сломана
    call_command('idm_check_and_resolve', resolve_only=True)
    assert Inconsistency.objects.active().count() == 8
    assert Role.objects.count() == 2


def test_system_breaks_on_unknown_node_inconsistencies(aware_simple_system, arda_users, department_structure):
    """Сломаем систему групповыми и персональными ролями на неизвестные узлы дерева"""

    fellowship = department_structure.fellowship
    system = aware_simple_system
    user_roles = [
        {
            'login': 'frodo',
            'roles': [
                [{'role': 'salesman'}],
                [{'role': 'evilperson'}],
                [{'role': 'animal'}, {'animal': 'lobster'}]
            ]
        },
    ]
    group_roles = [
        {
            'group': fellowship.external_id,
            'roles': [
                [{'role': 'salesmen'}, {}],
                [{'role': 'evilpeople'}],
                [{'role': 'animal'}, {'animal': 'lobster'}],
            ]
        },
    ]
    with mock_all_roles(system, user_roles, group_roles), override('ru'):
        Inconsistency.objects.check_roles()

    # система должна поломаться, при этом должны создаться новые неконсистентности.
    # При этом система не учитывается при вызове idm_check_and_resolve, так как она сломана
    assert Inconsistency.objects.count() == 6
    system = refresh(system)
    assert system.is_broken is True
    assert len(mail.outbox) == 4
    message = mail.outbox[0]
    assert message.subject == 'Поломка в системе Simple система.'
    assert_contains([
        'Система "Simple система" заблокирована, так как выявлено большое количество расхождений: 6',
        'В системе есть, в IDM отсутствуют роли, выданные на неизвестные IDM узлы дерева ролей:',
        'Фродо Бэггинс (frodo)',
        'node={"role": "salesman"}',
        'node={"role": "evilperson"}',
        'node={"role": "animal"}, fields={"animal": "lobster"}',
        'Братство кольца (105)',
        'node={"role": "salesmen"}',
        'node={"role": "evilpeople"}',
        'node={"role": "animal"}, fields={"animal": "lobster"}',
    ], message.body)


def test_check_roles_only_active_systems(simple_system, arda_users):
    """ Выключенные системы не проверяем """

    raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'}, state='granted')

    simple_system.is_active = False
    simple_system.save()

    with mock_all_roles(simple_system, []):
        call_command('idm_check_and_resolve', check_only=True)

    assert Inconsistency.objects.count() == 0


def test_unaware_group_roles_are_not_inconsistencies(simple_system, arda_users, department_structure):
    """Если система - unaware, то выданные в ней групповые роли не попадают в сравнение"""

    fellowship = department_structure.fellowship
    assert simple_system.group_policy == 'unaware'
    raw_make_role(fellowship, simple_system, {'role': 'admin'}, state='granted')
    with mock_all_roles(simple_system, []):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 0


def test_get_roles_fallbacks_to_get_all_roles(generic_system, arda_users):
    result = {
        'users': [{
            'login': 'frodo',
            'roles': [
                [{'role': 'manager'}, {}],
            ]
        }]
    }

    def all_roles_or_exception(url, method, timeout):
        if url == 'http://example.com/get-roles/':
            raise PluginError(500, '')
        else:
            return result

    with mock.patch.object(generic_system.plugin.__class__, '_send_data') as send_data:
        send_data.side_effect = all_roles_or_exception
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 1
    assert_inconsistency(
        Inconsistency.objects.get(),
        system=generic_system,
        user=arda_users.frodo,
        type=Inconsistency.TYPE_THEIR,
        path='/role/manager/',
        remote_fields=None,
    )


def test_get_short_report(simple_system, arda_users):
    raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'})
    roles = [{
        'login': 'frodo',
        'roles': [{'role': 'manager'}]
    }, {
        'login': 'legolas',
        'roles': [{'role': 'unknown'}]
    }, {
        'login': 'johnsnow',
        'roles': [{'role': 'manager'}]
    }]
    with mock_all_roles(simple_system, roles), override_settings(IDM_INCONSISTENCY_REPORT_THRESHOLD=1):
        call_command('idm_check_and_resolve', check_only=True)

    assert len(mail.outbox) == 4
    message = mail.outbox[0]
    assert_contains([
        'В системе Simple система есть расхождения:',
        'У нас есть роль, в системе – нет: 1',
        'В системе есть роль, у нас – нет: 1',
        'В системе имеется пользователь, о котором мы не знаем: 1',
        'В системе есть роль, но она отсутствует в дереве ролей системы: 1'
    ], message.body)

    with override_settings(IDM_INCONSISTENCY_REPORT_THRESHOLD=1):
        report = Inconsistency.objects.get_report(simple_system)
        assert report == {
            simple_system: {
                'format': 'short',
                'statistics': {
                    'system_has_unknown_role': {
                        'count': 1,
                        'name': 'В системе есть роль, но она отсутствует в дереве ролей системы',
                    },
                    'system_has_unknown_user': {
                        'count': 1,
                        'name': 'В системе имеется пользователь, о котором мы не знаем',
                    },
                    'system_has_we_dont': {
                        'count': 1,
                        'name': 'В системе есть роль, у нас – нет',
                    },
                    'we_have_system_dont': {
                        'count': 1,
                        'name': 'У нас есть роль, в системе – нет',
                    }
                }
            }
        }


def test_report_filtering(simple_system, generic_system, arda_users):
    raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'})
    roles = [{
        'login': 'frodo',
        'roles': [{'role': 'manager'}]
    }]
    with mock_all_roles(simple_system, roles), mock_all_roles(generic_system, roles):
        Inconsistency.objects.check_roles()

    assert Inconsistency.objects.count() == 3
    report = Inconsistency.objects.get_report(generic_system)
    assert report == {
        generic_system: {
            'system_has_we_dont': {
                arda_users.frodo: [Inconsistency.objects.get(system=generic_system)]
            },
            'format': 'full'
        }
    }


def test_check_sox_system_issue(simple_system, arda_users):
    """Тест создания тикета для sox-системы"""
    frodo = arda_users.frodo
    simple_system.inconsistency_policy = SYSTEM_INCONSISTENCY_POLICY.STRICT_SOX
    simple_system.save()

    roles = [{
        'login': 'frodo',
        'roles': [{'role': 'admin'}, {'role': 'somebody'}],
    }]
    with mock_all_roles(simple_system, roles), mock_ids_repo('startrek2') as repo:
        call_command('idm_check_and_resolve', check_only=True)

    assert Inconsistency.objects.count() == 2
    their = Inconsistency.objects.get(type=Inconsistency.TYPE_THEIR)
    no_node = Inconsistency.objects.get(type=Inconsistency.TYPE_UNKNOWN_ROLE)
    assert_inconsistency(
        their,
        user=frodo,
        system=simple_system,
        path='/role/admin/',
        remote_fields=None,
    )
    assert_inconsistency(
        no_node,
        user=frodo,
        system=simple_system,
        remote_data={'role': 'somebody'},
    )

    assert not refresh(simple_system).is_broken
    assert len(mail.outbox) == 4
    message = mail.outbox[0]
    assert message.subject == 'Нарушение консистентности в системе Simple система'
    assert_contains([
        'Фродо Бэггинс (frodo) имеет в системе Simple система роли, '
        'которые не соответствуют ни одному из узлов дерева ролей системы, известных IDM:',
        'node={"role": "somebody"}',
        'Фродо Бэггинс (frodo) имеет в системе Simple система роли, о которых не знает IDM:',
        'Роль: Админ'
    ], message.body)

    assert len(repo.create.call_args_list) == 1
    issue_call = repo.create.call_args_list[0]
    # issue_call это mock.call, который является 2-tuple, 0-й элемент в котором это args, а 1-й это kwargs
    kwargs = issue_call[1]
    assert kwargs['queue'] == 'FIRE'
    assert kwargs['followers'] == []
    assert kwargs['components'] == settings.IDM_TRACKER_INCONSISTENCY_COMPONENTS
    assert kwargs['summary'] == 'Нарушение консистентности в SOX-системе Simple система'
    assert_contains([
        'В системе Simple система есть расхождения:',
        'Фродо Бэггинс (frodo) имеет в системе Simple система роли, ',
        'которые не соответствуют ни одному из узлов дерева ролей систем',
        'node={"role": "somebody"}',
    ], kwargs['description'])
    assert 'Админ' not in kwargs['description']


def test_create_issue_without_inconsistencies(simple_system, arda_users):
    """Тест создания тикета для sox-системы"""
    simple_system.inconsistency_policy = SYSTEM_INCONSISTENCY_POLICY.STRICT_SOX
    simple_system.save()

    for user in arda_users:
        raw_make_role(arda_users[user], simple_system, {'role': 'admin'}, state='granted')

    with mock_all_roles(simple_system, []), mock_ids_repo('startrek2') as repo:
        repo.create.call_args_list = []  # Не сбрасывается после предыдущего теста :/
        call_command('idm_check_and_resolve', check_only=True)

    assert Inconsistency.objects.active().count() == len(arda_users)
    assert Inconsistency.objects.active().filter(type=Inconsistency.TYPE_OUR).count() == len(arda_users)

    assert len(repo.create.call_args_list) == 0
