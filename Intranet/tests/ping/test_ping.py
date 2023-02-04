# coding: utf-8


import pytest
from mock import patch
from django.core.management import call_command

from idm.tests.utils import ignore_tasks

pytestmark = pytest.mark.django_db


def test_ping_200(client):
    # ping w/o trailing slash
    response = client.get('/ping')
    assert response.status_code == 200
    assert response.content == b"I'm alive"

    # ping w. trailing slash
    response = client.get('/ping/')
    assert response.status_code == 200
    assert response.content == b"I'm alive"
