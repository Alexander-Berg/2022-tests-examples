# coding: utf-8
from __future__ import print_function

from agent import topology, application


def test_topology():
    app = application.Application()

    tree = app.run_sync(lambda: topology.tree(app))

    interfaces = []
    for iface in tree.interfaces():
        if iface.host == "jmon-test.search.yandex.net":
            interfaces.append(iface)
    assert interfaces

    for iface in interfaces:
        assert iface.datacenter == "sas"
        assert iface.queue == "sas1.3.2"
        assert iface.switch == "sas1-s80"
        assert iface.host == "jmon-test.search.yandex.net"

        if iface.network_type == "backbone":
            assert iface.name == "jmon-test.search.yandex.net"
            assert iface.vlan == 604
            assert iface.vrf == "Hbf"

        elif iface.network_type == "fastbone":
            assert iface.name == "fb-jmon-test.search.yandex.net"
            assert iface.vlan == 761
            assert iface.vrf == "Search"

        else:
            assert False
