from infra.deploy.tools.yd_migrate.lib.migrator_nanny import NannyMigrator

from google.protobuf import json_format


def test_migrate_service_abgame_mongo():
    stage, release_rules = NannyMigrator({
        "_id": "abgame_mongo",
        "auth_attrs": {
            "_id": "4f5fa1fe919bafef93aa7c2cdf083fe44ffea17d",
            "change_info": {
                "author": "redwaan",
                "comment": "Copy of production_news_mongo by redwaan",
                "ctime": 1479474320257
            },
            "content": {
                "conf_managers": {
                    "groups": [],
                    "logins": [
                        "redwaan"
                    ]
                },
                "observers": {
                    "groups": [],
                    "logins": []
                },
                "ops_managers": {
                    "groups": [],
                    "logins": [
                        "redwaan"
                    ]
                },
                "owners": {
                    "groups": [],
                    "logins": [
                        "redwaan"
                    ]
                }
            }
        },
        "current_state": {
            "_id": "",
            "content": {
                "active_snapshots": [
                    {
                        "conf_id": "abgame_mongo-1492613526640",
                        "entered": 1492613657023,
                        "prepared": {
                            "last_transition_time": 1492613587938,
                            "status": "True"
                        },
                        "snapshot_id": "8c423e66a07f0ac24e432063c28eee7ab499fe53",
                        "state": "ACTIVE",
                        "taskgroup_id": "search-0006271544",
                        "taskgroup_started": {
                            "last_transition_time": 0,
                            "message": "",
                            "reason": "",
                            "status": ""
                        }
                    },
                    {
                        "conf_id": "abgame_mongo-1487156889665",
                        "entered": 1492613677832,
                        "prepared": {
                            "last_transition_time": 1487156960320,
                            "status": "True"
                        },
                        "snapshot_id": "6b7c0375888cb276ebc8430d63ec9f6c54dbcb4f",
                        "state": "PREPARED",
                        "taskgroup_id": "search-0006271552",
                        "taskgroup_started": {
                            "last_transition_time": 0,
                            "message": "",
                            "reason": "",
                            "status": ""
                        }
                    },
                    {
                        "conf_id": "abgame_mongo-1480355176433",
                        "entered": 1487156994636,
                        "prepared": {
                            "last_transition_time": 1487156994636,
                            "status": "True"
                        },
                        "snapshot_id": "b686a155c1e33bea48352461da6c06ac0c428e5f",
                        "state": "PREPARED",
                        "taskgroup_id": "search-0004872981",
                        "taskgroup_started": {
                            "last_transition_time": 0,
                            "message": "",
                            "reason": "",
                            "status": ""
                        }
                    },
                    {
                        "conf_id": "abgame_mongo-1480331629436",
                        "entered": 1480355297586,
                        "prepared": {},
                        "snapshot_id": "2d250b72a370a7a83f76d3a89f397a6e735ffd2f",
                        "state": "PREPARED",
                        "taskgroup_id": "search-0003604946",
                        "taskgroup_started": {
                            "last_transition_time": 0,
                            "message": "",
                            "reason": "",
                            "status": ""
                        }
                    }
                ],
                "is_paused": {
                    "info": {
                        "author": "redwaan",
                        "comment": "Copy of production_news_mongo by redwaan",
                        "ctime": 1479474320257,
                        "ticket_id": ""
                    },
                    "value": False
                },
                "rollback_snapshot": {
                    "snapshot_id": "6b7c0375888cb276ebc8430d63ec9f6c54dbcb4f"
                },
                "summary": {
                    "entered": 1492613677901,
                    "value": "ONLINE"
                }
            },
            "entered": 1492613677904,
            "reallocation": {
                "id": "",
                "state": {
                    "entered": 0,
                    "message": "",
                    "reason": "",
                    "status": ""
                },
                "taskgroup_id": ""
            }
        },
        "info_attrs": {
            "_id": "f1d40f5f43e6c7006a589a6ba53ae277d0bbc514",
            "change_info": {
                "author": "alonger",
                "comment": "RUNTIMECLOUD-4428: set cms_stub_policy: SKIP",
                "ctime": 1513185101366
            },
            "content": {
                "abc_group": 0,
                "balancers_integration": {
                    "auto_update_services_balancers": False
                },
                "category": "/users/redwaan/",
                "cms_settings": {
                    "cms_stub_policy": "SKIP"
                },
                "desc": "mongodb для abgame",
                "instancectl_settings": {
                    "autoupdate_instancectl_disabled": False
                },
                "labels": [
                    {
                        "key": "ctype"
                    },
                    {
                        "key": "itype"
                    },
                    {
                        "key": "prj"
                    },
                    {
                        "key": "geo",
                        "value": "unknown"
                    }
                ],
                "monitoring_settings": {},
                "recipes": {
                    "content": [
                        {
                            "context": [
                                {
                                    "key": "operating_degrade_level",
                                    "value": "1.0"
                                }
                            ],
                            "desc": "Activate",
                            "id": "default",
                            "labels": [],
                            "name": "_activate_only_service_configuration.yaml"
                        }
                    ],
                    "prepare_recipes": []
                },
                "scheduling_policy": {
                    "type": "NONE"
                },
                "tickets_integration": {
                    "gencfg_release_rule": {
                        "filter_params": {
                            "expression": "False"
                        },
                        "responsibles": []
                    },
                    "service_release_rules": [],
                    "service_release_tickets_enabled": False
                },
                "ui_settings": {}
            },
            "parent_id": "4254daa0fa56a05b224d6637d3ca541abe35593b"
        },
        "runtime_attrs": {
            "_id": "8c423e66a07f0ac24e432063c28eee7ab499fe53",
            "change_info": {
                "author": "alonger",
                "comment": "use gencfg group SAS_IMGS_RQ_ROBOT_ABGAME_MONGO instead of hardcoded instances",
                "ctime": 1492613526640
            },
            "content": {
                "engines": {
                    "engine_type": "ISS"
                },
                "instance_spec": {},
                "instances": {
                    "chosen_type": "EXTENDED_GENCFG_GROUPS",
                    "extended_gencfg_groups": {
                        "groups": [
                            {
                                "limits": {
                                    "cpu_policy": "normal",
                                    "io_policy": "normal"
                                },
                                "name": "SAS_IMGS_RQ_ROBOT_ABGAME_MONGO",
                                "release": "tags/stable-99-r7",
                                "tags": []
                            }
                        ],
                        "sysctl_settings": {
                            "params": []
                        },
                        "tags": []
                    },
                    "gencfg_groups": [],
                    "instance_list": [],
                    "iss_settings": {
                        "hooks_resource_limits": {
                            "iss_hook_install": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_notify": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_reopenlogs": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_status": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_stop": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_uninstall": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_validate": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            }
                        },
                        "instance_cls": "ru.yandex.iss.Instance"
                    }
                },
                "resources": {
                    "l7_fast_balancer_config_files": [],
                    "sandbox_files": [
                        {
                            "is_dynamic": False,
                            "local_path": "instancectl",
                            "resource_type": "INSTANCECTL",
                            "task_id": "68060167",
                            "task_type": "BUILD_INSTANCE_CTL"
                        },
                        {
                            "is_dynamic": False,
                            "local_path": "data_dir",
                            "resource_type": "SEMIEMPTY_DIRECTORY",
                            "task_id": "49938582",
                            "task_type": "REMOTE_COPY_RESOURCE"
                        }
                    ],
                    "services_balancer_config_files": [],
                    "static_files": [
                        {
                            "content": "#!/bin/bash\n\npexit()\n{\n    echo \"$@\"\n    exit -1\n}\n\nperr()\n{\n    echo \"ERROR: $@\"\n    exit -1...",
                            "is_dynamic": True,
                            "local_path": "backup_mongodb_service.sh"
                        },
                        {
                            "content": "#!/usr/bin/env python\n\nimport argparse\nimport xmlrpclib\nimport subprocess\n__author__ =...",
                            "is_dynamic": True,
                            "local_path": "upload_resource.py"
                        },
                        {
                            "content": "2a8NCgorno84AwMjyh1X24Wz14hUwretq4sODGVR+kGEZUYjjtBelsYtHibW9OBq\nX/ssrlSc4tYNWOzbWg7SGhrp0MliAACMjpIIaLnEW84HtWiIMQ5NvALPQ7j42kEf\ni...",
                            "is_dynamic": False,
                            "local_path": "mongo_key"
                        }
                    ],
                    "template_set_files": [
                        {
                            "is_dynamic": False,
                            "layout": "# bsconfig defines following environment variables:\n#\n#   BSCONFIG_DBTOP\n#   BSCONFIG_IDIR\n#   ...",
                            "local_path": "instancectl.conf",
                            "templates": []
                        }
                    ],
                    "url_files": [
                        {
                            "is_dynamic": False,
                            "local_path": "mongodb.tgz",
                            "url": "rbtorrent:1a317e9f7eb25b54cbda740c2d71d55a3a888338"
                        }
                    ]
                }
            },
            "meta_info": {
                "annotations": {},
                "conf_id": "abgame_mongo-1492613526640",
                "is_disposable": False,
                "scheduling_config": {
                    "scheduling_priority": "NONE"
                },
                "startrek_tickets": []
            },
            "parent_id": "6b7c0375888cb276ebc8430d63ec9f6c54dbcb4f"
        },
        "target_state": {
            "_id": "",
            "content": {
                "info": {
                    "author": "alonger",
                    "comment": "-",
                    "ctime": 1492613613138,
                    "ticket_id": ""
                },
                "is_enabled": True,
                "labels": [],
                "recipe": "default",
                "recipe_parameters": [],
                "snapshot_id": "8c423e66a07f0ac24e432063c28eee7ab499fe53",
                "snapshot_info": {
                    "author": "alonger",
                    "comment": "use gencfg group SAS_IMGS_RQ_ROBOT_ABGAME_MONGO instead of hardcoded instances",
                    "ctime": 1492613526640,
                    "ticket_id": ""
                },
                "snapshot_meta_info": {
                    "annotations": {},
                    "conf_id": "abgame_mongo-1492613526640",
                    "is_disposable": False,
                    "revert_info": {
                        "last_transition_time": 0,
                        "message": "",
                        "reason": "",
                        "status": ""
                    },
                    "scheduling_config": {
                        "scheduling_priority": 0
                    },
                    "startrek_tickets": []
                },
                "snapshots": [
                    {
                        "info": {
                            "author": "alonger",
                            "comment": "Implicitly disabled to activate another",
                            "ctime": 1492613613139,
                            "ticket_id": ""
                        },
                        "labels": [],
                        "recipe_parameters": [],
                        "snapshot_id": "6b7c0375888cb276ebc8430d63ec9f6c54dbcb4f",
                        "snapshot_info": {
                            "author": "redwaan",
                            "comment": "",
                            "ctime": 1487156889665,
                            "ticket_id": ""
                        },
                        "snapshot_meta_info": {
                            "annotations": {},
                            "is_disposable": False,
                            "revert_info": {
                                "last_transition_time": 0,
                                "message": "",
                                "reason": "",
                                "status": ""
                            },
                            "scheduling_config": {
                                "scheduling_priority": 0
                            },
                            "startrek_tickets": []
                        },
                        "state": "PREPARED",
                        "tracked_tickets": {
                            "startrek_tickets": [],
                            "tickets": []
                        }
                    },
                    {
                        "info": {
                            "author": "redwaan",
                            "comment": "Implicitly disabled to activate another",
                            "ctime": 1487156899217,
                            "ticket_id": ""
                        },
                        "labels": [],
                        "recipe_parameters": [],
                        "snapshot_id": "b686a155c1e33bea48352461da6c06ac0c428e5f",
                        "snapshot_info": {
                            "author": "redwaan",
                            "comment": "fixxed broken symbols",
                            "ctime": 1480355176433,
                            "ticket_id": ""
                        },
                        "snapshot_meta_info": {
                            "annotations": {},
                            "is_disposable": False,
                            "revert_info": {
                                "last_transition_time": 0,
                                "message": "",
                                "reason": "",
                                "status": ""
                            },
                            "scheduling_config": {
                                "scheduling_priority": 0
                            },
                            "startrek_tickets": []
                        },
                        "state": "PREPARED",
                        "tracked_tickets": {
                            "startrek_tickets": [],
                            "tickets": []
                        }
                    },
                    {
                        "info": {
                            "author": "redwaan",
                            "comment": "Implicitly disabled to activate another",
                            "ctime": 1480354306317,
                            "ticket_id": ""
                        },
                        "labels": [],
                        "recipe_parameters": [],
                        "snapshot_id": "2d250b72a370a7a83f76d3a89f397a6e735ffd2f",
                        "snapshot_info": {
                            "author": "redwaan",
                            "comment": "добавил mongo_key, временно убрал аутентификацию",
                            "ctime": 1480331629436,
                            "ticket_id": ""
                        },
                        "snapshot_meta_info": {
                            "annotations": {},
                            "is_disposable": False,
                            "revert_info": {
                                "last_transition_time": 0,
                                "message": "",
                                "reason": "",
                                "status": ""
                            },
                            "scheduling_config": {
                                "scheduling_priority": 0
                            },
                            "startrek_tickets": []
                        },
                        "state": "PREPARED",
                        "tracked_tickets": {
                            "startrek_tickets": [],
                            "tickets": []
                        }
                    }
                ],
                "tracked_tickets": {
                    "startrek_tickets": [],
                    "tickets": []
                }
            },
            "info": {
                "author": "alonger",
                "comment": "-",
                "ctime": 1492613614006,
                "ticket_id": ""
            }
        }
    },
        stage_id='',
        raise_on_error=False,
        mock_clients=True,
    ).migrate()
    return {
        "stage": json_format.MessageToJson(stage),
        "release_rules": [json_format.MessageToJson(rl) for rl in release_rules]
    }


def test_migrate_service_test_danibw_hello_world():
    stage, release_rules = NannyMigrator(
        {
            "_id": "test_danibw_hello_word",
            "auth_attrs": {
                "_id": "gbglp6cyimkcloy7odeeiqkw",
                "change_info": {
                    "author": "danibw",
                    "comment": "-",
                    "ctime": 1572607891760
                },
                "content": {
                    "conf_managers": {
                        "groups": [],
                        "logins": [
                            "danibw"
                        ]
                    },
                    "observers": {
                        "groups": [],
                        "logins": []
                    },
                    "ops_managers": {
                        "groups": [],
                        "logins": [
                            "danibw"
                        ]
                    },
                    "owners": {
                        "groups": [
                            "103591"
                        ],
                        "logins": [
                            "danibw"
                        ]
                    }
                }
            },
            "current_state": {
                "_id": "",
                "content": {
                    "active_snapshots": [
                        {
                            "conf_id": "test_danibw_hello_word-1594820507773",
                            "entered": 1594820513768,
                            "prepared": {
                                "last_transition_time": 1594820513768,
                                "status": "False"
                            },
                            "snapshot_id": "9ceb8c36741f23935534993009d6f8dc71befe66",
                            "state": "GENERATING",
                            "taskgroup_id": "search-0118282454",
                            "taskgroup_started": {
                                "last_transition_time": 1594820513768,
                                "message": "",
                                "reason": "",
                                "status": "true"
                            }
                        },
                        {
                            "conf_id": "test_danibw_hello_word-1594162334640",
                            "entered": 1594820513688,
                            "prepared": {
                                "last_transition_time": 1594162401869,
                                "status": "True"
                            },
                            "snapshot_id": "f3c6d7795de7d20b28860b1f3fb2c66ec3b53b14",
                            "state": "DEACTIVATE_PENDING",
                            "taskgroup_id": "search-0117164762",
                            "taskgroup_started": {
                                "last_transition_time": 1594162403370,
                                "message": "",
                                "reason": "",
                                "status": "true"
                            }
                        },
                        {
                            "conf_id": "test_danibw_hello_word-1593425015253",
                            "entered": 1594162455639,
                            "prepared": {
                                "last_transition_time": 1593425376194,
                                "status": "True"
                            },
                            "snapshot_id": "f9853debc5bc92348cf8b4831fb6ca8f56e51955",
                            "state": "PREPARED",
                            "taskgroup_id": "search-0117164786",
                            "taskgroup_started": {
                                "last_transition_time": 1594162442956,
                                "message": "",
                                "reason": "",
                                "status": "true"
                            }
                        },
                        {
                            "conf_id": "test_danibw_hello_word-1578930670596",
                            "entered": 1593425483859,
                            "prepared": {
                                "last_transition_time": 1578930756543,
                                "status": "True"
                            },
                            "snapshot_id": "faff0207e3ea49cf413ff0b12468d479e4f8ddc1",
                            "state": "PREPARED",
                            "taskgroup_id": "search-0115906362",
                            "taskgroup_started": {
                                "last_transition_time": 1593425439819,
                                "message": "",
                                "reason": "",
                                "status": "true"
                            }
                        },
                        {
                            "conf_id": "test_danibw_hello_word-1578929655313",
                            "entered": 1578930853081,
                            "prepared": {
                                "last_transition_time": 1578929749272,
                                "status": "True"
                            },
                            "snapshot_id": "dfa5dfb2769bf9d049753400b9ab4ee4a9fc4783",
                            "state": "PREPARED",
                            "taskgroup_id": "search-0092891254",
                            "taskgroup_started": {
                                "last_transition_time": 1578930834471,
                                "message": "",
                                "reason": "",
                                "status": "true"
                            }
                        }
                    ],
                    "is_paused": {
                        "info": {
                            "author": "danibw",
                            "comment": "Initial commit",
                            "ctime": 1571745556219
                        },
                        "value": False
                    },
                    "rollback_snapshot": {
                        "snapshot_id": "f3c6d7795de7d20b28860b1f3fb2c66ec3b53b14"
                    },
                    "summary": {
                        "entered": 1594820513776,
                        "value": "PREPARING"
                    }
                },
                "entered": 1571745556336,
                "reallocation": {
                    "id": "",
                    "state": {
                        "entered": 0,
                        "message": "",
                        "reason": "",
                        "status": ""
                    },
                    "taskgroup_id": ""
                }
            },
            "info_attrs": {
                "_id": "yxuls3zn4dbfv74tkzttkkpc",
                "change_info": {
                    "author": "danibw",
                    "comment": "",
                    "ctime": 1574929465659
                },
                "content": {
                    "abc_group": 4450,
                    "balancers_integration": {
                        "auto_update_services_balancers": False
                    },
                    "category": "/users/danibw/",
                    "cms_settings": {
                        "cms_stub_policy": "SKIP"
                    },
                    "desc": "danibw hello_world service",
                    "disk_quotas": {
                        "policy": "ENABLED",
                        "root_fs_quota": 1500,
                        "work_dir_quota": 1500
                    },
                    "instancectl_settings": {
                        "autoupdate_instancectl_disabled": False
                    },
                    "labels": [],
                    "monitoring_settings": {
                        "deploy_monitoring": {
                            "content": {
                                "alert_methods": [
                                    "email"
                                ],
                                "deploy_timeout": 3600
                            },
                            "is_enabled": False
                        },
                        "juggler_settings": {
                            "content": {
                                "active_checks": [
                                    {
                                        "checks": [],
                                        "flap_detector": {},
                                        "module": {
                                            "logic_or": {
                                                "mode": "CRIT"
                                            },
                                            "type": "logic_or"
                                        },
                                        "passive_checks": [
                                            {
                                                "juggler_host_name": "test_danibw_hello_word",
                                                "juggler_service_name": "test_danibw_hw_check",
                                                "notifications": [],
                                                "options": {
                                                    "args": [],
                                                    "env_vars": []
                                                }
                                            }
                                        ],
                                        "per_auto_tags": []
                                    }
                                ],
                                "instance_resolve_type": "NANNY",
                                "juggler_hosts": [
                                    {
                                        "golem_responsible": {
                                            "groups": [],
                                            "logins": [
                                                "danibw"
                                            ]
                                        },
                                        "name": "test_danibw_hello_word"
                                    }
                                ],
                                "juggler_tags": []
                            },
                            "is_enabled": True
                        },
                        "panels": {
                            "juggler": [],
                            "quentao": [],
                            "yasm": []
                        }
                    },
                    "recipes": {
                        "content": [
                            {
                                "context": [],
                                "desc": "Activate",
                                "id": "default",
                                "labels": [],
                                "name": "_activate_only_service_configuration.yaml"
                            }
                        ],
                        "prepare_recipes": [
                            {
                                "context": [],
                                "desc": "Prepare",
                                "id": "default",
                                "labels": [],
                                "name": "_prepare_service_configuration.yaml"
                            }
                        ]
                    },
                    "scheduling_policy": {
                        "type": "NONE"
                    },
                    "tickets_integration": {
                        "service_release_rules": [],
                        "service_release_tickets_enabled": False
                    },
                    "ui_settings": {}
                },
                "parent_id": "ffdqikwwituymo2puvg7hfvs"
            },
            "runtime_attrs": {
                "_id": "9ceb8c36741f23935534993009d6f8dc71befe66",
                "change_info": {
                    "author": "yanddmi",
                    "comment": "coredump",
                    "ctime": 1594820507773
                },
                "content": {
                    "engines": {
                        "engine_type": "YP_LITE"
                    },
                    "instance_spec": {
                        "auxDaemons": [
                            {
                                "jugglerAgent": {
                                    "resourceRequest": {
                                        "limit": []
                                    }
                                },
                                "type": "JUGGLER_AGENT"
                            }
                        ],
                        "containers": [
                            {
                                "command": [
                                    "bash",
                                    "-c",
                                    "sleep 10000"
                                ],
                                "coredumpPolicy": {
                                    "coredumpProcessor": {
                                        "aggregator": {
                                            "saas": {
                                                "gdb": {
                                                    "execPath": "/usr/bin/gdb",
                                                    "type": "LITERAL"
                                                },
                                                "serviceName": "danibw_test",
                                                "url": "http://cores.n.yandex-team.ru/corecomes"
                                            },
                                            "type": "SAAS_AGGREGATOR"
                                        },
                                        "cleanupPolicy": {
                                            "ttl": {
                                                "seconds": 3600
                                            },
                                            "type": "TTL"
                                        },
                                        "countLimit": 3,
                                        "path": "/cores",
                                        "probability": 100,
                                        "totalSizeLimit": 10240
                                    },
                                    "customProcessor": {
                                        "command": ""
                                    },
                                    "type": "COREDUMP"
                                },
                                "env": [
                                    {
                                        "name": "owner",
                                        "valueFrom": {
                                            "literalEnv": {
                                                "value": "danibw"
                                            },
                                            "secretEnv": {
                                                "field": "",
                                                "keychainSecret": {
                                                    "keychainId": "",
                                                    "secretId": "",
                                                    "secretRevisionId": ""
                                                },
                                                "secretName": ""
                                            },
                                            "type": "LITERAL_ENV",
                                            "vaultSecretEnv": {
                                                "field": "",
                                                "vaultSecret": {
                                                    "delegationToken": "",
                                                    "secretId": "",
                                                    "secretName": "",
                                                    "secretVer": ""
                                                }
                                            }
                                        }
                                    },
                                    {
                                        "name": "YP_TOKEN",
                                        "valueFrom": {
                                            "literalEnv": {
                                                "value": ""
                                            },
                                            "secretEnv": {
                                                "field": "",
                                                "keychainSecret": {
                                                    "keychainId": "",
                                                    "secretId": "",
                                                    "secretRevisionId": ""
                                                },
                                                "secretName": ""
                                            },
                                            "type": "VAULT_SECRET_ENV",
                                            "vaultSecretEnv": {
                                                "field": "YP_TOKEN",
                                                "vaultSecret": {
                                                    "delegationToken": "ophcxAqKtViuLIAWMUxSUb4Y3huaWrPDgd0rsnAbmn4.1.c1673cdc729decc8",
                                                    "secretId": "sec-01de9vdj14mxc0ch2ssj1hf5z6",
                                                    "secretName": "yp_service_controller",
                                                    "secretVer": "ver-01de9yzm3qc4cmwxvsk03zb1ah"
                                                }
                                            }
                                        }
                                    },
                                    {
                                        "name": "TEST_KEY",
                                        "valueFrom": {
                                            "literalEnv": {
                                                "value": ""
                                            },
                                            "secretEnv": {
                                                "field": "test_key",
                                                "keychainSecret": {
                                                    "keychainId": "nanny-yd-migration-test",
                                                    "secretId": "secret-id",
                                                    "secretRevisionId": "94baa4f7-bada-4303-8e1b-50634144cb14&test&1593424994697"
                                                },
                                                "secretName": ""
                                            },
                                            "type": "SECRET_ENV",
                                            "vaultSecretEnv": {
                                                "field": "",
                                                "vaultSecret": {
                                                    "delegationToken": "",
                                                    "secretId": "",
                                                    "secretName": "",
                                                    "secretVer": ""
                                                }
                                            }
                                        }
                                    }
                                ],
                                "hostDevices": [],
                                "lifecycle": {
                                    "preStop": {
                                        "execAction": {
                                            "command": [
                                                "/bin/sh",
                                                "-c",
                                                "exit 0"
                                            ]
                                        },
                                        "httpGet": {
                                            "host": "",
                                            "httpHeaders": [],
                                            "path": "",
                                            "port": "",
                                            "uriScheme": ""
                                        },
                                        "tcpSocket": {
                                            "host": "",
                                            "port": ""
                                        },
                                        "type": "EXEC"
                                    },
                                    "stopGracePeriodSeconds": 0,
                                    "termBarrier": "IGNORE",
                                    "terminationGracePeriodSeconds": 10
                                },
                                "name": "test_danibw_hello_word-1",
                                "readinessProbe": {
                                    "handlers": [
                                        {
                                            "execAction": {
                                                "command": [
                                                    "/bin/sh",
                                                    "-c",
                                                    "exit 0"
                                                ]
                                            },
                                            "httpGet": {
                                                "host": "",
                                                "httpHeaders": [],
                                                "path": "",
                                                "port": "",
                                                "uriScheme": "HTTP"
                                            },
                                            "tcpSocket": {
                                                "host": "",
                                                "port": ""
                                            },
                                            "type": "EXEC"
                                        }
                                    ],
                                    "initialDelaySeconds": 5,
                                    "maxPeriodSeconds": 60,
                                    "minPeriodSeconds": 5,
                                    "periodBackoff": 2
                                },
                                "reopenLogAction": {
                                    "handler": {
                                        "execAction": {
                                            "command": []
                                        },
                                        "httpGet": {
                                            "host": "",
                                            "httpHeaders": [],
                                            "path": "",
                                            "port": "",
                                            "uriScheme": ""
                                        },
                                        "tcpSocket": {
                                            "host": "",
                                            "port": ""
                                        },
                                        "type": "NONE"
                                    }
                                },
                                "resourceRequest": {
                                    "limit": []
                                },
                                "restartPolicy": {
                                    "maxPeriodSeconds": 60,
                                    "minPeriodSeconds": 1,
                                    "periodBackoff": 2,
                                    "periodJitterSeconds": 20
                                },
                                "securityPolicy": {
                                    "runAsUser": ""
                                },
                                "unistatEndpoints": [
                                    {
                                        "path": "/unistat",
                                        "port": "80"
                                    }
                                ]
                            }
                        ],
                        "dockerImage": {
                            "name": "",
                            "registry": "registry.yandex.net"
                        },
                        "hostProvidedDaemons": [
                            {
                                "type": "HOST_SKYNET"
                            }
                        ],
                        "id": "",
                        "initContainers": [
                            {
                                "command": [
                                    "/bin/sh",
                                    "-c",
                                    "bash -c \"sleep 1\""
                                ],
                                "name": "test_danibw_hello_word-2"
                            }
                        ],
                        "instanceAccess": {
                            "skynetSsh": "ENABLED"
                        },
                        "instancectl": {
                            "fetchableMeta": {
                                "sandboxResource": {
                                    "resourceId": "1536388832",
                                    "resourceType": "INSTANCECTL",
                                    "taskId": "689687702",
                                    "taskType": ""
                                },
                                "type": "SANDBOX_RESOURCE"
                            },
                            "url": [
                                "rbtorrent:0d685dcfed98821c292b90c962a8aab5aeebcbed"
                            ],
                            "version": "2.1"
                        },
                        "layersConfig": {
                            "bind": [],
                            "layer": [
                                {
                                    "fetchableMeta": {
                                        "sandboxResource": {
                                            "resourceId": "1199437063",
                                            "resourceType": "PORTO_LAYER_SEARCH_UBUNTU_BIONIC",
                                            "taskId": "542939289",
                                            "taskType": "BUILD_PORTO_LAYER"
                                        },
                                        "type": "SANDBOX_RESOURCE"
                                    },
                                    "url": [
                                        "rbtorrent:4b92cd7b2182df255acea24b309972ec748ec696"
                                    ]
                                }
                            ]
                        },
                        "networkProperties": {
                            "etcHosts": "KEEP_ETC_HOSTS",
                            "resolvConf": "USE_NAT64"
                        },
                        "notifyAction": {
                            "handlers": [],
                            "resourceRequest": {
                                "limit": [],
                                "request": []
                            }
                        },
                        "osContainerSpec": {
                            "auxDaemons": [],
                            "dockerImage": {
                                "name": "",
                                "registry": ""
                            },
                            "hostProvidedDaemons": [],
                            "initContainers": [],
                            "instanceAccess": {
                                "skynetSsh": "DISABLED"
                            },
                            "instancectl": {
                                "fetchableMeta": {
                                    "sandboxResource": {
                                        "resourceId": "1126674332",
                                        "resourceType": "INSTANCECTL",
                                        "taskId": "510350972",
                                        "taskType": ""
                                    },
                                    "type": "SANDBOX_RESOURCE"
                                },
                                "url": [
                                    "rbtorrent:fc184755054cbf861231c8b2e8df29dbe3036840"
                                ],
                                "version": "1.196"
                            },
                            "layersConfig": {
                                "bind": [],
                                "layer": [
                                    {
                                        "fetchableMeta": {
                                            "sandboxResource": {
                                                "resourceId": "703602428",
                                                "resourceType": "PORTO_LAYER_SEARCH_UBUNTU_PRECISE_APP",
                                                "taskId": "313732989",
                                                "taskType": "BUILD_PORTO_LAYER"
                                            },
                                            "type": "SANDBOX_RESOURCE"
                                        },
                                        "url": [
                                            "rbtorrent:4eccf4fdbd83d58dc290d550ac82969724c53945"
                                        ]
                                    }
                                ]
                            },
                            "networkProperties": {
                                "etcHosts": "KEEP_ETC_HOSTS",
                                "resolvConf": "DEFAULT_RESOLV_CONF"
                            },
                            "notifyAction": {
                                "handlers": [],
                                "resourceRequest": {
                                    "limit": [],
                                    "request": []
                                }
                            },
                            "type": "SANDBOX_LAYERS",
                            "volume": []
                        },
                        "qemuKvm": {
                            "image": {
                                "linux": {
                                    "authorizedUsers": {
                                        "type": "SERVICE_MANAGERS",
                                        "userList": {
                                            "logins": [],
                                            "nannyGroupIds": [],
                                            "staffGroupIds": []
                                        }
                                    },
                                    "image": {
                                        "fetchableMeta": {
                                            "sandboxResource": {
                                                "resourceId": "409331232",
                                                "resourceType": "QEMU_IMAGE",
                                                "taskId": "178867853",
                                                "taskType": "REMOTE_COPY_RESOURCE"
                                            },
                                            "type": "SANDBOX_RESOURCE"
                                        },
                                        "url": [
                                            "rbtorrent:59f6fc1ff7efd59a8f37547dc4a4baebfa8af023"
                                        ]
                                    }
                                },
                                "type": "LINUX",
                                "windows": {
                                    "image": {
                                        "fetchableMeta": {
                                            "sandboxResource": {
                                                "resourceId": "",
                                                "resourceType": "",
                                                "taskId": "",
                                                "taskType": ""
                                            },
                                            "type": "SANDBOX_RESOURCE"
                                        },
                                        "url": []
                                    }
                                }
                            },
                            "instancectl": {
                                "fetchableMeta": {
                                    "sandboxResource": {
                                        "resourceId": "1126674332",
                                        "resourceType": "INSTANCECTL",
                                        "taskId": "510350972",
                                        "taskType": ""
                                    },
                                    "type": "SANDBOX_RESOURCE"
                                },
                                "url": [
                                    "rbtorrent:fc184755054cbf861231c8b2e8df29dbe3036840"
                                ],
                                "version": "1.196"
                            },
                            "layersConfig": {
                                "fetchableMeta": {
                                    "sandboxResource": {
                                        "resourceId": "562598663",
                                        "resourceType": "PORTO_LAYER_SEARCH_QEMU_UBUNTU_XENIAL",
                                        "taskId": "248826138",
                                        "taskType": ""
                                    },
                                    "type": "SANDBOX_RESOURCE"
                                },
                                "url": [
                                    "rbtorrent:882c02f072d0c4d4bbf556fe50ceec626e795446"
                                ],
                                "version": "vmagent_last"
                            },
                            "vmagent": {
                                "fetchableMeta": {
                                    "sandboxResource": {
                                        "resourceId": "947964256",
                                        "resourceType": "VMAGENT_PACK",
                                        "taskId": "428833900",
                                        "taskType": ""
                                    },
                                    "type": "SANDBOX_RESOURCE"
                                },
                                "url": [
                                    "rbtorrent:278507f66246cc9f02a7ffae6242496d0929a096"
                                ],
                                "version": "0.20"
                            }
                        },
                        "type": "SANDBOX_LAYERS",
                        "volume": [
                            {
                                "itsVolume": {
                                    "itsUrl": "http://its.yandex-team.ru/v1",
                                    "maxRetryPeriodSeconds": 300,
                                    "periodSeconds": 60
                                },
                                "name": "prepare",
                                "secretVolume": {
                                    "keychainSecret": {
                                        "keychainId": "",
                                        "secretId": "",
                                        "secretRevisionId": ""
                                    },
                                    "secretName": ""
                                },
                                "templateVolume": {
                                    "template": [
                                        {
                                            "dstPath": "config.yaml",
                                            "srcPath": "config_template.yaml"
                                        }
                                    ]
                                },
                                "type": "TEMPLATE",
                                "vaultSecretVolume": {
                                    "vaultSecret": {
                                        "delegationToken": "",
                                        "secretId": "",
                                        "secretName": "",
                                        "secretVer": ""
                                    }
                                },
                                "version": ""
                            },
                            {
                                "itsVolume": {
                                    "itsUrl": "http://its.yandex-team.ru/v1",
                                    "maxRetryPeriodSeconds": 300,
                                    "periodSeconds": 60
                                },
                                "name": "my_secrets",
                                "secretVolume": {
                                    "keychainSecret": {
                                        "keychainId": "",
                                        "secretId": "",
                                        "secretRevisionId": ""
                                    },
                                    "secretName": ""
                                },
                                "templateVolume": {
                                    "template": []
                                },
                                "type": "VAULT_SECRET",
                                "vaultSecretVolume": {
                                    "vaultSecret": {
                                        "delegationToken": "VTjjG0K4BB4OYUdtr34W8i22VLX5ua79ZnPBcOnaxB8.1.e9439cc4c5f3b52a",
                                        "secretId": "sec-01de9vdj14mxc0ch2ssj1hf5z6",
                                        "secretName": "yp_service_controller",
                                        "secretVer": "ver-01de9yzm3qc4cmwxvsk03zb1ah"
                                    }
                                },
                                "version": ""
                            },
                            {
                                "itsVolume": {
                                    "itsUrl": "http://its.yandex-team.ru/v1",
                                    "maxRetryPeriodSeconds": 300,
                                    "periodSeconds": 60
                                },
                                "name": "nanny_vault",
                                "secretVolume": {
                                    "keychainSecret": {
                                        "keychainId": "nanny-yd-migration-test",
                                        "secretId": "secret-id",
                                        "secretRevisionId": "94baa4f7-bada-4303-8e1b-50634144cb14&test&1593424994697"
                                    },
                                    "secretName": ""
                                },
                                "templateVolume": {
                                    "template": []
                                },
                                "type": "SECRET",
                                "vaultSecretVolume": {
                                    "vaultSecret": {
                                        "delegationToken": "",
                                        "secretId": "",
                                        "secretName": "",
                                        "secretVer": ""
                                    }
                                },
                                "version": ""
                            }
                        ]
                    },
                    "instances": {
                        "chosen_type": "YP_POD_IDS",
                        "extended_gencfg_groups": {
                            "containers_settings": {
                                "slot_porto_properties": "NONE"
                            },
                            "groups": [],
                            "instance_properties_settings": {
                                "tags": "ALL_STATIC"
                            },
                            "network_settings": {
                                "hbf_nat": "enabled"
                            },
                            "tags": []
                        },
                        "gencfg_groups": [],
                        "instance_list": [],
                        "iss_settings": {
                            "hooks_resource_limits": {
                                "iss_hook_install": {
                                    "cpu_policy": "normal",
                                    "io_policy": "normal"
                                },
                                "iss_hook_notify": {
                                    "cpu_policy": "normal",
                                    "io_policy": "normal"
                                },
                                "iss_hook_reopenlogs": {
                                    "cpu_policy": "normal",
                                    "io_policy": "normal"
                                },
                                "iss_hook_status": {
                                    "cpu_policy": "normal",
                                    "io_policy": "normal"
                                },
                                "iss_hook_stop": {
                                    "cpu_policy": "normal",
                                    "io_policy": "normal"
                                },
                                "iss_hook_uninstall": {
                                    "cpu_policy": "normal",
                                    "io_policy": "normal"
                                },
                                "iss_hook_validate": {
                                    "cpu_policy": "normal",
                                    "io_policy": "normal"
                                }
                            },
                            "instance_cls": "ru.yandex.iss.Instance"
                        },
                        "yp_pod_ids": {
                            "orthogonal_tags": {
                                "ctype": "unknown",
                                "itype": "unknown",
                                "metaprj": "unknown",
                                "prj": "test-danibw-hello-word"
                            },
                            "pods": [
                                {
                                    "cluster": "SAS",
                                    "pod_id": "test-danibw-hello-word-1"
                                }
                            ]
                        },
                        "yp_pods": {
                            "allocations": [],
                            "orthogonal_tags": {
                                "ctype": "unknown",
                                "itype": "unknown",
                                "metaprj": "unknown",
                                "prj": "default-content-service"
                            },
                            "tags": []
                        }
                    },
                    "resources": {
                        "l7_fast_balancer_config_files": [],
                        "sandbox_files": [
                            {
                                "extract_path": "",
                                "is_dynamic": False,
                                "local_path": "test_resource",
                                "resource_id": "1198993879",
                                "resource_type": "SAMOGON_BUNDLE",
                                "task_id": "542785615",
                                "task_type": "HTTP_UPLOAD_2"
                            },
                            {
                                "extract_path": "{JUGGLER_CHECKS_PATH}/juggler_checks.tar.gz",
                                "is_dynamic": False,
                                "local_path": "juggler_checks.tar.gz",
                                "resource_id": "1230434244",
                                "resource_type": "JUGGLER_CHECKS_BUNDLE",
                                "task_id": "556664721",
                                "task_type": "BUILD_JUGGLER_CHECKS_BUNDLE"
                            }
                        ],
                        "services_balancer_config_files": [],
                        "static_files": [
                            {
                                "content": "my_data",
                                "is_dynamic": False,
                                "local_path": "my_static_file.txt"
                            },
                            {
                                "content": "port: {{ BSCONFIG_IPORT }}\n",
                                "is_dynamic": False,
                                "local_path": "config_template.yaml"
                            }
                        ],
                        "template_set_files": [],
                        "url_files": []
                    }
                },
                "meta_info": {
                    "annotations": {
                        "deploy_engine": "YP_LITE",
                        "startable": "true"
                    },
                    "conf_id": "test_danibw_hello_word-1594820507773",
                    "infra_notifications": {},
                    "is_disposable": False,
                    "scheduling_config": {
                        "scheduling_priority": "NONE"
                    },
                    "startrek_tickets": []
                },
                "parent_id": "f3c6d7795de7d20b28860b1f3fb2c66ec3b53b14"
            },
            "target_state": {
                "_id": "",
                "content": {
                    "info": {
                        "author": "yanddmi",
                        "comment": "-",
                        "ctime": 1594820513665
                    },
                    "is_enabled": True,
                    "labels": [],
                    "prepare_recipe": "default",
                    "recipe": "default",
                    "recipe_parameters": [],
                    "snapshot_id": "9ceb8c36741f23935534993009d6f8dc71befe66",
                    "snapshot_info": {
                        "author": "yanddmi",
                        "comment": "coredump",
                        "ctime": 1594820507773
                    },
                    "snapshot_meta_info": {
                        "annotations": {
                            "deploy_engine": "YP_LITE",
                            "startable": "true"
                        },
                        "conf_id": "test_danibw_hello_word-1594820507773",
                        "is_disposable": False,
                        "revert_info": {
                            "last_transition_time": 0,
                            "message": "",
                            "reason": "",
                            "status": ""
                        },
                        "scheduling_config": {
                            "scheduling_priority": 0
                        },
                        "startrek_tickets": []
                    },
                    "snapshots": [
                        {
                            "info": {
                                "author": "yanddmi",
                                "comment": "Implicitly disabled to activate another",
                                "ctime": 1578930676602
                            },
                            "labels": [],
                            "prepare_recipe": "default",
                            "recipe": "default",
                            "recipe_parameters": [],
                            "snapshot_id": "dfa5dfb2769bf9d049753400b9ab4ee4a9fc4783",
                            "snapshot_info": {
                                "author": "yanddmi",
                                "comment": "",
                                "ctime": 1578929655313
                            },
                            "snapshot_meta_info": {
                                "annotations": {
                                    "deploy_engine": "YP_LITE",
                                    "startable": "true"
                                },
                                "conf_id": "test_danibw_hello_word-1578929655313",
                                "is_disposable": False,
                                "revert_info": {
                                    "last_transition_time": 0,
                                    "message": "",
                                    "reason": "",
                                    "status": ""
                                },
                                "scheduling_config": {
                                    "scheduling_priority": 0
                                },
                                "startrek_tickets": []
                            },
                            "state": "PREPARED",
                            "tracked_tickets": {
                                "startrek_tickets": [],
                                "tickets": []
                            }
                        },
                        {
                            "info": {
                                "author": "danibw",
                                "comment": "Implicitly disabled to activate another",
                                "ctime": 1593425302380
                            },
                            "labels": [],
                            "prepare_recipe": "default",
                            "recipe": "default",
                            "recipe_parameters": [],
                            "snapshot_id": "faff0207e3ea49cf413ff0b12468d479e4f8ddc1",
                            "snapshot_info": {
                                "author": "yanddmi",
                                "comment": "",
                                "ctime": 1578930670596
                            },
                            "snapshot_meta_info": {
                                "annotations": {
                                    "deploy_engine": "YP_LITE",
                                    "startable": "true"
                                },
                                "conf_id": "test_danibw_hello_word-1578930670596",
                                "is_disposable": False,
                                "revert_info": {
                                    "last_transition_time": 0,
                                    "message": "",
                                    "reason": "",
                                    "status": ""
                                },
                                "scheduling_config": {
                                    "scheduling_priority": 0
                                },
                                "startrek_tickets": []
                            },
                            "state": "PREPARED",
                            "tracked_tickets": {
                                "startrek_tickets": [],
                                "tickets": []
                            }
                        },
                        {
                            "info": {
                                "author": "danibw",
                                "comment": "Implicitly disabled to activate another",
                                "ctime": 1594162339106
                            },
                            "labels": [],
                            "prepare_recipe": "default",
                            "recipe": "default",
                            "recipe_parameters": [],
                            "snapshot_id": "f9853debc5bc92348cf8b4831fb6ca8f56e51955",
                            "snapshot_info": {
                                "author": "danibw",
                                "comment": "Update secret nanny-yd-migration-test/secret-id revision (batch change from keychain UI)",
                                "ctime": 1593425015253
                            },
                            "snapshot_meta_info": {
                                "annotations": {
                                    "deploy_engine": "YP_LITE",
                                    "startable": "true"
                                },
                                "conf_id": "test_danibw_hello_word-1593425015253",
                                "is_disposable": False,
                                "revert_info": {
                                    "last_transition_time": 0,
                                    "message": "",
                                    "reason": "",
                                    "status": ""
                                },
                                "startrek_tickets": []
                            },
                            "state": "PREPARED",
                            "tracked_tickets": {
                                "startrek_tickets": [],
                                "tickets": []
                            }
                        },
                        {
                            "info": {
                                "author": "yanddmi",
                                "comment": "Implicitly disabled to activate another",
                                "ctime": 1594820513666
                            },
                            "labels": [],
                            "prepare_recipe": "default",
                            "recipe": "default",
                            "recipe_parameters": [],
                            "snapshot_id": "f3c6d7795de7d20b28860b1f3fb2c66ec3b53b14",
                            "snapshot_info": {
                                "author": "danibw",
                                "comment": "",
                                "ctime": 1594162334640
                            },
                            "snapshot_meta_info": {
                                "annotations": {
                                    "deploy_engine": "YP_LITE",
                                    "startable": "true"
                                },
                                "conf_id": "test_danibw_hello_word-1594162334640",
                                "is_disposable": False,
                                "revert_info": {
                                    "last_transition_time": 0,
                                    "message": "",
                                    "reason": "",
                                    "status": ""
                                },
                                "scheduling_config": {
                                    "scheduling_priority": 0
                                },
                                "startrek_tickets": []
                            },
                            "state": "PREPARED",
                            "tracked_tickets": {
                                "startrek_tickets": [],
                                "tickets": []
                            }
                        }
                    ],
                    "tracked_tickets": {
                        "startrek_tickets": [],
                        "tickets": []
                    }
                },
                "info": {
                    "author": "yanddmi",
                    "comment": "-",
                    "ctime": 1594820513666
                }
            },
            "unique_id_index": "test-danibw-hello-word"
        },
        stage_id='',
        raise_on_error=False,
        mock_clients=True,
    ).migrate()
    return {
        "stage": json_format.MessageToJson(stage),
        "release_rules": [json_format.MessageToJson(rl) for rl in release_rules]
    }


def test_migrate_service_prestable_market_fmcg_main_man():
    stage, release_rules = NannyMigrator({
        "_id": "prestable_market_fmcg_main_man",
        "auth_attrs": {
            "_id": "24tolrp555sac3owtps74hg5",
            "change_info": {
                "author": "valter",
                "comment": "-",
                "ctime": 1565620505577
            },
            "content": {
                "conf_managers": {
                    "groups": [],
                    "logins": [
                        "nanny-robot",
                        "robot-market-infra"
                    ]
                },
                "observers": {
                    "groups": [],
                    "logins": []
                },
                "ops_managers": {
                    "groups": [],
                    "logins": [
                        "nanny-robot",
                        "robot-market-infra",
                        "robot-mrkt-rtc"
                    ]
                },
                "owners": {
                    "groups": [
                        "41953",
                        "113748",
                        "92199"
                    ],
                    "logins": [
                        "d3rp",
                        "andreevdm"
                    ]
                }
            }
        },
        "current_state": {
            "_id": "",
            "content": {
                "active_snapshots": [
                    {
                        "conf_id": "prestable_market_fmcg_main_man-1578994127732",
                        "entered": 1578994588818,
                        "prepared": {
                            "last_transition_time": 1578994338023,
                            "status": "True"
                        },
                        "snapshot_id": "eee395fb3d21d8a0505f7828f4f177bce0c2b7ee",
                        "state": "ACTIVE",
                        "taskgroup_id": "search-0092971451",
                        "taskgroup_started": {
                            "last_transition_time": 1578994340447,
                            "message": "",
                            "reason": "",
                            "status": "true"
                        }
                    },
                    {
                        "conf_id": "prestable_market_fmcg_main_man-1578923160286",
                        "entered": 1578994604578,
                        "prepared": {
                            "last_transition_time": 1578923393738,
                            "status": "True"
                        },
                        "snapshot_id": "8963ff5d6becc7566c2f2d8ece9f6bdda323907c",
                        "state": "PREPARED",
                        "taskgroup_id": "search-0092971789",
                        "taskgroup_started": {
                            "last_transition_time": 1578994589154,
                            "message": "",
                            "reason": "",
                            "status": "true"
                        }
                    },
                    {
                        "conf_id": "prestable_market_fmcg_main_man-1578898575508",
                        "entered": 1578923695400,
                        "prepared": {
                            "last_transition_time": 1578898752879,
                            "status": "True"
                        },
                        "snapshot_id": "0d9aed657ef05a14bfaa2fdbdcdcb6a9c70e63c5",
                        "state": "PREPARED",
                        "taskgroup_id": "search-0092879357",
                        "taskgroup_started": {
                            "last_transition_time": 1578923679451,
                            "message": "",
                            "reason": "",
                            "status": "true"
                        }
                    },
                    {
                        "conf_id": "prestable_market_fmcg_main_man-1578571790026",
                        "entered": 1578899013396,
                        "prepared": {
                            "last_transition_time": 1578571976703,
                            "status": "True"
                        },
                        "snapshot_id": "7c2eb5404e5261125d6612e16b78cfd6df9c4a42",
                        "state": "PREPARED",
                        "taskgroup_id": "search-0092830228",
                        "taskgroup_started": {
                            "last_transition_time": 1578899000840,
                            "message": "",
                            "reason": "",
                            "status": "true"
                        }
                    }
                ],
                "is_paused": {
                    "info": {
                        "author": "robot-mrkt-rtc",
                        "comment": "copy testing_market_template_service_for_java_iva to prestable_market_fmcg_main_man",
                        "ctime": 1563806098357
                    },
                    "value": False
                },
                "rollback_snapshot": {
                    "snapshot_id": "8963ff5d6becc7566c2f2d8ece9f6bdda323907c"
                },
                "summary": {
                    "entered": 1578994610872,
                    "value": "ONLINE"
                }
            },
            "entered": 1563806099527,
            "reallocation": {
                "id": "",
                "state": {
                    "entered": 0,
                    "message": "",
                    "reason": "",
                    "status": ""
                },
                "taskgroup_id": ""
            }
        },
        "info_attrs": {
            "_id": "dmt4f655hamb3ooesulcerk5",
            "change_info": {
                "author": "slyder",
                "comment": "CSADMIN-25765",
                "ctime": 1573297794904
            },
            "content": {
                "abc_group": 0,
                "balancers_integration": {
                    "auto_update_services_balancers": False
                },
                "category": "/market/fmcg/main/prestable/",
                "cms_settings": {
                    "cms_stub_policy": "SKIP"
                },
                "desc": "Market service",
                "disk_quotas": {
                    "policy": "ENABLED",
                    "root_fs_quota": 5000,
                    "work_dir_quota": 5000
                },
                "instancectl_settings": {
                    "autoupdate_instancectl_disabled": False
                },
                "labels": [
                    {
                        "key": "geo"
                    },
                    {
                        "key": "ctype"
                    },
                    {
                        "key": "itype"
                    },
                    {
                        "key": "prj"
                    }
                ],
                "monitoring_settings": {
                    "deploy_monitoring": {
                        "is_enabled": False
                    },
                    "juggler_settings": {
                        "is_enabled": False
                    },
                    "panels": {
                        "juggler": [],
                        "quentao": [],
                        "yasm": []
                    }
                },
                "queue_id": "MARKET",
                "recipes": {
                    "content": [
                        {
                            "context": [
                                {
                                    "key": "operating_degrade_level",
                                    "value": "0.1"
                                },
                                {
                                    "key": "stop_degrade_level",
                                    "value": "0.1"
                                }
                            ],
                            "desc": "Activate",
                            "id": "default",
                            "labels": [],
                            "name": "_activate_only_service_configuration.yaml"
                        }
                    ],
                    "prepare_recipes": [
                        {
                            "context": [
                                {
                                    "key": "operating_degrade_level",
                                    "value": "1"
                                },
                                {
                                    "key": "stop_degrade_level",
                                    "value": "0.1"
                                }
                            ],
                            "desc": "Prepare",
                            "id": "default",
                            "labels": [],
                            "name": "_prepare_service_configuration.yaml"
                        }
                    ]
                },
                "scheduling_policy": {
                    "based_on_snapshot_priority": {
                        "activate_recipe": "default",
                        "prepare_recipe": "default"
                    },
                    "type": "BASED_ON_SNAPSHOT_PRIORITY"
                },
                "tickets_integration": {
                    "docker_release_rule": {
                        "match_release_type": "-",
                        "responsibles": []
                    },
                    "gencfg_release_rule": {
                        "filter_params": {
                            "expression": "True"
                        },
                        "queue_id": "MARKET",
                        "responsibles": []
                    },
                    "instancectl_release_rule": {
                        "auto_commit_settings": {},
                        "match_release_type": "-",
                        "queue_id": "INSTANCECTL",
                        "responsibles": []
                    },
                    "service_release_rules": [
                        {
                            "approve_policy": {
                                "type": "NONE"
                            },
                            "auto_commit_settings": {
                                "enabled": False,
                                "scheduling_priority": "NORMAL"
                            },
                            "desc": "Application",
                            "filter_params": {
                                "expression": "sandbox_release.release_type in (\"prestable\",)"
                            },
                            "queue_id": "MARKET",
                            "responsibles": [],
                            "sandbox_resource_type": "MARKET_FMCG_MAIN_APP",
                            "ticket_priority": "NORMAL"
                        },
                        {
                            "approve_policy": {
                                "type": "NONE"
                            },
                            "auto_commit_settings": {
                                "enabled": True
                            },
                            "desc": "Config",
                            "filter_params": {
                                "expression": "sandbox_release.release_type in (\"prestable\",)"
                            },
                            "queue_id": "MARKET",
                            "responsibles": [],
                            "sandbox_resource_type": "MARKET_COMMON_CONFIG",
                            "sandbox_task_type": "MARKET_YA_PACKAGE",
                            "ticket_priority": "NORMAL"
                        },
                        {
                            "approve_policy": {
                                "type": "NONE"
                            },
                            "auto_commit_settings": {
                                "enabled": True,
                                "scheduling_priority": "NORMAL"
                            },
                            "desc": "Datasources",
                            "filter_params": {
                                "expression": "sandbox_release.release_type in (\"prestable\",)"
                            },
                            "queue_id": "MARKET",
                            "responsibles": [],
                            "sandbox_resource_type": "MARKET_DATASOURCES_PRESTABLE",
                            "sandbox_task_type": "BUILD_MARKET_DATASOURCES",
                            "ticket_priority": "NORMAL"
                        },
                        {
                            "approve_policy": {
                                "type": "NONE"
                            },
                            "auto_commit_settings": {
                                "enabled": True,
                                "scheduling_priority": "NONE"
                            },
                            "desc": "juggler checks",
                            "filter_params": {
                                "expression": "sandbox_release.release_type in (\"prestable\",)"
                            },
                            "queue_id": "MARKET",
                            "responsibles": [],
                            "sandbox_resource_type": "MARKET_JUGGLER_RTC_CHECKS_BUNDLE",
                            "sandbox_task_type": "BUILD_JUGGLER_CHECKS_BUNDLE",
                            "ticket_priority": "NORMAL"
                        },
                        {
                            "auto_commit_settings": {
                                "enabled": False,
                                "scheduling_priority": "NORMAL"
                            },
                            "desc": "java",
                            "filter_params": {
                                "expression": "sandbox_release.release_type in (\"prestable\",)"
                            },
                            "queue_id": "MARKET",
                            "responsibles": [],
                            "sandbox_resource_type": "YANDEX_JDK_SET",
                            "ticket_priority": "NORMAL"
                        }
                    ],
                    "service_release_tickets_enabled": True
                },
                "ui_settings": {
                    "set_snapshot_as_current_on_activate": "True"
                },
                "yp_settings": {
                    "mirror_to_yp": "SKIP"
                }
            },
            "parent_id": "yrhe6rigjnoccbmxhgwvddv2"
        },
        "runtime_attrs": {
            "_id": "eee395fb3d21d8a0505f7828f4f177bce0c2b7ee",
            "change_info": {
                "author": "robot-market-infra",
                "comment": "Release from pipeline: https://tsum.yandex-team.ru/pipe/projects/fmcg/delivery-dashboard/market-fmcg/release/5e1c8ea264971b5fc50317ee",
                "ctime": 1578994127732
            },
            "content": {
                "engines": {
                    "engine_type": "ISS_MAN"
                },
                "instance_spec": {
                    "auxDaemons": [
                        {
                            "type": "JUGGLER_AGENT"
                        }
                    ],
                    "containers": [
                        {
                            "command": [
                                "bash",
                                "-c",
                                "mkdir -p data/{INSTANCECTL_CONTAINER}; mkdir -p /var/logs/yandex/{INSTANCECTL_CONTAINER} ;"
                                " /usr/sbin/nginx -p {BSCONFIG_IDIR}/conf/nginx -c {BSCONFIG_IDIR}/conf/nginx/nginx.conf"
                            ],
                            "coredumpPolicy": {
                                "coredumpProcessor": {
                                    "aggregator": {
                                        "saas": {
                                            "gdb": {
                                                "execPath": "/usr/bin/gdb",
                                                "type": "LITERAL"
                                            },
                                            "serviceName": "",
                                            "url": "http://cores.n.yandex-team.ru/corecomes"
                                        },
                                        "type": "DISABLED"
                                    },
                                    "cleanupPolicy": {
                                        "ttl": {
                                            "seconds": 3600
                                        },
                                        "type": "DISABLED"
                                    },
                                    "countLimit": 3,
                                    "path": "/cores",
                                    "probability": 100,
                                    "totalSizeLimit": 10240
                                },
                                "customProcessor": {
                                    "command": ""
                                },
                                "type": "NONE"
                            },
                            "env": [],
                            "hostDevices": [],
                            "lifecycle": {
                                "preStop": {
                                    "execAction": {
                                        "command": []
                                    },
                                    "httpGet": {
                                        "host": "",
                                        "httpHeaders": [],
                                        "path": "",
                                        "port": "",
                                        "uriScheme": ""
                                    },
                                    "tcpSocket": {
                                        "host": "",
                                        "port": ""
                                    },
                                    "type": "NONE"
                                },
                                "stopGracePeriodSeconds": 0,
                                "termBarrier": "IGNORE",
                                "terminationGracePeriodSeconds": 10
                            },
                            "name": "nginx",
                            "readinessProbe": {
                                "failureThreshold": 0,
                                "handlers": [
                                    {
                                        "execAction": {
                                            "command": []
                                        },
                                        "httpGet": {
                                            "host": "",
                                            "httpHeaders": [],
                                            "path": "",
                                            "port": "",
                                            "uriScheme": "HTTP"
                                        },
                                        "tcpSocket": {
                                            "host": "",
                                            "port": "{BSCONFIG_IPORT}"
                                        },
                                        "type": "TCP_SOCKET"
                                    }
                                ],
                                "initialDelaySeconds": 5,
                                "maxPeriodSeconds": 60,
                                "minPeriodSeconds": 5,
                                "periodBackoff": 2,
                                "successThreshold": 0
                            },
                            "reopenLogAction": {
                                "handler": {
                                    "execAction": {
                                        "command": []
                                    },
                                    "httpGet": {
                                        "host": "",
                                        "httpHeaders": [],
                                        "path": "",
                                        "port": "",
                                        "uriScheme": ""
                                    },
                                    "tcpSocket": {
                                        "host": "",
                                        "port": ""
                                    },
                                    "type": "NONE"
                                }
                            },
                            "resourceRequest": {
                                "limit": [],
                                "request": []
                            },
                            "restartPolicy": {
                                "maxPeriodSeconds": 60,
                                "minPeriodSeconds": 1,
                                "periodBackoff": 2,
                                "periodJitterSeconds": 20,
                                "type": "ALWAYS"
                            },
                            "unistatEndpoints": []
                        },
                        {
                            "command": [
                                "/usr/bin/rfsd",
                                "-p",
                                "{BSCONFIG_IPORT_PLUS_5}",
                                "-q",
                                "-f",
                                "-e",
                                "conf/rfs-exports.conf",
                                "-u",
                                "root"
                            ],
                            "coredumpPolicy": {
                                "coredumpProcessor": {
                                    "aggregator": {
                                        "saas": {
                                            "gdb": {
                                                "execPath": "/usr/bin/gdb",
                                                "type": "LITERAL"
                                            },
                                            "serviceName": "",
                                            "url": "http://cores.n.yandex-team.ru/corecomes"
                                        },
                                        "type": "DISABLED"
                                    },
                                    "cleanupPolicy": {
                                        "ttl": {
                                            "seconds": 3600
                                        },
                                        "type": "DISABLED"
                                    },
                                    "countLimit": 3,
                                    "path": "/cores",
                                    "probability": 100,
                                    "totalSizeLimit": 10240
                                },
                                "customProcessor": {
                                    "command": ""
                                },
                                "type": "NONE"
                            },
                            "env": [],
                            "hostDevices": [],
                            "lifecycle": {
                                "preStop": {
                                    "execAction": {
                                        "command": []
                                    },
                                    "httpGet": {
                                        "host": "",
                                        "httpHeaders": [],
                                        "path": "",
                                        "port": "",
                                        "uriScheme": ""
                                    },
                                    "tcpSocket": {
                                        "host": "",
                                        "port": ""
                                    },
                                    "type": "NONE"
                                },
                                "stopGracePeriodSeconds": 0,
                                "termBarrier": "IGNORE",
                                "terminationGracePeriodSeconds": 10
                            },
                            "name": "rfsd",
                            "readinessProbe": {
                                "failureThreshold": 0,
                                "handlers": [
                                    {
                                        "execAction": {
                                            "command": []
                                        },
                                        "httpGet": {
                                            "host": "",
                                            "httpHeaders": [],
                                            "path": "",
                                            "port": "",
                                            "uriScheme": "HTTP"
                                        },
                                        "tcpSocket": {
                                            "host": "",
                                            "port": "{BSCONFIG_IPORT_PLUS_5}"
                                        },
                                        "type": "TCP_SOCKET"
                                    }
                                ],
                                "initialDelaySeconds": 5,
                                "maxPeriodSeconds": 60,
                                "minPeriodSeconds": 5,
                                "periodBackoff": 2,
                                "successThreshold": 0
                            },
                            "reopenLogAction": {
                                "handler": {
                                    "execAction": {
                                        "command": []
                                    },
                                    "httpGet": {
                                        "host": "",
                                        "httpHeaders": [],
                                        "path": "",
                                        "port": "",
                                        "uriScheme": ""
                                    },
                                    "tcpSocket": {
                                        "host": "",
                                        "port": ""
                                    },
                                    "type": "NONE"
                                }
                            },
                            "resourceRequest": {
                                "limit": [],
                                "request": []
                            },
                            "restartPolicy": {
                                "maxPeriodSeconds": 60,
                                "minPeriodSeconds": 1,
                                "periodBackoff": 2,
                                "periodJitterSeconds": 20,
                                "type": "ALWAYS"
                            },
                            "unistatEndpoints": []
                        },
                        {
                            "command": [
                                "/bin/bash",
                                "bin/logrotate.sh",
                                "1800"
                            ],
                            "coredumpPolicy": {
                                "coredumpProcessor": {
                                    "aggregator": {
                                        "saas": {
                                            "gdb": {
                                                "execPath": "/usr/bin/gdb",
                                                "type": "LITERAL"
                                            },
                                            "serviceName": "",
                                            "url": "http://cores.n.yandex-team.ru/corecomes"
                                        },
                                        "type": "DISABLED"
                                    },
                                    "cleanupPolicy": {
                                        "ttl": {
                                            "seconds": 3600
                                        },
                                        "type": "DISABLED"
                                    },
                                    "countLimit": 3,
                                    "path": "/cores",
                                    "probability": 100,
                                    "totalSizeLimit": 10240
                                },
                                "customProcessor": {
                                    "command": ""
                                },
                                "type": "NONE"
                            },
                            "env": [],
                            "hostDevices": [],
                            "lifecycle": {
                                "preStop": {
                                    "execAction": {
                                        "command": []
                                    },
                                    "httpGet": {
                                        "host": "",
                                        "httpHeaders": [],
                                        "path": "",
                                        "port": "",
                                        "uriScheme": ""
                                    },
                                    "tcpSocket": {
                                        "host": "",
                                        "port": ""
                                    },
                                    "type": "NONE"
                                },
                                "stopGracePeriodSeconds": 0,
                                "termBarrier": "IGNORE",
                                "terminationGracePeriodSeconds": 10
                            },
                            "name": "logrotate",
                            "readinessProbe": {
                                "failureThreshold": 0,
                                "handlers": [
                                    {
                                        "execAction": {
                                            "command": [
                                                "/bin/sh",
                                                "-c",
                                                "pgrep -f 'logrotate.sh' > /dev/null"
                                            ]
                                        },
                                        "httpGet": {
                                            "host": "",
                                            "httpHeaders": [],
                                            "path": "",
                                            "port": "",
                                            "uriScheme": "HTTP"
                                        },
                                        "tcpSocket": {
                                            "host": "",
                                            "port": ""
                                        },
                                        "type": "EXEC"
                                    }
                                ],
                                "initialDelaySeconds": 5,
                                "maxPeriodSeconds": 60,
                                "minPeriodSeconds": 5,
                                "periodBackoff": 2,
                                "successThreshold": 0
                            },
                            "reopenLogAction": {
                                "handler": {
                                    "execAction": {
                                        "command": []
                                    },
                                    "httpGet": {
                                        "host": "",
                                        "httpHeaders": [],
                                        "path": "",
                                        "port": "",
                                        "uriScheme": ""
                                    },
                                    "tcpSocket": {
                                        "host": "",
                                        "port": ""
                                    },
                                    "type": "NONE"
                                }
                            },
                            "resourceRequest": {
                                "limit": [],
                                "request": []
                            },
                            "restartPolicy": {
                                "maxPeriodSeconds": 60,
                                "minPeriodSeconds": 1,
                                "periodBackoff": 2,
                                "periodJitterSeconds": 20,
                                "type": "ALWAYS"
                            },
                            "unistatEndpoints": []
                        },
                        {
                            "command": [
                                "bash",
                                "-c",
                                "mkdir -p /var/logs/yandex/{INSTANCECTL_CONTAINER} ;"
                                " bin/{INSTANCECTL_CONTAINER}-start.sh --logdir=/var/logs/yandex/{INSTANCECTL_CONTAINER} --httpport={BSCONFIG_IPORT_PLUS_1}"
                                " --debugport={BSCONFIG_IPORT_PLUS_2} --tmpdir={BSCONFIG_IDIR}/tmp --datadir={BSCONFIG_IDIR}/pdata --extdatadir={BSCONFIG_IDIR}/data-getter --environment={a_ctype}"
                            ],
                            "coredumpPolicy": {
                                "coredumpProcessor": {
                                    "aggregator": {
                                        "saas": {
                                            "gdb": {
                                                "execPath": "/usr/bin/gdb",
                                                "type": "LITERAL"
                                            },
                                            "serviceName": "",
                                            "url": "http://cores.n.yandex-team.ru/corecomes"
                                        },
                                        "type": "DISABLED"
                                    },
                                    "cleanupPolicy": {
                                        "ttl": {
                                            "seconds": 3600
                                        },
                                        "type": "DISABLED"
                                    },
                                    "countLimit": 3,
                                    "path": "/cores",
                                    "probability": 100,
                                    "totalSizeLimit": 10240
                                },
                                "customProcessor": {
                                    "command": ""
                                },
                                "type": "NONE"
                            },
                            "env": [
                                {
                                    "name": "market.fmcg.yt.username",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "market.fmcg.yt.username",
                                            "keychainSecret": {
                                                "keychainId": "market_fmcg_production",
                                                "secretId": "yt_secret",
                                                "secretRevisionId": "824829a9-7119-4539-afeb-be1a7f733c0a&initial-revision&1533025505071"
                                            },
                                            "secretName": ""
                                        },
                                        "type": "SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "",
                                            "vaultSecret": {
                                                "delegationToken": "",
                                                "secretId": "",
                                                "secretName": "",
                                                "secretVer": ""
                                            }
                                        }
                                    }
                                },
                                {
                                    "name": "market.fmcg.yt.password",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "market.fmcg.yt.password",
                                            "keychainSecret": {
                                                "keychainId": "market_fmcg_production",
                                                "secretId": "yt_secret",
                                                "secretRevisionId": "824829a9-7119-4539-afeb-be1a7f733c0a&initial-revision&1533025505071"
                                            },
                                            "secretName": ""
                                        },
                                        "type": "SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "",
                                            "vaultSecret": {
                                                "delegationToken": "",
                                                "secretId": "",
                                                "secretName": "",
                                                "secretVer": ""
                                            }
                                        }
                                    }
                                },
                                {
                                    "name": "market.fmcg.mds.accessKey",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "market.fmcg.mds.accessKey",
                                            "keychainSecret": {
                                                "keychainId": "market_fmcg_production",
                                                "secretId": "mds_secret",
                                                "secretRevisionId": "bdc31b8b-955f-4148-9f43-358d943b96e5&initial-revision&1533025613897"
                                            },
                                            "secretName": ""
                                        },
                                        "type": "SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "",
                                            "vaultSecret": {
                                                "delegationToken": "",
                                                "secretId": "",
                                                "secretName": "",
                                                "secretVer": ""
                                            }
                                        }
                                    }
                                },
                                {
                                    "name": "market.fmcg.mds.secretKey",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "market.fmcg.mds.secretKey",
                                            "keychainSecret": {
                                                "keychainId": "market_fmcg_production",
                                                "secretId": "mds_secret",
                                                "secretRevisionId": "bdc31b8b-955f-4148-9f43-358d943b96e5&initial-revision&1533025613897"
                                            },
                                            "secretName": ""
                                        },
                                        "type": "SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "",
                                            "vaultSecret": {
                                                "delegationToken": "",
                                                "secretId": "",
                                                "secretName": "",
                                                "secretVer": ""
                                            }
                                        }
                                    }
                                },
                                {
                                    "name": "market.fmcg.xiva.token.send",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "market.fmcg.xiva.token.send",
                                            "keychainSecret": {
                                                "keychainId": "market_fmcg_production",
                                                "secretId": "xiva_secret",
                                                "secretRevisionId": "4089f7e9-9a73-4400-abed-a4ffecc7f743&initial-revision&1535641309921"
                                            },
                                            "secretName": ""
                                        },
                                        "type": "SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "",
                                            "vaultSecret": {
                                                "delegationToken": "",
                                                "secretId": "",
                                                "secretName": "",
                                                "secretVer": ""
                                            }
                                        }
                                    }
                                },
                                {
                                    "name": "market.fmcg.xiva.token.subscribe",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "market.fmcg.xiva.token.subscribe",
                                            "keychainSecret": {
                                                "keychainId": "market_fmcg_production",
                                                "secretId": "xiva_secret",
                                                "secretRevisionId": "4089f7e9-9a73-4400-abed-a4ffecc7f743&initial-revision&1535641309921"
                                            },
                                            "secretName": ""
                                        },
                                        "type": "SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "",
                                            "vaultSecret": {
                                                "delegationToken": "",
                                                "secretId": "",
                                                "secretName": "",
                                                "secretVer": ""
                                            }
                                        }
                                    }
                                },
                                {
                                    "name": "market.fmcg.tracker.oauth",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "market.fmcg.tracker.oauth",
                                            "keychainSecret": {
                                                "keychainId": "market_fmcg_production",
                                                "secretId": "tracker_secret",
                                                "secretRevisionId": "123c94ee-2657-4c82-bf1b-5f0ae04c598a&initial-revision&1545745203851"
                                            },
                                            "secretName": ""
                                        },
                                        "type": "SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "",
                                            "vaultSecret": {
                                                "delegationToken": "",
                                                "secretId": "",
                                                "secretName": "",
                                                "secretVer": ""
                                            }
                                        }
                                    }
                                },
                                {
                                    "name": "market.fmcg.vkusvill.token",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "",
                                            "keychainSecret": {
                                                "keychainId": "",
                                                "secretId": "",
                                                "secretRevisionId": ""
                                            },
                                            "secretName": ""
                                        },
                                        "type": "VAULT_SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "token",
                                            "vaultSecret": {
                                                "delegationToken": "eDnKvNHHGpLMEPHcArOe606CNncQ2_a_QpzPzjATZyI.1.aa271294cd197c6a",
                                                "secretId": "sec-01deymhd80w9fer2n6r74scm5d",
                                                "secretName": "supercheck-vkusvill",
                                                "secretVer": "ver-01deymhd8adgp3pdc7vmbmhkh6"
                                            }
                                        }
                                    }
                                },
                                {
                                    "name": "market.fmcg.vkusvill.salt",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "",
                                            "keychainSecret": {
                                                "keychainId": "",
                                                "secretId": "",
                                                "secretRevisionId": ""
                                            },
                                            "secretName": ""
                                        },
                                        "type": "VAULT_SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "salt",
                                            "vaultSecret": {
                                                "delegationToken": "3UHNQhnL2_xPAJDu4zl5W44XhYKcU_rljVXP6MzPZjg.1.e99628a013936e02",
                                                "secretId": "sec-01deymhd80w9fer2n6r74scm5d",
                                                "secretName": "supercheck-vkusvill",
                                                "secretVer": "ver-01deymhd8adgp3pdc7vmbmhkh6"
                                            }
                                        }
                                    }
                                },
                                {
                                    "name": "market.fmcg.db.password",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "",
                                            "keychainSecret": {
                                                "keychainId": "",
                                                "secretId": "",
                                                "secretRevisionId": ""
                                            },
                                            "secretName": ""
                                        },
                                        "type": "VAULT_SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "password",
                                            "vaultSecret": {
                                                "delegationToken": "idL1EWzMcWjwbz_slJRCL5y-opDDQ_STkSVd5Fn7xjQ.1.210cbedccc41f15b",
                                                "secretId": "sec-01df1etgdhnadwe558f9y3m3q3",
                                                "secretName": "market_fmcg_production.pgaas",
                                                "secretVer": "ver-01df377n9hpctbd2qe8js2wfv7"
                                            }
                                        }
                                    }
                                },
                                {
                                    "name": "market.fmcg.trust.token",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "",
                                            "keychainSecret": {
                                                "keychainId": "",
                                                "secretId": "",
                                                "secretRevisionId": ""
                                            },
                                            "secretName": ""
                                        },
                                        "type": "VAULT_SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "market.fmcg.trust.token",
                                            "vaultSecret": {
                                                "delegationToken": "73Top3MU7Fhg3n9Ku2W6OFAOCrhB_-8B77NzfdZ3PXc.1.de15a9c6dd40106f",
                                                "secretId": "sec-01djzm2hhsvd3k5154ecad5vg0",
                                                "secretName": "supercheck_trust",
                                                "secretVer": "ver-01djzm2hj8frzrkvmdrfngn6nt"
                                            }
                                        }
                                    }
                                },
                                {
                                    "name": "market.fmcg.tvm.secret",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "",
                                            "keychainSecret": {
                                                "keychainId": "",
                                                "secretId": "",
                                                "secretRevisionId": ""
                                            },
                                            "secretName": ""
                                        },
                                        "type": "VAULT_SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "supercheck.tvm.secret.production",
                                            "vaultSecret": {
                                                "delegationToken": "VNzVtpZzSwn5kJDSnHWV0oKGhfxWph6WEXsFagACi88.1.8455e736aaeb1476",
                                                "secretId": "sec-01dkc1c856870hn0zjy2xxds4v",
                                                "secretName": "supercheck-tvm",
                                                "secretVer": "ver-01dkc1c86zmvnkas9qjcv8n93r"
                                            }
                                        }
                                    }
                                },
                                {
                                    "name": "market.fmcg.appmetrica.events.apikey",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "",
                                            "keychainSecret": {
                                                "keychainId": "",
                                                "secretId": "",
                                                "secretRevisionId": ""
                                            },
                                            "secretName": ""
                                        },
                                        "type": "VAULT_SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "post-apikey",
                                            "vaultSecret": {
                                                "delegationToken": "k5u7aMjy_bZgfnp94SA1eicAHsmK6VVCtfKK9cQ3EBg.1.7f6f4d3eba9ccb4a",
                                                "secretId": "sec-01dmnqye774pa0tjkzzqqvq7fg",
                                                "secretName": "appmetrica-prod",
                                                "secretVer": "ver-01dmnqye7f3y7vdbpfe3wzfxsn"
                                            }
                                        }
                                    }
                                },
                                {
                                    "name": "market.fmcg.ulybka.eshop.key",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "",
                                            "keychainSecret": {
                                                "keychainId": "",
                                                "secretId": "",
                                                "secretRevisionId": ""
                                            },
                                            "secretName": ""
                                        },
                                        "type": "VAULT_SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "key",
                                            "vaultSecret": {
                                                "delegationToken": "CMwfmoTeurRHS5ZvLt52GOUTq_UfDui5WNWxCcgGzPE.1.c1334d06ebad02e4",
                                                "secretId": "sec-01dnexzw5ke4hvr4v4vdrnbg2j",
                                                "secretName": "ulybka-radugi-eshop-key",
                                                "secretVer": "ver-01dw76m6d7189cvcrsxf66dk02"
                                            }
                                        }
                                    }
                                },
                                {
                                    "name": "market.fmcg.ulybka.order.token",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "",
                                            "keychainSecret": {
                                                "keychainId": "",
                                                "secretId": "",
                                                "secretRevisionId": ""
                                            },
                                            "secretName": ""
                                        },
                                        "type": "VAULT_SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "push_token",
                                            "vaultSecret": {
                                                "delegationToken": "ZJ8TmeoRXQmbHYfw9a3UrWQyjm-9XpjwwmkDpsbFUNM.1.4721af4548e78c8c",
                                                "secretId": "sec-01dnh8gh7n19g9yrfyb00f00tg",
                                                "secretName": "ulybka",
                                                "secretVer": "ver-01dnh8gh891r601aw3n05rsf2v"
                                            }
                                        }
                                    }
                                },
                                {
                                    "name": "market.fmcg.ulybka.username",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "",
                                            "keychainSecret": {
                                                "keychainId": "",
                                                "secretId": "",
                                                "secretRevisionId": ""
                                            },
                                            "secretName": ""
                                        },
                                        "type": "VAULT_SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "market.fmcg.ulybka.username",
                                            "vaultSecret": {
                                                "delegationToken": "AcNUMu2b95ZEhKN3p0__u-sdNVePRIP1o689b0K-ljU.1.d7f343dd25c3ae5b",
                                                "secretId": "sec-01dnh8gh7n19g9yrfyb00f00tg",
                                                "secretName": "ulybka",
                                                "secretVer": "ver-01dsqfsqjqdtkswrrgg5rp472f"
                                            }
                                        }
                                    }
                                },
                                {
                                    "name": "market.fmcg.ulybka.password",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "",
                                            "keychainSecret": {
                                                "keychainId": "",
                                                "secretId": "",
                                                "secretRevisionId": ""
                                            },
                                            "secretName": ""
                                        },
                                        "type": "VAULT_SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "market.fmcg.ulybka.password",
                                            "vaultSecret": {
                                                "delegationToken": "kR8EGRUuzEM5f1SSUFFcHFFFbtjfR5EN9w6FpxiMcL4.1.40d36981b995d68b",
                                                "secretId": "sec-01dnh8gh7n19g9yrfyb00f00tg",
                                                "secretName": "ulybka",
                                                "secretVer": "ver-01dw7kaxtkkbwnyrnynb61hr44"
                                            }
                                        }
                                    }
                                },
                                {
                                    "name": "market.fmcg.sender.token",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "",
                                            "secretName": ""
                                        },
                                        "type": "VAULT_SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "market.fmcg.sender.token",
                                            "vaultSecret": {
                                                "delegationToken": "_Nw-ciJ8aaRefSXoE4B-UtAmiqQS2Nl7RH8Dllhtn9U.1.ddb75c2497b07b29",
                                                "secretId": "sec-01dy4wv9a0qpkahhy00ccfxpsr",
                                                "secretName": "yandex-sender-token",
                                                "secretVer": "ver-01dy4wv9aew25wnr0nzma5yndy"
                                            }
                                        }
                                    }
                                }
                            ],
                            "hostDevices": [],
                            "lifecycle": {
                                "preStop": {
                                    "execAction": {
                                        "command": []
                                    },
                                    "httpGet": {
                                        "host": "",
                                        "httpHeaders": [],
                                        "path": "/close",
                                        "port": "",
                                        "uriScheme": ""
                                    },
                                    "tcpSocket": {
                                        "host": "",
                                        "port": ""
                                    },
                                    "type": "HTTP_GET"
                                },
                                "stopGracePeriodSeconds": 10,
                                "termBarrier": "IGNORE",
                                "terminationGracePeriodSeconds": 10
                            },
                            "name": "market-fmcg-main",
                            "readinessProbe": {
                                "failureThreshold": 0,
                                "handlers": [
                                    {
                                        "execAction": {
                                            "command": []
                                        },
                                        "httpGet": {
                                            "host": "",
                                            "httpHeaders": [],
                                            "path": "/ping",
                                            "port": "{BSCONFIG_IPORT_PLUS_1}",
                                            "uriScheme": "HTTP"
                                        },
                                        "tcpSocket": {
                                            "host": "",
                                            "port": ""
                                        },
                                        "type": "HTTP_GET"
                                    }
                                ],
                                "initialDelaySeconds": 5,
                                "maxPeriodSeconds": 60,
                                "minPeriodSeconds": 5,
                                "periodBackoff": 2,
                                "successThreshold": 0
                            },
                            "reopenLogAction": {
                                "handler": {
                                    "execAction": {
                                        "command": []
                                    },
                                    "httpGet": {
                                        "host": "",
                                        "httpHeaders": [],
                                        "path": "",
                                        "port": "",
                                        "uriScheme": ""
                                    },
                                    "tcpSocket": {
                                        "host": "",
                                        "port": ""
                                    },
                                    "type": "NONE"
                                }
                            },
                            "resourceRequest": {
                                "limit": [],
                                "request": []
                            },
                            "restartPolicy": {
                                "maxPeriodSeconds": 60,
                                "minPeriodSeconds": 1,
                                "periodBackoff": 2,
                                "periodJitterSeconds": 20,
                                "type": "ALWAYS"
                            },
                            "unistatEndpoints": []
                        },
                        {
                            "command": [
                                "bin/push-client-start.sh"
                            ],
                            "coredumpPolicy": {
                                "coredumpProcessor": {
                                    "aggregator": {
                                        "saas": {
                                            "gdb": {
                                                "execPath": "/usr/bin/gdb",
                                                "type": "LITERAL"
                                            },
                                            "serviceName": "",
                                            "url": "http://cores.n.yandex-team.ru/corecomes"
                                        },
                                        "type": "DISABLED"
                                    },
                                    "cleanupPolicy": {
                                        "ttl": {
                                            "seconds": 3600
                                        },
                                        "type": "DISABLED"
                                    },
                                    "countLimit": 3,
                                    "path": "/cores",
                                    "probability": 100,
                                    "totalSizeLimit": 10240
                                },
                                "customProcessor": {
                                    "command": ""
                                },
                                "type": "NONE"
                            },
                            "env": [],
                            "hostDevices": [],
                            "lifecycle": {
                                "preStop": {
                                    "execAction": {
                                        "command": []
                                    },
                                    "httpGet": {
                                        "host": "",
                                        "httpHeaders": [],
                                        "path": "",
                                        "port": "",
                                        "uriScheme": ""
                                    },
                                    "tcpSocket": {
                                        "host": "",
                                        "port": ""
                                    },
                                    "type": "NONE"
                                },
                                "stopGracePeriodSeconds": 0,
                                "termBarrier": "IGNORE",
                                "terminationGracePeriodSeconds": 10
                            },
                            "name": "push-client",
                            "readinessProbe": {
                                "failureThreshold": 0,
                                "handlers": [],
                                "initialDelaySeconds": 5,
                                "maxPeriodSeconds": 60,
                                "minPeriodSeconds": 5,
                                "periodBackoff": 2,
                                "successThreshold": 0
                            },
                            "reopenLogAction": {
                                "handler": {
                                    "execAction": {
                                        "command": []
                                    },
                                    "httpGet": {
                                        "host": "",
                                        "httpHeaders": [],
                                        "path": "",
                                        "port": "",
                                        "uriScheme": ""
                                    },
                                    "tcpSocket": {
                                        "host": "",
                                        "port": ""
                                    },
                                    "type": "NONE"
                                }
                            },
                            "resourceRequest": {
                                "limit": [],
                                "request": []
                            },
                            "restartPolicy": {
                                "maxPeriodSeconds": 60,
                                "minPeriodSeconds": 1,
                                "periodBackoff": 2,
                                "periodJitterSeconds": 20,
                                "type": "ALWAYS"
                            },
                            "securityPolicy": {
                                "runAsUser": ""
                            },
                            "unistatEndpoints": []
                        },
                        {
                            "command": [
                                "/bin/bash",
                                "bin/logkeeper-minion.sh"
                            ],
                            "coredumpPolicy": {
                                "coredumpProcessor": {
                                    "aggregator": {
                                        "saas": {
                                            "gdb": {
                                                "execPath": "/usr/bin/gdb",
                                                "type": "LITERAL"
                                            },
                                            "serviceName": "",
                                            "url": "http://cores.n.yandex-team.ru/corecomes"
                                        },
                                        "type": "DISABLED"
                                    },
                                    "cleanupPolicy": {
                                        "ttl": {
                                            "seconds": 3600
                                        },
                                        "type": "DISABLED"
                                    },
                                    "countLimit": 3,
                                    "path": "/cores",
                                    "probability": 100,
                                    "totalSizeLimit": 10240
                                },
                                "customProcessor": {
                                    "command": ""
                                },
                                "type": "NONE"
                            },
                            "env": [],
                            "hostDevices": [],
                            "lifecycle": {
                                "preStop": {
                                    "execAction": {
                                        "command": []
                                    },
                                    "httpGet": {
                                        "host": "",
                                        "httpHeaders": [],
                                        "path": "",
                                        "port": "",
                                        "uriScheme": ""
                                    },
                                    "tcpSocket": {
                                        "host": "",
                                        "port": ""
                                    },
                                    "type": "NONE"
                                },
                                "stopGracePeriodSeconds": 0,
                                "termBarrier": "IGNORE",
                                "terminationGracePeriodSeconds": 10
                            },
                            "name": "logkeeper-minion",
                            "readinessProbe": {
                                "failureThreshold": 0,
                                "handlers": [],
                                "initialDelaySeconds": 5,
                                "maxPeriodSeconds": 60,
                                "minPeriodSeconds": 5,
                                "periodBackoff": 2,
                                "successThreshold": 0
                            },
                            "reopenLogAction": {
                                "handler": {
                                    "execAction": {
                                        "command": []
                                    },
                                    "httpGet": {
                                        "host": "",
                                        "httpHeaders": [],
                                        "path": "",
                                        "port": "",
                                        "uriScheme": ""
                                    },
                                    "tcpSocket": {
                                        "host": "",
                                        "port": ""
                                    },
                                    "type": "NONE"
                                }
                            },
                            "resourceRequest": {
                                "limit": [],
                                "request": []
                            },
                            "restartPolicy": {
                                "maxPeriodSeconds": 60,
                                "minPeriodSeconds": 1,
                                "periodBackoff": 2,
                                "periodJitterSeconds": 20,
                                "type": "ALWAYS"
                            },
                            "securityPolicy": {
                                "runAsUser": ""
                            },
                            "unistatEndpoints": []
                        }
                    ],
                    "dockerImage": {
                        "name": "",
                        "registry": "registry.yandex.net"
                    },
                    "hostProvidedDaemons": [
                        {
                            "type": "YASM_AGENT"
                        }
                    ],
                    "id": "",
                    "initContainers": [
                        {
                            "command": [
                                "/bin/sh",
                                "-c",
                                "tar -zxf market-common-config.tar.gz && bash bin/prepare.sh {BSCONFIG_INAME}"
                            ],
                            "env": [],
                            "hostDevices": [],
                            "name": "prepare",
                            "unistatEndpoints": []
                        },
                        {
                            "command": [
                                "/bin/sh",
                                "-c",
                                "ln -sfn pdata pstate"
                            ],
                            "env": [],
                            "hostDevices": [],
                            "name": "prepare-logrotate",
                            "unistatEndpoints": []
                        }
                    ],
                    "instanceAccess": {
                        "skynetSsh": "ENABLED"
                    },
                    "instancectl": {
                        "fetchableMeta": {
                            "sandboxResource": {
                                "resourceId": "1176089255",
                                "resourceType": "INSTANCECTL",
                                "taskId": "532412396",
                                "taskType": ""
                            },
                            "type": "SANDBOX_RESOURCE"
                        },
                        "url": [
                            "rbtorrent:41496d19171ee14033e507c0f98806bbfc20223d"
                        ],
                        "version": "1.198"
                    },
                    "layersConfig": {
                        "bind": [],
                        "layer": [
                            {
                                "fetchableMeta": {
                                    "sandboxResource": {
                                        "resourceId": "603632582",
                                        "resourceType": "PORTO_LAYER_SEARCH_UBUNTU_TRUSTY",
                                        "taskId": "267288486",
                                        "taskType": "BUILD_PORTO_LAYER"
                                    },
                                    "type": "SANDBOX_RESOURCE"
                                },
                                "url": [
                                    "rbtorrent:7c9da8ab309c31f4ad279c53a7db4b5cc05c1990"
                                ]
                            },
                            {
                                "fetchableMeta": {
                                    "sandboxResource": {
                                        "resourceId": "697045791",
                                        "resourceType": "PORTO_LAYER_MARKET_BASE_TRUSTY",
                                        "taskId": "310548904",
                                        "taskType": "BUILD_PORTO_LAYER"
                                    },
                                    "type": "SANDBOX_RESOURCE"
                                },
                                "url": [
                                    "rbtorrent:1c2bcd4460e392c32801ae738fc4a6f9b5eb4851"
                                ]
                            },
                            {
                                "fetchableMeta": {
                                    "sandboxResource": {
                                        "resourceId": "697071853",
                                        "resourceType": "PORTO_LAYER_MARKET_JDK",
                                        "taskId": "310560705",
                                        "taskType": "BUILD_PORTO_LAYER"
                                    },
                                    "type": "SANDBOX_RESOURCE"
                                },
                                "url": [
                                    "rbtorrent:a57dfeff2c59e1d15377adcf3751eae7515935ed"
                                ]
                            }
                        ]
                    },
                    "networkProperties": {
                        "etcHosts": "KEEP_ETC_HOSTS",
                        "resolvConf": "DEFAULT_RESOLV_CONF"
                    },
                    "notifyAction": {
                        "handlers": [],
                        "resourceRequest": {
                            "limit": [],
                            "request": []
                        }
                    },
                    "osContainerSpec": {
                        "auxDaemons": [],
                        "dockerImage": {
                            "name": "",
                            "registry": ""
                        },
                        "hostProvidedDaemons": [],
                        "initContainers": [],
                        "instanceAccess": {
                            "skynetSsh": "DISABLED"
                        },
                        "instancectl": {
                            "fetchableMeta": {
                                "sandboxResource": {
                                    "resourceId": "",
                                    "resourceType": "",
                                    "taskId": "",
                                    "taskType": ""
                                },
                                "type": "SANDBOX_RESOURCE"
                            },
                            "url": [],
                            "version": ""
                        },
                        "layersConfig": {
                            "bind": [],
                            "layer": []
                        },
                        "networkProperties": {
                            "etcHosts": "KEEP_ETC_HOSTS",
                            "resolvConf": "DEFAULT_RESOLV_CONF"
                        },
                        "notifyAction": {
                            "handlers": [],
                            "resourceRequest": {
                                "limit": [],
                                "request": []
                            }
                        },
                        "type": "NONE",
                        "volume": []
                    },
                    "qemuKvm": {
                        "image": {
                            "linux": {
                                "authorizedUsers": {
                                    "type": "SERVICE_MANAGERS",
                                    "userList": {
                                        "logins": [],
                                        "nannyGroupIds": [],
                                        "staffGroupIds": []
                                    }
                                },
                                "image": {
                                    "fetchableMeta": {
                                        "sandboxResource": {
                                            "resourceId": "",
                                            "resourceType": "",
                                            "taskId": "",
                                            "taskType": ""
                                        },
                                        "type": "SANDBOX_RESOURCE"
                                    },
                                    "url": []
                                }
                            },
                            "type": "LINUX",
                            "windows": {
                                "image": {
                                    "fetchableMeta": {
                                        "sandboxResource": {
                                            "resourceId": "",
                                            "resourceType": "",
                                            "taskId": "",
                                            "taskType": ""
                                        },
                                        "type": "SANDBOX_RESOURCE"
                                    },
                                    "url": []
                                }
                            }
                        },
                        "instancectl": {
                            "fetchableMeta": {
                                "sandboxResource": {
                                    "resourceId": "",
                                    "resourceType": "",
                                    "taskId": "",
                                    "taskType": ""
                                },
                                "type": "SANDBOX_RESOURCE"
                            },
                            "url": [],
                            "version": ""
                        },
                        "layersConfig": {
                            "fetchableMeta": {
                                "sandboxResource": {
                                    "resourceId": "",
                                    "resourceType": "",
                                    "taskId": "",
                                    "taskType": ""
                                },
                                "type": "SANDBOX_RESOURCE"
                            },
                            "url": [],
                            "version": ""
                        },
                        "vmagent": {
                            "fetchableMeta": {
                                "sandboxResource": {
                                    "resourceId": "",
                                    "resourceType": "",
                                    "taskId": "",
                                    "taskType": ""
                                },
                                "type": "SANDBOX_RESOURCE"
                            },
                            "url": [],
                            "version": ""
                        }
                    },
                    "type": "SANDBOX_LAYERS",
                    "volume": [
                        {
                            "itsVolume": {
                                "itsUrl": "http://its.yandex-team.ru/v1",
                                "maxRetryPeriodSeconds": 300,
                                "periodSeconds": 60
                            },
                            "name": "supercheck-ym-secrets",
                            "secretVolume": {
                                "keychainSecret": {
                                    "keychainId": "",
                                    "secretId": "",
                                    "secretRevisionId": ""
                                },
                                "secretName": ""
                            },
                            "templateVolume": {
                                "template": []
                            },
                            "type": "VAULT_SECRET",
                            "vaultSecretVolume": {
                                "vaultSecret": {
                                    "delegationToken": "RVPjQRmPMh76tLj1rnEqebF3DNtwQUMxbkAcey7rJYM.1.34e2ecc7753348b4",
                                    "secretId": "sec-01dhvck02d3regcstep0fk0ct1",
                                    "secretName": "supercheck-ym-secrets",
                                    "secretVer": "ver-01dkew0w0gqkkz3hztwty5y8d8"
                                }
                            },
                            "version": ""
                        }
                    ]
                },
                "instances": {
                    "chosen_type": "EXTENDED_GENCFG_GROUPS",
                    "extended_gencfg_groups": {
                        "containers_settings": {
                            "slot_porto_properties": "ALL_EXCEPT_GUARANTEES"
                        },
                        "gencfg_volumes_settings": {
                            "use_volumes": True
                        },
                        "groups": [
                            {
                                "limits": {
                                    "cpu_policy": "normal",
                                    "io_policy": "normal",
                                    "ulimit": "data: 68719476736 68719476736; memlock: 68719476736 68719476736"
                                },
                                "name": "MAN_MARKET_PREP_FMCG_MAIN",
                                "release": "tags/stable-136-r226",
                                "tags": []
                            }
                        ],
                        "instance_properties_settings": {
                            "tags": "TOPOLOGY_DYNAMIC"
                        },
                        "network_settings": {
                            "hbf_nat": "disabled",
                            "use_mtn": True
                        },
                        "sysctl_settings": {
                            "params": []
                        },
                        "tags": []
                    },
                    "gencfg_groups": [],
                    "instance_list": [],
                    "iss_settings": {
                        "hooks_resource_limits": {
                            "iss_hook_install": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_notify": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_reopenlogs": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_status": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_stop": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_uninstall": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_validate": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            }
                        },
                        "instance_cls": "ru.yandex.iss.Instance"
                    },
                    "yp_pod_ids": {
                        "orthogonal_tags": {
                            "ctype": "unknown",
                            "itype": "unknown",
                            "metaprj": "unknown",
                            "prj": "prestable-market-fmcg-main-man"
                        },
                        "pods": []
                    }
                },
                "resources": {
                    "l7_fast_balancer_config_files": [],
                    "sandbox_files": [
                        {
                            "extract_path": "",
                            "is_dynamic": False,
                            "local_path": "application.tar",
                            "resource_id": "1299743366",
                            "resource_type": "MARKET_FMCG_MAIN_APP",
                            "task_id": "587322873",
                            "task_type": "HTTP_UPLOAD_2"
                        },
                        {
                            "is_dynamic": False,
                            "local_path": "push-client",
                            "resource_type": "STATBOX_PUSHCLIENT",
                            "task_id": "146741790",
                            "task_type": "BUILD_STATBOX_PUSHCLIENT"
                        },
                        {
                            "is_dynamic": False,
                            "local_path": "datasources.tar.gz",
                            "resource_id": "1188206737",
                            "resource_type": "MARKET_DATASOURCES_STABLE",
                            "task_id": "537820902",
                            "task_type": "BUILD_MARKET_DATASOURCES"
                        },
                        {
                            "is_dynamic": False,
                            "local_path": "market-common-config.tar.gz",
                            "resource_id": "1273870427",
                            "resource_type": "MARKET_COMMON_CONFIG",
                            "task_id": "575361401",
                            "task_type": "MARKET_YA_PACKAGE"
                        },
                        {
                            "extract_path": "{JUGGLER_CHECKS_PATH}/market-juggler-rtc-checks-bundle.tar.gz",
                            "is_dynamic": False,
                            "local_path": "market-juggler-rtc-checks-bundle.tar.gz",
                            "resource_id": "916064765",
                            "resource_type": "MARKET_JUGGLER_RTC_CHECKS_BUNDLE",
                            "task_id": "413635186",
                            "task_type": "BUILD_JUGGLER_CHECKS_BUNDLE"
                        }
                    ],
                    "services_balancer_config_files": [],
                    "static_files": [
                        {
                            "content": "{%- set environment = env.BSCONFIG_ITAGS.split(\"a_ctype_\")[1].split()[0] -%}"
                            "\n{%- set location = env.BSCONFIG_ITAGS.split(\"a_dc_\")[1].split()[0] -%}\n{%- set nginx_log_prefix = \"market-fmcg-main\" -%}"
                            "\n{%- set log_files = [\n                        \"nginx/\" + nginx_log_prefix + \"-access-tskv.log\",\n                    ]-%}"
                            "\n{%- set nginx_includes = [\n                            \"include/logging\",\n                        ]-%}"
                            "\n{%- for l in read_plain_text_file(\"external/push-client.conf\") -%}\n{{ log_files.append(nginx_log_prefix + \"/\" + l) }}\n{%- endfor -%} ",
                            "is_dynamic": False,
                            "local_path": "variables.tmpl.static"
                        }
                    ],
                    "template_set_files": [],
                    "url_files": []
                }
            },
            "meta_info": {
                "annotations": {
                    "deploy_engine": "ISS_MAN",
                    "startable": "true"
                },
                "changes_record": {
                    "files": [
                        {
                            "sandbox_file_change": {
                                "resource_type": "MARKET_FMCG_MAIN_APP",
                                "task_id": "587322873",
                                "task_type": "HTTP_UPLOAD_2"
                            },
                            "type": "SANDBOX_FILE"
                        }
                    ]
                },
                "conf_id": "prestable_market_fmcg_main_man-1578994127732",
                "infra_notifications": {},
                "is_disposable": False,
                "scheduling_config": {
                    "sched_activate_recipe": "",
                    "sched_prepare_recipe": "",
                    "scheduling_priority": "NONE"
                },
                "startrek_tickets": [],
                "ticket_info": {
                    "release_id": "SANDBOX_RELEASE-587322873-PRESTABLE",
                    "ticket_id": "MARKET-43957515"
                }
            },
            "parent_id": "8963ff5d6becc7566c2f2d8ece9f6bdda323907c"
        },
        "target_state": {
            "_id": "",
            "content": {
                "info": {
                    "author": "nanny-robot",
                    "comment": "Setting ACTIVE in meta task search-0092971065/d849b519-51fe-4870-abf8-55ce79ba452d",
                    "ctime": 1578994135083
                },
                "is_enabled": True,
                "labels": [
                    {
                        "key": "source_task_group_id",
                        "value": "search-0092971065"
                    }
                ],
                "prepare_recipe": "default",
                "recipe": "default",
                "recipe_parameters": [],
                "snapshot_id": "eee395fb3d21d8a0505f7828f4f177bce0c2b7ee",
                "snapshot_info": {
                    "author": "robot-market-infra",
                    "comment": "Release from pipeline: https://tsum.yandex-team.ru/pipe/projects/fmcg/delivery-dashboard/market-fmcg/release/5e1c8ea264971b5fc50317ee",
                    "ctime": 1578994127732
                },
                "snapshot_meta_info": {
                    "annotations": {
                        "deploy_engine": "ISS_MAN",
                        "startable": "true"
                    },
                    "conf_id": "prestable_market_fmcg_main_man-1578994127732",
                    "is_disposable": False,
                    "revert_info": {
                        "last_transition_time": 0,
                        "message": "",
                        "reason": "",
                        "status": ""
                    },
                    "scheduling_config": {
                        "scheduling_priority": 0
                    },
                    "startrek_tickets": [],
                    "ticket_info": {
                        "release_id": "SANDBOX_RELEASE-587322873-PRESTABLE",
                        "ticket_id": "MARKET-43957515"
                    }
                },
                "snapshots": [
                    {
                        "info": {
                            "author": "nanny-robot",
                            "comment": "Implicitly disabled to activate another",
                            "ctime": 1578898583869
                        },
                        "labels": [],
                        "prepare_recipe": "default",
                        "recipe": "default",
                        "recipe_parameters": [],
                        "snapshot_id": "7c2eb5404e5261125d6612e16b78cfd6df9c4a42",
                        "snapshot_info": {
                            "author": "robot-market-infra",
                            "comment": "Release from pipeline: https://tsum.yandex-team.ru/pipe/projects/fmcg/delivery-dashboard/market-fmcg/release/5e1700df6af1125cb7374e6c",
                            "ctime": 1578571790026
                        },
                        "snapshot_meta_info": {
                            "annotations": {
                                "deploy_engine": "ISS_MAN",
                                "startable": "true"
                            },
                            "conf_id": "prestable_market_fmcg_main_man-1578571790026",
                            "is_disposable": False,
                            "revert_info": {
                                "last_transition_time": 0,
                                "message": "",
                                "reason": "",
                                "status": ""
                            },
                            "scheduling_config": {
                                "scheduling_priority": 0
                            },
                            "startrek_tickets": [],
                            "ticket_info": {
                                "release_id": "SANDBOX_RELEASE-584599460-PRESTABLE",
                                "ticket_id": "MARKET-43846828"
                            }
                        },
                        "state": "PREPARED",
                        "tracked_tickets": {
                            "startrek_tickets": [],
                            "tickets": [
                                {
                                    "ticket_id": "MARKET-43846828"
                                }
                            ]
                        }
                    },
                    {
                        "info": {
                            "author": "nanny-robot",
                            "comment": "Implicitly disabled to activate another",
                            "ctime": 1578923168418
                        },
                        "labels": [],
                        "prepare_recipe": "default",
                        "recipe": "default",
                        "recipe_parameters": [],
                        "snapshot_id": "0d9aed657ef05a14bfaa2fdbdcdcb6a9c70e63c5",
                        "snapshot_info": {
                            "author": "robot-market-infra",
                            "comment": "Release from pipeline: https://tsum.yandex-team.ru/pipe/projects/fmcg/delivery-dashboard/market-fmcg/release/5e188637a80d36112d6ca672",
                            "ctime": 1578898575508
                        },
                        "snapshot_meta_info": {
                            "annotations": {
                                "deploy_engine": "ISS_MAN",
                                "startable": "true"
                            },
                            "conf_id": "prestable_market_fmcg_main_man-1578898575508",
                            "is_disposable": False,
                            "revert_info": {
                                "last_transition_time": 0,
                                "message": "",
                                "reason": "",
                                "status": ""
                            },
                            "scheduling_config": {
                                "scheduling_priority": 0
                            },
                            "startrek_tickets": [],
                            "ticket_info": {
                                "release_id": "SANDBOX_RELEASE-585489243-PRESTABLE",
                                "ticket_id": "MARKET-43928164"
                            }
                        },
                        "state": "PREPARED",
                        "tracked_tickets": {
                            "startrek_tickets": [],
                            "tickets": [
                                {
                                    "ticket_id": "MARKET-43928164"
                                }
                            ]
                        }
                    },
                    {
                        "info": {
                            "author": "nanny-robot",
                            "comment": "Implicitly disabled to activate another",
                            "ctime": 1578994135083
                        },
                        "labels": [],
                        "prepare_recipe": "default",
                        "recipe": "default",
                        "recipe_parameters": [],
                        "snapshot_id": "8963ff5d6becc7566c2f2d8ece9f6bdda323907c",
                        "snapshot_info": {
                            "author": "robot-market-infra",
                            "comment": "Release from pipeline: https://tsum.yandex-team.ru/pipe/projects/fmcg/delivery-dashboard/market-fmcg/release/5e1c4b895b8a9e5e0ea609e1",
                            "ctime": 1578923160286
                        },
                        "snapshot_meta_info": {
                            "annotations": {
                                "deploy_engine": "ISS_MAN",
                                "startable": "true"
                            },
                            "conf_id": "prestable_market_fmcg_main_man-1578923160286",
                            "is_disposable": False,
                            "revert_info": {
                                "last_transition_time": 0,
                                "message": "",
                                "reason": "",
                                "status": ""
                            },
                            "scheduling_config": {
                                "scheduling_priority": 0
                            },
                            "startrek_tickets": [],
                            "ticket_info": {
                                "release_id": "SANDBOX_RELEASE-587116432-PRESTABLE",
                                "ticket_id": "MARKET-43938333"
                            }
                        },
                        "state": "PREPARED",
                        "tracked_tickets": {
                            "startrek_tickets": [],
                            "tickets": [
                                {
                                    "ticket_id": "MARKET-43938333"
                                }
                            ]
                        }
                    }
                ],
                "tracked_tickets": {
                    "startrek_tickets": [],
                    "tickets": [
                        {
                            "ticket_id": "MARKET-43957515"
                        }
                    ]
                }
            },
            "info": {
                "author": "nanny-robot",
                "comment": "SimpleCountLimitPolicy applied",
                "ctime": 1578994589514
            }
        }
    },
        stage_id='',
        raise_on_error=False,
        mock_clients=True,
    ).migrate()
    return {
        "stage": json_format.MessageToJson(stage),
        "release_rules": [json_format.MessageToJson(rl) for rl in release_rules]
    }


def test_migrate_service_man_pre_yp_resource_cache_ctl():
    stage, release_rules = NannyMigrator({
        "_id": "man_pre_yp_resource_cache_ctl",
        "auth_attrs": {
            "_id": "jadigmdvdssw7k7n6qyla5kk",
            "change_info": {
                "author": "chegoryu",
                "comment": "-",
                "ctime": 1561992730891
            },
            "content": {
                "conf_managers": {
                    "groups": [],
                    "logins": [
                        "chegoryu"
                    ]
                },
                "observers": {
                    "groups": [],
                    "logins": []
                },
                "ops_managers": {
                    "groups": [],
                    "logins": [
                        "chegoryu"
                    ]
                },
                "owners": {
                    "groups": [
                        "103591"
                    ],
                    "logins": [
                        "chegoryu",
                        "ndnuriev",
                        "avitella",
                        "yanddmi"
                    ]
                }
            }
        },
        "current_state": {
            "_id": "",
            "content": {
                "active_snapshots": [
                    {
                        "conf_id": "man_pre_yp_resource_cache_ctl-1576142777847",
                        "entered": 1576142891266,
                        "prepared": {
                            "last_transition_time": 1576142891266,
                            "status": "True"
                        },
                        "snapshot_id": "ebb141fe20c603b8028fe52ffd1fcaf26adce14d",
                        "state": "ACTIVE",
                        "taskgroup_id": "search-0089427514",
                        "taskgroup_started": {
                            "last_transition_time": 1576142797875,
                            "message": "",
                            "reason": "",
                            "status": "true"
                        }
                    },
                    {
                        "conf_id": "man_pre_yp_resource_cache_ctl-1569512757945",
                        "entered": 1576142910473,
                        "prepared": {
                            "last_transition_time": 1569512977199,
                            "status": "True"
                        },
                        "snapshot_id": "228f4ec9864d8f1bf7f91025468cd07ef8687068",
                        "state": "PREPARED",
                        "taskgroup_id": "search-0089427633",
                        "taskgroup_started": {
                            "last_transition_time": 1576142891424,
                            "message": "",
                            "reason": "",
                            "status": "true"
                        }
                    },
                    {
                        "conf_id": "man_pre_yp_resource_cache_ctl-1564145502331",
                        "entered": 1569513058674,
                        "prepared": {
                            "last_transition_time": 1564145736166,
                            "status": "True"
                        },
                        "snapshot_id": "c8dc5ceea04aebfdd3b4c39c123d76465e2d0052",
                        "state": "PREPARED",
                        "taskgroup_id": "search-0080968512",
                        "taskgroup_started": {
                            "last_transition_time": 1569513046366,
                            "message": "",
                            "reason": "",
                            "status": "true"
                        }
                    },
                    {
                        "conf_id": "man_pre_yp_resource_cache_ctl-1562592371028",
                        "entered": 1564145883196,
                        "prepared": {
                            "last_transition_time": 1562592579549,
                            "status": "True"
                        },
                        "snapshot_id": "dab4ebcf37b324d111127d90548f6f1ba1a64bb4",
                        "state": "PREPARED",
                        "taskgroup_id": "search-0074731839",
                        "taskgroup_started": {
                            "last_transition_time": 1564145873629,
                            "message": "",
                            "reason": "",
                            "status": "true"
                        }
                    }
                ],
                "is_paused": {
                    "info": {
                        "author": "chegoryu",
                        "comment": "Copy sas_test_yp_resource_cache_ctl to man_pre_yp_resource_cache_ctl",
                        "ctime": 1556111463591
                    },
                    "value": False
                },
                "rollback_snapshot": {
                    "snapshot_id": "228f4ec9864d8f1bf7f91025468cd07ef8687068"
                },
                "summary": {
                    "entered": 1576142910526,
                    "value": "ONLINE"
                }
            },
            "entered": 1556111463769,
            "reallocation": {
                "id": "",
                "state": {
                    "entered": 0,
                    "message": "",
                    "reason": "",
                    "status": ""
                },
                "taskgroup_id": ""
            }
        },
        "info_attrs": {
            "_id": "7a5zyxy2ch6l6x5w6v5simqt",
            "change_info": {
                "author": "chegoryu",
                "comment": "",
                "ctime": 1556111910470
            },
            "content": {
                "abc_group": 1979,
                "balancers_integration": {
                    "auto_update_services_balancers": False
                },
                "category": "/infra/resource_cache_controller/",
                "cms_settings": {
                    "cms_stub_policy": "SKIP"
                },
                "desc": "ResourceCacheController for man-pre",
                "disk_quotas": {
                    "policy": "DISABLED",
                    "root_fs_quota": 0,
                    "work_dir_quota": 0
                },
                "instancectl_settings": {
                    "autoupdate_instancectl_disabled": False
                },
                "labels": [
                    {
                        "key": "geo",
                        "value": "man"
                    },
                    {
                        "key": "ctype",
                        "value": "prestable"
                    },
                    {
                        "key": "itype",
                        "value": "yp_resource_cache_controller"
                    },
                    {
                        "key": "prj",
                        "value": "yp-man-pre"
                    },
                    {
                        "key": "dc",
                        "value": "man"
                    }
                ],
                "monitoring_settings": {
                    "deploy_monitoring": {
                        "is_enabled": False
                    },
                    "juggler_settings": {
                        "is_enabled": False
                    },
                    "panels": {
                        "juggler": [],
                        "quentao": [],
                        "yasm": []
                    }
                },
                "recipes": {
                    "content": [
                        {
                            "context": [],
                            "desc": "Activate",
                            "id": "default",
                            "labels": [],
                            "name": "_activate_only_service_configuration.yaml"
                        }
                    ],
                    "prepare_recipes": [
                        {
                            "context": [],
                            "desc": "Prepare",
                            "id": "default",
                            "labels": [],
                            "name": "_prepare_service_configuration.yaml"
                        }
                    ]
                },
                "scheduling_policy": {
                    "type": "NONE"
                },
                "tickets_integration": {
                    "docker_release_rule": {
                        "match_release_type": "-",
                        "responsibles": []
                    },
                    "gencfg_release_rule": {
                        "filter_params": {
                            "expression": "False"
                        },
                        "responsibles": []
                    },
                    "instancectl_release_rule": {
                        "match_release_type": "-",
                        "queue_id": "INSTANCECTL",
                        "responsibles": []
                    },
                    "service_release_rules": [],
                    "service_release_tickets_enabled": False
                },
                "ui_settings": {},
                "yp_settings": {
                    "mirror_to_yp": "SKIP"
                }
            },
            "parent_id": "nwioifoi7idy7i7sc4euw46t"
        },
        "runtime_attrs": {
            "_id": "ebb141fe20c603b8028fe52ffd1fcaf26adce14d",
            "change_info": {
                "author": "nanny-robot",
                "comment": "Replication policy applied. Replaced pods: \"MAN: mjfgp2pqvo4l55gf\" -> \"vvpeyfshfzw34zfj\".",
                "ctime": 1576142777847
            },
            "content": {
                "engines": {
                    "engine_type": "YP_LITE"
                },
                "instance_spec": {
                    "auxDaemons": [],
                    "containers": [
                        {
                            "command": [],
                            "coredumpPolicy": {
                                "coredumpProcessor": {
                                    "aggregator": {
                                        "saas": {
                                            "gdb": {
                                                "execPath": "/usr/bin/gdb",
                                                "type": "LITERAL"
                                            },
                                            "serviceName": "",
                                            "url": "http://cores.n.yandex-team.ru/corecomes"
                                        },
                                        "type": "DISABLED"
                                    },
                                    "cleanupPolicy": {
                                        "ttl": {
                                            "seconds": 3600
                                        },
                                        "type": "DISABLED"
                                    },
                                    "countLimit": 3,
                                    "path": "/cores",
                                    "probability": 100,
                                    "totalSizeLimit": 10240
                                },
                                "customProcessor": {
                                    "command": ""
                                },
                                "type": "NONE"
                            },
                            "env": [
                                {
                                    "name": "YT_TOKEN",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "",
                                            "keychainSecret": {
                                                "keychainId": "resource_cache_controller",
                                                "secretId": "yt_locke_token",
                                                "secretRevisionId": "5f850a57-3329-4adf-bb20-1a71c17c0650&fix&1553863277911"
                                            },
                                            "secretName": ""
                                        },
                                        "type": "SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "",
                                            "vaultSecret": {
                                                "delegationToken": "",
                                                "secretId": "",
                                                "secretName": "",
                                                "secretVer": ""
                                            }
                                        }
                                    }
                                },
                                {
                                    "name": "YP_TOKEN",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": ""
                                        },
                                        "secretEnv": {
                                            "field": "",
                                            "keychainSecret": {
                                                "keychainId": "resource_cache_controller",
                                                "secretId": "yp_token",
                                                "secretRevisionId": "83399cea-be2f-476a-b830-6f9e33e184db&initial-revision&1553862974923"
                                            },
                                            "secretName": ""
                                        },
                                        "type": "SECRET_ENV",
                                        "vaultSecretEnv": {
                                            "field": "",
                                            "vaultSecret": {
                                                "delegationToken": "",
                                                "secretId": "",
                                                "secretName": "",
                                                "secretVer": ""
                                            }
                                        }
                                    }
                                },
                                {
                                    "name": "YP_CLUSTER",
                                    "valueFrom": {
                                        "literalEnv": {
                                            "value": "man-pre"
                                        },
                                        "secretEnv": {
                                            "field": "",
                                            "keychainSecret": {
                                                "keychainId": "",
                                                "secretId": "",
                                                "secretRevisionId": ""
                                            },
                                            "secretName": ""
                                        },
                                        "type": "LITERAL_ENV",
                                        "vaultSecretEnv": {
                                            "field": "",
                                            "vaultSecret": {
                                                "delegationToken": "",
                                                "secretId": "",
                                                "secretName": "",
                                                "secretVer": ""
                                            }
                                        }
                                    }
                                }
                            ],
                            "hostDevices": [],
                            "lifecycle": {
                                "preStop": {
                                    "execAction": {
                                        "command": []
                                    },
                                    "httpGet": {
                                        "host": "",
                                        "httpHeaders": [],
                                        "path": "",
                                        "port": "",
                                        "uriScheme": ""
                                    },
                                    "tcpSocket": {
                                        "host": "",
                                        "port": ""
                                    },
                                    "type": "NONE"
                                },
                                "stopGracePeriodSeconds": 0,
                                "termBarrier": "IGNORE",
                                "terminationGracePeriodSeconds": 10
                            },
                            "name": "resource_cache_controller",
                            "readinessProbe": {
                                "handlers": [],
                                "initialDelaySeconds": 5,
                                "maxPeriodSeconds": 60,
                                "minPeriodSeconds": 5,
                                "periodBackoff": 2
                            },
                            "reopenLogAction": {
                                "handler": {
                                    "execAction": {
                                        "command": []
                                    },
                                    "httpGet": {
                                        "host": "",
                                        "httpHeaders": [],
                                        "path": "",
                                        "port": "",
                                        "uriScheme": ""
                                    },
                                    "tcpSocket": {
                                        "host": "",
                                        "port": ""
                                    },
                                    "type": "NONE"
                                }
                            },
                            "resourceRequest": {
                                "limit": []
                            },
                            "restartPolicy": {
                                "maxPeriodSeconds": 60,
                                "minPeriodSeconds": 1,
                                "periodBackoff": 2,
                                "periodJitterSeconds": 20
                            },
                            "securityPolicy": {
                                "runAsUser": ""
                            },
                            "unistatEndpoints": []
                        }
                    ],
                    "dockerImage": {
                        "name": "",
                        "registry": "registry.yandex.net"
                    },
                    "hostProvidedDaemons": [],
                    "id": "",
                    "initContainers": [],
                    "instanceAccess": {
                        "skynetSsh": "ENABLED"
                    },
                    "instancectl": {
                        "fetchableMeta": {
                            "sandboxResource": {
                                "resourceId": "993438063",
                                "resourceType": "INSTANCECTL",
                                "taskId": "449985771",
                                "taskType": ""
                            },
                            "type": "SANDBOX_RESOURCE"
                        },
                        "url": [
                            "rbtorrent:a9fab80f0c2bedc901d1e54f81ef676c7b56f0c4"
                        ],
                        "version": "1.184"
                    },
                    "layersConfig": {
                        "bind": [],
                        "layer": [
                            {
                                "fetchableMeta": {
                                    "sandboxResource": {
                                        "resourceId": "814305252",
                                        "resourceType": "PORTO_LAYER_SEARCH_UBUNTU_PRECISE_APP",
                                        "taskId": "367121226",
                                        "taskType": "BUILD_PORTO_LAYER"
                                    },
                                    "type": "SANDBOX_RESOURCE"
                                },
                                "url": [
                                    "rbtorrent:5d906b1c2b13cc9fcb5de7323c840a1c533f6c00"
                                ]
                            }
                        ]
                    },
                    "networkProperties": {
                        "etcHosts": "KEEP_ETC_HOSTS",
                        "resolvConf": "DEFAULT_RESOLV_CONF"
                    },
                    "notifyAction": {
                        "handlers": [],
                        "resourceRequest": {
                            "limit": [],
                            "request": []
                        }
                    },
                    "osContainerSpec": {
                        "auxDaemons": [],
                        "dockerImage": {
                            "name": "",
                            "registry": ""
                        },
                        "hostProvidedDaemons": [],
                        "initContainers": [],
                        "instanceAccess": {
                            "skynetSsh": "DISABLED"
                        },
                        "instancectl": {
                            "fetchableMeta": {
                                "sandboxResource": {
                                    "resourceId": "788465309",
                                    "resourceType": "INSTANCECTL",
                                    "taskId": "354618858",
                                    "taskType": ""
                                },
                                "type": "SANDBOX_RESOURCE"
                            },
                            "url": [
                                "rbtorrent:f3dd1364b975acf587e5766461a4fbd066675169"
                            ],
                            "version": "1.165"
                        },
                        "layersConfig": {
                            "bind": [],
                            "layer": [
                                {
                                    "fetchableMeta": {
                                        "sandboxResource": {
                                            "resourceId": "703602428",
                                            "resourceType": "PORTO_LAYER_SEARCH_UBUNTU_PRECISE_APP",
                                            "taskId": "313732989",
                                            "taskType": "BUILD_PORTO_LAYER"
                                        },
                                        "type": "SANDBOX_RESOURCE"
                                    },
                                    "url": [
                                        "rbtorrent:4eccf4fdbd83d58dc290d550ac82969724c53945"
                                    ]
                                }
                            ]
                        },
                        "networkProperties": {
                            "etcHosts": "KEEP_ETC_HOSTS",
                            "resolvConf": "DEFAULT_RESOLV_CONF"
                        },
                        "notifyAction": {
                            "handlers": [],
                            "resourceRequest": {
                                "limit": [],
                                "request": []
                            }
                        },
                        "type": "SANDBOX_LAYERS",
                        "volume": []
                    },
                    "qemuKvm": {
                        "image": {
                            "linux": {
                                "authorizedUsers": {
                                    "type": "SERVICE_MANAGERS",
                                    "userList": {
                                        "logins": [],
                                        "nannyGroupIds": [],
                                        "staffGroupIds": []
                                    }
                                },
                                "image": {
                                    "fetchableMeta": {
                                        "sandboxResource": {
                                            "resourceId": "409331232",
                                            "resourceType": "QEMU_IMAGE",
                                            "taskId": "178867853",
                                            "taskType": "REMOTE_COPY_RESOURCE"
                                        },
                                        "type": "SANDBOX_RESOURCE"
                                    },
                                    "url": [
                                        "rbtorrent:59f6fc1ff7efd59a8f37547dc4a4baebfa8af023"
                                    ]
                                }
                            },
                            "type": "LINUX",
                            "windows": {
                                "image": {
                                    "fetchableMeta": {
                                        "sandboxResource": {
                                            "resourceId": "",
                                            "resourceType": "",
                                            "taskId": "",
                                            "taskType": ""
                                        },
                                        "type": "SANDBOX_RESOURCE"
                                    },
                                    "url": []
                                }
                            }
                        },
                        "instancectl": {
                            "fetchableMeta": {
                                "sandboxResource": {
                                    "resourceId": "788465309",
                                    "resourceType": "INSTANCECTL",
                                    "taskId": "354618858",
                                    "taskType": ""
                                },
                                "type": "SANDBOX_RESOURCE"
                            },
                            "url": [
                                "rbtorrent:f3dd1364b975acf587e5766461a4fbd066675169"
                            ],
                            "version": "1.165"
                        },
                        "layersConfig": {
                            "fetchableMeta": {
                                "sandboxResource": {
                                    "resourceId": "562598663",
                                    "resourceType": "PORTO_LAYER_SEARCH_QEMU_UBUNTU_XENIAL",
                                    "taskId": "248826138",
                                    "taskType": ""
                                },
                                "type": "SANDBOX_RESOURCE"
                            },
                            "url": [
                                "rbtorrent:882c02f072d0c4d4bbf556fe50ceec626e795446"
                            ],
                            "version": "vmagent_last"
                        },
                        "vmagent": {
                            "fetchableMeta": {
                                "sandboxResource": {
                                    "resourceId": "801650300",
                                    "resourceType": "VMAGENT_PACK",
                                    "taskId": "361396805",
                                    "taskType": ""
                                },
                                "type": "SANDBOX_RESOURCE"
                            },
                            "url": [
                                "rbtorrent:99b06913daab303ccc9f045fb2afabd0a4d730dc"
                            ],
                            "version": "0.17"
                        }
                    },
                    "type": "SANDBOX_LAYERS",
                    "volume": []
                },
                "instances": {
                    "chosen_type": "YP_POD_IDS",
                    "extended_gencfg_groups": {
                        "containers_settings": {
                            "slot_porto_properties": "NONE"
                        },
                        "groups": [],
                        "instance_properties_settings": {
                            "tags": "ALL_STATIC"
                        },
                        "network_settings": {
                            "hbf_nat": "enabled"
                        },
                        "tags": []
                    },
                    "gencfg_groups": [],
                    "instance_list": [],
                    "iss_settings": {
                        "hooks_resource_limits": {
                            "iss_hook_install": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_notify": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_reopenlogs": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_status": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_stop": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_uninstall": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_validate": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            }
                        },
                        "instance_cls": "ru.yandex.iss.Instance"
                    },
                    "yp_pod_ids": {
                        "orthogonal_tags": {
                            "ctype": "unknown",
                            "itype": "unknown",
                            "metaprj": "unknown",
                            "prj": "default-content-service"
                        },
                        "pods": [
                            {
                                "cluster": "MAN",
                                "pod_id": "vvpeyfshfzw34zfj"
                            },
                            {
                                "cluster": "MAN",
                                "pod_id": "ur4zesyy3chbtg6r"
                            },
                            {
                                "cluster": "MAN",
                                "pod_id": "yp6qq67aeab2vmem"
                            }
                        ]
                    },
                    "yp_pods": {
                        "allocations": [],
                        "orthogonal_tags": {
                            "ctype": "unknown",
                            "itype": "unknown",
                            "metaprj": "unknown",
                            "prj": "default-content-service"
                        },
                        "tags": []
                    }
                },
                "resources": {
                    "l7_fast_balancer_config_files": [],
                    "sandbox_files": [
                        {
                            "extract_path": "",
                            "is_dynamic": False,
                            "local_path": "resource_cache_controller.tar.gz",
                            "resource_id": "1139271923",
                            "resource_type": "RESOURCE_CACHE_CONTROLLER_PACKAGE",
                            "task_id": "515938072",
                            "task_type": "YA_PACKAGE"
                        }
                    ],
                    "services_balancer_config_files": [],
                    "static_files": [
                        {
                            "content": "#!/usr/bin/env bash\n\n[defaults]\nits_poll = 1\nlogs_dir = /logs/\n\n[resource_cache_controller]"
                            "\nbinary = /bin/sh\narguments =\n    -c 'exec ./resource_cache_controller/resource_cache_controller run"
                            " -V YpClient.Address=${YP_CLUSTER}.yp.yandex.net:8090 -V HttpService.Port=%(BSCONFIG_IPORT)s"
                            " -V Logger.Path=%(logs_dir)scurrent-eventlog-resource_cache_controller-%(BSCONFIG_IPORT)s"
                            " -V Logger.RotatePath=%(logs_dir)seventlog-resource_cache_controller-%(BSCONFIG_IPORT)s.PREV"
                            " -V Logger.MaxLogSizeBytes=2147483648 -V LeadingInvader.Proxy=yp-${YP_CLUSTER} -V LeadingInvader.Path=//yp/resource_cache_controller/leader_lock"
                            " -V Logger.QueueSize=16384'\n\ninstall_script =\n    set -x\n    if ! tar xf %(BSCONFIG_IDIR)s/resource_cache_controller.tar.gz; then"
                            "\n        echo \"Failed unpacking resource_cache_controller.tar.gz\"\n        exit 1\n    fi\n    echo \"Starting\"\n\nstatus_script ="
                            "\n    retcode=$(curl -s -o /dev/null -w '%%{http_code}' http://localhost:%(BSCONFIG_IPORT)s/ping)\n    if [ $retcode -eq 200 ] 2>/dev/null; then"
                            "\n        exit 0\n    fi\n    exit 1\n\nreopenlog_script =\n    wget -qO /dev/null \"http://localhost:%(BSCONFIG_IPORT)s/reopen_log\""
                            "\n\nstop_script =\n    wget -qO /dev/null \"http://localhost:%(BSCONFIG_IPORT)s/shutdown\"",
                            "is_dynamic": False,
                            "local_path": "instancectl.conf"
                        }
                    ],
                    "template_set_files": [],
                    "url_files": []
                }
            },
            "meta_info": {
                "annotations": {
                    "deploy_engine": "YP_LITE",
                    "startable": "true"
                },
                "conf_id": "man_pre_yp_resource_cache_ctl-1576142777847",
                "infra_notifications": {},
                "is_disposable": False,
                "scheduling_config": {
                    "scheduling_priority": "NORMAL"
                },
                "startrek_tickets": []
            },
            "parent_id": "228f4ec9864d8f1bf7f91025468cd07ef8687068"
        },
        "target_state": {
            "_id": "",
            "content": {
                "info": {
                    "author": "nanny-robot",
                    "comment": "Replication policy applied. Replaced pods: \"MAN: mjfgp2pqvo4l55gf\" -> \"vvpeyfshfzw34zfj\".",
                    "ctime": 1576142778232
                },
                "is_enabled": True,
                "labels": [],
                "recipe": "replication.default",
                "recipe_parameters": [],
                "snapshot_id": "ebb141fe20c603b8028fe52ffd1fcaf26adce14d",
                "snapshot_info": {
                    "author": "nanny-robot",
                    "comment": "Replication policy applied. Replaced pods: \"MAN: mjfgp2pqvo4l55gf\" -> \"vvpeyfshfzw34zfj\".",
                    "ctime": 1576142777847
                },
                "snapshot_meta_info": {
                    "annotations": {
                        "deploy_engine": "YP_LITE",
                        "startable": "true"
                    },
                    "conf_id": "man_pre_yp_resource_cache_ctl-1576142777847",
                    "is_disposable": False,
                    "revert_info": {
                        "last_transition_time": 0,
                        "message": "",
                        "reason": "",
                        "status": ""
                    },
                    "scheduling_config": {
                        "scheduling_priority": 1
                    },
                    "startrek_tickets": []
                },
                "snapshots": [
                    {
                        "info": {
                            "author": "chegoryu",
                            "comment": "Implicitly disabled to activate another",
                            "ctime": 1564145615146
                        },
                        "labels": [],
                        "prepare_recipe": "default",
                        "recipe": "default",
                        "recipe_parameters": [],
                        "snapshot_id": "dab4ebcf37b324d111127d90548f6f1ba1a64bb4",
                        "snapshot_info": {
                            "author": "chegoryu",
                            "comment": "Update instancectl.conf (batch change from \"yp_resource_cache_ctl\" dashboard)",
                            "ctime": 1562592371028
                        },
                        "snapshot_meta_info": {
                            "annotations": {
                                "deploy_engine": "YP_LITE",
                                "startable": "true"
                            },
                            "conf_id": "man_pre_yp_resource_cache_ctl-1562592371028",
                            "is_disposable": False,
                            "revert_info": {
                                "last_transition_time": 0,
                                "message": "",
                                "reason": "",
                                "status": ""
                            },
                            "startrek_tickets": []
                        },
                        "state": "PREPARED",
                        "tracked_tickets": {
                            "startrek_tickets": [],
                            "tickets": []
                        }
                    },
                    {
                        "info": {
                            "author": "chegoryu",
                            "comment": "Implicitly disabled to activate another",
                            "ctime": 1569512782548
                        },
                        "labels": [],
                        "prepare_recipe": "default",
                        "recipe": "default",
                        "recipe_parameters": [],
                        "snapshot_id": "c8dc5ceea04aebfdd3b4c39c123d76465e2d0052",
                        "snapshot_info": {
                            "author": "chegoryu",
                            "comment": "Update resource_cache_controller.tar.gz (batch change from \"yp_resource_cache_ctl\" dashboard)",
                            "ctime": 1564145502331
                        },
                        "snapshot_meta_info": {
                            "annotations": {
                                "deploy_engine": "YP_LITE",
                                "startable": "true"
                            },
                            "conf_id": "man_pre_yp_resource_cache_ctl-1564145502331",
                            "is_disposable": False,
                            "revert_info": {
                                "last_transition_time": 0,
                                "message": "",
                                "reason": "",
                                "status": ""
                            },
                            "startrek_tickets": []
                        },
                        "state": "PREPARED",
                        "tracked_tickets": {
                            "startrek_tickets": [],
                            "tickets": []
                        }
                    },
                    {
                        "info": {
                            "author": "nanny-robot",
                            "comment": "Implicitly disabled to activate another",
                            "ctime": 1576142778232
                        },
                        "labels": [],
                        "prepare_recipe": "default",
                        "recipe": "default",
                        "recipe_parameters": [],
                        "snapshot_id": "228f4ec9864d8f1bf7f91025468cd07ef8687068",
                        "snapshot_info": {
                            "author": "chegoryu",
                            "comment": "Update resource_cache_controller.tar.gz (batch change from \"yp_resource_cache_ctl\" dashboard)",
                            "ctime": 1569512757945
                        },
                        "snapshot_meta_info": {
                            "annotations": {
                                "deploy_engine": "YP_LITE",
                                "startable": "true"
                            },
                            "conf_id": "man_pre_yp_resource_cache_ctl-1569512757945",
                            "is_disposable": False,
                            "revert_info": {
                                "last_transition_time": 0,
                                "message": "",
                                "reason": "",
                                "status": ""
                            },
                            "startrek_tickets": []
                        },
                        "state": "PREPARED",
                        "tracked_tickets": {
                            "startrek_tickets": [],
                            "tickets": []
                        }
                    }
                ],
                "tracked_tickets": {
                    "startrek_tickets": [],
                    "tickets": []
                }
            },
            "info": {
                "author": "nanny-robot",
                "comment": "Replication policy applied. Replaced pods: \"MAN: mjfgp2pqvo4l55gf\" -> \"vvpeyfshfzw34zfj\".",
                "ctime": 1576142778232
            }
        }
    },
        stage_id='',
        raise_on_error=False,
        mock_clients=True,
    ).migrate()
    return {
        "stage": json_format.MessageToJson(stage),
        "release_rules": [json_format.MessageToJson(rl) for rl in release_rules]
    }


def test_migrate_service_test_yanddmi_hello_word():
    stage, release_rules = NannyMigrator({
        "_id": "test_yanddmi_hello_word",
        "auth_attrs": {
            "_id": "jcn6adhmw2zb7newd4egdmjr",
            "change_info": {
                "author": "yanddmi",
                "comment": "Copy of test_danibw_hello_word by yanddmi",
                "ctime": 1579092991448
            },
            "content": {
                "conf_managers": {
                    "groups": [],
                    "logins": [
                        "yanddmi"
                    ]
                },
                "observers": {
                    "groups": [],
                    "logins": []
                },
                "ops_managers": {
                    "groups": [],
                    "logins": [
                        "yanddmi"
                    ]
                },
                "owners": {
                    "groups": [],
                    "logins": [
                        "yanddmi"
                    ]
                }
            }
        },
        "current_state": {
            "_id": "",
            "content": {
                "active_snapshots": [
                    {
                        "conf_id": "test_yanddmi_hello_word-1585925831992",
                        "entered": 1585925949926,
                        "prepared": {
                            "last_transition_time": 1585925908878,
                            "status": "True"
                        },
                        "snapshot_id": "b3baac7dc4550db7a666ce05f6dc8cde33793e47",
                        "state": "ACTIVE",
                        "taskgroup_id": "search-0103531754",
                        "taskgroup_started": {
                            "last_transition_time": 1585925910128,
                            "message": "",
                            "reason": "",
                            "status": "true"
                        }
                    },
                    {
                        "conf_id": "test_yanddmi_hello_word-1585925163341",
                        "entered": 1585925965446,
                        "prepared": {
                            "last_transition_time": 1585925343854,
                            "status": "True"
                        },
                        "snapshot_id": "4136bb0f7a32d5fe6a04cd1ec412b615095a4489",
                        "state": "PREPARED",
                        "taskgroup_id": "search-0103531842",
                        "taskgroup_started": {
                            "last_transition_time": 1585925950043,
                            "message": "",
                            "reason": "",
                            "status": "true"
                        }
                    },
                    {
                        "conf_id": "test_yanddmi_hello_word-1585843729404",
                        "entered": 1585925397134,
                        "prepared": {
                            "last_transition_time": 1585843799040,
                            "status": "True"
                        },
                        "snapshot_id": "5d9f19f659098aaa45b063f838c3492ad73612a0",
                        "state": "PREPARED",
                        "taskgroup_id": "search-0103530815",
                        "taskgroup_started": {
                            "last_transition_time": 1585925382897,
                            "message": "",
                            "reason": "",
                            "status": "true"
                        }
                    },
                    {
                        "conf_id": "test_yanddmi_hello_word-1585843060315",
                        "entered": 1585843862666,
                        "prepared": {
                            "last_transition_time": 1585843147732,
                            "status": "True"
                        },
                        "snapshot_id": "8ef95fe403de89864915228dcc3906844cf3d469",
                        "state": "PREPARED",
                        "taskgroup_id": "search-0103389346",
                        "taskgroup_started": {
                            "last_transition_time": 1585843845858,
                            "message": "",
                            "reason": "",
                            "status": "true"
                        }
                    }
                ],
                "is_paused": {
                    "info": {
                        "author": "yanddmi",
                        "comment": "Copy of test_danibw_hello_word by yanddmi",
                        "ctime": 1579092991448
                    },
                    "value": False
                },
                "rollback_snapshot": {
                    "snapshot_id": "4136bb0f7a32d5fe6a04cd1ec412b615095a4489"
                },
                "summary": {
                    "entered": 1585925965517,
                    "value": "ONLINE"
                }
            },
            "entered": 1579092991941,
            "reallocation": {
                "id": "",
                "state": {
                    "entered": 0,
                    "message": "",
                    "reason": "",
                    "status": ""
                },
                "taskgroup_id": ""
            }
        },
        "info_attrs": {
            "_id": "3tu4r4y2d3kapevymc3lqfzj",
            "change_info": {
                "author": "yanddmi",
                "comment": "",
                "ctime": 1585925549700
            },
            "content": {
                "abc_group": 4450,
                "balancers_integration": {
                    "auto_update_services_balancers": False
                },
                "category": "/users/yanddmi/",
                "cms_settings": {
                    "cms_stub_policy": "SKIP"
                },
                "desc": "for nanny -> YD migration tests",
                "disk_quotas": {
                    "policy": "ENABLED",
                    "root_fs_quota": 1500,
                    "work_dir_quota": 1500
                },
                "instancectl_settings": {
                    "autoupdate_instancectl_disabled": False
                },
                "labels": [],
                "monitoring_settings": {
                    "deploy_monitoring": {
                        "content": {
                            "alert_methods": [
                                "email"
                            ],
                            "deploy_timeout": 3600
                        },
                        "is_enabled": False
                    },
                    "juggler_settings": {
                        "content": {
                            "active_checks": [
                                {
                                    "checks": [],
                                    "flap_detector": {},
                                    "module": {
                                        "logic_or": {
                                            "mode": "CRIT"
                                        },
                                        "type": "logic_or"
                                    },
                                    "passive_checks": [
                                        {
                                            "juggler_host_name": "test_danibw_hello_word-BFg",
                                            "juggler_service_name": "test_danibw_hw_check",
                                            "notifications": [],
                                            "options": {
                                                "args": [],
                                                "env_vars": []
                                            }
                                        }
                                    ],
                                    "per_auto_tags": []
                                }
                            ],
                            "instance_resolve_type": "NANNY",
                            "juggler_hosts": [
                                {
                                    "golem_responsible": {
                                        "groups": [],
                                        "logins": [
                                            "danibw"
                                        ]
                                    },
                                    "name": "test_danibw_hello_word-BFg"
                                }
                            ],
                            "juggler_tags": []
                        },
                        "is_enabled": True
                    },
                    "panels": {
                        "juggler": [],
                        "quentao": [],
                        "yasm": []
                    }
                },
                "recipes": {
                    "content": [
                        {
                            "context": [],
                            "desc": "Activate",
                            "id": "default",
                            "labels": [],
                            "name": "_activate_only_service_configuration.yaml"
                        }
                    ],
                    "prepare_recipes": [
                        {
                            "context": [],
                            "desc": "Prepare",
                            "id": "default",
                            "labels": [],
                            "name": "_prepare_service_configuration.yaml"
                        }
                    ]
                },
                "scheduling_policy": {
                    "maintain_active_trunk": {
                        "activate_recipe": "default",
                        "prepare_recipe": "default"
                    },
                    "type": "MAINTAIN_ACTIVE_TRUNK"
                },
                "tickets_integration": {
                    "docker_release_rule": {
                        "match_release_type": "-",
                        "responsibles": []
                    },
                    "gencfg_release_rule": {
                        "filter_params": {
                            "expression": "False"
                        },
                        "responsibles": []
                    },
                    "instancectl_release_rule": {
                        "match_release_type": "-",
                        "queue_id": "INFRA",
                        "responsibles": []
                    },
                    "service_release_rules": [
                        {
                            "approve_policy": {
                                "multiple_approve": {
                                    "approvers": {
                                        "groups": [],
                                        "logins": []
                                    },
                                    "approves_count": 1,
                                    "mandatory_approvers": {
                                        "groups": [],
                                        "logins": []
                                    }
                                },
                                "type": "NONE"
                            },
                            "auto_commit_settings": {
                                "enabled": True,
                                "scheduling_priority": "NORMAL"
                            },
                            "desc": "update static",
                            "filter_params": {
                                "expression": "sandbox_release.release_type in (\"stable\", \"prestable\", \"testing\", \"unstable\")"
                            },
                            "queue_id": "INFRA",
                            "responsibles": [
                                {
                                    "email": "yanddmi@yandex-team.ru",
                                    "login": "yanddmi"
                                }
                            ],
                            "sandbox_resource_type": "SAMOGON_BUNDLE",
                            "sandbox_task_type": "YA_PACKAGE",
                            "ticket_priority": "NORMAL"
                        },
                        {
                            "approve_policy": {
                                "multiple_approve": {
                                    "approvers": {
                                        "groups": [],
                                        "logins": []
                                    },
                                    "approves_count": 1,
                                    "mandatory_approvers": {
                                        "groups": [],
                                        "logins": []
                                    }
                                },
                                "type": "NONE"
                            },
                            "auto_commit_settings": {
                                "enabled": True,
                                "scheduling_priority": "NORMAL"
                            },
                            "desc": "update layer",
                            "filter_params": {
                                "expression": "sandbox_release.release_type in (\"stable\", \"prestable\", \"testing\", \"unstable\")"
                            },
                            "queue_id": "INFRA",
                            "responsibles": [],
                            "sandbox_resource_type": "HORIZONTAL_POD_AUTOSCALER_CONTROLLER_PACKAGE",
                            "sandbox_task_type": "YA_PACKAGE",
                            "ticket_priority": "NORMAL"
                        }
                    ],
                    "service_release_tickets_enabled": True
                },
                "ui_settings": {}
            },
            "parent_id": "i6b3fg2bozu5xg324wcixyxh"
        },
        "runtime_attrs": {
            "_id": "b3baac7dc4550db7a666ce05f6dc8cde33793e47",
            "change_info": {
                "author": "nanny-robot",
                "comment": "YA_PACKAGE 645094298 (unstable)",
                "ctime": 1585925831992
            },
            "content": {
                "engines": {
                    "engine_type": "YP_LITE"
                },
                "instance_spec": {
                    "auxDaemons": [],
                    "containers": [],
                    "dockerImage": {
                        "name": "",
                        "registry": "registry.yandex.net"
                    },
                    "hostProvidedDaemons": [
                        {
                            "type": "HOST_SKYNET"
                        }
                    ],
                    "id": "",
                    "initContainers": [],
                    "instanceAccess": {
                        "skynetSsh": "ENABLED"
                    },
                    "instancectl": {
                        "fetchableMeta": {
                            "sandboxResource": {
                                "resourceId": "1402383617",
                                "resourceType": "INSTANCECTL",
                                "taskId": "631341753",
                                "taskType": ""
                            },
                            "type": "SANDBOX_RESOURCE"
                        },
                        "url": [
                            "rbtorrent:13e64e8763550435f01a500f0c1455f83cb0ead6"
                        ],
                        "version": "1.201"
                    },
                    "layersConfig": {
                        "bind": [],
                        "layer": [
                            {
                                "fetchableMeta": {
                                    "sandboxResource": {
                                        "resourceId": "1199437063",
                                        "resourceType": "PORTO_LAYER_SEARCH_UBUNTU_BIONIC",
                                        "taskId": "542939289",
                                        "taskType": "BUILD_PORTO_LAYER"
                                    },
                                    "type": "SANDBOX_RESOURCE"
                                },
                                "url": [
                                    "rbtorrent:4b92cd7b2182df255acea24b309972ec748ec696"
                                ]
                            },
                            {
                                "fetchableMeta": {
                                    "sandboxResource": {
                                        "resourceId": "1435195014",
                                        "resourceType": "HORIZONTAL_POD_AUTOSCALER_CONTROLLER_PACKAGE",
                                        "taskId": "645094298",
                                        "taskType": "YA_PACKAGE"
                                    },
                                    "type": "SANDBOX_RESOURCE"
                                },
                                "url": [
                                    "rbtorrent:6d6a7ab1e74ee8765984a6342a279181151ad0bd"
                                ]
                            }
                        ]
                    },
                    "networkProperties": {
                        "etcHosts": "KEEP_ETC_HOSTS",
                        "resolvConf": "USE_NAT64"
                    },
                    "notifyAction": {
                        "handlers": [],
                        "resourceRequest": {
                            "limit": [],
                            "request": []
                        }
                    },
                    "osContainerSpec": {
                        "auxDaemons": [],
                        "dockerImage": {
                            "name": "",
                            "registry": ""
                        },
                        "hostProvidedDaemons": [],
                        "initContainers": [],
                        "instanceAccess": {
                            "skynetSsh": "DISABLED"
                        },
                        "instancectl": {
                            "fetchableMeta": {
                                "sandboxResource": {
                                    "resourceId": "1126674332",
                                    "resourceType": "INSTANCECTL",
                                    "taskId": "510350972",
                                    "taskType": ""
                                },
                                "type": "SANDBOX_RESOURCE"
                            },
                            "url": [
                                "rbtorrent:fc184755054cbf861231c8b2e8df29dbe3036840"
                            ],
                            "version": "1.196"
                        },
                        "layersConfig": {
                            "bind": [],
                            "layer": [
                                {
                                    "fetchableMeta": {
                                        "sandboxResource": {
                                            "resourceId": "703602428",
                                            "resourceType": "PORTO_LAYER_SEARCH_UBUNTU_PRECISE_APP",
                                            "taskId": "313732989",
                                            "taskType": "BUILD_PORTO_LAYER"
                                        },
                                        "type": "SANDBOX_RESOURCE"
                                    },
                                    "url": [
                                        "rbtorrent:4eccf4fdbd83d58dc290d550ac82969724c53945"
                                    ]
                                }
                            ]
                        },
                        "networkProperties": {
                            "etcHosts": "KEEP_ETC_HOSTS",
                            "resolvConf": "DEFAULT_RESOLV_CONF"
                        },
                        "notifyAction": {
                            "handlers": [],
                            "resourceRequest": {
                                "limit": [],
                                "request": []
                            }
                        },
                        "type": "SANDBOX_LAYERS",
                        "volume": []
                    },
                    "qemuKvm": {
                        "image": {
                            "linux": {
                                "authorizedUsers": {
                                    "type": "SERVICE_MANAGERS",
                                    "userList": {
                                        "logins": [],
                                        "nannyGroupIds": [],
                                        "staffGroupIds": []
                                    }
                                },
                                "image": {
                                    "fetchableMeta": {
                                        "sandboxResource": {
                                            "resourceId": "409331232",
                                            "resourceType": "QEMU_IMAGE",
                                            "taskId": "178867853",
                                            "taskType": "REMOTE_COPY_RESOURCE"
                                        },
                                        "type": "SANDBOX_RESOURCE"
                                    },
                                    "url": [
                                        "rbtorrent:59f6fc1ff7efd59a8f37547dc4a4baebfa8af023"
                                    ]
                                }
                            },
                            "type": "LINUX",
                            "windows": {
                                "image": {
                                    "fetchableMeta": {
                                        "sandboxResource": {
                                            "resourceId": "",
                                            "resourceType": "",
                                            "taskId": "",
                                            "taskType": ""
                                        },
                                        "type": "SANDBOX_RESOURCE"
                                    },
                                    "url": []
                                }
                            }
                        },
                        "instancectl": {
                            "fetchableMeta": {
                                "sandboxResource": {
                                    "resourceId": "1126674332",
                                    "resourceType": "INSTANCECTL",
                                    "taskId": "510350972",
                                    "taskType": ""
                                },
                                "type": "SANDBOX_RESOURCE"
                            },
                            "url": [
                                "rbtorrent:fc184755054cbf861231c8b2e8df29dbe3036840"
                            ],
                            "version": "1.196"
                        },
                        "layersConfig": {
                            "fetchableMeta": {
                                "sandboxResource": {
                                    "resourceId": "562598663",
                                    "resourceType": "PORTO_LAYER_SEARCH_QEMU_UBUNTU_XENIAL",
                                    "taskId": "248826138",
                                    "taskType": ""
                                },
                                "type": "SANDBOX_RESOURCE"
                            },
                            "url": [
                                "rbtorrent:882c02f072d0c4d4bbf556fe50ceec626e795446"
                            ],
                            "version": "vmagent_last"
                        },
                        "vmagent": {
                            "fetchableMeta": {
                                "sandboxResource": {
                                    "resourceId": "947964256",
                                    "resourceType": "VMAGENT_PACK",
                                    "taskId": "428833900",
                                    "taskType": ""
                                },
                                "type": "SANDBOX_RESOURCE"
                            },
                            "url": [
                                "rbtorrent:278507f66246cc9f02a7ffae6242496d0929a096"
                            ],
                            "version": "0.20"
                        }
                    },
                    "type": "SANDBOX_LAYERS",
                    "volume": []
                },
                "instances": {
                    "chosen_type": "YP_POD_IDS",
                    "extended_gencfg_groups": {
                        "containers_settings": {
                            "slot_porto_properties": "NONE"
                        },
                        "groups": [],
                        "instance_properties_settings": {
                            "tags": "ALL_STATIC"
                        },
                        "network_settings": {
                            "hbf_nat": "enabled"
                        },
                        "tags": []
                    },
                    "gencfg_groups": [],
                    "instance_list": [],
                    "iss_settings": {
                        "hooks_resource_limits": {
                            "iss_hook_install": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_notify": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_reopenlogs": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_status": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_stop": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_uninstall": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            },
                            "iss_hook_validate": {
                                "cpu_policy": "normal",
                                "io_policy": "normal"
                            }
                        },
                        "instance_cls": "ru.yandex.iss.Instance"
                    },
                    "yp_pod_ids": {
                        "orthogonal_tags": {
                            "ctype": "unknown",
                            "itype": "unknown",
                            "metaprj": "unknown",
                            "prj": "test-danibw-hello-word",
                            "tier": "none"
                        },
                        "pods": [
                            {
                                "cluster": "SAS",
                                "pod_id": "test-yanddmi-hello-word-1"
                            }
                        ]
                    },
                    "yp_pods": {
                        "allocations": [],
                        "orthogonal_tags": {
                            "ctype": "unknown",
                            "itype": "unknown",
                            "metaprj": "unknown",
                            "prj": "default-content-service"
                        },
                        "tags": []
                    }
                },
                "resources": {
                    "l7_fast_balancer_config_files": [],
                    "sandbox_files": [
                        {
                            "extract_path": "",
                            "is_dynamic": False,
                            "local_path": "test_resource",
                            "resource_id": "1433328201",
                            "resource_type": "SAMOGON_BUNDLE",
                            "task_id": "644323048",
                            "task_type": "YA_PACKAGE"
                        }
                    ],
                    "services_balancer_config_files": [],
                    "static_files": [
                        {
                            "content": "my_data",
                            "is_dynamic": False,
                            "local_path": "my_static_file.txt"
                        },
                        {
                            "content": "port: {{ BSCONFIG_IPORT }}\n",
                            "is_dynamic": False,
                            "local_path": "config_template.yaml"
                        },
                        {
                            "content": "#!/usr/bin/env bash\n\n[sleeper]\nlimit_core = unlimited\nbinary = /bin/sh"
                            "\narguments = -c '\n\tenv;\n    sleep 10000\n    '\nenv_match = ^ENV_(.*)$"
                            "\nopt_match = ^OPT_(.*)$\nstatus_script = exit 0\nprepare_script = exit 0\nstop_script = exit 0",
                            "is_dynamic": False,
                            "local_path": "instancectl.conf"
                        },
                        {
                            "content": "ENV_YANDEX_ENV_TYPE=testing\nENV_BASE_PORT=%(BSCONFIG_IPORT)s\nENV_LD_LIBRARY_PATH=%(BSCONFIG_IDIR)s\nOPT_MY_OPTION=123",
                            "is_dynamic": False,
                            "local_path": "Conf.local"
                        }
                    ],
                    "template_set_files": [],
                    "url_files": []
                }
            },
            "meta_info": {
                "annotations": {
                    "deploy_engine": "YP_LITE",
                    "startable": "true"
                },
                "changes_record": {
                    "files": [
                        {
                            "sandbox_file_change": {
                                "resource_type": "HORIZONTAL_POD_AUTOSCALER_CONTROLLER_PACKAGE",
                                "task_id": "645094298",
                                "task_type": "YA_PACKAGE"
                            },
                            "type": "SANDBOX_FILE"
                        }
                    ]
                },
                "conf_id": "test_yanddmi_hello_word-1585925831992",
                "infra_notifications": {},
                "is_disposable": False,
                "scheduling_config": {
                    "sched_activate_recipe": "",
                    "sched_prepare_recipe": "",
                    "scheduling_priority": "NORMAL"
                },
                "startrek_tickets": [],
                "ticket_info": {
                    "release_id": "SANDBOX_RELEASE-645094298-UNSTABLE",
                    "ticket_id": "INFRA-46366340"
                }
            },
            "parent_id": "4136bb0f7a32d5fe6a04cd1ec412b615095a4489"
        },
        "target_state": {
            "_id": "",
            "content": {
                "info": {
                    "author": "nanny-robot",
                    "comment": "Maintaining active trunk",
                    "ctime": 1585925835960
                },
                "is_enabled": True,
                "labels": [],
                "prepare_recipe": "default",
                "recipe": "default",
                "recipe_parameters": [],
                "snapshot_id": "b3baac7dc4550db7a666ce05f6dc8cde33793e47",
                "snapshot_info": {
                    "author": "nanny-robot",
                    "comment": "YA_PACKAGE 645094298 (unstable)",
                    "ctime": 1585925831992
                },
                "snapshot_meta_info": {
                    "annotations": {
                        "deploy_engine": "YP_LITE",
                        "startable": "true"
                    },
                    "conf_id": "test_yanddmi_hello_word-1585925831992",
                    "is_disposable": False,
                    "revert_info": {
                        "last_transition_time": 0,
                        "message": "",
                        "reason": "",
                        "status": ""
                    },
                    "scheduling_config": {
                        "scheduling_priority": 1
                    },
                    "startrek_tickets": [],
                    "ticket_info": {
                        "release_id": "SANDBOX_RELEASE-645094298-UNSTABLE",
                        "ticket_id": "INFRA-46366340"
                    }
                },
                "snapshots": [
                    {
                        "info": {
                            "author": "nanny-robot",
                            "comment": "Implicitly disabled to activate another",
                            "ctime": 1585843737302
                        },
                        "labels": [],
                        "prepare_recipe": "default",
                        "recipe": "default",
                        "recipe_parameters": [],
                        "snapshot_id": "8ef95fe403de89864915228dcc3906844cf3d469",
                        "snapshot_info": {
                            "author": "nanny-robot",
                            "comment": "YA_PACKAGE 644313555 (unstable)",
                            "ctime": 1585843060315
                        },
                        "snapshot_meta_info": {
                            "annotations": {
                                "deploy_engine": "YP_LITE",
                                "startable": "true"
                            },
                            "conf_id": "test_yanddmi_hello_word-1585843060315",
                            "is_disposable": False,
                            "revert_info": {
                                "last_transition_time": 0,
                                "message": "",
                                "reason": "",
                                "status": ""
                            },
                            "scheduling_config": {
                                "scheduling_priority": 1
                            },
                            "startrek_tickets": [],
                            "ticket_info": {
                                "release_id": "SANDBOX_RELEASE-644313555-UNSTABLE",
                                "ticket_id": "INFRA-46335808"
                            }
                        },
                        "state": "PREPARED",
                        "tracked_tickets": {
                            "startrek_tickets": [],
                            "tickets": [
                                {
                                    "ticket_id": "INFRA-46335808"
                                }
                            ]
                        }
                    },
                    {
                        "info": {
                            "author": "nanny-robot",
                            "comment": "Implicitly disabled to activate another",
                            "ctime": 1585923181255
                        },
                        "labels": [],
                        "prepare_recipe": "default",
                        "recipe": "default",
                        "recipe_parameters": [],
                        "snapshot_id": "5d9f19f659098aaa45b063f838c3492ad73612a0",
                        "snapshot_info": {
                            "author": "nanny-robot",
                            "comment": "YA_PACKAGE 644323048 (unstable)",
                            "ctime": 1585843729404
                        },
                        "snapshot_meta_info": {
                            "annotations": {
                                "deploy_engine": "YP_LITE",
                                "startable": "true"
                            },
                            "conf_id": "test_yanddmi_hello_word-1585843729404",
                            "is_disposable": False,
                            "revert_info": {
                                "last_transition_time": 0,
                                "message": "",
                                "reason": "",
                                "status": ""
                            },
                            "scheduling_config": {
                                "scheduling_priority": 1
                            },
                            "startrek_tickets": [],
                            "ticket_info": {
                                "release_id": "SANDBOX_RELEASE-644323048-UNSTABLE",
                                "ticket_id": "INFRA-46336320"
                            }
                        },
                        "state": "PREPARED",
                        "tracked_tickets": {
                            "startrek_tickets": [],
                            "tickets": [
                                {
                                    "ticket_id": "INFRA-46336320"
                                }
                            ]
                        }
                    },
                    {
                        "info": {
                            "author": "nanny-robot",
                            "comment": "Implicitly disabled to activate another",
                            "ctime": 1585925835960
                        },
                        "labels": [],
                        "prepare_recipe": "default",
                        "recipe": "default",
                        "recipe_parameters": [],
                        "snapshot_id": "4136bb0f7a32d5fe6a04cd1ec412b615095a4489",
                        "snapshot_info": {
                            "author": "yanddmi",
                            "comment": "",
                            "ctime": 1585925163341
                        },
                        "snapshot_meta_info": {
                            "annotations": {
                                "deploy_engine": "YP_LITE",
                                "startable": "true"
                            },
                            "conf_id": "test_yanddmi_hello_word-1585925163341",
                            "is_disposable": False,
                            "revert_info": {
                                "last_transition_time": 0,
                                "message": "",
                                "reason": "",
                                "status": ""
                            },
                            "scheduling_config": {
                                "scheduling_priority": 1
                            },
                            "startrek_tickets": []
                        },
                        "state": "PREPARED",
                        "tracked_tickets": {
                            "startrek_tickets": [],
                            "tickets": []
                        }
                    }
                ],
                "tracked_tickets": {
                    "startrek_tickets": [],
                    "tickets": [
                        {
                            "ticket_id": "INFRA-46366340"
                        }
                    ]
                }
            },
            "info": {
                "author": "nanny-robot",
                "comment": "Maintaining active trunk",
                "ctime": 1585925835961
            }
        },
        "unique_id_index": "test-yanddmi-hello-word"
    },
        stage_id='',
        raise_on_error=False,
        mock_clients=True,
    ).migrate()
    return {
        "stage": json_format.MessageToJson(stage),
        "release_rules": [json_format.MessageToJson(rl) for rl in release_rules]
    }
