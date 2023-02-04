from copy import deepcopy

import pytest
import attr

from django.db import models

from idm.closuretree.managers import CttManager
from idm.nodes.tracking import ExternalTrackingNode
from idm.nodes.updatable import UpdatableNode
from idm.nodes.canonical import Hashable, tuple_of, SELF

pytestmark = pytest.mark.django_db


def refresh(node):
    return type(node).objects.get(pk=node.pk)


class StatefulCttManager(CttManager):
    pass


@attr.s(slots=True)
class NodeCanonical(Hashable):
    slug = attr.ib(validator=attr.validators.instance_of(str))
    name = attr.ib(validator=attr.validators.instance_of(str), default='')
    children = attr.ib(validator=tuple_of(SELF), cmp=False, repr=False, hash=False, default=(), converter=tuple)


class Fetcher(object):
    def __init__(self):
        self.data = None

    def fetch(self, node):
        return self.data

    def update(self, data):
        self.data = data


class Node(UpdatableNode):
    name = models.CharField(max_length=255, default='')
    objects = StatefulCttManager()
    fetcher = Fetcher()

    class Meta:
        app_label = 'users'

    def __str__(self):
        return self.slug

    def as_canonical(self):
        return NodeCanonical(name=self.name, slug=self.slug, hash=self.hash)

    def get_prefetched_children(self, include_depriving=False):
        return self.get_child_nodes(include_depriving=include_depriving)

    def fetch_data(self):
        return self.data

    def find_matching_child(self, data_item, nodes):
        match = None
        matching = [node for node in nodes if node.slug == data_item['slug']]
        if matching and len(matching) == 1:
            match = matching[0]
        return match


class ExternalIdNode(ExternalTrackingNode):
    external_id = models.IntegerField()

    objects = StatefulCttManager()
    EXTERNAL_ID_FIELD = 'external_id'
    REQUIRE_EXTERNAL_ID = True

    class Meta:
        app_label = 'users'

    def __str__(self):
        return str(self.external_id)

    def find_matching_child(self, data_item, nodes):
        match = None
        matching = [node for node in nodes if node.external_id == data_item['external_id']]
        if matching and len(matching) == 1:
            match = matching[0]
        return match

    def fetch_data(self):
        return self.data


def test_path_generation():
    root = Node(name='root', slug='rootnode')
    root.save()
    root = Node.objects.get(pk=root.pk)
    assert root.path == '/rootnode/'
    home = Node(name='home', slug='home', parent=root)
    home.save()
    assert home.path == '/rootnode/home/'
    etc = Node(name='etc', slug='etc', parent=root)
    etc.save()
    assert etc.path == '/rootnode/etc/'
    user = Node(name='etc', slug='user', parent=home)
    user.save()
    assert user.path == '/rootnode/home/user/'
    docs = Node(name='docs', slug='docs', parent=user)
    docs.save()
    assert docs.path == '/rootnode/home/user/docs/'
    # поменяем парента, у дочернего элемента (docs) должны пересчитаться пути
    # user.parent = etc
    # user.save()
    # docs = Node.objects.get(pk=docs.pk)
    # assert docs.path == '/rootnode/etc/user/docs/'

    secondroot = Node(name='second', slug='second')
    secondroot.save()
    assert secondroot.path == '/second/'
    home2 = Node(name='home', slug='home', parent=secondroot)
    home2.save()
    assert home2.path == '/second/home/'


@pytest.mark.xfail
def test_synchronization():
    root = Node(name='root', slug='root')
    root.save()
    test_data = NodeCanonical(
        slug='root',
        hash=None,
        children=(
            NodeCanonical(
                slug='home',
                hash=None,
                children=(
                    NodeCanonical(slug='user', hash=None),
                )
            ),
            NodeCanonical(slug='etc', hash=None),
        )
    )
    root.fetcher.update(test_data)
    result, _ = root.system.synchronize()
    assert result is True
    assert Node.objects.count() == 4
    root = refresh(root)
    root.fetcher.update(test_data)
    result, _ = root.system.synchronize()  # то же дерево, ничего не должно поменяться
    assert result is False
    assert Node.objects.count() == 4
    # попробуем синхронизировать кусок, должно опять же быть в сохранности
    home = Node.objects.get(slug='home')
    home.data = test_data['children'][0]
    assert home.system.synchronize() == (False, None)
    # удалим одну из нод, нод должно стать меньше
    data_with_no_etc = deepcopy(test_data)
    del data_with_no_etc['children'][1]
    root = refresh(root)
    root.data = data_with_no_etc
    assert root.system.synchronize()[0] is True
    assert Node.objects.count() == 4
    assert Node.objects.filter(state='active').count() == 3
    deleted_node = Node.objects.get(slug='etc')
    assert deleted_node.state == 'depriving'
    root = refresh(root)
    root.data = data_with_no_etc
    result, _ = root.system.synchronize()
    assert result is False  # повторная синхронизация ничего не должна менять
    # теперь вернём всё как было, нода должна вернуться
    root = refresh(root)
    root.data = test_data
    assert root.system.synchronize()[0] is True
    assert Node.objects.count() == 5
    assert Node.objects.filter(state='active').count() == 4
    # проверим, что изменения в других данных сохраняются
    changed_names_data = deepcopy(test_data)
    changed_names_data['name'] = 'new root'
    changed_names_data['children'][0]['name'] = 'new home'
    root = refresh(root)
    root.data = changed_names_data
    assert root.system.synchronize()[0] is True
    assert Node.objects.get(slug='home').name == 'new home'


@pytest.mark.xfail
def test_external_id_sync():
    root = ExternalIdNode(slug='root', external_id=0)
    root.save()
    test_data = {
        'slug': 'root',
        'external_id': 0,
        'children': [
            {
                'slug': 'home',
                'external_id': 2,
                'children': [
                    {
                        'slug': 'user',
                        'external_id': 3
                    }
                ]},
            {
                'slug': 'etc',
                'external_id': 1
            }
        ]
    }
    root.data = test_data
    result, _ = root.system.synchronize()
    assert result is True
    root = refresh(root)
    root.data = test_data
    result, _ = root.system.synchronize()
    assert result is False
    assert ExternalIdNode.objects.count() == 4
    assert ExternalIdNode.objects.active().count() == 4
    test_data = {
        'slug': 'root',
        'external_id': 0,
        'children': [
            {
                'slug': 'home',
                'external_id': 2,
                'children': [
                    {
                        'slug': 'user',
                        'external_id': 3
                    }
                ]},
            {
                'slug': 'etc',
                'external_id': 5
            }
        ]
    }
    queue = root.get_queue(test_data)
    assert len(queue) == 2
    assert [item.type for item in queue] == ['add', 'remove']
    root.data = test_data
    assert root.system.synchronize()[0] is True
    assert ExternalIdNode.objects.count() == 5
    assert ExternalIdNode.objects.active().count() == 4
    test_data = {
        'slug': 'root',
        'external_id': 0,
        'children': [
            {
                'slug': 'home',
                'external_id': 2,
                'children': [
                    {
                        'slug': 'user',
                        'external_id': 3
                    },
                    {
                        'slug': 'etc',
                        'external_id': 5
                    }
                ]
            },
        ]
    }
    queue = root.get_queue(test_data)
    assert len(queue) == 2
    assert [item.type for item in queue] == ['add', 'remove']
    root.data = test_data
    assert root.system.synchronize()[0] is True
    assert ExternalIdNode.objects.count() == 6
    assert ExternalIdNode.objects.active().count() == 4
    source = ExternalIdNode.objects.get(external_id=5, parent__slug='root')
    target = ExternalIdNode.objects.get(external_id=5, parent__slug='home')
    assert source.path == '/root/etc/'
    assert target.path == '/root/home/etc/'
    assert target.moved_from == source
    assert source.moved_to == target
