# coding: utf-8


import mock
import pytest

from idm.core.models import Action, Role
from idm.tests.utils import set_workflow, refresh, DEFAULT_WORKFLOW, patch_role

pytestmark = pytest.mark.django_db


def test_group_aware_system_deprive_group_role(aware_simple_system, arda_users, department_structure):
    """Проверим, что aware-система при отзыве групповой роли отправляет пуш про группу,
    но не отправляет про каждого пользователя"""

    frodo = arda_users.frodo
    fellowship = department_structure.fellowship
    set_workflow(aware_simple_system, group_code=DEFAULT_WORKFLOW)
    role = Role.objects.request_role(frodo, fellowship, aware_simple_system, '', {'role': 'admin'}, None)
    role = refresh(role)
    assert role.state == 'granted'
    assert role.refs.count() == 0
    patch_role(role, system_specific={'id': 42})

    with mock.patch.object(aware_simple_system.plugin.__class__, 'remove_role') as remove_role:
        remove_role.return_value = {
            'data': {}
        }
        role.deprive_or_decline(frodo)
        remove_role.assert_called_once_with(
            role_data={'role': 'admin'},
            path='/role/admin/',
            fields_data=None,
            system_specific={'id': 42},
            group_id=fellowship.external_id,
            id=role.pk,
            is_fired=False,
            request_id=Action.objects.get(action='deprive').pk,
            unique_id='',
        )
