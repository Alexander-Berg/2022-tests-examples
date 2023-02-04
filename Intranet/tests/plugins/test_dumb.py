# -*- coding: utf-8 -*-


import pytest
from django.core import mail

from idm.core.constants.action import ACTION
from idm.core.models import Action
from idm.inconsistencies.models import Inconsistency
from idm.tests.utils import refresh, assert_contains, raw_make_role, sync_role_nodes


pytestmark = [pytest.mark.django_db]


def test_add_role(dumb_system, arda_users):
    frodo = arda_users.frodo

    role = raw_make_role(frodo, dumb_system, {'role': 'manager'}, state='approved')
    action = role.actions.create(requester=frodo, user=frodo, action='approve')
    assert role.system_specific is None

    dumb_system.add_role_async(action)

    role = refresh(role)
    assert role.state == 'granted'


def test_remove_role(dumb_system, arda_users):
    frodo = arda_users.frodo
    role = raw_make_role(frodo, dumb_system, {'role': 'manager'}, state='granted',
                         system_specific={'passport-login': 'frodo-baggins'})

    role.set_state('depriving')

    role = refresh(role)
    assert role.state == 'deprived'
    assert not role.is_active
    # Проверим, что добавился role action
    last_action = role.actions.order_by('-id').first()
    assert last_action.action == 'remove'
    # И отправилось уведомление
    assert len(mail.outbox) == 1
    assert_contains((
        'Робот отозвал вашу роль в системе "%s"' % dumb_system.name,
        'Роль: Менеджер',
    ), mail.outbox[0].body)


def test_tree_sync(dumb_system):
    Action.objects.all().delete()

    sync_role_nodes(dumb_system)

    assert list(Action.objects.values_list('action', flat=True)) == \
           [ACTION.ROLE_TREE_SYNC_FAILED, ACTION.ROLE_TREE_STARTED_SYNC]


def test_roles_sync(dumb_system, arda_users):
    frodo = arda_users.frodo
    raw_make_role(frodo, dumb_system, {'role': 'manager'}, state='granted')

    assert Inconsistency.objects.count() == 0

    Inconsistency.objects.check_roles(dumb_system.slug)

    assert Inconsistency.objects.count() == 0

    report = Inconsistency.objects.get_report(dumb_system)
    assert report == {}
