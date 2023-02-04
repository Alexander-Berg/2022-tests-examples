# coding: utf-8


import pytest
from django.core import mail
from idm.core.models import RoleNode
from idm.tests.utils import (make_role, clear_mailbox, assert_contains, add_perms_by_role, get_recievers)

pytestmark = [
    pytest.mark.django_db,
]


def test_send_reminder_about_depriving_nodes(simple_system, complex_system, arda_users):
    """Проверяем команду уведомления систем об удалённых узлах с большим количеством активных ролей.
    """
    add_perms_by_role('responsible', arda_users.manve, complex_system)

    for user in [arda_users.frodo, arda_users.legolas, arda_users.gimli]:
        role = make_role(user, simple_system, {'role': 'manager'})
        role.set_raw_state('granted', is_active=True)
        role = make_role(user, complex_system, {'project': 'subs', 'role': 'developer'})
        role.set_raw_state('granted', is_active=True)
    for user in [arda_users.manve, arda_users.gandalf, arda_users.sauron, arda_users.saruman]:
        role = make_role(user, complex_system, {'project': 'subs', 'role': 'manager'})
        role.set_raw_state('granted', is_active=True)

    for node in RoleNode.objects.filter(slug='manager'):
        node.mark_depriving()
    RoleNode.objects.get(value_path='/subs/developer/').mark_depriving()

    clear_mailbox()
    RoleNode.objects.send_reminders(threshold=3)
    assert len(mail.outbox) == 5
    msg1 = mail.outbox[0]
    assert msg1.subject == 'Удалённые узлы в системе "Simple система"'
    assert_contains([
        'При синхронизации системы "Simple система" было обнаружено '
        'удаление узлов с большим количеством активных ролей:',
        '/manager/ - 3 ролей'
    ], msg1.body)
    msg2 = mail.outbox[4]
    assert get_recievers(mail.outbox) == {
        'idm-notification@yandex-team.ru',
        'simple@yandex-team.ru',
        'simplesystem@yandex-team.ru',
        'manve@example.yandex.ru'
    }
    assert msg2.subject == 'Удалённые узлы в системе "Complex система"'
    assert_contains([
        'При синхронизации системы "Complex система" было обнаружено '
        'удаление узлов с большим количеством активных ролей:',
        '/subs/manager/ - 4 ролей',
        '/subs/developer/ - 3 ролей'
    ], msg2.body)

    mail.outbox = []
    RoleNode.objects.send_reminders(threshold=3)
    assert len(mail.outbox) == 0


def test_send_reminder_about_depriving_nodes_with_inactive_roles(simple_system, complex_system, arda_users):
    """Проверяем случай, когда удалённый узел имеет только большое количество неактивных ролей.
    """
    add_perms_by_role('responsible', arda_users.manve, complex_system)
    for user in [arda_users.frodo, arda_users.legolas, arda_users.gimli]:
        role = make_role(user, simple_system, {'role': 'manager'})
        role.set_raw_state('deprived', is_active=False)
        role = make_role(user, complex_system, {'project': 'subs', 'role': 'developer'})
        role.set_raw_state('granted', is_active=True)
    for user in [arda_users.manve, arda_users.gandalf, arda_users.sauron, arda_users.saruman]:
        role = make_role(user, complex_system, {'project': 'subs', 'role': 'manager'})
        role.set_raw_state('deprived', is_active=False)

    for node in RoleNode.objects.filter(slug='manager'):
        node.mark_depriving()
    RoleNode.objects.get(value_path='/subs/developer/').mark_depriving()

    clear_mailbox()
    RoleNode.objects.send_reminders(threshold=3)
    assert len(mail.outbox) == 2
    msg = mail.outbox[0]
    assert get_recievers(mail.outbox) == {'idm-notification@yandex-team.ru', 'manve@example.yandex.ru'}
    assert msg.subject == 'Удалённые узлы в системе "Complex система"'
    assert_contains([
        'При синхронизации системы "Complex система" было обнаружено '
        'удаление узлов с большим количеством активных ролей:',
        '/subs/developer/ - 3 ролей'
    ], msg.body)
