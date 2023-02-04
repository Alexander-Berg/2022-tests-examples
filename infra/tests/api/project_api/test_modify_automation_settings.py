"""Tests project automation settings modification API."""

from unittest.mock import call

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase, patch, add_dns_domain_and_vlan_scheme_to_project
from walle.constants import VLAN_SCHEMES
from walle.models import timestamp
from walle.projects import HEALING_AUTOMATION_LIMIT_NAMES, DNS_AUTOMATION_LIMIT_NAMES, AUTOMATION_LIMIT_NAMES
from walle.util.misc import filter_dict_keys


@pytest.fixture
def test(request, monkeypatch_production_env, monkeypatch_timestamp):
    test = TestCase.create(request)
    test.mock_projects()
    return test


@pytest.mark.parametrize("method", ("PUT", "DELETE"))
@pytest.mark.parametrize("subpath", ("", "/healing", "/dns"))
def test_unauthenticated(test, unauthenticated, method, subpath):
    project = test.mock_project({"id": "some-id"})
    result = test.api_client.open("/v1/projects/" + project.id + "/enable_automation" + subpath, method=method, data={})

    assert result.status_code == http.client.UNAUTHORIZED
    test.projects.assert_equal()


@pytest.mark.usefixtures("unauthorized_project")
@pytest.mark.parametrize("method", ("PUT", "DELETE"))
@pytest.mark.parametrize("subpath", ("", "/healing", "/dns"))
def test_unauthorized(test, method, subpath):
    project = test.mock_project({"id": "some-id"})
    result = test.api_client.open("/v1/projects/" + project.id + "/enable_automation" + subpath, method=method, data={})
    assert result.status_code == http.client.FORBIDDEN
    test.projects.assert_equal()


@patch("walle.util.notifications.on_dns_automation_enabled")
@patch("walle.util.notifications.on_healing_automation_enabled")
@pytest.mark.parametrize("subpath", ("", "/healing", "/dns"))
@pytest.mark.parametrize("vlan_scheme", VLAN_SCHEMES)
def test_enable_disabled(notify_healing, notify_dns, test, subpath, vlan_scheme):
    project = test.mock_project(
        add_dns_domain_and_vlan_scheme_to_project(
            {
                "id": "some-id",
                "healing_automation": {"enabled": False},
                "dns_automation": {"enabled": False},
            },
            vlan_scheme=vlan_scheme,
        )
    )

    project.healing_automation.credit = {"mock": "mock"}
    project.dns_automation.credit = {"mock": "mock"}

    project.healing_automation.credit_end_time = 666
    project.dns_automation.credit_end_time = 666
    project.save()

    result = test.api_client.put(
        "/v1/projects/" + project.id + "/enable_automation" + subpath, data={"reason": "reason-mock"}
    )

    assert result.status_code == http.client.OK
    automations = {
        "": [project.healing_automation, project.dns_automation],
        "/healing": [project.healing_automation],
        "/dns": [project.dns_automation],
    }[subpath]

    for automation in automations:
        automation.enabled = True
        automation.status_message = "reason-mock"
        automation.failure_log_start_time = timestamp()

        del automation.credit
        del automation.credit_end_time

    if subpath in ("", "/healing"):
        assert notify_healing.mock_calls == [call(test.api_issuer, project.id, reason="reason-mock")]
    if subpath in ("", "/dns"):
        assert notify_dns.mock_calls == [call(test.api_issuer, project.id, reason="reason-mock")]

    test.projects.assert_equal()


class TestEnableDisabledWithCredit:
    @patch("walle.util.notifications.on_healing_automation_enabled")
    def test_enable_disabled_healing_with_credit(self, notify_healing, test):
        project = test.mock_project(
            {
                "id": "some-id",
                "healing_automation": {"enabled": False},
                "dns_automation": {"enabled": False},
            }
        )

        credit = {limit: count for count, limit in enumerate(HEALING_AUTOMATION_LIMIT_NAMES)}

        result = test.api_client.put(
            "/v1/projects/{}/enable_automation/healing".format(project.id),
            data={"credit": dict({"time": 666}, **credit)},
        )
        assert result.status_code == http.client.OK

        project.healing_automation.enabled = True
        project.healing_automation.failure_log_start_time = timestamp()
        project.healing_automation.credit = credit
        project.healing_automation.credit_end_time = timestamp() + 666

        assert notify_healing.mock_calls == [call(test.api_issuer, project.id, reason=None)]
        test.projects.assert_equal()

    @patch("walle.util.notifications.on_healing_automation_enabled")
    def test_enable_healing_allows_custom_credits(self, notify_healing, test):
        project = test.mock_project(
            {
                "id": "some-id",
                "healing_automation": {"enabled": False},
                "dns_automation": {"enabled": False},
            }
        )

        credit = {"max_custom_check_failures": 12}

        result = test.api_client.put(
            "/v1/projects/{}/enable_automation/healing".format(project.id),
            data={"credit": dict({"time": 666}, **credit)},
        )
        assert result.status_code == http.client.OK

        project.healing_automation.enabled = True
        project.healing_automation.failure_log_start_time = timestamp()
        project.healing_automation.credit = credit
        project.healing_automation.credit_end_time = timestamp() + 666

        assert notify_healing.mock_calls == [call(test.api_issuer, project.id, reason=None)]
        test.projects.assert_equal()

    @patch("walle.util.notifications.on_dns_automation_enabled")
    @pytest.mark.parametrize("vlan_scheme", VLAN_SCHEMES)
    def test_enable_disabled_dns_with_credit(self, notify_dns, test, vlan_scheme):
        project = test.mock_project(
            add_dns_domain_and_vlan_scheme_to_project(
                {
                    "id": "some-id",
                    "healing_automation": {"enabled": False},
                    "dns_automation": {"enabled": False},
                },
                vlan_scheme=vlan_scheme,
            )
        )

        credit = {limit: count for count, limit in enumerate(DNS_AUTOMATION_LIMIT_NAMES)}

        result = test.api_client.put(
            "/v1/projects/{}/enable_automation/dns".format(project.id), data={"credit": dict({"time": 666}, **credit)}
        )
        assert result.status_code == http.client.OK

        project.dns_automation.enabled = True
        project.dns_automation.failure_log_start_time = timestamp()
        project.dns_automation.credit = filter_dict_keys(credit, DNS_AUTOMATION_LIMIT_NAMES) or None
        project.dns_automation.credit_end_time = timestamp() + 666

        assert notify_dns.mock_calls == [call(test.api_issuer, project.id, reason=None)]

        test.projects.assert_equal()

    def test_enable_dns_forbids_custom_credits(self, test):
        project = test.mock_project(
            {
                "id": "some-id",
                "healing_automation": {"enabled": False},
                "dns_automation": {"enabled": False},
            }
        )

        credit = {"max_custom_check_failures": 12}

        result = test.api_client.put(
            "/v1/projects/{}/enable_automation/dns".format(project.id), data={"credit": dict({"time": 666}, **credit)}
        )

        assert result.status_code == http.client.BAD_REQUEST
        test.projects.assert_equal()

    @patch("walle.util.notifications.on_dns_automation_enabled")
    @patch("walle.util.notifications.on_healing_automation_enabled")
    @pytest.mark.parametrize("vlan_scheme", VLAN_SCHEMES)
    def test_enable_both_disabled_with_credit(self, notify_healing, notify_dns, test, vlan_scheme):
        project = test.mock_project(
            add_dns_domain_and_vlan_scheme_to_project(
                {
                    "id": "some-id",
                    "healing_automation": {"enabled": False},
                    "dns_automation": {"enabled": False},
                },
                vlan_scheme=vlan_scheme,
            )
        )

        credit = {limit: count for count, limit in enumerate(AUTOMATION_LIMIT_NAMES)}

        result = test.api_client.put(
            "/v1/projects/{}/enable_automation".format(project.id), data={"credit": dict({"time": 666}, **credit)}
        )
        assert result.status_code == http.client.OK

        automation_fields = (project.healing_automation, project.dns_automation)
        automation_limits = (HEALING_AUTOMATION_LIMIT_NAMES, DNS_AUTOMATION_LIMIT_NAMES)

        for automation_field, limit_names in zip(automation_fields, automation_limits):
            automation_field.enabled = True
            automation_field.failure_log_start_time = timestamp()
            automation_field.credit = filter_dict_keys(credit, limit_names)
            automation_field.credit_end_time = timestamp() + 666

        assert notify_healing.mock_calls == [call(test.api_issuer, project.id, reason=None)]
        assert notify_dns.mock_calls == [call(test.api_issuer, project.id, reason=None)]

        test.projects.assert_equal()

    @patch("walle.util.notifications.on_dns_automation_enabled")
    @patch("walle.util.notifications.on_healing_automation_enabled")
    def test_enable_both_automation_forbid_custom_credits(self, notify_healing, notify_dns, test):
        project = test.mock_project(
            {
                "id": "some-id",
                "healing_automation": {"enabled": False},
                "dns_automation": {"enabled": False},
            }
        )

        credit = {"max_custom_check_failures": 12}

        result = test.api_client.put(
            "/v1/projects/{}/enable_automation".format(project.id), data={"credit": dict({"time": 666}, **credit)}
        )
        assert result.status_code == http.client.BAD_REQUEST
        test.projects.assert_equal()


@patch("walle.util.notifications.on_dns_automation_enabled")
@patch("walle.util.notifications.on_healing_automation_enabled")
@pytest.mark.parametrize("subpath", ("", "/healing", "/dns"))
@pytest.mark.parametrize("vlan_scheme", VLAN_SCHEMES)
def test_enable_enabled(notify_healing, notify_dns, test, subpath, vlan_scheme):
    project = test.mock_project(
        add_dns_domain_and_vlan_scheme_to_project(
            {
                "id": "some-id",
                "healing_automation": {"enabled": True, "credit_end_time": 666},
                "dns_automation": {"enabled": True, "credit_end_time": 666},
            },
            vlan_scheme=vlan_scheme,
        )
    )
    project.save()

    result = test.api_client.put(
        "/v1/projects/" + project.id + "/enable_automation" + subpath, data={"reason": "reason-mock"}
    )
    assert result.status_code == http.client.CONFLICT
    expected_message = {
        "": (
            "Rejecting to enable automated healing: already enabled for the project.\n"
            "Rejecting to enable DNS automation: already enabled for the project."
        ),
        "/healing": "Rejecting to enable automated healing: already enabled for the project.",
        "/dns": "Rejecting to enable DNS automation: already enabled for the project.",
    }[subpath]
    assert result.json["message"] == expected_message

    assert notify_healing.call_count == 0
    assert notify_dns.call_count == 0
    test.projects.assert_equal()


@patch("walle.util.notifications.on_dns_automation_disabled")
@patch("walle.util.notifications.on_healing_automation_disabled")
@pytest.mark.parametrize("subpath", ("", "/healing", "/dns"))
@pytest.mark.parametrize("vlan_scheme", VLAN_SCHEMES)
def test_disable_enabled(notify_healing, notify_dns, test, subpath, vlan_scheme):
    project = test.mock_project(
        add_dns_domain_and_vlan_scheme_to_project(
            {
                "id": "some-id",
                "healing_automation": {"enabled": True, "status_message": "reason-mock"},
                "dns_automation": {"enabled": True, "status_message": "reason-mock"},
            },
            vlan_scheme=vlan_scheme,
        )
    )

    result = test.api_client.delete("/v1/projects/" + project.id + "/enable_automation" + subpath)
    assert result.status_code == http.client.OK, result.status + result.data

    automations = {
        "": [project.healing_automation, project.dns_automation],
        "/healing": [project.healing_automation],
        "/dns": [project.dns_automation],
    }[subpath]

    for automation in automations:
        automation.enabled = False
        del automation.status_message
        del automation.credit
        del automation.credit_end_time

    if subpath in ("", "/healing"):
        assert notify_healing.mock_calls == [call(test.api_issuer, project.id, reason=None)]
    if subpath in ("", "/dns"):
        assert notify_dns.mock_calls == [call(test.api_issuer, project.id, reason=None)]

    test.projects.assert_equal()


@patch("walle.util.notifications.on_dns_automation_disabled")
@patch("walle.util.notifications.on_healing_automation_disabled")
@pytest.mark.parametrize("subpath", ("", "/healing", "/dns"))
@pytest.mark.parametrize("vlan_scheme", VLAN_SCHEMES)
def test_disable_disabled(notify_healing, notify_dns, test, subpath, vlan_scheme):
    project = test.mock_project(
        add_dns_domain_and_vlan_scheme_to_project(
            {
                "id": "some-id",
                "healing_automation": {"enabled": False},
                "dns_automation": {"enabled": False},
            },
            vlan_scheme=vlan_scheme,
        )
    )
    result = test.api_client.delete("/v1/projects/" + project.id + "/enable_automation" + subpath)
    assert result.status_code == http.client.OK, result.status + result.data

    assert notify_healing.call_count == 0
    assert notify_dns.call_count == 0
    test.projects.assert_equal()


@patch("walle.util.notifications.on_dns_automation_enabled")
@pytest.mark.parametrize("missing_field", ["dns_domain", "vlan_scheme"])
@pytest.mark.parametrize("vlan_scheme", VLAN_SCHEMES)
def test_enable_dns_in_project_without_dns_or_vlan_scheme_fails(notify_dns, test, missing_field, vlan_scheme):
    project = test.mock_project(
        add_dns_domain_and_vlan_scheme_to_project(
            {
                "id": "some-id",
                "dns_automation": {"enabled": False},
            },
            vlan_scheme=vlan_scheme,
        )
    )
    project[missing_field] = None
    project.save()

    result = test.api_client.put(
        "/v1/projects/{}/enable_automation/dns".format(project.id), data={"reason": "reason-mock"}
    )
    assert result.status_code == http.client.BAD_REQUEST

    test.projects.assert_equal()
