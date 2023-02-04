# coding: utf-8


import mock
import pytest

from idm.core.models import Action, Role
from idm.tests.utils import set_workflow, refresh, DEFAULT_WORKFLOW

pytestmark = pytest.mark.django_db


def test_group_aware_system_add_group_role(aware_simple_system, arda_users, department_structure):
    """Проверим, что aware-система отправляет пуш про группу, но не отправляет про каждого пользователя"""

    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    set_workflow(aware_simple_system, group_code=DEFAULT_WORKFLOW)
    with mock.patch.object(aware_simple_system.plugin.__class__, 'add_role') as add_role:
        add_role.return_value = {
            'data': {}
        }
        role = Role.objects.request_role(frodo, fellowship, aware_simple_system, '', {'role': 'admin'},
                                         {'login': 'token'})
        call_params = {
            'role_data': {'role': 'admin'},
            'fields_data': {'login': 'token'},
            'id': role.pk,
            'path': '/role/admin/',
            'group_id': fellowship.external_id,
            'with_external': True,
            'with_inheritance': True,
            'with_robots': True,
            'request_id': Action.objects.get(action='approve').pk,
            'unique_id': '',
        }
        add_role.assert_called_once_with(**call_params)

    role = refresh(role)
    assert role.state == 'granted'
    assert role.refs.count() == 0
