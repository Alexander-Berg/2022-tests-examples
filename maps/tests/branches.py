def test_branches(coordinator):
    coordinator.upload('pkg-a', '1.0', 'gen-ab1', tvm_id=1)
    assert 'pkg-a' not in coordinator.torrents(host='c1').all_dataset_names
    assert 'pkg-a' not in coordinator.torrents(host='a1').all_dataset_names
    coordinator.move('pkg-a', '1.0', '+prestable', 'gen-ab1', tvm_id=1)
    assert 'pkg-a' in coordinator.torrents(host='c1').all_dataset_names
    assert 'pkg-a' not in coordinator.torrents(host='a1').all_dataset_names
    coordinator.move('pkg-a', '1.0', '+stable', 'gen-ab1', tvm_id=1)
    assert 'pkg-a' in coordinator.torrents(host='c1').all_dataset_names
    assert 'pkg-a' in coordinator.torrents(host='a1').all_dataset_names
    coordinator.move('pkg-a', '1.0', '-prestable', 'gen-ab1', tvm_id=1)
    assert 'pkg-a' not in coordinator.torrents(host='c1').all_dataset_names
    assert 'pkg-a' in coordinator.torrents(host='a1').all_dataset_names
