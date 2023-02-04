# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import unittest
import mock

import saas.library.python.gencfg as gencfg
import saas.tools.devops.lib23.nanny_helpers as nanny_helpers


@mock.patch('infra.nanny.nanny_services_rest.nanny_services_rest.client.ServiceRepoClient', autospec=True)
class TestNannyService(unittest.TestCase):
    TEST_GENCFG_GROUPS = [{
        "release": "tags\/stable-123-r4567",
        "limits": {
            "io_policy": "normal",
            "cpu_policy": "normal"
        },
        "name": '{}_TEST_GROUP'.format(geo),
        "cpu_set_policy": "SERVICE",
        "tags": []
    } for geo in {'SAS', 'MAN', 'VLA'}]
    TEST_RUNTIME_ATTRS = {
        "content": {
            "instance_spec": {
                "networkProperties": {
                    "resolvConf": "DEFAULT_RESOLV_CONF",
                    "etcHosts": "KEEP_ETC_HOSTS"
                },
                "hostProvidedDaemons": [{"type": "YASM_AGENT"}, {"type": "HOST_SKYNET"}],
                "instancectl": {
                    "url": [
                        "rbtorrent:bf6b015527383a09c650b2374e9a579c1fdfd90c"
                    ],
                    "fetchableMeta": {
                        "type": "SANDBOX_RESOURCE",
                        "sandboxResource": {
                            "resourceType": "INSTANCECTL",
                            "resourceId": "1047997169",
                            "taskId": "474979056",
                            "taskType": ""
                        }
                    },
                    "version": "1.192"
                },
                "notifyAction": {
                    "resourceRequest": {
                        "request": [],
                        "limit": []
                    },
                    "handlers": []
                },
                "volume": [],
                "initContainers": [],
                "dockerImage": {
                    "registry": "registry.yandex.net",
                    "name": ""
                },
                "osContainerSpec": {
                    "networkProperties": {
                        "resolvConf": "DEFAULT_RESOLV_CONF",
                        "etcHosts": "KEEP_ETC_HOSTS"
                    },
                    "hostProvidedDaemons": [],
                    "instancectl": {
                        "url": [],
                        "fetchableMeta": {
                            "type": "SANDBOX_RESOURCE",
                            "sandboxResource": {
                                "resourceType": "",
                                "resourceId": "",
                                "taskId": "",
                                "taskType": ""
                            }
                        },
                        "version": ""
                    },
                    "notifyAction": {
                        "resourceRequest": {
                            "request": [],
                            "limit": []
                        },
                        "handlers": []
                    },
                    "volume": [],
                    "initContainers": [],
                    "dockerImage": {
                        "registry": "",
                        "name": ""
                    },
                    "auxDaemons": [],
                    "instanceAccess": {
                        "skynetSsh": "DISABLED"
                    },
                    "layersConfig": {
                        "bind": [],
                        "layer": []
                    },
                    "type": "NONE"
                },
                "auxDaemons": [
                    {
                        "type": "LOGROTATE"
                    }
                ],
                "instanceAccess": {
                    "skynetSsh": "ENABLED"
                },
                "qemuKvm": {
                    "layersConfig": {
                        "url": [],
                        "fetchableMeta": {
                            "type": "SANDBOX_RESOURCE",
                            "sandboxResource": {
                                "resourceType": "",
                                "resourceId": "",
                                "taskId": "",
                                "taskType": ""
                            }
                        },
                        "version": ""
                    },
                    "image": {
                        "windows": {
                            "image": {
                                "url": [],
                                "fetchableMeta": {
                                    "type": "SANDBOX_RESOURCE",
                                    "sandboxResource": {
                                        "resourceType": "",
                                        "resourceId": "",
                                        "taskId": "",
                                        "taskType": ""
                                    }
                                }
                            }
                        },
                        "type": "LINUX",
                        "linux": {
                            "image": {
                                "url": [],
                                "fetchableMeta": {
                                    "type": "SANDBOX_RESOURCE",
                                    "sandboxResource": {
                                        "resourceType": "",
                                        "resourceId": "",
                                        "taskId": "",
                                        "taskType": ""
                                    }
                                }
                            },
                            "authorizedUsers": {
                                "userList": {
                                    "logins": [],
                                    "nannyGroupIds": [],
                                    "staffGroupIds": []
                                },
                                "type": "SERVICE_MANAGERS"
                            }
                        }
                    },
                    "instancectl": {
                        "url": [],
                        "fetchableMeta": {
                            "type": "SANDBOX_RESOURCE",
                            "sandboxResource": {
                                "resourceType": "",
                                "resourceId": "",
                                "taskId": "",
                                "taskType": ""
                            }
                        },
                        "version": ""
                    },
                },
                "layersConfig": {
                    "bind": [],
                    "layer": [
                        {
                            "url": [
                                "rbtorrent:2343c8fed4d0448f4d6eca04f4e052145e2a87e3"
                            ],
                            "fetchableMeta": {
                                "type": "SANDBOX_RESOURCE",
                                "sandboxResource": {
                                    "resourceType": "PORTO_LAYER_SEARCH_UBUNTU_PRECISE_APP",
                                    "resourceId": "853070953",
                                    "taskId": "384803943",
                                    "taskType": "BUILD_PORTO_LAYER"
                                }
                            }
                        }
                    ]
                },
                "type": "SANDBOX_LAYERS",
                "id": "",
                "containers": [
                    {
                        "name": "saas_daemon",
                        "unistatEndpoints": [],
                        "hostDevices": [],
                        "reopenLogAction": {
                            "handler": {
                                "execAction": {
                                    "command": []
                                },
                                "type": "NONE",
                                "httpGet": {
                                    "host": "",
                                    "path": "",
                                    "uriScheme": "",
                                    "httpHeaders": [],
                                    "port": ""
                                },
                                "tcpSocket": {
                                    "host": "",
                                    "port": ""
                                }
                            }
                        },
                        "resourceRequest": {
                            "request": [],
                            "limit": []
                        },
                        "command": [],
                        "env": [
                            {
                                "valueFrom": {
                                    "secretEnv": {
                                        "field": "",
                                        "keychainSecret": {
                                            "keychainId": "robot-saas-yt",
                                            "secretId": "YT_TOKEN",
                                            "secretRevisionId": "28d8cbe5-4a55-4471-83d0-dc3c831f044b&initial-revision&1536670403716"
                                        },
                                        "secretName": ""
                                    },
                                    "type": "SECRET_ENV",
                                    "vaultSecretEnv": {
                                        "field": "",
                                        "vaultSecret": {
                                            "secretVer": "",
                                            "secretId": "",
                                            "delegationToken": "",
                                            "secretName": ""
                                        }
                                    },
                                    "literalEnv": {
                                        "value": ""
                                    }
                                },
                                "name": "YT_TOKEN"
                            }
                        ],
                        "readinessProbe": {
                            "periodBackoff": 0,
                            "handlers": [],
                            "initialDelaySeconds": 0,
                            "successThreshold": 0,
                            "maxPeriodSeconds": 0,
                            "minPeriodSeconds": 0,
                            "failureThreshold": 0
                        },
                        "coredumpPolicy": {
                            "coredumpProcessor": {
                                "totalSizeLimit": 102400,
                                "probability": 100,
                                "aggregator": {
                                    "type": "DISABLED",
                                    "saas": {
                                        "url": "http:\/\/cores.n.yandex-team.ru\/corecomes",
                                        "gdb": {
                                            "type": "LITERAL",
                                            "execPath": "\/usr\/bin\/gdb"
                                        },
                                        "serviceName": ""
                                    }
                                },
                                "cleanupPolicy": {
                                    "type": "TTL",
                                    "ttl": {
                                        "seconds": 360000
                                    }
                                },
                                "countLimit": 3,
                                "path": "\/cores"
                            },
                            "customProcessor": {
                                "command": ""
                            },
                            "type": "COREDUMP"
                        }
                    }
                ]
            },
            "instances": {
                "yp_pods": {
                    "orthogonal_tags": {
                        "metaprj": "unknown",
                        "itype": "unknown",
                        "ctype": "unknown",
                        "prj": "saas-cloud-market-idxapi-stratocaster"
                    },
                    "allocations": [],
                    "tags": []
                },
                "chosen_type": "EXTENDED_GENCFG_GROUPS",
                "extended_gencfg_groups": {
                    "network_settings": {
                        "hbf_nat": "enabled",
                        "use_mtn": True
                    },
                    "tags": [
                        "OPT_BSCONFIG_CTYPE=production",
                        "OPT_BSCONFIG_ITYPE=rtyserver",
                        "OPT_CUSTOM_CTYPE=stable_kv"
                    ],
                    "instance_properties_settings": {
                        "tags": "TOPOLOGY_DYNAMIC"
                    },
                    "gencfg_volumes_settings": {
                        "use_volumes": True
                    },
                    "containers_settings": {
                        "slot_porto_properties": "ALL_EXCEPT_GUARANTEES"
                    },
                    "groups": TEST_GENCFG_GROUPS,
                    "sysctl_settings": {
                        "params": []
                    }
                },
                "yp_pod_ids": {
                    "pods": [],
                    "orthogonal_tags": {
                        "metaprj": "unknown",
                        "itype": "unknown",
                        "ctype": "unknown",
                        "prj": "saas-cloud-market-idxapi-stratocaster"
                    }
                }
            },
            "engines": {
                "engine_type": "ISS_MULTI"
            },
            "resources": {
                "services_balancer_config_files": [],
                "url_files": [],
                "sandbox_files": [
                    {
                        "task_type": "BUILD_RTYSERVER",
                        "task_id": "481726845",
                        "resource_id": "1062221870",
                        "extract_path": "",
                        "is_dynamic": False,
                        "local_path": "rtyserver",
                        "resource_type": "RTYSERVER"
                    },
                    {
                        "task_type": "BUILD_RTYSERVER_CONFIG",
                        "task_id": "362046675",
                        "resource_id": "803151212",
                        "is_dynamic": False,
                        "local_path": "loop.conf",
                        "resource_type": "RTYSERVER_LOOP_CONF"
                    },
                    {
                        "task_type": "BUILD_RTYSERVER_CONFIG",
                        "task_id": "362046675",
                        "resource_id": "803151195",
                        "is_dynamic": False,
                        "local_path": "loop_data",
                        "resource_type": "RTYSERVER_LOOP_DATA"
                    },
                    {
                        "is_dynamic": False,
                        "task_id": "54475663",
                        "local_path": "dict.dict",
                        "task_type": "REMOTE_COPY_RESOURCE",
                        "resource_type": "RTYSERVER_DICT"
                    },
                    {
                        "task_type": "BUILD_RTYSERVER",
                        "task_id": "481726845",
                        "resource_id": "1062247679",
                        "is_dynamic": False,
                        "local_path": "gdb_toolkit.tgz",
                        "resource_type": "GDB_SEARCH_TOOLKIT"
                    },
                    {
                        "task_type": "BUILD_STATBOX_PUSHCLIENT",
                        "task_id": "386474916",
                        "resource_id": "856760182",
                        "is_dynamic": False,
                        "local_path": "push-client",
                        "resource_type": "STATBOX_PUSHCLIENT"
                    }
                ],
                "static_files": [
                    {
                        "is_dynamic": False,
                        "content": "count: 1",
                        "local_path": "zerodiff"
                    }
                ],
                "template_set_files": [],
                "l7_fast_balancer_config_files": []
            }
        },
        "parent_id": "5d8427592a81f77301b15638dce2dbe30171c4ca",
        "meta_info": {
            "is_disposable": False,
            "scheduling_config": {
                "scheduling_priority": "NONE"
            },
            "startrek_tickets": [],
            "conf_id": "saas_cloud_market_idxapi_stratocaster-1565626795875",
            "annotations": {
                "startable": "true",
                "deploy_engine": "ISS_MULTI"
            }
        }
    }

    def setUp(self):
        with mock.patch('saas.tools.devops.lib23.service_token.ServiceToken', autospec=True):
            self.nanny_service = nanny_helpers.NannyService('test_service', runtime_attrs=self.TEST_RUNTIME_ATTRS)

    def test_groups_with_io_limits(self, *args):
        def fake_group_fabric(limited_io=False):
            test_io_limits = {
                'hdd_io_ops_read_limit': 1000, 'hdd_io_ops_write_limit': 1000, 'hdd_io_read_limit': 10485760.0,  'hdd_io_write_limit': 10485760.0,
                'ssd_io_ops_read_limit': 1000, 'ssd_io_ops_write_limit': 1000, 'ssd_io_read_limit': 524288000.0, 'ssd_io_write_limit': 104857600.0
            }
            group_mock = mock.MagicMock(spec=gencfg.GencfgGroup)
            group_mock.io_limits = test_io_limits if limited_io else {}
            return group_mock

        self.nanny_service.is_gencfg_allocated = mock.Mock(return_value=True)
        self.nanny_service.get_gencfg_groups = mock.Mock(return_value=[fake_group_fabric(False), fake_group_fabric(False)])

        self.assertFalse(self.nanny_service.has_groups_with_io_limits())
        self.assertTrue(self.nanny_service.has_groups_without_io_limits())

        self.nanny_service.get_gencfg_groups = mock.Mock(return_value=[fake_group_fabric(True), fake_group_fabric(False)])

        self.assertTrue(self.nanny_service.has_groups_with_io_limits())
        self.assertTrue(self.nanny_service.has_groups_without_io_limits())

        self.nanny_service.get_gencfg_groups = mock.Mock(return_value=[fake_group_fabric(True), fake_group_fabric(True)])

        self.assertTrue(self.nanny_service.has_groups_with_io_limits())
        self.assertFalse(self.nanny_service.has_groups_without_io_limits())

    def test_get_gencfg_groups(self, *args):
        group_list = ['MAN_TEST_GROUP', 'SAS_TEST_GROUP', 'VLA_TEST_GROUP']
        self.nanny_service.is_gencfg_allocated = mock.Mock(return_value=True)
        self.nanny_service._runtime_attrs = self.TEST_RUNTIME_ATTRS

        self.assertListEqual(sorted(self.nanny_service.get_gencfg_groups(names_only=True)), group_list)
        gencfg_groups = sorted(self.nanny_service.get_gencfg_groups(), key=lambda g: g.name)
        self.assertTrue(all([isinstance(g, gencfg.GencfgGroup) for g in gencfg_groups]))
        self.assertListEqual([g.name for g in gencfg_groups], group_list)
        self.assertEqual(gencfg_groups[0].tag, gencfg.GencfgTag('stable-123-r4567'))
