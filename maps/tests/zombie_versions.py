def test_zombie_versions(mongo, coordinator):
    coordinator.upload('pkg-a', '1.0', 'gen-ab1', tvm_id=1)
    coordinator.step_in('pkg-a', '1.0', 'gen-ab1', branch='testing', tvm_id=1)
    coordinator.step_in('pkg-a', '1.0', 'gen-ab1', tvm_id=1)

    coordinator.upload('pkg-b', '1.0', 'gen-ab1', tvm_id=1)
    coordinator.upload('pkg-b', '2.0', 'gen-ab1', tvm_id=1)
    coordinator.step_in('pkg-b', '1.0', 'gen-ab1', branch='testing', tvm_id=1)
    coordinator.step_in('pkg-b', '2.0', 'gen-ab1', branch='stable', tvm_id=1)

    coordinator.remove('pkg-a', '1.0', 'gen-ab1', tvm_id=1)
    coordinator.remove('pkg-b', '1.0', 'gen-ab1', tvm_id=1)

    coordinator.http_post('/debug/PurgeExpiredVersions', now=1)

    assert mongo.db['branch_deploys'].count({'dataset': 'pkg-a'}) == 0, 'zombie version found'

    assert mongo.db['branch_deploys'].count({'dataset': 'pkg-b', 'version': '2.0'}) != 0, 'pkg-b dataset disappeared'
