from datetime import timedelta
import pytest
from intranet.vconf.tests.call.factories import NodeFactory

from django.utils import timezone
from intranet.vconf.src.ext_api.cms import CMSApi


pytestmark = pytest.mark.django_db


def test_get_suitable_nodes_all_suitable():
    NodeFactory(id=1, load_value=10)
    NodeFactory(id=2, load_value=20)
    NodeFactory(id=3, load_value=30)
    NodeFactory(id=4, load_value=40)
    NodeFactory(id=5, load_value=50)

    node_ids = [node.id for node in CMSApi.get_suitable_nodes()]
    assert sorted(node_ids) == [1, 2]


def test_get_suitable_nodes_too_many_suitable():
    now = timezone.now()
    NodeFactory(id=1, load_value=10)
    NodeFactory(id=2, load_value=20, last_update=now - timedelta(minutes=10))
    NodeFactory(id=3, load_value=30, last_update=now - timedelta(minutes=20))

    node_ids = [node.id for node in CMSApi.get_suitable_nodes()]
    assert sorted(node_ids) == [1, 2, 3]


def test_get_suitable_nodes_enough_suitable():
    now = timezone.now()
    NodeFactory(id=1, load_value=10)
    NodeFactory(id=2, load_value=20)
    NodeFactory(id=3, load_value=30)
    NodeFactory(id=4, load_value=40, last_update=now - timedelta(minutes=10))
    NodeFactory(id=5, load_value=50, last_update=now - timedelta(minutes=20))

    node_ids = [node.id for node in CMSApi.get_suitable_nodes()]
    assert sorted(node_ids) == [1]


def test_get_suitable_nodes_too_many_disabled():
    NodeFactory(id=1, load_value=10)
    NodeFactory(id=2, load_value=20, enabled=False)
    NodeFactory(id=3, load_value=30, enabled=False)

    node_ids = [node.id for node in CMSApi.get_suitable_nodes()]
    assert sorted(node_ids) == [1]
