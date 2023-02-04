"""Tests project automation limits modification API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase


@pytest.fixture
def test(request, monkeypatch_production_env):
    test = TestCase.create(request)
    request.addfinalizer(test.projects.assert_equal)
    test.mock_projects()
    return test


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unauthenticated(test, unauthenticated, method):
    project = test.mock_project({"id": "some-id"})
    result = test.api_client.open("/v1/projects/" + project.id + "/automation_limits", method=method, data={})
    assert result.status_code == http.client.UNAUTHORIZED


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unauthorized(test, unauthorized_project, method):
    project = test.mock_project({"id": "some-id"})
    result = test.api_client.open(
        "/v1/projects/" + project.id + "/automation_limits", method=method, data={"max_dead_hosts": []}
    )
    assert result.status_code == http.client.FORBIDDEN


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_set(test, method):
    new_limits = {
        "max_unreachable_failures": [{"period": "1s", "limit": 1}, {"period": "10m", "limit": 10}],
        "max_dead_hosts": [{"period": "100h", "limit": 100}, {"period": "1000d", "limit": 1000}],
    }

    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open("/v1/projects/" + project.id + "/automation_limits", method=method, data=new_limits)
    assert result.status_code == http.client.OK

    project.automation_limits.update(new_limits)
    assert result.json == project.automation_limits

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
def test_unset(test, method):
    new_limits = {
        "max_dns_fixes": [],
        "max_unreachable_failures": [],
        "max_ssh_failures": [],
        "max_memory_failures": [],
        "max_disk_failures": [],
        "max_link_failures": [],
        "max_cpu_failures": [],
        "max_reboots_failures": [],
        "max_tainted_kernel_failures": [],
        "max_dead_hosts": [],
    }
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open("/v1/projects/" + project.id + "/automation_limits", method=method, data=new_limits)
    assert result.status_code == http.client.OK

    for limit_name in new_limits:
        del project.automation_limits[limit_name]

    assert result.json == project.automation_limits

    test.projects.assert_equal()


def test_set_custom(test):
    project = test.mock_project({"id": "some-id"})

    new_limits = {"max_dead_hosts": [{"period": "1h", "limit": 10}], "custom_check": [{"period": "100h", "limit": 100}]}
    project.automation_limits.update(new_limits)

    result_limits = test.api_client.patch("/v1/projects/" + project.id + "/automation_limits", data=new_limits)
    assert result_limits.status_code == http.client.OK
    assert result_limits.json == project.automation_limits

    # check that we can get custom limits back too.
    result_project = test.api_client.get(
        "/v1/projects/" + project.id, query_string={"fields": ["name", "automation_limits"]}
    )

    assert result_project.json["automation_limits"] == project.automation_limits

    test.projects.assert_equal()


class TestInvalidInput:
    @staticmethod
    def assert_is_bad_request(test, data):
        project = test.mock_project({"id": "some-id"})

        result = test.api_client.patch("/v1/projects/" + project.id + "/automation_limits", data=data)
        assert result.status_code == http.client.BAD_REQUEST

        test.projects.assert_equal()

    def test_empty_limits_are_bad_request(self, test):
        self.assert_is_bad_request(test, {})

    def test_invalid_period_is_bad_request(self, test):
        self.assert_is_bad_request(test, {"max_dead_hosts": [{"period": "1", "limit": 1}]})

    def test_unknown_period_is_bad_request(self, test):
        self.assert_is_bad_request(test, {"max_dead_hosts": [{"period": "1r", "limit": 1}]})

    def test_missing_period_is_bad_request(self, test):
        self.assert_is_bad_request(test, {"max_dead_hosts": [{"limit": 1}]})

    def test_invalid_limit_is_bad_request(self, test):
        self.assert_is_bad_request(test, {"max_dead_hosts": [{"period": "1d", "limit": -1}]})

    def test_missing_limit_is_bad_request(self, test):
        self.assert_is_bad_request(test, {"max_dead_hosts": [{"period": "1d"}]})

    def test_extra_field_is_bad_request(self, test):
        self.assert_is_bad_request(test, {"max_dead_hosts": [{"period": "1d", "limit": 1, "extra_field": 0}]})
