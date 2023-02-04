import pytest
from billing.hot.faas.tasklets.deploy_faas.impl.deploy_controller.stage import Stage
from ci.tasklet.common.proto.sandbox_pb2 import SandboxResource
from google.protobuf.json_format import MessageToDict


class TestHandleDuBase:
    @pytest.fixture
    def sequence_func(self, faas_tasklet, mock):
        return lambda: [
            Stage("mock", mock),
            Stage("testing filter input", faas_tasklet._filter_input_resources),
            Stage("testing get spec", faas_tasklet._get_current_spec),
            Stage("testing handle deploy units", faas_tasklet._handle_du),
        ]


class TestHandleDu(TestHandleDuBase):
    @pytest.fixture
    def spec(self):
        return {
            "account_id": "abc:cba",
            "revision": 1,
            "deploy_units": {
                "faas-tests-update": {
                    "replica_set": {
                        "replica_set_template": {
                            "pod_template_spec": {
                                "spec": {
                                    "pod_agent_payload": {
                                        "spec": {
                                            "resources": {
                                                "static_resources": [
                                                    {
                                                        "id": "faas",
                                                        "verification": {
                                                            "checksum": "EMPTY:",
                                                            "check_period_ms": 18000,
                                                        },
                                                        "url": "sbr:1",
                                                    },
                                                ]
                                            },
                                            "workloads": [],
                                            "boxes": [],
                                            "mutable_workloads": [],
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                "faas-tests-not_update": {
                    "replica_set": {
                        "replica_set_template": {
                            "pod_template_spec": {
                                "spec": {
                                    "pod_agent_payload": {
                                        "spec": {
                                            "resources": {
                                                "static_resources": [
                                                    {
                                                        "id": "faas",
                                                        "verification": {
                                                            "checksum": "MD5:verysafehash123",
                                                            "check_period_ms": 18000,
                                                        },
                                                        "url": "sbr:2",
                                                    },
                                                ]
                                            },
                                            "workloads": [],
                                            "boxes": [],
                                            "mutable_workloads": [],
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                "faas-tests-remove": {
                    "replica_set": {
                        "replica_set_template": {
                            "pod_template_spec": {
                                "spec": {
                                    "pod_agent_payload": {
                                        "spec": {
                                            "resources": {
                                                "static_resources": [
                                                    {
                                                        "id": "faas",
                                                        "verification": {
                                                            "checksum": "MD5:verysafehash123",
                                                            "check_period_ms": 18000,
                                                        },
                                                        "url": "sbr:3",
                                                    },
                                                ]
                                            },
                                            "workloads": [],
                                            "boxes": [],
                                            "mutable_workloads": [],
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
            },
        }

    @pytest.fixture
    def faas_resources(self, env):
        return [
            SandboxResource(
                id=2,
                type="FAAS_RESOURCE",
                task_id=1,
                attributes={
                    "peerdirs": '"billing/hot/faas/tests"',
                    "revision": "1",
                    "ttl": "inf",
                    "functions": '[{"function": "billing.test.function", "name": "calc"}]',
                    "tenant": "tests",
                    "namespaces": '["tests"]',
                    "instance": "update",
                    "current_environment": env.value,
                },
            ),
            SandboxResource(
                id=3,
                type="FAAS_RESOURCE",
                task_id=1,
                attributes={
                    "peerdirs": '"billing/hot/faas/tests"',
                    "revision": "1",
                    "ttl": "inf",
                    "functions": '[{"function": "billing.test.function", "name": "calc"}]',
                    "namespaces": '["tests"]',
                    "tenant": "tests",
                    "instance": "not_update",
                    "current_environment": env.value,
                },
            ),
        ]

    @pytest.fixture
    def correct_deploy_units(self, env):
        return {
            "faas-tests-not_update": {
                "replica_set": {
                    "replica_set_template": {
                        "pod_template_spec": {
                            "spec": {
                                "pod_agent_payload": {
                                    "spec": {
                                        "resources": {
                                            "static_resources": [
                                                {
                                                    "id": "faas",
                                                    "verification": {
                                                        "checksum": "MD5:verysafehash123",
                                                        "check_period_ms": "18000",
                                                    },
                                                    "url": "sbr:2",
                                                }
                                            ]
                                        }
                                    }
                                }
                            }
                        }
                    },
                    "per_cluster_settings": {
                        "sas": {
                            "pod_count": 1,
                            "deployment_strategy": {"max_unavailable": 1},
                        },
                        "vla": {
                            "pod_count": 1,
                            "deployment_strategy": {"max_unavailable": 1},
                        },
                    },
                }
            },
            "faas-tests-update": {
                "replica_set": {
                    "replica_set_template": {
                        "pod_template_spec": {
                            "spec": {
                                "pod_agent_payload": {
                                    "spec": {
                                        "resources": {
                                            "static_resources": [
                                                {
                                                    "id": "faas",
                                                    "verification": {
                                                        "checksum": "MD5:verysafehash123",
                                                        "check_period_ms": "18000",
                                                    },
                                                    "url": "sbr:2",
                                                }
                                            ]
                                        }
                                    }
                                }
                            }
                        },
                    },
                    "per_cluster_settings": {
                        "sas": {
                            "pod_count": 1,
                            "deployment_strategy": {"max_unavailable": 1},
                        },
                        "vla": {
                            "pod_count": 1,
                            "deployment_strategy": {"max_unavailable": 1},
                        },
                    },
                }
            },
        }

    def result_to_dict(self, result):
        res = {}
        for name, data in result.items():
            res[name] = MessageToDict(result[name], preserving_proto_field_name=True)
        return res

    def test(self, tasklet_with_sequence, correct_deploy_units):
        tasklet_with_sequence.run()
        assert correct_deploy_units == self.result_to_dict(tasklet_with_sequence.context.spec.deploy_units)


class TestFunctionUpdate(TestHandleDuBase):
    @pytest.fixture
    def spec(self):
        return {
            "account_id": "abc:cba",
            "revision": 1,
            "deploy_units": {
                "faas-tests-update-function": {
                    "replica_set": {
                        "replica_set_template": {
                            "pod_template_spec": {
                                "spec": {
                                    "pod_agent_payload": {
                                        "spec": {
                                            "resources": {
                                                "static_resources": [
                                                    {
                                                        "id": "faas",
                                                        "verification": {
                                                            "checksum": "MD5:verysafehash123",
                                                            "check_period_ms": 18000,
                                                        },
                                                        "url": "sbr:2",
                                                    },
                                                ]
                                            },
                                            "workloads": [],
                                            "boxes": [
                                                {
                                                    "id": "box-api",
                                                    "static_resources": [
                                                        {
                                                            "resource_ref": "faas",
                                                            "mount_point": "/opt/bin",
                                                        }
                                                    ],
                                                    "env": [
                                                        {
                                                            "name": "YENV_TYPE",
                                                            "value": {"literal_env": {"value": "testing"}},
                                                        },
                                                        {
                                                            "name": "TRACING_LOCAL_AGENT",
                                                            "value": {"literal_env": {"value": "localhost:6831"}},
                                                        },
                                                        {
                                                            "name": "FUNCTION",
                                                            "value": {
                                                                "literal_env": {
                                                                    "value": '[{"function": "billing.hot.faas.lib.python.dummy.dummy_calculator.dummy_calculator", "name": "calc"}]'
                                                                }
                                                            },
                                                        },
                                                    ],
                                                }
                                            ],
                                            "mutable_workloads": [],
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
            },
        }

    @pytest.fixture
    def faas_resources(self, env):
        return [
            SandboxResource(
                id=2,
                type="FAAS_RESOURCE",
                task_id=1,
                attributes={
                    "peerdirs": '"billing/hot/faas/tests"',
                    "revision": "1",
                    "ttl": "inf",
                    "functions": '[{"function": "totally.new.function", "name": "new_function"}]',
                    "namespaces": '["tests"]',
                    "tenant": "tests",
                    "instance": "update-function",
                    "current_environment": env.value,
                },
            )
        ]

    @pytest.fixture
    def correct_spec(self):
        return {
            "account_id": "abc:cba",
            "revision": 1,
            "deploy_units": {
                "faas-tests-update-function": {
                    "replica_set": {
                        "replica_set_template": {
                            "pod_template_spec": {
                                "spec": {
                                    "pod_agent_payload": {
                                        "spec": {
                                            "resources": {
                                                "static_resources": [
                                                    {
                                                        "id": "faas",
                                                        "verification": {
                                                            "checksum": "MD5:verysafehash123",
                                                            "check_period_ms": "18000",
                                                        },
                                                        "url": "sbr:2",
                                                    }
                                                ]
                                            },
                                            "boxes": [
                                                {
                                                    "id": "box-api",
                                                    "static_resources": [
                                                        {
                                                            "resource_ref": "faas",
                                                            "mount_point": "/opt/bin",
                                                        }
                                                    ],
                                                    "env": [
                                                        {
                                                            "name": "YENV_TYPE",
                                                            "value": {"literal_env": {"value": "testing"}},
                                                        },
                                                        {
                                                            "name": "TRACING_LOCAL_AGENT",
                                                            "value": {"literal_env": {"value": "localhost:6831"}},
                                                        },
                                                        {
                                                            "name": "FUNCTION",
                                                            "value": {
                                                                "literal_env": {
                                                                    "value": '[{"function": "totally.new.function", '
                                                                    '"name": "new_function"}]'
                                                                }
                                                            },
                                                        },
                                                    ],
                                                }
                                            ],
                                        }
                                    }
                                }
                            }
                        },
                        "per_cluster_settings": {
                            "sas": {
                                "pod_count": 1,
                                "deployment_strategy": {"max_unavailable": 1},
                            },
                            "vla": {
                                "pod_count": 1,
                                "deployment_strategy": {"max_unavailable": 1},
                            },
                        },
                    }
                }
            },
        }

    def test(self, tasklet_with_sequence, correct_spec):
        tasklet_with_sequence.run()
        spec = MessageToDict(tasklet_with_sequence.context.spec, preserving_proto_field_name=True)
        assert spec == correct_spec
