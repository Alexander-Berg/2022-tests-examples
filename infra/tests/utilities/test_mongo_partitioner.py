import collections
import time
from unittest import mock

import gevent
import pytest

from walle.util.mongo import MongoPartitionerService
from walle.util.mongo.lock import LockError, LockIsExpiredError, MongoLock


def _prepare_partitioner(node):
    service = "test_service"
    with mock.patch('walle.util.cloud_tools.get_process_identifier', return_value=str(node)):
        partitioner = MongoPartitionerService(service)
    partitioner.start()
    return partitioner


def _prepare_ten_partitioners():
    nodes_count = 10
    partitioners = []
    for node in range(nodes_count):
        partitioner = _prepare_partitioner(node)
        partitioners.append(partitioner)

    for partitioner in partitioners:
        # NOTE(rocco66): we should update after starting each nodes for correct shards redistributing
        partitioner._update_party()

    return partitioners


def test_partitioner_distributing(walle_test):
    partitioners = _prepare_ten_partitioners()
    shards = []
    partitioner_distributions = collections.defaultdict(int)
    shards_count = len(partitioners) * 100
    for shard_id in range(shards_count):
        for partitioner in partitioners:
            if shard := partitioner.get_shard(shard_id):
                shards.append(shard)
                partitioner_distributions[partitioner._this_node] += 1
                break
        else:
            assert False, f"can't find node for shard {shard_id}"

    assert len(shards) == shards_count
    half_of_shards_per_node = (shards_count / len(partitioners)) / 2
    assert all(
        partitioner_shards_count > half_of_shards_per_node
        for partitioner_shards_count in partitioner_distributions.values()
    )


def test_distribute_on_live_nodes(walle_test):
    partitioners = _prepare_ten_partitioners()

    # NOTE(rocco66): redistribute shards on live nodes only
    for partitioner in partitioners[1:]:
        partitioner.stop()

    last_partitioner = partitioners[0]
    last_partitioner._update_party()

    shards_count = len(partitioners) * 100
    for shard in range(shards_count):
        assert last_partitioner.get_shard(shard)


def test_shard_migration_race(walle_test):
    shard = "some-shard"

    partitioner1 = _prepare_partitioner("node1")
    shard1 = partitioner1.get_shard(shard)
    partitioner1.stop()

    # NOTE(rocco66): shard was migrated to other node
    partitioner2 = _prepare_partitioner("node2")
    shard2 = partitioner2.get_shard(shard)

    # NOTE(rocco66): but new node's lock will fault until node1 releases lock
    with shard1.lock:
        with pytest.raises(LockError):
            with shard2.lock:
                pass

    with shard2.lock:
        pass


def _wait(predicate, wait_seconds, fail_message):
    deadline = time.time() + wait_seconds

    while predicate():
        if time.time() > deadline:
            raise RuntimeError(fail_message)
        gevent.idle()


def test_lost_party_lock(walle_test):
    partitioner = _prepare_partitioner("process_id")
    origin_lock = partitioner._party_lock
    with mock.patch('walle.util.mongo.lock.InterruptableLock.acquire', return_value=False):
        MongoLock.objects().delete()
        gevent.kill(partitioner._party_supervisor, exception=LockIsExpiredError("locked_object_id", "message"))
        _wait(lambda: partitioner._party_lock is origin_lock, wait_seconds=2, fail_message="lock was not re created")
    _wait(lambda: not partitioner._party_lock.acquired(), wait_seconds=2, fail_message="lock was not re acquired")
    assert partitioner._party_lock.get_whole_party()
