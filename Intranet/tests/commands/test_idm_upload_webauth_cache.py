# coding: utf-8
import pytest
from unittest import mock
from django.core.management import call_command
from idm.tests.utils import raw_make_role, refresh
from idm.core.management.commands.idm_upload_webauth_cache import Command
pytestmark = pytest.mark.django_db

@pytest.mark.parametrize('use_webauth', (True, False))
def test_upload_webauth_cache(simple_system, pt1_system, arda_users, use_webauth):
    simple_system.use_webauth = use_webauth
    simple_system.save()

    frodo = arda_users.frodo
    raw_make_role(frodo, simple_system, {'role': 'admin'})
    raw_make_role(frodo, simple_system, {'role': 'admin'}, fields_data={'smth': 'test', 'one': 1})
    raw_make_role(frodo, pt1_system, {'project': 'proj2', 'role': 'invisible_role'})

    with mock.patch.object(Command, '_upload_roles') as mock_upload:
        call_command('idm_upload_webauth_cache')

    if use_webauth:
        mock_upload.assert_called_once_with(
            {'simple': {'/admin/': [
                ('frodo', (('one', 1), ('smth', 'test'))),
                ('frodo', ())
            ]}}
        )
    else:
        mock_upload.assert_not_called()
