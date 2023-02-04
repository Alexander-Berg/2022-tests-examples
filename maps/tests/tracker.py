def test_fb_host(coordinator):
    dataset = 'pkg-b'
    custom_fb_host = 'custom.fb-b2.host'
    pkg_hash = coordinator.upload(dataset, '1.0', 'gen-ab1', branch='stable', tvm_id=1)

    response = coordinator.announce(pkg_hash, 'b1')
    assert ':b1' not in response
    assert ':fb-b1' not in response
    assert ':b2' not in response

    response = coordinator.announce(pkg_hash, f'b2,{custom_fb_host}')
    assert ':b1' in response
    assert ':fb-b1' in response
    assert ':b2' not in response
    assert (':' + custom_fb_host) not in response
    # avoid default fastbone alias
    assert ':fb-b2' not in response

    response = coordinator.announce(pkg_hash, 'b1')
    assert ':b1' not in response
    assert ':fb-b1' not in response
    assert ':b2' in response
    assert (':' + custom_fb_host) in response
    # avoid default fastbone alias
    assert ':fb-b2' not in response


def test_postdl_after_download(coordinator):
    pkg_hash = coordinator.upload('pkg-b', '1.0', 'gen-ab1', branch='stable', tvm_id=1)
    coordinator.announce(pkg_hash, 'b1', left=1)
    assert '' == coordinator.get_postdl('b1').text
    coordinator.announce(pkg_hash, 'b1')
    assert 'pkg-b\t1.0\n' == coordinator.get_postdl('b1').text
    coordinator.announce(pkg_hash, 'b1')
    assert 'pkg-b\t1.0\n' == coordinator.get_postdl('b1').text
    coordinator.postdl('pkg-b', '1.0', 'b1')
    assert '' == coordinator.get_postdl('b1').text
    coordinator.announce(pkg_hash, 'b1')
    assert '' == coordinator.get_postdl('b1').text
    coordinator.announce(pkg_hash, 'b1', left=1)
    assert '' == coordinator.get_postdl('b1').text
    coordinator.announce(pkg_hash, 'b1')
    assert 'pkg-b\t1.0\n' == coordinator.get_postdl('b1').text


def test_announce_same_host(coordinator):
    pkg_hash = coordinator.upload('pkg-b', '1', 'gen-ab1', branch='stable', tvm_id=1)
    assert 'gen-ab1' not in coordinator.announce(pkg_hash, 'gen-ab1', left=0)
    assert 'gen-ab1' in coordinator.announce(pkg_hash, 'storage1', left=1)
    assert 'storage1' in coordinator.announce(pkg_hash, 'gen-ab1', left=0)
    assert 'storage1' not in coordinator.announce(pkg_hash, 'storage1', left=1)
