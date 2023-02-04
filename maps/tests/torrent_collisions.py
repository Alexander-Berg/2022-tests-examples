def test_torrent_collisions(coordinator):
    hash = coordinator.upload(
        'pkg-c', '1.0', 'gen-cd1', branch='stable', data='collision', tvm_id=2)
    assert hash
    hash = coordinator.upload(
        'pkg-c', '2.0', 'gen-cd1', branch='stable', data='collision', tvm_id=2)
    assert hash
    hash = coordinator.upload(
        'pkg-d', '1.0', 'gen-cd1', branch='stable', data='collision', tvm_id=2)
    assert hash

    coordinator.announce(hash, 'cd11')
    assert 'pkg-c\t1.0' in coordinator.get_postdl('cd11').text
    assert 'pkg-c\t2.0' in coordinator.get_postdl('cd11').text
    assert 'pkg-d\t1.0' in coordinator.get_postdl('cd11').text

    coordinator.postdl('pkg-c', '1.0', 'cd11')
    coordinator.postdl('pkg-d', '1.0', 'cd11')

    assert 'pkg-c\t1.0' not in coordinator.get_postdl('cd11').text
    assert 'pkg-c\t2.0' in coordinator.get_postdl('cd11').text
    assert 'pkg-d\t1.0' not in coordinator.get_postdl('cd11').text

    coordinator.announce(hash, 'cd11', left=1)
    assert 'pkg-c\t1.0' not in coordinator.get_postdl('cd11').text
    assert 'pkg-c\t2.0' not in coordinator.get_postdl('cd11').text
    assert 'pkg-d\t1.0' not in coordinator.get_postdl('cd11').text

    coordinator.announce(hash, 'cd11')
    assert 'pkg-c\t1.0' in coordinator.get_postdl('cd11').text
    assert 'pkg-c\t2.0' in coordinator.get_postdl('cd11').text
    assert 'pkg-d\t1.0' in coordinator.get_postdl('cd11').text
