# coding: utf-8


import pytest

from idm.tests.utils import add_perms_by_role, raw_make_role, refresh, days_from_now, create_user
from idm.utils import reverse


pytestmark = pytest.mark.django_db


def test_200(client):
    client.login(create_user())
    url = reverse('api_dispatch_list', api_name='v1', resource_name='status') + '200/'
    response = client.json.get(url)
    assert response.status_code == 200


def test_500(client):
    client.login(create_user())
    url = reverse('api_dispatch_list', api_name='v1', resource_name='status') + '500/'
    response = client.json.get(url)
    assert response.status_code == 500


def test_400(client):
    client.login(create_user())
    url = reverse('api_dispatch_list', api_name='v1', resource_name='status') + '400/'
    response = client.json.get(url)
    assert response.status_code == 400


def test_400_with_any_wrong_status(client):
    client.login(create_user())
    url = reverse('api_dispatch_list', api_name='v1', resource_name='status')

    for status in [0, 201, 405, 300, None]:
        suffix = str(status) + '/'
        url += suffix

        response = client.json.get(url)
        assert response.status_code == 400


def test_touch_role(client, simple_system, arda_users):
    frodo = arda_users['frodo']
    add_perms_by_role('superuser', frodo)

    client.login('frodo')
    url = reverse('api_dispatch_list', api_name='v1', resource_name='status') + '200/'
    role = raw_make_role(arda_users['gandalf'], simple_system, {'role': 'manager'})
    old_time = role.updated

    with days_from_now(1):
        response = client.json.get(url, {'touch_role': role.pk})

    role = refresh(role)
    assert response.status_code == 200
    assert role.updated != old_time


@pytest.mark.parametrize('code', [400, 500])
def test_touch_role_with_bad_code(client, simple_system, arda_users, code):
    frodo = arda_users['frodo']
    add_perms_by_role('superuser', frodo)

    client.login('frodo')
    url = reverse('api_dispatch_list', api_name='v1', resource_name='status') + str(code) + '/'
    role = raw_make_role(arda_users['gandalf'], simple_system, {'role': 'manager'})
    old_time = role.updated

    with days_from_now(1):
        response = client.json.get(url, {'touch_role': role.pk})

    role = refresh(role)
    assert response.status_code == code
    assert role.updated == old_time
