import pytest

from django.urls.base import reverse

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_category_list(su_client):
    url = reverse('api:categories:list')
    response = su_client.get(url)
    assert response.status_code == 200


def test_category_subscribe(su_client):
    category = f.CategoryFactory.create()
    url = reverse('api:categories:subscribe', kwargs={'pk': category.id})
    response = su_client.post(url)
    assert response.status_code == 200


def test_category_unsubscribe(su_client):
    category = f.CategoryFactory.create()
    url = reverse('api:categories:unsubscribe', kwargs={'pk': category.id})
    response = su_client.post(url)
    assert response.status_code == 200
