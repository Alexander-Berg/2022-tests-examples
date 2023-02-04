# coding: utf-8
"""
Тесты API открытого для тестировщиков на тестинге (triple-combo!)
"""


import pytest

from idm.utils import reverse

pytestmark = pytest.mark.django_db


@pytest.fixture
def users_url():
    return reverse('api_dispatch_list', api_name='testapi', resource_name='users')


def get_user_url(username):
    return reverse('api_dispatch_detail', api_name='testapi', resource_name='users', username=username)


@pytest.mark.robotless
def test_get_users(client, users_for_test, users_url):
    """
    GET /testapi/users/
    """
    client.login('art')
    data = client.json.get(users_url).json()
    (art, fantom, terran, admin) = users_for_test
    fantom.is_active = False
    fantom.save()

    assert data['meta']['total_count'] == 4
    assert [item['username'] for item in data['objects']] == ['admin', 'art', 'fantom', 'terran']


def test_get_user(client, users_for_test):
    """
    GET /testapi/users/art/
    """
    client.login('art')
    data = client.json.get(get_user_url('art')).json()
    assert data['username'] == 'art'
    assert data['is_active'] is True
