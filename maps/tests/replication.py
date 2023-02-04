def test_get_empty_config(coordinator):
    assert coordinator.replication_config() == {}


def test_notempty_config(mongo, coordinator):
    mongo.reconfigure(config='data/replication.conf')
    assert coordinator.replication_config() == {'pkg-a': {'from': 'stable', 'branches': ['stable']}}


def test_replication(mongo, coordinator):
    mongo.reconfigure(config='data/replication.conf')
    coordinator.upload('pkg-a', '1.0', 'replicator', tvm_id=2017335)
