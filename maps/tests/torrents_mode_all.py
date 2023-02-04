from maps.infra.ecstatic.proto import coordinator_pb2
from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts


def test_torrents_mode_all(mongo, coordinator):
    def test_no_torrents():
        response = coordinator.torrents_raw().content
        torrents = coordinator_pb2.TorrentsBackupInfo()
        torrents.ParseFromString(response)
        assert len(torrents.torrent_descriptors) == 0

    def test_one_dataset():
        pkg_a_hash = coordinator.upload('pkg-a', '1.0', 'gen-ab1', tvm_id=1)

        coordinator.announce(pkg_a_hash, 'storage11')
        coordinator.announce(pkg_a_hash, 'storage12')
        coordinator.announce(pkg_a_hash, ['a1', 'a2'])

        response = coordinator.torrents_raw().content
        torrents = coordinator_pb2.TorrentsBackupInfo()
        torrents.ParseFromString(response)

        assert len(torrents.torrent_descriptors) == 1

        descriptor = torrents.torrent_descriptors[0]

        assert pkg_a_hash == descriptor.info_hash
        assert len(descriptor.qualified_datasets) == 1

        q = descriptor.qualified_datasets[0]

        assert q.dataset == "pkg-a"
        assert q.version == "1.0"
        assert len(q.tag) == 0

    def test_one_dataset_one_host():
        pkg_a_hash = coordinator.upload('pkg-a', '1.0', 'gen-ab1', tvm_id=1)

        coordinator.announce(pkg_a_hash, 'storage11')
        coordinator.announce(pkg_a_hash, 'storage12')
        coordinator.announce(pkg_a_hash, ['a1'])

        response = coordinator.torrents_raw().content
        torrents = coordinator_pb2.TorrentsBackupInfo()
        torrents.ParseFromString(response)

        assert len(torrents.torrent_descriptors) == 1

        descriptor = torrents.torrent_descriptors[0]

        assert pkg_a_hash == descriptor.info_hash
        assert len(descriptor.qualified_datasets) == 1

        q = descriptor.qualified_datasets[0]

        assert q.dataset == "pkg-a"
        assert q.version == "1.0"
        assert len(q.tag) == 0

    def test_one_dataset_not_adopted():
        pkg_a_hash = coordinator.upload('pkg-a', '1.0', 'gen-ab1', tvm_id=1)
        coordinator.announce(pkg_a_hash, 'storage11')
        coordinator.announce(pkg_a_hash, ['a1'])

        response = coordinator.torrents_raw().content
        torrents = coordinator_pb2.TorrentsBackupInfo()
        torrents.ParseFromString(response)

        assert len(torrents.torrent_descriptors) == 0

    def test_5_datasets_3_not_adopted():
        pkg_a_hash = coordinator.upload('pkg-a', '1.0', 'gen-ab1', tvm_id=1)
        pkg_b_hash = coordinator.upload('pkg-b', '0.7', 'gen-ab1', tvm_id=1)
        pkg_c_hash = coordinator.upload('pkg-c', '1.2', 'gen-cd1', tvm_id=2)
        pkg_d_hash = coordinator.upload('pkg-d', '9.7', 'gen-cd1', tvm_id=2)
        pkg_e_hash = coordinator.upload('pkg-e', '2.5', 'gen-cd1', tvm_id=2)

        coordinator.announce(pkg_a_hash, 'storage11')
        coordinator.announce(pkg_a_hash, 'storage12')

        coordinator.announce(pkg_b_hash, 'storage11')

        coordinator.announce(pkg_c_hash, 'storage11')
        coordinator.announce(pkg_c_hash, 'storage13')

        coordinator.announce(pkg_d_hash, 'storage13')

        coordinator.announce(pkg_e_hash, 'storage12')

        response = coordinator.torrents_raw().content
        torrents = coordinator_pb2.TorrentsBackupInfo()
        torrents.ParseFromString(response)

        matched = 0
        for descriptor in torrents.torrent_descriptors:
            if descriptor.info_hash == pkg_a_hash:
                matched = matched + 1
                assert len(descriptor.qualified_datasets) == 1
                q = descriptor.qualified_datasets[0]
                assert q.dataset == "pkg-a"
                assert q.version == "1.0"
                assert len(q.tag) == 0
            elif descriptor.info_hash == pkg_c_hash:
                matched = matched + 1
                assert len(descriptor.qualified_datasets) == 1
                q = descriptor.qualified_datasets[0]
                assert q.dataset == "pkg-c"
                assert q.version == "1.2"
                assert len(q.tag) == 0
            else:
                assert False

        assert matched == 2

    def test_two_datasets_one_short_lived():
        pkg_a_hash = coordinator.upload('pkg-a', '1.0', 'gen-ab1', tvm_id=1)
        coordinator.announce(pkg_a_hash, 'storage11')
        coordinator.announce(pkg_a_hash, 'storage12')

        pkg_f_hash = coordinator.upload('pkg-f', '1.1', 'gen-h1', tvm_id=4)
        coordinator.announce(pkg_f_hash, 'storage11')
        coordinator.announce(pkg_f_hash, 'storage13')

        response = coordinator.torrents_raw().content
        torrents = coordinator_pb2.TorrentsBackupInfo()
        torrents.ParseFromString(response)

        assert len(torrents.torrent_descriptors) == 1

        descriptor = torrents.torrent_descriptors[0]

        assert pkg_a_hash == descriptor.info_hash
        assert len(descriptor.qualified_datasets) == 1

        q = descriptor.qualified_datasets[0]

        assert q.dataset == "pkg-a"
        assert q.version == "1.0"
        assert len(q.tag) == 0

    def test_two_datasets_with_different_hashes():
        pkg_a_hash = coordinator.upload('pkg-a', '1.0', 'gen-ab1', tvm_id=1)
        coordinator.announce(pkg_a_hash, 'storage11')
        coordinator.announce(pkg_a_hash, 'storage12')
        coordinator.announce(pkg_a_hash, resolve_hosts(['rtc:maps_b']))

        pkg_b_hash = coordinator.upload('pkg-b', '1.7', 'gen-ab1', tvm_id=1)
        coordinator.announce(pkg_b_hash, 'storage11')
        coordinator.announce(pkg_b_hash, 'storage12')
        coordinator.announce(pkg_b_hash, resolve_hosts(['rtc:maps_b']))

        response = coordinator.torrents_raw().content
        torrents = coordinator_pb2.TorrentsBackupInfo()
        torrents.ParseFromString(response)

        assert len(torrents.torrent_descriptors) == 2

        matched = 0
        for descriptor in torrents.torrent_descriptors:
            if descriptor.info_hash == pkg_a_hash:
                matched = matched + 1
                assert len(descriptor.qualified_datasets) == 1
                q = descriptor.qualified_datasets[0]
                assert q.dataset == "pkg-a"
                assert q.version == "1.0"
                assert len(q.tag) == 0
            elif descriptor.info_hash == pkg_b_hash:
                matched = matched + 1
                assert len(descriptor.qualified_datasets) == 1
                q = descriptor.qualified_datasets[0]
                assert q.dataset == "pkg-b"
                assert q.version == "1.7"
                assert len(q.tag) == 0
            else:
                assert False

        assert matched == 2

    def test_two_datasets_with_identical_hashes():
        pkg_a_hash = coordinator.upload('pkg-a', '1.0', 'gen-ab1', data='feedbabe', disk_usage=42, tvm_id=1)
        coordinator.announce(pkg_a_hash, 'storage11')
        coordinator.announce(pkg_a_hash, 'storage12')
        coordinator.announce(pkg_a_hash, resolve_hosts(['rtc:maps_b']))

        pkg_b_hash = coordinator.upload('pkg-b', '1.7', 'gen-ab1', data='feedbabe', disk_usage=42, tvm_id=1)
        coordinator.announce(pkg_b_hash, 'storage11')
        coordinator.announce(pkg_b_hash, 'storage12')
        coordinator.announce(pkg_b_hash, resolve_hosts(['rtc:maps_b']))

        assert pkg_a_hash == pkg_b_hash

        response = coordinator.torrents_raw().content
        torrents = coordinator_pb2.TorrentsBackupInfo()
        torrents.ParseFromString(response)

        assert len(torrents.torrent_descriptors) == 1

        descriptor = torrents.torrent_descriptors[0]

        assert pkg_a_hash == descriptor.info_hash
        assert len(descriptor.qualified_datasets) == 2

        matched = 0
        for q in descriptor.qualified_datasets:
            if q.dataset == "pkg-a":
                matched = matched + 1
                assert len(q.tag) == 0
                assert q.version == "1.0"
            elif q.dataset == "pkg-b":
                matched = matched + 1
                assert len(q.tag) == 0
                assert q.version == "1.7"
            else:
                assert False

        assert matched == 2

    # Two datasets uploaded on different hosts.
    def test_two_datasets_uploaded_on_different_hosts():
        pkg_a_hash = coordinator.upload('pkg-a', '1.0', 'gen-ab1', tvm_id=1)
        coordinator.announce(pkg_a_hash, 'storage11')
        coordinator.announce(pkg_a_hash, 'storage12')
        coordinator.announce(pkg_a_hash, resolve_hosts(['rtc:maps_b']))

        pkg_c_hash = coordinator.upload('pkg-c', '1.7', 'gen-cd1', tvm_id=2)
        coordinator.announce(pkg_c_hash, 'storage11')
        coordinator.announce(pkg_c_hash, 'storage12')
        coordinator.announce(pkg_c_hash, resolve_hosts(['rtc:maps_b']))

        response = coordinator.torrents_raw().content
        torrents = coordinator_pb2.TorrentsBackupInfo()
        torrents.ParseFromString(response)

        assert len(torrents.torrent_descriptors) == 2

        matched = 0
        for descriptor in torrents.torrent_descriptors:
            if descriptor.info_hash == pkg_a_hash:
                matched = matched + 1
                assert len(descriptor.qualified_datasets) == 1
                q = descriptor.qualified_datasets[0]
                assert q.dataset == "pkg-a"
                assert q.version == "1.0"
                assert len(q.tag) == 0
            elif descriptor.info_hash == pkg_c_hash:
                matched = matched + 1
                assert len(descriptor.qualified_datasets) == 1
                q = descriptor.qualified_datasets[0]
                assert q.dataset == "pkg-c"
                assert q.version == "1.7"
                assert len(q.tag) == 0
            else:
                assert False

        assert matched == 2

    def test_branches():
        pkg_a_hash = coordinator.upload('pkg-a', '1.0', 'gen-ab1', branch='testing', tvm_id=1)
        coordinator.step_in('pkg-a', '1.0', 'gen-ab1', branch='stable', tvm_id=1)

        pkg_b_hash = coordinator.upload('pkg-b', '0.7', 'gen-ab1', branch='stable/hold', tvm_id=1)
        coordinator.step_in('pkg-b', '0.7', 'gen-ab1', branch='testing', tvm_id=1)

        pkg_c_hash = coordinator.upload('pkg-c', '1.2', 'gen-cd1', branch='stable', tvm_id=2)
        pkg_d_hash = coordinator.upload('pkg-d', '9.7', 'gen-cd1', tvm_id=2)

        coordinator.announce(pkg_a_hash, 'storage11')
        coordinator.announce(pkg_a_hash, 'storage12')

        coordinator.announce(pkg_b_hash, 'storage12')
        coordinator.announce(pkg_b_hash, 'storage13')

        coordinator.announce(pkg_c_hash, 'storage11')
        coordinator.announce(pkg_c_hash, 'storage13')

        coordinator.announce(pkg_d_hash, 'storage11')
        coordinator.announce(pkg_d_hash, 'storage12')

        response = coordinator.torrents_raw().content
        torrents = coordinator_pb2.TorrentsBackupInfo()
        torrents.ParseFromString(response)

        matched = 0
        for descriptor in torrents.torrent_descriptors:
            if descriptor.info_hash == pkg_a_hash:
                matched = matched + 1
                assert len(descriptor.qualified_datasets) == 1
                q = descriptor.qualified_datasets[0]
                assert q.dataset == "pkg-a"
                assert q.version == "1.0"
                assert len(q.tag) == 0
            elif descriptor.info_hash == pkg_b_hash:
                matched = matched + 1
                assert len(descriptor.qualified_datasets) == 1
                q = descriptor.qualified_datasets[0]
                assert q.dataset == "pkg-b"
                assert q.version == "0.7"
                assert len(q.tag) == 0
            elif descriptor.info_hash == pkg_c_hash:
                matched = matched + 1
                assert len(descriptor.qualified_datasets) == 1
                q = descriptor.qualified_datasets[0]
                assert q.dataset == "pkg-c"
                assert q.version == "1.2"
                assert len(q.tag) == 0
            elif descriptor.info_hash == pkg_d_hash:
                matched = matched + 1
                assert len(descriptor.qualified_datasets) == 1
                q = descriptor.qualified_datasets[0]
                assert q.dataset == "pkg-d"
                assert q.version == "9.7"
                assert len(q.tag) == 0
            else:
                assert False

        assert matched == 4

        matched = 0
        for record in torrents.branches_by_versioned_dataset:
            branch_hold = {branch_hold.name: branch_hold.on_hold for branch_hold in record.branches_hold}
            if record.dataset == "pkg-a" and record.version == "1.0":
                matched = matched + 1
                assert branch_hold.get("testing") is not None
                assert not branch_hold["testing"]
                assert branch_hold.get("stable") is not None
                assert not branch_hold["stable"]
            elif record.dataset == "pkg-b" and record.version == "0.7":
                matched = matched + 1
                assert branch_hold.get("testing") is not None
                assert not branch_hold["testing"]
                assert branch_hold.get("stable") is not None
                assert branch_hold["stable"]
            elif record.dataset == "pkg-c" and record.version == "1.2":
                matched = matched + 1
                assert branch_hold.get("stable") is not None
                assert not branch_hold["stable"]
            else:
                assert False

        assert matched == 3

    mongo.reset()
    mongo.reconfigure(config='data/ecstatic-torrents-test.conf')

    test_no_torrents()

    mongo.reset()
    mongo.reconfigure(config='data/ecstatic-torrents-test.conf')

    test_one_dataset()

    mongo.reset()
    mongo.reconfigure(config='data/ecstatic-torrents-test.conf')

    test_one_dataset_one_host()

    mongo.reset()
    mongo.reconfigure(config='data/ecstatic-torrents-test.conf')

    test_one_dataset_not_adopted()

    mongo.reset()
    mongo.reconfigure(config='data/ecstatic-torrents-test.conf')

    test_5_datasets_3_not_adopted()

    mongo.reset()
    mongo.reconfigure(config='data/ecstatic-torrents-test.conf')

    test_two_datasets_one_short_lived()

    mongo.reset()
    mongo.reconfigure(config='data/ecstatic-torrents-test.conf')

    test_two_datasets_with_different_hashes()

    mongo.reset()
    mongo.reconfigure(config='data/ecstatic-torrents-test.conf')

    test_two_datasets_with_identical_hashes()

    mongo.reset()
    mongo.reconfigure(config='data/ecstatic-torrents-test.conf')

    test_two_datasets_uploaded_on_different_hosts()

    mongo.reset()
    mongo.reconfigure(config='data/ecstatic-torrents-test.conf')

    test_branches()
