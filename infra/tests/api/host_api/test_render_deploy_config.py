import json

import pytest
import http.client

from walle.constants import PROVISIONER_LUI
from walle.hosts import Host, DeployConfiguration
from walle.util import deploy_config

PROVISIONER = PROVISIONER_LUI
CONFIG = "mock-config"
DEPLOY_POLICY = deploy_config.DeployConfigPolicies.PASSTHROUGH
HOSTNAME = "some-host.y-t.net"
CONFIG_CONTENT = {"some_key": "some_val"}


class TestRenderDeployConfig:
    @pytest.mark.parametrize(
        "passed_params",
        [
            {"deploy_config_name": "bla"},
            {"deploy_config_policy": DEPLOY_POLICY},
            {"deploy_config_name": "bla", "deploy_config_policy": DEPLOY_POLICY},
            {"provisioner": "lui"},
        ],
    )
    def test_disallows_insufficient_params(self, walle_test, passed_params):
        resp = walle_test.api_client.post("/v1/hosts/{}/render-deploy-config".format(HOSTNAME), data=passed_params)
        assert resp.status_code == http.client.BAD_REQUEST
        expected_error = "provisioner, deploy_config_name and deploy_config_policy can be used only together"
        assert expected_error in resp.json["message"]

    def test_calls_policy(self, walle_test, deduce_deploy_configuration_mock, policy_generate_mock):
        walle_test.mock_host({"name": HOSTNAME})
        resp = walle_test.api_client.post("/v1/hosts/{}/render-deploy-config".format(HOSTNAME), data={})
        assert resp.status_code == http.client.OK
        assert policy_generate_mock.called

    def test_returns_deduced_params(self, walle_test, deduce_deploy_configuration_mock, policy_generate_mock):
        walle_test.mock_host({"name": HOSTNAME})
        resp = walle_test.api_client.post("/v1/hosts/{}/render-deploy-config".format(HOSTNAME), data={})
        assert resp.status_code == http.client.OK
        assert resp.json["deduced_provisioner"] == PROVISIONER
        assert resp.json["deduced_deploy_config"] == CONFIG
        assert resp.json["deduced_deploy_config_policy"] == DEPLOY_POLICY

    def test_decodes_config_json(self, walle_test, deduce_deploy_configuration_mock, policy_generate_mock):
        walle_test.mock_host({"name": HOSTNAME})
        resp = walle_test.api_client.post("/v1/hosts/{}/render-deploy-config".format(HOSTNAME), data={})
        assert resp.status_code == http.client.OK
        assert resp.json["config_content"] == CONFIG_CONTENT
        assert "config_content_json" not in resp.json

    @pytest.fixture()
    def deduce_deploy_configuration_mock(self, mp):
        deploy_conf = DeployConfiguration(
            provisioner=PROVISIONER,
            config=CONFIG,
            deploy_config_policy=DEPLOY_POLICY,
            tags=None,
            certificate=None,
            ipxe=None,
            network=None,
        )
        mock = mp.method(
            Host.deduce_deploy_configuration, return_value=(None, None, None, None, None, deploy_conf), obj=Host
        )
        return mock

    @pytest.fixture()
    def policy_generate_mock(self, mp):
        return_value = {"config_content_json": json.dumps(CONFIG_CONTENT)}
        mock = mp.method(
            deploy_config.PassthroughConfigStrategy.generate,
            return_value=return_value,
            obj=deploy_config.PassthroughConfigStrategy,
        )
        return mock
