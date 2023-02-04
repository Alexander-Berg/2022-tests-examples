import pytest
from google.protobuf.json_format import MessageToDict
from copy import deepcopy
from ci.tasklet.common.proto.sandbox_pb2 import SandboxResource
from billing.hot.faas.tasklets.deploy_faas.impl.enums import Env


class TestDuAdd:
    """
    Initial: empty spec

    Plan: add FAAS_RESOURCE and some other resource -> run tasklet

    Result: spec with FAAS_RESOURCE
    """

    @pytest.fixture
    def faas_resources(self):
        return [
            # valid resource
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
                    "instance": "add",
                },
            ),
            # valid resource that won't be filtered
            SandboxResource(
                id=2,
                type="LOGS_RESOURCE",
                task_id=2,
                attributes={"test": "test", "testing": "testing"},
            ),
        ]

    @pytest.fixture
    def spec(self, base_spec):
        return deepcopy(base_spec)

    @pytest.fixture
    def correct_spec(self, env):

        api_workload_env = [
            {
                "name": "URL_PREFIX",
                "value": {"literal_env": {"value": "/faas-tests-add"}},
            },
        ]

        secret_refs = {
            "test_secret_name": {
                "secret_id": "sec-tvm-test",
                "secret_version": "test_version",
            }
        }

        if env not in [Env.DEVELOPMENT, ]:
            api_workload_env.extend(
                [
                    {
                        "name": "ERROR_BOOSTER_ENABLED",
                        "value": {"literal_env": {"value": "true"}},
                    },
                    {
                        "name": "TRACING_ENABLED",
                        "value": {"literal_env": {"value": "true"}},
                    },
                ]
            )

        if env not in [Env.DEVELOPMENT, ]:
            api_workload_env.extend(
                [
                    {
                        "name": "ERROR_BOOSTER_UA_ENABLED",
                        "value": {"literal_env": {"value": "true"}},
                    },
                ]
            )

        spec = {
            "revision": 1,
            "deploy_units": {
                "faas-tests-add": {
                    "network_defaults": {"network_id": "_BILLING_DEPLOY_FAAS_TEST_NETS_"},
                    "images_for_boxes": {
                        "box-otel-agent": {
                            "registry_host": "registry.yandex.net",
                            "name": "rtc-base/focal",
                            "tag": "sb-1113623707",
                        },
                        "box-nginx": {
                            "registry_host": "registry.yandex.net",
                            "name": "balance/nginx-ssl",
                            "tag": "svn.9474466-sandbox.1314217226",
                        },
                        "box-unified-agent": {
                            "registry_host": "registry.yandex.net",
                            "name": "rtc-base/focal",
                            "tag": "sb-1113623707",
                        },
                        "box-api": {
                            "registry_host": "registry.yandex.net",
                            "name": "rtc-base/focal",
                            "tag": "sb-1113623707",
                        },
                    },
                    "endpoint_sets": [{"port": 443}],
                    "box_juggler_configs": {
                        "box-unified-agent": {
                            "archived_checks": [{"url": "https://proxy.sandbox.yandex-team.ru/2084119605"}],
                            "port": 31582,
                        },
                        "box-nginx": {
                            "archived_checks": [{"url": "https://proxy.sandbox.yandex-team.ru/2084119605"}],
                            "port": 31580,
                        },
                        "box-otel-agent": {
                            "archived_checks": [{"url": "https://proxy.sandbox.yandex-team.ru/2084119605"}],
                            "port": 31581,
                        },
                        "box-api": {
                            "archived_checks": [{"url": "https://proxy.sandbox.yandex-team.ru/2084119605"}],
                            "port": 31579,
                        },
                    },
                    "logrotate_configs": {
                        "box-nginx": {
                            "raw_config": "include /etc/logrotate.d",
                            "run_period_millisecond": "3600000",
                        }
                    },
                    "replica_set": {
                        "replica_set_template": {
                            "pod_template_spec": {
                                "spec": {
                                    "resource_requests": {
                                        "vcpu_guarantee": f"{2000 if env == Env.PRODUCTION else 1000}",
                                        "vcpu_limit": f"{2000 if env == Env.PRODUCTION else 1000}",
                                        "memory_guarantee": "1073741824",
                                        "memory_limit": "1073741824",
                                        "network_bandwidth_guarantee": f"{10485760 if env == Env.PRODUCTION else 3145728}",
                                    },
                                    "disk_volume_requests": [
                                        {
                                            "id": "disk-0",
                                            "labels": {
                                                "attributes": [
                                                    {
                                                        "key": "used_by_infra",
                                                        "value": "JXRydWU=",
                                                    }
                                                ]
                                            },
                                            "storage_class": "ssd",
                                            "quota_policy": {
                                                "capacity": "5368709120",
                                                "bandwidth_guarantee": "2097152",
                                                "bandwidth_limit": "2097152",
                                            },
                                        }
                                    ],
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
                                                    },
                                                    {
                                                        "id": "nginx_ssl",
                                                        "verification": {
                                                            "checksum": "EMPTY:",
                                                            "check_period_ms": "18000",
                                                        },
                                                        "files": {
                                                            "files": [
                                                                {
                                                                    "file_name": "faas.crt",
                                                                    "secret_data": {
                                                                        "id": "secret_certificate",
                                                                        "alias": "certificate_secret_private_key",
                                                                    },
                                                                },
                                                                {
                                                                    "file_name": "faas.key",
                                                                    "secret_data": {
                                                                        "id": "secret_private_key",
                                                                        "alias": "certificate_secret_private_key",
                                                                    },
                                                                },
                                                            ]
                                                        },
                                                    },
                                                    {
                                                        "id": "config-otel.yaml",
                                                        "verification": {
                                                            "checksum": "MD5:d2d6855046880d8b9996f8b04c04ebec",
                                                            "check_period_ms": "18000",
                                                        },
                                                        "url": "sbr:2342437831",
                                                    },
                                                    {
                                                        "id": "nginx.conf",
                                                        "verification": {
                                                            "checksum": "MD5:c42e9374d129210b80ff08ad377dd357",
                                                            "check_period_ms": "18000",
                                                        },
                                                        "url": "sbr:2512700458",
                                                    },
                                                    {
                                                        "id": "unified_agent_config",
                                                        "verification": {
                                                            "checksum": "MD5:9b74bba99c874bf2a3895c96473c1280",
                                                            "check_period_ms": "18000",
                                                        },
                                                        "url": "sbr:2513637215",
                                                    },
                                                    {
                                                        "id": "unified_agent",
                                                        "verification": {
                                                            "checksum": "MD5:c0d020b1a0527405a3ea4d8d560ef6c2",
                                                            "check_period_ms": "18000",
                                                        },
                                                        "url": "sbr:3372753689",
                                                    },
                                                    {
                                                        "id": "otel_agent",
                                                        "verification": {
                                                            "checksum": "EMPTY:",
                                                            "check_period_ms": "18000",
                                                        },
                                                        "url": "sbr:3037768036",
                                                    },
                                                ]
                                            },
                                            "workloads": [
                                                {
                                                    "id": "nginx-workload",
                                                    "readiness_check": {
                                                        "http_get": {
                                                            "port": 80,
                                                            "path": "/ping",
                                                            "expected_answer": "pong",
                                                        }
                                                    },
                                                    "box_ref": "box-nginx",
                                                    "start": {"command_line": "nginx -c /etc/nginx/nginx.conf"},
                                                },
                                                {
                                                    "id": "api-workload",
                                                    "env": api_workload_env,
                                                    "readiness_check": {
                                                        "http_get": {
                                                            "port": 9000,
                                                            "path": "/ping",
                                                            "expected_answer": "pong",
                                                        }
                                                    },
                                                    "stop_policy": {
                                                        "max_tries": 3,
                                                        "container": {
                                                            "command_line": 'bash -c "pkill -SIGUSR1 faas && sleep 15 && pkill faas"'
                                                        },
                                                    },
                                                    "box_ref": "box-api",
                                                    "start": {
                                                        "command_line": 'bash -c "chmod a+x /opt/bin/faas && /opt/bin/faas runserver --port 9000 --host ::1"'
                                                    },
                                                },
                                                {
                                                    "id": "otel-agent-workload",
                                                    "readiness_check": {
                                                        "http_get": {
                                                            "port": 13133,
                                                            "path": "/",
                                                            "any": True,
                                                        }
                                                    },
                                                    "box_ref": "box-otel-agent",
                                                    "liveness_check": {
                                                        "http_get": {
                                                            "port": 13133,
                                                            "path": "/",
                                                            "any": True,
                                                        }
                                                    },
                                                    "start": {
                                                        "command_line": (
                                                            "/otel_agent_bin/otelcontribcol --config /etc/otel/config.yaml "
                                                            "--set exporters.jaeger.endpoint"
                                                            "=billing.c.jaeger.yandex-team.ru:14250"
                                                            if env == Env.PRODUCTION
                                                            else "/otel_agent_bin/otelcontribcol "
                                                            "--config /etc/otel/config.yaml "
                                                            "--metrics-level none"
                                                        )
                                                    },
                                                },
                                                {
                                                    "id": "unified-agent-workload",
                                                    "env": [
                                                        {
                                                            "name": "LOGBROKER_TOPIC",
                                                            "value": {"literal_env": {"value": "test/topic"}},
                                                        },
                                                        {
                                                            "name": "TVM_CLIENT_ID",
                                                            "value": {"literal_env": {"value": "123"}},
                                                        },
                                                        {
                                                            "name": "TVM_SECRET",
                                                            "value": {
                                                                "secret_env": {
                                                                    "id": "client_secret",
                                                                    "alias": "tvm.secret.123",
                                                                }
                                                            },
                                                        },
                                                    ],
                                                    "readiness_check": {
                                                        "http_get": {
                                                            "port": 22502,
                                                            "path": "/ready",
                                                            "expected_answer": "OK",
                                                        }
                                                    },
                                                    "box_ref": "box-unified-agent",
                                                    "liveness_check": {
                                                        "http_get": {
                                                            "port": 22502,
                                                            "path": "/status",
                                                            "any": True,
                                                        }
                                                    },
                                                    "start": {
                                                        "command_line": "/unified_agent_bin/unified_agent -c /unified_agent_config/config.yaml"
                                                    },
                                                },
                                            ],
                                            "boxes": [
                                                {
                                                    "id": "box-api",
                                                    "static_resources": [
                                                        {
                                                            "resource_ref": "faas",
                                                            "mount_point": "/opt/bin",
                                                        }
                                                    ],
                                                    "compute_resources": {
                                                        "vcpu_guarantee": f"{1000 if env == Env.PRODUCTION else 500}",
                                                        "vcpu_limit": f"{1000 if env == Env.PRODUCTION else 500}",
                                                    },
                                                    "env": [
                                                        {
                                                            "name": "YENV_TYPE",
                                                            "value": {"literal_env": {"value": env.value}},
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
                                                },
                                                {
                                                    "id": "box-otel-agent",
                                                    "static_resources": [
                                                        {
                                                            "resource_ref": "config-otel.yaml",
                                                            "mount_point": "/etc/otel",
                                                        },
                                                        {
                                                            "resource_ref": "otel_agent",
                                                            "mount_point": "/otel_agent_bin",
                                                        },
                                                    ],
                                                    "env": [
                                                        {
                                                            "name": "GOMAXPROCS",
                                                            "value": {"literal_env": {"value": "1"}},
                                                        }
                                                    ],
                                                },
                                                {
                                                    "id": "box-nginx",
                                                    "static_resources": [
                                                        {
                                                            "resource_ref": "nginx.conf",
                                                            "mount_point": "/etc/nginx/sites-enabled/",
                                                        },
                                                        {
                                                            "resource_ref": "nginx_ssl",
                                                            "mount_point": "/etc/nginx/ssl",
                                                        },
                                                    ],
                                                },
                                                {
                                                    "id": "box-unified-agent",
                                                    "rootfs": {},
                                                    "static_resources": [
                                                        {
                                                            "resource_ref": "unified_agent_config",
                                                            "mount_point": "/unified_agent_config",
                                                        },
                                                        {
                                                            "resource_ref": "unified_agent",
                                                            "mount_point": "/unified_agent_bin",
                                                        },
                                                    ],
                                                },
                                            ],
                                            "mutable_workloads": [
                                                {"workload_ref": "nginx-workload"},
                                                {"workload_ref": "api-workload"},
                                                {"workload_ref": "otel-agent-workload"},
                                                {"workload_ref": "unified-agent-workload"},
                                            ],
                                        }
                                    },
                                    "secret_refs": secret_refs,
                                }
                            },
                            "constraints": {"antiaffinity_constraints": [{"key": "rack", "max_pods": "1"}]},
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
                    "logbroker_tools_sandbox_info": {"revision": "2739742779"},
                    "deploy_settings": {
                        "cluster_sequence": [
                            {"yp_cluster": "vla"},
                            {"yp_cluster": "sas"},
                        ]
                    },
                    "patchers_revision": 13,
                }
            },
            "account_id": "abc:cba",
        }
        if env == Env.PRODUCTION:
            spec["deploy_units"]["faas-tests-add"]["sox_service"] = True

        return spec

    def test(self, default_sequence_tasklet, correct_spec):
        default_sequence_tasklet.run()
        spec = MessageToDict(default_sequence_tasklet.context.spec, preserving_proto_field_name=True)
        assert spec == correct_spec
