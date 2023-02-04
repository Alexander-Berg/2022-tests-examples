import pytest

from idm.monitorings.metric import ReviewRolesThresholdExceededMetric

pytestmark = [pytest.mark.django_db]


def test_review_roles_threshold_exceeded_ok(client):
    ReviewRolesThresholdExceededMetric.set({})
    response = client.get('/monitorings/review-roles-exceeded/')
    assert response.status_code == 200


def test_reviews_roles_threshold_exceeded_fail(client):
    # Если кеш пустой - зажигаем мониторинг, какие-то проблемы с таской на пересмотр ролей
    response = client.get('/monitorings/review-roles-exceeded/')
    assert response.status_code == 400
    assert response.content == b'Cache is empty!'

    # Зажигаем мониторинг, если в кеше лежат данные
    ReviewRolesThresholdExceededMetric.set({'self': 5})
    response = client.get('/monitorings/review-roles-exceeded/')
    assert response.status_code == 400
    assert response.content == b'Number of roles to review on last run: {"self": 5}'
