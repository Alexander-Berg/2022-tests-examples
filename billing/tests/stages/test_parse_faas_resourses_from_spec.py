import pytest
from billing.hot.faas.tasklets.deploy_faas.impl.deploy_controller.stage import Stage
from copy import deepcopy
from yp import data_model


class TestParseFaasResoursesEmptySpec:
    @pytest.fixture
    def sequence_func(self, faas_tasklet):
        return lambda: [
            Stage("testing get spec", faas_tasklet._get_current_spec),
            Stage("testing changes", faas_tasklet._parse_faas_resources_from_spec),
        ]

    @pytest.fixture
    def spec(self, base_spec):
        return deepcopy(base_spec)

    @pytest.fixture
    def faas_resources(self):
        return []

    def test(self, tasklet_with_sequence, spec):
        tasklet_with_sequence.run()
        assert tasklet_with_sequence.context.instance_faas_resource_data == {}


class TestParseFaasResoursesIncorrectSpec:
    @pytest.fixture
    def sequence_func(self, faas_tasklet):
        return lambda: [
            Stage("testing get spec", faas_tasklet._get_current_spec),
            Stage("testing changes", faas_tasklet._parse_faas_resources_from_spec),
        ]

    @pytest.fixture
    def spec(self, base_spec):
        spec = deepcopy(base_spec)

        spec["deploy_units"] = {
            "faas-tests-endpoint": {
                "images_for_boxes": {
                    "box-nginx": {
                        "registry_host": "registry.yandex.net",
                        "name": "paysys/deploy",
                        "tag": "nginx_ssl_deploy_v0.7",
                    },
                },
                "revision": 1,
                "replica_set": {"replica_set_template": {"pod_template_spec": {"spec": {}}}},
            },
        }
        return spec

    @pytest.fixture
    def faas_resources(self):
        return []

    def test(self, tasklet_with_sequence, spec):
        tasklet_with_sequence.run()

        assert tasklet_with_sequence.context.instance_faas_resource_data == {
            "faas-tests-endpoint": {
                "resource": None,
                "functions": None,
                "cluster_settings": {},
            }
        }


class TestParseFaasResourses:
    @pytest.fixture
    def sequence_func(self, faas_tasklet):
        return lambda: [
            Stage("testing get spec", faas_tasklet._get_current_spec),
            Stage("testing changes", faas_tasklet._parse_faas_resources_from_spec),
        ]

    @pytest.fixture
    def spec(self, base_spec):
        spec = deepcopy(base_spec)

        spec["deploy_units"] = {
            "faas-tests-endpoint": {
                "images_for_boxes": {
                    "box-nginx": {
                        "registry_host": "registry.yandex.net",
                        "name": "paysys/deploy",
                        "tag": "nginx_ssl_deploy_v0.7",
                    },
                },
                "revision": 1,
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
                                                },
                                                {
                                                    "id": "nginx_ssl",
                                                    "verification": {
                                                        "checksum": "EMPTY:",
                                                        "check_period_ms": 18000,
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
                                                        "check_period_ms": 18000,
                                                    },
                                                    "url": "sbr:2342437831",
                                                },
                                                {
                                                    "id": "nginx.conf",
                                                    "verification": {
                                                        "checksum": "MD5:c42e9374d129210b80ff08ad377dd357",
                                                        "check_period_ms": 18000,
                                                    },
                                                    "url": "sbr:2512700458",
                                                },
                                                {
                                                    "id": "unified_agent_config",
                                                    "verification": {
                                                        "checksum": "MD5:9b74bba99c874bf2a3895c96473c1280",
                                                        "check_period_ms": 18000,
                                                    },
                                                    "url": "sbr:2513637215",
                                                },
                                                {
                                                    "id": "unified_agent",
                                                    "verification": {
                                                        "checksum": "MD5:c0d020b1a0527405a3ea4d8d560ef6c2",
                                                        "check_period_ms": 18000,
                                                    },
                                                    "url": "sbr:3372753689",
                                                },
                                                {
                                                    "id": "otel_agent",
                                                    "verification": {
                                                        "checksum": "EMPTY:",
                                                        "check_period_ms": 18000,
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
                                                "env": [
                                                    {
                                                        "name": "URL_PREFIX",
                                                        "value": {"literal_env": {"value": "/faas-tests-add"}},
                                                    }
                                                ],
                                                "readiness_check": {
                                                    "http_get": {
                                                        "port": 9000,
                                                        "path": "/ping",
                                                        "expected_answer": "pong",
                                                    }
                                                },
                                                "box_ref": "box-api",
                                                "start": {
                                                    "command_line": 'bash -c "chmod a+x /opt/bin/faas && /opt/bin/faas runserver --port 9000 --host ::1"'
                                                },
                                                "stop_policy": {
                                                    "container": {
                                                        "command_line": 'bash -c "pkill -SIGUSR1 faas && sleep 15 && pkill faas"'
                                                    },
                                                    "max_tries": 3,
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
                                                    "command_line": "/otel_agent_bin/otelcontribcol --config /etc/otel/config.yaml "
                                                    "--set exporters.jaeger.endpoint"
                                                    "=billing.c.jaeger.yandex-team.ru:14250"
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
                                                    "command_line": "/unified_agent_bin/unified_agent "
                                                    "-c /unified_agent_config/config.yaml"
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
                                                    "vcpu_guarantee": 1,
                                                    "vcpu_limit": 1,
                                                },
                                                "env": [
                                                    {
                                                        "name": "YENV_TYPE",
                                                        "value": {
                                                            "literal_env": {
                                                                "value": "testing",
                                                            }
                                                        },
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
                                    }
                                },
                            }
                        }
                    }
                },
            },
        }
        return spec

    @pytest.fixture
    def faas_resources(self):
        return []

    @pytest.fixture
    def correct_instance_faas_resourse_data(self):
        return {
            "faas-tests-endpoint": {
                "functions": '[{"function": "billing.hot.faas.lib.python.dummy.dummy_calculator.dummy_calculator", '
                '"name": "calc"}]',
                "resource": data_model.TResource(
                    id="faas",
                    url="sbr:1",
                    verification=data_model.TVerification(checksum="MD5:verysafehash123", check_period_ms=18000),
                ),
                "cluster_settings": {},
            }
        }

    def test(self, tasklet_with_sequence, correct_instance_faas_resourse_data):
        tasklet_with_sequence.run()
        assert tasklet_with_sequence.context.instance_faas_resource_data == correct_instance_faas_resourse_data


class TestParseFaasResoursesClusterDeploySettings:
    @pytest.fixture
    def sequence_func(self, faas_tasklet):
        return lambda: [
            Stage("testing get spec", faas_tasklet._get_current_spec),
            Stage("testing changes", faas_tasklet._parse_faas_resources_from_spec),
        ]

    @pytest.fixture
    def spec(self, base_spec):
        spec = deepcopy(base_spec)

        spec["deploy_units"] = {
            "faas-tests-endpoint": {
                "images_for_boxes": {
                    "box-nginx": {
                        "registry_host": "registry.yandex.net",
                        "name": "paysys/deploy",
                        "tag": "nginx_ssl_deploy_v0.7",
                    },
                },
                "revision": 1,
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
                                                },
                                                {
                                                    "id": "nginx_ssl",
                                                    "verification": {
                                                        "checksum": "EMPTY:",
                                                        "check_period_ms": 18000,
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
                                                        "check_period_ms": 18000,
                                                    },
                                                    "url": "sbr:2342437831",
                                                },
                                                {
                                                    "id": "nginx.conf",
                                                    "verification": {
                                                        "checksum": "MD5:c42e9374d129210b80ff08ad377dd357",
                                                        "check_period_ms": 18000,
                                                    },
                                                    "url": "sbr:2512700458",
                                                },
                                                {
                                                    "id": "unified_agent_config",
                                                    "verification": {
                                                        "checksum": "MD5:9b74bba99c874bf2a3895c96473c1280",
                                                        "check_period_ms": 18000,
                                                    },
                                                    "url": "sbr:2513637215",
                                                },
                                                {
                                                    "id": "unified_agent",
                                                    "verification": {
                                                        "checksum": "MD5:c0d020b1a0527405a3ea4d8d560ef6c2",
                                                        "check_period_ms": 18000,
                                                    },
                                                    "url": "sbr:3372753689",
                                                },
                                                {
                                                    "id": "otel_agent",
                                                    "verification": {
                                                        "checksum": "EMPTY:",
                                                        "check_period_ms": 18000,
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
                                                "env": [
                                                    {
                                                        "name": "URL_PREFIX",
                                                        "value": {"literal_env": {"value": "/faas-tests-add"}},
                                                    }
                                                ],
                                                "readiness_check": {
                                                    "http_get": {
                                                        "port": 9000,
                                                        "path": "/ping",
                                                        "expected_answer": "pong",
                                                    }
                                                },
                                                "box_ref": "box-api",
                                                "start": {
                                                    "command_line": 'bash -c "chmod a+x /opt/bin/faas && /opt/bin/faas runserver --port 9000 --host ::1"'
                                                },
                                                "stop_policy": {
                                                    "container": {
                                                        "command_line": 'bash -c "pkill -SIGUSR1 faas && sleep 15 && pkill faas"'
                                                    },
                                                    "max_tries": 3,
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
                                                    "command_line": "/otel_agent_bin/otelcontribcol --config /etc/otel/config.yaml "
                                                    "--set exporters.jaeger.endpoint"
                                                    "=billing.c.jaeger.yandex-team.ru:14250"
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
                                                    "command_line": "/unified_agent_bin/unified_agent "
                                                    "-c /unified_agent_config/config.yaml"
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
                                                    "vcpu_guarantee": 1,
                                                    "vcpu_limit": 1,
                                                },
                                                "env": [
                                                    {
                                                        "name": "YENV_TYPE",
                                                        "value": {
                                                            "literal_env": {
                                                                "value": "testing",
                                                            }
                                                        },
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
                                    }
                                },
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
            },
        }
        return spec

    @pytest.fixture
    def faas_resources(self):
        return []

    @pytest.fixture
    def correct_instance_faas_resourse_data(self):
        return {
            "faas-tests-endpoint": {
                "functions": '[{"function": "billing.hot.faas.lib.python.dummy.dummy_calculator.dummy_calculator", '
                '"name": "calc"}]',
                "resource": data_model.TResource(
                    id="faas",
                    url="sbr:1",
                    verification=data_model.TVerification(checksum="MD5:verysafehash123", check_period_ms=18000),
                ),
                "cluster_settings": {"sas": {"amount": 1}, "vla": {"amount": 1}},
            }
        }

    def test(self, tasklet_with_sequence, correct_instance_faas_resourse_data):
        tasklet_with_sequence.run()
        assert tasklet_with_sequence.context.instance_faas_resource_data == correct_instance_faas_resourse_data
