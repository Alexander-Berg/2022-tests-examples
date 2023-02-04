# coding: utf-8
from __future__ import print_function

import pytest

from agent import topology, application
from agent.selector import TargetTree, LeveledSelector, PeerToPeerSelector


class AlwaysYesSelector(LeveledSelector):

    def _maybe_yes(self, probability):
        return bool(probability)


@pytest.yield_fixture(scope="module", autouse=True)
def topology_tree():
    app = application.Application()
    yield app.run_sync(lambda: topology.tree(app))


def get_host(topology_tree):
    return {
        iface.name: iface
        for iface in topology_tree.interfaces()
    }


def test_leveled_selector(topology_tree):
    source = get_host(topology_tree)["jmon-test.search.yandex.net"]

    interface_map = {
        iface.name: iface for iface in topology_tree.interfaces()
        if iface.network_type == source.network_type
        and iface.vrf == source.vrf
    }

    selector = AlwaysYesSelector(
        TargetTree(source, interface_map.itervalues())
    )
    target_list = selector.select()
    assert target_list

    for target in target_list:
        iface = interface_map[target.name]
        assert iface.name != source.name
        assert iface.vrf == source.vrf
        assert iface.network_type == source.network_type


def test_peer_to_peer_selector(topology_tree):
    source = get_host(topology_tree)["jmon-test.search.yandex.net"]

    interface_map = {
        iface.name: iface for iface in topology_tree.interfaces() if iface.vlan == source.vlan
    }

    selector = PeerToPeerSelector(TargetTree(source, interface_map.itervalues()))
    assert sorted([t.name for t in selector.select()]) == sorted(
        iface.name for iface in interface_map.itervalues()
        if iface.name != "jmon-test.search.yandex.net"
    )


@pytest.mark.parametrize("level,per_switch_targets,per_queue_targets,per_dc_targets,per_root_targets", (
    ("switch", 64, 0, 0, 0),
    ("queue", 0, 64, 0, 0),
    ("dc", 0, 0, 64, 0),
    ("root", 0, 0, 0, 64),
))
def test_each_selector_level(topology_tree, level, per_switch_targets, per_queue_targets, per_dc_targets, per_root_targets):
    source = get_host(topology_tree)["jmon-test.search.yandex.net"]

    interface_map = {
        iface.name: iface for iface in topology_tree.interfaces()
        if iface.network_type == source.network_type
    }

    selector = AlwaysYesSelector(
        TargetTree(source, interface_map.itervalues()),
        per_switch_target_count=per_switch_targets,
        per_queue_target_count=per_queue_targets,
        per_intra_datacenter_target_count=per_dc_targets,
        per_inter_datacenter_target_count=per_root_targets
    )

    target_list = [target.name for target in selector.select()]
    assert target_list

    for target_name in target_list:
        source = interface_map[selector.source_name]
        target = interface_map[target_name]

        source_switch = source.switch
        target_switch = target.switch

        source_queue = source.queue
        target_queue = target.queue

        source_datacenter = source.datacenter
        target_datacenter = target.datacenter

        if level == "switch":
            assert target_switch == source_switch
        elif level == "queue":
            assert target_switch != source_switch
            assert target_queue == source_queue
        elif level == "dc":
            assert target_queue != source_queue
            assert target_datacenter == source_datacenter
        elif level == "root":
            assert target_datacenter != source_datacenter
        else:
            raise RuntimeError()


def test_invalid_tree(topology_tree):
    host = get_host(topology_tree)
    first = host["jmon-test.search.yandex.net"]
    second = host["fb-jmon-test.search.yandex.net"]

    assert not TargetTree(first, [second]).is_valid()
    assert not TargetTree(second, [second]).is_valid()
    assert TargetTree(second, [first, second]).is_valid()
    assert not AlwaysYesSelector(TargetTree(second, [second])).select()
