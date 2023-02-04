# coding: utf-8
from __future__ import print_function

import pytest

from agent import topology
from agent import application
from agent import transformers
from agent import settings

LOCAL_NAME = "sas1-1234.search.yandex.net"
FASTBONE_LOCAL_NAME = "fb-{0}".format(LOCAL_NAME)


@pytest.yield_fixture(scope="module", autouse=True)
def custom_hostname():
    current = settings.current()
    current.hostname = LOCAL_NAME
    try:
        yield
    finally:
        current.hostname = None


def test_group_aware_transformer():
    app = application.Application()

    topology_tree = app.run_sync(lambda: topology.tree(app))

    group_transformer = transformers.GroupAwareTransfomer([
        {"jmon-test.search.yandex.net", LOCAL_NAME},
        {"invalid-host", LOCAL_NAME}
    ])

    selector_list = group_transformer.transform(topology_tree)
    assert len(selector_list) == len(topology_tree.local_interfaces())
    assert {x.source_name for x in selector_list} == set(x.name for x in topology_tree.local_interfaces())

    for selector in selector_list:
        target_set = {target.name for target in selector.select()}
        if selector.source_name == FASTBONE_LOCAL_NAME:
            assert target_set == {"fb-jmon-test.search.yandex.net"}
        elif selector.source_name == LOCAL_NAME:
            assert target_set == {"jmon-test.search.yandex.net"}
        else:
            assert False, "wrong source name"


def test_network_aware_transformer():
    app = application.Application()

    topology_tree = app.run_sync(lambda: topology.tree(app))

    network_transformer = transformers.NetworkAwareTransfomer()

    selector_list = network_transformer.transform(topology_tree)
    assert len(selector_list) == len(topology_tree.local_interfaces())
    assert {x.source_name for x in selector_list} == set(x.name for x in topology_tree.local_interfaces())

    for selector in selector_list:
        target_set = {target.name for target in selector.select()}
        if selector.source_name == FASTBONE_LOCAL_NAME:
            assert target_set.issubset({
                iface.name for iface in topology_tree.interfaces()
                if iface.network_type == "fastbone" and iface.vrf == "Hbf"
            })
        elif selector.source_name == LOCAL_NAME:
            assert target_set.issubset({
                iface.name for iface in topology_tree.interfaces()
                if iface.network_type == "backbone" and iface.vrf == "Hbf"
            })
        else:
            assert False, "wrong source name"


def test_vlan_aware_transformer():
    app = application.Application()

    topology_tree = app.run_sync(lambda: topology.tree(app))

    search_vrf = "Hbf"
    group_transformer = transformers.VlanAwareTransfomer(
        vrfs={search_vrf: {search_vrf}}
    )

    selector_list = group_transformer.transform(topology_tree)
    assert len(selector_list) == len(topology_tree.local_interfaces())
    assert {x.source_name for x in selector_list} == set(x.name for x in topology_tree.local_interfaces())

    for selector in selector_list:
        target_set = {target.name for target in selector.select()}
        if selector.source_name == FASTBONE_LOCAL_NAME:
            assert target_set.issubset({
                iface.name for iface in topology_tree.interfaces()
                if iface.network_type == "fastbone" and iface.vrf == search_vrf
            })
        elif selector.source_name == LOCAL_NAME:
            assert target_set.issubset({
                iface.name for iface in topology_tree.interfaces()
                if iface.network_type == "backbone" and iface.vrf == search_vrf
            })
        else:
            assert False, "wrong source name"
