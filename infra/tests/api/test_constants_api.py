"""Tests constants API."""

from itertools import chain

import pytest
import http.client

import walle.expert.automation_plot
import walle.scenario.constants as scenario_constants
from infra.walle.server.tests.lib.util import TestCase
from walle import hosts
from walle.expert.types import CheckType
from walle.util.deploy_config import DeployConfigPolicies


@pytest.fixture
def test(request):
    return TestCase.create(request)


def test_get_constants(test):
    response = test.api_client.get("/v1/constants")
    assert response.status_code == http.client.OK

    constants = response.json
    assert type(constants) is dict and constants


def test_statuses_have_filters_first(test):
    response = test.api_client.get("/v1/constants")
    expected_first = hosts.HostStatus.ALL_FILTERS + [hosts.HostStatus.READY]
    slice_size = len(expected_first)

    assert response.json["host_statuses"][:slice_size] == expected_first


def test_all_checks_are_present_in_check_groups(test):
    response = test.api_client.get("/v1/constants")
    constants = response.json

    assert set(chain(*constants["health_check_groups"].values())) == set(CheckType.ALL_UI_TYPES)


def test_automation_plot_checks_present_in_health_statuses(test, mp):
    mock_automation_plots_checks = {'automation_plot_check_mock1', 'automation_plot_check_mock2'}
    mp.function(walle.expert.automation_plot.get_all_automation_plots_checks, return_value=mock_automation_plots_checks)

    response = test.api_client.get("/v1/constants")
    health_statuses = set(response.json["health_statuses"])

    assert mock_automation_plots_checks.issubset(health_statuses)


def test_automation_plot_checks_present_in_constants(test, mp):
    mock_automation_plots_checks = {'automation_plot_check_mock1', 'automation_plot_check_mock2'}
    mp.function(walle.expert.automation_plot.get_all_automation_plots_checks, return_value=mock_automation_plots_checks)

    response = test.api_client.get("/v1/constants")

    assert mock_automation_plots_checks == set(response.json["automation_plots_checks"])


def test_deploy_config_policies(walle_test):
    response = walle_test.api_client.get("/v1/constants")
    assert response.status_code == http.client.OK
    policies = response.json["deploy_config_policies"]

    # check that default policy goes first
    assert policies[0]["name"] == DeployConfigPolicies.DISKMANAGER

    assert {policy["name"] for policy in policies} == set(DeployConfigPolicies.get_all_names())
    assert all(policy.get("description") for policy in policies)


def test_scenario_statuses(walle_test):
    response = walle_test.api_client.get("/v1/constants")
    assert response.status_code == http.client.OK

    scenario_statuses = response.json["scenario_statuses"]
    assert set(scenario_statuses) == set(scenario_constants.ScenarioFsmStatus)


def test_scenario_host_statuses(walle_test):
    response = walle_test.api_client.get("/v1/constants")
    assert response.status_code == http.client.OK

    scenario_host_statuses = response.json["scenario_host_statuses"]
    assert scenario_host_statuses == scenario_constants.HostScenarioStatus.ALL
