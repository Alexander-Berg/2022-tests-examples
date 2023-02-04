# coding: utf-8
import socket

import django
import pytest

django.setup()

from django.contrib.auth.models import User
from django.core.cache import cache


pytestmark = pytest.mark.django_db(transaction=True)


@pytest.fixture(autouse=True)
def current_node(transactional_db,):
    from intranet.dogma.dogma.core.models import Node
    Node.objects.create(hostname=socket.getfqdn())


@pytest.fixture()
def client(settings, client):
    def login(username, **kwargs): # В kwargs можно задать uid, password
        settings.YAUTH_TEST_USER = {
            'login': username,
            'uid': kwargs.pop('uid', '1'),
        }
        for item in kwargs:
            settings.YAUTH_TEST_USER[item] = kwargs[item]
    client.login = login
    return client


@pytest.fixture
def users(transactional_db,):
    admin = User(username='admin', password='admin')
    admin.is_superuser = True
    admin.save()
    vasya = User(username='vasya', password='vasya')
    vasya.save()

    return {
        'admin': admin,
        'vasya': vasya,
    }


def pytest_runtest_teardown(item):
    cache.clear()
    from intranet.dogma.dogma.core.logic.users import EmailGuesser
    EmailGuesser.GUESSED_EMAILS = {}
