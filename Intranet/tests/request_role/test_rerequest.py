# coding: utf-8


from textwrap import dedent

import pytest
from django.core import mail
from django.core.management import call_command
from django.utils import timezone
from freezegun import freeze_time

from idm.core.constants.role import ROLE_STATE
from idm.core.workflow.exceptions import Forbidden, RoleAlreadyExistsError, DataValidationError
from mock import patch

from idm.core.models import Role, RoleRequest, ApproveRequest
from idm.inconsistencies.models import Inconsistency
from idm.tests.utils import (set_workflow, refresh, assert_action_chain, crash_system, raw_make_role,
                             mock_all_roles, add_perms_by_role, clear_mailbox, assert_contains, expire_role,
                             make_inconsistency, mock_tree, sync_role_nodes)
from idm.users.models import Group


# разрешаем использование базы в тестах
pytestmark = pytest.mark.django_db


@pytest.mark.robot
def test_no_email_is_sent_if_imported_role_is_rerequested_by_owner(simple_system, arda_users):
    """Проверим, что если роль импортирована из системы, то при её перезапросе мы отправим письмо про перезапрос,
    а не пугающее письмо, что роль была получена в обход IDM"""

    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = ["legolas"]')
    with mock_all_roles(simple_system, [{'login': 'frodo', 'roles': [{'role': 'manager'}]}]):
        Inconsistency.objects.check_roles()
    Inconsistency.objects.resolve(simple_system.slug, force=True)
    assert Role.objects.count() == 1

    role = Role.objects.select_related('node', 'system__actual_workflow').get()
    assert role.state == 'granted'
    assert role.system_id == simple_system.id
    assert role.user_id == frodo.id
    assert role.requests.count() == 0
    assert len(mail.outbox) == 0

    role.set_state('need_request')
    assert len(mail.outbox) == 0

    role.rerequest(frodo)
    assert len(mail.outbox) == 0


@pytest.mark.robot
def test_no_email_is_sent_if_imported_role_is_rerequested_by_other(simple_system, arda_users):
    """Проверим, что если роль импортирована из системы, то при её перезапросе мы отправим письмо про перезапрос,
    а не пугающее письмо, что роль была получена в обход IDM"""

    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    add_perms_by_role('superuser', gandalf)
    set_workflow(simple_system, 'approvers = ["legolas"]')
    with mock_all_roles(simple_system, [{'login': 'frodo', 'roles': [{'role': 'manager'}]}]):
        Inconsistency.objects.check_roles()
    Inconsistency.objects.resolve(simple_system.slug, force=True)
    assert Role.objects.count() == 1

    role = Role.objects.select_related('node', 'system__actual_workflow').get()
    assert role.state == 'granted'
    assert role.system_id == simple_system.id
    assert role.user_id == frodo.id
    assert role.requests.count() == 0
    assert len(mail.outbox) == 0

    role.set_state('need_request')
    assert len(mail.outbox) == 0

    role.rerequest(gandalf)
    assert len(mail.outbox) == 0


def test_switch_state_rerequested_to_depriving_through_expire(simple_system, arda_users):
    """Проверяем переключение состояния роли из rerequested в depriving в случае истечения даты протухания"""

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)

    assert role.state == 'granted'
    assert role.requests.count() == 1
    assert role.requests.get().approves.count() == 0

    role.set_state('need_request')
    assert role.state == 'need_request'

    set_workflow(simple_system, 'approvers = ["gandalf"]')
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)
    role = refresh(role)
    assert role.state == 'rerequested'
    assert role.expire_at is not None

    assert role.requests.count() == 2

    role.set_state('depriving', transition='expire')
    role = refresh(role)
    assert role.state == 'deprived'
    assert role.expire_at is None
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        'ask_rerequest', 'rerequest', 'apply_workflow', 'expire', 'first_remove_role_push', 'remove',
    ])


def test_switch_state_rerequested_to_depriving_through_expire_explicitly(simple_system, arda_users):
    """Проверяем переключение состояния роли из rerequested в depriving в случае истечения
    даты протухания через expire"""
    frodo = arda_users.frodo
    legolas = arda_users.legolas

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)

    assert role.state == 'granted'
    assert role.requests.count() == 1
    assert role.requests.get().approves.count() == 0

    role.set_state('need_request')
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'need_request'

    # переводим роль в состояние rerequested
    set_workflow(simple_system, 'approvers = [approver("legolas")]')

    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)

    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'rerequested'
    assert role.expire_at is not None

    assert role.requests.count() == 2  # rerequest роли породил новый запрос роли
    role_request = role.get_last_request()
    assert role_request.approves.count() == 1
    assert role_request.approves.get().requests.get().approver_id == legolas.id
    assert not role_request.is_done

    # проверим корректность подстановки action для запроса роли
    action = role_request.actions.get()
    assert role.actions.exclude(action='apply_workflow').order_by('-pk').first() == action
    assert action.action == 'rerequest'
    assert action.user_id == frodo.id

    role.set_state('depriving', transition='expire')
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'deprived'
    assert role.expire_at is None
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        'ask_rerequest', 'rerequest', 'apply_workflow', 'expire', 'first_remove_role_push', 'remove',
    ])

    # теперь запрос закроется
    role_request = refresh(role_request)
    assert role_request.is_done


def test_switch_state_rerequested_to_depriving_through_deprive(simple_system, arda_users):
    """Проверяем переключение состояния роли из rerequested в depriving в случае явного отзыва роли"""
    frodo = arda_users.frodo

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)

    assert role.state == 'granted'
    assert role.requests.count() == 1  # запрос уже отработан, тк нет подтверждающих

    role.set_state('need_request')
    role = refresh(role)
    assert role.state == 'need_request'

    # переводим роль в состояние rerequested
    set_workflow(simple_system, 'approvers = [approver("legolas")]')
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)

    role = refresh(role)
    assert role.state == 'rerequested'
    assert role.expire_at is not None

    assert role.requests.count() == 2  # rerequest роли породил запрос
    role_request = role.get_last_request()
    assert role_request.approves.count() == 1
    assert role_request.approves.get().requests.get().approver_id == arda_users.legolas.id
    assert not role_request.is_done

    action = role_request.actions.get()
    # проверим корректность подстановки action для запроса роли
    assert role.actions.order_by('-added', '-pk').exclude(action='apply_workflow').first() == action
    assert action.action == 'rerequest'
    assert action.user_id == frodo.id

    role.set_state('depriving', transition='deprive')
    role = refresh(role)
    assert role.state == 'deprived'
    assert role.expire_at is None
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        'ask_rerequest', 'rerequest', 'apply_workflow', 'deprive', 'first_remove_role_push', 'remove',
    ])

    # теперь запрос закроется
    role_request = refresh(role_request)
    assert role_request.is_done


def test_switch_state_requested_to_expired(simple_system, users_for_test):
    """Проверяем переключение состояния роли из requested в expired в случае истечения срока её подтверждения
    TestpalmID: 3456788-112
    """
    set_workflow(simple_system, 'approvers = [approver("art")]')

    (art, fantom, terran, admin) = users_for_test

    role = Role.objects.request_role(admin, terran, simple_system, '', {'role': 'admin'}, None)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)

    assert role.requests.count() == 1  # запрос создан, но не отработал
    assert role.get_last_request().approves.count() == 1
    assert not role.get_last_request().is_done

    assert role.state == 'requested'
    expire_role(role, 1)
    assert len(mail.outbox) == 2

    clear_mailbox()
    simple_system.deprive_expired_roles()

    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'expired'
    assert role.expire_at is None
    assert_action_chain(role, ['request', 'apply_workflow', 'expire'])

    assert role.requests.count() == 1  # запрос отработал на expire роли
    with pytest.raises(RoleRequest.DoesNotExist):
        role.get_open_request()
    assert len(mail.outbox) == 1
    message, = mail.outbox
    assert message.to == ['admin@example.yandex.ru']
    assert message.subject == 'Запрошенная вами роль в системе "Simple система" не получила одобрения в срок'
    assert message.cc == []
    assert_contains((
        'Запрошенная вами для Легат Аврелий (terran) роль в системе "Simple система" не получила подтверждения в срок:',
        'Роль: Админ'
    ), message.body)

def test_switch_state_need_request_to_expire(simple_system, arda_users):
    """
    Проверяем случай, когда роль перешла в статус "нужно запросить", но не была перезапрошена вовремя
    TestpalmID: 3456788-113
    """

    # выдаём роль пользователю
    frodo = arda_users.frodo
    role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state=ROLE_STATE.GRANTED)

    # просим перезапросить
    role.ask_rerequest(transfer=None)
    role = refresh(role)
    assert role.state == ROLE_STATE.NEED_REQUEST
    assert role.expire_at is not None
    clear_mailbox()

    # запускаем отзыв просроченных ролей
    expire_time = role.expire_at + timezone.timedelta(seconds=1)
    with freeze_time(expire_time):
        call_command('idm_deprive_expired')

    # роль не была перезапрошена вовремя, поэтому перешла в статус "Отозвана"
    role = refresh(role)
    assert role.state == ROLE_STATE.DEPRIVED
    assert role.expire_at is None
    assert_action_chain(role, ['ask_rerequest', 'expire', 'first_remove_role_push', 'remove'])

    # проверка письма с причиной отзыва роли
    message, = mail.outbox
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Simple система. Роль отозвана'
    assert_contains((
        'Добрый день, Фродо!',
        'Робот отозвал вашу роль в системе "Simple система":',
        f'Роль: Админ (https://example.com/system/simple/#role={role.id})',
        'Комментарий: Роль не перезапрошена в срок и будет удалена из системы.',
        'Повторно запросить роль можно на странице роли'
    ), message.body)


def test_switch_state_imported_to_expire(simple_system, arda_users):
    """
    Проверяем случай, когда роль перешла в статус "импортирована", но не была подтверждена вовремя
    TestpalmID: 3456788-114
    """
    # получаем роль в статусе "импортирована" через разрешение расхождений
    frodo = arda_users.frodo
    set_workflow(simple_system, "approvers = [approver('legolas'), approver('gandalf')]")
    make_inconsistency(
        type=Inconsistency.TYPE_THEIR,
        user=frodo,
        system=simple_system,
        path='/role/admin/',
    )
    call_command('idm_check_and_resolve', resolve_only=True)
    role = frodo.roles.select_related('node').get()
    assert role.state == ROLE_STATE.IMPORTED
    clear_mailbox()

    # запускаем отзыв просроченных ролей
    expire_time = role.expire_at + timezone.timedelta(seconds=1)
    with freeze_time(expire_time):
        call_command('idm_deprive_expired')

    # роль не была подтверждена вовремя, поэтому перешла в статус "Отозвана"
    role = refresh(role)
    assert role.state == ROLE_STATE.DEPRIVED
    assert role.expire_at is None
    assert_action_chain(role, ['import', 'apply_workflow', 'resolve_inconsistency', 'expire', 'first_remove_role_push', 'remove'])

    # проверка письма с причиной отзыва роли
    message, = mail.outbox
    assert message.to == ['frodo@example.yandex.ru']
    assert message.subject == 'Simple система. Роль отозвана'
    assert_contains((
        'Добрый день, Фродо!',
        'Робот отозвал вашу роль в системе "Simple система":',
        f'Роль: Админ (https://example.com/system/simple/#role={role.id})',
        'Комментарий: Созданная в связи с разрешением расхождения роль так и не',
        'получила подтвержения в срок.',
        'Повторно запросить роль можно на странице роли'
    ), message.body)


def test_expire_group_role(simple_system, arda_users, department_structure):
    """Проверим переключение в expired для групповой роли, запрошенной не её ответственным"""

    legolas = arda_users.legolas
    fellowship = department_structure.fellowship
    add_perms_by_role('responsible', legolas, simple_system)
    set_workflow(simple_system, group_code='approvers = ["saruman"]')

    role = Role.objects.request_role(legolas, fellowship, simple_system, '', {'role': 'admin'}, None)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)

    assert role.state == 'requested'
    expire_role(role, 1)
    assert len(mail.outbox) == 2

    clear_mailbox()
    simple_system.deprive_expired_roles()

    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'expired'
    assert role.expire_at is None
    assert_action_chain(role, ['request', 'apply_workflow', 'expire'])

    assert role.requests.count() == 1  # запрос отработал на expire роли
    with pytest.raises(RoleRequest.DoesNotExist):
        role.get_open_request()
    assert len(mail.outbox) == 1
    message, = mail.outbox
    assert message.to == ['legolas@example.yandex.ru']
    assert message.subject == 'Запрошенная вами роль в системе "Simple система" не получила одобрения в срок'
    assert message.cc == []
    assert_contains((
        'Запрошенная вами для группы "Братство кольца" роль в системе "Simple система" '
        'не получила подтверждения в срок:',
        'Роль: Админ'
    ), message.body)


def test_rerequested_role_cant_hang_if_exception_was_raised_during_approve(simple_system, users_for_test):
    """Роль должна быть либо подтверждена, либо остаться в том же состоянии.

    Этот тест пробует воспроизвести ошибку, когда сотруднику подтвердили роль,
    но в момент подтверждения (по середине процесса), возникло исключение.
    В таких случаях очень часто оказывается, что ApproveRequest уже отмечен,
    как подтвержденный, а сам роль так и остается невыданной.
    """
    (art, fantom, terran, admin) = users_for_test

    role = Role.objects.request_role(admin, terran, simple_system, '', {'role': 'admin'}, None)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)

    role.set_state('need_request')
    role = refresh(role)
    assert role.state == 'need_request'

    # перезапрашиваем роль
    set_workflow(simple_system, 'approvers = [approver("admin")]')
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)

    role.rerequest(terran)
    role = refresh(role)

    assert role.state == 'rerequested'
    assert role.expire_at is not None

    # подтверждаем роль
    role_request = role.get_last_request()
    req = ApproveRequest.objects.select_related_for_set_decided().get()

    with patch.object(Role, 'set_state') as set_state:
        set_state.side_effect = RuntimeError('Shit happened')

        with pytest.raises(RuntimeError):
            req.set_approved(admin)

    # Проверяем, что состояние так и не изменилось, ни у роли, ни у запроса
    role = refresh(role)
    assert role.state == 'rerequested'
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        'ask_rerequest', 'rerequest', 'apply_workflow',
    ])

    role_request = refresh(role_request)
    assert not role_request.is_done

    req = refresh(req)
    assert req.approved is None
    assert req.decision == ''


def test_dont_call_add_role_if_role_was_rerequested(simple_system, users_for_test):
    """Мы не должны дергать метод role_add плагина, если роль подтвердили после того, как она была перезапрошена.
    """
    (art, fantom, terran, admin) = users_for_test

    role = Role.objects.request_role(admin, terran, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)

    role.set_state('need_request')

    set_workflow(simple_system, 'approvers = [approver("admin")]')
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)

    role.rerequest(terran)
    role = refresh(role)

    role_request = role.get_last_request()
    req = role_request.approves.get().requests.select_related_for_set_decided().get()

    with patch.object(simple_system.plugin.__class__, 'add_role') as add_role:
        req.set_approved(admin)

        assert not add_role.called

    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'granted'
    role_request = refresh(role_request)
    assert role_request.is_done


def test_role_rerequested_to_granted_when_approver_exists(simple_system, users_for_test):
    """Проверяет выдачу роли посредством её перезапроса.
    """
    (art, fantom, terran, admin) = users_for_test

    role = raw_make_role(terran, simple_system, {'role': 'admin'}, state='need_request')

    # перезапрашиваем роль
    set_workflow(simple_system, 'approvers = [approver("admin")]')

    role.rerequest(terran)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)

    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'rerequested'
    assert role.expire_at is not None

    assert role.requests.count() == 1  # rerequest роли породил новый запрос роли
    role_request = role.get_last_request()
    assert role_request.approves.count() == 1
    assert role_request.approves.get().requests.get().approver_id == admin.id
    assert not role_request.is_done
    # проверим корректность подстановки action для запроса роли
    assert role.actions.exclude(action='apply_workflow').order_by('-pk').first() == role_request.actions.get()

    action = role_request.actions.get()
    assert action.action == 'rerequest'
    assert action.user_id == terran.id

    # подтверждаем роль
    req = role_request.approves.get().requests.select_related_for_set_decided().get()
    with patch.object(simple_system.plugin.__class__, 'add_role') as add_role:
        req.set_approved(admin)

        assert not add_role.called

    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'granted'
    assert_action_chain(role, ['rerequest', 'apply_workflow', 'approve', 'grant'])
    role_request = refresh(role_request)
    assert role_request.is_done


def test_role_rerequested_to_granted_when_approver_equals_requester(simple_system, users_for_test):
    """Проверяет выдачу роль посредством её перезапроса, при этоим роль подтверждает сам запрашивающий
    """
    (art, fantom, terran, admin) = users_for_test

    role = raw_make_role(terran, simple_system, {'role': 'admin'}, state='need_request')

    # перезапрашиваем роль
    set_workflow(simple_system, 'approvers = [approver("terran")]')

    role.rerequest(terran)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)

    assert role.state == 'granted'
    assert role.expire_at is None

    assert_action_chain(role, ['rerequest', 'apply_workflow', 'approve', 'grant'])


def test_role_with_refs_can_be_rerequested(simple_system, arda_users):
    """Проверяем, что при перезапросе роли, у которой есть связанные, ни в одну систему не уходит запросов"""

    frodo = arda_users.get('frodo')
    set_workflow(simple_system, dedent('''
    approvers = []
    if role.get('role') == 'admin':
        ttl_days = 1
        ref_roles = [{
            'system': '%s',
            'role_data': {
                'role': 'manager'
            }
        }]
    ''') % simple_system.slug)
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'granted'
    assert role.requests.count() == 1
    assert role.ttl_days == 1
    role.set_state('need_request')
    assert role.requests.count() == 1
    role.rerequest(frodo)
    assert role.requests.count() == 2
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'granted'
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        'ask_rerequest', 'rerequest', 'apply_workflow', 'approve', 'grant',
    ])
    ref = role.refs.get()
    assert_action_chain(ref, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])


def test_role_with_refs_requiring_approval_can_be_rerequested(simple_system, arda_users):
    """Проверяем, что при перезапросе роли, у которой есть связанные требующие подтверждения роли, мы их не трогаем"""

    frodo = arda_users.get('frodo')
    legolas = arda_users.get('legolas')
    set_workflow(simple_system, dedent('''
    if role.get('role') == 'admin':
        approvers = []
        ttl_days = 1
        ref_roles = [{
            'system': '%s',
            'role_data': {
                'role': 'manager'
            }
        }]
    elif role.get('role') == 'manager':
        approvers = ['legolas']
    ''') % simple_system.slug)

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'granted'
    assert role.requests.count() == 1
    assert role.ttl_days == 1
    ref = role.refs.get()
    assert ref.requests.count() == 1
    assert ref.state == 'requested'
    role.set_state('need_request')
    assert role.requests.count() == 1
    role.rerequest(frodo)
    assert role.requests.count() == 2
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'granted'
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        'ask_rerequest', 'rerequest', 'apply_workflow', 'approve', 'grant',
    ])
    ref = refresh(ref)
    assert_action_chain(ref, ['request', 'apply_workflow'])
    assert ref.requests.count() == 1  # запрос был всего один

    # Теперь отклоним запрос связанной роли и перезапросим родительскую. Связанная роль должна запроситься снова.
    approve_request = ref.requests.get().approves.get().requests.select_related_for_set_decided().get()
    approve_request.set_declined(legolas)
    ref = refresh(ref)
    assert ref.state == 'declined'
    role.set_state('need_request')
    role.rerequest(frodo)
    assert role.refs.count() == 2
    newref = role.refs.get(state='requested')
    assert_action_chain(ref, ['request', 'apply_workflow', 'decline'])
    assert_action_chain(newref, ['request', 'apply_workflow'])


def test_role_with_failed_refs_are_rerequested_on_rerequest(simple_system, other_system, arda_users):
    """Проверяем, что связанные роли, которые не удалось выдать, могут быть выданы при перезапросе"""

    frodo = arda_users.get('frodo')
    set_workflow(simple_system, dedent('''
    if role.get('role') == 'admin':
        approvers = []
        ref_roles = [{
            'system': '%s',
            'role_data': {
                'role': 'manager'
            }
        }]
    ''') % other_system.slug)

    with crash_system(other_system):
        role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'granted'
    assert role.refs.count() == 1
    ref = role.refs.get()
    assert ref.state == 'failed'
    role.set_state('need_request')
    role.rerequest(frodo)
    assert role.requests.count() == 2
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        'ask_rerequest', 'rerequest', 'apply_workflow', 'approve', 'grant',
    ])
    assert role.refs.count() == 2
    newref = role.refs.exclude(pk=ref.pk).get()
    assert_action_chain(ref, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'fail'])
    assert_action_chain(newref, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant'])


def test_group_role_can_be_rerequested(simple_system, arda_users, department_structure):
    """Проверяем, что при перезапросе групповой роли связанные роли не пересоздаются"""

    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    frodo = arda_users.get('frodo')
    set_workflow(simple_system, group_code='approvers = []')
    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.refs.count() == fellowship.members.count()
    refs = set(role.refs.all())
    legolas_role = role.refs.get(user__username='legolas')
    assert_action_chain(legolas_role, ['request', 'approve', 'first_add_role_push', 'grant'])
    role.set_state('need_request')
    role.rerequest(frodo)
    assert set(role.refs.all()) == refs
    assert role.refs.count() == fellowship.members.count()
    legolas_role = refresh(legolas_role)
    assert_action_chain(legolas_role, ['request', 'approve', 'first_add_role_push', 'grant'])


def test_rerequesting_group_role_rerequest_personal_roles(simple_system, arda_users, department_structure):
    """Проверяем, что если запрошенные по групповой роли персональные роли не получилось выдать, то при перезапросе
    групповой роли они будут выданы"""

    fellowship = Group.objects.get(slug='fellowship-of-the-ring')
    fellowship_count = fellowship.members.count()
    frodo = arda_users.get('frodo')
    set_workflow(simple_system, group_code='approvers = []')
    with crash_system(simple_system):
        role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)
    assert role.state == 'granted'
    assert role.refs.count() == fellowship.members.count()
    legolas_role = role.refs.get(user__username='legolas')
    assert legolas_role.state == 'failed'
    assert_action_chain(legolas_role, ['request', 'approve', 'first_add_role_push', 'fail'])
    role.set_state('need_request')
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)
    assert role.refs.count() == fellowship_count
    assert role.refs.filter(state='failed').count() == 0
    assert role.refs.filter(state='granted').count() == fellowship_count
    new_legolas_role = role.refs.get(user__username='legolas', state='granted')
    assert new_legolas_role == legolas_role
    assert_action_chain(new_legolas_role, [
        'request', 'approve', 'first_add_role_push', 'fail', 'rerequest', 'approve', 'first_add_role_push', 'grant',
    ])


def test_failed_role_can_be_rerequested(simple_system, arda_users):
    """Проверяем, что роль в состоянии failed может быть перезапрошена и попадёт в requested"""

    frodo = arda_users.frodo
    with crash_system(simple_system):
        role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    role = refresh(role)
    assert role.state == 'failed'
    set_workflow(simple_system, 'approvers = ["gandalf"]')
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)

    role.rerequest(frodo)
    role = refresh(role)
    assert role.state == 'requested'
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'fail', 'rerequest', 'apply_workflow',
    ])
    assert role.requests.count() == 2


def test_deprived_role_can_be_rerequested(simple_system, arda_users):
    """Проверяем, что роль в состоянии deprived может быть перезапрошена и попадёт в requested"""

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.deprive_or_decline(frodo)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'deprived'
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        'deprive', 'first_remove_role_push', 'remove',
    ])
    set_workflow(simple_system, 'approvers = ["gandalf"]')

    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'requested'
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        'deprive', 'first_remove_role_push', 'remove', 'rerequest', 'apply_workflow',
    ])
    assert role.requests.count() == 2


def test_declined_role_can_be_rerequested(simple_system, arda_users):
    """Проверяем, что роль в состоянии declined может быть перезапрошена и попадёт в requested"""

    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = ["gandalf"]')
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'requested'
    role.deprive_or_decline(frodo)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'declined'
    assert_action_chain(role, ['request', 'apply_workflow', 'decline'])
    set_workflow(simple_system, 'approvers = ["gandalf"]')

    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'requested'
    assert_action_chain(role, ['request', 'apply_workflow', 'decline', 'rerequest', 'apply_workflow'])
    assert role.requests.count() == 2


def test_expired_role_can_be_rerequested(simple_system, arda_users):
    """Проверяем, что роль в состоянии expire может быть перезапрошена и попадёт в requested"""

    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = ["gandalf"]')
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'requested'
    expire_role(role, 1)
    simple_system.deprive_expired_roles()

    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'expired'
    role.rerequest(frodo)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'requested'
    assert_action_chain(role, ['request', 'apply_workflow', 'expire', 'rerequest', 'apply_workflow'])
    assert role.requests.count() == 2


def test_requested_role_can_be_rerequested(simple_system, arda_users):
    """Проверяем, что роль в состоянии requested может быть перезапрошена и попадёт в requested"""

    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = ["varda", "legolas"]')
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'requested'
    ApproveRequest.objects.select_related_for_set_decided().get(approver=arda_users.varda).set_approved(arda_users.varda)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'requested'

    set_workflow(simple_system, 'approvers = ["gandalf", "varda"]')
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'requested'

    assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'rerequest', 'apply_workflow'])
    assert role.requests.count() == 2
    request1, request2 = role.requests.order_by('pk')
    assert request1.is_done is True
    assert request2.is_done is False
    requests = ApproveRequest.objects.filter(approve__role_request=request2).order_by('approver__username')
    gandalf_request, varda_request = requests
    assert gandalf_request.approver_id == arda_users.gandalf.id
    assert gandalf_request.approved is None
    assert gandalf_request.decision == ''
    assert varda_request.approver_id == arda_users.varda.id
    assert varda_request.approved is True
    assert varda_request.decision == 'approve'


def test_requested_role_can_be_rerequested_and_granted(simple_system, arda_users):
    """Проверим случай, когда после перезапроса подтверждающих роль автоматически выдаётся"""

    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = ["varda", "legolas"]')
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'requested'
    ApproveRequest.objects.select_related_for_set_decided().get(approver=arda_users.varda).set_approved(arda_users.varda)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'requested'

    set_workflow(simple_system, 'approvers = ["varda"]')
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'granted'

    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'rerequest', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
    ])
    assert role.requests.count() == 2
    request1, request2 = role.requests.order_by('pk')
    assert request1.is_done is True
    assert request2.is_done is True
    varda_request = ApproveRequest.objects.filter(approve__role_request=request2).get()
    assert varda_request.approver_id == arda_users.varda.id
    assert varda_request.approved is True
    assert varda_request.decision == 'approve'


def test_its_impossible_to_rerequest_role_for_inactive_node(simple_system, arda_users):
    """Проверяем, что невозможно перезапросить роль, если узел этой роли пропал из дерева ролей"""

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'granted'
    role.deprive_or_decline(frodo)
    role.node.state = 'deprived'
    role.node.save(update_fields=['state'])
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    with pytest.raises(Forbidden) as excinfo:
        role.rerequest(frodo)
    expected_message = 'Роль не может быть запрошена, так как система больше не поддерживает данный узел дерева ролей'
    assert str(excinfo.value) == expected_message


def test_its_impossible_to_rerequest_role_if_same_role_is_granted(simple_system, arda_users):
    """Проверка, что невозможно перезапросить роль, если такая же роль уже выдана"""

    frodo = arda_users.frodo
    role1 = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    role1 = refresh(role1)
    assert role1.state == 'granted'
    role1.deprive_or_decline(frodo)
    role1 = refresh(role1)
    assert role1.state == 'deprived'

    role2 = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    role2 = refresh(role2)
    assert role2.state == 'granted'

    with pytest.raises(RoleAlreadyExistsError) as excinfo:
        role1.rerequest(frodo)
    expected_message = (
        'У пользователя "Фродо Бэггинс" уже есть такая роль (Роль: Менеджер) в системе "Simple система"'
        ' в состоянии "Выдана"'
    )
    assert str(excinfo.value) == expected_message


def test_autoapprove_deprived_role(simple_system, arda_users):
    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.deprive_or_decline(frodo)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'deprived'
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        'deprive', 'first_remove_role_push', 'remove',
    ])

    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    assert role.state == 'granted'
    assert_action_chain(
        role,
        [
            'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
            'deprive', 'first_remove_role_push', 'remove',
            'rerequest', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        ]
    )
    assert role.requests.count() == 2


def test_rerequested_to_rerequested(simple_system, arda_users):
    """Проверим, что можно переперезапросить роль"""

    frodo = arda_users.frodo
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)

    assert role.state == 'granted'
    assert role.requests.count() == 1
    assert role.requests.get().approves.count() == 0

    role.set_state('need_request')
    role = refresh(role)
    assert role.state == 'need_request'

    set_workflow(simple_system, 'approvers = ["gandalf"]')
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)
    role = refresh(role)
    assert role.state == 'rerequested'
    assert role.expire_at is not None

    assert role.requests.count() == 2

    set_workflow(simple_system, 'approvers = ["varda"]')
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)
    role = refresh(role)
    assert role.state == 'rerequested'
    assert role.expire_at is not None
    assert role.requests.count() == 3

    assert ApproveRequest.objects.filter(approver__username='varda').count() == 1
    assert_action_chain(
        role,
        [
            'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
            'ask_rerequest', 'rerequest', 'apply_workflow',
            'rerequest', 'apply_workflow',
        ]
    )


def test_rerequest_granted_role(simple_system, arda_users, ad_system):
    """Проверим, что суперпользователь и только суперпользователь может перезапросить granted роль,
    при этом подтверждающие будут сохранены"""

    tree = ad_system.plugin.get_info()
    with mock_tree(ad_system, tree):
        sync_role_nodes(ad_system)

    frodo = arda_users.frodo
    set_workflow(simple_system, 'approvers = ["legolas"]')
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)

    approverq = role.requests.get().approves.get().requests.select_related_for_set_decided().get()
    approverq.fetch_approver()
    approverq.set_approved(approverq.approver)

    role = refresh(role)
    assert role.state == 'granted'

    set_workflow(simple_system, 'approvers = ["legolas"]; ad_groups=["OU=group1"]')
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)

    with pytest.raises(Forbidden) as exc:
        role.rerequest(frodo)

    assert str(exc.value) == 'Перезапрос роли невозможен: Нельзя перезапросить роль в состоянии "Выдана"'
    add_perms_by_role('superuser', frodo)

    role.rerequest(frodo)
    role = refresh(role)
    assert role.state == 'granted'
    assert role.ad_groups == []
    assert role.ref_roles == [{
        'system': 'ad_system',
        'role_data': {
            'type': 'roles_in_groups',
            'ad_group': 'OU=group1',
            'group_roles': 'member',
        }
    }]
    assert role.requests.count() == 2
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        'rerequest', 'apply_workflow', 'approve', 'grant',
    ])

    set_workflow(simple_system, 'approvers = ["legolas", "gandalf"]; ad_groups=["OU=group2"]')
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)

    role = refresh(role)
    assert role.state == 'rerequested'
    assert role.ad_groups == []
    assert role.ref_roles == [{
        'system': 'ad_system',
        'role_data': {
            'type': 'roles_in_groups',
            'ad_group': 'OU=group2',
            'group_roles': 'member',
        }
    }]
    assert_action_chain(
        role,
        [
            'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
            'rerequest', 'apply_workflow', 'approve', 'grant',
            'rerequest', 'apply_workflow',
        ]
    )
    assert role.requests.count() == 3
    request = role.get_last_request()
    assert request.approves.count() == 2
    assert ApproveRequest.objects.filter(approve__role_request=request, decision='approve').count() == 1
    assert ApproveRequest.objects.filter(approve__role_request=request, decision='').count() == 1


def test_rerequest_deprives_stale_roles(simple_system, arda_users, department_structure):
    """Проверим, что переход granted -> rerequested -> granted отзывает те роли, которые перестали иметь причину
    в виде ref_roles в воркфлоу"""

    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    count = fellowship.members.count()
    add_perms_by_role('superuser', frodo)

    set_workflow(
        simple_system,
        code='approvers = []',
        group_code=dedent('''
        approvers = []
        if role['role'] == 'admin':
            ref_roles=[{
                'system': '%s',
                'role_data': {'role': 'manager'},
            }]
        ''' % simple_system.slug
    ))

    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)
    assert role.refs.count() == count + 1
    assert role.refs.first().state == 'granted'
    group_ref = role.refs.get(user=None)
    assert group_ref.refs.count() == count
    assert group_ref.state == 'granted'

    # всё остается так же, только связанная роль теперь superuser
    set_workflow(
        simple_system,
        code='approvers = []',
        group_code=dedent('''
        approvers = []
        if role['role'] == 'admin':
            ref_roles=[{
                'system': '%s',
                'role_data': {'role': 'superuser'},
            }]
        ''' % simple_system.slug
    ))

    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)
    role = refresh(role)
    assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'grant', 'rerequest', 'apply_workflow', 'approve', 'grant'])

    assert role.refs.count() == count + 2
    group_ref = refresh(group_ref)
    assert group_ref.state == 'deprived'
    assert group_ref.refs.filter(state='deprived').count() == group_ref.refs.count()

    new_group_refs = role.refs.filter(user=None).exclude(pk=group_ref.pk)
    assert new_group_refs.count() == 1
    new_group_ref = new_group_refs.get()
    assert new_group_ref.refs.count() == count
    assert new_group_ref.refs.filter(state='granted').count() == count
    new_group_ref.fetch_node()
    assert new_group_ref.node.slug == 'superuser'

    # перезапросим ещё раз и убедимся, что всё ок
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)
    role = refresh(role)
    assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'grant',
                               'rerequest', 'apply_workflow', 'approve', 'grant',
                               'rerequest', 'apply_workflow', 'approve', 'grant'])

    assert role.refs.count() == count + 2
    group_ref = refresh(group_ref)
    assert group_ref.state == 'deprived'
    new_group_ref = refresh(new_group_ref)
    assert new_group_ref.state == 'granted'


def test_rerequest_deprives_stale_roles_with_fields_data(simple_system, arda_users):
    """
    Проверим, что переход granted -> rerequested -> granted отзывает те роли, которые перестали иметь причину
    в виде ref_roles в воркфлоу. Данный тест отличается от предыдущего тем, что тестирует:
    * пользовательские роли
    * роли с полями
    """

    frodo = arda_users.frodo
    add_perms_by_role('superuser', frodo)

    set_workflow(simple_system, dedent('''
        approvers = []
        if role['role'] == 'admin':
            ref_roles=[{
                'system': '%s',
                'role_data': {'role': 'manager'},
                'role_fields': {'login': 'frodo'},
            }]
        ''' % simple_system.slug
    ))

    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)
    assert role.refs.count() == 1
    ref = role.refs.get()
    assert ref.state == 'granted'

    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)
    role = refresh(role)
    assert role.refs.count() == 1
    ref = refresh(ref)
    assert ref.state == 'granted'
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        'rerequest', 'apply_workflow', 'approve', 'grant',
    ])

    # добавим ещё одну роль
    set_workflow(simple_system, dedent('''
        approvers = []
        if role['role'] == 'admin':
            ref_roles=[{
                'system': '%(system)s',
                'role_data': {'role': 'manager'},
                'role_fields': {'login': 'frodo'},
            }, {
                'system': '%(system)s',
                'role_data': {'role': 'manager'},
                'role_fields': {'login': 'sauron'},
            }]
        ''' % {
        'system': simple_system.slug
    }))

    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)
    role = refresh(role)
    assert_action_chain(role, ['request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
                               'rerequest', 'apply_workflow', 'approve', 'grant',
                               'rerequest', 'apply_workflow', 'approve', 'grant'])

    assert role.refs.count() == 2
    ref = refresh(ref)
    assert ref.state == 'granted'

    new_ref = role.refs.exclude(pk=ref.pk).get()
    assert new_ref.fields_data == {'login': 'sauron'}

    # теперь уберём старое
    set_workflow(simple_system, dedent('''
        approvers = []
        if role['role'] == 'admin':
            ref_roles=[{
                'system': '%(system)s',
                'role_data': {'role': 'manager'},
                'role_fields': {'login': 'sauron'},
            }]
        ''' % {
        'system': simple_system.slug
    }))

    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)

    assert role.state == 'granted'
    assert role.refs.count() == 2
    ref = refresh(ref)
    assert ref.state == 'deprived'
    new_ref = refresh(new_ref)
    assert new_ref.state == 'granted'

    # перезапросим ещё раз и убедимся, что всё ок
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(frodo)
    role = refresh(role)
    assert role.refs.count() == 2
    ref = refresh(ref)
    assert ref.state == 'deprived'
    new_ref = refresh(new_ref)
    assert new_ref.state == 'granted'


@pytest.mark.robot
def test_rerequest_imported_role(simple_system, arda_users, superuser_gandalf):
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
    set_workflow(simple_system, 'approvers = ["gimli"]')

    inconsistency.resolve(force=True)
    inconsistency = refresh(inconsistency)
    assert inconsistency.state == 'resolved'
    assert frodo.roles.count() == 1
    role = frodo.roles.get()
    assert role.state == 'granted'
    assert role.fields_data == {'passport-login': 'yndx-frodo98'}
    assert role.system_specific == {'passport-login': 'yndx-frodo98', 'hello': 'world'}

    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(gandalf)
    role = refresh(role)
    assert_action_chain(role, ['import', 'approve', 'grant', 'resolve_inconsistency', 'rerequest', 'apply_workflow'])
    assert role.state == 'rerequested'
    assert role.requests.count() == 1
    assert role.requests.get().approves.get().requests.get().approver_id == arda_users.gimli.id


def test_role_with_changed_fields_data_rerequest(arda_users, simple_system):
    """Перезапросим роль не требующую подтверждения, изменив в ней fields_data после создания"""

    frodo = arda_users.frodo
    frodo.passport_logins.create(login='abc', state='created', is_fully_registered=True)
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, {'passport-login': 'abc', '1': None})
    clear_mailbox()
    assert Role.objects.count() == 1
    assert role.fields_data == {'passport-login': 'abc'}
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.set_state('need_request')
    role.fields_data = {'passport-login': 'abc', 'qty': 10, '1': None}
    role.save(update_fields=('fields_data',))
    role.rerequest(frodo)
    role.refresh_from_db()
    assert role.fields_data == {'passport-login': 'abc'}


def test_role_rerequest_error_after_changed_fields_data(arda_users, complex_system):
    """Перезапросим роль не требующую подтверждения, изменив в ней fields_data после создания"""

    frodo = arda_users.frodo
    frodo.passport_logins.create(login='abc', state='created', is_fully_registered=True)
    role = Role.objects.request_role(frodo, frodo, complex_system, '', {'project': 'subs', 'role': 'developer'}, {'passport-login': 'abc', 'field_1': 'qwe', 'field_2': 'asd', '1': None})
    clear_mailbox()
    assert Role.objects.count() == 1
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.set_state('need_request')
    role.fields_data = {10: 10}
    role.save(update_fields=('fields_data',))
    with pytest.raises(DataValidationError) as err:
        role.rerequest(frodo)
    assert str(err.value) == 'Некоторые поля заполнены некорректно'
