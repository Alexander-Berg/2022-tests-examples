import pytest
from ci.tasklet.common.proto.sandbox_pb2 import SandboxResource
from google.protobuf.json_format import MessageToDict


class TestNoUpdates:
    """
    Initial: spec with one resource

    Plan: add FAAS_RESOURCE with next revision -> run tasklet

    Result: spec with updated resource
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
                    "namespace": "tests",
                    "instance": "no-updates",
                },
            ),
        ]

    @pytest.fixture
    def spec(self):
        return {
            "account_id": "abc:cba",
            "revision": 1,
            "deploy_units": {
                "faas-tests-no-updates": {
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
                "faas-tests-no-updates": {
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
            "account_id": "abc:cba",
        }

    def test(self, default_sequence_tasklet, correct_spec):
        default_sequence_tasklet.run()
        spec = MessageToDict(default_sequence_tasklet.context.spec, preserving_proto_field_name=True)
        assert spec == correct_spec
