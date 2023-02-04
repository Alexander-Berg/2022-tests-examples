# coding: utf-8

import pytest

from procu.api import models
from procu.utils.test import assert_status, prepare_user

pytestmark = [pytest.mark.django_db]


# ------------------------------------------------------------------------------

REFERENCE = {
    'is_subscribed_created': False,
    'sort_quotes_by_updated': True,
    'signature': '--\nРобот Закупок',
}


def test_retrieve_settings(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu')

    resp = client.get('/api/users/settings')
    assert_status(resp, 200)

    assert resp.json() == REFERENCE


def test_update_settings(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu')

    new_settings = {
        'is_subscribed_created': True,
        'sort_quotes_by_updated': False,
        'signature': 'foooo',
    }

    resp = client.patch('/api/users/settings', data=new_settings)
    assert_status(resp, 200)

    user_settings = models.User.objects.values(
        'is_subscribed_created', 'sort_quotes_by_updated', 'signature'
    ).get(username='robot-procu')

    assert user_settings == new_settings
