import pytest

from django.core import mail
from django.core.management import call_command
from mock import patch

from idm.core.constants.role import ROLE_STATE
from idm.core.models import Role, RoleNode
from idm.tests.utils import ignore_tasks, clear_mailbox
pytestmark = pytest.mark.django_db


def test_fail_tasks(simple_system, arda_users, department_structure):
    """
    Эмулируем асинхронное создание ролей и возможность изменения состояния групповой роли:
        - запрашиваем групповую роль;
        - на групповую роль запрашиваем пользовательские роли и блокируем выполнение тасок;
        - изменяем состояние групповой роли на deprived и допинываем пользовательские роли;
        - проверяем что в систему роли не отправляются.
    """
    shire = department_structure.fellowship
    role_node = RoleNode.objects.get_node_by_data(simple_system, {'role': 'manager'})
    group_role = Role.objects.create_role(shire, simple_system, role_node, None, save=True)
    group_role.set_raw_state('granted')
    with ignore_tasks():
        group_role.request_group_roles()
    group_role.set_state('depriving')
    clear_mailbox()
    simple_system.actions.clear()
    with patch('idm.core.querysets.role.get_queue_size', return_value=0):
        call_command('idm_poke_hanging_roles')
    for action in simple_system.actions.all():
        if action.group is None:
            assert action.comment == 'Не удалось добавить роль в систему из-за ошибки в IDM.'
    assert group_role.refs.all().count() == shire.employees.count()
    for role in group_role.refs.all():
        assert role.state == ROLE_STATE.IDM_ERROR
    assert len(mail.outbox) == 0
