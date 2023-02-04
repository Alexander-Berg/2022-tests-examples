import pytest
from ci.tasklet.common.proto.sandbox_pb2 import SandboxResource
from google.protobuf.json_format import MessageToDict


class TestDuUpdateWithDefault:
    """
    Initial: spec with 2 dus

    Plan: add one du with update and the other without -> run tasklet

    Result: spec with one updated du and one unchanged
    """

    @pytest.fixture
    def faas_resources(self):
        return [
            # new version
            SandboxResource(
                id=2,
                type="FAAS_RESOURCE",
                task_id=1,
                attributes={
                    "peerdirs": '"billing/hot/faas/tests"',
                    "revision": "1",
                    "ttl": "inf",
                    "functions": '[{"function": "billing.hot.faas.lib.python.dummy.dummy_calculator.dummy_calculator", "name": "calc"}]',
                    "tenant": "tests",
                    "instance": "update",
                },
            ),
            # the same version
            SandboxResource(
                id=11,
                type="FAAS_RESOURCE",
                task_id=1,
                attributes={
                    "peerdirs": '"billing/hot/faas/tests"',
                    "revision": "1",
                    "ttl": "inf",
                    "functions": '[{"function": "billing.hot.faas.lib.python.dummy.dummy_calculator.dummy_calculator", "name": "calc"}]',
                    "tenant": "tests",
                    "instance": "not_update",
                },
            ),
        ]

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
                                                        "url": "sbr:11",
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
            },
        }

    @pytest.fixture
    def correct_spec(self):
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
                                                            "checksum": "MD5:verysafehash123",
                                                            "check_period_ms": "18000",
                                                        },
                                                        "url": "sbr:2",
                                                    },
                                                ]
                                            },
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
                                                        "url": "sbr:11",
                                                    },
                                                ]
                                            },
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
            },
        }

    def test(self, default_sequence_tasklet, correct_spec):
        default_sequence_tasklet.run()
        spec = MessageToDict(default_sequence_tasklet.context.spec, preserving_proto_field_name=True)
        assert spec == correct_spec
