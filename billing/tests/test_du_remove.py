import pytest
from yt.orm.library.common import protobuf_to_dict  # noqa
from google.protobuf.json_format import MessageToDict
from copy import deepcopy


class TestDuRemove:
    """
    Initial: empty spec

    Plan: add one du -> run tasklet

    Result: get initial empty spec
    """

    @pytest.fixture
    def faas_resources(self):
        return []

    @pytest.fixture
    def du_to_remove(self):
        return {
            "faas-tests-to_remove": {
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

    @pytest.fixture
    def correct_spec(self, base_spec):
        return deepcopy(base_spec)

    @pytest.fixture
    def spec(self, base_spec, du_to_remove):
        initial_spec = deepcopy(base_spec)
        initial_spec["deploy_units"] = du_to_remove
        return initial_spec

    def test(self, default_sequence_tasklet, base_spec):
        default_sequence_tasklet.run()
        spec = MessageToDict(default_sequence_tasklet.context.spec, preserving_proto_field_name=True)

        assert spec == base_spec
