import json

from infra.ya_salt.hostmanager import hostmanager as hm
from infra.ya_salt.lib import saltutil
from infra.ya_salt.proto import ya_salt_pb2

GRAINS = {
    "SSDs": [
        "loop1",
        "sdd",
        "loop6",
        "loop4",
        "loop2",
        "loop0",
        "sdc",
        "md5",
        "loop7",
        "loop5",
        "loop3"
    ],
    "cpu_model": "Intel(R) Xeon(R) CPU E5-2660 v4 @ 2.00GHz",
    "cpuarch": "x86_64",
    "disks": [
        "sdb",
        "md4",
        "md2",
        "sda",
        "md3"
    ],
    "disks_info": {
        "nonssd": [
            "/dev/sda",
            "/dev/sdb"
        ],
        "nvme": [],
        "ssd": [
            "/dev/sdc",
            "/dev/sdd"
        ]
    },
    "domain": "search.yandex.net",
    "fqdn": "sas2-0317.search.yandex.net",
    "fqdn_ip4": [],
    "fqdn_ip6": [
        "2a02:6b8:c02:41d:0:604:df5:d845"
    ],
    "gcfg_tag": "tags/stable-127-r376",
    "gencfg": [
        "ALL_INFRA_PRESTABLE",
        "ALL_RTC",
        "ALL_RUNTIME",
        "ALL_SEARCH",
        "OPT_shardid=136",
        "SAS_CALLISTO_DEPLOY",
        "SAS_DISK_LUCENE",
        "SAS_IMGS_BASE",
        "SAS_IMGS_BASE_HAMSTER",
        "SAS_IMGS_BASE_NIDX",
        "SAS_IMGS_CBIR_BASE",
        "SAS_IMGS_CBIR_BASE_HAMSTER",
        "SAS_IMGS_CBIR_BASE_NIDX",
        "SAS_IMGS_LARGE_THUMB",
        "SAS_IMGS_LARGE_THUMB_NIDX",
        "SAS_IMGS_RIM_3K",
        "SAS_IMGS_RIM_3K_DEPLOY",
        "SAS_IMGS_T1_BASE",
        "SAS_IMGS_T1_BASE_NIDX",
        "SAS_IMGS_T1_CBIR_BASE",
        "SAS_IMGS_T1_CBIR_BASE_NIDX",
        "SAS_IMGS_THUMB_NEW",
        "SAS_IMGS_THUMB_NEW_NIDX",
        "SAS_JUGGLER_CLIENT_PRESTABLE",
        "SAS_JUGGLER_CLIENT_STABLE",
        "SAS_KERNEL_TEST",
        "SAS_KERNEL_UPDATE_3",
        "SAS_PSI_DYNAMIC",
        "SAS_PSI_DYNAMIC_AGENTS",
        "SAS_PSI_DYNAMIC_ROTOR",
        "SAS_PSI_YT_MASTER",
        "SAS_RTC_SLA_TENTACLES_PROD",
        "SAS_RUNTIME",
        "SAS_SEARCH",
        "SAS_VIDEO_DEPLOY",
        "SAS_VIDEO_PLATINUM_BASE",
        "SAS_VIDEO_PLATINUM_BASE_HAMSTER",
        "SAS_WEB_BASE",
        "SAS_WEB_CALLISTO_CAM_BASE",
        "SAS_WEB_DEPLOY",
        "SAS_WEB_GEMINI_BASE",
        "SAS_WEB_INT",
        "SAS_WEB_REMOTE_STORAGE_BASE",
        "SAS_WEB_TIER1_JUPITER_BASE",
        "SAS_WEB_TIER1_JUPITER_BASE_HAMSTER",
        "SAS_WEB_TIER1_JUPITER_INT",
        "SAS_WEB_TIER1_JUPITER_INT_HAMSTER",
        "SAS_YASM_YASMAGENT_PRESTABLE",
        "SAS_YASM_YASMAGENT_STABLE",
        "SAS_YT_PROD2_PORTOVM",
    ],
    "host": "sas2-0317",
    "id": "sas2-0317.search.yandex.net",
    "interfaces": [
        {
            "bandwidth": None,
            "card": None,
            "mtu": "1452",
            "name": "ip6tnl0",
            "netdev_group": "default",
            "state": "down"
        },
        {
            "bandwidth": 10000,
            "card": {
                "model": "Intel Corporation 82599 10 Gigabit Network Connection (rev 01)",
                "vendor": "intel"
            },
            "mtu": "9000",
            "name": "eth0",
            "netdev_group": "backbone",
            "state": "up"
        },
        {
            "bandwidth": 10000,
            "card": None,
            "mtu": "9000",
            "name": "vlan700",
            "netdev_group": "fastbone",
            "state": "up"
        },
        {
            "bandwidth": 10000,
            "card": None,
            "mtu": "9000",
            "name": "vlan788",
            "netdev_group": "fastbone",
            "state": "up"
        },
        {
            "bandwidth": None,
            "card": None,
            "mtu": "65536",
            "name": "lo",
            "netdev_group": "default",
            "state": "unknown"
        },
        {
            "bandwidth": 10000,
            "card": None,
            "mtu": "9000",
            "name": "vlan688",
            "netdev_group": "backbone",
            "state": "up"
        }
    ],
    "ip4_interfaces": {
        "L3-0": [],
        "L3-1": [],
        "L3-2": [],
        "L3-3": [],
        "L3-4": [],
        "L3-5": [],
        "eth0": [],
        "ip6tnl0": [],
        "lo": [
            "127.0.0.1"
        ],
        "vlan688": [],
        "vlan700": [],
        "vlan788": []
    },
    "ip6_interfaces": {
        "L3-0": [
            "fe80::14ca:cfff:fef7:c486"
        ],
        "L3-1": [
            "fe80::30c9:4eff:fee3:9874"
        ],
        "L3-2": [
            "fe80::4c9b:27ff:fede:76c8"
        ],
        "L3-3": [
            "fe80::bc04:60ff:fea6:e40f"
        ],
        "L3-4": [
            "fe80::7837:7eff:fe70:a63e"
        ],
        "L3-5": [
            "fe80::acf5:75ff:fed8:623"
        ],
        "eth0": [
            "2a02:6b8:c02:41d:0:604:df5:d845",
            "fe80::1e1b:dff:fef5:d845"
        ],
        "ip6tnl0": [],
        "lo": [
            "::1"
        ],
        "vlan688": [
            "2a02:6b8:c11:e80::badc:ab1e",
            "fe80::a:0",
            "fe80::1e1b:dff:fef5:d845"
        ],
        "vlan700": [
            "2a02:6b8:fc02:11d:0:604:df5:d845",
            "fe80::1e1b:dff:fef5:d845"
        ],
        "vlan788": [
            "2a02:6b8:fc0b:e80::badc:ab1e",
            "fe80::a:0",
            "fe80::1e1b:dff:fef5:d845"
        ]
    },
    "ip_interfaces": {
        "L3-0": [
            "fe80::14ca:cfff:fef7:c486"
        ],
        "L3-1": [
            "fe80::30c9:4eff:fee3:9874"
        ],
        "L3-2": [
            "fe80::4c9b:27ff:fede:76c8"
        ],
        "L3-3": [
            "fe80::bc04:60ff:fea6:e40f"
        ],
        "L3-4": [
            "fe80::7837:7eff:fe70:a63e"
        ],
        "L3-5": [
            "fe80::acf5:75ff:fed8:623"
        ],
        "eth0": [
            "2a02:6b8:c02:41d:0:604:df5:d845",
            "fe80::1e1b:dff:fef5:d845"
        ],
        "ip6tnl0": [],
        "lo": [
            "127.0.0.1",
            "::1"
        ],
        "vlan688": [
            "2a02:6b8:c11:e80::badc:ab1e",
            "fe80::a:0",
            "fe80::1e1b:dff:fef5:d845"
        ],
        "vlan700": [
            "2a02:6b8:fc02:11d:0:604:df5:d845",
            "fe80::1e1b:dff:fef5:d845"
        ],
        "vlan788": [
            "2a02:6b8:fc0b:e80::badc:ab1e",
            "fe80::a:0",
            "fe80::1e1b:dff:fef5:d845"
        ]
    },
    "ipv4": [
        "127.0.0.1"
    ],
    "ipv6": [
        "::1",
        "2a02:6b8:c02:41d:0:604:df5:d845",
        "2a02:6b8:c11:e80::badc:ab1e",
        "2a02:6b8:fc02:11d:0:604:df5:d845",
        "2a02:6b8:fc0b:e80::badc:ab1e",
        "fe80::a:0",
        "fe80::14ca:cfff:fef7:c486",
        "fe80::1e1b:dff:fef5:d845",
        "fe80::30c9:4eff:fee3:9874",
        "fe80::4c9b:27ff:fede:76c8",
        "fe80::7837:7eff:fe70:a63e",
        "fe80::acf5:75ff:fed8:623",
        "fe80::bc04:60ff:fea6:e40f"
    ],
    "kernel": "Linux",
    "kernelrelease": "4.19.25-3",
    "lldp": [
        {
            "port": "10GE1/0/1",
            "switch": "sas2-9s55"
        }
    ],
    "localhost": "sas2-0317.search.yandex.net",
    "location": "sas",
    "lsb_distrib_codename": "xenial",
    "lsb_distrib_description": "Ubuntu 16.04.5 LTS",
    "lsb_distrib_id": "Ubuntu",
    "lsb_distrib_release": "16.04",
    "lui": {
        "checksum": "60d151a3d9765e5ddfde6ca3637d8aa1",
        "datetime": "Sat Sep  1 17:21:39 2018",
        "name": "runtime-ubuntu-16.04",
        "project_id": "604",
        "timestamp": "1535811699"
    },
    "machine_id": "cbe46e11217e4cbfb9735e2b38be86ff",
    "manufacturer": "Yandex",
    "mdadm": [
        "md2",
        "md3",
        "md4",
        "md5"
    ],
    "mem_total": 515878,
    "nodename": "sas2-0317.search.yandex.net",
    "num_cpus": 56,
    "num_gpus": 1,
    "os": "Ubuntu",
    "os_family": "Debian",
    "osarch": "amd64",
    "oscodename": "xenial",
    "osfinger": "Ubuntu-16.04",
    "osfullname": "Ubuntu",
    "osmajorrelease": "16",
    "osrelease": "16.04",
    "osrelease_info": [
        16,
        4
    ],
    "points_info": {
        "/place": {
            "device": "/dev/md4",
            "disks_info": [
                {
                    "sdb": {
                        "disk_type": "hdd"
                    }
                },
                {
                    "sda": {
                        "disk_type": "hdd"
                    }
                }
            ],
            "mount_point": "/place",
            "raid": "raid1"
        },
        "/ssd": {
            "device": "/dev/md5",
            "disks_info": [
                {
                    "sdc": {
                        "disk_type": "ssd"
                    }
                },
                {
                    "sdd": {
                        "disk_type": "ssd"
                    }
                }
            ],
            "mount_point": "/ssd",
            "raid": "raid0"
        }
    },
    "shell": "/bin/bash",
    "uuid": "00000000-0000-0040-8000-1c1b0df5d845",
    "virtual": "physical",
    "walle_country": "ru",
    "walle_dc": "sas",
    "walle_location": "sas",
    "walle_project": "rtc-mtn",
    "walle_tags": [
        "rtc",
        "runtime",
        "search",
        "skynet_installed",
        "yasm_monitored",
        "rtc_network"
    ],
}


def test_spec_from_grains():
    spec = ya_salt_pb2.HostmanSpec()
    f = hm.spec_from_grains
    f(spec, GRAINS)
    assert json.loads(spec.salt.grains_json) == GRAINS
    assert spec.location == 'sas'
    assert spec.dc == 'sas'
    assert spec.walle_project == 'rtc-mtn'
    assert spec.walle_tags == [
        "rtc",
        "runtime",
        "search",
        "skynet_installed",
        "yasm_monitored",
        "rtc_network",
    ]
    assert spec.gencfg_groups == [
        "ALL_INFRA_PRESTABLE",
        "ALL_RTC",
        "ALL_RUNTIME",
        "ALL_SEARCH",
        "SAS_CALLISTO_DEPLOY",
        "SAS_DISK_LUCENE",
        "SAS_IMGS_BASE",
        "SAS_IMGS_BASE_HAMSTER",
        "SAS_IMGS_BASE_NIDX",
        "SAS_IMGS_CBIR_BASE",
        "SAS_IMGS_CBIR_BASE_HAMSTER",
        "SAS_IMGS_CBIR_BASE_NIDX",
        "SAS_IMGS_LARGE_THUMB",
        "SAS_IMGS_LARGE_THUMB_NIDX",
        "SAS_IMGS_RIM_3K",
        "SAS_IMGS_RIM_3K_DEPLOY",
        "SAS_IMGS_T1_BASE",
        "SAS_IMGS_T1_BASE_NIDX",
        "SAS_IMGS_T1_CBIR_BASE",
        "SAS_IMGS_T1_CBIR_BASE_NIDX",
        "SAS_IMGS_THUMB_NEW",
        "SAS_IMGS_THUMB_NEW_NIDX",
        "SAS_JUGGLER_CLIENT_PRESTABLE",
        "SAS_JUGGLER_CLIENT_STABLE",
        "SAS_KERNEL_TEST",
        "SAS_KERNEL_UPDATE_3",
        "SAS_PSI_DYNAMIC",
        "SAS_PSI_DYNAMIC_AGENTS",
        "SAS_PSI_DYNAMIC_ROTOR",
        "SAS_PSI_YT_MASTER",
        "SAS_RTC_SLA_TENTACLES_PROD",
        "SAS_RUNTIME",
        "SAS_SEARCH",
        "SAS_VIDEO_DEPLOY",
        "SAS_VIDEO_PLATINUM_BASE",
        "SAS_VIDEO_PLATINUM_BASE_HAMSTER",
        "SAS_WEB_BASE",
        "SAS_WEB_CALLISTO_CAM_BASE",
        "SAS_WEB_DEPLOY",
        "SAS_WEB_GEMINI_BASE",
        "SAS_WEB_INT",
        "SAS_WEB_REMOTE_STORAGE_BASE",
        "SAS_WEB_TIER1_JUPITER_BASE",
        "SAS_WEB_TIER1_JUPITER_BASE_HAMSTER",
        "SAS_WEB_TIER1_JUPITER_INT",
        "SAS_WEB_TIER1_JUPITER_INT_HAMSTER",
        "SAS_YASM_YASMAGENT_PRESTABLE",
        "SAS_YASM_YASMAGENT_STABLE",
        "SAS_YT_PROD2_PORTOVM",
    ]
    tests = [
        ({'gencfg': []}, 'production'),
        ({'gencfg': ['ALL_INFRA_PRESTABLE']}, 'production'),
        ({'gencfg': [], 'walle_tags': ['skynet_installed', 'rtc.stage-prestable']}, 'prestable'),
        ({'gencfg': ['SAS_RUNTIME', 'PRODUCTION']}, 'production'),
    ]
    spec = ya_salt_pb2.HostmanSpec()
    for grains, what in tests:
        spec.Clear()
        f(spec, grains)
        assert spec.env_type == what, grains


def test_fill_spec_from_repo():
    config, err = saltutil.configure_dummy_minion('test-env', 'test-hostname')
    assert err is None
    repo = saltutil.SaltRepo(meta=ya_salt_pb2.LocalRepoMeta(),
                             config=config,
                             path='/non-existing')
    spec = ya_salt_pb2.HostmanSpec()
    hm.fill_spec_from_repo(spec, repo)
    assert spec.hostname == 'test-hostname'
    assert spec.salt.environment == 'test-env'
