# coding: utf-8


import pytest

from idm.utils import reverse
from idm.users.models import Group


pytestmark = pytest.mark.django_db


def test_rolerequest(client, simple_system, users_for_test):
    client.login('art')
    url = reverse('api_dispatch_list', api_name='frontend', resource_name='permissions/rolerequest')

    response = client.json.post(url, {'system': simple_system.slug, 'user': 'art'})

    assert response.status_code == 204

    response = client.json.post(url, {'system': simple_system.slug, 'user': 'art', 'path': '/manager/'})

    assert response.status_code == 204

    response = client.json.post(url, {'system': simple_system.slug, 'user': 'terran'})

    assert response.status_code == 403
    expected_message = ('У пользователя "Центурион Марк" нет прав на запрос роли'
                        ' для пользователя "Легат Аврелий" в системе "simple": Недостаточно прав')
    assert response.json()['message'] == expected_message

    group = Group.objects.create(external_id=777)
    simple_system.group_policy = 'unavailable'
    simple_system.save()

    response = client.json.post(url, {'system': simple_system.slug, 'group': group.external_id})

    assert response.status_code == 409
    assert response.json()['message'] == 'Система "Simple система" не поддерживает групповые роли'

    client.login('admin')
    response = client.json.post(url, {'system': simple_system.slug, 'user': 'terran'})

    assert response.status_code == 204
