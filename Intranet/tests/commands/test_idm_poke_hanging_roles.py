# coding: utf-8


import datetime
from textwrap import dedent

import pytest
import waffle.testutils
from django.core import mail
from django.core.management import call_command

from django.utils import timezone

from mock import patch
from waffle.testutils import override_switch

from idm.core.constants.action import ACTION
from idm.core.constants.affiliation import AFFILIATION
from idm.core.constants.groupmembership import GROUPMEMBERSHIP_STATE
from idm.core.constants.role import ROLE_STATE
from idm.core.constants.rolefield import FIELD_TYPE
from idm.core.models import Role, RoleNode, RoleField, Action
from idm.core.plugins.errors import PluginError
from idm.framework.task import DelayingError
from idm.tests.utils import (raw_make_role, clear_mailbox, refresh, assert_action_chain, set_workflow,
                             assert_contains, crash_system, disable_tasks, DEFAULT_WORKFLOW)
from idm.users.models import Group, GroupMembership

pytestmark = pytest.mark.django_db


def test_deprive_hanging_roles(simple_system, arda_users):
    """Запускаем команду отзыва зависших ролей (тех, которые уже пытались отозвать, но не получилось)"""

    frodo = arda_users['frodo']
    role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='depriving',
                         updated=timezone.now() - datetime.timedelta(days=1))
    clear_mailbox()

    call_command('idm_poke_hanging_roles')

    role = refresh(role)
    assert role.state == 'deprived'
    assert role.expire_at is None
    assert len(mail.outbox) == 1
    assert_action_chain(role, ['redeprive', 'remove'])


def test_deprive_hanging_roles_with_specified_system(simple_system, complex_system, arda_users):
    """Запускаем команду отзыва зависших ролей (тех, которые уже пытались отозвать, но не получилось)"""

    frodo = arda_users['frodo']
    sam = arda_users['sam']
    frodo_role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='depriving',
                         updated=timezone.now() - datetime.timedelta(days=1))
    sam_role = raw_make_role(sam, complex_system, {'project': 'subs', 'role': 'manager'}, state='depriving',
                             updated=timezone.now() - datetime.timedelta(days=1))

    clear_mailbox()

    call_command('idm_poke_hanging_roles', '--system', simple_system.slug)

    frodo_role = refresh(frodo_role)
    sam_role = refresh(sam_role)
    assert frodo_role.state == 'deprived'
    assert frodo_role.expire_at is None
    assert sam_role.state == 'depriving'
    assert len(mail.outbox) == 1
    assert_action_chain(frodo_role, ['redeprive', 'remove'])
    assert_action_chain(sam_role, [])


def test_deprive_hanging_roles_with_specified_broken_system(simple_system, arda_users):
    """Запускаем команду отзыва зависших ролей (тех, которые уже пытались отозвать, но не получилось)"""

    frodo = arda_users['frodo']
    role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='depriving',
                         updated=timezone.now() - datetime.timedelta(days=1))
    simple_system.is_active = False
    simple_system.save()
    clear_mailbox()

    call_command('idm_poke_hanging_roles', '--system', simple_system.slug)

    role = refresh(role)
    assert role.state == 'depriving'
    assert len(mail.outbox) == 0
    assert_action_chain(role, [])


def test_grant_approved_roles(simple_system, arda_users):
    """Запускаем команду выдачи зависших в approved ролей"""

    frodo = arda_users['frodo']

    with disable_tasks():
        role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    clear_mailbox()
    role = refresh(role)
    assert role.state == 'approved'
    assert_action_chain(role, ['request', 'apply_workflow', 'approve'])

    with patch('idm.core.querysets.role.get_queue_size', return_value=1):
        call_command('idm_poke_hanging_roles')

    role = refresh(role)
    assert role.state == 'granted'
    assert role.expire_at is None
    assert len(mail.outbox) == 1
    assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])


def test_grant_applicable_ref_roles(simple_system, arda_users):
    """Запускаем проверку выдачи невыданных связанных ролей"""

    frodo = arda_users['frodo']
    set_workflow(simple_system, dedent('''
        approvers = []
        if role.get('role') == 'admin':
            ref_roles = [{
                'system': '%(simple)s',
                'role_data': {
                    'role': 'manager'
                }
            }]
        else:
            upyachka('this is spartaaa!!!!')
    ''') % {
        'simple': simple_system.slug
    })
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert Role.objects.count() == 1
    clear_mailbox()
    role = refresh(role)
    assert role.refs.count() == 0
    assert role.state == 'granted'
    assert_action_chain(
        role,
        ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant', 'ref_role_error'],
    )

    # исправим workflow
    set_workflow(simple_system, dedent('''
        approvers = []
        if role.get('role') == 'admin':
            ref_roles = [{
                'system': '%s',
                'role_data': {
                    'role': 'manager'
                }
            }]
    ''') % simple_system.slug)
    call_command('idm_poke_hanging_roles')

    role = refresh(role)
    assert role.state == 'granted'
    assert role.refs.count() == 1
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Simple система. Новая роль'
    assert_contains(['Вы получили новую роль в системе', 'Simple система',
                        'Роль: Менеджер',
                        'поскольку у вас есть роль в системе', 'Роль: Админ'], message.body)


def test_grant_applicable_ref_roles_other_system(simple_system, complex_system, arda_users):
    """Аналогично тесту test_grant_applicable_ref_roles, но для случая, когда связанные роли выдаются
    в другой системе"""

    frodo = arda_users.frodo
    frodo.passport_logins.create(login='yndx-frodo', state='created', is_fully_registered=True)
    set_workflow(complex_system, 'error')
    set_workflow(simple_system, dedent('''
        approvers = []
        if role.get('role') == 'admin':
            ref_roles = [{
                'system': '%(complex)s',
                'role_data': {
                    'project': 'subs',
                    'role': 'manager',
                },
                'role_fields': {
                    'passport-login': 'yndx-frodo',
                    'field_1': '100',
                }
            }]
    ''') % {
        'complex': complex_system.slug
    })
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert Role.objects.count() == 1
    clear_mailbox()
    role = refresh(role)
    assert role.refs.count() == 0
    assert role.state == 'granted'
    assert_action_chain(
        role,
        ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant', 'ref_role_error'],
    )

    # исправим workflow
    set_workflow(complex_system, 'approvers = []')
    call_command('idm_poke_hanging_roles')

    role = refresh(role)
    assert role.state == 'granted'
    assert role.refs.count() == 1
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Complex система. Новая роль'
    assert_contains(['Вы получили новую роль в системе', 'Complex система',
                        'Роль: Менеджер',
                        'поскольку у вас есть роль в системе', 'Роль: Админ'], message.body)


def test_grant_applicable_ref_roles_for_specified_system(simple_system, complex_system, arda_users):
    """Запускаем проверку выдачи невыданных связанных ролей"""

    frodo = arda_users['frodo']
    frodo.passport_logins.create(login='yndx-frodo', state='created', is_fully_registered=True)

    set_workflow(simple_system, dedent('''
            approvers = []
            if role.get('role') == 'admin':
                ref_roles = [{
                    'system': '%s',
                    'role_data': {
                        'role': 'manager'
                    }
                }]
            else:
                upyachka('this is spartaaa!!!!')
        ''') % simple_system.slug)

    set_workflow(complex_system, dedent('''
                approvers = []
                if role.get('role') == 'developer':
                    ref_roles = [{
                        'system': '%s',
                        'role_data': {
                            'project': 'subs',
                            'role': 'manager'
                        },
                        'role_fields': {
                            'passport-login': 'yndx-frodo',
                            'field_1': '100',
                        },
                    }]
                else:
                    upyachka('this is spartaaa!!!!')
            ''') % complex_system.slug)

    simple_role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    complex_role = Role.objects.request_role(frodo, frodo, complex_system, '',
                                             {'project': 'subs', 'role': 'developer'},
                                             {'passport-login': 'yndx-frodo', 'field_1': '100'}
                                             )
    assert Role.objects.count() == 2
    clear_mailbox()
    simple_role = refresh(simple_role)
    complex_role = refresh(complex_role)
    for role in simple_role, complex_role:
        assert role.refs.count() == 0
        assert role.state == 'granted'
        assert_action_chain(
            role,
            ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant', 'ref_role_error'],
        )

    # исправим workflow
    set_workflow(simple_system, dedent('''
        approvers = []
        if role.get('role') == 'admin':
            ref_roles = [{
                'system': '%s',
                'role_data': {
                    'role': 'manager'
                }
            }]
    ''') % simple_system.slug)
    set_workflow(complex_system, dedent('''
            approvers = []
            if role.get('role') == 'admin':
                ref_roles = [{
                    'system': '%s',
                    'role_data': {
                        'project': 'subs',
                        'role': 'manager'
                    }
                }]
        ''') % complex_system.slug)

    call_command('idm_poke_hanging_roles', '--system', simple_system.slug)

    simple_role = refresh(simple_role)
    complex_role = refresh(complex_role)
    assert simple_role.state == 'granted'
    assert simple_role.refs.count() == 1
    assert len(mail.outbox) == 1
    assert complex_role.state == 'granted'
    assert complex_role.refs.count() == 0
    assert len(mail.outbox) == 1

    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Simple система. Новая роль'
    assert_contains(['Вы получили новую роль в системе', 'Simple система',
                        'Роль: Менеджер',
                        'поскольку у вас есть роль в системе', 'Роль: Админ'], message.body)


def test_we_dont_try_to_request_ref_roles_that_are_already_granted(simple_system, complex_system, arda_users):
    """Проверим, что мы не пытаемся запрашивать уже выданные связанные роли"""

    frodo = arda_users.frodo
    frodo.passport_logins.create(login='yndx-frodo', state='created', is_fully_registered=True)
    set_workflow(complex_system)
    set_workflow(simple_system, dedent('''
        approvers = []
        if role.get('role') == 'admin':
            ref_roles = [{
                'system': '%(complex)s',
                'role_data': {
                    'project': 'subs',
                    'role': 'manager',
                },
                'role_fields': {
                    'passport-login': 'yndx-frodo',
                    'field_1': '100',
                }
            }]
    ''') % {
        'complex': complex_system.slug
    })
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert Role.objects.count() == 2
    clear_mailbox()
    role = refresh(role)
    assert role.refs.count() == 1
    assert role.state == 'granted'
    assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])
    ref = role.refs.get()
    assert_action_chain(ref, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])

    # мы не должны даже пытаться запрашивать уже выданную роль
    with patch.object(Role.objects, 'request_role') as request_role:
        request_role.side_effect = NotImplementedError
        call_command('idm_poke_hanging_roles')


def test_dont_try_to_request_ref_roles_that_are_already_granted_visibility(simple_system, complex_system, arda_users):
    """Проверим, что мы не пытаемся запрашивать уже выданные связанные роли"""

    frodo = arda_users.frodo
    frodo.passport_logins.create(login='yndx-frodo', state='created', is_fully_registered=True)
    set_workflow(complex_system)
    set_workflow(simple_system, dedent('''
        approvers = []
        if role.get('role') == 'admin':
            ref_roles = [{
                'system': '%(complex)s',
                'role_data': {
                    'project': 'subs',
                    'role': 'manager',
                },
                'role_fields': {
                    'passport-login': 'yndx-frodo',
                    'field_1': '100',
                },
                'visibility': False
            }]
    ''') % {
        'complex': complex_system.slug
    })
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert Role.objects.count() == 2
    clear_mailbox()
    role = refresh(role)
    assert role.refs.count() == 1
    assert role.state == 'granted'
    assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])
    ref = role.refs.get()
    assert_action_chain(ref, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])

    # мы не должны даже пытаться запрашивать уже выданную роль
    with patch.object(Role.objects, 'request_role') as request_role:
        request_role.side_effect = NotImplementedError
        call_command('idm_poke_hanging_roles')


def test_do_not_grant_applicable_ref_roles_for_inactive_parent_roles(simple_system, arda_users):
    """Невыданные персональные связанные роли должны выдаваться только для активных родительских ролей"""

    frodo = arda_users['frodo']
    set_workflow(simple_system, dedent('''
        approvers = []
        if role.get('role') == 'admin':
            ref_roles = [{
                'system': '%(simple)s',
                'role_data': {
                    'role': 'manager'
                }
            }]
        else:
            upyachka('this is spartaaa!!!!')
    ''') % {
        'simple': simple_system.slug
    })
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert Role.objects.count() == 1
    clear_mailbox()
    role = refresh(role)
    assert role.refs.count() == 0
    assert role.state == 'granted'
    assert_action_chain(
        role,
        ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant', 'ref_role_error'],
    )
    role.deprive_or_decline(frodo)

    # исправим workflow
    set_workflow(simple_system, dedent('''
        approvers = []
        if role.get('role') == 'admin':
            ref_roles = [{
                'system': '%s',
                'role_data': {
                    'role': 'manager'
                }
            }]
    ''') % simple_system.slug)
    call_command('idm_poke_hanging_roles')

    # у неактивной роли не появилось связанных
    role = refresh(role)
    assert role.state == 'deprived'
    assert role.refs.count() == 0


def test_poke_ref_group_roles(simple_system, arda_users, department_structure):
    """Проверим, что мы выдаём по групповой роли связанную групповую, если нужно"""

    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    members_count = fellowship.members.count()
    template = dedent('''
        approvers = []
        if role.get('role') == 'admin':
            ref_roles = [{
                'system': '%s',
                'role_data': {
                    'role': 'manager'
                }
            }]
        else:
            %%s
    ''') % simple_system.slug
    # зададим сломанный workflow
    set_workflow(simple_system, group_code=template % 'raise AccessDenied()')
    # запросим роль
    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)
    assert role.refs.count() == members_count
    # связанная групповая не выдалась
    assert role.refs.filter(user=None).count() == 0
    # чиним воркфлоу
    set_workflow(simple_system, group_code=template % 'pass')
    # вызовем человека из Кемерово
    call_command('idm_poke_hanging_roles')
    # убедимся, что он пришёл и поправил всё
    role = refresh(role)
    assert role.refs.count() == members_count + 1  # по числу пользователей + связанная групповая
    assert role.refs.filter(state='granted').count() == members_count + 1
    # связанная групповая роль
    ref_group_role = role.refs.get(user=None)
    assert ref_group_role.refs.count() == members_count
    assert ref_group_role.refs.filter(state='granted').count() == members_count
    # а всего ролей должно быть members_count+2
    assert Role.objects.filter(user=None).count() == 2
    assert Role.objects.filter(group=None).count() == members_count*2


def test_grant_applicable_ref_roles_if_some_are_granted_some_not(simple_system, arda_users):
    """Если часть связанных ролей выдана, а другая нет, то выдаваться должны только те, которых не хватало"""

    frodo = arda_users['frodo']
    set_workflow(simple_system, dedent('''
        approvers = []
        if role.get('role') == 'admin':
            ref_roles = [{
                'system': '%(simple)s',
                'role_data': {
                    'role': 'manager'
                }
            }, {
                'system': '%(simple)s',
                'role_data': {
                    'role': 'poweruser'
                }
            }]
        elif role.get('role') == 'poweruser':
            upyachka('this is spartaaa!!!!')
    ''') % {
        'simple': simple_system.slug
    })
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert Role.objects.count() == 2
    clear_mailbox()
    role = refresh(role)
    assert role.refs.count() == 1
    assert role.state == 'granted'
    ref = role.refs.get()
    assert ref.state == 'granted'
    assert_action_chain(
        role,
        ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant', 'ref_role_error'],
    )
    assert_action_chain(ref, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])

    # исправим workflow
    set_workflow(simple_system, dedent('''
        approvers = []
        if role.get('role') == 'admin':
            ref_roles = [{
                'system': '%(simple)s',
                'role_data': {
                    'role': 'manager'
                }
            }, {
                'system': '%(simple)s',
                'role_data': {
                    'role': 'poweruser'
                }
            }]
    ''') % {
        'simple': simple_system.slug
    })
    call_command('idm_poke_hanging_roles')

    role = refresh(role)
    assert role.state == 'granted'
    assert role.refs.count() == 2
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Simple система. Новая роль'
    assert_contains(['Вы получили новую роль в системе', 'Simple система',
                        'Роль: Могучий Пользователь',
                        'поскольку у вас есть роль в системе', 'Роль: Админ'], message.body)


@waffle.testutils.override_switch('idm.enable_ignore_failed_roles_on_poke', active=True)
def test_grant_applicable_user_roles_if_they_failed_previously(simple_system, arda_users, department_structure):
    """Если пользовательские роли, которые должны были быть выданы по групповой, по каким-то причинам не были выданы
    сразу, они должны быть выданы командой idm_poke_hanging_roles"""

    frodo = arda_users.get('frodo')
    legolas = arda_users.get('legolas')
    set_workflow(simple_system, group_code='approvers = []')
    group = Group.objects.get(slug='fellowship-of-the-ring')
    members_count = group.members.count()

    with crash_system(simple_system):
        role = Role.objects.request_role(frodo, group, simple_system, comment='', data={'role': 'manager'},
                                         fields_data={'login': 'fr0d0'})
    assert Role.objects.count() == 1 + members_count
    user_roles = Role.objects.filter(group=None)
    assert user_roles.count() == members_count
    assert user_roles.filter(state='failed').count() == members_count
    role = refresh(role)
    assert role.state == 'granted'
    assert_action_chain(role.refs.first(), ['request', 'approve', 'first_add_role_push', 'fail'])

    # повторим
    with crash_system(simple_system):
        call_command('idm_poke_hanging_roles', '--retry-failed')
    assert_action_chain(role.refs.first(), [
        'request', 'approve', 'first_add_role_push', 'fail', 'rerequest', 'approve', 'first_add_role_push', 'fail'])

    # теперь всё ок
    call_command('idm_poke_hanging_roles', '--retry-failed')
    assert role.refs.count() == members_count
    assert role.refs.filter(state='granted').count() == members_count
    userrole = role.refs.get(state='granted', user=legolas)
    assert_action_chain(
        role.refs.first(),
        [
            'request', 'approve', 'first_add_role_push', 'fail',
            'rerequest', 'approve', 'first_add_role_push', 'fail',
            'rerequest', 'approve', 'first_add_role_push', 'grant'
        ]
    )
    assert userrole.parent == role
    assert userrole.system_id == simple_system.id
    assert userrole.fields_data == {'login': 'fr0d0'}
    assert userrole.system_specific == {'login': 'fr0d0'}
    userrole.fetch_node()
    assert userrole.node.value_path == '/manager/'
    assert userrole.node.data == {'role': 'manager'}
    assert userrole.group_id is None


@waffle.testutils.override_switch('idm.enable_ignore_failed_roles_on_poke', active=True)
def test_not_grant_applicable_user_roles_if_they_failed_previously(simple_system, arda_users, department_structure):
    """Если пользовательские роли, которые должны были быть выданы по групповой, по каким-то причинам не были выданы
    сразу, они не должны быть выданы командой idm_poke_hanging_roles"""

    frodo = arda_users.get('frodo')
    legolas = arda_users.get('legolas')
    set_workflow(simple_system, group_code='approvers = []')
    group = Group.objects.get(slug='fellowship-of-the-ring')
    members_count = group.members.count()

    with crash_system(simple_system):
        role = Role.objects.request_role(frodo, group, simple_system, comment='', data={'role': 'manager'},
                                         fields_data={'login': 'fr0d0'})
    assert Role.objects.count() == 1 + members_count
    user_roles = Role.objects.filter(group=None)
    assert user_roles.count() == members_count
    assert user_roles.filter(state='failed').count() == members_count
    role = refresh(role)
    assert role.state == 'granted'
    assert_action_chain(role.refs.first(), ['request', 'approve', 'first_add_role_push', 'fail'])

    # повторим
    with crash_system(simple_system):
        call_command('idm_poke_hanging_roles')
    assert_action_chain(role.refs.first(), ['request', 'approve', 'first_add_role_push', 'fail'])

    # теперь всё еще FAILED
    call_command('idm_poke_hanging_roles')
    assert role.refs.count() == members_count
    assert role.refs.filter(state='granted').count() == 0
    assert_action_chain(
        role.refs.first(),
        ['request', 'approve', 'first_add_role_push', 'fail']
    )


def test_dont_fail_after_dalaying_error(simple_system, arda_users):
    """ Если сервис не отвечает, роль не нужно переводить в статус 'failed' """
    frodo = arda_users.get('frodo')
    simple_system.retry_failed_roles = True
    simple_system.save()

    with patch('idm.core.tasks.roles.RoleAdded.add_role') as add_role:
        add_role.side_effect = PluginError(500, 'plugin_error')
        role = Role.objects.request_role(frodo, frodo, simple_system, comment='', data={'role': 'manager'})
    role = refresh(role)
    assert role.state == ROLE_STATE.APPROVED
    fail_role = Action.objects.latest('id')
    assert fail_role.action == ACTION.FAIL
    assert fail_role.data['comment'] == 'Не удалось добавить роль в систему из-за ошибки в системе.'
    error = fail_role.error
    assert error == 'PluginError: code=500, message="plugin_error", data=None, answer="None"'


def test_ignore_awaiting_user_roles(simple_system, arda_users, department_structure):
    """Если пользовательские роли, которые должны были быть выданы по групповой, висят в awaiting,
    их можно спокойно тыкать сколько угодно раз"""

    frodo = arda_users.get('frodo')
    legolas = arda_users.get('legolas')
    set_workflow(simple_system, group_code='approvers = []')
    group = Group.objects.get(slug='fellowship-of-the-ring')

    RoleField.objects.all().delete()
    node = RoleNode.objects.get(slug='manager')
    node.fields.create(type=FIELD_TYPE.PASSPORT_LOGIN, slug='passport-login', is_required=True)

    group.memberships.exclude(user=legolas).delete()
    mb = GroupMembership.objects.get(group=group)
    login = legolas.passport_logins.create(login='ynds-legolas', state='created', is_fully_registered=False)
    mb.passport_login = login
    mb.save()

    group_role = Role.objects.request_role(
        frodo, group, simple_system,
        comment='',
        data={'role': 'manager'},
    )
    assert Role.objects.count() == 2
    group_role = refresh(group_role)
    assert group_role.state == 'granted'
    legolas_role = Role.objects.get(group=None)
    assert legolas_role.state == 'awaiting'
    assert_action_chain(legolas_role, ['request', 'approve', 'await'])
    assert legolas_role.passport_logins.get() == login
    assert legolas_role.fields_data == {'passport-login': 'ynds-legolas'}

    # повторим
    call_command('idm_poke_hanging_roles')
    assert Role.objects.filter(group=None).count() == 1  # новых ролей взамен старой не создалось
    legolas_role = refresh(legolas_role)
    assert_action_chain(legolas_role, ['request', 'approve', 'await'])  # при синке никаких синков

    login = mb.passport_login
    login.is_fully_registered = True
    login.save()
    # теперь логин дореган
    call_command('idm_poke_hanging_roles')
    assert Role.objects.filter(group=None).count() == 1  # новых ролей взамен старой не создалось
    legolas_role = refresh(legolas_role)
    assert legolas_role.state == 'granted'
    assert_action_chain(legolas_role, ['request', 'approve', 'await', 'approve', 'first_add_role_push', 'grant'])

    assert legolas_role.parent_id == group_role.id
    assert legolas_role.system_id == simple_system.id
    assert group_role.fields_data is None
    assert legolas_role.fields_data == {'passport-login': 'ynds-legolas'}
    assert legolas_role.system_specific == {'passport-login': 'ynds-legolas'}
    assert legolas_role.passport_logins.get() == login
    legolas_role.fetch_node()
    assert legolas_role.node.value_path == '/manager/'
    assert legolas_role.node.data == {'role': 'manager'}
    assert legolas_role.group is None


def test_do_not_grant_applicable_user_role_if_group_role_is_inactive(simple_system, arda_users, department_structure):
    """Не выдаём пользовательские роли по групповой роли, если эта роль отозвана или по иным причинам неактивна"""

    frodo = arda_users.frodo
    set_workflow(simple_system, group_code='approvers = []')
    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    members_count = fellowship.members.count()

    with crash_system(simple_system):
        role = Role.objects.request_role(frodo, fellowship, simple_system, comment='', data={'role': 'manager'},
                                         fields_data={'a': 'b', 'c': 'd'})
    assert Role.objects.count() == 1 + members_count
    user_roles = Role.objects.filter(group=None)
    assert user_roles.count() == members_count
    assert user_roles.filter(state=ROLE_STATE.FAILED).count() == members_count
    role = refresh(role)
    # отзовём роль
    role.deprive_or_decline(frodo)
    role = refresh(role)
    assert role.state == ROLE_STATE.DEPRIVED
    # связанные роли не должны выдаться
    call_command('idm_poke_hanging_roles')
    assert Role.objects.count() == 1 + members_count
    assert Role.objects.filter(is_active=True).count() == 0
    assert Role.objects.filter(state=ROLE_STATE.DEPRIVED).count() == 1
    assert Role.objects.filter(state=ROLE_STATE.FAILED).count() == members_count


@pytest.mark.parametrize('is_active', (True, False))
def test_deprive_ref_roles_of_inactive_group_role(simple_system, arda_users, department_structure, is_active):
    """Отзываем активные связанные роли, выданные по неактивной групповой"""

    frodo = arda_users.frodo
    set_workflow(simple_system, group_code='approvers = []')
    fellowship = department_structure.fellowship
    members_count = fellowship.members.count()

    role = Role.objects.request_role(frodo, fellowship, simple_system, comment='', data={'role': 'manager'},
                                     fields_data={'a': 'b', 'c': 'd'})
    assert Role.objects.count() == 1 + members_count
    user_roles = Role.objects.filter(group=None)
    assert user_roles.count() == members_count
    assert user_roles.filter(state='granted').count() == members_count
    role = refresh(role)
    # отзовём групповую роль неправомерным способом
    role.set_raw_state(ROLE_STATE.DEPRIVED, is_active=False)
    role = refresh(role)
    assert role.state == ROLE_STATE.DEPRIVED

    simple_system.is_active = is_active
    simple_system.save()

    # связанные роли должны отозваться, если система активна
    call_command('idm_poke_hanging_roles', stage='deprive_refs')

    assert Role.objects.count() == 1 + members_count
    if is_active:
        assert Role.objects.filter(is_active=True).count() == 0
        assert Role.objects.filter(state=ROLE_STATE.DEPRIVED).count() == 1 + members_count
        assert_action_chain(role.refs.first(), [
            'request', 'approve', 'first_add_role_push', 'grant', 'deprive', 'first_remove_role_push', 'remove',
        ])
    else:
        # ничего не произошло
        assert Role.objects.filter(is_active=True).count() == members_count


def test_deprive_ref_roles_of_inactive_user_role(simple_system, arda_users):
    """Отзываем активные связанные роли, выданные по неактивной пользовательской"""

    frodo = arda_users.frodo
    set_workflow(simple_system, group_code='approvers = []')

    set_workflow(simple_system, dedent('''
        approvers = []
        if role.get('role') == 'admin':
            ref_roles = [{
                'system': '%(simple)s',
                'role_data': {
                    'role': 'manager'
                }
            }, {
                'system': '%(simple)s',
                'role_data': {
                    'role': 'poweruser'
                }
            }]
    ''') % {
        'simple': simple_system.slug
    })
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    assert Role.objects.count() == 3
    assert role.refs.count() == 2
    role = refresh(role)
    # отзовём групповую роль неправомерным способом
    role.set_raw_state('deprived', is_active=False)
    role = refresh(role)
    assert role.state == 'deprived'
    # связанные роли должны отозваться
    call_command('idm_poke_hanging_roles')
    assert Role.objects.count() == 3
    assert Role.objects.filter(is_active=True).count() == 0
    assert role.refs.count() == 2
    assert role.refs.filter(state=ROLE_STATE.DEPRIVED).count() == 2
    assert_action_chain(role.refs.first(), [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        'deprive', 'first_remove_role_push', 'remove',
    ])


def test_deprive_unapplicable_personal_roles_with_specified_system(
    simple_system,
    other_system,
    arda_users,
    department_structure,
):
    """Отзываем активные персональные роли, выданные по групповой,
    которая не является родительской для групп пользователя, делаем это для конкретной системы"""

    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    valinor = department_structure.valinor
    set_workflow(simple_system, DEFAULT_WORKFLOW, DEFAULT_WORKFLOW)
    set_workflow(other_system, DEFAULT_WORKFLOW, DEFAULT_WORKFLOW)

    role1 = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)
    # c ролями 2-4 ничего произойти не должно
    role2 = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None)
    role3 = Role.objects.request_role(frodo, fellowship, other_system, '', {'role': 'admin'}, None)
    role4 = Role.objects.request_role(frodo, fellowship, other_system, '', {'role': 'manager'}, None)
    # зададим другую группу, в которую не входят члены fellowship
    role1.group = valinor
    role1.save(update_fields=('group',))
    role3.group = valinor
    role3.save(update_fields=('group',))
    for role in (role1, role2, role3, role4):
        assert role.refs.filter(state='granted').count() == fellowship.members.count()

    call_command('idm_poke_hanging_roles', stage='request_or_deprive_personal', system=simple_system)

    role1 = refresh(role1)
    role2 = refresh(role2)
    role3 = refresh(role3)
    role4 = refresh(role4)
    assert role1.state == 'granted'
    assert role1.refs.filter(state='granted').count() == valinor.get_descendant_members().count()
    members_to_onhold = len(fellowship.get_descendant_members().exclude(id__in=valinor.get_descendant_members()))
    assert role1.refs.filter(state='onhold').count() == members_to_onhold

    for role in (role2, role3, role4):
        # с этими ролями всё хорошо, ничего не отозвалось
        assert role.state == 'granted'
        assert role.refs.filter(state='granted').count() == fellowship.members.count()


def test_deprive_inapplicable_personal_roles_up(simple_system, arda_users, department_structure):
    """Отзываем активные персональные роли, выданные по групповой,
    которая не является родительской для групп пользователя.
    Проверим случай, когда пользователи входят в группу не напрямую."""

    frodo = arda_users.frodo
    associations = department_structure.associations
    valinor = department_structure.valinor
    fellowship = department_structure.fellowship
    set_workflow(simple_system, DEFAULT_WORKFLOW, DEFAULT_WORKFLOW)

    role1 = Role.objects.request_role(frodo, associations, simple_system, '', {'role': 'admin'}, None)
    # role2 - тестовая, чтобы проверить, что с нормальными ролями ничего не случилось
    role2 = Role.objects.request_role(frodo, associations, simple_system, '', {'role': 'manager'}, None)
    # одну из ролей переведём в onhold, чтобы проверить, что из этого статуса роль не будет отозвана
    gimli_role = role1.refs.select_related('system').get(user=arda_users.gimli)
    gimli_role.set_state('onhold')
    # зададим другую группу, в которую не входят члены fellowship (так можно было? @cracker)
    role1.group = valinor
    role1.save(update_fields=('group',))
    # члены associations и fellowship не пересекаются, 1 отвечает за роль в onhold
    assert role1.refs.filter(state='granted').count() == fellowship.members.count() + associations.members.count() - 1
    assert role2.refs.filter(state='granted').count() == fellowship.members.count() + associations.members.count()

    call_command('idm_poke_hanging_roles', stage='request_or_deprive_personal')
    role1 = refresh(role1)
    role2 = refresh(role2)
    assert role1.state == 'granted'
    assert role1.refs.filter(state='granted').count() == valinor.get_descendant_members().count()
    members_to_onhold = len(associations.get_descendant_members().exclude(id__in=valinor.get_descendant_members()))
    assert role1.refs.filter(state='onhold').count() == members_to_onhold
    gimli_role = refresh(gimli_role)
    assert gimli_role.state == 'onhold'
    assert role2.state == 'granted'
    # с role2 всё хорошо, ничего не отозвалось
    assert role2.refs.filter(state='granted').count() == fellowship.members.count() + associations.members.count()


def test_deprive_inapplicable_personal_roles_in_inactive_system(simple_system, complex_system, arda_users,
                                                                department_structure):
    """Проверим, что команда отзывает все необходимые роли в несломанных системах"""
    frodo = arda_users.frodo
    valinor = department_structure.valinor
    complex_system.group_policy = 'unaware'
    complex_system.save()
    fellowship = department_structure.fellowship
    intersection_count = len(
        set(fellowship.members.values_list('username', flat=True)) &
        set(valinor.members.values_list('username', flat=True))
    )
    set_workflow(simple_system, DEFAULT_WORKFLOW, DEFAULT_WORKFLOW)
    set_workflow(complex_system, DEFAULT_WORKFLOW, DEFAULT_WORKFLOW)

    role1 = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)
    role2 = Role.objects.request_role(frodo, fellowship, complex_system, '', {'project': 'rules', 'role': 'admin'},
                                      {'passport-login': 'yndx-frodo', 'field_1': 'hello'})
    role1 = refresh(role1)
    role2 = refresh(role2)
    assert role1.state == 'granted'
    assert role1.refs.filter(state='granted').count() == fellowship.members.count()
    assert role2.state == 'granted'
    assert role2.refs.awaiting().count() == fellowship.members.count()  # не привязан логин

    for mb in GroupMembership.objects.filter(group=fellowship, state=GROUPMEMBERSHIP_STATE.ACTIVE).select_related('user'):
        login = mb.user.passport_logins.create(login=mb.user.username, state='created', is_fully_registered=True)
        mb.passport_login = login
        mb.save()
    Role.objects.poke_awaiting_roles()
    assert role2.refs.filter(state='granted').count() == fellowship.members.count()

    # сломаем систему и перенесём две роли в другое место, а затем запустим команду
    simple_system.is_active = False
    simple_system.save()

    Role.objects.filter(user=None).update(group=valinor)
    call_command('idm_poke_hanging_roles', stage='request_or_deprive_personal')

    role1 = refresh(role1)
    role2 = refresh(role2)
    # в сломанной системе роли тоже перешли в onhold
    for role in [role1, role2]:
        assert role.state == 'granted'
        assert role.refs.filter(state='granted').count() == intersection_count
        assert role.refs.filter(state='onhold').count() == fellowship.members.count() - intersection_count


@pytest.mark.parametrize('deprive_switch', [True, False])
@pytest.mark.parametrize('hold_switch', [True, False])
@pytest.mark.parametrize(('attr', 'attr_val', 'subjects'), [
    ('affiliation', AFFILIATION.EXTERNAL, 'external'),
    ('is_robot', True, 'robots'),
])
def test_deprive_inapplicable_roles_by_additional_fields(simple_system, arda_users, department_structure,
                                                         attr, attr_val, subjects, hold_switch, deprive_switch):
    """Проверим случай, когда пользователь становится внешним в системе без выдачи ролей внешним."""

    frodo = arda_users.frodo
    associations = department_structure.associations
    set_workflow(simple_system, DEFAULT_WORKFLOW, DEFAULT_WORKFLOW)
    group_role = Role.objects.request_role(
        frodo, associations, simple_system, '', {'role': 'poweruser'}, None, **{'with_' + subjects: False}
    )
    personal_role = Role.objects.request_role(
        frodo, frodo, simple_system, '', {'role': 'manager'}, None, **{'with_' + subjects: False}
    )

    setattr(frodo, attr, attr_val)
    frodo.save()

    with override_switch('deprive_inapplicable_%s_roles' % subjects, active=deprive_switch),\
            override_switch('hold_inapplicable_%s_roles' % subjects, active=hold_switch):
        Role.objects.deprive_inapplicable_personal_roles()

    personal_role.refresh_from_db()
    assert personal_role.state == ROLE_STATE.GRANTED

    personal_role_by_group_role = Role.objects.get(parent=group_role, user=frodo)
    if not hold_switch:
        expected_state = ROLE_STATE.GRANTED
    else:
        expected_state = ROLE_STATE.DEPRIVED if deprive_switch else ROLE_STATE.ONHOLD
    assert personal_role_by_group_role.state == expected_state


@override_switch('idm.deprive_not_immediately', active=True)
def test_deprive_hanging_roles_if_deprive_not_immediately_active(simple_system, arda_users):
    frodo = arda_users.frodo
    depriving_role = raw_make_role(
        frodo,
        simple_system,
        {'role': 'admin'},
        state=ROLE_STATE.DEPRIVING,
    )
    depriving_validation_role = raw_make_role(
        frodo,
        simple_system,
        {'role': 'manager'},
        state=ROLE_STATE.DEPRIVING_VALIDATION,
        depriving_at=timezone.now() - timezone.timedelta(days=1)
    )

    call_command('idm_poke_hanging_roles')

    depriving_role.refresh_from_db()
    assert depriving_role.state == ROLE_STATE.DEPRIVED

    depriving_validation_role.refresh_from_db()
    assert depriving_validation_role.state == ROLE_STATE.DEPRIVING_VALIDATION


def test_deprive_dismissed_personal_role(simple_system, arda_users, department_structure):
    """Проверка того, что poke_hanging_depriving_roles ретраит персональные роли уволенных"""
    frodo = arda_users.frodo
    associations = department_structure.associations
    set_workflow(simple_system, DEFAULT_WORKFLOW, DEFAULT_WORKFLOW)
    group_role = Role.objects.request_role(
        frodo, associations, simple_system, '', {'role': 'poweruser'}
    )
    frodo.is_active = False
    frodo.save()
    personal = group_role.refs.select_related('user', 'system', 'node').get(user=frodo)
    personal.state = ROLE_STATE.DEPRIVING
    personal.save(update_fields=['state'])
    call_command('idm_poke_hanging_roles', stage='poke_depriving')
    personal.refresh_from_db()
    assert personal.state == ROLE_STATE.DEPRIVED


def test_deprive_inapplicable_roles_without_hold(simple_system, other_system, arda_users, department_structure):
    """Отзываем активные персональные роли, выданные по групповой,
    которая не является родительской для групп пользователя
    Проверим случай, если установлен флаг without_hold=True"""

    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    set_workflow(simple_system, DEFAULT_WORKFLOW, DEFAULT_WORKFLOW)

    role = Role.objects.request_role(
        frodo, fellowship, simple_system, '', {'role': 'admin'}, None, without_hold=True
    )

    # зададим другую группу, в которую не входят члены fellowship
    valinor = department_structure.valinor
    role.group = valinor
    role.save(update_fields=('group',))
    assert role.refs.filter(state='granted').count() == fellowship.members.count()

    with patch.object(Role, 'deprive_or_decline') as deprive_or_decline:
        deprive_or_decline.side_effect = Exception("test error")
        with patch.object(timezone, 'now', return_value=timezone.now() - timezone.timedelta(minutes=5)):
            call_command('idm_poke_hanging_roles', stage='request_or_deprive_personal', system=simple_system)

    role = refresh(role)
    assert role.refs.filter(state='granted').count() == valinor.get_descendant_members().count()
    members_to_onhold = len(fellowship.get_descendant_members().exclude(id__in=valinor.get_descendant_members()))
    assert role.refs.filter(state='onhold').count() == members_to_onhold

    simple_system.deprive_expired_roles()

    role = refresh(role)
    assert role.refs.filter(state='granted').count() == valinor.get_descendant_members().count()
    members_to_onhold = len(fellowship.get_descendant_members().exclude(id__in=valinor.get_descendant_members()))
    assert role.refs.filter(state='deprived').count() == members_to_onhold
