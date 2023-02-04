import pytest

from idm.monitorings.metric import ActiveRolesOfInactiveGroupsMetric

pytestmark = [pytest.mark.django_db]


def test_active_roles_if_inactive_groups_ok(client):
    ActiveRolesOfInactiveGroupsMetric.set({'key1': 0, 'key2': 0})
    response = client.get('/monitorings/active-roles-of-inactive-groups/')
    assert response.status_code == 200


def test_active_roles_if_inactive_groups_fail(client, arda_users):
    # Если кеш пустой - зажигаем мониторинг, какие-то проблемы с таской
    response = client.get('/monitorings/active-roles-of-inactive-groups/')
    assert response.status_code == 400
    assert response.content == b'Cache is empty!'

    # Зажигаем мониторинг, если в кеше лежат данные о ролях
    ActiveRolesOfInactiveGroupsMetric.set({'key1': 'value1', 'key2': 'value2'})
    response = client.get('/monitorings/active-roles-of-inactive-groups/')
    assert response.status_code == 400
    assert response.content == b'Active roles of inactive groups:\nkey1: value1\nkey2: value2'
