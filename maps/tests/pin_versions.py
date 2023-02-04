def test_pin_versions(mongo, coordinator):
    def check_versions_count(count, hosts):
        for host in hosts:
            assert count == coordinator.versions(host).text.count('\n')

    pkg_b1_hash = coordinator.upload('pkg-b', '1.0', 'gen-ab1', branch='stable', tvm_id=1)
    pkg_b2_hash = coordinator.upload('pkg-b', '2.0', 'gen-ab1', branch='stable', tvm_id=1)
    pkg_b3_hash = coordinator.upload('pkg-b', '3.0', 'gen-ab1', branch='stable', tvm_id=1)
    pkg_b4_hash = coordinator.upload('pkg-b', '4.0', 'gen-ab1', branch='stable', tvm_id=1)

    coordinator.announce(pkg_b1_hash, ['b1', 'b2', 'b3', 'b4'])
    coordinator.postdl('pkg-b', '1.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '1.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '__NONE__', ['b5'])

    coordinator.announce(pkg_b2_hash, ['b1', 'b2', 'b3', 'b4'])
    coordinator.postdl('pkg-b', '2.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '2.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '__NONE__', ['b5'])

    coordinator.announce(pkg_b1_hash, ['b4'], left=1)
    coordinator.announce(pkg_b2_hash, ['b4'], left=1)
    coordinator.require_version('pkg-b', '2.0', ['b1', 'b2', 'b3'])
    coordinator.require_version('pkg-b', '__NONE__', ['b4', 'b5'])

    coordinator.announce(pkg_b1_hash, ['b4'])
    coordinator.postdl('pkg-b', '1.0', ['b4'])
    coordinator.require_version('pkg-b', '2.0', ['b1', 'b2', 'b3'])
    coordinator.require_version('pkg-b', '__NONE__', ['b4', 'b5'])

    mongo.reconfigure()
    coordinator.require_version('pkg-b', '2.0', ['b1', 'b2', 'b3'])
    coordinator.require_version('pkg-b', '__NONE__', ['b4', 'b5'])

    coordinator.announce(pkg_b2_hash, ['b4'])
    coordinator.postdl('pkg-b', '2.0', ['b4'])
    coordinator.require_version('pkg-b', '2.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '__NONE__', ['b5'])

    coordinator.switch_failed('pkg-b', '2.0', 'b4')
    coordinator.require_version('pkg-b', '1.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '__NONE__', ['b5'])

    coordinator.step_in('pkg-b', '2.0', 'gen-ab1', branch='stable', tvm_id=1)
    coordinator.require_version('pkg-b', '2.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '__NONE__', ['b5'])

    coordinator.announce(pkg_b3_hash, ['b1', 'b2', 'b3', 'b4'])
    coordinator.postdl('pkg-b', '3.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '3.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '__NONE__', ['b5'])

    coordinator.announce(pkg_b2_hash, ['b4'], left=1)
    coordinator.remove('pkg-b', '3.0', 'gen-ab1', tvm_id=1)
    coordinator.require_version('pkg-b', '1.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '__NONE__', ['b5'])

    coordinator.announce(pkg_b2_hash, ['b4'])
    coordinator.postdl('pkg-b', '2.0', ['b4'])
    coordinator.require_version('pkg-b', '2.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '__NONE__', ['b5'])

    coordinator.announce(pkg_b2_hash, ['b4'], left=1)
    coordinator.require_version('pkg-b', '2.0', ['b1', 'b2', 'b3'])
    coordinator.require_version('pkg-b', '__NONE__', ['b4', 'b5'])

    coordinator.retire('pkg-b', '2.0', 'gen-ab1', branch='stable', tvm_id=1)
    coordinator.require_version('pkg-b', '1.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '__NONE__', ['b5'])

    coordinator.announce(pkg_b4_hash, ['b1', 'b2', 'b3', 'b4'])
    coordinator.postdl('pkg-b', '4.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '4.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '__NONE__', ['b5'])

    pkg_bt4_hash = coordinator.upload('pkg-b:tag', '4.0', 'gen-ab1', branch='stable', tvm_id=1)
    coordinator.require_version('pkg-b', '4.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '__NONE__', ['b5'])
    coordinator.require_version('pkg-b:tag', '__CURRENT__', ['b1', 'b2', 'b3', 'b4', 'b5'])

    coordinator.announce(pkg_bt4_hash, ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '4.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '__NONE__', ['b5'])
    coordinator.require_version('pkg-b:tag', '__CURRENT__', ['b1', 'b2', 'b3', 'b4', 'b5'])

    coordinator.postdl('pkg-b:tag', '4.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '4.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '__NONE__', ['b5'])
    coordinator.require_version('pkg-b:tag', '4.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b:tag', '__NONE__', ['b5'])

    coordinator.remove('pkg-b:tag', '4.0', 'gen-ab1', tvm_id=1)
    coordinator.require_version('pkg-b', '4.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '__NONE__', ['b5'])
    check_versions_count(1, ['b1', 'b2', 'b3', 'b4', 'b5'])

    mongo.reconfigure(hosts_config='data/host-groups-b6-added.conf')
    coordinator.require_version('pkg-b', '4.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '__CURRENT__', ['b5'])
    coordinator.require_version('pkg-b', '__CURRENT__', ['b6'])

    coordinator.announce(pkg_b1_hash, ['b6'])
    coordinator.postdl('pkg-b', '1.0', ['b6'])
    coordinator.require_version('pkg-b', '4.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '__NONE__', ['b5'])
    # version is unspecified for b6

    coordinator.announce(pkg_b4_hash, ['b6'])
    coordinator.postdl('pkg-b', '4.0', ['b6'])
    coordinator.require_version('pkg-b', '4.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '__NONE__', ['b5'])
    coordinator.require_version('pkg-b', '4.0', ['b6'])

    mongo.reconfigure(hosts_config='data/host-groups.conf')
    coordinator.require_version('pkg-b', '4.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '__NONE__', ['b5'])
    check_versions_count(0, ['b6'])

    mongo.reconfigure(hosts_config='data/host-groups-b6-added.conf')
    coordinator.announce(pkg_b4_hash, ['b6'], left=1)
    coordinator.require_version('pkg-b', '4.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', '__NONE__', ['b5'])
    coordinator.require_version('pkg-b', '__NONE__', ['b6'])
