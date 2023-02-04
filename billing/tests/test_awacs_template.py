import pytest
from google.protobuf.json_format import MessageToDict
from billing.hot.faas.tasklets.deploy_faas.impl.awacs_template import (
    backend_template,
    upstream_template,
)


class TestAwacs:
    @pytest.fixture
    def awacs_namespace(self):
        return "faas.unitests.yandex-team.net"

    @pytest.fixture
    def stage_id(self):
        return "awacs-test-stage"

    @pytest.fixture
    def endpoint(self):
        return "faas-awacs-creator"

    @pytest.fixture
    def clusters(self):
        return ["vla", "man", "sas"]

    @pytest.fixture
    def backends(self, clusters, endpoint, stage_id):
        return [f"{stage_id}_{endpoint}_{cluster}" for cluster in clusters]

    @pytest.fixture
    def correct_backend(self):
        return {
            "selector": {
                "type": "YP_ENDPOINT_SETS_SD",
                "yp_endpoint_sets": [
                    {
                        "cluster": "vla",
                        "endpoint_set_id": "awacs-test-stage.faas-awacs-creator",
                        "port": {},
                        "weight": {},
                    }
                ],
            }
        }

    @pytest.fixture
    def correct_upstream(self):
        return {
            "yandex_balancer": {
                "config": {
                    "l7_upstream_macro": {
                        "version": "0.0.2",
                        "id": "faas_unitests_yandex-team_net-faas-awacs-creator",
                        "matcher": {"path_re": "/faas-awacs-creator(/.*)?"},
                        "monitoring": {"uuid": "faas_unitests_yandex-team_net-faas-awacs-creator"},
                        "headers": [
                            {"create": {"target": "X-Real-IP", "func": "realip"}},
                            {"create": {"target": "X-Balancer-IP", "func": "localip"}},
                            {
                                "create": {
                                    "target": "X-Request-Id",
                                    "keep_existing": True,
                                    "func": "reqid",
                                }
                            },
                        ],
                        "by_dc_scheme": {
                            "dc_balancer": {
                                "weights_section_id": "bygeo",
                                "method": "BY_DC_WEIGHT",
                                "attempts": 2,
                            },
                            "balancer": {
                                "compat": {"method": "ACTIVE"},
                                "attempts": 2,
                                "max_reattempts_share": 0.15,
                                "fast_attempts": 2,
                                "health_check": {
                                    "delay": "5s",
                                    "request": "GET /ping HTTP/1.1\\nHost: faas.unitests.yandex-team.net\\n\\n",
                                },
                                "use_https_to_endpoints": {},
                                "do_not_retry_http_responses": True,
                                "retry_non_idempotent": False,
                                "connect_timeout": "70ms",
                                "backend_timeout": "10s",
                                "keepalive_count": 100,
                            },
                            "dcs": [
                                {
                                    "name": "vla",
                                    "backend_ids": ["awacs-test-stage_faas-awacs-creator_vla"],
                                },
                                {
                                    "name": "man",
                                    "backend_ids": ["awacs-test-stage_faas-awacs-creator_man"],
                                },
                                {
                                    "name": "sas",
                                    "backend_ids": ["awacs-test-stage_faas-awacs-creator_sas"],
                                },
                            ],
                            "on_error": {
                                "static": {
                                    "status": 504,
                                    "content": "Service unavailable",
                                }
                            },
                        },
                    }
                },
                "mode": "EASY_MODE2",
            },
            "labels": {"order": "10000000"},
        }

    def test_awacs_backend(self, stage_id, endpoint, clusters, correct_backend):
        backend = backend_template(stage_id, endpoint, clusters[0])
        backend = MessageToDict(backend, preserving_proto_field_name=True)
        assert backend == correct_backend

    def test_awacs_upstream(self, stage_id, endpoint, backends, awacs_namespace, correct_upstream):
        id = f"{awacs_namespace.replace('.', '_')}-{endpoint}"
        upstream = upstream_template(id, awacs_namespace, endpoint, backends)
        upstream = MessageToDict(upstream, preserving_proto_field_name=True)
        assert upstream == correct_upstream
