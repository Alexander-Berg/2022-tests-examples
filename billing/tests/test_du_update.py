import pytest
from ci.tasklet.common.proto.sandbox_pb2 import SandboxResource
from google.protobuf.json_format import MessageToDict


class TestDuUpdate:
    """
    Initial: spec with one resource

    Plan: add FAAS_RESOURCE with next revision -> run tasklet

    Result: spec with updated resource
    """

    @pytest.fixture
    def faas_resources(self):
        return [
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
                    "instance": "main",
                },
            ),
            SandboxResource(
                id=2,
                type="LOGS_RESOURCE",
                task_id=2,
                attributes={"test": "test", "testing": "testing"},
            ),
        ]

    @pytest.fixture
    def spec(self):
        return {
            "account_id": "abc:cba",
            "revision": 1,
            "deploy_units": {
                "faas-tests-main": {
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
                                                            "checksum": "EMPTY:",  # template will create different
                                                            # hash for the given resource
                                                            "check_period_ms": 18000,
                                                        },
                                                        "url": "sbr:1",
                                                    }
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
                }
            },
            "revision_info": {"description": "test spec"},
        }

    @pytest.fixture
    def correct_spec(self):
        return {
            "revision": 1,
            "deploy_units": {
                "faas-tests-main": {
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
                    },
                }
            },
            "account_id": "abc:cba",
            "revision_info": {"description": "test spec"},
        }

    def test(self, default_sequence_tasklet, correct_spec):
        default_sequence_tasklet.run()
        spec = MessageToDict(default_sequence_tasklet.context.spec, preserving_proto_field_name=True)
        assert spec == correct_spec


class TestDuUpdateClusterSettings:
    """
    Initial: spec with one resource

    Plan: add FAAS_RESOURCE with same version, but different cluster config -> run tasklet

    Result: spec with updated cluster config
    """

    @pytest.fixture
    def faas_resources(self):
        return [
            SandboxResource(
                id=1,
                type="FAAS_RESOURCE",
                task_id=1,
                attributes={
                    "peerdirs": '"billing/hot/faas/tests"',
                    "revision": "1",
                    "ttl": "inf",
                    "functions": '[{"function": "billing.hot.faas.lib.python.dummy.dummy_calculator.dummy_calculator", "name": "calc"}]',
                    "tenant": "tests",
                    "instance": "cluster-settings-update",
                    "instance_settings": '{"active": true, "dcs": [{"name": "sas", "amount": 5}]}',
                },
            ),
        ]

    @pytest.fixture
    def spec(self):
        return {
            "account_id": "abc:cba",
            "revision": 1,
            "deploy_units": {
                "faas-tests-cluster-settings-update": {
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
                                                        "url": "sbr:1",
                                                    }
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
                }
            },
        }

    @pytest.fixture
    def correct_spec(self):
        return {
            "revision": 1,
            "deploy_units": {
                "faas-tests-cluster-settings-update": {
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
                                                        "url": "sbr:1",
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
                                "pod_count": 5,
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
            "account_id": "abc:cba",
        }

    def test(self, default_sequence_tasklet, correct_spec):
        default_sequence_tasklet.run()
        spec = MessageToDict(default_sequence_tasklet.context.spec, preserving_proto_field_name=True)
        assert spec == correct_spec
