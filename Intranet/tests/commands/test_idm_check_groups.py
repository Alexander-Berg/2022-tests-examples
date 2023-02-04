import pytest
from django.core import mail
from django.core.management import call_command

from idm.core.models import Role
from idm.monitorings.metric import ActiveRolesOfInactiveGroupsMetric
from idm.tests.utils import set_workflow, DEFAULT_WORKFLOW

# разрешаем использование базы в тестах
pytestmark = [pytest.mark.django_db]


def test_check_groups(simple_system, arda_users, department_structure, settings):
    # проверяет отправку оповещений мониторингу при появлении активных ролей у удаленных групп
    ok_cached_value = {
        'group_roles_amount': 0,
        'personal_roles_amount': 0,
        'group_roles_onhold_amount': 0,
        'personal_roles_onhold_amount': 0,
        'personal_by_ref_amount': 0,
    }

    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    set_workflow(simple_system, group_code=DEFAULT_WORKFLOW)

    cached_value = ok_cached_value.copy()
    role = Role.objects.request_role(frodo, fellowship, simple_system, '', {'role': 'manager'}, None)
    assert role.state == 'granted'
    mail.outbox = []

    call_command('idm_check_groups')
    assert len(mail.outbox) == 0
    assert ActiveRolesOfInactiveGroupsMetric.get() == cached_value

    fellowship.state = 'deprived'
    fellowship.save()
    cached_value = ok_cached_value.copy()
    cached_value['group_roles_amount'] = 1
    cached_value['personal_roles_amount'] = fellowship.members.count()

    call_command('idm_check_groups')
    assert len(mail.outbox) == 0
    assert ActiveRolesOfInactiveGroupsMetric.get() == cached_value

    role.put_on_hold()
    cached_value = ok_cached_value.copy()
    cached_value['group_roles_onhold_amount'] = 1
    cached_value['personal_roles_onhold_amount'] = fellowship.members.count()

    call_command('idm_check_groups')
    assert len(mail.outbox) == 0
    assert ActiveRolesOfInactiveGroupsMetric.get() == cached_value
