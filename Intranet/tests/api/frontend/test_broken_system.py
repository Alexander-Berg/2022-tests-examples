# coding: utf-8


import pytest

from idm.tests.utils import add_perms_by_role, raw_make_role
from idm.utils import reverse

pytestmark = pytest.mark.django_db


def get_role_url(role_id):
    return reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=role_id)


@pytest.fixture
def roles_url():
    return reverse('api_dispatch_list', api_name='frontend', resource_name='roles')


@pytest.fixture
def broken_system(simple_system):
    simple_system.is_broken = True
    simple_system.save()
    return simple_system


def test_role_in_broken_system(client, broken_system, arda_users, roles_url):
    frodo = arda_users.frodo

    # role granted to art
    add_perms_by_role('responsible', frodo, broken_system)
    role = raw_make_role(frodo, broken_system, {'role': 'superuser'}, state='granted')

    client.login('frodo')
    data = client.json.get(get_role_url(role.pk)).json()

    assert data['permissions'] == {
        'can_be_deprived': False,
        'can_be_approved': False,
        'can_be_rerequested': False,
        'can_be_poked_if_failed': False,
    }

    role2 = raw_make_role(frodo, broken_system, {'role': 'admin'}, state='need_request')

    data = client.json.get(get_role_url(role2.pk)).json()
    assert data['permissions'] == {
        'can_be_deprived': False,
        'can_be_approved': False,
        'can_be_rerequested': False,
        'can_be_poked_if_failed': False,
    }
