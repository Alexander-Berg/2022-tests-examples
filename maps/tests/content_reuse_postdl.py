def test_content_reuse_postdl(coordinator):
    torrent_hash = coordinator.upload(
        'pkg-a', '1.0', 'gen-ab1', data='postdl_test', tvm_id=1)
    coordinator.announce(torrent_hash, 'gen-ab1')
    coordinator.announce(torrent_hash, 'a1')

    # Not deployed dataset should not postdl
    assert coordinator.get_postdl('a1').text == ''
    coordinator.step_in('pkg-a', '1.0', 'gen-ab1', branch='stable', tvm_id=1)

    expected = 'pkg-a\t1.0\n'
    assert coordinator.get_postdl('a1').text == expected
    coordinator.postdl('pkg-a', '1.0', 'a1')
    assert coordinator.get_postdl('a1').text == ''

    # Upload the same dataset under different names
    torrent_hash = coordinator.upload(
        'pkg-a', '1.1', 'gen-ab1', branch='stable', data='postdl_test', tvm_id=1)
    assert torrent_hash
    torrent_hash = coordinator.upload(
        'pkg-b', '1.0', 'gen-ab1', branch='stable', data='postdl_test', tvm_id=1)
    assert torrent_hash

    # Hosts that still have that dataset should adopt new version
    coordinator.announce(torrent_hash, 'a1')
    expected_1 = 'pkg-a\t1.1\n'
    assert coordinator.get_postdl('a1').text == expected_1

    # Lagging hosts should install both versions
    coordinator.announce(torrent_hash, 'a2')
    response = coordinator.get_postdl('a2').text
    assert expected in response
    assert expected_1 in response
    assert 'pkg-b' not in response

    coordinator.announce(torrent_hash, 'b1')
    assert coordinator.get_postdl('b1').text == 'pkg-b\t1.0\n'


def test_drop_data(coordinator):
    torrent_hash = coordinator.upload(
        'pkg-a', '1.0', 'gen-ab1', data='postdl_test', now=0, tvm_id=1)
    coordinator.step_in('pkg-a', '1.0', 'gen-ab1', branch='stable', tvm_id=1)
    coordinator.announce(torrent_hash, 'a1', now=1)

    expected = 'pkg-a\t1.0\n'
    assert coordinator.get_postdl('a1').text == expected
    coordinator.postdl('pkg-a', '1.0', 'a1')
    assert coordinator.get_postdl('a1').text == ''

    coordinator.announce(torrent_hash, 'a1', event='stopped', now=2)
    coordinator.announce(torrent_hash, 'a1', left=1, now=3)
    assert coordinator.get_postdl('a1').text == ''
    coordinator.announce(torrent_hash, 'a1', now=4)
    assert coordinator.get_postdl('a1').text == expected
