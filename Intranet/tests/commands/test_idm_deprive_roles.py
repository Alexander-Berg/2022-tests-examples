# coding: utf-8


from contextlib import contextmanager
from textwrap import dedent

import pytest
from django.core import management, mail
from django.core.management import CommandError
from mock import patch
import waffle.testutils

from idm.core.constants.action import ACTION
from idm.core.constants.role import ROLE_STATE
from idm.core.models import Role, RoleField, Action
from idm.core.tasks import DismissUser
from idm.tests.utils import raw_make_role, set_workflow
from idm.users.models import User

pytestmark = [pytest.mark.django_db]


@contextmanager
def patch_dismiss_task():
    with patch.object(DismissUser, 'apply') as dismiss:
        yield dismiss


def test_idm_deprive_roles_regular(simple_system, arda_users):
    User.objects.update(is_active=False, ldap_blocked=False)

    for user in User.objects.users().all():
        raw_make_role(user, simple_system, {'role': 'manager'}, state='granted')

    with patch_dismiss_task() as dismiss:
        management.call_command('idm_deprive_roles')

    kwargslist = [call_[0][1] for call_ in sorted(dismiss.call_args_list, key=lambda x: x[0][1]['username'])]
    for kwargs, user in zip(kwargslist, User.objects.users().order_by('username')):
        assert kwargs == {'username': user.username}


@waffle.testutils.override_switch('idm.deprive_not_immediately', active=True)
@pytest.mark.parametrize('state', ROLE_STATE.ALL_STATES - {'created'})
def test_dismiss_user(arda_users, simple_system, state):
    frodo = arda_users.frodo
    frodo.is_active = False
    frodo.save(update_fields=('is_active',))

    role = raw_make_role(frodo, simple_system, {'role': 'manager'}, state=state)
    management.call_command('idm_deprive_roles')
    role.refresh_from_db()

    if state == ROLE_STATE.DEPRIVING:
        # depriving роли мы не трогаем
        assert role.is_active
        return

    assert not role.is_active
    assert role.state in ROLE_STATE.ALREADY_INACTIVE_STATES

    if state in (ROLE_STATE.REQUESTED, ROLE_STATE.AWAITING):
        decline = role.actions.get(action=ACTION.DECLINE)
        assert decline.comment == 'Сотрудник уволен'
        assert decline.role.state == ROLE_STATE.DECLINED
    elif state in ROLE_STATE.ALREADY_INACTIVE_STATES:
        assert role.actions.count() == 0  # с неактивными ролями ничего не происходит
    else:
        remove = role.actions.get(action=ACTION.REMOVE)
        if state == ROLE_STATE.APPROVED:
            assert remove.comment == 'Сотрудник уволен'
        else:
            assert remove.comment == 'Роль удалена из системы.'
        assert remove.role.state == ROLE_STATE.DEPRIVED

        if state == ROLE_STATE.APPROVED:
            # У approved роли не будет deprive экшена
            return
        deprive = role.actions.get(action=ACTION.DEPRIVE)
        comment = 'Сотрудник уволен'
        comments = {
            'review_request': (
                'Перезапрошенная в связи с регулярным пересмотром роль отозвана и будет удалена из системы: '
                'Сотрудник уволен'
            ),
            'need_request': 'Неперезапрошенная роль отозвана и будет удалена из системы: Сотрудник уволен',
            'expiring': 'Неперезапрошенная роль отозвана и будет удалена из системы: Сотрудник уволен',
            'rerequested': 'Перезапрошенная роль отозвана и будет удалена из системы: Сотрудник уволен',
            'imported': (
                'Созданная в связи с разрешением расхождения роль отозвана и будет удалена из системы: '
                'Сотрудник уволен'
            ),
        }
        assert deprive.comment == comments.get(state, comment)


@waffle.testutils.override_switch('idm.deprive_not_immediately', active=True)
@pytest.mark.parametrize('user_fired', (True, False))
def test_add_role_after_dismiss_user(arda_users, simple_system, user_fired):
    frodo = arda_users.frodo

    role = raw_make_role(frodo, simple_system, {'role': 'admin'}, state='approved')

    if user_fired:
        DismissUser.delay(username=frodo.username)

    action = role.actions.create(
        user=role.user,
        group=role.group,
        requester=None,
        impersonator=None,
        action='approve',
        system=role.system,
    )

    role.requests.update(is_done=True)

    with patch('idm.tests.base.SimplePlugin.add_role') as add_role:
        simple_system.add_role_async(action)

        if user_fired:
            assert not add_role.called
        else:
            assert add_role.called


@waffle.testutils.override_switch('idm.deprive_not_immediately', active=True)
@pytest.mark.parametrize('exceed_threshold', (True, False))
@pytest.mark.parametrize('state', ('granted', 'requested'))
@pytest.mark.parametrize('block', [True, False])
@pytest.mark.parametrize('system_active', [True, False])
def test_threshold_pass(arda_users, simple_system, state, exceed_threshold, block, system_active, settings):
    to_state_map = {
        'granted': 'deprived',
        'requested': 'declined',
    }
    simple_system.is_active = system_active
    simple_system.save(update_fields=('is_active',))

    User.objects.update(is_active=False)
    count = User.objects.users().count()
    if exceed_threshold:
        settings.IDM_DISMISSED_USERS_THRESHOLD = count
    else:
        settings.IDM_DISMISSED_USERS_THRESHOLD = count + 1
    for user in User.objects.users():
        raw_make_role(user, simple_system, {'role': 'manager'}, state=state)
    if exceed_threshold and system_active:
        with pytest.raises(CommandError, match='^Dismissed users threshold violation$'):
            management.call_command('idm_deprive_roles', block=block)
    else:
        management.call_command('idm_deprive_roles', block=block)
    if exceed_threshold or not system_active:
        assert Role.objects.filter(state=state).count() == count
    else:
        to_state = to_state_map[state]
        assert Role.objects.filter(state=to_state).count() == count

    if system_active and exceed_threshold:
        assert len(mail.outbox) == 0
    else:
        message_count = 0
        if system_active:
            message_count = User.objects.users().count()
        assert len(mail.outbox) == message_count


def test_parentful_roles_deprive(arda_users, simple_system, complex_system):
    """Проверим, что если у роли в неотвечающей системе есть связанная в отвечающей, то
    связанная отзывается"""

    frodo = arda_users.frodo
    RoleField.objects.filter(node__system=complex_system).update(is_active=False)

    set_workflow(complex_system, dedent('''
        approvers = []
        ref_roles = [{
            'system': '%s',
            'role_data': {
                'role': 'manager'
            }
        }]
    ''' % simple_system.slug))

    complex_role = Role.objects.request_role(
        frodo, frodo, complex_system, '',
        {'project': 'rules', 'role': 'admin'}
    )
    assert complex_role.refs.count() == 1
    simple_role = complex_role.refs.get()
    assert simple_role.state == 'granted'
    complex_system.is_broken = True
    complex_system.save()
    frodo.is_active = False
    frodo.save()
    management.call_command('idm_deprive_roles')
    simple_role.refresh_from_db()
    complex_role.refresh_from_db()
    assert complex_role.state == 'granted'
    assert simple_role.state == 'deprived'


def test_queryset_deprive_or_decline_with_parent_action(arda_users_with_roles, idm_robot):
    parent_action = Action.objects.create(action=ACTION.GROUP_RESPONSIBLE_ADDED)
    roles = Role.objects.filter(parent__isnull=True)
    roles.deprive_or_decline(idm_robot, parent_action=parent_action)
    assert Action.objects.filter(parent=parent_action).count() == roles.count()
