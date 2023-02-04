# -*- coding: utf-8 -*-
from __future__ import absolute_import, unicode_literals

import pytest

import infra.netmon.build_topology.lib.url as urllib
import infra.netmon.build_topology.lib.build_topology
import deepdiff
import json
from infra.netmon.build_topology.lib.ipnetwork import NetworkSet

IGNORE = u"this_value_is_ignored"


def _unify_strings(data):
    return json.loads(json.dumps(data))


def compareDicts(expected, actual, exclude_paths=(), **kwargs):
    result = deepdiff.DeepSearch(expected, IGNORE)
    exclude_paths = list(exclude_paths) + list(result.get("matched_values", []))
    return deepdiff.DeepDiff(_unify_strings(expected), _unify_strings(actual), exclude_paths=exclude_paths, **kwargs)


def test_network_set():
    network_list = [
        {
            "address": "37.9.96.0/26",
            "vlan": 562,
            "type": "backbone"
        },
        {
            "address": "2a02:6b8:c01:106::/64",
            "vlan": 1307,
            "type": "backbone"
        },
        {
            "address": "2a02:6b8:f000:a12::/64",
            "vlan": 761,
            "type": "fastbone"
        }
    ]
    network_set = NetworkSet([(value["address"], value) for value in network_list])
    assert network_set.get("37.9.96.1") == {
        'address': '37.9.96.0/26',
        'type': 'backbone',
        'vlan': 562
    }
    assert network_set.get("2a02:06b8:0c01:0105:ffff:ffff:ffff:ffff") is None
    assert network_set.get("2a02:06b8:0c01:0106:0000:0000:0000:0000") == {
        'address': '2a02:6b8:c01:106::/64',
        'type': 'backbone',
        'vlan': 1307
    }
    assert network_set.get("2a02:06b8:0c01:0106:ffff:ffff:ffff:ffff") == {
        'address': '2a02:6b8:c01:106::/64',
        'type': 'backbone',
        'vlan': 1307
    }
    assert network_set.get("2a02:06b8:0c01:0107:0000:0000:0000:0000") is None
    assert network_set.get("2a02:06b8:f000:0a12:0000:0000:0000:0001") == {
        'address': '2a02:6b8:f000:a12::/64',
        'type': 'fastbone',
        'vlan': 761
    }


@pytest.fixture(autouse=True)
def monkeypatch_make_request(monkeypatch):
    def callback(*args, **kwargs):
        raise RuntimeError("test shouldn't use external data")
    monkeypatch.setattr(urllib, "makeRequest", callback)


@pytest.fixture(autouse=True)
def monkeypatch_abc_load_roles(monkeypatch):
    monkeypatch.setattr(infra.netmon.build_topology.lib.build_topology, "_abc_load_roles", lambda: [
        (1, "rick")
    ])


@pytest.fixture(autouse=True)
def monkeypatch_staff_load_persons(monkeypatch):
    monkeypatch.setattr(infra.netmon.build_topology.lib.build_topology, "_staff_load_persons", lambda: [
        ("morty", "some_group")
    ])


@pytest.fixture(autouse=True)
def monkeypatch_conductor_load_hosts(monkeypatch):
    monkeypatch.setattr(infra.netmon.build_topology.lib.build_topology, "_conductor_load_hosts", lambda: [
        ("netmon.fin.adfox.net", 1)
    ])


@pytest.fixture(autouse=True)
def monkeypatch_noc_export_load_pods(monkeypatch):
    monkeypatch.setattr(infra.netmon.build_topology.lib.build_topology, "_noc_export_load_pods", lambda: [
        ("sas1-s141",  {"domain": "sas", "pod": 1}),
        ("man1-s8",   {"domain": "man", "pod": 1}),
        ("man1-s257", {"domain": "man", "pod": 67})
    ])


@pytest.fixture(autouse=True)
def monkeypatch_bot_load_hosts(monkeypatch):
    monkeypatch.setattr(infra.netmon.build_topology.lib.build_topology, "_bot_load_hosts", lambda: [
        {
            "FQDN": "sas1-0227.search.yandex.net",
            "Inv": "900172053",
            "LocationSegment1": "RU",
            "LocationSegment2": "SAS",
            "LocationSegment3": "SASTA",
            "LocationSegment4": "SAS-1.2.1",
            "LocationSegment5": "7",
            "LocationSegment6": "41",
            "LocationSegment7": "-",
            "MAC1": "0025904FF6D4",
            "MAC2": "0025904FF6D5",
            "MAC3": None,
            "MAC4": None,
            "Status": "OPERATION",
            "planner_id": "1"
        },
        {
            "FQDN": "man1-3316.search.yandex.net",
            "Inv": "100406792",
            "LocationSegment1": "FI",
            "LocationSegment2": "MANTSALA",
            "LocationSegment3": "B",
            "LocationSegment4": "MAN-1#B.1.06",
            "LocationSegment5": "1D8",
            "LocationSegment6": "1",
            "LocationSegment7": "-",
            "MAC1": "0015B2A67ECE",
            "MAC2": "0015B2A67ECF",
            "MAC3": None,
            "MAC4": None,
            "Status": "OPERATION",
            "planner_id": "1"
        },
        {
            "FQDN": "man1-3722-19.cloud.yandex.net",
            "Inv": "101086271",
            "LocationSegment1": "FI",
            "LocationSegment2": "MANTSALA",
            "LocationSegment3": "B",
            "LocationSegment4": "MAN-2#B.1.07",
            "LocationSegment5": "2D23",
            "LocationSegment6": "1",
            "LocationSegment7": "-",
            "MAC1": "FCAA14DAE25B",
            "MAC2": "FCAA14DAE25C",
            "MAC3": None,
            "MAC4": None,
            "Status": "OPERATION",
            "planner_id": None
        }
    ])


@pytest.fixture(autouse=True)
def monkeypatch_bot_load_switches(monkeypatch):
    monkeypatch.setattr(infra.netmon.build_topology.lib.build_topology, "_bot_load_switches", lambda: [
        {
            "FQDN": "sas1-s141.yndx.net",
            "InstanceNumber": "101275194",
            "LocationSegment1": "RU",
            "LocationSegment2": "SAS",
            "LocationSegment3": "SASTA",
            "LocationSegment4": "SAS-1.2.1",
            "LocationSegment5": "13",
            "LocationSegment6": "24",
            "LocationSegment7": "-"
        },
        {
            "FQDN": "man1-s8.yndx.net",
            "InstanceNumber": "100406736",
            "LocationSegment1": "FI",
            "LocationSegment2": "MANTSALA",
            "LocationSegment3": "B",
            "LocationSegment4": "MAN-1#B.1.06",
            "LocationSegment5": "1D8",
            "LocationSegment6": "25",
            "LocationSegment7": "-"
        },
        {
            "FQDN": "man1-s257.yndx.net",
            "InstanceNumber": "100330395",
            "LocationSegment1": "FI",
            "LocationSegment2": "MANTSALA",
            "LocationSegment3": "B",
            "LocationSegment4": "MAN-2#B.1.07",
            "LocationSegment5": "2D23",
            "LocationSegment6": "22",
            "LocationSegment7": "-"
        }
    ])
    monkeypatch.setattr(infra.netmon.build_topology.lib.build_topology, 'MIN_BOT_HOSTS', 3)


@pytest.fixture(autouse=True)
def monkeypatch_walle_load_hosts(monkeypatch):
    monkeypatch.setattr(infra.netmon.build_topology.lib.build_topology, "_walle_load_hosts", lambda: [
        {
            "inv": 900172053,
            "location": {
                "rack": "7",
                "short_datacenter_name": "sas",
                "short_queue_name": "sas1.2.1",
                "switch": "sas1-s141"
            },
            "macs": [
                "00:25:90:4f:f6:d4",
                "00:25:90:4f:f6:d5"
            ],
            "name": "sas1-0227.search.yandex.net",
            "state": "assigned",
            "status": "ready",
            "owners": ["@some_group"],
            "project": "with_tags"
        },
        {
            "inv": 100406792,
            "location": {
                "rack": "1D8",
                "short_datacenter_name": "man",
                "short_queue_name": "man1",
                "switch": "man1-s8"
            },
            "macs": [
                "00:15:b2:a6:7e:ce",
                "00:15:b2:a6:7e:cf",
                "f4:52:14:8c:12:e0"
            ],
            "name": "man1-3316.search.yandex.net",
            "state": "assigned",
            "status": "ready",
            "owners": [],
            "project": "with_tags"
        },
        {
            "inv": 101086271,
            "location": {
                "rack": "2D23",
                "short_datacenter_name": "man",
                "short_queue_name": "man2",
                "switch": "man1-s257"
            },
            "macs": [
                "fc:aa:14:d9:a2:5d",
                "fc:aa:14:da:e2:5b",
                "fc:aa:14:da:e2:5c"
            ],
            "name": "man1-3722-19.cloud.yandex.net",
            "state": "assigned",
            "status": "ready",
            "owners": [],
            "project": "without_tags"
        }
    ])


@pytest.fixture(autouse=True)
def monkeypatch_walle_load_projects(monkeypatch):
    monkeypatch.setattr(infra.netmon.build_topology.lib.build_topology, "_walle_load_projects", lambda: [
        {
            "id": "with_tags",
            "tags": ["walle_tag"]
        },
        {
            "id": "without_tags"
        }
    ])


@pytest.fixture(autouse=True)
def monkeypatch_walle_load_tree(monkeypatch):
    monkeypatch.setattr(infra.netmon.build_topology.lib.build_topology, "_walle_load_tree", lambda: [
        {
            "path": "RU",
            "nodes": [
                {
                    "path": "RU|SAS",
                    "nodes": [
                        {
                            "path": "RU|SAS|SASTA",
                            "nodes": [
                                {
                                    "path": "RU|SAS|SASTA|SAS-1.2.1",
                                    "nodes": [
                                        {
                                            "path": "RU|SAS|SASTA|SAS-1.2.1|7",
                                            "name": "7"
                                        }
                                    ],
                                    "name": "SAS-1.2.1",
                                    "short_name": "sas1.2.1"
                                }
                            ],
                            "name": "SASTA",
                            "short_name": "sas"
                        }
                    ],
                    "name": "SAS"
                }
            ],
            "name": "RU"
        }
    ])


@pytest.fixture(autouse=True)
def monkeypatch_racktables_load_networks(monkeypatch):
    monkeypatch.setattr(infra.netmon.build_topology.lib.build_topology, "_racktables_load_networks", lambda: [
        (
            "2a02:6b8:b000:150::/64",
            "604",
            "backbone"
        ),
        (
            "2a02:6b8:b000:6038::/64",
            "604",
            "backbone"
        ),
        (
            "2a02:6b8:b010:a508::/64",
            "1306",
            "backbone"
        ),
        (
            "2a02:6b8:c01:106::/64",
            "1307",
            "backbone"
        ),
        (
            "2a02:6b8:f000:a12::/64",
            "761",
            "fastbone"
        ),
        (
            "2a02:6b8:f000:1037::/64",
            "761",
            "fastbone"
        )
    ])


@pytest.fixture(autouse=True)
def monkeypatch_racktables_load_vrfs(monkeypatch):
    monkeypatch.setattr(infra.netmon.build_topology.lib.build_topology, "_racktables_load_vrfs", lambda: [
        (
            "2a02:6b8:b000::/48",
            "Search"
        ),
        (
            "2a02:6b8:f000::/48",
            "Search"
        )
    ])


@pytest.fixture(autouse=True)
def monkeypatch_racktables_load_switches(monkeypatch):
    monkeypatch.setattr(infra.netmon.build_topology.lib.build_topology, "_racktables_load_switches", lambda: [
        (100330395, "man1-s257"),
        (100406736, "man1-s8"),
        (101275194, "sas1-s141")
    ])


@pytest.fixture(autouse=True)
def monkeypatch_racktables_load_macros(monkeypatch):
    monkeypatch.setattr(infra.netmon.build_topology.lib.build_topology, "_racktables_load_macros", lambda: [
        ("_SEARCHSASMETANETS_", "2a02:6b8:b000:6000::/56")
    ])


@pytest.fixture(autouse=True)
def monkeypatch_racktables_load_mtn(monkeypatch):
    monkeypatch.setattr(infra.netmon.build_topology.lib.build_topology, "_racktables_load_mtn", lambda: [
        ("_ADFOX_", "1414")
    ])


@pytest.fixture(autouse=True)
def monkeypatch_netmap_load_ip(monkeypatch):
    monkeypatch.setattr(infra.netmon.build_topology.lib.build_topology, "_netmap_load_ip", lambda: [
        (
            "sas1-s141",
            "gi0/0/16",
            "0025.904f.f6d4",
            "2a02:6b8:f000:a12:225:90ff:fe4f:f6d4",
            "fb-sas1-0227.search.yandex.net"
        ),
        (
            "sas1-s141",
            "gi0/0/16",
            "0025.904f.f6d4",
            "2a02:6b8:b000:150:225:90ff:fe4f:f6d4",
            "sas1-0227.search.yandex.net"
        ),
        (
            "man1-s8",
            "40ge1/0/3:2",
            "f452.148c.12e0",
            "2a02:6b8:b000:6038:f652:14ff:fe8c:12e0",
            "man1-3316.search.yandex.net"
        ),
        (
            "man1-s8",
            "40ge1/0/3:2",
            "06fe.7644.edcd",
            "2a02:6b8:b000:6038:4fe:76ff:fe44:edcd",
            "man1-3316-29106.vm.search.yandex.net"
        ),
        (
            "man1-s257",
            "40ge1/0/1:3",
            "fcaa.14d9.a25d",
            "2a02:6b8:b010:a508:feaa:14ff:fed9:a25d",
            "man1-3722-19.cloud.yandex.net"
        ),
        (
            "man1-s257",
            "40ge1/0/1:3",
            "fa16.3eee.f64e",
            "2a02:6b8:c01:106:0:1414:0:10",
            "netmon.fin.adfox.net"
        )
    ])


@pytest.fixture(autouse=True)
def monkeypatch_netmap_load_mac(monkeypatch):
    monkeypatch.setattr(infra.netmon.build_topology.lib.build_topology, "_netmap_load_mac", lambda: [
        ("0025.904f.f6d4", "sas1-141", "gi0/0/16", 1501169735),
        ("f452.148c.12e0", "man1-s8", "40ge1/0/3:2", 1501169736),
        ("06fe.7644.edcd", "man1-s8", "40ge1/0/3:2", 1501169736),
        ("fcaa.14d9.a25d", "man1-s257", "40ge1/0/1:3", 1501169737),
        ("fa16.3eee.f64e", "man1-s257", "40ge1/0/1:3", 1501169737),
    ])


@pytest.fixture(autouse=True)
def monkeypatch_netmon_load_interfaces(monkeypatch):
    monkeypatch.setattr(infra.netmon.build_topology.lib.build_topology, "_netmon_load_interfaces", lambda: [
        (
            "fb-man1-3316.search.yandex.net",
            "2a02:6b8:f000:1037:f652:14ff:fe8c:12e0",
            "f4:52:14:8c:12:e0",
            "1501169735"
        )
    ])


def test_build_topology():
    hosts_data = infra.netmon.build_topology.lib.build_topology.build_topology(use_file_cache=False)
    assert compareDicts(hosts_data, [
        {
            "domain": ".vm.search.yandex.net",
            "name": "man1-3316-29106",
            "interfaces": [
                {
                    "switch": "man1-s8",
                    "vlan": 604,
                    "vrf": "Search",
                    "name": "man1-3316-29106",
                    "hwaddr": "6FE7644EDCD",
                    "ipv6addr": "2a02:6b8:b000:6038:4fe:76ff:fe44:edcd",
                    "network_type": "backbone",
                    "ipv4addr": "unknown",
                    "domain": ".vm.search.yandex.net",
                    "fqdn": "man1-3316-29106.vm.search.yandex.net",
                    "project_id": "unknown",
                    "macro": "_SEARCHSASMETANETS_"
                }
            ],
            "vlan": 604,
            "vrf": "Search",
            "ipv4addr": "unknown",
            "dc": "man",
            "fqdn": "man1-3316-29106.vm.search.yandex.net",
            "queue": "man1",
            "pod": "man-1",
            "children": [],
            "switch": "man1-s8",
            "invnum": "",
            "hwaddrs": [],
            "ipv6addr": "2a02:6b8:b000:6038:4fe:76ff:fe44:edcd",
            "rack": "1D8",
            "owners": ["rick"],
            "walle_project": "unknown",
            "walle_tags": [],
            "bot_short_names": ()
        },
        {
            "domain": ".search.yandex.net",
            "interfaces": [
                {
                    "switch": "man1-s8",
                    "vlan": 604,
                    "vrf": "Search",
                    "name": "man1-3316",
                    "hwaddr": "F452148C12E0",
                    "ipv6addr": "2a02:6b8:b000:6038:f652:14ff:fe8c:12e0",
                    "network_type": "backbone",
                    "ipv4addr": "unknown",
                    "domain": ".search.yandex.net",
                    "fqdn": "man1-3316.search.yandex.net",
                    "project_id": "unknown",
                    "macro": "_SEARCHSASMETANETS_"
                },
                {
                    "switch": "man1-s8",
                    "vlan": 761,
                    "vrf": "Search",
                    "name": "fb-man1-3316",
                    "hwaddr": "F452148C12E0",
                    "ipv6addr": "2a02:6b8:f000:1037:f652:14ff:fe8c:12e0",
                    "network_type": "fastbone",
                    "ipv4addr": "unknown",
                    "domain": ".search.yandex.net",
                    "fqdn": "fb-man1-3316.search.yandex.net",
                    "project_id": "unknown",
                    "macro": "unknown"
                }
            ],
            "vlan": 604,
            "vrf": "Search",
            "dc": "man",
            "invnum": "100406792",
            "hwaddrs": ["15B2A67ECE", "15B2A67ECF", "F452148C12E0"],
            "children": [
                "man1-3316-29106.vm.search.yandex.net"
            ],
            "name": "man1-3316",
            "ipv4addr": "unknown",
            "fqdn": "man1-3316.search.yandex.net",
            "queue": "man1",
            "pod": "man-1",
            "switch": "man1-s8",
            "ipv6addr": "2a02:6b8:b000:6038:f652:14ff:fe8c:12e0",
            "rack": "1D8",
            "owners": ["rick"],
            "walle_project": "with_tags",
            "walle_tags": ["walle_tag"],
            "bot_short_names": ()
        },
        {
            "domain": ".cloud.yandex.net",
            "interfaces": [
                {
                    "switch": "man1-s257",
                    "vlan": 1306,
                    "vrf": "unknown",
                    "name": "man1-3722-19",
                    "hwaddr": "FCAA14D9A25D",
                    "ipv6addr": "2a02:6b8:b010:a508:feaa:14ff:fed9:a25d",
                    "network_type": "backbone",
                    "ipv4addr": "unknown",
                    "domain": ".cloud.yandex.net",
                    "fqdn": "man1-3722-19.cloud.yandex.net",
                    "project_id": "unknown",
                    "macro": "unknown"
                }
            ],
            "vlan": 1306,
            "vrf": "unknown",
            "dc": "man",
            "invnum": "101086271",
            "hwaddrs": ["FCAA14D9A25D", "FCAA14DAE25B", "FCAA14DAE25C"],
            "children": [
                "netmon.fin.adfox.net"
            ],
            "name": "man1-3722-19",
            "ipv4addr": "unknown",
            "fqdn": "man1-3722-19.cloud.yandex.net",
            "queue": "man2",
            "pod": "man-67",
            "switch": "man1-s257",
            "ipv6addr": "2a02:6b8:b010:a508:feaa:14ff:fed9:a25d",
            "rack": "2D23",
            "owners": [],
            "walle_project": "without_tags",
            "walle_tags": [],
            "bot_short_names": ()
        },
        {
            "domain": ".fin.adfox.net",
            "name": "netmon",
            "interfaces": [
                {
                    "switch": "man1-s257",
                    "vlan": 1307,
                    "vrf": "unknown",
                    "name": "netmon",
                    "hwaddr": "FA163EEEF64E",
                    "ipv6addr": "2a02:6b8:c01:106:0:1414:0:10",
                    "network_type": "backbone",
                    "ipv4addr": "unknown",
                    "domain": ".fin.adfox.net",
                    "fqdn": "netmon.fin.adfox.net",
                    "project_id": "1414",
                    "macro": "_ADFOX_"
                }
            ],
            "vlan": 1307,
            "vrf": "unknown",
            "ipv4addr": "unknown",
            "dc": "man",
            "fqdn": "netmon.fin.adfox.net",
            "queue": "man2",
            "pod": "man-67",
            "children": [],
            "switch": "man1-s257",
            "invnum": "",
            "hwaddrs": [],
            "ipv6addr": "2a02:6b8:c01:106:0:1414:0:10",
            "rack": "2D23",
            "owners": ["rick"],
            "walle_project": "unknown",
            "walle_tags": [],
            "bot_short_names": ()
        },
        {
            "domain": ".search.yandex.net",
            "name": "sas1-0227",
            "rack": "7",
            "interfaces": [
                {
                    "switch": "sas1-s141",
                    "name": "sas1-0227",
                    "domain": ".search.yandex.net",
                    "hwaddr": "25904FF6D4",
                    "ipv4addr": "unknown",
                    "ipv6addr": "2a02:6b8:b000:150:225:90ff:fe4f:f6d4",
                    "vlan": 604,
                    "vrf": "Search",
                    "network_type": "backbone",
                    "fqdn": "sas1-0227.search.yandex.net",
                    "project_id": "unknown",
                    "macro": "unknown"
                },
                {
                    "switch": "sas1-s141",
                    "name": "fb-sas1-0227",
                    "domain": ".search.yandex.net",
                    "hwaddr": "25904FF6D4",
                    "ipv4addr": "unknown",
                    "ipv6addr": "2a02:6b8:f000:a12:225:90ff:fe4f:f6d4",
                    "vlan": 761,
                    "vrf": "Search",
                    "network_type": "fastbone",
                    "fqdn": "fb-sas1-0227.search.yandex.net",
                    "project_id": "unknown",
                    "macro": "unknown"
                }
            ],
            "vlan": 604,
            "vrf": "Search",
            "ipv4addr": "unknown",
            "dc": "sas",
            "fqdn": "sas1-0227.search.yandex.net",
            "queue": "sas1.2.1",
            "pod": "sas-1",
            "switch": "sas1-s141",
            "invnum": "900172053",
            "hwaddrs": ["25904FF6D4", "25904FF6D5"],
            "ipv6addr": "2a02:6b8:b000:150:225:90ff:fe4f:f6d4",
            "children": [],
            "owners": ["morty", "rick"],
            "walle_project": "with_tags",
            "walle_tags": ["walle_tag"],
            "bot_short_names": (u'sas', u'sas1.2.1')
        }
    ]) == {}
