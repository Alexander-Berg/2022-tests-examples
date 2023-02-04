from wiki.async_operations.operation_executors.move_cluster.compress_operations import compress
from wiki.async_operations.operation_executors.move_cluster.consts import MoveCluster as MoveClusterBase
from wiki.pages.constants import PageOrderPosition


class MoveCluster(MoveClusterBase):
    def __eq__(self, other: 'MoveCluster'):
        attributes = ['source', 'target', 'next_to_slug', 'position']
        return all(getattr(self, attr) == getattr(other, attr) for attr in attributes)


def test_simple():
    operations = [
        MoveCluster(source='root/a', target='root/b/a'),
        MoveCluster(source='root/b/a', target='root/c/a'),
    ]

    need_operations = [
        MoveCluster(source='root/a', target='root/c/a'),
    ]

    assert compress(operations) == need_operations


def test_circle():
    operations = [
        MoveCluster(source='root/a', target='root/other'),
        MoveCluster(source='root/c', target='root/bb'),
        MoveCluster(source='root/other', target='root/c'),
    ]
    assert compress(operations) == operations


def test_just_change_rank():
    operations = [
        MoveCluster(source='root/next', target='root/b'),
        MoveCluster(source='root/a', target='root/a', next_to_slug='root/b', position=PageOrderPosition.BEFORE),
        MoveCluster(source='root/b', target='root/other'),
        MoveCluster(source='root/a', target='root/a', next_to_slug='root/c', position=PageOrderPosition.AFTER),
        MoveCluster(source='root/c', target='root/same'),
        MoveCluster(source='root/a', target='root/a', next_to_slug='root/other', position=PageOrderPosition.BEFORE),
    ]
    need_operations = [
        MoveCluster(source='root/next', target='root/other'),
        MoveCluster(source='root/a', target='root/a', next_to_slug='root/other', position=PageOrderPosition.BEFORE),
        MoveCluster(source='root/c', target='root/same'),
    ]
    assert compress(operations) == need_operations


def test_just_change_rank_and_other_relation():
    operations = [
        MoveCluster(source='root/a', target='root/a', next_to_slug='root/d', position=PageOrderPosition.AFTER),
        MoveCluster(source='root/b', target='root/other', next_to_slug='root/a', position=PageOrderPosition.BEFORE),
        MoveCluster(source='root/a', target='root/a', next_to_slug='root/c', position=PageOrderPosition.BEFORE),
    ]
    assert compress(operations) == operations


def test_same_rank():
    operations = [
        MoveCluster(source='root/a', target='root/b/a', next_to_slug='root/b/bd', position=PageOrderPosition.AFTER),
        MoveCluster(source='root/b/a', target='root/c/a', next_to_slug='root/c/cd', position=PageOrderPosition.BEFORE),
    ]

    need_operations = [
        MoveCluster(source='root/a', target='root/c/a', next_to_slug='root/c/cd', position=PageOrderPosition.BEFORE),
    ]

    assert compress(operations) == need_operations


def test_same_subscriber():
    operations = [
        MoveCluster(source='root/a', target='root/a', next_to_slug='root/b', position=PageOrderPosition.AFTER),
        MoveCluster(source='root/a', target='root/a', next_to_slug='root/с', position=PageOrderPosition.AFTER),
        MoveCluster(source='root/a', target='root/a', next_to_slug='root/e', position=PageOrderPosition.BEFORE),
    ]

    need_operations = [
        MoveCluster(source='root/a', target='root/a', next_to_slug='root/e', position=PageOrderPosition.BEFORE),
    ]

    assert compress(operations) == need_operations


def test_not_found_subscriber():
    operations = [
        MoveCluster(source='root/a', target='root/a', next_to_slug='root/c', position=PageOrderPosition.AFTER),
        MoveCluster(source='root/b', target='root/b', next_to_slug='root/с', position=PageOrderPosition.AFTER),
        MoveCluster(source='root/c', target='root/any'),
    ]
    assert compress(operations) == operations


def test_no_change_relative_position():
    operations = [
        MoveCluster(source='root/a', target='root/other', next_to_slug='root/b', position=PageOrderPosition.AFTER),
        MoveCluster(source='root/d', target='root/e', next_to_slug='root/other', position=PageOrderPosition.AFTER),
        MoveCluster(source='root/other', target='root/a', next_to_slug='root/d', position=PageOrderPosition.BEFORE),
    ]
    assert compress(operations) == operations
