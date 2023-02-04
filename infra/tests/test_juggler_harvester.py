from infra.rtc_sla_tentacles.backend.lib.harvesters.juggler import JugglerHarvester, StateKind, Status, Description


juggler_api_response_json = {
    "az1": {
        "timestamp-age": {
            "status": ["OK", 1566347353.744464],
            "methods": [],
            "check_time": 1566385151.281216,
            "tags": [
                "rtc_sla_tentacles_production",
                "a_mark_test_juggler_check_hostname"
            ],
            "notification_templates": [],
            "description_hash": "868E35894E56F85D8E81A8B92409B1A0",
            "description": [{
                "status": "OK",
                "downtime_ids": [],
                "description": "",
                "children": {
                    "tentacle1.man-pre.yp-c.yandex.net": {
                        "timestamp-age": {
                            "80": {
                                "status": "OK",
                                "downtime_ids": [],
                                "description": "http: body 'Ok' [2a02:6b8:c14:3b26:0:45f3:8108:0]",
                                "service_name": "timestamp-age",
                                "instance_name": "80",
                                "status_mtime": 1566311927,
                                "host_name": "tentacle1.man-pre.yp-c.yandex.net",
                                "state_kind": "actual",
                                "is_check": False
                            }
                        }
                    },
                    "tentacle2.man-pre.yp-c.yandex.net": {
                        "timestamp-age": {
                            "80": {
                                "status": "WARN",
                                "downtime_ids": [],
                                "description": ("http: body 'ERROR: Some resource error' "
                                                "[2a02:6b8:c08:afa2:0:45f3:43ca:0]"),
                                "service_name": "timestamp-age",
                                "instance_name": "80",
                                "status_mtime": 1566311933,
                                "host_name": "tentacle2.man-pre.yp-c.yandex.net",
                                "state_kind": "actual",
                                "is_check": False
                            }
                        }
                    },
                    "tentacle3.man-pre.yp-c.yandex.net": {
                        "timestamp-age": {
                            "80": {
                                "status": "CRIT",
                                "downtime_ids": [],
                                "description": ("http: response: bad response code 418, "
                                                "body 'Timestamp too old' [2a02:6b8:c01:86b:0:604:2d04:2ad0]"),
                                "service_name": "timestamp-age",
                                "instance_name": "80",
                                "status_mtime": 1566311950,
                                "host_name": "tentacle3.man-pre.yp-c.yandex.net",
                                "state_kind": "actual",
                                "is_check": False
                            }
                        }
                    },
                    "tentacle4.man-pre.yp-c.yandex.net": {
                        "timestamp-age": {
                            "80": {
                                "status": "CRIT",
                                "downtime_ids": [],
                                "description": ("http: connect: [Errno 111] Connection refused "
                                                "[2a02:6b8:c08:fa1f:0:45f3:9a9e:0]"),
                                "service_name": "timestamp-age",
                                "instance_name": "80",
                                "status_mtime": 1566311904,
                                "host_name": "tentacle4.man-pre.yp-c.yandex.net",
                                "state_kind": "actual",
                                "is_check": False
                            }
                        }
                    },
                    "tentacle5.man-pre.yp-c.yandex.net": {
                        "timestamp-age": {
                            "80": {
                                "status": "CRIT",
                                "downtime_ids": [],
                                "description": "Invalid hostname given",
                                "service_name": "timestamp-age",
                                "instance_name": "80",
                                "status_mtime": 1566311919,
                                "host_name": "tentacle5.man-pre.yp-c.yandex.net",
                                "state_kind": "unreach_skip",
                                "is_check": False
                            }
                        }
                    },
                    "tentacle6.man-pre.yp-c.yandex.net": {
                        "timestamp-age": {
                            "80": {
                                "status": "OK",
                                "downtime_ids": ["0123aa"],
                                "description": "Forced OK by downtimes [2a02:6b8:c14:3d28:0:45f3:912f:0]",
                                "service_name": "timestamp-age",
                                "instance_name": "80",
                                "status_mtime": 1566347256,
                                "host_name": "tentacle6.man-pre.yp-c.yandex.net",
                                "state_kind": "downtime_force_ok",
                                "is_check": False
                            }
                        }
                    },
                    "tentacle7.man-pre.yp-c.yandex.net": {
                        "timestamp-age": {
                            "80": {
                                "status": "OK",
                                "downtime_ids": [],
                                "description": "http: body 'Ok' [2a02:6b8:c14:371c:0:45f3:dd2c:0]",
                                "service_name": "timestamp-age",
                                "instance_name": "80",
                                "status_mtime": 1566311936,
                                "host_name": "tentacle7.man-pre.yp-c.yandex.net",
                                "state_kind": "actual",
                                "is_check": False
                            }
                        }
                    },
                    "tentacle8.man-pre.yp-c.yandex.net": {
                        "timestamp-age": {
                            "80": {
                                "status": "OK",
                                "downtime_ids": [],
                                "description": "http: body 'Ok' [2a02:6b8:c14:3d20:0:45f3:dd09:0]",
                                "service_name": "timestamp-age",
                                "instance_name": "80",
                                "status_mtime": 1566311918,
                                "host_name": "tentacle8.man-pre.yp-c.yandex.net",
                                "state_kind": "actual",
                                "is_check": False
                            }
                        }
                    },
                    "tentacle9.man-pre.yp-c.yandex.net": {
                        "timestamp-age": {
                            "80": {
                                "status": "OK",
                                "downtime_ids": [],
                                "description": ("[forced by stable_time] http: connect: [Errno 110] Connection timed "
                                                "out [2a02:6b8:c0b:64a4:0:44a1:cbf1:0]"),
                                "service_name": "timestamp-age",
                                "instance_name": "80",
                                "status_mtime": 1566311918,
                                "host_name": "tentacle9.man-pre.yp-c.yandex.net",
                                "state_kind": "flapping",
                                "is_check": False
                            }
                        }
                    },
                    "tentacle10.man-pre.yp-c.yandex.net": {
                        "timestamp-age": {
                            "80": {
                                "status": "CRIT",
                                "downtime_ids": [],
                                "description": "NO DATA",
                                "service_name": "timestamp-age",
                                "instance_name": "80",
                                "status_mtime": 1566311918,
                                "host_name": "tentacle10.man-pre.yp-c.yandex.net",
                                "state_kind": "no_data_force_crit",
                                "is_check": False
                            }
                        }
                    },
                },
                "summary": "0 from 8 failed",
                "is_check": True,
                "state_kind": "actual",
                "counts": {
                    "CRIT": 4,
                    "WARN": 1,
                    "OK": 5,
                    "INFO": 0
                }
            },
                1566347353.744464
            ]
        }
    }
}

expected_values = {
    "values": [
        {
            "fqdn": "tentacle1.man-pre.yp-c.yandex.net",
            "state_kind": StateKind.ACTUAL,
            "status": Status.OK,
            "description": Description.OK,
        },
        {
            "fqdn": "tentacle2.man-pre.yp-c.yandex.net",
            "state_kind": StateKind.ACTUAL,
            "status": Status.WARN,
            "description": Description.RESOURCE_ERROR,
        },
        {
            "fqdn": "tentacle3.man-pre.yp-c.yandex.net",
            "state_kind": StateKind.ACTUAL,
            "status": Status.CRIT,
            "description": Description.TIMESTAMP_TOO_OLD,
        },
        {
            "fqdn": "tentacle4.man-pre.yp-c.yandex.net",
            "state_kind": StateKind.ACTUAL,
            "status": Status.CRIT,
            "description": Description.HTTP_CONNECT_ERROR,
        },
        {
            "fqdn": "tentacle5.man-pre.yp-c.yandex.net",
            "state_kind": StateKind.OTHER,
            "status": Status.CRIT,
            "description": Description.INVALID_HOSTNAME,
        },
        {
            "fqdn": "tentacle6.man-pre.yp-c.yandex.net",
            "state_kind": StateKind.DOWNTIME_FORCE_OK,
            "status": Status.OK,
            "description": Description.OTHER,
        },
        {
            "fqdn": "tentacle7.man-pre.yp-c.yandex.net",
            "state_kind": StateKind.ACTUAL,
            "status": Status.OK,
            "description": Description.OK,
        },
        {
            "fqdn": "tentacle8.man-pre.yp-c.yandex.net",
            "state_kind": StateKind.ACTUAL,
            "status": Status.OK,
            "description": Description.OK,
        },
        {
            "fqdn": "tentacle9.man-pre.yp-c.yandex.net",
            "state_kind": StateKind.FLAPPING,
            "status": Status.OK,
            "description": Description.HTTP_CONNECT_ERROR,
        },
        {
            "fqdn": "tentacle10.man-pre.yp-c.yandex.net",
            "state_kind": StateKind.NO_DATA_FORCE_CRIT,
            "status": Status.CRIT,
            "description": Description.NO_DATA,
        },
    ]
}


expected_meta = {
    "count_values_types": {
        "state_kind": {
            "ACTUAL": 6,
            "DOWNTIME_FORCE_OK": 1,
            "FLAPPING": 1,
            "NO_DATA_FORCE_CRIT": 1,
            "OTHER": 1
        },
        "status": {
            "OK": 5,
            "WARN": 1,
            "CRIT": 4,
            "OTHER": 0
        },
        "description": {
            "OK": 3,
            "TIMESTAMP_TOO_OLD": 1,
            "RESOURCE_ERROR": 1,
            "ICMPPING_DOWN": 0,
            "INVALID_HOSTNAME": 1,
            "HTTP_CONNECT_ERROR": 2,
            "NO_DATA": 1,
            "OTHER": 1
        }
    }
}


def test_juggler_harvester_transform(config_interface, fake_snapshot_manager):
    arguments = {
        "host_name": "test_juggler_check_hostname",
        "service_name": "timestamp-age"
    }
    h = JugglerHarvester(name="az1",
                         arguments=arguments,
                         common_parameters=None,
                         common_settings=None,
                         snapshot_manager=fake_snapshot_manager,
                         config_interface=config_interface,
                         several_harvesters=False)
    actual_meta, actual_values = h.transform(1, juggler_api_response_json)
    assert actual_meta == expected_meta
    assert actual_values == expected_values
