# coding: utf-8
"""
Здесь тесты для вьюх, которые работают лишь на тестинге
"""


import pytest
from datetime import datetime
from mock import patch, call

from idm.tests.utils import create_user
from idm.utils import reverse
from idm.ping.management.commands.ping import Command
from django.utils import timezone

pytestmark = pytest.mark.django_db


@pytest.fixture()
def command_url():
    return reverse('api_dispatch_list', api_name='testapi', resource_name='commands')


# TODO: v-sopov: падаем и падали
def test_call_command(client, users_for_test, command_url):
    client.login('admin')
    response = client.json.post(command_url, {'command': 'ping'})
    assert response.status_code == 200
    assert response.json() == {'status': 'ok'}

    response = client.json.post(command_url, {'command': 'notexisting'})
    assert response.status_code == 400
    assert response.json() == {'status': 'not found', 'message': 'Unknown command: \'notexisting\''}

    with patch.object(Command, 'handle') as handle:
        handle.side_effect = Exception('Ups')
        response = client.json.post(command_url, {'command': 'ping'})
    assert response.status_code == 500
    assert response.json() == {'status': 'error', 'message': 'Ups'}


def test_argparse_types(client, command_url):
    client.login(create_user())

    with patch('idm.core.querysets.role.RoleManager.request_or_deprive_personal_roles') as personal:
        with patch('idm.core.querysets.role.RoleManager.request_applicable_ref_roles') as refs:
            client.json.post(command_url, {
                'command': 'idm_poke_hanging_roles',
                'kwargs': {
                    'stage': 'request_or_deprive_personal',
                    'retry_failed': True,
                }
            })

    personal.assert_called_once_with(system=None, retry_failed=True)
    refs.assert_not_called()
