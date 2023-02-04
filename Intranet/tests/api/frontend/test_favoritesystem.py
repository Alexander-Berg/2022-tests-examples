# coding: utf-8


import pytest

from idm.core.models import FavoriteSystem
from idm.utils import reverse
# разрешаем использование базы в тестах
pytestmark = pytest.mark.django_db


@pytest.fixture
def resorce_url():
    return reverse('api_dispatch_list', api_name='frontend', resource_name='favoritesystems')


def test_get_favorites(client, simple_system, pt1_system, users_for_test, resorce_url):
    """
    GET /frontend/favoritesystems/
    """
    art = users_for_test[0]
    client.login('art')

    data = client.json.get(resorce_url).json()
    assert data['objects'] == []

    FavoriteSystem.objects.create(user=art, system=simple_system)

    data = client.json.get(resorce_url).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['system']['slug'] == simple_system.slug
    assert data['objects'][0]['system']['name'] == simple_system.name


def test_add_favorites(client, simple_system, pt1_system, users_for_test, resorce_url):
    """
    POST /frontend/favoritesystems/
    """
    client.login('art')

    data = client.json.get(resorce_url).json()
    assert data['objects'] == []

    resp = client.json.post(resorce_url, {'system': 'simple'})
    assert resp.status_code == 201
    assert FavoriteSystem.objects.count() == 1

    data = client.json.get(resorce_url).json()
    assert len(data['objects']) == 1
    assert data['objects'][0]['system']['slug'] == simple_system.slug

    resp = client.json.post(resorce_url, {'system': 'simple'})
    assert resp.status_code == 204
    assert FavoriteSystem.objects.count() == 1


def test_del_system(client, simple_system, users_for_test, resorce_url):
    """
    DELETE /frontend/favoritesystems/
    """
    art = users_for_test[0]
    FavoriteSystem.objects.create(user=art, system=simple_system)
    client.login('art')

    resp = client.json.delete(resorce_url, {'system': 'simple'})
    assert resp.status_code == 204

    assert FavoriteSystem.objects.count() == 0
