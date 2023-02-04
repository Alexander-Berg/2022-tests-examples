from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts


def test_resolved_cache(mongo):
    reference_hosts = [
        {'host': 'i1dca', 'dc': 'dca'},
        {'host': 'i2dca', 'dc': 'dca'},
        {'host': 'i3dca', 'dc': 'dca'},
        {'host': 'i4dcb', 'dc': 'dcb'},
        {'host': 'i5dcb', 'dc': 'dcb'},
        {'host': 'i6dcb', 'dc': 'dcb'}
    ]

    def check_deploy_groups(group_name, hosts):
        deploy_groups = list(mongo.db.deploy_groups.find())
        assert len(deploy_groups) == 1
        group = deploy_groups[0]
        assert len(group['items']) == 1
        item = group['items'][0]
        assert item['title'] == group_name
        assert sorted(item['hosts']) == hosts
        assert group['title'] == group_name

    def check_hostlist_full():
        assert sorted(mongo.db.host_dc.find({'group': 'rtc:maps_dc2'}, {'host': 1, 'dc': 1, '_id': 0}),
                      key=lambda info: (info['host'], info['dc'])) == reference_hosts
        check_deploy_groups('rtc:maps_dc2', [host['host'] for host in reference_hosts])

    def check_hostlist_empty():
        assert sorted(mongo.db.host_dc.find({'group': 'rtc:maps_dc2'}, {'host': 1, 'dc': 1, '_id': 0})) == []
        check_deploy_groups('rtc:maps_dc2 NEW', [])

    mongo.reconfigure(config='data/dc_parallel.conf', hosts_config='data/host-groups.conf')
    check_hostlist_full()

    # reconfigure with empty hostlist, assure group is loaded from resolver cache in mongo
    mongo.reconfigure(config='data/dc_parallel.conf', hosts_config='data/host-groups-a2-pulled.conf')
    check_hostlist_full()

    # manually clean resolver cache
    mongo.db.host_dc.remove({'group': 'rtc:maps_dc2'})

    # reconfigure with empty hostlist and check check group is empty
    mongo.reconfigure(config='data/dc_parallel.conf', hosts_config='data/host-groups-a2-pulled.conf')
    check_hostlist_empty()
    assert list(mongo.db.deploy_index.find()) == []

    # reconfigure with non-empty host list and check group includes all hosts
    mongo.reconfigure(config='data/dc_parallel.conf', hosts_config='data/host-groups.conf')
    check_hostlist_full()


def test_resolved_cache_with_failed_dc(mongo):
    mongo.reconfigure(config='data/dc_parallel.conf')
    mongo.reconfigure(config='data/dc_parallel.conf', hosts_config='data/host-groups-failed.conf')
    assert {host['host'] for host in mongo.db['host_dc'].find({'group': 'rtc:maps_dc2'})} == set(resolve_hosts('rtc:maps_dc2'))
