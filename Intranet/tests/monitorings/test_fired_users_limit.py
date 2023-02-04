import pytest

from idm.monitorings.metric import FiredUsersLimitExceededMetric

pytestmark = [pytest.mark.django_db]


def test_requester_exceeded_depriving_threshold_ok(client):
    FiredUsersLimitExceededMetric.set(0)
    response = client.get('/monitorings/not-blocked-limit/')
    assert response.status_code == 200


def test_requester_exceeded_depriving_threshold_fail(client, arda_users):
    # Если кеш пустой - зажигаем мониторинг, какие-то проблемы с таской
    response = client.get('/monitorings/not-blocked-limit/')
    assert response.status_code == 400
    assert response.content == b'Cache is empty!'

    # Зажигаем мониторинг, если в кеше лежат данные о пользователях
    FiredUsersLimitExceededMetric.set(123)
    response = client.get('/monitorings/not-blocked-limit/')
    assert response.status_code == 400
    assert response.content == b'Not blocked fired users count: 123'
