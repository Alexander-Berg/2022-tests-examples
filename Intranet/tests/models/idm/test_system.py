# coding: utf-8
import contextlib
from typing import Callable
from unittest import mock

import pytest
import waffle.testutils
from django.conf import settings
from django.core import mail
from django.db.models.signals import post_save
from django.dispatch import Signal

from idm.core.constants.action import ACTION
from idm.core.constants.role import ROLE_STATE
from idm.core.models import Action, Role, System
from idm.core.models.system import setup_system_when_created
from idm.permissions.utils import add_perms_by_role
from idm.tests.utils import capture_http, set_workflow, clear_mailbox, run_commit_hooks, create_system, create_user
from idm.utils import events

pytestmark = pytest.mark.django_db


@contextlib.contextmanager
def mute_signal(signal: Signal, receiver: Callable = None, sender=None, dispatch_uid=None):
    signal.sender_receivers_cache.clear()
    origin_receivers = list(signal.receivers)
    if dispatch_uid:
        signal.disconnect(sender=sender, dispatch_uid=dispatch_uid)
    elif receiver:
        signal.disconnect(receiver=receiver, sender=sender)
    else:
        signal.receivers = []
    signal._clear_dead_receivers()
    yield
    signal.receivers = origin_receivers
    signal.sender_receivers_cache.clear()


def test_get_emails(simple_system, arda_users):
    manve = arda_users.manve
    gandalf = arda_users.gandalf
    add_perms_by_role('responsible', manve, simple_system)
    add_perms_by_role('responsible', gandalf, simple_system, '/poweruser/')

    assert simple_system.get_emails(fallback_to_reponsibles=False) == ['simple@yandex-team.ru',
                                                                       'simplesystem@yandex-team.ru']
    assert simple_system.get_emails(fallback_to_reponsibles=True) == ['simple@yandex-team.ru',
                                                                      'simplesystem@yandex-team.ru']

    # удаляем рассылки
    simple_system.emails = ''
    simple_system.save()

    assert simple_system.get_emails(fallback_to_reponsibles=False) == []
    assert simple_system.get_emails(fallback_to_reponsibles=True) == ['manve@example.yandex.ru']
    # значение по умолчанию - False
    assert simple_system.get_emails() == []


@waffle.testutils.override_switch('idm.deprive_not_immediately', active=True)
def test_shutdown(generic_system, arda_users, department_structure):
    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    set_workflow(generic_system, group_code='approvers=[]')

    return_value = {
        'code': 0,
        'data': {},
    }

    with capture_http(generic_system, return_value):
        Role.objects.request_role(frodo, frodo, generic_system, '', {'role': 'manager'}, None)
        Role.objects.request_role(frodo, fellowship, generic_system, '', {'role': 'manager'}, None)
    assert Role.objects.count() == fellowship.members.count() + 2  # + персональная роль frodo и групповая роль
    assert Role.objects.filter(state=ROLE_STATE.GRANTED).count() == Role.objects.count()  # все роли в статусе granted

    with pytest.raises(AssertionError):
        generic_system.shutdown(requester=frodo)

    generic_system.is_active = False
    generic_system.save(update_fields=['is_active'])
    clear_mailbox()
    with capture_http(generic_system) as sender:
        generic_system.shutdown(requester=frodo)
        assert len(sender.http_post.call_args_list) == 0
    assert len(mail.outbox) == 0
    assert Role.objects.filter(state=ROLE_STATE.DEPRIVED).count() == Role.objects.count()  # все роли в статусе deprived
    shutdown_action = Action.objects.get(action=ACTION.SYSTEM_SHUTDOWN)
    assert shutdown_action.requester_id == frodo.id
    assert shutdown_action.system_id == generic_system.id
    for role in Role.objects.all().prefetch_related('actions'):
        deprive_action = role.actions.get(action=ACTION.DEPRIVE)
        assert deprive_action.parent_id == shutdown_action.id


@pytest.mark.parametrize(
    ('url', 'roles_tree_url', 'tvm_id', 'expected'), [
            ('https://some.paste/123', None, 123, 123,),
            ('https://some.paste/123', None, '', '',),
            ('https://some.paste/123', f'{settings.ARCANUM_REPO_PATH}smth/', '', '',),
            (f'{settings.ARCANUM_REPO_PATH}smth/', 'https://some.paste/123', 123, settings.ARCANUM_TVM_ID),
            (None, f'{settings.ARCANUM_REPO_PATH}smth/', 123, settings.ARCANUM_TVM_ID),
    ]
)
def test_get_tvm_id(generic_system, url, roles_tree_url, tvm_id, expected):
    generic_system.roles_tree_url = roles_tree_url
    generic_system.tvm_id = tvm_id
    generic_system.save()
    tvm_id = generic_system.get_tvm_id(url=url)
    assert tvm_id == expected


@pytest.mark.parametrize('old_value', [None, False, True])
@pytest.mark.parametrize('new_value', [False, True])
def test_export_to_tirole_changed(old_value, new_value):
    if old_value is not None:
        with mute_signal(post_save, sender=System):
            system = create_system(export_to_tirole=old_value, sync_role_tree=False)

    with run_commit_hooks(), mute_signal(post_save, receiver=setup_system_when_created, sender=System), \
            mock.patch('idm.utils.events.add_event') as add_event_mock:
        if old_value is None:
            system = System.objects.create(export_to_tirole=new_value)
        else:
            system.export_to_tirole = new_value
            system.save(update_fields=('export_to_tirole',))

    if new_value:
        add_event_mock.assert_called_once_with(event_type=events.EventType.YT_EXPORT_REQUIRED, system_id=system.id)
    else:
        add_event_mock.assert_not_called()


@pytest.mark.parametrize(('side_effect', 'action_key'), [
    ([NotImplemented], ACTION.ROLE_TREE_SYNCED),
    (Exception, ACTION.ROLE_TREE_SYNC_FAILED),
])
def test_synchronise__start_stop_actions(side_effect, action_key):
    system = create_system()
    user = create_user()

    with mock.patch('idm.core.fetchers.RoleNodeFetcher.fetch', side_effect=side_effect) as fetch_mock:
        system.synchronize(user=user)

    finish_action, start_action = system.actions.select_related('parent', 'requester')[:2]  # type: Action
    assert start_action.action == ACTION.ROLE_TREE_STARTED_SYNC
    assert start_action.requester == user

    assert finish_action.action == action_key
    assert finish_action.requester == user
    assert finish_action.parent == start_action

    fetch_mock.assert_called_once()