# coding: utf-8

import pytest
from django.utils import timezone

from uhura.models import SyncTime

pytestmark = pytest.mark.django_db
URL = '/staff_monitoring/'
HEADERS = {
    'HTTP_HOST': 'localhost',
    'content_type': 'application/json'
}


def test_bad_request_no_sync(client):
    response = client.get(URL, **HEADERS)
    assert response.status_code == 500


def test_bad_request_long_sync(client):
    SyncTime.objects.create(name='import_staff', last_success_start=timezone.now() - timezone.timedelta(hours=2))
    response = client.get(URL, **HEADERS)
    assert response.status_code == 500
    assert response.content.decode(encoding='utf-8') == u'Last sync was finished 2.0 hours ago'


def test_bad_request_short_sync(client):
    SyncTime.objects.create(name='import_staff', last_success_start=timezone.now())
    response = client.get(URL, **HEADERS)
    assert response.status_code == 200
    assert response.content.decode(encoding='utf-8') == u'Last sync was finished 0.0 hours ago'
