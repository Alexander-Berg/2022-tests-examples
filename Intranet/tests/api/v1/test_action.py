# coding: utf-8


import pytest

from idm.core.models import Action
from idm.tests.utils import add_perms_by_role, raw_make_role
from idm.utils import reverse


pytestmark = [pytest.mark.django_db, pytest.mark.robot]


def test_impersonator(client, simple_system, users_for_test):
    (art, fantom, terran, admin) = users_for_test
    Action.objects.all().delete()
    # разрешение имперсонатору дается только на систему, а система проверяется по роли
    fantom_role = raw_make_role(fantom, simple_system, {'role': 'manager'})
    fantom_action = Action.objects.create(action='grant', user=fantom, role=fantom_role)
    terran_role = raw_make_role(terran, simple_system, {'role': 'manager'})
    Action.objects.create(action='grant', user=terran, role=terran_role)

    client.login('art')
    add_perms_by_role('impersonator', art, simple_system)

    response = client.json.get(
        reverse('api_dispatch_list', api_name='v1', resource_name='actions'),
        {'_requester': 'fantom'},
    )
    assert response.status_code == 200
    data = response.json()

    assert len(data['objects']) == 1
    assert data['objects'][0]['id'] == fantom_action.id
