"""Tests inventory client."""

import json
from unittest.mock import Mock

import pytest

from infra.walle.server.tests.lib.util import TEST_HOST, load_mock_data
from walle.clients import inventory, deploy, bot
from walle.constants import PROVISIONER_EINE, PROVISIONER_LUI, FLEXY_EINE_PROFILE, EINE_NOP_PROFILE, NetworkTarget
from walle.errors import InvalidDeployConfiguration, InvalidHostConfiguration


def test_get_host_info(monkeypatch):
    mock_os_info_resp = json.loads(load_mock_data("mocks/bot_os_info_response.json"))
    mock_consist_of_resp = json.loads(load_mock_data("mocks/bot_consist_of_response.json"))
    monkeypatch.setattr(
        bot,
        "json_request",
        Mock(
            side_effect=[
                mock_os_info_resp,
                mock_consist_of_resp,
                mock_consist_of_resp,  # TODO(rocco66): double _consistof_request calls WTF
            ]
        ),
    )

    inv, name, ipmi_mac, macs, location, bot_project_id, platform = inventory.get_host_info_and_check_status(TEST_HOST)

    assert inv > 0 and name == TEST_HOST and ipmi_mac and macs
    for field in location._fields:
        assert getattr(location, field) is not None


class TestCheckDeployConfiguration:
    def test_lui_tags_set(self):
        with pytest.raises(InvalidDeployConfiguration):
            inventory.check_deploy_configuration(PROVISIONER_LUI, "web", None, tags=["bla"])

    def test_lui_unknown_config(self, mp):
        mp.function(deploy.get_deploy_configs, return_value=["not-web"])
        with pytest.raises(InvalidDeployConfiguration):
            inventory.check_deploy_configuration(PROVISIONER_LUI, "web", None)

    @pytest.fixture
    def mock_eine_profiles(self, mp):
        mp.function(inventory.get_eine_profiles, return_value=[FLEXY_EINE_PROFILE])

    def test_eine_unknown_eine_profile(self, mp, mock_eine_profiles):
        with pytest.raises(InvalidDeployConfiguration):
            inventory.check_deploy_configuration(PROVISIONER_EINE, EINE_NOP_PROFILE, None)

    def test_eine_with_cert(self, mp, mock_eine_profiles):
        with pytest.raises(InvalidHostConfiguration):
            inventory.check_deploy_configuration(PROVISIONER_EINE, FLEXY_EINE_PROFILE, None, need_certificate=True)

    @pytest.mark.parametrize("network", [NetworkTarget.PARKING, NetworkTarget.PROJECT, NetworkTarget.DEPLOY])
    def test_eine_with_network_not_service(self, mock_eine_profiles, network):
        with pytest.raises(InvalidHostConfiguration):
            inventory.check_deploy_configuration(PROVISIONER_EINE, FLEXY_EINE_PROFILE, None, network=network)

    def test_eine_with_network_service(self, mock_eine_profiles):
        inventory.check_deploy_configuration(PROVISIONER_EINE, FLEXY_EINE_PROFILE, None, network=NetworkTarget.SERVICE)

    def test_eine_with_deploy_config_policy(self, mock_eine_profiles):
        with pytest.raises(InvalidDeployConfiguration):
            inventory.check_deploy_configuration(PROVISIONER_EINE, FLEXY_EINE_PROFILE, None, deploy_config_policy="any")
