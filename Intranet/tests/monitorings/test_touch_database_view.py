import pytest

from django.urls import reverse
from rest_framework import status


pytestmark = pytest.mark.django_db


def test_update_touch_ok(crt_client):
    response = crt_client.json.get(reverse('check-database'))
    assert response.status_code == status.HTTP_200_OK
    assert response.content == b'ok'


def test_update_touch_error(crt_client, monkeypatch):

    def save(*args, **kwargs):
        raise Exception()

    monkeypatch.setattr('intranet.crt.monitorings.views.update_touch.CheckDataBaseAccess.save', save)

    response = crt_client.json.get(reverse('check-database'))
    assert response.status_code == status.HTTP_500_INTERNAL_SERVER_ERROR
    assert response.content == b'error'
