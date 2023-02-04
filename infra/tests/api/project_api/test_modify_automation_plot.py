"""Tests project modification API: set/change/remove automation plot."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import TestCase, drop_none

_EXISTING_AUTOMATION_PLOT = "rtc-automation-plot"
_PREVIOUS_AUTOMATION_PLOT = "search-automation-plot"
_NON_EXISTING_AUTOMATION_PLOT = "market-automation-plot"


@pytest.fixture
def test(request, monkeypatch_production_env):
    test = TestCase.create(request)
    test.mock_projects()
    test.automation_plot.mock({"id": _EXISTING_AUTOMATION_PLOT, "name": "Automation Plot"})
    return test


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("unauthenticated")
def test_unauthenticated(test, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open(
        "/v1/projects/{}/automation_plot".format(project.id),
        method=method,
        data={"automation_plot_id": _EXISTING_AUTOMATION_PLOT},
    )
    assert result.status_code == http.client.UNAUTHORIZED

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("unauthorized_project")
def test_unauthorized(test, method):
    project = test.mock_project({"id": "some-id"})

    result = test.api_client.open(
        "/v1/projects/{}/automation_plot".format(project.id),
        method=method,
        data={"automation_plot_id": _EXISTING_AUTOMATION_PLOT},
    )
    assert result.status_code == http.client.FORBIDDEN

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.parametrize("reason", [None, "reason mock"])
@pytest.mark.parametrize("automation_plot_id", [None, _PREVIOUS_AUTOMATION_PLOT])
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_set_automation_plot(test, method, reason, automation_plot_id):
    project = test.mock_project(
        drop_none({"id": "some-id", "name": "Some name", "automation_plot_id": automation_plot_id})
    )

    result = test.api_client.open(
        "/v1/projects/{}/automation_plot".format(project.id),
        method=method,
        data=drop_none({"automation_plot_id": _EXISTING_AUTOMATION_PLOT, "reason": reason}),
    )
    assert result.status_code == http.client.NO_CONTENT

    project.automation_plot_id = _EXISTING_AUTOMATION_PLOT
    test.projects.assert_equal()


@pytest.mark.parametrize("reason", [None, "reason mock"])
@pytest.mark.parametrize("automation_plot_id", [None, _PREVIOUS_AUTOMATION_PLOT])
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_unset_automation_plot(test, reason, automation_plot_id):
    project = test.mock_project({"id": "some-id", "name": "Some name", "automation_plot_id": automation_plot_id})

    result = test.api_client.open(
        "/v1/projects/{}/automation_plot".format(project.id), method="DELETE", data=drop_none({"reason": reason})
    )
    assert result.status_code == http.client.NO_CONTENT

    del project.automation_plot_id
    test.projects.assert_equal()


@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_non_existing_project_delete_plot(test):
    result = test.api_client.open("/v1/projects/project-id/automation_plot", method="DELETE", data={})
    assert result.status_code == http.client.NOT_FOUND

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_non_existing_project_set_plot(test, method):
    result = test.api_client.open(
        "/v1/projects/project-id/automation_plot", method=method, data={"automation_plot_id": _EXISTING_AUTOMATION_PLOT}
    )
    assert result.status_code == http.client.NOT_FOUND

    test.projects.assert_equal()


@pytest.mark.parametrize("method", ["POST", "PATCH"])
@pytest.mark.usefixtures("monkeypatch_locks", "authorized_admin")
def test_non_existing_plot(test, method):
    project = test.mock_project({"id": "some-id", "name": "Some name"})

    result = test.api_client.open(
        "/v1/projects/{}/automation_plot".format(project.id),
        method=method,
        data={"automation_plot_id": _NON_EXISTING_AUTOMATION_PLOT},
    )
    assert result.status_code == http.client.NOT_FOUND

    test.projects.assert_equal()
