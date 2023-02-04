# coding: utf-8


from datetime import timedelta

import pytest
from django.core import mail, management
from django.utils import timezone
from freezegun import freeze_time

from idm.core.workflow.exceptions import Forbidden
from idm.core.models import Role, RoleRequest
from idm.tests.utils import set_workflow, create_user, refresh, assert_action_chain
from idm.users.models import Group

# разрешаем использование базы в тестах
pytestmark = [pytest.mark.django_db]


def test_role_with_ttl_rerequest(simple_system):
    """проверка невозможности перезапроса протухшей роли
    """
    terran = create_user('terran')
    admin = create_user('admin', superuser=True)
    set_workflow(simple_system, "approvers = []")

    expire_date = timezone.now() + timedelta(days=7)
    role = Role.objects.request_role(admin, terran, simple_system, '', {'role': 'admin'}, None, ttl_date=expire_date)
    role = refresh(role)

    # Неистекшая временная роль должна быть доступна для перезапроса
    role.set_state('depriving')
    role = Role.objects.select_related('system__actual_workflow', 'node').get(pk=role.pk)
    role.rerequest(terran)
    role = refresh(role)
    assert role.state == 'granted'

    # Невозможно запустить перезапрос для истекшей роли
    with pytest.raises(Forbidden) as excinfo, freeze_time(expire_date):
        management.call_command('idm_deprive_expired')
        role = refresh(role)
        role.rerequest(terran)
    expected_message = (
        'Перезапрос роли невозможен: Нельзя перезапросить временную роль. Запросите новую роль через форму пожалуйста.'
    )
    assert str(excinfo.value) == expected_message


def test_role_with_ttl_days(simple_system):
    """прописываем в workflow временную роль, запрашиваем её, по истечении срока протухания - должен отозваться
    """
    terran = create_user('terran')
    admin = create_user('admin', superuser=True)
    set_workflow(simple_system, "ttl_days = 7; approvers = []")

    assert terran.roles.count() == 0
    assert len(mail.outbox) == 0
    assert RoleRequest.objects.count() == 0

    role = Role.objects.request_role(admin, terran, simple_system, '', {'role': 'manager'}, None)

    # роль выдалась, но с проставленной датой протухания
    assert terran.roles.count() == 1
    role = refresh(role)
    assert role.state == 'granted'
    assert role.is_active
    assert timezone.localtime(timezone.now() + timedelta(days=7)).date() == timezone.localtime(role.expire_at).date()
    assert len(mail.outbox) == 1  # письмо о выдаче роли
    assert role.requests.count() == 1
    assert role.requests.get().approves.count() == 0

    with freeze_time(timezone.now() + timedelta(days=7)):
        # запускаем механизм отзыва протухших ролей
        management.call_command('idm_deprive_expired')

    role = refresh(role)
    assert role.state == 'deprived'
    assert not role.is_active
    assert_action_chain(role, [
        'request', 'apply_workflow', 'approve', 'first_add_role_push', 'grant',
        'expire', 'first_remove_role_push', 'remove',
    ])

    assert len(mail.outbox) == 2  # письмо об отзыве роли


def test_min_ttl_days(simple_system):
    """роль истекает при минимуме введенного ttl и ttl из воркфлоу
    """
    terran = create_user('terran')
    admin = create_user('admin', superuser=True)
    set_workflow(simple_system, "ttl_days = 7; approvers = []")

    role = Role.objects.request_role(admin, terran, simple_system, '', {'role': 'manager'}, None)
    role = refresh(role)
    assert role.state == 'granted'
    assert role.ttl_days == 7

    role = Role.objects.request_role(admin, terran, simple_system, '', {'role': 'admin'}, None, ttl_days=4)
    role = refresh(role)
    assert role.state == 'granted'
    assert role.ttl_days == 4

    set_workflow(simple_system, "approvers = []")
    role = Role.objects.request_role(admin, terran, simple_system, '', {'role': 'poweruser'}, None, ttl_days=9)
    role = refresh(role)
    assert role.state == 'granted'
    assert role.ttl_days == 9


def test_role_with_ttl_date(simple_system):
    """прописываем в workflow временную роль, запрашиваем её, по истечении срока протухания - должен отозваться
    """
    terran = create_user('terran')
    admin = create_user('admin', superuser=True)
    set_workflow(simple_system, "approvers = []")

    dt = timezone.localtime(timezone.now() + timedelta(days=8))
    role = Role.objects.request_role(admin, terran, simple_system, '', {'role': 'manager'}, None, ttl_date=dt)

    role = refresh(role)
    assert role.state == 'granted'
    assert role.is_active
    assert timezone.localtime(role.expire_at).date() == timezone.localtime(timezone.now() + timedelta(days=8)).date()


def test_group_role_ttl_days(simple_system, arda_users, department_structure):
    """Проверка, что временная групповая роль не проставляет связанным пользовательским ролям ttl_days"""
    frodo = arda_users.get('frodo')
    gandalf = arda_users.get('gandalf')
    set_workflow(simple_system, group_code="ttl_days = 7; approvers = []")
    group = Group.objects.get(slug='fellowship-of-the-ring')

    role = Role.objects.request_role(frodo, group, simple_system, '', {'role': 'manager'}, None)
    role = refresh(role)
    assert Role.objects.count() == group.members.count() + 1
    assert role.state == 'granted'
    assert role.is_active
    assert timezone.localtime(timezone.now() + timedelta(days=7)).date() == timezone.localtime(role.expire_at).date()
    gandalf_role = Role.objects.get(user=gandalf)
    assert gandalf_role.state == 'granted'
    assert gandalf_role.ttl_days is None

    with freeze_time(timezone.now() + timedelta(days=7)):
        # запускаем механизм отзыва протухших ролей
        management.call_command('idm_deprive_expired')

    role = refresh(role)
    assert role.state == 'deprived'
    assert Role.objects.filter(state='deprived').count() == group.members.count() + 1


def test_expire_requested_role(simple_system, arda_users, department_structure):
    """Проверка, что роль устаревает на этапе запроса"""

    frodo = arda_users.get('frodo')
    set_workflow(simple_system, "approvers = [approver('gandalf')]")

    dt = timezone.localtime(timezone.now() + timedelta(days=2))
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None, ttl_date=dt)
    role = refresh(role)
    assert role.state == 'requested'
    assert timezone.localtime(role.expire_at).date() == dt.date()

    with freeze_time(timezone.now() + timedelta(days=2)):
        # запускаем механизм отзыва протухших ролей
        management.call_command('idm_deprive_expired')

    role = refresh(role)
    assert role.state == 'expired'


def test_expire_granted_role(simple_system, arda_users, department_structure):
    """Проверка, что роль устаревает вовремя"""
    frodo = arda_users.get('frodo')
    set_workflow(simple_system, "approvers = []")

    dt = timezone.localtime(timezone.now() + timedelta(days=20))
    role = Role.objects.request_role(frodo, frodo, simple_system, '', {'role': 'manager'}, None, ttl_date=dt)
    role = refresh(role)
    assert role.state == 'granted'

    with freeze_time(dt - timedelta(minutes=1)):
        # запускаем механизм отзыва протухших ролей
        management.call_command('idm_deprive_expired')

    role = refresh(role)
    assert role.state == 'granted'

    with freeze_time(dt + timedelta(minutes=1)):
        # запускаем механизм отзыва протухших ролей
        management.call_command('idm_deprive_expired')

    role = refresh(role)
    assert role.state == 'deprived'


def test_rerun_workflow_correct_expiration(users_for_test, simple_system, idm_robot):
    user = users_for_test[0]
    role = Role.objects.request_role(user, user, simple_system, '', {'role': 'poweruser'}, None)
    role.refresh_from_db()

    day = timedelta(days=1)
    now = timezone.now()
    role.set_raw_state('granted',
                       granted_at=now - day * 10,
                       expire_at=now + day * 10,
                       review_at=now + day * 10,
                       ttl_days=15,
                       review_days=15)
    role.rerun_workflow(idm_robot)
    role.refresh_from_db()
    assert role.expire_at == now + day * 5
    assert role.review_at == now + day * 5
    role.set_raw_state('granted',
                       granted_at=now - day * 10,
                       expire_at=now + day * 10,
                       review_at=now + day * 10,
                       ttl_days=25,
                       review_days=25)
    role.rerun_workflow(idm_robot)
    role.refresh_from_db()
    assert role.expire_at == now + day * 10
    assert role.review_at == now + day * 10


def test_rerun_workflow_remove_redundant_ttl(users_for_test, simple_system, idm_robot):
    user = users_for_test[0]
    role = Role.objects.request_role(user, user, simple_system, '', {'role': 'poweruser'}, None)
    role.refresh_from_db()
    set_workflow(simple_system, 'approvers=[];ttl_days=5;review_days=5')

    day = timedelta(days=1)
    now = timezone.now()
    for kw in [{'ttl_date': now + day * 10, 'review_date': now + day * 10},
               {'ttl_days': 10, 'review_days': 10}]:
        role.set_raw_state('granted', granted_at=now, **kw)
        role.rerun_workflow(idm_robot)
        role.refresh_from_db()
        assert not role.ttl_date
        assert not role.review_date
        assert role.ttl_days == 5
        assert role.review_days == 5
        assert role.expire_at == now + day * 5
        assert role.review_at == now + day * 5
