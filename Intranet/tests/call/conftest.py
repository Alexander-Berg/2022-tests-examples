import pytest

from django.conf import settings
from django.test import Client
from intranet.vconf.tests.call.factories import create_user, NodeFactory


@pytest.fixture
def ya_client():
    create_user(username=settings.AUTH_TEST_USER)
    return Client()


@pytest.fixture()
def cms_nodes(db):
    NodeFactory(id=1, load_value=10)
    NodeFactory(id=2, load_value=20)
