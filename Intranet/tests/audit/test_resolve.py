# -*- coding: utf-8 -*-


from datetime import timedelta
from textwrap import dedent

import pytest
from django.core import mail
from django.core.management import call_command
from django.utils import timezone
from django.utils.dateformat import format
from mock import patch

from idm.core.constants.system import SYSTEM_INCONSISTENCY_POLICY, SYSTEM_GROUP_POLICY
from idm.core.models import Role, Action, RoleRequest, ApproveRequest, UserPassportLogin
from idm.core.plugins.errors import PluginFatalError, PluginError
from idm.inconsistencies.models import Inconsistency
from idm.tests.utils import (raw_make_role, refresh, compare_time, set_workflow, assert_contains,
                             assert_action_chain, make_inconsistency, mock_all_roles, assert_inconsistency,
                             make_role, expire_role, capture_http, assert_http,
                             mock_tree, sync_role_nodes, days_from_now)
from idm.users.models import User

pytestmark = [pytest.mark.django_db, pytest.mark.robot]


def test_resolve_our_side_inconsistency(simple_system, arda_users, superuser_gandalf):
    """У нас роль есть, в системе нет. Надо убрать роль у нас."""

    gandalf = superuser_gandalf

    role = raw_make_role(arda_users.frodo, simple_system, {'role': 'admin'}, state='granted')
    with mock_all_roles(simple_system, []):
        Inconsistency.objects.check_roles()
    inconsistency = Inconsistency.objects.select_related('system__actual_workflow', 'our_role__node').get()
    assert_inconsistency(inconsistency, system=simple_system, type=Inconsistency.TYPE_OUR,
                         user=arda_users.frodo, our_role=role, state='active')

    inconsistency.resolve(requester=gandalf, force=True)
    role = refresh(role)
    assert not role.is_active
    assert role.state == 'deprived'

    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'
    assert gandalf.requested.count() == 1
    action = gandalf.requested.get()
    assert action.data == {
        'is_forcibly_resolved': True,
        'user': 'frodo',
        'comment': 'Разрешено расхождение',
    }
    assert action.inconsistency_id == inconsistency.id


def test_resolve_our_side_group_inconsistency(aware_simple_system, arda_users, department_structure, superuser_gandalf):
    """У нас роль есть, в системе нет. Надо убрать роль у нас."""

    fellowship = department_structure.fellowship
    system = aware_simple_system
    gandalf = superuser_gandalf

    role = raw_make_role(fellowship, system, {'role': 'admin'}, state='granted')
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_OUR,
        group=fellowship,
        system=role.system,
        our_role=role,
    )

    assert inconsistency.state == 'active'
    assert role.is_active is True
    inconsistency.resolve(gandalf, force=True)
    role = refresh(role)
    assert not role.is_active
    assert role.state == 'deprived'

    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'
    assert gandalf.requested.count() == 1
    action = gandalf.requested.get()
    assert action.data == {
        'is_forcibly_resolved': True,
        'group': fellowship.external_id,
        'comment': 'Разрешено расхождение',
    }
    assert action.inconsistency_id == inconsistency.id


def test_accept_inconsistency_from_their_side(simple_system, arda_users, superuser_gandalf):
    """ У нас роли нет, а в системе есть. Надо добавить роль в нашу базу."""

    frodo = arda_users.frodo
    gandalf = superuser_gandalf

    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=frodo,
        system=simple_system,
        path='/role/admin/',
        remote_fields={'passport-login': 'yndx-frodo98', 'hello': 'world'}
    )

    inconsistency.resolve(requester=gandalf, force=True)
    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'
    assert frodo.roles.count() == 1
    role = frodo.roles.get()
    assert role.state == 'granted'
    assert role.fields_data == {'passport-login': 'yndx-frodo98'}
    assert role.system_specific == {'passport-login': 'yndx-frodo98', 'hello': 'world'}

    assert gandalf.requested.count() == 1
    action = gandalf.requested.get()
    assert action.data == {
        'is_forcibly_resolved': True,
        'user': 'frodo',
        'comment': 'Разрешено расхождение',
    }
    assert action.inconsistency_id == inconsistency.id


def test_accept_group_inconsistency_from_their_side(aware_simple_system, arda_users, department_structure,
                                                    superuser_gandalf):
    """ У нас групповой роли нет, а в системе есть. Надо добавить в нашу базу."""

    fellowship = department_structure.fellowship
    system = aware_simple_system
    gandalf = superuser_gandalf
    set_workflow(system, code='approvers = []', group_code='approvers = []; ttl_days=2')

    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        group=fellowship,
        system=system,
        path='/role/admin/',
        remote_fields={'passport_login': 'alex'}
    )
    inconsistency.resolve(requester=gandalf, force=True)
    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'
    assert fellowship.roles.count() == 1
    role = fellowship.roles.get()
    assert role.state == 'granted'
    assert_action_chain(role, ['import', 'approve', 'grant', 'resolve_inconsistency'])
    assert role.refs.count() == 0
    resolve_action = role.actions.get(action='resolve_inconsistency')
    assert resolve_action.requester_id == gandalf.id
    assert resolve_action.inconsistency_id == inconsistency.id
    assert resolve_action.data == {
        'comment': 'Разрешено расхождение',
        'group': fellowship.external_id,
        'is_forcibly_resolved': True,
    }


def test_send_approverequest_emails_on_resolve_inconsistency(simple_system, arda_users):
    """Тестируем отправку писем с запросами подтверждения роли при разрешении неконсистентности"""

    frodo = arda_users.frodo
    set_workflow(simple_system, "approvers = [approver('legolas'), approver('gandalf')]")

    # пусть в системе есть роль админ для frodo, а у нас нет
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=frodo,
        system=simple_system,
        path='/role/superuser/',
    )

    # выполним команду разрешения неконсистентностей и удостоверимся в отправке email с запросами подтверждений
    call_command('idm_check_and_resolve', resolve_only=True)

    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'

    # отправляем только письмо владельцу роли
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.subject == 'В системе Simple система заведена роль в обход IDM.'
    assert message.to == ['frodo@example.yandex.ru']
    assert_contains((
        'В системе "Simple система" у вас появилась роль "Роль: Супер Пользователь"',
        'Похоже, что она была выдана в обход IDM.',
        'Роль должна быть подтверждена сотрудниками:',
        '* legolas',
        '* gandalf',
    ), message.body)

    assert frodo.roles.count() == 1
    role = frodo.roles.select_related('node').get()
    assert role.node.data == {'role': 'superuser'}
    assert role.inconsistency == inconsistency
    assert role.state == 'imported'
    assert role.is_active

    # завелся запрос
    assert role.requests.count() == 1
    assert role.requests.get().approves.count() == 2
    assert not role.requests.get().is_done

    assert_action_chain(role, ['import', 'apply_workflow', 'resolve_inconsistency'])


def test_resolve_their_with_sox_policy(simple_system, arda_users):
    """Тестируем опцию запрашивания ролей при импорте"""

    frodo = arda_users.frodo
    simple_system.inconsistency_policy = SYSTEM_INCONSISTENCY_POLICY.STRICT_SOX
    simple_system.save()
    set_workflow(simple_system, "approvers = [approver('legolas'), approver('gandalf')]")

    # пусть в системе есть роль админ для frodo, а у нас нет
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=frodo,
        system=simple_system,
        path='/role/superuser/',
    )

    # выполним команду разрешения неконсистентностей и удостоверимся в отправке email с запросами подтверждений
    call_command('idm_check_and_resolve', resolve_only=True)

    assert len(mail.outbox) == 3  # 2 письма подтверждающим, 1 юзеру
    requester_message = mail.outbox[2]
    assert requester_message.subject == 'Роль в системе "Simple система" требует подтверждения.'
    assert requester_message.to == ['frodo@example.yandex.ru']
    assert_contains((
        'robot-idm запросил для вас роль в системе "Simple система":',
        'Роль: Супер Пользователь',
        'Основные подтверждающие, оповещены:',
        'legolas',
        'gandalf',
    ), requester_message.body)

    assert frodo.roles.count() == 1
    role = frodo.roles.select_related('node').get()
    assert role.node.data == {'role': 'superuser'}
    assert role.inconsistency == inconsistency

    # завелся запрос
    assert role.state == 'requested'
    assert role.requests.count() == 1
    assert role.requests.get().approves.count() == 2
    assert not role.requests.get().is_done

    assert {action.action for action in role.actions.all()} == {'request', 'apply_workflow'}
    action = role.actions.get(action='request')
    assert action.comment == 'Запрошена при разрешении расхождения'
    assert {action.action for action in inconsistency.actions.all()} == {'remote_remove', 'resolve_inconsistency'}
    remote_remove = inconsistency.actions.get(action='remote_remove')
    assert remote_remove.comment == (
        'Расхождение разрешено без импорта роли, так как этого требует политика системы'
    )


def test_resolve_their_request_plugin_error(simple_system, arda_users):
    """Проверяем поведение в случае ошибки плагина при отзыве неконсистентной роли"""
    frodo = arda_users.frodo
    simple_system.inconsistency_policy = SYSTEM_INCONSISTENCY_POLICY.STRICT_SOX
    simple_system.save()
    set_workflow(simple_system, "approvers = [approver('legolas'), approver('gandalf')]")

    # пусть в системе есть роль админ для frodo, а у нас нет
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=frodo,
        system=simple_system,
        path='/role/superuser/',
    )

    plugin = simple_system.plugin
    with patch.object(plugin.__class__, 'remove_role') as remove_role:
        remove_role.side_effect = PluginError(1, 'blah minor', {'a': 'b'})
        call_command('idm_check_and_resolve', resolve_only=True)

    # проверим, что мы ретраились
    assert remove_role.call_count == 7

    # проверим, что неконсистентность не разрешилась и роль не запросилась|
    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'active'
    assert Role.objects.count() == 0


def test_resolve_their_request_single_plugin_error(simple_system, arda_users):
    """Проверяем поведение в случае ошибки плагина при отзыве неконсистентной роли"""
    frodo = arda_users.frodo
    simple_system.inconsistency_policy = SYSTEM_INCONSISTENCY_POLICY.STRICT_SOX
    simple_system.save()
    set_workflow(simple_system, "approvers = [approver('legolas'), approver('gandalf')]")

    # пусть в системе есть роль админ для frodo, а у нас нет
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=frodo,
        system=simple_system,
        path='/role/superuser/',
    )

    plugin = simple_system.plugin
    with patch.object(plugin.__class__, 'remove_role') as remove_role:
        remove_role.side_effect = [PluginError(1, 'blah minor', {'a': 'b'}), None]
        call_command('idm_check_and_resolve', resolve_only=True)

    # проверим, что мы ретраились 1 раз
    assert remove_role.call_count == 2

    # проверим, что неконсистентность разрешилась и роль запросилась|
    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'
    assert Role.objects.count() == 1
    assert Role.objects.get().state == 'requested'


def test_resolve_our(simple_system, arda_users):
    """Тестируем запрос роли при разрешении неконсистентности, если такая же роль уже была у нас (удалена)"""
    frodo = arda_users.frodo

    # старая роль, проверим, что не будет мешать
    old_role = raw_make_role(frodo, simple_system, {'role': 'superuser'}, state='deprived')

    # пусть у нас есть роль для frodo, а в системе нет
    role = raw_make_role(frodo, simple_system, {'role': 'superuser'}, state='granted')
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_OUR,
        user=frodo,
        system=simple_system,
        our_role=role,
    )
    # выполним команду разрешения неконсистентностей и удостоверимся, что неконсистентность разрешена
    call_command('idm_check_and_resolve', resolve_only=True)

    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'

    old_role = refresh(old_role)
    role = refresh(role)

    assert frodo.roles.count() == 2
    assert old_role.state == 'deprived'
    assert not old_role.is_active

    assert role.state == 'deprived'
    assert not role.is_active
    assert RoleRequest.objects.count() == 0  # запросы не создаются


def test_grant_role_for_inconsistency(simple_system, arda_users):
    """Тестируем выдачу проаппрувленной роли, созданной по неконсистентности"""
    frodo = arda_users.frodo

    # пусть в системе есть роль админ для frodo, а у нас нет
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=frodo,
        system=simple_system,
        path='/role/superuser/'
    )
    assert len(mail.outbox) == 0
    # выполним команду разрешения неконсистентностей и удостоверимся в отправке email с запросами подтверждений
    call_command('idm_check_and_resolve', resolve_only=True)

    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'

    assert frodo.roles.count() == 1
    role = frodo.roles.select_related('node').get()
    assert role.node.data == {'role': 'superuser'}
    assert role.inconsistency == inconsistency
    assert role.state == 'granted'
    assert compare_time(timezone.now(), role.granted_at, epsilon=3)
    assert role.is_active

    assert role.requests.count() == 1
    assert_action_chain(role, ['import', 'apply_workflow', 'approve', 'grant', 'resolve_inconsistency'])


def test_grant_role_refs_for_inconsistency(simple_system_w_refs, arda_users):
    """Проверяем, что выдалась связанная роль при разрешении неконсистентности"""

    frodo = arda_users.frodo
    # пусть в системе есть роль админ для frodo, а у нас нет
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=frodo,
        system=simple_system_w_refs,
        path='/role/admin/'
    )
    call_command('idm_check_and_resolve', resolve_only=True)

    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'
    assert frodo.roles.count() == 2
    role = frodo.roles.filter(inconsistency=None).get()
    role.fetch_node()
    assert role.node.data == {'role': 'manager'}
    assert role.fields_data == {'login': '/'}


def test_decline_and_deprive_role_from_inconsistency(simple_system, arda_users):
    """При запросе роли по неконсистентности, отклоним её и отзовем из системы"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    set_workflow(simple_system, 'approvers = [approver("legolas")]')

    # пусть в системе есть роль админ для frodo, а у нас нет
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=frodo,
        system=simple_system,
        path='/role/superuser/'
    )

    # выполним команду разрешения неконсистентностей
    call_command('idm_check_and_resolve', resolve_only=True)

    # отправляем только письмо владельцу роли
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.subject == 'В системе Simple система заведена роль в обход IDM.'
    assert message.to == ['frodo@example.yandex.ru']
    expire_at = timezone.localtime(timezone.now() + timedelta(days=7))
    assert_contains((
        'В системе "Simple система" у вас появилась роль "Роль: Супер Пользователь"',
        'Похоже, что она была выдана в обход IDM.',
        'Роль должна быть подтверждена сотрудниками:',
        ' * legolas',
        'Если роль не будет подтверждена до %s, она будет отозвана.' % format(expire_at, 'j E Y г.')
    ), message.body)

    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'
    role = Role.objects.get()
    assert role.state == 'imported'
    assert role.inconsistency == inconsistency
    assert role.requests.count() == 1
    role_request = role.requests.get()
    assert role_request.approves.count() == 1
    assert not role_request.is_done

    approve_request = ApproveRequest.objects.select_related_for_set_decided().get()
    assert approve_request.approved is None

    # теперь отклоним запрос на подтверждение роли
    approve_request.set_declined(legolas)

    role = refresh(role)
    assert role.state == 'deprived'
    assert not role.is_active

    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'
    assert role.requests.count() == 1
    assert role.requests.get().is_done


def test_expire_role_requested_by_inconsistency(simple_system, arda_users):
    """Проверяем переключение роли из requested в expired в случае истечения срока её подтверждения"""

    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = [approver("legolas")]')

    # пусть в системе есть роль админ для frodo, а у нас нет
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=frodo,
        system=simple_system,
        path='/role/superuser/'
    )

    # выполним команду разрешения неконсистентностей
    call_command('idm_check_and_resolve', resolve_only=True)
    assert Role.objects.count() == 1
    role = Role.objects.get()
    assert role.user_id == frodo.id
    assert role.state == 'imported'

    # отправляем только письмо владельцу роли
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.subject == 'В системе Simple система заведена роль в обход IDM.'
    assert message.to == ['frodo@example.yandex.ru']
    assert_contains((
        'В системе "Simple система" у вас появилась роль "Роль: Супер Пользователь"',
        'Похоже, что она была выдана в обход IDM.',
        'Роль должна быть подтверждена сотрудниками:',
        '* legolas',
    ), message.body)

    assert role.requests.count() == 1
    role_request = role.requests.get()
    assert role_request.approves.count() == 1
    assert role_request.is_done is False

    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'

    expire_role(role, days=3)

    simple_system.deprive_expired_roles()

    # роль должна перейти в состояние depriving, а затем deprived, тк из системы мы её тоже отозвали
    role = refresh(role)
    assert role.state == 'deprived'
    assert not role.is_active
    assert role.expire_at is None
    assert_action_chain(
        role, ['import', 'apply_workflow', 'resolve_inconsistency', 'expire', 'first_remove_role_push', 'remove']
    )

    assert role.requests.count() == 1
    assert role.requests.get().is_done is True

    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'


def test_decline_and_fail_deprive_role_from_inconsistency(generic_system, arda_users):
    """При запросе роли по неконсистентности, отклоняем роль, пытаемся удалить из системы, система не удаляет"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    set_workflow(generic_system, "approvers = [approver('legolas')]")

    # пусть в системе есть роль админ для frodo, а у нас нет
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=frodo,
        system=generic_system,
        path='/role/superuser/'
    )

    # выполним команду разрешения неконсистентностей
    call_command('idm_check_and_resolve', resolve_only=True)

    # отправляем только письмо владельцу роли
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.subject == 'В системе %s заведена роль в обход IDM.' % generic_system.name
    assert message.to == ['frodo@example.yandex.ru']
    assert_contains((
        'В системе "Generic система" у вас появилась роль "Роль: Супер Пользователь"',
        'Похоже, что она была выдана в обход IDM.',
        'Роль должна быть подтверждена сотрудниками:',
        ' * legolas',
    ), message.body)

    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'
    assert ApproveRequest.objects.count() == 1

    role = Role.objects.get()
    assert role.state == 'imported'
    assert role.inconsistency == inconsistency
    assert role.requests.count() == 1  # запрос создался
    assert role.requests.get().is_done is False

    approve_request = ApproveRequest.objects.select_related_for_set_decided().get()
    assert approve_request.approved is None

    # теперь отклоним запрос на подтверждение роли, но отзыв роли в системе сфейлится
    plugin = generic_system.plugin
    with patch.object(plugin.__class__, 'remove_role') as remove_role:
        remove_role.side_effect = PluginFatalError(1, 'blah minor', {'a': 'b'})
        approve_request.set_declined(legolas)

    role = refresh(role)
    assert role.state == 'depriving'   # состояние не меняется, тк при отзыва роли произошла ошибка
    assert role.is_active
    assert_action_chain(role, ['import', 'apply_workflow', 'resolve_inconsistency', 'deprive', 'first_remove_role_push'])
    error_action = role.actions.get(action='deprive')
    assert error_action.error == '''PluginFatalError: code=1, message="blah minor", data={'a': 'b'}, answer="None"'''
    assert error_action.data['code'] == 1
    assert role.requests.count() == 1
    assert role.requests.get().is_done is True

    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'


def test_remove_role_by_inconsistency(simple_system, arda_users):
    """Тестируем удаление роли у нас, если таковой не обнаружено в системе"""
    frodo = arda_users.frodo
    Action.objects.all().delete()

    # пусть у нас есть роль менеджер для frodo, а в системе - нет
    role = role = raw_make_role(frodo, simple_system, {'role': 'manager'}, state='granted')
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_OUR,
        user=frodo,
        system=simple_system,
        our_role=role,
    )

    # выполним команду и удостоверимся, что у нас нет больше роли для frodo (она removed)
    call_command('idm_check_and_resolve', resolve_only=True)

    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'

    assert Role.objects.count() == 1
    assert frodo.roles.count() == 1

    role = Role.objects.get()

    assert role.user_id == frodo.id
    assert role.state == 'deprived'
    assert role.is_active is False
    role.fetch_node()
    assert role.node.data == {'role': 'manager'}
    assert role.inconsistency is None

    assert Action.objects.count() == 4
    assert Action.objects.filter(action='resolve_inconsistency').count() == 1
    assert role.actions.count() == 2

    remove_action, resolve_action = role.actions.order_by('added')
    assert remove_action.action == 'remove'
    assert remove_action.data == {
        'comment': 'Роль удалена при разрешении расхождения',
    }
    assert remove_action.inconsistency_id == inconsistency.id
    assert resolve_action.action == 'resolve_inconsistency'
    assert resolve_action.data == {
        'comment': 'Разрешено расхождение',
        'user': 'frodo'
    }
    assert resolve_action.inconsistency_id == inconsistency.id
    assert resolve_action.role == role


def test_command_resolve_inconsistencies(simple_system, arda_users):
    """Тестируем команду разрешения неконсистентностей"""
    frodo = arda_users.frodo
    legolas = arda_users.legolas

    # пусть у нас есть роль менеджер для frodo, а в системе - роль админ для legolas
    role1 = raw_make_role(frodo, simple_system, {'role': 'manager'}, state='granted')
    inconsistency1 = make_inconsistency(
        type=Inconsistency.TYPE_OUR,
        user=frodo,
        system=simple_system,
        our_role=role1
    )

    inconsistency2 = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=legolas,
        system=simple_system,
        path='/role/superuser/',
    )

    assert Role.objects.count() == 1
    assert RoleRequest.objects.count() == 0

    # выполним команду и удостоверимся, что обе неконсистентности разрешились: одна роль удалилась и одна добавилась
    call_command('idm_check_and_resolve', resolve_only=True)

    inconsistency1 = refresh(inconsistency1)
    inconsistency2 = refresh(inconsistency2)
    assert inconsistency1.state == 'resolved'
    assert inconsistency2.state == 'resolved'

    assert Role.objects.count() == 2
    role1 = frodo.roles.get()
    role2 = legolas.roles.get()

    assert role1.state == 'deprived'
    assert not role1.is_active
    role1.fetch_node()
    assert role1.node.data == {'role': 'manager'}

    assert role2.state == 'granted'
    assert compare_time(timezone.now(), role2.granted_at, epsilon=3)
    assert role2.is_active
    role2.fetch_node()
    assert role2.node.data == {'role': 'superuser'}
    assert role2.user == legolas

    assert role1.requests.count() == 0
    assert role2.requests.count() == 1

    assert Action.objects.filter(action='resolve_inconsistency').count() == 2
    assert role1.actions.count() == 2
    assert role2.actions.count() == 5

    remove_action, resolve_action = role1.actions.order_by('added')
    assert remove_action.action == 'remove'
    assert remove_action.data == {
        'comment': 'Роль удалена при разрешении расхождения',
    }
    assert remove_action.inconsistency_id == inconsistency1.id
    assert resolve_action.action == 'resolve_inconsistency'
    assert resolve_action.data == {
        'comment': 'Разрешено расхождение',
        'user': 'frodo'
    }
    assert resolve_action.inconsistency_id == inconsistency1.id
    assert_action_chain(role2, ['import', 'apply_workflow', 'approve', 'grant', 'resolve_inconsistency'])
    import_action = role2.actions.get(action='import')
    assert import_action.data == {
        'comment': 'Запрошена при разрешении расхождения',
    }
    assert import_action.inconsistency_id == inconsistency2.id
    resolve_action = role2.actions.get(action='resolve_inconsistency')
    assert resolve_action.role == role2
    assert resolve_action.data == {
        'comment': 'Разрешено расхождение',
        'user': 'legolas'
    }
    assert resolve_action.inconsistency_id == inconsistency2.id


def test_inconsistency_on_dismiss_user(generic_system, arda_users):
    """
    При наличии неконсистентности для уволенного сотрудника,
    пытаемся отозвать его роль из системы без заведения таковой у себя
    """
    frodo = arda_users.frodo
    frodo.is_active = False
    frodo.save()
    legolas = arda_users.legolas

    set_workflow(generic_system, 'approvers = [approver("legolas")]')

    # пусть в системе есть роль админ для уволенного frodo, а у нас нет
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=frodo,
        system=generic_system,
        path='/role/superuser/',
        remote_fields={'foo': 'bar'}
    )

    # выполним команду разрешения неконсистентностей
    with patch.object(generic_system.plugin.__class__, 'remove_role') as remove_role:
        remove_role.return_value = {"code": 0}
        call_command('idm_check_and_resolve', resolve_only=True)
        remove_role.assert_called_once_with(username='frodo',
                                            request_id=Action.objects.get(action='remote_remove').pk,
                                            unique_id='',
                                            is_fired=True,
                                            role_data={'role': 'superuser'},
                                            path='/role/superuser/',
                                            fields_data={'foo': 'bar'},
                                            subject_type=None,
                                            system_specific={'foo': 'bar'})

    assert len(mail.outbox) == 0  # аппрувы рассылаться не должны
    assert frodo.roles.count() == 0
    assert legolas.approve_requests.count() == 0
    assert RoleRequest.objects.count() == 0

    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'

    assert {action.action for action in inconsistency.actions.all()} == {'resolve_inconsistency', 'remote_remove'}
    action = inconsistency.actions.get(action='resolve_inconsistency')
    assert action.action == 'resolve_inconsistency'
    assert action.data == {
        'user': 'frodo',
        'resolved_because_user_is_not_active': True,
        'comment': 'Расхождение разрешено, так как пользователь уволен'
    }


def test_inconsistency_on_group_deletion(aware_generic_system, arda_users, department_structure):
    """
    При наличии неконсистентности для удалённой группы пытаемся отозвать её роль
    из системы без заведения таковой у себя
    """
    system = aware_generic_system
    fellowship = department_structure.fellowship
    fellowship.state = 'depriving'
    fellowship.save()

    set_workflow(system, 'approvers = [approver("legolas")]')
    Action.objects.all().delete()

    # пусть в системе есть роль админ для удалённой группы, а у нас нет
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        group=fellowship,
        system=system,
        path='/role/superuser/',
        remote_fields={'foo': 'bar'}
    )

    # выполним команду разрешения неконсистентностей
    with patch.object(system.plugin.__class__, 'remove_role') as remove_role:
        remove_role.return_value = {"code": 0}
        call_command('idm_check_and_resolve', resolve_only=True)
        remove_role.assert_called_once_with(
            group_id=fellowship.external_id,
            is_fired=True,
            role_data={'role': 'superuser'},
            path='/role/superuser/',
            fields_data={'foo': 'bar'},
            subject_type=None,
            system_specific={'foo': 'bar'},
            request_id=Action.objects.get(action='remote_remove').pk,
            unique_id='',
        )

    assert len(mail.outbox) == 0  # аппрувы рассылаться не должны
    assert Role.objects.count() == 0

    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'

    assert Action.objects.count() == 4
    expected_set = {'started_sync_with_system', 'resolve_inconsistency', 'synced_with_system', 'remote_remove'}
    assert set(Action.objects.values_list('action', flat=True)) == expected_set
    action = Action.objects.get(action='resolve_inconsistency')
    assert action.data == {
        'comment': 'Расхождение разрешено, так как группа удалена',
        'group': 105,
        'resolved_because_group_is_not_active': True,
    }


@pytest.mark.parametrize('inconsistency_policy', [
    SYSTEM_INCONSISTENCY_POLICY.STRICT, SYSTEM_INCONSISTENCY_POLICY.STRICT_SOX
])
def test_inconsistency_on_dismiss_user_without_node(generic_system, arda_users, inconsistency_policy):
    """
    При наличии неконсистентности для уволенного сотрудника,
    пытаемся отозвать его роль из системы без заведения таковой у себя, причём узла такого в системе тоже нет
    """
    frodo = arda_users.frodo
    frodo.is_active = False
    frodo.save()
    generic_system.inconsistency_policy = inconsistency_policy
    generic_system.save()

    all_roles = [{
        'login': 'frodo',
        'roles': [
            [{'role': 'spam_eggs'}, {'foo': 'bar'}],
        ]
    }]

    with mock_all_roles(generic_system, all_roles):
        Inconsistency.objects.check_roles()

    assert Inconsistency.objects.count() == 1
    inconsistency = Inconsistency.objects.select_related('system__actual_workflow').get()
    assert inconsistency.type == Inconsistency.TYPE_UNKNOWN_ROLE

    # выполним команду разрешения неконсистентностей
    with patch.object(generic_system.plugin.__class__, 'remove_role') as remove_role:
        remove_role.return_value = {"code": 0}
        call_command('idm_check_and_resolve', resolve_only=True)
        if inconsistency_policy == SYSTEM_INCONSISTENCY_POLICY.STRICT_SOX:
            remove_role.assert_called_once_with(username='frodo',
                                                is_fired=True,
                                                role_data={'role': 'spam_eggs'},
                                                path=None,
                                                fields_data={'foo': 'bar'},
                                                subject_type=None,
                                                system_specific={'foo': 'bar'},
                                                request_id=Action.objects.get(action='remote_remove').pk,
                                                unique_id='')
        else:
            remove_role.call_args_list == []

    assert Role.objects.count() == 0
    assert len(mail.outbox) == 0

    inconsistency = refresh(inconsistency)
    if inconsistency_policy == SYSTEM_INCONSISTENCY_POLICY.STRICT:
        assert inconsistency.state == 'active'
    else:
        assert inconsistency.state == 'resolved'
        assert {action.action for action in inconsistency.actions.all()} == {'resolve_inconsistency', 'remote_remove'}
        action = inconsistency.actions.get(action='resolve_inconsistency')
        assert action.action == 'resolve_inconsistency'
        assert action.data == {
            'user': 'frodo',
            'resolved_because_role_unknown': True,
            'comment': 'Расхождение разрешено, так как узла не существует в дереве ролей системы',
        }


def test_fired_user_and_inconsistency(generic_system, arda_users):
    """
    Два сотрудника, у каждого по одной разной неконсистентности, уволим обоих,
    посмотрим, что будет с неконсистентностями
    """
    frodo = arda_users.frodo
    legolas = arda_users.legolas

    role = raw_make_role(frodo, generic_system, {'role': 'superuser'}, state='granted')
    make_inconsistency(
        user=frodo,
        type=Inconsistency.TYPE_OUR,
        system=generic_system,
        our_role=role,
    )
    make_inconsistency(
        user=legolas,
        type=Inconsistency.TYPE_THEIR,
        system=generic_system,
        path='/role/superuser/',
    )

    assert frodo.is_active
    assert legolas.is_active
    assert frodo.roles.count() == 1
    assert legolas.roles.count() == 0
    assert RoleRequest.objects.count() == 0

    User.objects.filter(username__in=['frodo', 'legolas']).update(is_active=False)
    frodo = refresh(frodo)
    legolas = refresh(legolas)

    assert not frodo.is_active
    assert not legolas.is_active

    # теперь попытаемся разрешить неконсистентности
    with patch.object(generic_system.plugin.__class__, 'remove_role') as remove_role:
        remove_role.return_value = {"code": 0}
        call_command('idm_check_and_resolve', resolve_only=True)

    assert frodo.roles.count() == 1
    assert legolas.roles.count() == 0
    assert RoleRequest.objects.count() == 0

    role = frodo.roles.get()
    assert role.state == 'deprived'
    assert not role.is_active

    assert frodo.inconsistencies.get().state == 'resolved'
    assert legolas.inconsistencies.get().state == 'resolved'


def test_fired_user_and_deprive_role(generic_system, arda_users):
    """
    Два сотрудника, у каждого по одной разной неконсистентности разного типа, уволим обоих,
    посмотрим, что будет с неконсистентностями, если отозвать роли
    """
    frodo = arda_users.frodo
    legolas = arda_users.legolas

    role = raw_make_role(frodo, generic_system, {'role': 'superuser'}, state='granted')
    inconsistency1 = make_inconsistency(
        user=frodo,
        type=Inconsistency.TYPE_OUR,
        system=generic_system,
        our_role=role,
    )
    inconsistency2 = make_inconsistency(
        user=legolas,
        type=Inconsistency.TYPE_THEIR,
        system=generic_system,
        path='/role/admin/'
    )

    User.objects.filter(username__in=['frodo', 'legolas']).update(is_active=False, ldap_active=False)
    frodo = refresh(frodo)
    legolas = refresh(legolas)

    assert not frodo.is_active
    assert not legolas.is_active

    # теперь попытаемся отозвать роли уволенных сотрудников
    with patch.object(generic_system.plugin.__class__, 'remove_role') as remove_role:
        remove_role.return_value = {"code": 0}
        call_command('idm_deprive_roles')

    assert frodo.roles.count() == 1
    assert legolas.roles.count() == 0

    role = refresh(role)
    assert role.state == 'deprived'
    assert not role.is_active
    assert_action_chain(role, ['deprive', 'first_remove_role_push', 'remove'])

    inconsistency1 = refresh(inconsistency1)
    inconsistency2 = refresh(inconsistency2)
    assert inconsistency1.state == 'active'
    assert inconsistency2.state == 'active'

    # теперь попытаемся разрешить неконсистентности
    with patch.object(generic_system.plugin.__class__, 'remove_role') as remove_role:
        remove_role.return_value = {"code": 0}
        call_command('idm_check_and_resolve', resolve_only=True)

    assert Role.objects.count() == 1
    role = refresh(role)
    assert role.state == 'deprived'
    assert_action_chain(role, ['deprive', 'first_remove_role_push', 'remove'])
    inconsistency1 = refresh(inconsistency1)
    inconsistency2 = refresh(inconsistency2)
    assert inconsistency1.state == 'resolved'
    assert inconsistency2.state == 'resolved'


def test_not_failing_on_cyrillic_logins(simple_system, arda_users):
    """Не нужно падать, если система прислала кириллический логин"""
    set_workflow(simple_system, 'approvers = [approver("frodo")]')

    all_roles = [{
        'login': 'вася',
        'roles': [
            [{'role': 'admin'}, {}]
        ]
    }]
    with mock_all_roles(simple_system, all_roles):
        # Эти команды не должны падать
        Inconsistency.objects.check_roles()
        assert Inconsistency.objects.count() == 1
        inconsistency = Inconsistency.objects.select_related('system__actual_workflow').get()
        assert inconsistency.type == Inconsistency.TYPE_UNKNOWN_USER
        Inconsistency.objects.resolve()
        assert Role.objects.count() == 0


@pytest.mark.parametrize('inconsistency_policy', [
    SYSTEM_INCONSISTENCY_POLICY.STRICT,
    SYSTEM_INCONSISTENCY_POLICY.STRICT_SOX
])
def test_resolve_unknown_role(simple_system, arda_users, inconsistency_policy):
    """Проверим разрешение неконсистентностей про роль, которой нет в дереве ролей"""
    roles = [
        {
            'login': 'frodo',
            'roles': [
                [{'role': 'doge'}, {'login': 'wowwowwow'}]
            ]
        }
    ]
    simple_system.inconsistency_policy = inconsistency_policy
    simple_system.save()

    with mock_all_roles(simple_system, roles):
        Inconsistency.objects.check_roles()
    inconsistency = Inconsistency.objects.select_related('system__actual_workflow').get()
    assert_inconsistency(
        inconsistency,
        type=Inconsistency.TYPE_UNKNOWN_ROLE,
        system=simple_system,
        user=arda_users.frodo,
        remote_data={'role': 'doge'},
        remote_fields={'login': 'wowwowwow'},
    )
    Inconsistency.objects.resolve()
    inconsistency = refresh(inconsistency)
    synced_action = Action.objects.get(action='synced_with_system')
    if inconsistency_policy == SYSTEM_INCONSISTENCY_POLICY.STRICT_SOX:
        assert inconsistency.state == 'resolved'
        assert synced_action.data == {
            'status': 0,
            'report_created': 1,
            'report_created_count': 0,
            'report_existed': 0,
            'report_errors': 0,
            'report_deprived_count': 1,
        }
    else:
        assert inconsistency.state == 'active'
        assert synced_action.data == {
            'status': 0,
            'report_created': 0,
            'report_created_count': 0,
            'report_existed': 0,
            'report_errors': 1,
            'report_deprived_count': 0,
        }


@pytest.mark.parametrize('inconsistency_policy', [
    SYSTEM_INCONSISTENCY_POLICY.STRICT_SOX, SYSTEM_INCONSISTENCY_POLICY.STRICT
])
def test_resolve_unknown_owner(aware_simple_system, arda_users, inconsistency_policy):
    """Проверим разрешение неконсистентности логина"""
    set_workflow(aware_simple_system, 'approvers = [approver("frodo")]')
    aware_simple_system.inconsistency_policy = inconsistency_policy
    aware_simple_system.save()

    user_roles = [{
        'login': 'tylerdurden',
        'roles': [
            [{'role': 'admin'}, {}],
            [{'role': 'fight'}, {'login': 'tyler'}],
        ]
    }]
    group_roles = [{
        'group': 700,
        'roles': [
            [{'role': 'manager'}, {}],
            [{'role': 'mayhem'}, {'login': 'joe'}],
        ]
    }]
    with mock_all_roles(aware_simple_system, user_roles=user_roles, group_roles=group_roles):
        Inconsistency.objects.check_roles()
        types = set(Inconsistency.objects.values_list('type', flat=True))
        assert set(types) == {Inconsistency.TYPE_UNKNOWN_USER, Inconsistency.TYPE_UNKNOWN_GROUP}
        if inconsistency_policy == SYSTEM_INCONSISTENCY_POLICY.STRICT:
            assert Inconsistency.objects.count() == 2
        else:
            assert Inconsistency.objects.count() == 4

        Inconsistency.objects.resolve()

        if inconsistency_policy == SYSTEM_INCONSISTENCY_POLICY.STRICT:
            assert Inconsistency.objects.active().count() == 2
        else:
            # в случае strict мы всё должны разрешить
            assert Inconsistency.objects.active().count() == 0
            synced_action = Action.objects.get(action='synced_with_system')
            assert synced_action.data == {
                'status': 0,
                'report_created': 4,
                'report_created_count': 0,
                'report_existed': 0,
                'report_errors': 0,
                'report_deprived_count': 4,
            }
            inc = Inconsistency.objects.get(remote_fields={'login': 'joe'})
            resolve_action = inc.actions.get(action='resolve_inconsistency')
            assert resolve_action.data == {
                'comment': 'Расхождение разрешено, так как узла не существует',
                'resolved_because_role_unknown': True,
                'group': 700,
            }


def test_dont_deprive_rerequested_role_that_was_initially_through_resolving_inconsistency(simple_system, arda_users,
                                                                                          superuser_gandalf):
    """Проверим, что если роль была изначально выдана через неконсистентность, а потом перезапрошена,
    то мы не отзываем такую роль при разрешении неконсистентностей"""

    set_workflow(simple_system, 'approvers = ["legolas"]')
    frodo = arda_users.frodo
    gandalf = superuser_gandalf
    all_roles = [{
        'login': 'frodo',
        'roles': [{'role': 'manager'}]
    }]

    with mock_all_roles(simple_system, all_roles):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 1
    inconsistency = Inconsistency.objects.select_related('system__actual_workflow').get()
    inconsistency.resolve(requester=gandalf, force=True)
    assert Role.objects.count() == 1
    role = Role.objects.select_related('system__actual_workflow', 'node').get()
    assert role.user_id == frodo.id
    role.set_state('need_request')
    role.rerequest(frodo)
    assert role.state == 'rerequested'
    assert_action_chain(role, ['import', 'approve', 'grant', 'resolve_inconsistency', 'ask_rerequest', 'rerequest', 'apply_workflow'])
    with mock_all_roles(simple_system, all_roles):
        Inconsistency.objects.check_roles()
    role = refresh(role)
    assert role.state == 'rerequested'
    assert_action_chain(role, ['import', 'approve', 'grant', 'resolve_inconsistency', 'ask_rerequest', 'rerequest', 'apply_workflow'])


def test_inconsistency_policy(simple_system, arda_users):
    """Проверим, что если политика системы в отношении неконсистентностей - trust, то мы автоматически доверяем ей"""

    set_workflow(simple_system, 'approvers = ["legolas"]')
    simple_system.inconsistency_policy = 'trust'
    simple_system.save()
    roles = [{
        'login': 'frodo',
        'roles': [
            [{'role': 'manager'}, {'passport-login': 'yndx-%s' % login}] for login in list(arda_users.keys())
        ]
    }]
    with mock_all_roles(simple_system, roles):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == len(arda_users)

    Inconsistency.objects.resolve()
    assert Role.objects.count() == len(arda_users)
    role = Role.objects.order_by('?').first()
    assert role.state == 'granted'
    assert role.inconsistency is None  # не сохраняем ссылку на неконсистентность, так как её всё равно удалим
    assert ApproveRequest.objects.count() == 0
    assert_action_chain(role, ['import', 'approve', 'grant', 'resolve_inconsistency'])


def test_invisible_role_is_autotrusted(complex_system, arda_users):
    """Проверим, что мы автоматически доверяем неконсистентностям про роли на невидимые узлы"""

    set_workflow(complex_system, 'approvers = ["legolas"]')
    assert complex_system.inconsistency_policy == SYSTEM_INCONSISTENCY_POLICY.STRICT
    roles = [{
        'login': 'frodo',
        'roles': [
            [{'project': 'rules', 'role': 'auditor'}, {}],
            [{'project': 'rules', 'role': 'invisic'}, {}],
        ]
    }]
    with mock_all_roles(complex_system, roles):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 2
    Inconsistency.objects.resolve()
    assert Role.objects.count() == 2
    auditor_role = Role.objects.get(node__slug='auditor')
    invisible_role = Role.objects.get(node__slug='invisic')
    assert auditor_role.state == 'imported'
    assert invisible_role.state == 'granted'
    assert_action_chain(auditor_role, ['import', 'apply_workflow', 'resolve_inconsistency'])
    assert_action_chain(invisible_role, ['import', 'approve', 'grant', 'resolve_inconsistency'])


@pytest.mark.parametrize('group_names', [['fellowship'], ['fellowship', 'shire']])
@pytest.mark.parametrize('add_user_role', [False, True])
def test_personal_roles_are_resolved_by_pushing_once(generic_system, arda_users, department_structure,
                                                     group_names, add_user_role):
    """Проверим, что если в системе нет персональной роли, то при разрешении такой неконсистентности мы не отзовём роль,
    выданную по групповой, а напротив, добавим такую роль в систему
    """
    groups = [getattr(department_structure, name) for name in group_names]
    total_count = 0
    with capture_http(generic_system, {'code': 0, 'data': {}}):
        for group in groups:
            make_role(group, generic_system, {'role': 'admin'})
            total_count += group.members.count() + 1
        if add_user_role:
            make_role(arda_users.frodo, generic_system, {'role': 'admin'})
            total_count += 1
    assert Role.objects.filter(state='granted').count() == total_count
    members = set()
    for group in groups:
        members |= set(group.members.all())
    roles = [
        {
            'login': user.username,
            'roles': [
                {'role': 'admin'}
            ]
        } for user in members if user.username != 'frodo'
    ]
    with mock_all_roles(generic_system, roles):
        Inconsistency.objects.check_roles()
    assert Inconsistency.objects.count() == 1
    assert Inconsistency.objects.active().our().count() == 1
    with capture_http(generic_system, {'code': 0, 'data': {}}) as sender:
        Inconsistency.objects.resolve()

    # неконсистентности разрешаются в нашу пользу
    assert Role.objects.filter(state='granted').count() == total_count
    frodo_roles = Role.objects.filter(user=arda_users.frodo)
    assert len(frodo_roles) == len(group_names) + int(add_user_role)
    processed_role = Inconsistency.objects.our()[0].our_role
    for role in frodo_roles:
        if role == processed_role:
            assert_action_chain(
                role, ['request', 'approve', 'first_add_role_push', 'grant', 'approve', 'first_add_role_push', 'grant']
            )
        else:
            assert_action_chain(role, ['request', 'approve', 'grant'])
    assert processed_role.parent_id is not None
    second_approve = processed_role.actions.filter(action='approve').order_by('-pk').first()
    assert second_approve.comment == 'Роль повторно добавлена в систему при разрешении расхождения'
    assert_http(sender.http_post, url='http://example.com/add-role/', data={
        'fields': 'null',
        'login': 'frodo',
        'role': '{"role": "admin"}',
        'path': '/role/admin/',
    })


@pytest.mark.parametrize('policy', [SYSTEM_INCONSISTENCY_POLICY.TRUST_IDM, SYSTEM_INCONSISTENCY_POLICY.STRICT])
def test_referenced_depriving_roles_are_resolved_as_usual(simple_system, generic_system, arda_users, policy):
    """Проверим, что если в системе нет связанной роли, а роль с нашей стороны находится в depriving,
    то при разрешении такой неконсистентности мы переведём роль в статус deprived"""

    simple_system.inconsistency_policy = policy
    simple_system.save(update_fields=['inconsistency_policy'])

    frodo = arda_users.frodo

    workflow = dedent("""
    approvers = []
    if role.get('role') == 'admin':
        ref_roles = [{
            'system': '%s',
            'role_data': {
                'role': 'manager'
            },
            'role_fields': {
                'login': scope
            }
        }]
    """ % generic_system.slug)
    set_workflow(simple_system, workflow, workflow)

    with capture_http(generic_system, {'code': 0, 'data': {}}):
        role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    role = refresh(role)
    assert role.state == 'granted'
    ref = role.refs.get()
    assert ref.state == 'granted'

    # теперь попробуем удалить узел, на который выдана роль. система при этом отвечает ошибкой
    tree = simple_system.plugin.get_info()
    del tree['roles']['values']['manager']

    with days_from_now(-30):
        with mock_tree(generic_system, tree):
            with capture_http(generic_system, {'code': 1, 'fatal': 'error'}):
                sync_role_nodes(generic_system)
    # Доудаляем узлы
    with mock_tree(generic_system, tree):
        with capture_http(generic_system, {'code': 1, 'fatal': 'error'}):
            sync_role_nodes(generic_system)

    ref = refresh(ref)
    assert_action_chain(ref, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant', 'deprive', 'first_remove_role_push',
    ])
    assert ref.state == 'depriving'

    # теперь система не отдаёт эти роли при сверке
    with mock_all_roles(generic_system, []):
        with capture_http(generic_system, {'code': 0, 'data': {}}) as sender:
            Inconsistency.objects.check_and_resolve(generic_system)

    ref = refresh(ref)
    assert ref.state == 'deprived'
    assert_action_chain(
        ref,
        [
            'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant', 'deprive',
            'first_remove_role_push', 'remove', 'resolve_inconsistency',
        ]
    )
    remove_action = ref.actions.get(action='remove')
    assert remove_action.comment == 'Роль удалена из системы.: Роль удалена при разрешении расхождения'


def test_import_role_with_no_approvers_defined(simple_system, arda_users, idm_robot, ad_system):
    """Проверим, что можно импортировать даже роль без определённых в workflow подтверждающих.
    При этом другие параметры применятся"""

    set_workflow(simple_system, 'ad_groups = ["OU=group1"]; no_email=True')
    roles = [{
        'login': 'frodo',
        'roles': [
            [{'role': 'manager'}, {}],
        ]
    }]

    tree = ad_system.plugin.get_info()
    with mock_tree(ad_system, tree):
        sync_role_nodes(ad_system)

    with mock_all_roles(simple_system, roles):
        Inconsistency.objects.check_roles()
    inconsistency = Inconsistency.objects.select_related('system__actual_workflow').get()
    inconsistency.resolve(force=True)
    assert Role.objects.count() == 2
    role = list(Role.objects.filter())[1]
    assert role.state == 'granted'
    assert_action_chain(role, ['import', 'approve', 'grant', 'resolve_inconsistency'])
    assert role.ad_groups == []
    assert role.ref_roles == [{
        'system': 'ad_system',
        'role_data': {
            'type': 'roles_in_groups',
            'ad_group': 'OU=group1',
            'group_roles': 'member',
        }
    }]
    assert role.no_email is True


def test_role_by_inconsistency_is_not_pushed_back(generic_system, arda_users):
    """ Проверим, что роль, заведённая по неконсистентности, не отправляется в систему при подтверждении"""

    set_workflow(generic_system, 'approvers = ["legolas"]')
    roles = [{
        'login': 'frodo',
        'roles': [
            [{'role': 'manager'}, {}],
        ]
    }]
    with mock_all_roles(generic_system, roles):
        Inconsistency.objects.check_roles()
    inconsistency = Inconsistency.objects.select_related('system__actual_workflow').get()
    inconsistency.resolve()
    role = Role.objects.get()
    assert role.state == 'imported'
    assert role.inconsistency == inconsistency
    with patch.object(generic_system.plugin.__class__, '_send_data') as send_data:
        send_data.side_effect = ValueError
        approve_request = role.get_open_request().approves.get().requests.select_related_for_set_decided().get()
        approve_request.set_approved(arda_users.legolas)
    role = refresh(role)
    assert role.state == 'granted'
    assert_action_chain(role, ['import', 'apply_workflow', 'resolve_inconsistency', 'approve', 'grant'])


def test_resolve_inconsistency_when_role_disappear_from_system(simple_system, arda_users):
    """
    Роль была в выдаче системы, мы завели неконсистентность, запросили роль,
    роль пропала из выдачи системы, если роль в imported - надо удалить и закрыть неконсистентность"""
    set_workflow(simple_system, 'approvers = [approver("legolas")]')
    frodo = arda_users.frodo

    all_roles = [{
        'login': 'frodo',
        'roles': [
            [{'role': 'admin'}, {}]
        ]
    }]

    with mock_all_roles(simple_system, all_roles):
        Inconsistency.objects.check_roles()

    # должна создаться неконсистентность
    assert Inconsistency.objects.count() == 1
    inconsistency = Inconsistency.objects.select_related('system__actual_workflow').get()
    assert_inconsistency(
        inconsistency,
        system=simple_system,
        user=frodo,
        path='/role/admin/',
    )

    # выполнить команду resolve_inconsistencies
    call_command('idm_check_and_resolve', resolve_only=True)

    # должна появиться роль
    assert Role.objects.count() == 1
    role = Role.objects.get()
    assert role.user_id == frodo.id
    assert role.state == 'imported'
    assert role.is_active is True
    role.fetch_node()
    assert role.node.data == {'role': 'admin'}

    assert role.requests.count() == 1  # завелся запрос c одним аппрувом
    request = role.requests.get()
    assert request.approves.count() == 1
    assert request.is_done is False

    # теперь роль пропадает из выдачи системы
    with mock_all_roles(simple_system, []):
        Inconsistency.objects.check_roles()

    # неконсистентность должна закрыться
    assert Inconsistency.objects.count() == 2
    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'
    assert Role.objects.count() == 1
    role = refresh(role)
    assert role.state == 'imported'

    # теперь разрешим вторую неконсистентность, роль отзовётся
    call_command('idm_check_and_resolve', resolve_only=True)
    role = refresh(role)
    assert role.state == 'deprived'
    assert_action_chain(role, ['import', 'apply_workflow', 'resolve_inconsistency', 'remove', 'resolve_inconsistency'])


def test_our_side_inconsistency_if_role_is_deprived(simple_system, arda_users):
    """Если роль, на которую заведена неконсистентность, отозвана, то разрешение неконсистентности
    не должно приводить к повторному отзыву"""

    frodo = arda_users.frodo

    role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='granted')
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_OUR,
        system=simple_system,
        user=frodo,
        our_role=role,
    )

    role.deprive_or_decline(frodo)
    role = refresh(role)
    assert role.state == 'deprived'

    inconsistency.resolve()
    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'
    assert inconsistency.actions.count() == 1
    action = inconsistency.actions.get()
    assert action.action == 'resolve_inconsistency'
    assert action.inconsistency_id == inconsistency.id
    assert action.data == {
        'comment': 'Расхождение закрыто, так как аналогичная роль отозвана',
        'user': 'frodo',
    }
    assert Role.objects.count() == 1  # роли не создалось


def test_expire_requested_role_on_inconsistency_resolution(generic_system, arda_users):
    """При разрешении неконсистентности запросы на такую же роль должны истекать."""

    # TODO: Здесь есть одна проблема: из неконсистентностей мы знаем только system_specific,
    # а из роли только fields_data, потому что она запрошена, но ещё не выдана.
    # Соответственно, здесь нельзя точно установить соответствие между запрошенными ролями и существующими
    # неконсистентностями. Сейчас это делается через копирование в fields_data подмножества system_specific,
    # но в будущем нужно решить это лучше.

    frodo = arda_users.frodo
    set_workflow(generic_system, 'approvers = ["legolas"]')
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        system=generic_system,
        user=frodo,
        path='/role/admin/',
        remote_fields={'passport-login': 'yndx-frodo'}
    )
    role = Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'admin'},
                                     {'passport-login': 'yndx-frodo'})

    role = refresh(role)
    assert role.fields_data == {'passport-login': 'yndx-frodo'}
    assert role.system_specific is None

    inconsistency = Inconsistency.objects.select_related('system__actual_workflow').get(pk=inconsistency.pk)
    inconsistency.resolve()
    assert inconsistency.role is not None
    new_role = inconsistency.role
    assert new_role.state == 'imported'
    role = refresh(role)
    assert role.state == 'expired'
    assert_action_chain(role, ['request', 'apply_workflow', 'expire'])


def test_make_inconsistency_obsolete_on_role_grant(generic_system, arda_users):
    """При выдаче роли неконсистентности про неё (type=their) помечаются obsolete"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas

    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        system=generic_system,
        user=frodo,
        path='/role/admin/',
        remote_fields={'passport-login': 'yndx-frodo'},
    )
    # не совпадают поля, закрыться не должна
    inconsistency1 = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        system=generic_system,
        user=frodo,
        path='/role/admin/',
    )

    set_workflow(generic_system, 'approvers = ["legolas"]')
    role = Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'admin'}, None)

    # запрос не закрывает неконсистентность
    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'active'

    # а выдача закрывает
    with patch.object(generic_system.plugin.__class__, '_send_data') as send_data:
        send_data.return_value = {
            'code': 0,
            'data': {'passport-login': 'yndx-frodo'}
        }
        approve_request = role.get_open_request().approves.get().requests.select_related_for_set_decided().get()
        approve_request.set_approved(legolas)
    role = refresh(role)
    assert role.state == 'granted'
    assert role.system_specific == {'passport-login': 'yndx-frodo'}
    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'obsolete'

    # но только точно совпадающую, inconsistency1 не закрылась
    inconsistency1 = refresh(inconsistency1)
    assert inconsistency1.state == 'active'


def test_mark_inconsistency_obsolete_on_role_deprive(simple_system, arda_users):
    """При отзыве роли неконсистентность про неё (type=their) должна переходить в obsolete.
    Но вообще этого не должно случаться, неконсистентность ещё на этапе"""

    frodo = arda_users.frodo
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        system=simple_system,
        user=frodo,
        path='/role/admin/',
        remote_fields={'login': 'hello'},
    )
    inconsistency1 = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        system=simple_system,
        user=frodo,
        path='/role/admin/',
    )

    role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='granted', system_specific={'login': 'hello'})
    role.deprive_or_decline(frodo)
    role = refresh(role)
    assert role.state == 'deprived'

    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'obsolete'
    assert Role.objects.count() == 1  # роли не создалось
    # но только точно совпадающую, inconsistency1 не закрылась
    inconsistency1 = refresh(inconsistency1)
    assert inconsistency1.state == 'active'


def test_accept_inconsistency_from_their_side_with_passport_login_occupied_by_another_user(simple_system, arda_users):
    """ У нас роли нет, в системе есть, но паспортный логин принадлежит другому пользователю."""

    frodo = arda_users.frodo
    gandalf = arda_users.gandalf

    UserPassportLogin.objects.create(user=gandalf, login='yndx-gandalf')

    # На чужой паспортный логин роль не выдадим
    inconsistency = make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=frodo,
        system=simple_system,
        path='/role/admin/',
        remote_fields={'passport-login': 'yndx-gandalf'}
    )
    Inconsistency.objects.resolve_system(simple_system)
    assert inconsistency.state == 'active'
    assert frodo.roles.count() == 0
    action = Action.objects.get(action='synced_with_system')
    assert action.data == {
        'status': 0,
        'report_errors': 1,
        'report_created_count': 0,
        'report_existed': 0,
        'report_deprived_count': 0,
        'report_created': 0,
    }


@pytest.mark.parametrize('real_user', [True, False])
@pytest.mark.parametrize('subject_type', ['user', 'tvm_app'])
@pytest.mark.parametrize('supports_types', [True, False])
def test_system_use_tvm_roles(simple_system, arda_users, real_user, subject_type, supports_types):
    """ При сверке неконсистентностей мы сохраняем subject_type, полученный от системы.
    В случае неконсистентности на стороне системы, мы добавляем его в пуш на удаление роли.
    """
    simple_system.use_tvm_role = supports_types
    simple_system.inconsistency_policy = 'donottrust_strict'
    simple_system.save()
    all_roles = [{
        'login': 'frodo' if real_user else 'harry_potter',
        'subject_type': subject_type,
        'roles': [
            [
                {'role': 'admin'},
            ]
        ]
    }]
    with mock_all_roles(simple_system, all_roles):
        Inconsistency.objects.check_roles()
    inc = Inconsistency.objects.get()
    if supports_types:
        assert inc.remote_subject_type == subject_type
    expected_args = {
        'fields_data': None,
        'is_fired': False,
        'path': '/role/admin/',
        'role_data': {'role': 'admin'},
        'subject_type': (supports_types or None) and subject_type,
        'system_specific': None,
        'username': all_roles[0]['login'],
        'request_id': Action.objects.order_by('added').last().pk + 1,
        'unique_id': '',
    }
    with patch.object(simple_system.plugin.__class__, 'remove_role') as remove_role:
        inc.resolve()
        remove_role.assert_called_once_with(**expected_args)


def test_resolve_our_side_inconsistency_in_idm_direct(generic_system, arda_users, superuser_gandalf):
    """У нас роль есть, в системе нет. Надо запушить роль в систему."""

    gandalf = superuser_gandalf
    generic_system.inconsistency_policy = SYSTEM_INCONSISTENCY_POLICY.TRUST_IDM
    generic_system.save(update_fields=['inconsistency_policy'])
    role = raw_make_role(arda_users.frodo, generic_system, {'role': 'admin'}, state='granted')
    with mock_all_roles(generic_system, []):
        Inconsistency.objects.check_roles()
    inconsistency = Inconsistency.objects.select_related('our_role__system').get()
    assert_inconsistency(inconsistency, system=generic_system, type=Inconsistency.TYPE_OUR,
                         user=arda_users.frodo, our_role=role, state='active')

    with capture_http(generic_system, {'code': 0, 'data': {}}) as mocked:
        inconsistency.resolve(requester=gandalf, force=True)
    data = {
        'login': 'frodo',
        'role': '{"role": "admin"}',
        'fields': 'null',
        'path': '/role/admin/'
    }
    assert_http(mocked.http_post, url='http://example.com/add-role/', data=data)

    role = refresh(role)
    assert role.is_active
    assert role.state == 'granted'

    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'
    assert gandalf.requested.count() == 1
    action = gandalf.requested.get()
    assert action.data == {
        'is_forcibly_resolved': True,
        'user': 'frodo',
        'comment': 'Разрешено расхождение',
    }
    assert action.inconsistency_id == inconsistency.id


def test_resolve_our_side_group_inconsistency_in_idm_direct(generic_system, arda_users, department_structure,
                                                            superuser_gandalf):
    """У нас роль есть, в системе нет. Надо запушить роль в систему."""

    fellowship = department_structure.fellowship
    system = generic_system
    gandalf = superuser_gandalf
    generic_system.inconsistency_policy = SYSTEM_INCONSISTENCY_POLICY.TRUST_IDM
    generic_system.group_policy = SYSTEM_GROUP_POLICY.AWARE
    generic_system.save(update_fields=['inconsistency_policy', 'group_policy'])
    role = raw_make_role(fellowship, system, {'role': 'admin'}, state='granted')

    with mock_all_roles(generic_system, []):
        Inconsistency.objects.check_roles()
    inconsistency = Inconsistency.objects.select_related('our_role__system').get()
    assert_inconsistency(inconsistency, system=generic_system, type=Inconsistency.TYPE_OUR,
                         group=fellowship, our_role=role, state='active')

    assert inconsistency.state == 'active'
    assert role.is_active
    with capture_http(generic_system, {'code': 0, 'data': {}}) as mocked:
        inconsistency.resolve(requester=gandalf, force=True)
    data = {
        'group': fellowship.external_id,
        'role': '{"role": "admin"}',
        'fields': 'null',
        'path': '/role/admin/'
    }
    assert_http(mocked.http_post, url='http://example.com/add-role/', data=data)
    role = refresh(role)
    assert role.is_active
    assert role.state == 'granted'
    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'
    assert gandalf.requested.count() == 1
    action = gandalf.requested.get()
    assert action.data == {
        'is_forcibly_resolved': True,
        'group': fellowship.external_id,
        'comment': 'Разрешено расхождение',
    }
    assert action.inconsistency_id == inconsistency.id


def test_resolve_inconsistency_from_their_side_in_idm_direct(generic_system, arda_users, superuser_gandalf):
    """ У нас роли нет, а в системе есть. Надо отозвать роль в системе"""

    frodo = arda_users.frodo
    gandalf = superuser_gandalf
    generic_system.inconsistency_policy = SYSTEM_INCONSISTENCY_POLICY.TRUST_IDM
    generic_system.save(update_fields=['inconsistency_policy'])
    all_roles = [{
        'login': 'frodo',
        'roles': [
            [{'role': 'admin'}],
        ]
    }]
    with mock_all_roles(generic_system, all_roles):
        Inconsistency.objects.check_roles()

    inconsistency = Inconsistency.objects.select_related('system__actual_workflow').get()
    assert inconsistency.state == 'active'

    with capture_http(generic_system, {'code': 0, 'data': {}}) as mocked:
        inconsistency.resolve(requester=gandalf, force=True)
    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'
    assert frodo.roles.count() == 0

    data = {
        'login': 'frodo',
        'role': '{"role": "admin"}',
        'fields': 'null',
        'path': '/role/admin/'
    }
    assert_http(mocked.http_post, url='http://example.com/remove-role/', data=data)

    assert gandalf.requested.count() == 1
    action = gandalf.requested.get()
    assert action.data == {
        'is_forcibly_resolved': True,
        'comment': 'Расхождение разрешено без импорта роли, так как этого требует политика системы',
        'resolved_because_role_unknown': True,
        'resolved_because_of_policy': True,
    }
    assert action.inconsistency_id == inconsistency.id


def test_resolve_group_inconsistency_from_their_side_in_idm_direct(generic_system, arda_users, department_structure,
                                                                   superuser_gandalf):
    """ У нас групповой роли нет, а в системе есть. Надо отозвать роль в системе."""

    fellowship = department_structure.fellowship
    gandalf = superuser_gandalf
    generic_system.inconsistency_policy = SYSTEM_INCONSISTENCY_POLICY.TRUST_IDM
    generic_system.group_policy = SYSTEM_GROUP_POLICY.AWARE
    generic_system.save(update_fields=['inconsistency_policy', 'group_policy'])

    all_roles = [{
        'group': fellowship.external_id,
        'roles': [
            [{'role': 'admin'}],
        ]
    }]
    with mock_all_roles(generic_system, user_roles=[], group_roles=all_roles):
        Inconsistency.objects.check_roles()
    inconsistency = Inconsistency.objects.select_related('system__actual_workflow').get()
    assert inconsistency.state == 'active'

    with capture_http(generic_system, {'code': 0, 'data': {}}) as mocked:
        inconsistency.resolve(requester=gandalf, force=True)
    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'
    assert fellowship.roles.count() == 0

    data = {
        'group': fellowship.external_id,
        'role': '{"role": "admin"}',
        'fields': 'null',
        'path': '/role/admin/'
    }
    assert_http(mocked.http_post, url='http://example.com/remove-role/', data=data)

    assert gandalf.requested.count() == 1
    action = gandalf.requested.get()
    assert action.data == {
        'is_forcibly_resolved': True,
        'comment': 'Расхождение разрешено без импорта роли, так как этого требует политика системы',
        'resolved_because_role_unknown': True,
        'resolved_because_of_policy': True,
    }
    assert action.inconsistency_id == inconsistency.id


@pytest.mark.parametrize('inconsistencies_for_break,can_be_broken,is_broken,inconsistencies_state', [
    (None, True, False, 'resolved'),
    (0, True, True, 'active'),
    (0, False, False, 'active'),
    (10, True, False, 'resolved'),
])
def test_inconsistencies_for_break(generic_system, arda_users,
                                   inconsistencies_for_break, can_be_broken, is_broken, inconsistencies_state):
    """Проверим, что если в системе нет связанной роли, а роль с нашей стороны находится в depriving,
    то при разрешении такой неконсистентности мы переведём роль в статус deprived"""

    generic_system.inconsistencies_for_break = inconsistencies_for_break
    generic_system.can_be_broken = can_be_broken
    generic_system.save(update_fields=['inconsistencies_for_break', 'can_be_broken'])

    frodo = arda_users.frodo
    # Создадим 4 роли для frodo, которые попадут в расхождения
    for i in range(4):
        raw_make_role(
            frodo,
            generic_system,
            {'role': 'manager'},
            state='granted',
            fields_data={'passport-login': '%s-yndx-frodo' % i},
            system_specific={'passport-login': '%s-yndx-frodo' % i},
        )

    assert Role.objects.count() == 4
    assert Role.objects.filter(state='granted').count() == 4

    # система не отдаёт эти роли при сверке
    with mock_all_roles(generic_system, []):
        with capture_http(generic_system, {'code': 0, 'data': {}}) as sender:
            Inconsistency.objects.check_and_resolve(generic_system)

    # Будет создано 4 расхождения
    # Если inconsistencies_for_break < 5, то расхождения разрешаться не будут, систему пометим сломанной
    # Иначе разрешим расхождения в нужную сторону
    assert generic_system.is_broken == is_broken
    assert set(Inconsistency.objects.values_list('state', flat=True)) == {inconsistencies_state}
