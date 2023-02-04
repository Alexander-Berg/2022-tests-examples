def test_web(mongo, coordinator):
    pkg_b_hash = coordinator.upload('pkg-b', '1.0', 'gen-ab1', branch='stable', tvm_id=1)
    coordinator.announce(pkg_b_hash, ['b1'])
    coordinator.postdl_failed('pkg-b', '1.0', ['b1'])

    coordinator.http_get('/pkg/list')
    coordinator.http_get('/pkg/pkg-b/versions')
    coordinator.http_get('/pkg/pkg-b/1.0/status')
    coordinator.http_get('/pkg/pkg-b/1.0/errlogs/b1')
